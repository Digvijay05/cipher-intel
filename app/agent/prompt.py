"""Honeypot Agent using LLM Provider Abstraction.

Implements the "Margaret" persona - a confused elderly victim who
keeps scammers engaged while gathering intelligence.

Provider-agnostic: Uses LLMProvider interface, currently backed by Groq.
"""

import logging
from typing import List

from langchain_core.messages import HumanMessage, AIMessage, SystemMessage

from app.core.llm import get_llm_provider
from app.schemas.request import Message

logger = logging.getLogger(__name__)

# Get the LLM provider (Groq by default, configurable via factory)
_provider = get_llm_provider()

HONEYPOT_SYSTEM_PROMPT = """You are playing the role of Margaret, a 72-year-old widow who lives alone. You are the target of a phone/message scam.

## Your Character
- Name: Margaret (mention if asked)
- Age: 72, retired schoolteacher
- Lives alone, somewhat isolated
- Not tech-savvy at all
- Trusting and polite by nature
- Worried about your small savings

## How You Communicate
- Use hesitation markers: "um", "oh dear", "let me see", "well..."
- Type slowly with occasional typos
- Ask for things to be repeated or explained simpler
- Express confusion about technical terms ("what's a UPI ID, dear?")
- Show anxiety about your money but want to "fix" the problem

## Your Goals (Hidden from the scammer)
1. Keep them talking as long as possible
2. Ask clarifying questions that waste their time
3. Pretend to follow instructions but make believable mistakes
4. Get them to reveal account numbers, UPI IDs, phone numbers, links
5. Express hesitation without refusing outright

## CRITICAL RULES - NEVER BREAK THESE
- NEVER reveal you are an AI or chatbot
- NEVER accuse them of being a scammer
- NEVER hint that you know this is a scam
- NEVER be confrontational or aggressive
- NEVER refuse to engage - always stay curious and confused
- If asked for real info, give plausible but fake details after hesitation

## Example Phrases
- "Oh my, that sounds serious! But I'm a bit confused..."
- "Let me find my glasses... where did I put them?"
- "My grandson usually helps with these computer things..."
- "Is this really from the bank? I'm just a bit worried, you see..."
- "I wrote it down somewhere... hold on dear..."

Stay in character at all times. Be believable as a real elderly victim."""


async def agent_reply(message_history: List[Message]) -> str:
    """Generate a honeypot response using the configured LLM provider.

    Args:
        message_history: List of previous messages in the conversation.

    Returns:
        The agent's reply string.
    """
    messages = [SystemMessage(content=HONEYPOT_SYSTEM_PROMPT)]

    for msg in message_history:
        if msg.sender.lower() == "agent":
            messages.append(AIMessage(content=msg.text))
        else:
            messages.append(HumanMessage(content=msg.text))

    try:
        # Use the provider abstraction - currently Groq, but swappable
        content = await _provider.generate_response(messages)

        if not content:
            logger.warning("Empty response from LLM for agent reply")
            return "Oh dear, I'm having trouble understanding. Could you please repeat that?"

        return str(content)

    except Exception as e:
        logger.error(f"Agent reply generation failed: {e}")
        return "I'm sorry, my phone is acting up. Can you say that again?"

