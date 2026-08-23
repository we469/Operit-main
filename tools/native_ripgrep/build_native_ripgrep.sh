#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_HOME:?ANDROID_HOME is required}"
: "${ANDROID_NDK_VERSION:?ANDROID_NDK_VERSION is required}"
: "${ANDROID_API_LEVEL:?ANDROID_API_LEVEL is required}"
: "${RUST_VERSION:?RUST_VERSION is required}"

export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/$ANDROID_NDK_VERSION"
TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/aarch64-linux-android${ANDROID_API_LEVEL}-clang"

if [[ ! -x "$CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER" ]]; then
  echo "Android Rust linker not found: $CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER" >&2
  exit 1
fi

rustup toolchain install "$RUST_VERSION" --profile minimal
rustup target add --toolchain "$RUST_VERSION" aarch64-linux-android

cargo "+$RUST_VERSION" build \
  --manifest-path tools/native_ripgrep/Cargo.toml \
  --release \
  --target aarch64-linux-android \
  --locked

OUTPUT="tools/native_ripgrep/target/aarch64-linux-android/release/liboperit_ripgrep.so"
if [[ ! -s "$OUTPUT" ]]; then
  echo "Native ripgrep output was not produced: $OUTPUT" >&2
  find tools/native_ripgrep/target -maxdepth 5 -type f -print >&2 || true
  exit 1
fi

mkdir -p app/src/main/jniLibs/arm64-v8a
install -Dm755 "$OUTPUT" app/src/main/jniLibs/arm64-v8a/liboperit_ripgrep.so

ls -lh app/src/main/jniLibs/arm64-v8a/liboperit_ripgrep.so
file app/src/main/jniLibs/arm64-v8a/liboperit_ripgrep.so || true
