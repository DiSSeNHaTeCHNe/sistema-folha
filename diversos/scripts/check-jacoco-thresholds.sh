#!/usr/bin/env bash
# Verifica limiares JaCoCo por domínio após `cd backend && mvn test`.
# Exit 0 se todos os limiares forem atendidos; exit 1 caso contrário.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
JACOCO_XML="${JACOCO_XML:-$ROOT/backend/target/site/jacoco/jacoco.xml}"

if [[ ! -f "$JACOCO_XML" ]]; then
  echo "ERROR: JaCoCo report not found at $JACOCO_XML" >&2
  echo "Run: cd backend && mvn test" >&2
  exit 1
fi

python3 - "$JACOCO_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1]
root = ET.parse(path).getroot()

thresholds = {
    "organograma": 50.0,
    "security": 40.0,
    "importacao": 75.0,
    "global": 75.0,
}

def domain_lines(key: str) -> tuple[int, int]:
    covered = missed = 0
    for pkg in root.findall("package"):
        name = pkg.get("name", "")
        if key != "global" and key not in name:
            continue
        if key == "global":
            continue
        counter = next((c for c in pkg.findall("counter") if c.get("type") == "LINE"), None)
        if counter is not None:
            covered += int(counter.get("covered", 0))
            missed += int(counter.get("missed", 0))
    return covered, missed

def global_lines() -> tuple[int, int]:
    counter = next((c for c in root.findall("counter") if c.get("type") == "LINE"), None)
    if counter is None:
        return 0, 0
    covered = int(counter.get("covered", 0))
    missed = int(counter.get("missed", 0))
    return covered, missed

failed = False
print("JaCoCo domain thresholds:")
for key in ("organograma", "security", "importacao", "global"):
    covered, missed = global_lines() if key == "global" else domain_lines(key)
    total = covered + missed
    pct = (100.0 * covered / total) if total else 0.0
    min_pct = thresholds[key]
    status = "PASS" if pct >= min_pct else "FAIL"
    if status == "FAIL":
        failed = True
    print(f"  {key:12} {pct:5.1f}% ({covered}/{total})  min {min_pct:.0f}%  [{status}]")

if failed:
    sys.exit(1)
PY
