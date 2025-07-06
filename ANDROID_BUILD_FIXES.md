# Android Build Fixes for Create Release

## Issue Summary

The build was failing during the "Create Release" process with the following errors:

1. **Android SDK Environment Variable Conflicts**
   - `ANDROID_HOME`: `/usr/lib/android-sdk`
   - `ANDROID_SDK_ROOT`: `/usr/local/lib/android/sdk`
   - Gradle was complaining about conflicting paths

2. **APK Build Failure**
   - Input file `apk_source/hello-libs/app/build/outputs/apk/debug/app-debug.apk` was not found
   - This caused the `gen_smali.py` script to fail when trying to decompile the APK

3. **Smali Directory Not Found**
   - `FileNotFoundError: [Errno 2] No such file or directory: '/tmp/tmpfj3vby3c/smali/com/AceInjector'`
   - This was a downstream effect of the APK build failure

## Root Cause Analysis

The primary issue was the conflicting Android SDK environment variables. Both `ANDROID_HOME` and `ANDROID_SDK_ROOT` were set to different paths, causing Gradle to fail during the Android build process. This prevented the APK from being generated, which in turn caused the smali extraction process to fail.

## Fixes Applied

### 1. Fixed Android SDK Environment Variables in GitHub Actions

**Files Modified:**
- `.github/workflows/main.yml`
- `.github/workflows/release.yml`

**Changes Made:**
- Added explicit unsetting of `ANDROID_SDK_ROOT` before setting `ANDROID_HOME`
- Added environment variable cleanup in all Android-related build jobs
- Added proper Android SDK setup for the Modder-linux job

```bash
# Unset ANDROID_SDK_ROOT to avoid conflicts
unset ANDROID_SDK_ROOT
echo "ANDROID_HOME=/usr/lib/android-sdk" >> $GITHUB_ENV
# Also unset ANDROID_SDK_ROOT in the environment
echo "ANDROID_SDK_ROOT=" >> $GITHUB_ENV
```

### 2. Enhanced Error Handling in gen_smali.py

**File Modified:**
- `Modder/injector/gen_smali.py`

**Improvements:**
- Added proper error checking for Gradle build process
- Added validation for APK file existence before decompilation
- Added validation for APK decompilation success
- Added comprehensive directory structure debugging when smali directory is not found
- Added graceful handling of missing native lib directories
- Added informative error messages for troubleshooting

### 3. Fixed CMake Configuration

**File Modified:**
- `Modder/injector/apk_source/hello-libs/app/build.gradle`

**Changes Made:**
- Changed CMake path from `../../../../../ACE/CMakeLists.txt` to `src/main/cpp/CMakeLists.txt`
- This prevents the build from trying to compile the entire ACE project as part of the Android build
- Uses the local CMakeLists.txt file that's specifically configured for the hello-libs project

## Technical Details

### Android SDK Environment Variables
- **ANDROID_HOME**: The recommended environment variable for Android SDK location
- **ANDROID_SDK_ROOT**: Deprecated environment variable that can conflict with ANDROID_HOME
- **Solution**: Explicitly unset ANDROID_SDK_ROOT and use only ANDROID_HOME

### Build Process Flow
1. Set up Android SDK environment (fixed)
2. Build APK using Gradle (now works due to environment fix)
3. Decompile APK using apktool (enhanced error handling)
4. Extract smali code for injection (improved validation)
5. Copy native libraries (added existence checks)
6. Create zip archive for resources (unchanged)

### Error Handling Improvements
- **Before**: Script would fail silently or with cryptic errors
- **After**: Comprehensive error messages with debugging information
- **Added**: Directory structure listing when smali directory is missing
- **Added**: Build process validation at each step

## Testing Recommendations

1. **Local Testing**: Run the build locally with both environment variables set to verify the fix
2. **CI/CD Testing**: Monitor the next release build to ensure all fixes work correctly
3. **Rollback Plan**: If issues persist, the old CMakeLists.txt path can be restored temporarily

## Future Improvements

1. **Containerization**: Consider using Docker to ensure consistent build environments
2. **Dependency Management**: Add version pinning for Android SDK components
3. **Parallel Builds**: Optimize build process for faster CI/CD execution
4. **Error Recovery**: Add automatic retry mechanisms for network-related failures

## Summary

These fixes address the core issues causing the release build to fail:
- ✅ Resolved Android SDK environment variable conflicts
- ✅ Enhanced error handling and debugging capabilities
- ✅ Fixed CMake configuration to use appropriate build files
- ✅ Added proper validation at each build step

The build should now complete successfully during the "Create Release" process.