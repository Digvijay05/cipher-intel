import asyncio
from app.services.agent import AgentController
from app.models.schemas import Message

async def run():
    ac = AgentController()
    msg = Message(sender="+123", text="URGENT: account suspended. call now https://link.com", timestamp=1000)
    await ac.process_message("test-124", msg, [])

if __name__ == "__main__":
    asyncio.run(run())
