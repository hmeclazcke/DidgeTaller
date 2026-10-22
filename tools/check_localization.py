"""Validate app resource parity and positional formats; no external dependencies."""

from collections import Counter
from pathlib import Path
import re
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
POSITIONAL = re.compile(r"%(\d+)\$[-+ 0,(]*\d*(?:\.\d+)?([sdf])")


def signature(value):
    return Counter((int(index), kind) for index, kind in POSITIONAL.findall(value))


def read(folder):
    result = {}
    for item in ET.parse(RES / folder / "strings.xml").getroot():
        name = item.attrib["name"]
        assert name not in result, f"Duplicate resource: {folder}/{name}"
        if item.tag == "plurals":
            value = {child.attrib["quantity"]: child.text or "" for child in item}
            assert {"one", "other"} <= value.keys(), f"Missing quantity: {name}"
        elif item.tag == "string-array":
            value = [child.text or "" for child in item]
        else:
            value = item.text or ""
            assert value.strip(), f"Empty resource: {folder}/{name}"
        result[name] = (item.tag, value)
    return result


def main():
    english, spanish = read("values"), read("values-es")
    assert english.keys() == spanish.keys(), "English/Spanish resource keys differ"
    for name, (kind, value) in english.items():
        translated_kind, translated = spanish[name]
        assert kind == translated_kind, f"Different resource types: {name}"
        if kind == "string":
            assert signature(value) == signature(translated), f"Format mismatch: {name}"
        elif kind == "plurals":
            assert value.keys() == translated.keys(), f"Quantity mismatch: {name}"
            for quantity in value:
                assert signature(value[quantity]) == signature(translated[quantity]), f"Plural format mismatch: {name}/{quantity}"
        else:
            assert len(value) == len(translated), f"Array length mismatch: {name}"
    declared = {item.attrib["{http://schemas.android.com/apk/res/android}name"] for item in ET.parse(RES / "xml/locales_config.xml").getroot()}
    assert declared == {"en", "es"}, "Unexpected supported languages"
    sources = list((ROOT / "app/src/main/java").rglob("*.kt"))
    references = set()
    for source in sources:
        references.update(re.findall(r"(?<!android\.)R\.(?:string|array|plurals)\.(\w+)", source.read_text(encoding="utf-8")))
    assert references <= english.keys(), f"Unknown resource references: {references - english.keys()}"
    counts = Counter(kind for kind, _ in english.values())
    print(f"Localization parity OK: {counts['string']} strings, {counts['plurals']} plurals, {counts['string-array']} arrays; en/es and positional formats agree.")


if __name__ == "__main__":
    main()
