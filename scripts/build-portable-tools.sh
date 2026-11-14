#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"
xcrun swiftc -j 1 -Onone -target arm64-apple-macos13.0 tools/portable-adb/main.swift \
  -o distribution/macos/support/portable-adb -framework Security
xcrun clang -target arm64-apple-macos13.0 -Wall -Wextra -Werror tools/portable-adb/detach.c \
  -o distribution/macos/support/portable-run
codesign --verify distribution/macos/support/portable-adb
codesign --verify distribution/macos/support/portable-run
