#!/bin/bash

# Test script for billing patch functionality
# Usage: ./test_billing_patch.sh [apk_file]

set -e

APK_FILE="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"
TEST_DIR="$SCRIPT_DIR/test_output"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Google Play Billing Patch Test ===${NC}"

# Check if APK file is provided
if [ -z "$APK_FILE" ]; then
    echo -e "${RED}Error: Please provide an APK file as argument${NC}"
    echo "Usage: $0 <apk_file>"
    exit 1
fi

# Check if APK file exists
if [ ! -f "$APK_FILE" ]; then
    echo -e "${RED}Error: APK file '$APK_FILE' not found${NC}"
    exit 1
fi

echo -e "${GREEN}Testing APK: $APK_FILE${NC}"

# Create test directories
mkdir -p "$BUILD_DIR"
mkdir -p "$TEST_DIR"

# Build billing-hack APK
echo -e "${YELLOW}Building billing-hack APK...${NC}"
cd "$SCRIPT_DIR/billing-hack"

if ./gradlew assembleRelease; then
    echo -e "${GREEN}✓ billing-hack APK built successfully${NC}"
    
    # Copy to build directory
    cp app/build/outputs/apk/release/release-signed.apk "$BUILD_DIR/billing-hack.apk"
    echo -e "${GREEN}✓ billing-hack APK copied to build directory${NC}"
else
    echo -e "${RED}✗ Failed to build billing-hack APK${NC}"
    exit 1
fi

cd "$SCRIPT_DIR"

# Test APK analysis (if tools are available)
echo -e "${YELLOW}Analyzing target APK...${NC}"

if command -v aapt &> /dev/null; then
    echo -e "${GREEN}Checking APK package information:${NC}"
    aapt dump badging "$APK_FILE" | head -5
    
    echo -e "${GREEN}Checking for billing permissions:${NC}"
    if aapt dump permissions "$APK_FILE" | grep -i billing; then
        echo -e "${GREEN}✓ Found billing permissions${NC}"
    else
        echo -e "${YELLOW}! No billing permissions found${NC}"
    fi
else
    echo -e "${YELLOW}! aapt not found - skipping APK analysis${NC}"
fi

# Test pattern detection (basic check)
echo -e "${YELLOW}Testing pattern detection...${NC}"

if command -v unzip &> /dev/null && command -v strings &> /dev/null; then
    # Extract APK to temporary directory
    TEMP_DIR=$(mktemp -d)
    unzip -q "$APK_FILE" -d "$TEMP_DIR"
    
    # Look for billing patterns in the extracted files
    echo -e "${GREEN}Searching for Google Play Billing patterns:${NC}"
    
    FOUND_PATTERNS=0
    
    # Check for common billing patterns
    if find "$TEMP_DIR" -name "*.dex" -exec strings {} \; | grep -i "billingclient" | head -3; then
        echo -e "${GREEN}✓ Found BillingClient patterns${NC}"
        ((FOUND_PATTERNS++))
    fi
    
    if find "$TEMP_DIR" -name "*.dex" -exec strings {} \; | grep -i "com.android.vending" | head -3; then
        echo -e "${GREEN}✓ Found Google Play Vending patterns${NC}"
        ((FOUND_PATTERNS++))
    fi
    
    if find "$TEMP_DIR" -name "*.dex" -exec strings {} \; | grep -i "InAppBillingService" | head -3; then
        echo -e "${GREEN}✓ Found InAppBillingService patterns${NC}"
        ((FOUND_PATTERNS++))
    fi
    
    # Check for new billing library patterns
    if find "$TEMP_DIR" -name "*.dex" -exec strings {} \; | grep -E "(queryProductDetailsAsync|ProductDetails)" | head -3; then
        echo -e "${GREEN}✓ Found modern billing library patterns (PBL 5+)${NC}"
        ((FOUND_PATTERNS++))
    fi
    
    # Clean up
    rm -rf "$TEMP_DIR"
    
    if [ $FOUND_PATTERNS -gt 0 ]; then
        echo -e "${GREEN}✓ Found $FOUND_PATTERNS billing pattern(s) - APK likely uses Google Play Billing${NC}"
    else
        echo -e "${YELLOW}! No billing patterns found - APK might not use Google Play Billing${NC}"
    fi
else
    echo -e "${YELLOW}! Required tools not found - skipping pattern detection${NC}"
fi

# Test billing-hack installation (if ADB is available)
if command -v adb &> /dev/null; then
    echo -e "${YELLOW}Testing billing-hack installation...${NC}"
    
    # Check if device is connected
    if adb devices | grep -q "device$"; then
        echo -e "${GREEN}Android device detected${NC}"
        
        # Install billing-hack APK
        if adb install -r "$BUILD_DIR/billing-hack.apk"; then
            echo -e "${GREEN}✓ billing-hack installed successfully${NC}"
            
            # Check if service is running
            sleep 2
            if adb shell "pm list packages | grep org.billinghack"; then
                echo -e "${GREEN}✓ billing-hack package verified${NC}"
            else
                echo -e "${RED}✗ billing-hack package not found${NC}"
            fi
        else
            echo -e "${YELLOW}! Failed to install billing-hack (device may not be configured for testing)${NC}"
        fi
    else
        echo -e "${YELLOW}! No Android device connected - skipping installation test${NC}"
    fi
else
    echo -e "${YELLOW}! ADB not found - skipping installation test${NC}"
fi

# Summary
echo -e "${YELLOW}=== Test Summary ===${NC}"
echo -e "${GREEN}✓ billing-hack APK built successfully${NC}"
echo -e "${GREEN}✓ Basic pattern detection completed${NC}"

# Check versions
echo -e "${GREEN}Build Configuration:${NC}"
cd "$SCRIPT_DIR/billing-hack"
echo "  - Kotlin version: $(grep kotlin_version build.gradle | cut -d"'" -f2)"
echo "  - Android Gradle Plugin: $(grep 'com.android.tools.build:gradle' build.gradle | cut -d"'" -f2)"
echo "  - Target SDK: $(grep targetSdkVersion app/build.gradle | head -1 | grep -o '[0-9]*')"
echo "  - Min SDK: $(grep minSdkVersion app/build.gradle | head -1 | grep -o '[0-9]*')"

cd "$SCRIPT_DIR"

echo -e "${GREEN}=== Test Completed ===${NC}"
echo -e "${YELLOW}Note: For full testing, use Android Studio to build and test the Modder component${NC}"

# Cleanup
echo -e "${YELLOW}Cleaning up temporary files...${NC}"
# Keep build directory for manual testing
echo -e "${GREEN}Build artifacts saved in: $BUILD_DIR${NC}"