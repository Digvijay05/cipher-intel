"""Agent package.

Exports agent_reply function and controller.
"""

from app.agent.prompt import agent_reply
from app.agent.controller import AgentController, get_controller

# Shim for testing - allows unittest.mock.patch("app.agent.client") to work
client = None

__all__ = ["agent_reply", "AgentController", "get_controller"]
