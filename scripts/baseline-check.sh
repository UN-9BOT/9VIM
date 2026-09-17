#!/bin/sh

# Run the complete, repository-owned baseline gate after validating the
# toolchain contract. This script never installs or mutates host tooling.
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
MANIFEST=${BASELINE_TOOLCHAIN_MANIFEST:-$ROOT_DIR/config/baseline-toolchain.properties}
PREFLIGHT_ONLY=false

usage() {
    cat <<'EOF'
Usage: ./scripts/baseline-check.sh [--preflight-only]

Validate the declared baseline toolchain and run all mandatory Gradle gates.

BASELINE_TOOLCHAIN_MANIFEST overrides the default manifest path. The
--preflight-only mode validates the manifest, Java, Gradle wrapper, Android
SDK platform, and Android build tools without starting Gradle.
EOF
}

error() {
    printf 'baseline-check: error: %s\n' "$1" >&2
}

if [ "$#" -gt 0 ]; then
    case "$1" in
        --preflight-only)
            if [ "$#" -ne 1 ]; then
                usage >&2
                exit 2
            fi
            PREFLIGHT_ONLY=true
            ;;
        --help|-h)
            if [ "$#" -ne 1 ]; then
                usage >&2
                exit 2
            fi
            usage
            exit 0
            ;;
        *)
            usage >&2
            exit 2
            ;;
    esac
fi

if [ ! -r "$MANIFEST" ]; then
    error "toolchain manifest '$MANIFEST' is missing or unreadable"
    exit 1
fi

# Keep the manifest intentionally flat and deterministic. Blank and comment
# lines are allowed; every other line must be a key=value entry.
while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
        ''|\#*)
            continue
            ;;
        *=*)
            continue
            ;;
        *)
            error "malformed manifest line: '$line' (expected key=value)"
            exit 1
            ;;
    esac
done < "$MANIFEST"

manifest_entry() {
    key=$1
    count=$(awk -F= -v key="$key" '$1 == key { count++ } END { print count + 0 }' "$MANIFEST")
    if [ "$count" -ne 1 ]; then
        error "manifest key '$key' must occur exactly once (found $count)"
        exit 1
    fi

    value=$(awk -F= -v key="$key" '$1 == key { print substr($0, index($0, "=") + 1); exit }' "$MANIFEST")
    if [ -z "$value" ]; then
        error "manifest key '$key' must have a non-empty value"
        exit 1
    fi
    printf '%s' "$value"
}

expected_gradle=$(manifest_entry gradle)
expected_agp=$(manifest_entry agp)
expected_java=$(manifest_entry java)
expected_compile_sdk=$(manifest_entry compileSdk)
expected_target_sdk=$(manifest_entry targetSdk)
expected_min_sdk=$(manifest_entry minSdk)
expected_build_tools=$(manifest_entry buildTools)

failed=0
check_equal() {
    label=$1
    expected=$2
    observed=$3
    if [ "$observed" != "$expected" ]; then
        error "$label mismatch: observed '$observed', expected '$expected'"
        failed=1
    fi
}

wrapper_properties=$ROOT_DIR/gradle/wrapper/gradle-wrapper.properties
if [ ! -r "$wrapper_properties" ]; then
    error "Gradle wrapper properties '$wrapper_properties' are missing or unreadable"
    failed=1
    observed_gradle=missing
else
    wrapper_url=$(awk -F= '$1 == "distributionUrl" { print substr($0, index($0, "=") + 1); exit }' "$wrapper_properties")
    wrapper_url=$(printf '%s' "$wrapper_url" | sed 's/\\:/:/g')
    observed_gradle=$(printf '%s' "$wrapper_url" | sed -n 's#.*gradle-\([0-9][0-9.]*\)-[^/]*\.zip.*#\1#p')
    [ -n "$observed_gradle" ] || observed_gradle=unreadable
fi
check_equal 'Gradle wrapper' "$expected_gradle" "$observed_gradle"

version_catalog=$ROOT_DIR/gradle/libs.versions.toml
if [ ! -r "$version_catalog" ]; then
    error "version catalog '$version_catalog' is missing or unreadable"
    failed=1
    observed_agp=missing
else
    observed_agp=$(sed -n 's/^[[:space:]]*android-gradle-plugin[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$version_catalog" | sed -n '1p')
    [ -n "$observed_agp" ] || observed_agp=unreadable
fi
check_equal 'Android Gradle Plugin' "$expected_agp" "$observed_agp"

if command -v java >/dev/null 2>&1; then
    java_version=$(java -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | sed -n '1p')
    if [ -z "$java_version" ]; then
        observed_java=unreadable
    elif printf '%s' "$java_version" | grep -q '^1\.'; then
        observed_java=$(printf '%s' "$java_version" | sed 's/^1\.//' | sed 's/\..*//')
    else
        observed_java=$(printf '%s' "$java_version" | sed 's/\..*//')
    fi
else
    observed_java=missing
fi
check_equal 'Java major version' "$expected_java" "$observed_java"

build_gradle=$ROOT_DIR/8vim/build.gradle.kts
if [ ! -r "$build_gradle" ]; then
    error "Android module build file '$build_gradle' is missing or unreadable"
    failed=1
    observed_compile_sdk=missing
    observed_target_sdk=missing
    observed_min_sdk=missing
else
    observed_compile_sdk=$(sed -n 's/^[[:space:]]*compileSdk[[:space:]]*=[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$build_gradle" | sed -n '1p')
    observed_target_sdk=$(sed -n 's/^[[:space:]]*targetSdk[[:space:]]*=[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$build_gradle" | sed -n '1p')
    observed_min_sdk=$(sed -n 's/^[[:space:]]*minSdk[[:space:]]*=[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$build_gradle" | sed -n '1p')
    [ -n "$observed_compile_sdk" ] || observed_compile_sdk=unreadable
    [ -n "$observed_target_sdk" ] || observed_target_sdk=unreadable
    [ -n "$observed_min_sdk" ] || observed_min_sdk=unreadable
fi
check_equal 'compile SDK declaration' "$expected_compile_sdk" "$observed_compile_sdk"
check_equal 'target SDK declaration' "$expected_target_sdk" "$observed_target_sdk"
check_equal 'minimum SDK declaration' "$expected_min_sdk" "$observed_min_sdk"

sdk_root=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
if [ -z "$sdk_root" ] && [ -r "$ROOT_DIR/local.properties" ]; then
    sdk_root=$(sed -n 's/^sdk\.dir=//p' "$ROOT_DIR/local.properties" | sed -n '1p' | sed 's/\\:/\:/g; s/\\ / /g')
fi
if [ -n "$sdk_root" ] && [ "${sdk_root#/}" = "$sdk_root" ]; then
    sdk_root=$ROOT_DIR/$sdk_root
fi

if [ -n "$sdk_root" ] && [ -f "$sdk_root/platforms/android-$expected_compile_sdk/android.jar" ]; then
    observed_platform="android-$expected_compile_sdk"
else
    observed_platform=missing
fi
check_equal 'Android SDK platform' "android-$expected_compile_sdk" "$observed_platform"

if [ -n "$sdk_root" ] && [ -x "$sdk_root/build-tools/$expected_build_tools/aapt2" ]; then
    observed_build_tools=$expected_build_tools
else
    observed_build_tools=missing
fi
check_equal 'Android build tools' "$expected_build_tools" "$observed_build_tools"

if [ "$failed" -ne 0 ]; then
    error 'preflight failed; install/configure the declared toolchain without changing the repository manifest'
    exit 1
fi

if [ "$PREFLIGHT_ONLY" = true ]; then
    printf 'baseline-check: preflight passed using %s\n' "$MANIFEST"
    exit 0
fi

if [ ! -x "$ROOT_DIR/gradlew" ]; then
    error "Gradle wrapper '$ROOT_DIR/gradlew' is missing or not executable"
    exit 1
fi

exec "$ROOT_DIR/gradlew" --no-daemon :8vim:testDebugUnitTest :8vim:lint :8vim:ktlintCheck :8vim:checkstyle :8vim:assembleDebug
