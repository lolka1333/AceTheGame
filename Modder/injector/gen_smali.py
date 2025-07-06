# =====================================================
"""
brief:
    for auto generating a smali from java program
    that can be injected into an apk

    the smali code and native lib will be used for starting up the memory scanning/editing
    service directly in the apk 

steps:
    - build the apk [./apk_source] with gradle 
    - decompile the apk using apktool
    - get smali code for injection and ACE engine's native library 
      from decompilation in the previous step

      it is possible because it use ACE engine's lib via JNI
      and has class to start the library 
    - copy the smali and native library to [Modder]'s resource directory
  

"""
# =====================================================
import subprocess
import os
import shutil
import tempfile

# ============================= paths ==================
APK_SOURCE_ROOT_DIR = os.path.join("apk_source", "hello-libs")
APK_BUILT_OUTPUT_PATH = (
    os.path.join("apk_source", "hello-libs", "app","build","outputs","apk", "debug", "app-debug.apk")
)

OUT_CODE_FOR_INJECT_DIR = (
    os.path.join("..", "..", "Modder","modder","src","main", "resources", "AceAndroidLib", "code_to_inject")
)
SMALI_RELATIVE_DIR = os.path.join("smali", "com", "AceInjector")

OUT_SMALI_DIR = os.path.join(OUT_CODE_FOR_INJECT_DIR, SMALI_RELATIVE_DIR)
OUT_SMALI_DIR_ZIPPED_FILE = OUT_SMALI_DIR
NATIVE_LIB_OUT_DIR = os.path.join(OUT_CODE_FOR_INJECT_DIR, "lib")

# =========================== funcs =============


def cp_folder(src: str, dest: str):
    # if destination exist remove
    if os.path.exists(dest):
        shutil.rmtree(dest)
    # then copy
    shutil.copytree(src, dest)


# creating temp dir for decompilation result
# https://stackoverflow.com/a/55104228/14073678
with tempfile.TemporaryDirectory() as temp_decompiled_apk_dir:
    generated_smali_dir = os.path.join(temp_decompiled_apk_dir, SMALI_RELATIVE_DIR)
    # get path to native lib for source and destination
    generated_native_lib_dir = os.path.join(temp_decompiled_apk_dir, "lib")

    print("Generating temporary apk")

    # Build the APK
    if os.name == "posix":
        result = subprocess.run("./gradlew assembleDebug", cwd=APK_SOURCE_ROOT_DIR, shell=True)
    else:
        result = subprocess.run("gradlew assembleDebug", cwd=APK_SOURCE_ROOT_DIR, shell=True)
    
    # Check if build was successful
    if result.returncode != 0:
        print(f"ERROR: Gradle build failed with exit code {result.returncode}")
        print("This is likely due to Android SDK environment variable conflicts.")
        print("Make sure ANDROID_HOME is set and ANDROID_SDK_ROOT is unset.")
        exit(1)

    # Check if APK file exists
    if not os.path.exists(APK_BUILT_OUTPUT_PATH):
        print(f"ERROR: APK file not found at {APK_BUILT_OUTPUT_PATH}")
        print("The build may have failed or the output path may be incorrect.")
        exit(1)

    # decode without resources and
    # put the smali results in smali folder
    # also force them using -f
    print("decompiling temp APK")
    # command must be put in one string when setting `shell=True`
    # https://stackoverflow.com/questions/26417658/subprocess-call-arguments-ignored-when-using-shell-true-w-list
    result = subprocess.run(
        f"apktool d {APK_BUILT_OUTPUT_PATH} -r -f -o {temp_decompiled_apk_dir}",
        shell=True,
    )
    
    # Check if decompilation was successful
    if result.returncode != 0:
        print(f"ERROR: APK decompilation failed with exit code {result.returncode}")
        exit(1)

    # Check if the expected smali directory exists
    if not os.path.exists(generated_smali_dir):
        print(f"ERROR: Expected smali directory not found at {generated_smali_dir}")
        print("The decompilation may have failed or the smali structure may be different.")
        print("Contents of decompiled APK:")
        for root, dirs, files in os.walk(temp_decompiled_apk_dir):
            level = root.replace(temp_decompiled_apk_dir, '').count(os.sep)
            indent = ' ' * 2 * level
            print(f"{indent}{os.path.basename(root)}/")
            subindent = ' ' * 2 * (level + 1)
            for file in files[:10]:  # Limit to first 10 files per directory
                print(f"{subindent}{file}")
            if len(files) > 10:
                print(f"{subindent}... and {len(files) - 10} more files")
        exit(1)

    # put the smali for injection
    print("Copying smali %s to %s" % (generated_smali_dir, OUT_SMALI_DIR))
    cp_folder(generated_smali_dir, OUT_SMALI_DIR)

    print("Copying native libs")
    if os.path.exists(generated_native_lib_dir):
        cp_folder(generated_native_lib_dir, NATIVE_LIB_OUT_DIR)
    else:
        print(f"Warning: Native lib directory not found at {generated_native_lib_dir}")

    # zip the smali code for easier resource access by [Modder]
    # don't need to add ".zip" extension, because
    # it will do it for us
    shutil.make_archive(OUT_SMALI_DIR_ZIPPED_FILE, "zip", generated_smali_dir)
    print("Generated zipped smali at %s" % (OUT_SMALI_DIR_ZIPPED_FILE))
    print("Code for injection is generated at %s" % (OUT_CODE_FOR_INJECT_DIR))
