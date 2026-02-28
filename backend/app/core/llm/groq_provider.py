"""Groq LLM Provider implementation.

Uses langchain-groq to interface with Groq's high-speed inference API.
All configuration sourced from centralized settings module.
"""

import logging
from typing import List

from langchain_groq import ChatGroq
from langchain_core.messages import BaseMessage

from app.core.llm.base import LLMProvider, LLMProviderError
from app.config.settings import settings

logger = logging.getLogger(__name__)


class GroqProvider(LLMProvider):
    """LLM provider implementation using Groq API via langchain-groq."""

    def __init__(
        self,
        api_key: str = None,
        model: str = None,
        temperature: float = None,
        max_tokens: int = None,
    ):
        """Initialize the Groq provider.

        All defaults sourced from centralized settings.

        Args:
            api_key: Groq API key.
            model: Model identifier.
            temperature: Default sampling temperature.
            max_tokens: Default max tokens for responses.
        """
        self._api_key = api_key or settings.GROQ_API_KEY
        self._model = model or settings.GROQ_MODEL
        self._default_temperature = temperature if temperature is not None else settings.LLM_DEFAULT_TEMPERATURE
        self._default_max_tokens = max_tokens if max_tokens is not None else settings.LLM_DEFAULT_MAX_TOKENS

        self._chat = ChatGroq(
            model=self._model,
            api_key=self._api_key,
            temperature=self._default_temperature,
            max_tokens=self._default_max_tokens,
        )

        logger.info(f"GroqProvider initialized with model: {self._model}")

    @property
    def model_name(self) -> str:
        """Return the Groq model identifier."""
        return self._model

    async def generate_response(
        self,
        messages: List[BaseMessage],
        temperature: float = None,
        max_tokens: int = None,
    ) -> str:
        """Generate a response using Groq's API.

        Args:
            messages: List of LangChain message objects.
            temperature: Override default temperature if provided.
            max_tokens: Override default max_tokens if provided.

        Returns:
            The generated response text.

        Raises:
            LLMProviderError: If Groq API call fails.
        """
        effective_temp = temperature if temperature is not None else self._default_temperature
        effective_max = max_tokens if max_tokens is not None else self._default_max_tokens

        if effective_temp != self._default_temperature or effective_max != self._default_max_tokens:
            chat = ChatGroq(
                model=self._model,
                api_key=self._api_key,
                temperature=effective_temp,
                max_tokens=effective_max,
            )
        else:
            chat = self._chat

        try:
            response = await chat.ainvoke(messages)
            content = response.content

            if not content:
                logger.warning("Empty response from Groq LLM")
                return ""

            return str(content)

        except Exception as e:
            logger.error(f"Groq API call failed: {e}")
            raise LLMProviderError(
                message=f"Groq generation failed: {str(e)}",
                provider="groq",
                original_error=e,
            )
