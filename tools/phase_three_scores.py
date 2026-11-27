"""Paired source-attribution, coverage and causal results; only reads the research CSVs."""
import csv
import math
import statistics as st
from collections import defaultdict
from itertools import groupby

from phase_two_scores import calibration, clustered_difference, describe, write_rows
from pitch_study import DIRECTORY, percentile95

ROOT = DIRECTORY / "phase3"


def correct(row):
    hz, ref = float(row["estimated_hz"]), float(row["target_hz"])
    return hz > 0 and ref > 0 and abs(1200 * math.log2(hz / ref)) <= 50


def subharmonic(row):
    """An assertion below every known oscillator by more than the pitch tolerance."""
    return 0 < float(row["estimated_hz"]) < float(row["lowest_line"]) * 2**(-50/1200)


def matched_correct(rows, budget):
    accepted = sorted((r for r in rows if float(r["estimated_hz"]) > 0),
                      key=lambda r: float(r["strength"]), reverse=True)
    assert budget <= len(accepted)
    expected = 0.
    for _, ties in groupby(accepted, key=lambda r: float(r["strength"])):
        block = list(ties)
        kept = min(budget, len(block))
        expected += kept * sum(map(correct, block)) / len(block)
        budget -= kept
        if budget == 0:
            break
    return expected


def zero_noise_threshold(rows):
    ranks = [float(r["strength"]) for r in rows if float(r["target_hz"]) == 0 and float(r["estimated_hz"]) > 0]
    return math.nextafter(max(ranks), math.inf) if ranks else 0.


def load(split):
    groups = defaultdict(list)
    with (ROOT / f"{split}.csv").open() as file:
        for r in csv.DictReader(file):
            groups[r["profile"], r["estimator"]].append(r)
            if r["estimator"] == "SEPARATE":
                groups[r["profile"], "HYPOTHESIS"].append(dict(r, estimated_hz=r["hypothesis"], strength=r["rank"]))
    return groups


def static(rows):
    return [r for r in rows if float(r["elapsed_ms"]) == 0]


def retained(rows, threshold):
    return [r for r in rows if float(r["estimated_hz"]) > 0 and float(r["strength"]) >= threshold]


def temporal_ablation():
    held = load("held")
    with (ROOT / "ablation.csv").open() as source:
        fresh = {r["id"]: r for r in csv.DictReader(source)}
    table = []
    for (profile, method), rows in held.items():
        if method != "CONTEXT":
            continue
        size = 16384 if profile == "DIDGERIDOO" else 8192
        for name in ("articulation", "glide", "register", "gap", "articulation_full_window"):
            sample = [r for r in rows if r["category"] == name or
                      (name == "articulation_full_window" and r["category"] == "articulation" and
                       float(r["elapsed_ms"]) * int(r["rate"]) >= size * 1000)]
            reset = [dict(r, **fresh[r["id"]]) for r in sample]
            table.append(dict(profile=profile, category=name, n=len(sample),
                              correct_history=sum(map(correct, sample)), correct_reset=sum(map(correct, reset)),
                              gained=sum(correct(a) and not correct(b) for a,b in zip(sample,reset)),
                              lost=sum(correct(b) and not correct(a) for a,b in zip(sample,reset)),
                              emitted_history=len(retained(sample, 0)), emitted_reset=len(retained(reset, 0))))
    write_rows(ROOT / "context.csv", table)


def period_proposals():
    table = []
    for split in ("held", "frozen"):
        with (ROOT / f"{split}-periods.csv").open() as source:
            proposals = {r["id"]: r for r in csv.DictReader(source)}
        for (profile, method), rows in load(split).items():
            if method != "PERIOD":
                continue
            actual = [dict(r, **proposals[r["id"]]) for r in rows]
            table.append(dict(split=split, profile=profile, frames=len(rows),
                              identical_hz=sum(a["estimated_hz"] == b["estimated_hz"] for a,b in zip(rows,actual)),
                              **describe(static(actual))))
    write_rows(ROOT / "period-proposals.csv", table)


def run():
    dev = load("dev")
    totals, operating, costs, events, uncertainty = [], [], [], [], []
    for split in ("held", "frozen", "legacy", "flute"):
        groups = load(split)
        for (profile, method), rows in groups.items():
            key = dict(split=split, profile=profile, method=method)
            still = static(rows)
            categories = defaultdict(list)
            categories["all_static"] = still
            for r in rows:
                categories[r["category"]].append(r)
            for r in still:
                categories["rate"+r["rate"]].append(r)
            reference = {r["id"]: r for r in groups[profile, "YIN"]}
            assert len(rows) == len(reference) and all(r["id"] in reference for r in rows)
            for category, sample in categories.items():
                ambiguous = [r for r in sample if math.isnan(float(r["target_hz"])) and math.isfinite(float(r["lowest_line"]))]
                totals.append(dict(key, category=category, **describe(sample), source_controls=len(ambiguous),
                                   below_sources=sum(map(subharmonic, ambiguous)),
                                   changed_below=sum(subharmonic(r) and not correct(dict(r, target_hz=reference[r["id"]]["estimated_hz"]))
                                                     for r in ambiguous),
                                   newly_below=sum(subharmonic(r) and not subharmonic(reference[r["id"]]) for r in ambiguous)))
            if split in ("held", "frozen"):
                known = [r for r in still if float(r["target_hz"]) > 0]
                training = static(dev[profile, method])
                common = min(sum(float(r["estimated_hz"]) > 0 and float(r["target_hz"]) > 0
                                 for r in static(other)) for (p, _), other in groups.items() if p == profile)
                lo, hi = clustered_difference(still, reference)
                uncertainty.append(dict(key, common=common, expected_correct=matched_correct(known, common),
                                        gain_low=lo, gain_high=hi, **calibration(training, still)))
                thresholds = {"raw": 0, "75": .75, "90": .90, "95": .95, "99": .99,
                              "dev_no_noise": zero_noise_threshold(training)}
                if split == "held":
                    thresholds["matched_no_noise"] = zero_noise_threshold(still)
                for rule, threshold in thresholds.items():
                    selected = retained(still, threshold)
                    n = sum(float(r["target_hz"]) > 0 for r in selected)
                    hit = sum(map(correct, selected))
                    operating.append(dict(key, rule=rule, threshold=threshold, emitted=n, correct=hit,
                                          coverage=n / len(known), accuracy=hit/n if n else None,
                                          noise=sum(float(r["target_hz"]) == 0 for r in selected),
                                          below=sum(subharmonic(r) for r in selected if math.isnan(float(r["target_hz"])))))
            if split != "held":
                continue
            for seed in {r["seed"] for r in rows}:
                jump = [r for r in categories["register"] if r["seed"] == seed]
                first = next((float(r["elapsed_ms"]) for r in jump if correct(r)), None)
                confirmed = next((float(r["elapsed_ms"]) for i,r in enumerate(jump) if i and correct(r) and correct(jump[i-1])), None)
                events.append(dict(key, seed=seed, first=first, confirmed=confirmed))
            if method != "HYPOTHESIS":
                wall = [float(r["wall_ns"])/1e6 for r in rows]
                costs.append(dict(key, wall_ms=st.median(wall), p95_ms=percentile95(wall),
                                  cpu_ms=st.mean(float(r["cpu_ns"]) for r in rows)/1e6,
                                  bytes=st.mean(float(r["bytes"]) for r in rows)))
    for name, table in (("scores",totals),("operating",operating),("cost",costs),("events",events),("paired",uncertainty)):
        write_rows(ROOT / f"{name}.csv", table)
    temporal_ablation()
    period_proposals()


if __name__ == "__main__":
    run()
