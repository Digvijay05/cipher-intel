"""Locust load testing for CIPHER Honeypot API.

Simulates hundreds of concurrent scammer bots hitting the message endpoint
to validate circuit breaker behavior, rate limiting, and Cloud Ollama latency.

Usage:
    locust -f tests/locustfile.py --host=http://localhost:8000
"""

import os
import random
import time

from locust import HttpUser, between, events, task

CIPHER_API_KEY = os.environ.get("CIPHER_API_KEY", "test-key")


class ScammerBot(HttpUser):
    """Simulates a scammer sending messages to the honeypot."""

    wait_time = between(1, 5)

    def on_start(self):
        self.session_id = f"sim_{random.randint(1000, 9999)}"
        self.scammer_number = f"+1555{random.randint(100000, 999999)}"
        self.scenarios = [
            "Hello is this John? Your Amazon delivery failed.",
            "IRS WARNING: Pay immediately or warrant will be issued.",
            "Hey I found your number in my old phone, how have you been?",
            "URGENT: Your bank account is locked. Click here to verify.",
            "Congratulations! You won a $1000 gift card. Claim now.",
        ]

    @task(3)
    def initiate_scam(self):
        payload = {
            "sessionId": self.session_id,
            "message": {
                "sender": "scammer",
                "text": random.choice(self.scenarios),
                "timestamp": int(time.time() * 1000),
            },
            "conversationHistory": [],
        }

        with self.client.post(
            "/api/honeypot/message",
            json=payload,
            headers={"x-api-key": CIPHER_API_KEY},
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    if "reply" not in data:
                        response.failure("Malformed response: missing 'reply'")
                    else:
                        response.success()
                except ValueError:
                    response.failure("Invalid JSON returned")
            elif response.status_code == 503:
                events.request.fire(
                    request_type="CircuitBreaker",
                    name="CloudOllamaOffline",
                    response_time=response.elapsed.total_seconds() * 1000,
                    response_length=len(response.content),
                    exception=None,
                )

    @task(1)
    def fuzz_malformed_payload(self):
        """Fuzzing: send garbage to ensure the server rejects gracefully."""
        payload = {"message": None, "malicious_sql": "DROP TABLE sessions"}
        self.client.post(
            "/api/honeypot/message",
            headers={"x-api-key": CIPHER_API_KEY},
            json=payload,
            expected_status=422,
        )
