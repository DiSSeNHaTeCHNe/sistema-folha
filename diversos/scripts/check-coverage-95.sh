#!/usr/bin/env bash
# Gate único de cobertura 95% (AD-014): BE linha/branch (JaCoCo) + FE linha/branch (Vitest).
# Uso: após `cd backend && mvn test` e `cd frontend && npm run test:coverage`
#   bash diversos/scripts/check-coverage-95.sh
#   bash diversos/scripts/check-coverage-95.sh --threshold 0   # auto-teste (sempre passa)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
JACOCO_XML="${JACOCO_XML:-$ROOT/backend/target/site/jacoco/jacoco.xml}"
FE_SUMMARY="${FE_SUMMARY:-$ROOT/frontend/coverage/coverage-summary.json}"
THRESHOLD=95

while [[ $# -gt 0 ]]; do
  case "$1" in
    --threshold)
      THRESHOLD="${2:?--threshold requires a numeric value}"
      shift 2
      ;;
    -h|--help)
      echo "Usage: $0 [--threshold N]"
      echo "  N = minimum percentage for all 4 metrics (default: 95)"
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

if [[ ! -f "$JACOCO_XML" ]]; then
  echo "ERROR: JaCoCo report not found at $JACOCO_XML" >&2
  echo "Run: cd backend && mvn test" >&2
  exit 1
fi

if [[ ! -f "$FE_SUMMARY" ]]; then
  echo "ERROR: Vitest coverage summary not found at $FE_SUMMARY" >&2
  echo "Run: cd frontend && npm run test:coverage" >&2
  exit 1
fi

python3 - "$JACOCO_XML" "$FE_SUMMARY" "$THRESHOLD" <<'PY'
import json
import sys
import xml.etree.ElementTree as ET

jacoco_path, fe_path, threshold_str = sys.argv[1:4]
threshold = float(threshold_str)

root = ET.parse(jacoco_path).getroot()

def jacoco_pct(counter_type: str) -> tuple[float, int, int]:
    counter = next((c for c in root.findall("counter") if c.get("type") == counter_type), None)
    if counter is None:
        return 0.0, 0, 0
    covered = int(counter.get("covered", 0))
    missed = int(counter.get("missed", 0))
    total = covered + missed
    pct = (100.0 * covered / total) if total else 0.0
    return pct, covered, total

be_line_pct, be_line_cov, be_line_total = jacoco_pct("LINE")
be_branch_pct, be_branch_cov, be_branch_total = jacoco_pct("BRANCH")

with open(fe_path, encoding="utf-8") as fh:
    fe = json.load(fh)["total"]

fe_line_pct = float(fe["lines"]["pct"])
fe_branch_pct = float(fe["branches"]["pct"])
fe_line_cov = int(fe["lines"]["covered"])
fe_line_total = int(fe["lines"]["total"])
fe_branch_cov = int(fe["branches"]["covered"])
fe_branch_total = int(fe["branches"]["total"])

metrics = [
    ("Backend LINE", be_line_pct, be_line_cov, be_line_total),
    ("Backend BRANCH", be_branch_pct, be_branch_cov, be_branch_total),
    ("Frontend Lines", fe_line_pct, fe_line_cov, fe_line_total),
    ("Frontend Branches", fe_branch_pct, fe_branch_cov, fe_branch_total),
]

print(f"Coverage gate (threshold >= {threshold:.0f}%)")
print(f"{'Metric':<20} {'Pct':>7}  {'Covered/Total':>15}  {'Status':>6}")
print("-" * 55)

failed = []
for name, pct, covered, total in metrics:
    status = "PASS" if pct >= threshold else "FAIL"
    if status == "FAIL":
        failed.append((name, pct))
    print(f"{name:<20} {pct:6.2f}%  {covered:>6}/{total:<6}  [{status}]")

if failed:
    print()
    print(f"FAILED: {len(failed)} metric(s) below {threshold:.0f}%:")
    for name, pct in failed:
        print(f"  - {name}: {pct:.2f}% (need >= {threshold:.0f}%)")
    sys.exit(1)

print()
print("All metrics meet the threshold.")
PY
