/**
 * Bridge — typed access to the native module (Kotlin `MveBridge`).
 *
 * It backs three things the app needs from the device:
 *   • run(script)        — execute a shell script in the private sandbox
 *   • get/setPref(k,v)   — persist small settings in SharedPreferences
 *   • sandbox status     — is the shell set up
 *
 * When the native module isn't linked (e.g. JS-only dev), calls degrade to
 * harmless stubs so the UI still renders.
 */
import {NativeModules} from 'react-native';

interface NativeApi {
  run(command: string): Promise<string>;
  setupSandbox(): Promise<void>;
  sandboxStatus(): Promise<{installed: boolean; ready: boolean; working: boolean; statusText: string}>;
  getPref?(key: string): Promise<string>;
  setPref?(key: string, value: string): Promise<void>;
  killAll?(): Promise<void>;
}

const M: NativeApi | undefined = NativeModules.MveBridge;

export const Bridge = {
  isNative: M != null,

  run(script: string): Promise<string> {
    return M ? M.run(script) : Promise.resolve('[shell unavailable — native bridge not linked]');
  },
  setupSandbox(): Promise<void> {
    return M ? M.setupSandbox() : Promise.resolve();
  },
  sandboxStatus() {
    return M
      ? M.sandboxStatus()
      : Promise.resolve({installed: false, ready: false, working: false, statusText: 'no bridge'});
  },
  getPref(key: string): Promise<string> {
    return M && M.getPref ? M.getPref(key) : Promise.resolve('');
  },
  setPref(key: string, value: string): Promise<void> {
    return M && M.setPref ? M.setPref(key, value) : Promise.resolve();
  },
  /** Force-kill any running shell process. Part of the emergency stop. */
  killAll(): Promise<void> {
    return M && M.killAll ? M.killAll() : Promise.resolve();
  },
};
