"""Summarize opt-in :app:pitchStudy results using only the standard library."""
import argparse
import csv
import math
import statistics
from collections import defaultdict
from pathlib import Path

DIRECTORY = Path("build/reports/pitch-study")


def percentile95(values):
    return sorted(values)[math.ceil(.95 * len(values)) - 1] if values else None


def metrics(rows):
    hz_errors, cent_errors = [], []
    voiced = negative = false_positive = unlabelled = octave = 0
    for row in rows:
        target, hz = float(row["target_hz"]), float(row["estimated_hz"])
        if not math.isfinite(target):
            unlabelled += 1
        elif target == 0:
            negative += 1
            false_positive += hz > 0
        else:
            voiced += 1
            if hz > 0:
                cents = abs(1200 * math.log2(hz / target))
                hz_errors.append(abs(hz - target))
                cent_errors.append(cents)
                multiple = round(cents / 1200)
                octave += multiple >= 1 and abs(cents - multiple * 1200) <= 50
    return dict(voiced=voiced, detected=len(hz_errors), miss=voiced - len(hz_errors),
                **{f"within{t}": sum(c <= t for c in cent_errors) for t in (10, 25, 50)},
                octave=octave, negative=negative, false_positive=false_positive, unlabelled=unlabelled,
                median_hz=statistics.median(hz_errors) if hz_errors else None, p95_hz=percentile95(hz_errors),
                median_cents=statistics.median(cent_errors) if cent_errors else None, p95_cents=percentile95(cent_errors))


def summarize(split):
    with (DIRECTORY / f"{split}.csv").open() as source:
        rows = list(csv.DictReader(source))
    groups = defaultdict(list)
    transitions = defaultdict(list)
    for row in rows:
        profile, rate, method = (row[k] for k in ("profile", "rate", "estimator"))
        if row["category"] == "register":
            transitions[profile, method, row["seed"]].append(row)
            continue
        target = float(row["target_hz"])
        for section in ("all", "category/" + row["category"]):
            groups[rate, profile, method, section].append(row)
        if target > 0:
            band = next(b for b in (50, 100, 150, 400, 800, 1700) if target < b)
            groups[rate, profile, method, f"band/upper-{band}"].append(row)
            if float(row["estimated_hz"]) > 0:
                strength = float(row["strength"])
                low = next(v for v in (.95, .80, .65, 0) if strength >= v)
                groups[rate, profile, method, f"strength/from-{low}"].append(row)
    with (DIRECTORY / f"{split}-summary.csv").open("w", newline="") as out:
        writer = csv.writer(out)
        writer.writerow(["rate", "profile", "estimator", "group", *metrics([])])
        for key, samples in sorted(groups.items()):
            writer.writerow([*key, *metrics(samples).values()])
    with (DIRECTORY / f"{split}-transitions.csv").open("w", newline="") as out:
        writer = csv.writer(out)
        writer.writerow(["profile", "estimator", "offset", "first_correct_ms", "confirmed_ms"])
        for key, sequence in sorted(transitions.items()):
            correct = [metrics([row])["within50"] for row in sequence]
            first = next((row["elapsed_ms"] for ok, row in zip(correct, sequence) if ok), None)
            confirmed = next((sequence[i]["elapsed_ms"] for i in range(1, len(correct))
                              if correct[i - 1] and correct[i]), None)
            writer.writerow([*key, first, confirmed])


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("split", choices=["development", "held", "public"])
    summarize(parser.parse_args().split)
