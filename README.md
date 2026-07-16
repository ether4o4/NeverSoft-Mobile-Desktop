# MVE — Mobile Agent

A standalone Android app: **AI chat on top, a live terminal shell on the bottom.**
You watch the agent think and work at the same time — it explains itself in the
chat while you see it run real commands in the terminal below.

No launcher, no desktop, no OS. Just the two things that matter: **the agent and
the shell**, in one installable app.

## The split screen

```
┌─────────────────────────────┐
│  MVE agent      [STOP] [⚙]   │  app bar — emergency stop is always here
├─────────────────────────────┤
│  chat (you ⇄ the agent)      │  streams token by token
│  ─── drag to resize ───      │
│  shell (what it's running)   │  live command + output
├─────────────────────────────┤
│  Ask the agent to do…    [↑] │
└─────────────────────────────┘
```

When the agent decides to run something, it emits a shell block; the app runs it
in the sandbox and streams the command and output into the terminal, then feeds
the result back to the model and continues — until the task is done.

## Models: on-device or cloud

- **Cloud** — point it at any OpenAI-compatible endpoint (OpenAI, OpenRouter,
  DeepSeek, Groq, or your own gateway) with your own key. Streams live. Your key
  is stored only on the device.
- **On-device** — pick a small GGUF model (Qwen2.5 3B, Llama 3.2 3B, Gemma 2 2B,
  Phi-3.5) pulled from Hugging Face and run locally via llama.cpp. Private,
  offline. *(Wired end-to-end; the native inference engine lands in the engine
  build — see Status.)*

Switch providers from the model chip in the app bar.

## Emergency stop

The red **STOP** button in the app bar is always live. One tap:

1. aborts the in-flight model request instantly,
2. stops on-device token generation,
3. **force-kills any running shell process** (native `destroyForcibly`),
4. halts the agent loop and returns control.

## Architecture

The UI is provider- and shell-agnostic. Everything meets at two seams:

- `src/llm/*` — the `LlmProvider` interface. `openaiStream.ts` (cloud, SSE over
  XHR) and `localLlama.ts` (on-device, llama.rn) both implement it.
- `src/native/bridge.ts` → Kotlin `MveBridge` — runs shell scripts in a private
  sandbox, persists settings, and exposes the `killAll` used by the stop button.
- `src/agent/agent.ts` — the loop that ties the model to the shell.

## Status

**Milestone 1 (this build) — the app runs and is buildable:**
split-screen UI, cloud chat with live streaming, the on-device shell + agent
loop, settings/keys, and the emergency stop. The shell currently runs in the
app's private sandbox via `/system/bin/sh`.

**Milestone 2 — the engine build:**
add `llama.rn` (+ NDK config) so on-device GGUF models actually run locally, and
upgrade the sandbox from bare `sh` to a real **proot + Alpine** rootfs (package
manager, coreutils). The provider/shell seams already match, so these drop in
without touching the UI.

## Build

React Native 0.81. CI builds a debug APK on every pull request
(`.github/workflows/build-apk.yml` → `assembleDebug`); grab it from the run's
Artifacts. Locally:

```bash
npm install --legacy-peer-deps
npm run android        # run on a device/emulator
npm run build:debug    # or build a debug APK
```
