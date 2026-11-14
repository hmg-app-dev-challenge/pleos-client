#!/usr/bin/env python3
"""Package the existing signed APK and standalone macOS launchers, without a build."""
import argparse
import hashlib
from pathlib import Path
import shutil
import tempfile
import zipfile

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--apk', type=Path, default=root/'ivi/build/outputs/apk/release/ivi-release.apk')
parser.add_argument('--output', type=Path, default=Path.home()/'Desktop/ChaPark-IVI-0.5.5-macOS-AppleSilicon.zip')
args = parser.parse_args()
if not args.apk.is_file():
    parser.error('Build or provide the signed 0.5.5 APK first')
expected = "0a2a07cb4bd3b1e57370bc62fcb7805ad9524915db6ef13d67c3a8c54ef9cf7e"
if hashlib.sha256(args.apk.read_bytes()).hexdigest() != expected:
    parser.error('This package is pinned to the verified signed 0.5.5 APK')
for helper in ['portable-adb', 'portable-run']:
    if not (root/'distribution/macos/support'/helper).is_file():
        parser.error('Build the portable tools with scripts/build-portable-tools.sh first')
with tempfile.TemporaryDirectory(prefix='chapark-package-') as temp:
    folder=Path(temp)/'ChaPark-IVI-0.5.5'
    shutil.copytree(root/'distribution/macos',folder)
    apk=folder/'ChaPark-IVI-0.5.5-release.apk'
    shutil.copy2(args.apk,apk)
    (folder/'SHA256SUMS.txt').write_text(f'{hashlib.sha256(apk.read_bytes()).hexdigest()}  {apk.name}\n')
    args.output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(args.output,'w',zipfile.ZIP_DEFLATED,compresslevel=1) as archive:
        for path in sorted(folder.rglob('*')):
            if path.is_file():
                archive.write(path,path.relative_to(folder.parent))
print(args.output)
print(f'SHA-256: {hashlib.sha256(args.output.read_bytes()).hexdigest()}')
