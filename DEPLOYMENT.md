# Deployment Guide: Honeypot API with Docker + ngrok

Complete instructions for running the Honeypot API publicly using Docker and ngrok for hackathon evaluation.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Step-by-Step Setup](#step-by-step-setup)
4. [Obtaining Your Public URL](#obtaining-your-public-url)
5. [Verification Commands](#verification-commands)
6. [Common Failure Modes + Fixes](#common-failure-modes--fixes)
7. [Production Tips](#production-tips)

---

## Prerequisites

### Required Software

| Software | Version | Download |
|----------|---------|----------|
| Docker Desktop | 4.x+ | [docker.com/get-docker](https://docs.docker.com/get-docker/) |
| ngrok | 3.x+ | [ngrok.com/download](https://ngrok.com/download) |

### Required Accounts

| Service | Purpose | Link |
|---------|---------|------|
| OpenAI | GPT-4o-mini API | [platform.openai.com](https://platform.openai.com/api-keys) |
| ngrok | Free tunnel account | [dashboard.ngrok.com](https://dashboard.ngrok.com/signup) |

### API Keys Needed

```
HONEYPOT_API_KEY  = Your choice (strong random string)
OPENAI_API_KEY    = sk-... (from OpenAI dashboard)
NGROK_AUTHTOKEN   = (from ngrok dashboard, for auth)
```

---

## Quick Start

```powershell
# 1. Clone/navigate to project
cd AI_honeypot_Backendmain

# 2. Copy environment template
cp .env.example .env

# 3. Edit .env with your actual secrets
notepad .env   # Windows
# nano .env    # Linux/macOS

# 4. Authenticate ngrok (one-time)
ngrok config add-authtoken YOUR_AUTHTOKEN

# 5. Start everything (Windows)
.\scripts\start-with-ngrok.ps1

# 5. Start everything (Linux/macOS)
chmod +x scripts/start-with-ngrok.sh
./scripts/start-with-ngrok.sh
```

---

## Step-by-Step Setup

### Step 1: Install Docker

**Windows:**
1. Download [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/)
2. Run installer, enable WSL 2 if prompted
3. Start Docker Desktop
4. Verify: `docker --version`

**Linux:**
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# Log out and back in
docker --version
```

**macOS:**
1. Download [Docker Desktop for Mac](https://docs.docker.com/desktop/install/mac-install/)
2. Install and start
3. Verify: `docker --version`

### Step 2: Install ngrok

**Windows (with Chocolatey):**
```powershell
choco install ngrok
```

**Windows (manual):**
1. Download from [ngrok.com/download](https://ngrok.com/download)
2. Extract to a folder in your PATH
3. Verify: `ngrok version`

**Linux:**
```bash
curl -s https://ngrok-agent.s3.amazonaws.com/ngrok.asc | \
  sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null && \
  echo "deb https://ngrok-agent.s3.amazonaws.com buster main" | \
  sudo tee /etc/apt/sources.list.d/ngrok.list && \
  sudo apt update && sudo apt install ngrok
```

**macOS:**
```bash
brew install ngrok/ngrok/ngrok
```

### Step 3: Authenticate ngrok

1. Sign up at [dashboard.ngrok.com](https://dashboard.ngrok.com/signup)
2. Get your authtoken from [dashboard.ngrok.com/get-started/your-authtoken](https://dashboard.ngrok.com/get-started/your-authtoken)
3. Run:
   ```bash
   ngrok config add-authtoken YOUR_AUTHTOKEN_HERE
   ```

### Step 4: Configure Environment

```powershell
# Copy template
cp .env.example .env

# Edit with your secrets
notepad .env
```

Required values in `.env`:
```env
HONEYPOT_API_KEY=your-secure-random-api-key
OPENAI_API_KEY=sk-your-actual-openai-key
```

> **Tip:** Generate a secure API key:
> ```bash
> # PowerShell
> [guid]::NewGuid().ToString()
> 
> # Linux/macOS
> openssl rand -hex 32
> ```

### Step 5: Build and Start Containers

```powershell
# Build images and start containers
docker compose up --build -d

# View logs
docker compose logs -f
```

### Step 6: Verify Local Access

```powershell
# Health check (no auth)
curl http://localhost:8000/health
# Expected: {"status":"healthy"}

# API test (with auth)
curl -X POST http://localhost:8000/api/honeypot/test -H "x-api-key: YOUR_API_KEY"
# Expected: {"status":"ok","message":"Honeypot endpoint reachable"}
```

### Step 7: Start ngrok Tunnel

```powershell
# Start tunnel to port 8000
ngrok http 8000
```

ngrok will display output like:
```
Session Status                online
Account                       your-email (Plan: Free)
Forwarding                    https://abc123xyz.ngrok-free.app -> http://localhost:8000
```

**Copy the `https://....ngrok-free.app` URL** - this is your public endpoint.

---

## Obtaining Your Public URL

### Method 1: From ngrok Terminal

When ngrok starts, look for the `Forwarding` line:
```
Forwarding    https://abc123xyz.ngrok-free.app -> http://localhost:8000
```

### Method 2: ngrok Web Interface

1. Open browser to: [http://127.0.0.1:4040](http://127.0.0.1:4040)
2. URL is displayed on the main page

### Method 3: ngrok API

```powershell
curl -s http://127.0.0.1:4040/api/tunnels | ConvertFrom-Json | Select-Object -ExpandProperty tunnels | Select-Object public_url
```

### Method 4: Using a Static Domain (Recommended)

Free ngrok accounts can claim ONE free static domain:

1. Go to [dashboard.ngrok.com/cloud-edge/domains](https://dashboard.ngrok.com/cloud-edge/domains)
2. Click "New Domain" to get a free `*.ngrok-free.app` domain
3. Start ngrok with your domain:
   ```bash
   ngrok http 8000 --domain your-domain.ngrok-free.app
   ```

This gives you a **persistent URL** that doesn't change on restart.

---

## Verification Commands

### 1. Check Containers Are Running

```powershell
docker compose ps
```

Expected output:
```
NAME              STATUS                  PORTS
honeypot-api      Up X minutes (healthy)  0.0.0.0:8000->8000/tcp
honeypot-redis    Up X minutes (healthy)  0.0.0.0:6379->6379/tcp
```

### 2. Health Check (No Auth)

```powershell
# Local
curl http://localhost:8000/health

# Public (replace with your ngrok URL)
curl https://YOUR-URL.ngrok-free.app/health
```

Expected: `{"status":"healthy"}`

### 3. API Test Endpoint (With Auth)

```powershell
# Local
curl -X POST http://localhost:8000/api/honeypot/test -H "x-api-key: YOUR_API_KEY"

# Public
curl -X POST https://YOUR-URL.ngrok-free.app/api/honeypot/test -H "x-api-key: YOUR_API_KEY"
```

Expected: `{"status":"ok","message":"Honeypot endpoint reachable"}`

### 4. Full Message Test

```powershell
curl -X POST https://YOUR-URL.ngrok-free.app/api/honeypot/message `
  -H "Content-Type: application/json" `
  -H "x-api-key: YOUR_API_KEY" `
  -d '{
    "sessionId": "test-session-001",
    "message": {
      "sender": "scammer",
      "text": "Your bank account has been blocked! Send OTP now!",
      "timestamp": "2026-02-04T22:00:00Z"
    },
    "conversationHistory": []
  }'
```

Expected: `{"status":"success","reply":"..."}`  (LLM-generated response)

### 5. View Logs

```powershell
# All logs
docker compose logs -f

# Backend only
docker compose logs -f honeypot-backend

# Last 100 lines
docker compose logs --tail=100 honeypot-backend
```

---

## Common Failure Modes + Fixes

### ❌ Container exits immediately

**Symptom:** `docker compose ps` shows container exited

**Cause:** Missing or invalid environment variables

**Fix:**
```powershell
# Check logs for the error
docker compose logs honeypot-backend

# Verify .env has all required variables
cat .env | grep -E "HONEYPOT_API_KEY|OPENAI_API_KEY"
```

---

### ❌ "Invalid API Key" (401 error)

**Symptom:** API returns `{"detail":"Invalid API Key"}`

**Cause:** Incorrect `x-api-key` header value

**Fix:**
- Verify you're using the exact same key as in your `.env` file
- Ensure header name is lowercase: `x-api-key` (not `X-API-Key`)

---

### ❌ Cannot connect to Redis

**Symptom:** Logs show "Redis connection failed"

**Cause:** Redis container not healthy or network issue

**Fix:**
```powershell
# Check Redis status
docker compose ps redis

# Test Redis directly
docker compose exec redis redis-cli ping
# Expected: PONG

# Restart Redis
docker compose restart redis
```

---

### ❌ ngrok "connection refused"

**Symptom:** ngrok shows "failed to connect to localhost:8000"

**Cause:** Backend container not running or not on port 8000

**Fix:**
```powershell
# Verify backend is running
docker compose ps

# Verify port binding
curl http://localhost:8000/health

# If not running, restart
docker compose up -d
```

---

### ❌ ngrok URL not accessible externally

**Symptom:** Works locally but not from other devices/internet

**Cause:** Firewall, VPN, or ngrok authtoken issue

**Fix:**
- Disable VPN temporarily
- Ensure ngrok authtoken is set: `ngrok config check`
- Try a different ngrok region: `ngrok http 8000 --region us`

---

### ❌ LLM responses fail

**Symptom:** Logs show "OpenAI API error" or timeout

**Cause:** Invalid or expired OpenAI API key

**Fix:**
1. Verify key at [platform.openai.com/api-keys](https://platform.openai.com/api-keys)
2. Check you have API credits/billing set up
3. Test key directly:
   ```powershell
   curl https://api.openai.com/v1/models -H "Authorization: Bearer sk-YOUR-KEY"
   ```

---

### ❌ Callback to GUVI fails

**Symptom:** Logs show "Callback failed" or "Callback timeout"

**Cause:** Network issue reaching `hackathon.guvi.in`

**Fix:**
- Callback URL is hardcoded and cannot be changed
- Ensure outbound HTTPS (port 443) is allowed
- Check if GUVI endpoint is reachable:
  ```powershell
  curl -I https://hackathon.guvi.in
  ```

---

### ❌ Docker build fails

**Symptom:** `docker compose build` errors out

**Cause:** Usually Python package installation issue

**Fix:**
```powershell
# Clean build (no cache)
docker compose build --no-cache

# If still fails, check requirements.txt format
# Ensure no Windows line endings
```

---

## Production Tips

### 1. Use a Static ngrok Domain

Free accounts get one static domain. This prevents URL changes on restart.

### 2. Keep Docker Running

```powershell
# Start containers in background
docker compose up -d

# They will auto-restart on system reboot (unless-stopped policy)
```

### 3. Monitor Logs

```powershell
# Real-time monitoring
docker compose logs -f honeypot-backend

# Save to file
docker compose logs honeypot-backend > logs/backend.log
```

### 4. Check Resource Usage

```powershell
docker stats
```

### 5. Graceful Shutdown

```powershell
# Stop containers
docker compose down

# Stop and remove volumes (data loss!)
docker compose down -v
```

---

## File Reference

| File | Purpose |
|------|---------|
| `.env` | Your actual secrets (never commit) |
| `.env.example` | Template for environment variables |
| `Dockerfile` | Container build instructions |
| `docker-compose.yml` | Multi-container orchestration |
| `ngrok.yml.example` | ngrok configuration template |
| `scripts/start-with-ngrok.ps1` | Windows startup script |
| `scripts/start-with-ngrok.sh` | Linux/macOS startup script |

---

## Support

If you encounter issues not covered here:

1. Check container logs: `docker compose logs`
2. Verify all environment variables are set
3. Ensure Docker and ngrok are properly installed
4. Test each component in isolation (Redis, Backend, ngrok)
