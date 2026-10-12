"""Launch the opt-in OpenGL gallery check. Run under xvfb-run in headless CI.

Unlike the CPU rig checks, this launches the actual Minecraft client and verifies
the bundled GLSL compiles, atlas-backed models draw, and framebuffer PNGs exist.
It does not claim an interactive-world or artistic-quality test.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import io
import os
from pathlib import Path
import re
import signal
import struct
import subprocess
import sys
import time


ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "build/reports/visual-smoke"
CAPTURES = (
    "species-overview.png",
    "forms-overview.png",
    "magic-overview.png",
    "forms-strike.png",
)


def png_size(path: Path) -> tuple[int, int]:
    header = path.read_bytes()[:24]
    if len(header) != 24 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        raise ValueError(f"Not a framebuffer PNG: {path.name}")
    return struct.unpack(">II", header[16:24])


def fatal_resource_lines(log: str) -> list[str]:
    failures = []
    shader_errors = re.compile(
        r"VISUAL_SMOKE_FAIL|shader compilation failed|could(?:n't| not) compile.*shader"
        r"|could(?:n't| not) load.*shader|failed to load.*shader|arcane shader unavailable",
        re.IGNORECASE,
    )
    resource_errors = re.compile(
        r"unable to load model|missing texture|unable to load texture|failed to load texture"
        r"|exception loading blockstate|missing model|file not found|filenotfoundexception",
        re.IGNORECASE,
    )
    for line in log.splitlines():
        if shader_errors.search(line) or (
            "shadowslave" in line.lower() and resource_errors.search(line)
        ):
            failures.append(line)
    return failures


def emit_base64(path: Path) -> None:
    raw = path.read_bytes()
    width, _ = png_size(path)
    if width > 960:
        try:
            from PIL import Image
        except ImportError:
            raise SystemExit("--emit-base64 requires Pillow for images wider than 960px")
        with Image.open(io.BytesIO(raw)) as image:
            image.thumbnail((960, 960))
            buffer = io.BytesIO()
            image.save(buffer, format="PNG", optimize=True)
            raw = buffer.getvalue()
    print(f"VISUAL_SMOKE_IMAGE {path.name} {base64.b64encode(raw).decode('ascii')}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--timeout", type=int, default=180,
                        help="Whole Gradle/client process limit; client has its own 120s watchdog")
    parser.add_argument("--emit-base64", action="store_true",
                        help="Also emit small PNGs as log markers for remote review")
    args = parser.parse_args()
    if not 30 <= args.timeout <= 240:
        parser.error("--timeout must be between 30 and 240 seconds")
    REPORT.mkdir(parents=True, exist_ok=True)
    for name in CAPTURES:
        (REPORT / name).unlink(missing_ok=True)
    command = ["bash", str(ROOT / "gradlew"), "--no-daemon", "--console=plain",
               "-PvisualSmoke", "runClient"]
    started = time.monotonic()
    process = subprocess.Popen(command, cwd=ROOT, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True, start_new_session=True)
    timed_out = False
    try:
        output, _ = process.communicate(timeout=args.timeout)
    except subprocess.TimeoutExpired:
        timed_out = True
        os.killpg(process.pid, signal.SIGTERM)
        try:
            output, _ = process.communicate(timeout=10)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            output, _ = process.communicate()
    (REPORT / "stdout.log").write_text(output, encoding="utf-8")
    client_log = ROOT / "run/logs/latest.log"
    logs = output + "\n" + (client_log.read_text(encoding="utf-8", errors="replace")
                             if client_log.exists() else "")
    errors = []
    if timed_out:
        errors.append(f"Client/Gradle exceeded {args.timeout}s")
    if process.returncode != 0:
        errors.append(f"Gradle/client exited {process.returncode}")
    if "VISUAL_SMOKE_PASS captures=4 shader=arcane backend=OpenGL" not in logs:
        errors.append("Actual OpenGL client did not report VISUAL_SMOKE_PASS")
    errors.extend(fatal_resource_lines(logs))
    digests = set()
    for name in CAPTURES:
        capture = REPORT / name
        try:
            width, height = png_size(capture)
            if width < 320 or height < 240:
                errors.append(f"Capture too small: {name}: {width}x{height}")
            digests.add(hashlib.sha256(capture.read_bytes()).digest())
            print(f"VISUAL_SMOKE_PNG {name} {width}x{height} {capture.stat().st_size} bytes")
        except (OSError, ValueError) as error:
            errors.append(str(error))
    if len(digests) != len(CAPTURES):
        errors.append("Gallery capture stages are missing or identical")
    print("\n".join(output.splitlines()[-(180 if errors else 35):]))
    if errors:
        print("Visual smoke FAILED:", file=sys.stderr)
        for error in dict.fromkeys(errors):
            print(f"  {error}", file=sys.stderr)
        return 1
    print(f"Visual smoke passed: {len(CAPTURES)} actual OpenGL captures in "
          f"{time.monotonic() - started:.1f}s. Report: {REPORT}")
    if args.emit_base64:
        for name in CAPTURES:
            emit_base64(REPORT / name)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
