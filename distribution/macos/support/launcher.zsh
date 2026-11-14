#!/bin/zsh
# Standalone macOS launcher. Only macOS tools + installed Android SDK are used.
setopt NO_UNSET PIPE_FAIL
export LC_ALL=en_US.UTF-8
BUNDLE_DIR=${0:A:h:h}
STATE_DIR=${CHAPARK_LAUNCHER_STATE:-"$HOME/Library/Application Support/ChaPark IVI Launcher"}
APK="$BUNDLE_DIR/ChaPark-IVI-0.5.5-release.apk"
ACTION=${1:-start}
BRIDGE="$BUNDLE_DIR/support/portable-adb"
typeset -a ADB_KEYS
AVD_DIR=""
EMU_PID=""
CONSOLE_PORT=""
LOCK_HELD=0
FAILED=1
mkdir -p "$STATE_DIR/logs" "$STATE_DIR/registry" "$STATE_DIR/android-home"
fail() { print -u2 -- "오류: $*"; return 1; }
property() { /usr/bin/awk -F= -v key="$2" '{k=$1;gsub(/^[ \t]+|[ \t]+$/,"",k);if(k==key){sub(/^[^=]*=/,"");gsub(/^[ \t]+|[ \t\r]+$/,"",$0);print;exit}}' "$1"; }
# Bound every ADB invocation, including stalled authorization and installation.
limited() {
    local seconds=$1; shift
    "$@" & local child=$!
    local deadline=$((SECONDS+seconds)) result
    while kill -0 "$child" 2>/dev/null; do
        if ((SECONDS>=deadline)); then
            kill -TERM "$child" 2>/dev/null
            wait "$child" 2>/dev/null
            return 124
        fi
        sleep .2
    done
    wait "$child"; result=$?
    return "$result"
}
cleanup() {
    if ((FAILED)) && [[ "$ACTION" == start && -n "$EMU_PID" ]]; then
        kill -STOP "$EMU_PID" 2>/dev/null
        print -- "실행 오류로 에뮬레이터를 일시정지했습니다. 문제 해결 후 시작을 다시 실행하세요."
    fi
    if ((LOCK_HELD)); then
        /bin/rm -f "$STATE_DIR/action.lock/pid"
        /bin/rmdir "$STATE_DIR/action.lock" 2>/dev/null
    fi
}
trap cleanup EXIT
trap 'exit 130' INT TERM

find_sdk() {
    local candidate configured=""
    [[ -f "$BUNDLE_DIR/SDK_PATH.txt" ]] && configured=$(head -n 1 "$BUNDLE_DIR/SDK_PATH.txt")
    for candidate in "$configured" "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk"; do
        [[ -n "$candidate" && -x "$candidate/emulator/emulator" && -x "$candidate/platform-tools/adb" ]] || continue
        SDK_DIR=${candidate:A}; ADB="$SDK_DIR/platform-tools/adb"; return 0
    done
    fail "Android SDK의 emulator와 platform-tools/adb가 필요합니다. SDK_PATH.txt 첫 줄에 SDK 경로를 적어 주세요."
}
is_pleos() {
    local dir=$1 image
    [[ -f "$dir/config.ini" ]] || return 1
    image=$(property "$dir/config.ini" image.sysdir.1)
    [[ "$image" == /* ]] || image="$SDK_DIR/$image"
    [[ -f "$image/system.img" && -f "$image/source.properties" ]] || return 1
    [[ "$(property "$image/source.properties" Addon.VendorId)" == 42dot &&
       "$(property "$image/source.properties" AndroidVersion.ApiLevel)" == 34 &&
       "$(property "$image/source.properties" SystemImage.Abi)" == arm64-v8a ]]
}
# Resolve open userdata files, not process-name substrings or stale PID files.
running_avds() {
    local pid file dir
    for pid in ${(f)"$(/usr/bin/pgrep -f '/qemu-system-aarch64.*-avd' 2>/dev/null)"}; do
        [[ "$pid" == <-> ]] || continue
        for file in ${(f)"$(/usr/sbin/lsof -a -p "$pid" -Fn 2>/dev/null)"}; do
            [[ "$file" == n*/userdata-qemu.img ]] || continue
            dir=${file#n}; dir=${dir:h}
            is_pleos "$dir" && print -r -- "$pid|$dir"
        done
    done
}
select_avd() {
    local saved="" pair dir ini image available choice
    local -a candidates
    [[ -f "$STATE_DIR/selected-avd.txt" ]] && saved=$(<"$STATE_DIR/selected-avd.txt")
    if [[ -n "$saved" ]] && is_pleos "$saved"; then AVD_DIR=${saved:A}; return 0; fi
    for pair in ${(f)"$(running_avds)"}; do
        dir=${pair#*|}; [[ -d "$dir" ]] && candidates+=("$dir")
    done
    for ini in "$HOME"/.android/avd/*.ini(N) "${ANDROID_AVD_HOME:-$HOME/.android/avd}"/*.ini(N); do
        dir=$(property "$ini" path)
        [[ -n "$dir" ]] && is_pleos "$dir" && candidates+=("$dir")
    done
    candidates=( "${(@u)candidates}" )
    if (( ${#candidates} == 1 )); then AVD_DIR=$candidates[1]
    elif (( ${#candidates} > 1 )); then
        print -- "사용할 Pleos AVD를 선택하세요."
        select choice in "${candidates[@]}"; do [[ -n "$choice" ]] && { AVD_DIR=$choice; break; }; done
        [[ -n "$AVD_DIR" ]] || return 1
    else
        image="$SDK_DIR/system-images/android-34/pleos_car_emulator/arm64-v8a"
        [[ -f "$image/system.img" ]] || { fail "Pleos Connect API 34 ARM64 시스템 이미지를 먼저 설치하세요."; return 1; }
        available=$(/bin/df -Pk "$STATE_DIR" | /usr/bin/awk 'END{print $4}')
        ((available>=10485760)) || { fail "새 AVD를 만들려면 여유 공간 10GB 이상이 필요합니다. 기존 Pleos AVD를 켠 후 다시 실행해도 됩니다."; return 1; }
        AVD_DIR="$STATE_DIR/ChaPark_IVI.avd"
        mkdir -p "$AVD_DIR"
        # Only write a configuration for our own newly created AVD.
        if [[ ! -f "$AVD_DIR/config.ini" ]]; then
            cat > "$AVD_DIR/config.ini" <<CONFIG
AvdId=ChaPark_IVI
avd.ini.displayname=ChaPark IVI
avd.ini.encoding=UTF-8
abi.type=arm64-v8a
tag.id=android-automotive
image.sysdir.1=$image/
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
hw.sdCard=no
hw.multi_display_window=no
hw.hotplug_multi_display=no
disk.dataPartition.size=6442450944
fastboot.forceColdBoot=yes
showDeviceFrame=no
vm.heapSize=256
CONFIG
        fi
        is_pleos "$AVD_DIR" || { fail "지원하는 Pleos 이미지 구성을 확인하지 못했습니다."; return 1; }
    fi
    AVD_DIR=${AVD_DIR:A}
    print -r -- "$AVD_DIR" > "$STATE_DIR/selected-avd.txt"
}
find_running() {
    local pair file port
    EMU_PID=""; CONSOLE_PORT=""
    for pair in ${(f)"$(running_avds)"}; do
        [[ "${pair#*|}" == "$AVD_DIR" ]] || continue
        [[ -z "$EMU_PID" ]] || { fail "동일 AVD 프로세스가 여러 개입니다. 에뮬레이터 창을 확인하세요."; return 1; }
        EMU_PID=${pair%%|*}
    done
    if [[ -n "$EMU_PID" ]]; then
        for file in ${(f)"$(/usr/sbin/lsof -a -p "$EMU_PID" -iTCP -sTCP:LISTEN -Fn 2>/dev/null)"}; do
            [[ "$file" == n*:* ]] || continue
            port=${file##*:}
            if [[ "$port" == <-> ]] && ((port>=5554 && port<=5682 && port%2==0)); then CONSOLE_PORT=$port; break; fi
        done
    fi
    return 0
}
setup_adb() {
    [[ -x "$BRIDGE" ]] || { fail "support/portable-adb 파일이 필요합니다."; return 1; }
    [[ -s "$STATE_DIR/android-home/adbkey" ]] || "$ADB" keygen "$STATE_DIR/android-home/adbkey" >/dev/null || return 1
    ADB_KEYS=("$STATE_DIR/android-home/adbkey")
    local key="$AVD_DIR/../android-home/adbkey"
    [[ -r "$key" ]] && ADB_KEYS+=("${key:A}")
    [[ -r "$HOME/.android/adbkey" ]] && ADB_KEYS+=("$HOME/.android/adbkey")
    ADB_KEYS=("${(@u)ADB_KEYS}")
}
device() {
    local seconds=$1; shift
    limited "$seconds" "$BRIDGE" "$((CONSOLE_PORT+1))" "${ADB_KEYS[@]}" -- "$@"
}
start_emulator() {
    local port file deadline avd_name
    avd_name=$(property "$AVD_DIR/config.ini" AvdId)
    [[ "$avd_name" =~ '^[A-Za-z0-9_]+$' ]] || avd_name=ChaPark_IVI
    cat > "$STATE_DIR/registry/$avd_name.ini" <<CONFIG
avd.ini.encoding=UTF-8
path=$AVD_DIR
target=android-34
CONFIG
    for port in {5560..5680..2}; do
        if ! /usr/sbin/lsof -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1 &&
           ! /usr/sbin/lsof -iTCP:"$((port+1))" -sTCP:LISTEN >/dev/null 2>&1; then CONSOLE_PORT=$port; break; fi
    done
    [[ -n "$CONSOLE_PORT" ]] || { fail "에뮬레이터 실행 포트를 찾지 못했습니다."; return 1; }
    print -- "Pleos를 시작합니다. 첫 부팅은 몇 분 걸릴 수 있습니다."
    export ANDROID_AVD_HOME="$STATE_DIR/registry"
    export ANDROID_EMULATOR_HOME="$STATE_DIR/android-home"
    "$BUNDLE_DIR/support/portable-run" "$STATE_DIR/logs/emulator.log" \
        "$SDK_DIR/emulator/emulator" -avd "$avd_name" -port "$CONSOLE_PORT" -gpu host -no-snapshot -no-metrics || return 1
    # Locate the actual QEMU after the emulator launcher execs/forks.
    deadline=$((SECONDS+20))
    while ((SECONDS<deadline)); do
        find_running || return 1
        [[ -n "$EMU_PID" && -n "$CONSOLE_PORT" ]] && return 0
        sleep 1
    done
    fail "에뮬레이터 시작 실패. 로그: $STATE_DIR/logs/emulator.log"
}
main() {
    [[ "$(uname -s)" == Darwin && "$(uname -m)" == arm64 ]] || { fail "이 패키지는 Apple Silicon Mac용입니다."; return 1; }
    find_sdk || return 1
    # Recover only our stale action lock; never clear emulator locks or user data.
    if [[ -d "$STATE_DIR/action.lock" ]]; then
        local owner=""
        [[ -f "$STATE_DIR/action.lock/pid" ]] && owner=$(<"$STATE_DIR/action.lock/pid")
        if [[ "$owner" == <-> ]] && ! kill -0 "$owner" 2>/dev/null; then
            /bin/rm -f "$STATE_DIR/action.lock/pid"; /bin/rmdir "$STATE_DIR/action.lock" 2>/dev/null
        fi
    fi
    /bin/mkdir "$STATE_DIR/action.lock" 2>/dev/null || { fail "다른 시작/종료 작업이 진행 중입니다."; return 1; }
    LOCK_HELD=1; print -- $$ > "$STATE_DIR/action.lock/pid"
    if [[ "$ACTION" != start && ! -f "$STATE_DIR/selected-avd.txt" ]]; then print -- "먼저 시작.command를 실행하세요."; return 0; fi
    select_avd || return 1
    find_running || return 1
    case "$ACTION" in
        pause) [[ -n "$EMU_PID" ]] && { kill -STOP "$EMU_PID" || return 1; print -- "일시정지 완료. 화면과 ADB가 멈추며 데이터는 유지됩니다."; } || print -- "실행 중인 Pleos가 없습니다."; return 0;;
        resume) [[ -n "$EMU_PID" ]] && { kill -CONT "$EMU_PID" || return 1; print -- "재개했습니다. 종료된 경우 시작.command를 사용하세요."; } || print -- "종료된 상태입니다. 시작.command를 실행하세요."; return 0;;
        stop)
            if [[ -z "$EMU_PID" ]]; then print -- "이미 종료되어 있습니다."; return 0; fi
            kill -CONT "$EMU_PID" || return 1
            # SIGTERM requests normal emulator shutdown; never use SIGKILL or wipe-data.
            kill -TERM "$EMU_PID" || return 1
            local deadline=$((SECONDS+30))
            while kill -0 "$EMU_PID" 2>/dev/null && ((SECONDS<deadline)); do sleep 1; done
            kill -0 "$EMU_PID" 2>/dev/null && { fail "종료 대기 중입니다. 강제 삭제하지 말고 잠시 후 다시 확인하세요."; return 1; }
            print -- "종료 완료. 다음에는 시작.command를 다시 실행하세요. 여행 기록은 유지됩니다."; return 0;;
        start) ;;
        *) fail "알 수 없는 작업: $ACTION"; return 1;;
    esac
    [[ -f "$APK" && -f "$BUNDLE_DIR/SHA256SUMS.txt" ]] || { fail "APK와 SHA256SUMS.txt를 같은 폴더에 두세요."; return 1; }
    (cd "$BUNDLE_DIR" && /usr/bin/shasum -a 256 -c SHA256SUMS.txt) || return 1
    setup_adb || return 1
    if [[ -z "$EMU_PID" ]]; then start_emulator || return 1; else kill -CONT "$EMU_PID" || return 1; print -- "기존 Pleos를 재사용합니다."; fi
    [[ -n "$CONSOLE_PORT" ]] || { fail "기존 에뮬레이터의 ADB 포트를 찾지 못했습니다."; return 1; }
    local boot="" user="" output="" deadline=$((SECONDS+240)) headless=""
    print -- "Android 부팅과 ADB 연결을 기다립니다."
    while ((SECONDS<deadline)); do
        kill -0 "$EMU_PID" 2>/dev/null || { fail "에뮬레이터가 종료되었습니다. emulator.log를 확인하세요."; return 1; }
        boot=$(device 12 shell "getprop sys.boot_completed" 2>/dev/null)
        boot=${boot//$'\r'/}
        if [[ "$boot" == 1 ]]; then
            user=$(device 12 shell "am get-current-user" 2>/dev/null); user=${user//$'\r'/}
            headless=$(device 12 shell "getprop ro.fw.mu.headless_system_user" 2>/dev/null)
            [[ "$user" == <-> && ( "$user" != 0 || "$headless" != *true* ) ]] && break
        fi
        sleep 2
    done
    [[ "$boot" == 1 && "$user" == <-> && ( "$user" != 0 || "$headless" != *true* ) ]] || { fail "부팅/ADB 연결 시간 초과. USB 디버깅 승인 화면과 SDK 설치를 확인하세요."; return 1; }
    print -- "차Park 0.5.5를 설치합니다. 기존 여행 데이터는 유지합니다."
    output=$(device 60 install "$user" "$APK" 2>&1)
    print -r -- "$output"
    [[ "$output" == *Success* ]] || { fail "APK 설치 실패. 기존 앱을 자동 삭제하지 않았습니다."; return 1; }
    output=$(device 15 shell "am start --user $user -n com.chapark.ivi/.IviActivity" 2>&1)
    print -r -- "$output"
    [[ "$output" != *Error* && "$output" == *Starting* ]] || return 1
    print -- "차Park 실행 완료. 편집/휴식 중에는 일시정지.command, 사용을 마치면 종료.command를 실행하세요."
    print -- "AVD: $AVD_DIR"
}
main
result=$?
((result==0)) && FAILED=0
exit "$result"
