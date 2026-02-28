"""Ollama Cloud LLM Provider implementation.

Uses Ollama Cloud API for remote inference.
All configuration sourced from centralized settings module.
"""

import asyncio
import logging
import time
from typing import List, Dict

import httpx
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage, SystemMessage

from app.core.llm.base import LLMProvider, LLMProviderError
from app.config.settings import settings

logger = logging.getLogger(__name__)


def _convert_messages(messages: List[BaseMessage]) -> List[Dict[str, str]]:
    """Convert LangChain messages to Ollama chat format."""
    result = []
    for msg in messages:
        if isinstance(msg, SystemMessage):
            result.append({"role": "system", "content": msg.content})
        elif isinstance(msg, HumanMessage):
            result.append({"role": "user", "content": msg.content})
        elif isinstance(msg, AIMessage):
            result.append({"role": "assistant", "content": msg.content})
        else:
            result.append({"role": "user", "content": str(msg.content)})
    return result


class OllamaProvider(LLMProvider):
    """LLM provider implementation using Ollama Cloud API."""

    def __init__(
        self,
        api_key: str = None,
        model: str = None,
        base_url: str = None,
        temperature: float = None,
        max_tokens: int = None,
    ):
        """Initialize the Ollama Cloud provider.

        All defaults sourced from centralized settings.

        Args:
            api_key: Ollama API key.
            model: Model identifier.
            base_url: Ollama Cloud base URL.
            temperature: Default sampling temperature.
            max_tokens: Default max tokens for responses.
        """
        self._api_key = api_key or settings.OLLAMA_API_KEY
        self._model = model or settings.OLLAMA_MODEL
        self._base_url = (base_url or settings.OLLAMA_BASE_URL).rstrip("/")
        self._default_temperature = temperature if temperature is not None else settings.LLM_DEFAULT_TEMPERATURE
        self._default_max_tokens = max_tokens if max_tokens is not None else settings.LLM_DEFAULT_MAX_TOKENS
        self._chat_endpoint = f"{self._base_url}/api/chat"

        logger.info(f"OllamaProvider initialized: model={self._model}, endpoint={self._chat_endpoint}")

    @property
    def model_name(self) -> str:
        """Return the Ollama model identifier."""
        return self._model

    async def generate_response(
        self,
        messages: List[BaseMessage],
        temperature: float = None,
        max_tokens: int = None,
    ) -> str:
        """Generate a response using Ollama Cloud API.

        Args:
            messages: List of LangChain message objects.
            temperature: Override default temperature if provided.
            max_tokens: Override default max_tokens if provided.

        Returns:
            The generated response text.

        Raises:
            LLMProviderError: If Ollama API call fails after retries.
        """
        effective_temp = temperature if temperature is not None else self._default_temperature
        effective_max = max_tokens if max_tokens is not None else self._default_max_tokens

        ollama_messages = _convert_messages(messages)

        payload = {
            "model": self._model,
            "messages": ollama_messages,
            "stream": False,
            "options": {
                "temperature": effective_temp,
                "num_predict": effective_max,
            },
        }

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self._api_key}",
        }

        last_error = None
        for attempt in range(settings.LLM_MAX_RETRIES):
            try:
                start_time = time.time()

                async with httpx.AsyncClient(timeout=settings.LLM_REQUEST_TIMEOUT_SECONDS) as client:
                    response = await client.post(
                        self._chat_endpoint,
                        json=payload,
                        headers=headers,
                    )

                elapsed_ms = (time.time() - start_time) * 1000
                logger.debug(f"Ollama request completed in {elapsed_ms:.0f}ms, status={response.status_code}")

                if response.status_code == 200:
                    data = response.json()
                    content = data.get("message", {}).get("content", "")
                    if not content:
                        logger.warning("Empty response from Ollama Cloud")
                        return ""
                    return str(content)

                elif response.status_code == 401:
                    raise LLMProviderError(
                        message="Ollama Cloud authentication failed. Check OLLAMA_API_KEY.",
                        provider="ollama",
                    )
                elif response.status_code == 429:
                    logger.warning(
                        f"Ollama rate limited, retrying in "
                        f"{settings.LLM_RETRY_DELAY_SECONDS * (attempt + 1)}s"
                    )
                    await asyncio.sleep(settings.LLM_RETRY_DELAY_SECONDS * (attempt + 1))
                    continue
                else:
                    error_text = response.text[:200]
                    last_error = LLMProviderError(
                        message=f"Ollama API error {response.status_code}: {error_text}",
                        provider="ollama",
                    )
                    logger.warning(f"Ollama request failed: {response.status_code}")

            except httpx.TimeoutException as e:
                last_error = LLMProviderError(
                    message=f"Ollama request timed out after {settings.LLM_REQUEST_TIMEOUT_SECONDS}s",
                    provider="ollama",
                    original_error=e,
                )
                logger.warning(f"Ollama timeout on attempt {attempt + 1}")
            except httpx.RequestError as e:
                last_error = LLMProviderError(
                    message=f"Ollama network error: {str(e)}",
                    provider="ollama",
                    original_error=e,
                )
                logger.warning(f"Ollama network error on attempt {attempt + 1}: {e}")

            if attempt < settings.LLM_MAX_RETRIES - 1:
                await asyncio.sleep(settings.LLM_RETRY_DELAY_SECONDS * (attempt + 1))

        logger.error(f"Ollama API call failed after {settings.LLM_MAX_RETRIES} attempts")
        raise last_error or LLMProviderError(
            message="Ollama API call failed after retries",
            provider="ollama",
        )
