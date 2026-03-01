"""Event bus for internal service communication.

Provides pub/sub mechanisms using a simple interface. Implements both an
in-memory bus for local development/testing and a Redis Streams bus for
production.
"""

import asyncio
import json
import logging
from abc import ABC, abstractmethod
from typing import Any, Callable, Dict, List

from redis.asyncio import Redis

logger = logging.getLogger(__name__)


class EventBus(ABC):
    """Abstract interface for application event pub/sub."""

    @abstractmethod
    async def publish(self, event_type: str, payload: Dict[str, Any]) -> None:
        """Publish an event to the bus."""
        pass

    @abstractmethod
    async def subscribe(self, event_type: str, callback: Callable[[Dict[str, Any]], Any]) -> None:
        """Subscribe a callback to an event type."""
        pass


class InMemoryEventBus(EventBus):
    """Simple in-memory event bus for local dev/testing."""

    def __init__(self):
        self._subscribers: Dict[str, List[Callable]] = {}

    async def publish(self, event_type: str, payload: Dict[str, Any]) -> None:
        logger.debug(f"[InMemoryEventBus] Published {event_type}")
        if event_type in self._subscribers:
            for callback in self._subscribers[event_type]:
                asyncio.create_task(self._safe_invoke(callback, payload))

    async def subscribe(self, event_type: str, callback: Callable[[Dict[str, Any]], Any]) -> None:
        if event_type not in self._subscribers:
            self._subscribers[event_type] = []
        self._subscribers[event_type].append(callback)

    async def _safe_invoke(self, callback: Callable, payload: Any) -> None:
        try:
            if asyncio.iscoroutinefunction(callback):
                await callback(payload)
            else:
                callback(payload)
        except Exception as e:
            logger.error(f"[InMemoryEventBus] Callback error on event: {e}", exc_info=True)


class RedisStreamEventBus(EventBus):
    """Production-ready event bus using Redis Streams."""

    def __init__(self, redis_client: Redis):
        self._redis = redis_client
        self._subscribers: Dict[str, List[Callable]] = {}
        self._listen_tasks: List[asyncio.Task] = []

    async def publish(self, event_type: str, payload: Dict[str, Any]) -> None:
        # Convert all values to strings for Redis hash
        str_payload = {k: json.dumps(v) if isinstance(v, (dict, list)) else str(v) for k, v in payload.items()}
        await self._redis.xadd(f"cipher:events:{event_type}", str_payload, maxlen=1000)
        logger.debug(f"[RedisStreamEventBus] Published {event_type}")

    async def subscribe(self, event_type: str, callback: Callable[[Dict[str, Any]], Any]) -> None:
        if event_type not in self._subscribers:
            self._subscribers[event_type] = []
            # Start consumer task for this stream if not already listening
            task = asyncio.create_task(self._listen(event_type))
            self._listen_tasks.append(task)
            
        self._subscribers[event_type].append(callback)

    async def _listen(self, event_type: str) -> None:
        stream_key = f"cipher:events:{event_type}"
        last_id = "$"  # Only listen for new events after connection
        
        while True:
            try:
                messages = await self._redis.xread({stream_key: last_id}, count=10, block=5000)
                for stream, msg_list in messages:
                    for message_id, data in msg_list:
                        last_id = message_id
                        
                        # Parse string values back to native types if possible
                        parsed_payload = {}
                        for k, v in data.items():
                            k_dec = k.decode('utf-8')
                            v_dec = v.decode('utf-8')
                            try:
                                parsed_payload[k_dec] = json.loads(v_dec)
                            except json.JSONDecodeError:
                                parsed_payload[k_dec] = v_dec
                                
                        for callback in self._subscribers.get(event_type, []):
                            asyncio.create_task(self._safe_invoke(callback, parsed_payload))
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"[RedisStreamEventBus] Listen loop error: {e}")
                await asyncio.sleep(1)

    async def _safe_invoke(self, callback: Callable, payload: Any) -> None:
        try:
            if asyncio.iscoroutinefunction(callback):
                await callback(payload)
            else:
                callback(payload)
        except Exception as e:
            logger.error(f"[RedisStreamEventBus] Callback error: {e}", exc_info=True)


# Global instance injected at startup
_event_bus: EventBus | None = None

def get_event_bus() -> EventBus:
    global _event_bus
    if _event_bus is None:
        # Fallback to in-memory if not initialized
        _event_bus = InMemoryEventBus()
    return _event_bus

def set_event_bus(bus: EventBus) -> None:
    global _event_bus
    _event_bus = bus
