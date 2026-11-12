#!/usr/bin/env python3
"""Local Pleos AVD helper. Install adb-shell==0.4.4 in an isolated venv first.
Uses only the project-owned debugging key, never the user's existing adbkey.
"""
import argparse
import uuid
import shlex
from pathlib import Path
from adb_shell.adb_device import AdbDeviceTcp
from adb_shell.auth.sign_pythonrsa import PythonRSASigner

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--port', type=int, default=5557)
sub = parser.add_subparsers(dest='action', required=True)
sh = sub.add_parser('shell'); sh.add_argument('command')
install = sub.add_parser('install'); install.add_argument('apk', type=Path)
pull = sub.add_parser('pull'); pull.add_argument('remote'); pull.add_argument('local', type=Path)
appfile = sub.add_parser('pull-app-file')
appfile.add_argument('remote'); appfile.add_argument('local', type=Path)
appfile.add_argument('--user', type=int, default=10)
appfile.add_argument('--package', choices=['com.chapark.ivi', 'com.chapark.app'], default='com.chapark.ivi')
args = parser.parse_args()
key = root / '.local-avd/android-home/adbkey'
signer = PythonRSASigner(key.with_suffix('.pub').read_text(), key.read_text())
device = AdbDeviceTcp('127.0.0.1', args.port, default_transport_timeout_s=60)
try:
    device.connect(rsa_keys=[signer], auth_timeout_s=45)
    # adb-shell 0.4.4 stalls FileSync at the image's advertised large packet size.
    # Restrict to a bounded payload verified against this emulator.
    device._maxdata = 65536
    if args.action == 'shell':
        print(device.shell(args.command, read_timeout_s=60))
    elif args.action == 'install':
        remote = '/data/local/tmp/chapark-' + uuid.uuid4().hex + '.apk'
        device.push(str(args.apk.resolve()), remote, read_timeout_s=60, transport_timeout_s=60)
        result = device.shell('pm install -r ' + remote, read_timeout_s=60)
        print(result)
        device.shell('rm -f ' + remote)
        if 'Success' not in result:
            raise SystemExit(1)
    elif args.action == 'pull':
        args.local.parent.mkdir(parents=True, exist_ok=True)
        device.pull(args.remote, str(args.local), read_timeout_s=60, transport_timeout_s=60)
    else:
        args.local.parent.mkdir(parents=True, exist_ok=True)
        command = 'run-as ' + args.package + ' --user ' + str(args.user) + ' cat ' + shlex.quote(args.remote)
        args.local.write_bytes(device.shell(command, decode=False, read_timeout_s=60))
finally:
    device.close()
