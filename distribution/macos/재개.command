#!/bin/zsh
BUNDLE_DIR=${0:A:h}
/bin/zsh "$BUNDLE_DIR/support/launcher.zsh" resume
result=$?
if ((result!=0)); then
    print -- "문제가 해결되지 않으면 사용안내.txt와 로그 폴더를 확인하세요."
    [[ -t 0 ]] && read -r "?Enter 키를 누르면 닫습니다."
fi
exit "$result"
