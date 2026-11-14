#!/usr/bin/env python3
"""Pause/resume this project's running Pleos AVD without clearing its data.

Use pause while editing/building and resume for interaction or SDK verification.
The window is frozen and ADB is unavailable while paused. Do not pause mid-test.
"""
import argparse
import os
from pathlib import Path
import shlex
import signal
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("action", choices=["pause", "resume", "status"])
parser.add_argument("--avd", default="ChaPark_Release")
args = parser.parse_args()
project = Path(__file__).resolve().parent.parent
if not args.avd.replace("_", "").isalnum() or not (project / ".local-avd" / (args.avd + ".avd")).is_dir():
    parser.error("Choose an existing project AVD")
matches = []
for line in subprocess.check_output(["ps", "-axo", "pid=,command="], text=True).splitlines():
    fields = line.strip().split(None, 1)
    if len(fields) != 2:
        continue
    pid, command = fields
    try:
        words = shlex.split(command)
    except ValueError:
        continue
    if words and Path(words[0]).name == "qemu-system-aarch64" and "-avd" in words:
        index = words.index("-avd")
        if index + 1 < len(words) and words[index + 1] == args.avd:
            matches.append(int(pid))
if len(matches) != 1:
    raise SystemExit(f"Expected one running {args.avd}; found {len(matches)}")
pid = matches[0]
if args.action != "status":
    os.kill(pid, signal.SIGSTOP if args.action == "pause" else signal.SIGCONT)
subprocess.run(["ps", "-p", str(pid), "-o", "pid=,state=,%cpu=,comm="], check=True)
print("Paused: window/ADB frozen, memory retained." if args.action == "pause" else
      "Resumed: SDK connections may need to reconnect after a long pause." if args.action == "resume" else
      "State T means paused. CPU is a recent average; use top for a fresh sample.")
