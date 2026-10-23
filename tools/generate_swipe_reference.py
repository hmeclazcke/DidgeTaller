"""Generate synthetic golden data with the independently compiled legacy SPTK C library.

Usage: python tools/generate_swipe_reference.py path/to/swipe.dll output.tsv
No audio files or dependencies are downloaded. Test PCM generation is shared by contract
with SwipeNativeTest (integer truncation of 9000 * harmonic sum, 44100 Hz, two seconds).
"""
import ctypes
import math
import sys
from pathlib import Path

CASES = [
    ("pure35", 35, [1], False), ("pure70", 70, [1], False),
    ("pure140", 140, [1], False), ("missing35", 35, [0, 1, 0, 0, .6], False),
    ("weak35", 35, [.03, 1, .3], False),
    ("silence", 70, [0], False),
]


def generate(library, output):
    lib = ctypes.CDLL(str(Path(library).resolve()))
    pointer = ctypes.POINTER(ctypes.c_double)
    lib.swipe.argtypes = [pointer, pointer, ctypes.c_int, ctypes.c_int, ctypes.c_int,
                         ctypes.c_double, ctypes.c_double, ctypes.c_double, ctypes.c_int]
    lib.swipe.restype = None
    lines = ["case\tcenter_frame\thz"]
    for name, f0, harmonics, flute in CASES:
        pcm = [int(9000 * sum(a * math.sin(2 * math.pi * f0 * (h + 1) * i / 44100)
                             for h, a in enumerate(harmonics))) for i in range(88200)]
        offset, hop = (0, 4096) if flute else (4096, 8192)
        data = (ctypes.c_double * (len(pcm) - offset))(*pcm[offset:])
        result = (ctypes.c_double * math.ceil(len(data) / hop))()
        lib.swipe(data, result, len(data), 44100, hop, 200 if flute else 35,
                  1600 if flute else 140, .3, 1)
        for center in range(offset + hop, len(pcm) - hop + 1, hop):
            lines.append(f"{name}\t{center}\t{result[(center-offset)//hop]:.12f}")
    Path(output).write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    generate(*sys.argv[1:])
