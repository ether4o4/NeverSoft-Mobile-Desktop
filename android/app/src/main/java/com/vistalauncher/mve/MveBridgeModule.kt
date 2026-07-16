package com.vistalauncher.mve

import android.content.Context
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * MveBridgeModule — the native (Kotlin) endpoint of the JS `MveBridge`.
 *
 * This is the real implementation that backs `NativeModules.MveBridge`, so the
 * shell stops running on the JS mock once the app is built. Everything that can
 * be done on-device today is done for real here:
 *
 *   • Sandbox terminal  — commands run through `/system/bin/sh` rooted in the
 *     app's private sandbox dir; file list/read/write/search hit that tree.
 *   • Providers / keys   — persisted in SharedPreferences (user-supplied keys,
 *     privacy-first: nothing leaves the device except the chat call itself).
 *   • Chat               — a real OpenAI-compatible `/chat/completions` call to
 *     whichever provider the user enabled with a key.
 *
 * The MorsVitaEst engine (the `MveEngine` Kotlin facade) is the seam: once it is
 * vendored/linked into this build, `sendMessage` and the provider/memory calls
 * delegate to it instead of the inline implementations below. Method names and
 * return shapes already match the engine facade so that swap is mechanical.
 */
class MveBridgeModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

  private val io = Executors.newSingleThreadExecutor()
  private val prefs = reactContext.getSharedPreferences("mve_engine", Context.MODE_PRIVATE)
  private val history = mutableListOf<Pair<String, String>>()

  override fun getName(): String = "MveBridge"

  // ---- Providers -----------------------------------------------------------

  private data class ProviderDef(
      val id: String,
      val displayName: String,
      val baseUrl: String,
      val model: String,
  )

  private val providers = listOf(
      ProviderDef("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", "openrouter/auto"),
      ProviderDef("deepseek", "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
      ProviderDef("openai", "OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
  )

  private fun keyOf(id: String): String = prefs.getString("key_$id", "") ?: ""
  private fun enabledOf(id: String): Boolean = prefs.getBoolean("enabled_$id", false)

  @ReactMethod
  fun services(promise: Promise) = io.execute {
    try {
      val arr = Arguments.createArray()
      providers.forEach { p ->
        val m = Arguments.createMap()
        m.putString("instanceId", p.id)
        m.putString("serviceId", p.id)
        m.putString("displayName", p.displayName)
        m.putBoolean("enabled", enabledOf(p.id))
        arr.pushMap(m)
      }
      promise.resolve(arr)
    } catch (e: Exception) {
      promise.reject("mve_error", e)
    }
  }

  @ReactMethod
  fun getApiKey(instanceId: String, promise: Promise) = io.execute {
    promise.resolve(keyOf(instanceId))
  }

  @ReactMethod
  fun setApiKey(instanceId: String, apiKey: String, promise: Promise) = io.execute {
    prefs.edit().putString("key_$instanceId", apiKey).apply()
    promise.resolve(null)
  }

  @ReactMethod
  fun setServiceEnabled(instanceId: String, enabled: Boolean, promise: Promise) = io.execute {
    prefs.edit().putBoolean("enabled_$instanceId", enabled).apply()
    promise.resolve(null)
  }

  // ---- Chat ----------------------------------------------------------------

  @ReactMethod
  fun sendMessage(text: String, promise: Promise) = io.execute {
    history.add("user" to text)
    val provider = providers.firstOrNull { enabledOf(it.id) && keyOf(it.id).isNotBlank() }
    if (provider == null) {
      val msg =
          "No provider is enabled yet. Open MVE → Settings, add an API key and enable a provider."
      history.add("assistant" to msg)
      promise.resolve(msg)
      return@execute
    }
    val reply =
        try {
          chatCompletion(provider)
        } catch (e: Exception) {
          "Request failed: ${e.message}"
        }
    history.add("assistant" to reply)
    promise.resolve(reply)
  }

  private fun chatCompletion(provider: ProviderDef): String {
    val url = URL(provider.baseUrl.trimEnd('/') + "/chat/completions")
    val conn = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 20000
      readTimeout = 45000
      doOutput = true
      setRequestProperty("Content-Type", "application/json")
      setRequestProperty("Authorization", "Bearer ${keyOf(provider.id)}")
    }
    val body = JSONObject().apply {
      put("model", provider.model)
      put("messages", JSONArray().apply {
        history.forEach { (role, content) ->
          put(JSONObject().put("role", role).put("content", content))
        }
      })
    }
    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

    val code = conn.responseCode
    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
    val resp = stream?.bufferedReader()?.use { it.readText() } ?: ""
    if (code !in 200..299) return "Provider error $code: ${resp.take(300)}"

    val content = JSONObject(resp)
        .optJSONArray("choices")
        ?.optJSONObject(0)
        ?.optJSONObject("message")
        ?.optString("content")
    return content?.takeIf { it.isNotEmpty() } ?: "No content in provider response."
  }

  @ReactMethod
  fun getHistory(promise: Promise) = io.execute {
    val arr = Arguments.createArray()
    history.forEach { (role, content) ->
      arr.pushMap(Arguments.createMap().apply {
        putString("role", role)
        putString("content", content)
      })
    }
    promise.resolve(arr)
  }

  @ReactMethod
  fun startNewChat(promise: Promise) = io.execute {
    history.clear()
    promise.resolve(null)
  }

  @ReactMethod
  fun clearChat(promise: Promise) = io.execute {
    history.clear()
    promise.resolve(null)
  }

  // ---- Core toggles --------------------------------------------------------

  @ReactMethod
  fun isSandboxEnabled(promise: Promise) =
      promise.resolve(prefs.getBoolean("sandbox_enabled", true))

  @ReactMethod
  fun setSandboxEnabled(enabled: Boolean, promise: Promise) {
    prefs.edit().putBoolean("sandbox_enabled", enabled).apply()
    promise.resolve(null)
  }

  @ReactMethod
  fun isDaemonEnabled(promise: Promise) =
      promise.resolve(prefs.getBoolean("daemon_enabled", false))

  @ReactMethod
  fun setDaemonEnabled(enabled: Boolean, promise: Promise) {
    prefs.edit().putBoolean("daemon_enabled", enabled).apply()
    promise.resolve(null)
  }

  // ---- Sandbox: terminal + files ------------------------------------------

  private fun sandboxRoot(): File =
      File(reactContext.filesDir, "mve-sandbox").apply { if (!exists()) mkdirs() }

  private fun resolvePath(path: String): File = File(sandboxRoot(), path.trimStart('/'))

  private fun relPath(f: File): String = "/" + f.relativeTo(sandboxRoot()).path

  // Tracks the shell process currently running so the emergency stop can kill it.
  @Volatile private var current: Process? = null

  private fun execShell(command: String, dir: File): String {
    val proc = ProcessBuilder("/system/bin/sh", "-c", command)
        .directory(dir)
        .redirectErrorStream(true)
        .start()
    current = proc
    val out =
        try {
          proc.inputStream.bufferedReader().use { it.readText() }
        } finally {
          try {
            proc.waitFor()
          } catch (_: Exception) {}
          current = null
        }
    return out.trimEnd()
  }

  /** Emergency stop: force-kill any running shell process this instant. */
  @ReactMethod
  fun killAll(promise: Promise) {
    try {
      current?.destroyForcibly()
    } catch (_: Exception) {}
    current = null
    promise.resolve(null)
  }

  // ---- Generic settings persistence ---------------------------------------

  @ReactMethod
  fun getPref(key: String, promise: Promise) = io.execute {
    promise.resolve(prefs.getString("pref_$key", "") ?: "")
  }

  @ReactMethod
  fun setPref(key: String, value: String, promise: Promise) = io.execute {
    prefs.edit().putString("pref_$key", value).apply()
    promise.resolve(null)
  }

  @ReactMethod
  fun sandboxStatus(promise: Promise) = io.execute {
    val installed = File(sandboxRoot(), "root").exists()
    val enabled = prefs.getBoolean("sandbox_enabled", true)
    promise.resolve(Arguments.createMap().apply {
      putBoolean("installed", installed)
      putBoolean("ready", installed && enabled)
      putBoolean("working", installed && enabled)
      putString(
          "statusText",
          when {
            !enabled -> "Sandbox disabled"
            installed -> "Sandbox ready"
            else -> "Sandbox not set up — tap Setup"
          },
      )
    })
  }

  @ReactMethod
  fun setupSandbox(promise: Promise) = io.execute {
    try {
      val home = File(sandboxRoot(), "root").apply { mkdirs() }
      File(home, "notes.txt").writeText("Welcome to the MVE Linux sandbox.\n")
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("mve_error", e)
    }
  }

  @ReactMethod
  fun run(command: String, promise: Promise) = io.execute {
    try {
      promise.resolve(execShell(command, sandboxRoot()))
    } catch (e: Exception) {
      promise.resolve("error: ${e.message}")
    }
  }

  @ReactMethod
  fun listDir(path: String, promise: Promise) = io.execute {
    try {
      val arr = Arguments.createArray()
      resolvePath(path).listFiles()?.sortedBy { it.name }?.forEach { f ->
        arr.pushMap(Arguments.createMap().apply {
          putString("name", f.name)
          putString("path", relPath(f))
          putBoolean("isDirectory", f.isDirectory)
          putDouble("sizeBytes", f.length().toDouble())
          putDouble("lastModifiedMs", f.lastModified().toDouble())
        })
      }
      promise.resolve(arr)
    } catch (e: Exception) {
      promise.reject("mve_error", e)
    }
  }

  @ReactMethod
  fun readFile(path: String, promise: Promise) = io.execute {
    val f = resolvePath(path)
    promise.resolve(if (f.isFile) f.readText() else null)
  }

  @ReactMethod
  fun writeFile(path: String, content: String, promise: Promise) = io.execute {
    try {
      val f = resolvePath(path)
      f.parentFile?.mkdirs()
      f.writeText(content)
      promise.resolve(true)
    } catch (e: Exception) {
      promise.resolve(false)
    }
  }

  @ReactMethod
  fun searchFilenames(root: String, keyword: String, promise: Promise) = io.execute {
    val arr = Arguments.createArray()
    val base = resolvePath(root)
    if (base.exists()) {
      base.walkTopDown().forEach { f ->
        if (f.isFile && f.name.contains(keyword, ignoreCase = true)) {
          arr.pushString(relPath(f))
        }
      }
    }
    promise.resolve(arr)
  }

  @ReactMethod
  fun writeIfValid(path: String, content: String, validateCommand: String, promise: Promise) =
      io.execute {
        try {
          val f = resolvePath(path)
          f.parentFile?.mkdirs()
          f.writeText(content)
          val output =
              if (validateCommand.isBlank()) "validation skipped"
              else execShell(validateCommand, sandboxRoot())
          promise.resolve(Arguments.createMap().apply {
            putBoolean("saved", true)
            putString("output", output)
          })
        } catch (e: Exception) {
          promise.resolve(Arguments.createMap().apply {
            putBoolean("saved", false)
            putString("output", "error: ${e.message}")
          })
        }
      }
}
