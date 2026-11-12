#!/usr/bin/env python3
"""Access the emulator's documented Virtual Console HTTP surface through project ADB."""
import argparse,json,shlex
from pathlib import Path
from adb_shell.adb_device import AdbDeviceTcp
from adb_shell.auth.sign_pythonrsa import PythonRSASigner
p=argparse.ArgumentParser();p.add_argument('method',choices=['GET','PUT','POST']);p.add_argument('path');a=p.parse_args()
assert a.path.startswith(('/vehicle/','/browser/session')) and all(c.isalnum() or c in '/?=&-_.%' for c in a.path)
key=Path(__file__).resolve().parents[1]/'.local-avd/android-home/adbkey'
d=AdbDeviceTcp('127.0.0.1',5557,default_transport_timeout_s=10)
try:
 d.connect(rsa_keys=[PythonRSASigner(key.with_suffix('.pub').read_text(),key.read_text())],auth_timeout_s=10)
 request=f'{a.method} {a.path} HTTP/1.0\r\nHost: localhost\r\nContent-Length: 0\r\n\r\n'
 response=d.shell('printf %s '+shlex.quote(request)+' | nc -w 2 127.0.0.1 8088')
 print(response)
finally:d.close()
