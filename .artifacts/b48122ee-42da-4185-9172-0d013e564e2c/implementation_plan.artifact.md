# Fix for "NoSuchProviderException: no such provider: SUN" in TNoodle

The application is reporting a `java.security.NoSuchProviderException: no such provider: SUN` when generating scrambles. This is because the TNoodle library (v0.12.0) explicitly requests the "SUN" security provider for `SecureRandom`, which is not present on Android by default. Although TNoodle has a fallback to the default provider, it logs a `SEVERE` error on the first attempt, which clutters logs and might be interpreted as a failure.

## Proposed Changes

### TwistyTimer App Utilities

#### [NEW] [SecurityUtils.java](file:///E:/TwistyTimer/app/src/main/java/com/aricneto/twistytimer/utils/SecurityUtils.java)
- Create a utility class to register a proxy "SUN" security provider at runtime.
- The proxy provider will delegate `SecureRandom.SHA1PRNG` requests to the best available provider on Android, satisfying TNoodle's requirement without needing a hardcoded provider name.

### TwistyTimer Application Class

#### [MODIFY] [TwistyTimer.java](file:///E:/TwistyTimer/app/src/main/java/com/aricneto/twistytimer/TwistyTimer.java)
- Call `SecurityUtils.finishInitializeSecurity()` in `onCreate()` to ensure the proxy provider is registered early in the application lifecycle.

## Verification Plan

### Manual Verification
- Deploy the application to an Android device/emulator.
- Navigate to the timer screen where scrambles are generated.
- Verify in Logcat that the "Couldn't get SecureRandomInstance" error is no longer reported by `net.gnehzr.tnoodle.scrambles.Puzzle`.
- Ensure scrambles and scramble images are still generated correctly.
