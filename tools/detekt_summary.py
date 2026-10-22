"""Summarize Detekt SARIF without mixing audit findings and metric observations.

Run after Gradle's detekt and detektComplexity tasks. Uses only Python's stdlib.
"""

import argparse
from collections import Counter
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET


def results(path):
    report = json.loads(path.read_text(encoding="utf-8"))
    return [item for run in report["runs"] for item in run["results"]]


def location(item):
    physical = item["locations"][0]["physicalLocation"]
    return physical["artifactLocation"]["uri"], physical["region"]["startLine"]


def summarize(directory):
    findings = results(directory / "detekt.sarif")
    # An independent output format guards against an incomplete SARIF summary.
    xml = ET.parse(directory / "detekt.xml")
    if len(xml.findall(".//error")) != len(findings):
        raise ValueError("SARIF and Checkstyle finding counts disagree")
    functions = {}
    for item in results(directory / "complexity.sarif"):
        rule = item["ruleId"].rsplit(".", 1)[-1]
        if rule not in ("CyclomaticComplexMethod", "CognitiveComplexMethod"):
            raise ValueError(f"Unexpected metric rule: {rule}")
        match = re.search(r"The function (.+?) appears.*\(complexity: (\d+)\)",
                          item["message"]["text"])
        if match is None:
            raise ValueError("Unrecognized Detekt complexity message")
        path, line = location(item)
        key = (path, line, match[1])
        entry = functions.setdefault(key, {"file": path, "line": line, "function": match[1]})
        metric = "cyclomatic" if rule == "CyclomaticComplexMethod" else "cognitive"
        if metric in entry:
            raise ValueError(f"Duplicate {metric} measurement for {key}")
        entry[metric] = int(match[2])
    if not functions or any("cyclomatic" not in f or "cognitive" not in f for f in functions.values()):
        raise ValueError("Missing paired complexity measurements")
    ranked = sorted(functions.values(), key=lambda f: (-f["cyclomatic"], -f["cognitive"], f["file"], f["line"]))
    grouped = Counter((f.get("level", "warning"), f["ruleId"].rsplit(".", 1)[-1]) for f in findings)
    return {
        "total_findings": len(findings),
        "by_severity": dict(sorted(Counter(f.get("level", "warning") for f in findings).items())),
        "by_severity_and_rule": [{"severity": s, "rule": r, "count": n}
                                 for (s, r), n in sorted(grouped.items(), key=lambda row: (-row[1], row[0]))],
        "measured_functions": len(ranked),
        "functions_by_cyclomatic": ranked,
    }


def markdown(summary):
    lines = ["# Detekt measurements", "",
             f"Audit findings: **{summary['total_findings']}**. Metric observations are excluded.", "",
             "| SARIF severity | Rule | Findings |", "| --- | --- | ---: |"]
    for row in summary["by_severity_and_rule"]:
        lines.append(f"| {row['severity']} | {row['rule']} | {row['count']} |")
    lines += ["", "Top 10 by cyclomatic complexity (ties: cognitive, path, line).", "",
              "| Function | Source | Cyclomatic | Cognitive |", "| --- | --- | ---: | ---: |"]
    for f in summary["functions_by_cyclomatic"][:10]:
        lines.append(f"| {f['function']} | {f['file']}:{f['line']} | {f['cyclomatic']} | {f['cognitive']} |")
    lines += ["", f"Paired metrics available for {summary['measured_functions']} named functions.",
              "Nested local functions contribute to enclosing-function metrics; they are not separately ranked by these rules.",
              "No coverage or CRAP score was calculated.", ""]
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reports", type=Path, default=Path("build/reports/detekt"))
    args = parser.parse_args()
    summary = summarize(args.reports)
    (args.reports / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    report = markdown(summary)
    (args.reports / "summary.md").write_text(report, encoding="utf-8")
    print(report)


if __name__ == "__main__":
    main()
