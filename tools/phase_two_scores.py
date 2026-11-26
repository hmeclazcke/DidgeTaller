"""Score paired F0 hypotheses; keep ambiguous source labels out of pitch accuracy."""
import argparse
import csv
import math
import random
import statistics
from collections import defaultdict

from pitch_study import DIRECTORY, metrics

ROOT = DIRECTORY / "phase2"


def describe(rows):
    result = metrics(rows)
    for ratio in (3, 5):
        result[f"ratio{ratio}"] = sum(abs(abs(1200 * math.log2(float(r["estimated_hz"]) / float(r["target_hz"])))
                                           - 1200 * math.log2(ratio)) <= 50 for r in rows
                                      if float(r["estimated_hz"]) > 0 and float(r["target_hz"]) > 0)
    result["unlabelled_emitted"] = sum(math.isnan(float(r["target_hz"])) and float(r["estimated_hz"]) > 0
                                       for r in rows)
    return result


def curve(rows, threshold):
    voiced = [r for r in rows if float(r["target_hz"]) > 0]
    retained = []
    for r in voiced:
        hz = float(r["estimated_hz"])
        if "native_hz" in r and int(r["in_range"]) and float(r["rms"]) >= .005:
            hz = float(r["native_hz"])
        if hz > 0 and float(r["strength"]) >= threshold:
            retained.append(dict(r, estimated_hz=hz))
    return dict(threshold=threshold, coverage=len(retained)/len(voiced) if voiced else None,
                accuracy=metrics(retained)["within50"]/len(retained) if retained else None)


def calibration(train, test):
    """Laplace-smoothed bin estimates fitted only to accepted, labelled development trials."""
    edges = (.25, .5, .75, .9, .95, .99, math.inf)
    def examples(rows):
        for r in rows:
            if float(r["estimated_hz"]) > 0 and float(r["target_hz"]) > 0 and float(r["elapsed_ms"]) == 0:
                s = float(r["strength"])
                yield next(i for i, edge in enumerate(edges) if s < edge), s, metrics([r])["within50"]
    bins = defaultdict(list)
    training = list(examples(train))
    for index, _, ok in training:
        bins[index].append(ok)
    prior = (sum(ok for _, _, ok in training) + 1) / (len(training) + 2)
    errors = []
    for index, score, ok in examples(test):
        values = bins[index]
        pred = (sum(values)+1)/(len(values)+2) if values else prior
        errors.append(((prior-ok)**2, (score-ok)**2, (pred-ok)**2))
    return dict(n=len(errors), **dict(zip(("constant", "raw", "dev_bins"), map(statistics.mean, zip(*errors)))))


def clustered_difference(samples, reference):
    """Paired bootstrap of entire synthesized pitch groups, conditional on this generator."""
    groups = defaultdict(list)
    for row in samples:
        if float(row["target_hz"]) > 0:
            difference = metrics([row])["within50"] - metrics([reference[row["id"]]])["within50"]
            groups[row["seed"]].append(difference)
    totals = [(sum(g), len(g)) for g in groups.values()]
    rng = random.Random(7119)
    draws = []
    for _ in range(2000):
        sample = rng.choices(totals, k=len(totals))
        draws.append(100 * sum(n for n, _ in sample) / sum(d for _, d in sample))
    draws.sort()
    return [draws[49], draws[1949]]


def write_rows(path, rows):
    if rows:
        with path.open("w", newline="") as file:
            writer = csv.DictWriter(file, fieldnames=rows[0])
            writer.writeheader()
            writer.writerows(rows)


def summarize(split):
    by_method = defaultdict(list)
    for suffix in ("", "-tiny", "-full", "-pesto"):
        with (ROOT / f"{split}{suffix}.csv").open() as file:
            for r in csv.DictReader(file):
                by_method[r["profile"], r["estimator"]].append(r)
    scores, coverage, changes, paired, costs, calibrated = [], [], [], [], [], []
    for (profile, method), rows in by_method.items():
        key = dict(profile=profile, estimator=method)
        reference = {r["id"]: r for r in by_method[profile, "YIN"]}
        assert {r["id"] for r in rows} == set(reference) and len(rows) == len(reference)
        static = [r for r in rows if float(r["elapsed_ms"]) == 0]
        categories = defaultdict(list)
        for r in static:
            categories[r["category"]].append(r)
        for group, samples in [("all", static), *categories.items()]:
            scores.append(dict(key, group=group, **describe(samples)))
        for threshold in (0, .25, .5, .75, .9, .95, .99):
            coverage.append(dict(key, **curve(static, threshold)))
        if split == "test":
            lo, hi = clustered_difference(static, reference)
            paired.append(dict(key, low_pp=lo, high_pp=hi))
            suffix = "-" + method if method in ("tiny", "full", "pesto") else ""
            with (ROOT / f"dev{suffix}.csv").open() as dev:
                train = [r for r in csv.DictReader(dev) if r["estimator"] == method and r["profile"] == profile]
            calibrated.append(dict(key, **calibration(train, static)))
        events = defaultdict(list)
        for r in rows:
            if float(r["elapsed_ms"]) > 0:
                events[r["category"], r["seed"]].append(r)
        for (category, seed), frames in events.items():
            correct = [metrics([r])["within50"] for r in frames]
            first = next((r["elapsed_ms"] for r, ok in zip(frames, correct) if ok), None)
            confirmed = next((frames[i]["elapsed_ms"] for i in range(1, len(frames))
                              if correct[i-1] and correct[i]), None)
            changes.append(dict(key, category=category, seed=seed, first=first, confirmed=confirmed))
        wall = sorted(float(r["wall_ns"]) / 1e6 for r in rows)
        costs.append(dict(key, median_ms=statistics.median(wall), p95_ms=wall[math.ceil(len(wall)*.95)-1],
                          mean_cpu_ms=statistics.mean(float(r["cpu_ns"]) for r in rows)/1e6,
                          mean_bytes=statistics.mean(float(r["bytes"]) for r in rows)))
    for name, table in (("scores", scores), ("selective", coverage), ("events", changes),
                        ("paired", paired), ("costs", costs), ("calibration", calibrated)):
        write_rows(ROOT / f"{split}-{name}.csv", table)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("split", choices=("dev", "test", "old", "flute"))
    summarize(parser.parse_args().split)
