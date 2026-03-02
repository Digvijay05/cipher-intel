import asyncio
from app.services.agent import AgentController
from app.models.schemas import Message

async def run():
    ac = AgentController()
    msg = Message(sender="+123", text="URGENT: account suspended. call now https://link.com", timestamp=1000)
    try:
        await ac.process_message("test-124", msg, [])
    except Exception as e:
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(run())
