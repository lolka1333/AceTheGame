# Android Build Fixes

## Issues Fixed

### 1. ANDROID_PLATFORM Version Mismatch

**Problem**: The GitHub Actions workflows were setting `ANDROID_PLATFORM=android-21`, but the CMakeLists.txt requires at least API level 23 for `process_vm_readv` and `process_vm_writev` functions.

**Error Message**:
```
CMake Error at CMakeLists.txt:56 (message):
  You need to set -DANDROID_PLATFORM to at least "android-23"
```

**Fix**: Updated both workflow files to use `android-23`:
- `.github/workflows/weekly-build.yml`: Changed `-DANDROID_PLATFORM=android-21` to `-DANDROID_PLATFORM=android-23`
- `.github/workflows/release.yml`: Changed `-DANDROID_PLATFORM=android-21` to `-DANDROID_PLATFORM=android-23`

### 2. CMake Deprecation Warnings

**Problem**: CMake was showing deprecation warnings about compatibility with CMake < 3.10.

**Error Message**:
```
CMake Deprecation Warning at android.toolchain.cmake:34 (cmake_minimum_required):
  Compatibility with CMake < 3.10 will be removed from a future version of CMake.
  Update the VERSION argument <min> value.
```

**Fix**: Updated `ACE/CMakeLists.txt` to require CMake 3.15 instead of 3.12:
```cmake
cmake_minimum_required(VERSION 3.15)
```

## Why These Fixes Are Necessary

1. **API Level 23 Requirement**: The ACE project uses `process_vm_readv` and `process_vm_writev` functions which are only available starting from Android API level 23. These functions are essential for memory reading/writing operations on Android.

2. **CMake Version Compatibility**: Using a higher CMake version eliminates deprecation warnings from the Android NDK toolchain files and ensures better compatibility with newer build systems.

## Files Modified

- `.github/workflows/weekly-build.yml`
- `.github/workflows/release.yml`
- `ACE/CMakeLists.txt`

## Testing

After these fixes, the Android build should complete successfully without the platform version error or CMake deprecation warnings.