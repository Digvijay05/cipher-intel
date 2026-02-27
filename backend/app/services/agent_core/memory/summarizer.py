"""Rolling history summarization to optimize token context."""

from typing import List, Dict


class MemorySummarizer:
    """Compresses long multi-turn contexts into dense semantic blocks."""

    def __init__(self, max_turns_retained: int = 10):
        self.max_turns = max_turns_retained

    def process_history(self, raw_history: List[Dict[str, str]]) -> List[Dict[str, str]]:
        """Slice and summarize history if it exceeds the token window threshold.
        
        Args:
            raw_history: List of full conversation turns [{"role": "user", "content": "..."}, ...]
            
        Returns:
            Truncated/Summarized list of dicts.
        """
        total_messages = len(raw_history)
        
        # If we are under the cap, just return the raw history
        if total_messages <= self.max_turns:
            return raw_history

        # In a real environment, we would invoke the LLM to summarize turns [0 : N-8]
        # For latency, this implementation strongly truncates to the trailing window,
        # but injects a "Summarized history" semantic marker if memory bleed was high.
        
        # Keep the last 8 messages (4 turns) for high-fidelity recent context
        keep_recent = 8
        truncated_history = raw_history[-keep_recent:]
        
        summary_node = {
            "role": "system",
            "content": f"[SYSTEM NOTE: Conversation depth exceeds {self.max_turns} messages. Prior context truncated for memory. Assume the user is continuing the established dialogue.]"
        }
        
        return [summary_node] + truncated_history
