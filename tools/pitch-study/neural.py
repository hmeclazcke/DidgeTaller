"""One CPU-only model per process. Read only the bench generator's explicit PCM pack."""
import argparse
import csv
import hashlib
import json
import math
from pathlib import Path
import time

import numpy as np
import psutil
import torch

ROOT = Path("build/reports/pitch-study/phase2")
WEIGHTS = {
    "tiny": "d4993eea36ed1a0ad9ac549c740dae5265b049ce72004f00c2f59e01c0be8432",
    "full": "133225604dedd2e4005f8bbd1bd0a2ec073ba8b7a6cd31ff6d5edbbfa3539986",
    "pesto": "16c32e06ddd950e3e4866dfa3c7f8a87c4988f8adf43e57977b189f031f26f3e",
}


def check_weight(path, model):
    if hashlib.sha256(path.read_bytes()).hexdigest() != WEIGHTS[model]:
        raise ValueError(f"Unexpected weights for {model}")


def load(model):
    if model == "pesto":
        import pesto
        check_weight(Path(pesto.__file__).parent / "weights/mir-1k_g7.ckpt", model)
        net = pesto.load_model("mir-1k_g7", step_size=10., sampling_rate=44100).eval()

        def infer(x):
            hz, score, _ = net(x, 44100, convert_to_freq=True, return_activations=False)
            center = round(x.shape[-1] / (2 * 441))
            return hz[0, center].item(), score[0, center].item()

    else:
        import torchaudio
        import torchcrepe
        check_weight(Path(torchcrepe.__file__).parent / f"assets/{model}.pth", model)
        resample = torchaudio.transforms.Resample(44100, 16000)

        def infer(x):
            tail = resample(x)[:, -1024:]
            hz, score = torchcrepe.predict(tail, 16000, 160, 32, 2006, model,
                                           decoder=torchcrepe.decode.weighted_argmax,
                                           return_periodicity=True, pad=False)
            return hz.item(), score.item()
    return infer


@torch.inference_mode()
def evaluate(model, splits):
    torch.set_num_threads(1)
    torch.set_num_interop_threads(1)
    torch.manual_seed(291)
    np.random.seed(291)
    infer = load(model)
    proc = psutil.Process()
    # Warm both input dimensions. Count positive native tensor allocations for one inference,
    # independently of the timing pass; this is not process-wide allocation traffic.
    allocations = {}
    for n in (8192, 16384):
        probe = torch.sin(torch.arange(n) * (2 * math.pi * 68 / 44100))[None, :]
        for _ in range(4):
            infer(probe)
        with torch.profiler.profile(activities=[torch.profiler.ProfilerActivity.CPU], profile_memory=True) as profile:
            infer(probe)
        allocations[n] = sum(max(0, e.self_cpu_memory_usage) for e in profile.events())
    rss = proc.memory_info().rss
    for split in splits:
        with (ROOT / f"{split}.csv").open() as f:
            rows = [r for r in csv.DictReader(f) if r["estimator"] == "YIN"]
        with (ROOT / f"{split}.pcm").open("rb") as pcm, (ROOT / f"{split}-{model}.csv").open("w", newline="") as out:
            writer = csv.DictWriter(out, fieldnames=[*rows[0], "native_hz", "rms", "in_range"])
            writer.writeheader()
            for r in rows:
                n = int(r["samples"])
                assert pcm.tell() == int(r["offset"])
                samples = np.frombuffer(pcm.read(2*n), dtype="<i2").astype(np.float32) / 32768
                rms = float(samples.std())
                x = torch.from_numpy(samples)[None, :]
                cpu, start = time.process_time_ns(), time.perf_counter_ns()
                hz, strength = infer(x)
                wall = time.perf_counter_ns() - start
                used = time.process_time_ns() - cpu
                assert math.isfinite(hz) and math.isfinite(strength)
                flute = r["profile"] == "NATIVE_AMERICAN_FLUTE"
                in_range = (199 if flute else 35 * .95) <= hz <= int(r["max_hz"]) * (1.005 if flute else 1.05)
                # Fixed research operating point. Keep native outputs for evaluation at other thresholds.
                accepted = in_range and rms >= .005 and strength >= .5
                writer.writerow(dict(r, estimator=model, estimated_hz=hz if accepted else 0,
                                     native_hz=hz, rms=rms, in_range=int(in_range), strength=strength,
                                     wall_ns=wall, cpu_ns=used, bytes=allocations[n]))
    print(json.dumps(dict(model=model, allocated_tensor_bytes=allocations, warm_process_rss=rss,
                          final_process_rss=proc.memory_info().rss, weight_sha256=WEIGHTS[model])))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("model", choices=WEIGHTS)
    parser.add_argument("splits", nargs="+", choices=["dev", "test", "old", "flute"])
    args = parser.parse_args()
    evaluate(args.model, args.splits)
