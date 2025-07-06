#!/bin/bash

# Script to sign APK files with proper handling of existing signatures
# Usage: ./sign_apk.sh <apk_file> <keystore_file> <keystore_password> <key_password> <alias>

set -e

# Check arguments
if [ $# -lt 5 ]; then
    echo "Usage: $0 <apk_file> <keystore_file> <keystore_password> <key_password> <alias>"
    exit 1
fi

APK_FILE="$1"
KEYSTORE_FILE="$2"
KEYSTORE_PASSWORD="$3"
KEY_PASSWORD="$4"
ALIAS="$5"

# Check if APK file exists
if [ ! -f "$APK_FILE" ]; then
    echo "Error: APK file not found: $APK_FILE"
    exit 1
fi

# Check if keystore file exists
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "Error: Keystore file not found: $KEYSTORE_FILE"
    exit 1
fi

echo "Processing APK: $APK_FILE"

# Create backup
cp "$APK_FILE" "${APK_FILE}.backup"

# Remove existing signatures if present
echo "Removing existing signatures if any..."
zip -d "$APK_FILE" "META-INF/*.SF" "META-INF/*.RSA" "META-INF/*.DSA" "META-INF/MANIFEST.MF" 2>/dev/null || true

# Additional cleanup for any remaining signature files
echo "Performing thorough signature cleanup..."
unzip -Z1 "$APK_FILE" 2>/dev/null | grep -E '^META-INF/.*\.(SF|RSA|DSA)$' | while IFS= read -r file; do
    [ -n "$file" ] && zip -d "$APK_FILE" "$file" 2>/dev/null || true
done

# Sign the APK
echo "Signing APK..."
if jarsigner -verbose \
    -sigalg SHA256withRSA \
    -digestalg SHA256 \
    -keystore "$KEYSTORE_FILE" \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -tsa http://timestamp.digicert.com \
    "$APK_FILE" "$ALIAS"; then
    echo "✓ APK signed successfully"
else
    echo "✗ Failed to sign APK"
    exit 1
fi

# Verify the signature
echo "Verifying signature..."
if jarsigner -verify -verbose -certs "$APK_FILE"; then
    echo "✓ Signature verification passed"
else
    echo "✗ Signature verification failed"
    exit 1
fi

# Check if zipalign is available
if command -v zipalign &> /dev/null; then
    echo "Zipaligning APK..."
    mv "$APK_FILE" "${APK_FILE}.unaligned"
    zipalign -v 4 "${APK_FILE}.unaligned" "$APK_FILE"
    rm -f "${APK_FILE}.unaligned"
    echo "APK zipaligned successfully"
else
    echo "Warning: zipalign not found. Install Android SDK build-tools to enable zipalign."
    echo "You can install it with: sudo apt-get install zipalign"
fi

echo "APK signed successfully: $APK_FILE"
echo "Backup saved as: ${APK_FILE}.backup"