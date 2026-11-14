#!/usr/bin/env python3
"""Run one bounded device task, then always pause the Pleos emulator.

Example: python3 scripts/pleos-session.py --timeout 180 -- <device command>
Build on the host before entering a session. The AVD must already be running.
"""
import argparse
from pathlib import Path
import os
import signal
import subprocess
import sys


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--timeout', type=int, default=180)
    parser.add_argument('--avd', default='ChaPark_Release')
    parser.add_argument('command', nargs=argparse.REMAINDER)
    args = parser.parse_args()
    command = args.command[1:] if args.command[:1] == ['--'] else args.command
    if not command or args.timeout <= 0:
        parser.error('Provide a command after -- and a positive timeout')
    runtime = Path(__file__).with_name('pleos-runtime.py')
    control = [sys.executable, str(runtime)]
    subprocess.run(control + ['status', '--avd', args.avd], check=True)
    child = None
    def interrupted(signum, frame):
        raise KeyboardInterrupt
    signal.signal(signal.SIGTERM, interrupted)
    try:
        subprocess.run(control + ['resume', '--avd', args.avd], check=True)
        child = subprocess.Popen(command, start_new_session=True)
        try:
            return child.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            print(f'Device session exceeded {args.timeout}s; stopping the task.', file=sys.stderr)
            return 124
    except KeyboardInterrupt:
        return 130
    finally:
        if child is not None and child.poll() is None:
            os.killpg(child.pid, signal.SIGTERM)
            try:
                child.wait(timeout=5)
            except subprocess.TimeoutExpired:
                os.killpg(child.pid, signal.SIGKILL)
                child.wait()
        subprocess.run(control + ['pause', '--avd', args.avd], check=True)


if __name__ == '__main__':
    sys.exit(main())
