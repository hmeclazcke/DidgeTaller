"""Summarize JVM coverage and explicitly reviewed Detekt/JaCoCo method matches.

Uses only the standard library. No coverage or CRAP thresholds are enforced.
"""

import argparse
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET


KINDS = ("LINE", "INSTRUCTION", "BRANCH", "METHOD")


def counters(element):
    return {c.attrib["type"]: {key: int(c.attrib[key]) for key in ("covered", "missed")}
            for c in element.findall("counter") if c.attrib["type"] in KINDS}


def combine(rows):
    return {kind: {key: sum(row.get(kind, {}).get(key, 0) for row in rows)
                   for key in ("covered", "missed")} for kind in KINDS}


def fraction(row, kind):
    count = row.get(kind, {"covered": 0, "missed": 0})
    total = count["covered"] + count["missed"]
    return count["covered"] / total if total else None


def percent(row, kind):
    value = fraction(row, kind)
    return "n/a" if value is None else f"{100 * value:.2f}%"


def summarize(xml_path, detekt_path, matches_path):
    report = ET.parse(xml_path).getroot()
    files, classes, metadata_only = {}, {}, []
    for package in report.findall("package"):
        for source in package.findall("sourcefile"):
            relative = package.attrib["name"] + "/" + source.attrib["name"]
            measured = counters(source)
            exists = any((Path("app/src/main") / root / relative).is_file() for root in ("java", "kotlin"))
            if not exists:
                if any(value for count in measured.values() for value in count.values()):
                    raise ValueError(f"Coverage attributed to a non-project source: {relative}")
                metadata_only.append(relative)  # Empty SMAP source references, not analyzed dependencies.
                continue
            files[relative] = measured
        for cls in package.findall("class"):
            classes[cls.attrib["name"]] = cls
    overall = counters(report)
    if combine(list(files.values())) != overall:
        raise ValueError("Source-file counters disagree with report totals")
    if fraction(overall, "LINE") is None:
        raise ValueError("Missing production coverage denominator")
    groups = {"Overall production": list(files),
              "Pure core": [f for f in files if f.startswith("ar/didgetaller/core/")],
              "Audio Android adapters": [f for f in files if f.startswith("ar/didgetaller/audio/")],
              "Session persistence (data package)": [f for f in files if f.startswith("ar/didgetaller/data/")]}
    for name in ("PitchDetector", "PitchTrace", "PitchStream", "LivePitch", "Pcm16WavReader",
                 "ProgressiveWavWriter", "WavCodec", "SessionAnalysis", "WavAnalysis", "PlotViewport",
                 "PitchAxis", "ExplorerState", "PlotSurface", "AcousticCharts", "PlotGestures"):
        selected = [f for f in files if f.endswith("/" + name + ".kt")]
        if len(selected) != 1:
            raise ValueError(f"Missing or ambiguous component: {name}")
        groups[name + ".kt"] = selected
    components = {name: combine([files[f] for f in selected]) for name, selected in groups.items()}
    metrics = json.loads(detekt_path.read_text(encoding="utf-8"))["functions_by_cyclomatic"]
    matches, unmatched = [], []
    for spec in json.loads(matches_path.read_text(encoding="utf-8")):
        source = Path(spec["source"])
        if not source.is_file() or hashlib.sha256(source.read_text(encoding="utf-8").encode("utf-8")).hexdigest() != spec["source_sha256_lf"]:
            unmatched.append(dict(spec, reason="Source changed; re-review correspondence before calculating a score."))
            continue
        cls = classes.get(spec["owner"])
        if cls is None or cls.attrib["sourcefilename"] != source.name:
            unmatched.append(dict(spec, reason="Class/source correspondence unavailable."))
            continue
        methods = [m for m in cls.findall("method") if m.attrib["name"] == spec["name"]
                   and m.attrib["desc"] == spec["descriptor"]
                   and int(m.attrib["line"]) == spec["bytecode_line"]]
        detections = [m for m in metrics if m["file"] == spec["source"]
                      and m["function"] == spec["name"] and m["line"] == spec["declaration_line"]]
        if len(methods) != 1 or len(detections) != 1:
            unmatched.append(dict(spec, reason="Unique reviewed method/Detekt correspondence unavailable."))
            continue
        measured = counters(methods[0])
        complexity = detections[0]["cyclomatic"]
        row = dict(spec, cyclomatic=complexity, coverage=measured)
        if any(fraction(measured, kind) is None for kind in ("LINE", "INSTRUCTION")):
            unmatched.append(dict(spec, reason="Matched method has no coverage denominator."))
            continue
        # Explicit approximations: neither line nor instruction coverage is basis-path coverage.
        for kind in ("LINE", "INSTRUCTION"):
            coverage = fraction(measured, kind)
            row["crap_" + kind.lower() + "_approximation"] = complexity ** 2 * (1 - coverage) ** 3 + complexity
        matches.append(row)
    return {"components": components, "source_files": files, "metadata_only_source_files": sorted(metadata_only),
            "reviewed_method_matches": matches, "unmatched_method_specs": unmatched,
            "crap_limit": "Reviewed pure methods only; line/instruction approximations, not basis-path CRAP."}


def markdown(summary):
    lines = ["# JVM coverage measurements", "", "| Component (whole source files) | Lines | Instructions | Branches | Methods |",
             "| --- | ---: | ---: | ---: | ---: |"]
    for name, measured in summary["components"].items():
        lines.append("| " + name + " | " + " | ".join(percent(measured, k) for k in KINDS) + " |")
    lines += ["", "Only JVM test execution is included. Android-dependent zeros are retained.", "",
              "## Reviewed pure-method CRAP approximations", "",
              "| Method | Detekt MCC | Method lines | Method branches | CRAP-L | CRAP-I |",
              "| --- | ---: | ---: | ---: | ---: | ---: |"]
    for row in summary["reviewed_method_matches"]:
        name = row["owner"].rsplit("/", 1)[-1] + "." + row["name"]
        lines.append(f"| {name} | {row['cyclomatic']} | {percent(row['coverage'], 'LINE')} | "
                     f"{percent(row['coverage'], 'BRANCH')} | {row['crap_line_approximation']:.3f} | "
                     f"{row['crap_instruction_approximation']:.3f} |")
    lines += ["", summary["crap_limit"], "No project-wide CRAP score or coverage gate.", ""]
    for row in summary["unmatched_method_specs"]:
        lines.append(f"Score omitted for {row['owner']}.{row['name']}: {row['reason']}")
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--xml", type=Path, default=Path("app/build/reports/jacoco/debug/coverage.xml"))
    parser.add_argument("--detekt", type=Path, default=Path("build/reports/detekt/summary.json"))
    parser.add_argument("--matches", type=Path, default=Path("config/jacoco/method-matches.json"))
    args = parser.parse_args()
    summary = summarize(args.xml, args.detekt, args.matches)
    output = args.xml.parent
    (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    rendered = markdown(summary)
    (output / "summary.md").write_text(rendered, encoding="utf-8")
    print(rendered)


if __name__ == "__main__":
    main()
