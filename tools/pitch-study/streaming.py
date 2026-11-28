"""Evaluate official streaming PESTO on contiguous PCM at its own and the app's cadence."""
import csv
import math
from collections import defaultdict
from pathlib import Path
import time

import numpy as np
import pesto
import torch

from neural import ROOT, check_weight


def first_correct(times, good, confirm=False):
    return next((t for i, (t, ok) in enumerate(zip(times, good))
                 if t >= 0 and ok and (not confirm or i > 0 and times[i-1] >= 0 and good[i-1])), None)


def prechange_prefix(frame, relative_boundary, hop, old):
    """Restore the omitted start of the synthetic generator, checking it against observed PCM."""
    def samples(indices):
        phase = 2 * np.pi * old * indices / 44100
        return np.rint(12000 * (np.sin(phase) + .25 * np.sin(2 * phase))).astype(np.int16)

    candidates = []
    for end in range(len(frame) + hop - 4096, 49152 + hop, hop):
        start = end - len(frame)
        if not 40960 <= start + relative_boundary < 49152:
            continue
        check = samples(np.arange(start, start + relative_boundary))
        if np.array_equal(frame[:relative_boundary], check):
            candidates.append(start)
    if len(candidates) != 1:
        raise ValueError("Could not verify the pretransition PCM origin")
    return samples(np.arange(candidates[0]))


@torch.inference_mode()
def evaluate():
    torch.set_num_threads(1)
    torch.set_num_interop_threads(1)
    check_weight(Path(pesto.__file__).parent / "weights/mir-1k_g7.ckpt", "pesto")
    groups = defaultdict(list)
    with (ROOT/"test.csv").open() as file:
        for row in csv.DictReader(file):
            if row["estimator"] == "YIN" and row["category"].startswith("jump"):
                groups[row["profile"], row["category"], row["seed"]].append(row)
    with (ROOT/"test.pcm").open("rb") as pcm, (ROOT/"streaming.csv").open("w", newline="") as out:
        writer = csv.writer(out)
        writer.writerow(["profile","event","group","prechange_correct","first_native_ms","confirmed_native_ms",
                         "first_app_ms","confirmed_app_ms","hops","cpu_ms_per_hop","wall_ms_per_hop"])
        for (profile, event, seed), frames in groups.items():
            chunks = []
            hop = 8192 if profile == "DIDGERIDOO" else 4096
            for r in frames:
                pcm.seek(int(r["offset"]))
                frame = np.frombuffer(pcm.read(2*int(r["samples"])), dtype="<i2")
                if chunks:
                    assert np.array_equal(previous[hop:], frame[:-hop])
                chunks.append(frame if not chunks else frame[-hop:])
                previous = frame
            boundary = int(r["samples"]) - round(float(frames[0]["elapsed_ms"])*44.1)
            target = float(r["target_hz"])
            old = target / float(event.removeprefix("jump"))
            prefix = prechange_prefix(chunks[0], boundary, hop, old)
            boundary += len(prefix)
            audio = torch.from_numpy(np.concatenate([prefix, *chunks]).astype("float32") / 32768)
            net = pesto.load_model("mir-1k_g7", step_size=10., sampling_rate=44100,
                                   streaming=True, mirror=0., max_batch_size=1).eval()
            times, good, before = [], [], []
            cpu, start = time.process_time(), time.perf_counter()
            for end in range(441, len(audio)+1, 441):
                chunk = audio[end-441:end][None, :]
                hz, score, _ = net(chunk, 44100, convert_to_freq=True, return_activations=False)
                value = hz.item()
                accepted = score.item() >= .5 and chunk.std(unbiased=False).item() >= .005
                t = (end-boundary) / 44.1
                times.append(t)
                good.append(accepted and abs(1200*math.log2(value/target)) <= 50)
                if -40 <= t < 0:
                    before.append(accepted and abs(1200*math.log2(value/old)) <= 50)
            wall_ms = 1000*(time.perf_counter()-start)/len(times)
            cpu_ms = 1000*(time.process_time()-cpu)/len(times)
            delivered = [float(r["elapsed_ms"]) for r in frames]
            latest = [good[(boundary+round(t*44.1))//441-1] for t in delivered]
            writer.writerow([profile,event,seed,bool(before) and all(before),first_correct(times,good),
                             first_correct(times,good,True),first_correct(delivered,latest),
                             first_correct(delivered,latest,True),len(times),cpu_ms,wall_ms])


if __name__ == "__main__":
    evaluate()
