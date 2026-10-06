# MVE — Mobile Agent


## Latest polish test APK

[Download the newest verified test APK — rolling release page](https://github.com/ether4o4/NeverSoft-Mobile-Desktop/releases/tag/polish-test-latest)

This separate prerelease channel contains **test APKs**, for **arm64-v8a** devices, with source commit, package/version, native ABI and SHA-256 recorded on the release page. Open **Download newest verified test APK** on that page. Phone runtime testing is incomplete; test signing may differ from an installed/store version. Private repository downloads require GitHub access.

While this PR remains unmerged, a successful **Polish verification** build on `polish/mobile-2026-10-05` refreshes the channel (remove/reapply the `polish-verify` PR label to run verification). The initial download reuses the already verified polish build. After merge, successful **Build Vista Launcher APK** builds on `main` also refresh it through `workflow_run`. Only trusted same-repository intended workflows/refs qualify; failed, older or diverging builds leave the working download intact. Versioned APK assets remain available, avoiding a replacement gap. Existing release channels keep their current behavior.

The stable link opens a release page; the highlighted APK filename changes after each accepted build. The channel tag anchors `main`; the release body identifies the actual APK source commit. These README additions and future `main` automation take effect on the default branch only after this PR is merged.


A standalone Android app: **AI chat on top, a live terminal shell on the bottom.**
You watch the agent think and work at the same time — it explains itself in the
chat while you see it run real commands in the terminal below.

No launcher, no desktop, no OS. Just the two things that matter: **the agent and
the shell**, in one installable app.

## Download

Every push to `main` publishes an installable (debug-signed) APK at a stable URL:

**→ https://github.com/ether4o4/NeverSoft-Mobile-Desktop/releases/download/android-preview/mve.apk**

Enable "Install unknown apps" to sideload. The link never changes; the file
behind it refreshes with each build.

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
in the sandbox, streams the command and output into the terminal, feeds the
result back to the model, and continues — until the task is done.

## Models: on-device or cloud

- **On-device** — pick a small GGUF model (Qwen2.5 3B, Llama 3.2 3B, Gemma 2 2B,
  Phi-3.5), download it from Hugging Face right in the app, and run it locally via
  **llama.rn** (llama.cpp). Private, offline, no key.
- **Cloud** — point it at any OpenAI-compatible endpoint (OpenAI, OpenRouter,
  DeepSeek, Groq, or your own gateway) with your own key. Streams live. Your key
  stays on the device.

Switch from the model chip in the app bar; manage downloads in Settings.

## The shell

Two modes, chosen automatically:

- **proot + Alpine Linux** — a real userland (apk, coreutils, busybox), installed
  on demand from Settings. Commands run as fake-root inside the rootfs.
- **toybox fallback** — Android's `/system/bin/sh` in a private sandbox. Always
  available: `ls, cat, grep, find, sed, awk, echo, wc, ps…`

The agent and UI never change between modes; installing Alpine just upgrades what
the shell can do. See [`android/app/src/main/jniLibs/README.md`](android/app/src/main/jniLibs/README.md)
for the one binary (`libproot.so`) that unlocks Alpine.

## Emergency stop

The red **STOP** button in the app bar is always live. One tap:

1. aborts the in-flight model request instantly,
2. stops on-device token generation,
3. **force-kills any running shell process** (native `destroyForcibly`),
4. halts the agent loop and returns control.

## Architecture

- `src/llm/*` — the `LlmProvider` interface. `openaiStream.ts` (cloud, SSE over
  XHR) and `localLlama.ts` (on-device, llama.rn) both implement it.
- `src/agent/*` — the loop tying the model to the shell, and the emergency-stop
  core.
- `src/native/bridge.ts` → Kotlin `MveBridge` + `SandboxManager` — the shell,
  model downloads, settings, and `killAll`.
- `src/ui/*` — split-screen panes, composer, settings.

## Build

React Native 0.81. CI builds a debug APK on every pull request
(`.github/workflows/build-apk.yml` → `assembleDebug`); grab it from the run's
Artifacts. Push to `main` also publishes a draft GitHub Release. Locally:

```bash
npm install --legacy-peer-deps
npm run android        # run on a device/emulator
npm run build:debug    # or build a debug APK
```
