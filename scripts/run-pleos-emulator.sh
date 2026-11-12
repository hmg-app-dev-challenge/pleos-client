#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SDK_DIR=${ANDROID_SDK_ROOT:-"$HOME/Library/Android/sdk"}
AVD_DIR="$PROJECT_DIR/.local-avd"
IMAGE_DIR="$SDK_DIR/system-images/android-34/pleos_car_emulator/arm64-v8a"
[ -f "$IMAGE_DIR/system.img" ] || { echo 'Pleos API 34 arm64 image is missing.' >&2; exit 1; }
mkdir -p "$AVD_DIR/ChaPark_Pleos.avd"
if [ ! -f "$AVD_DIR/ChaPark_Pleos.avd/config.ini" ]; then
cat > "$AVD_DIR/ChaPark_Pleos.ini" <<CONFIG
avd.ini.encoding=UTF-8
path=$AVD_DIR/ChaPark_Pleos.avd
target=android-34
CONFIG
cat > "$AVD_DIR/ChaPark_Pleos.avd/config.ini" <<CONFIG
AvdId=ChaPark_Pleos
avd.ini.displayname=ChaPark Pleos Connect 2.1.0
avd.ini.encoding=UTF-8
abi.type=arm64-v8a
tag.id=android-automotive
tag.display=Automotive
image.sysdir.1=$IMAGE_DIR/
hw.device.name=Pleos_Connect_v2_0
hw.device.manufacturer=42dot
hw.cpu.arch=arm64
hw.cpu.ncore=4
hw.ramSize=4096
hw.lcd.width=2560
hw.lcd.height=1440
hw.lcd.density=160
hw.initialOrientation=landscape
hw.gpu.enabled=yes
hw.gpu.mode=host
hw.keyboard=yes
hw.mainKeys=no
hw.battery=no
hw.gps=yes
hw.audioInput=yes
hw.sdCard=no
hw.multi_display_window=no
hw.hotplug_multi_display=no
disk.dataPartition.size=2147483648
fastboot.forceColdBoot=yes
showDeviceFrame=no
vm.heapSize=256
CONFIG
fi
export ANDROID_AVD_HOME="$AVD_DIR"
export ANDROID_EMULATOR_HOME="$AVD_DIR/android-home"
mkdir -p "$ANDROID_EMULATOR_HOME"
if [ ! -s "$ANDROID_EMULATOR_HOME/adbkey" ]; then
    "$SDK_DIR/platform-tools/adb" keygen "$ANDROID_EMULATOR_HOME/adbkey"
fi
exec "$SDK_DIR/emulator/emulator" -avd ChaPark_Pleos -no-snapshot "$@"
