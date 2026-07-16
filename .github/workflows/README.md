# GitHub Actions Setup for Vista Launcher

## Automatic APK Builds

This project includes GitHub Actions that automatically build APKs on every push to `main` or `develop` branches.

### How it works:
1. Push code to GitHub
2. GitHub Actions automatically builds the APK
3. Download the APK from the Actions artifacts or Releases

### Manual Trigger:
Go to **Actions → Build Vista Launcher APK → Run workflow** and choose:
- `debug` - Fast build, unsigned
- `release` - Optimized build, signed (requires secrets)

### Setup for Release Signing (Optional):
To create signed release builds, add these secrets to your GitHub repository:

1. **RELEASE_KEYSTORE_BASE64** - Your keystore file encoded as base64:
   ```bash
   base64 -i android/app/release.keystore | pbcopy
   ```

2. **KEYSTORE_PASSWORD** - Keystore password

3. **KEY_ALIAS** - Key alias name

4. **KEY_PASSWORD** - Key password

Without these secrets, release builds will fall back to debug builds.

### Downloading the APK:

#### From Artifacts:
1. Go to the completed workflow run
2. Scroll down to "Artifacts"
3. Download `vista-launcher-debug` or `vista-launcher-release`

#### From Releases:
- Tagged releases are automatically created as drafts
- Go to **Releases** in your GitHub repo
- Download the APK from the latest release

### Build Outputs:
- Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `android/app/build/outputs/apk/release/app-release.apk`
