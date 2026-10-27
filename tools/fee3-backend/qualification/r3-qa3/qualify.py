#!/usr/bin/env python3
"""FEE3 R3 QA3 fail-closed non-mutating qualification. Python 3.11+, no pip.

Only connects DIRECTLY to local loopback, bypassing system/environment HTTP proxies.
Only GET and POST /validation /preview. Never follows HTTP redirects.
Detailed per-route transport diagnostics; never sends credentials or mutates Oracle.
"""
from __future__ import annotations
import argparse
from datetime import date, timedelta, datetime, timezone
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, build_opener, ProxyHandler, HTTPRedirectHandler
from http.client import HTTPException
import re

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
API = "/api/v1/fee3"
READ_GET = {
    "/readiness", "/items?page=0&size=5", "/references/operations",
    "/references/formulas", "/references/sources", "/workflow/readiness"
}
READ_POST = {"/validation", "/preview"}
MAX_BODY = 2_000_000

class TransportFailure(RuntimeError):
    """Safe error, with HTTP verb + allowlisted route for support diagnosis."""

class RejectRedirects(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise TransportFailure("HTTP redirect blocked; no outbound or credential forwarding is allowed")

# Python's urllib uses Windows/ENV proxies by default; curl.exe loopback may not.
# ProxyHandler({}) makes every call strictly direct, including POST validation/preview.
DIRECT_OPENER = build_opener(ProxyHandler({}), RejectRedirects())

def error_label(exc):
    """Do not include arbitrary server response bodies, request payloads or secrets."""
    if isinstance(exc, HTTPError):
        try:
            body = exc.read(1024)
            obj = json.loads(body.decode("utf-8"))
            code = str(obj.get("errorCode", "")) if isinstance(obj, dict) else ""
            code = re.sub(r"[^A-Za-z0-9_.-]", "", code)[:80]
        except (ValueError, UnicodeError, OSError):
            code = ""
        return "HTTP " + str(exc.code) + (" errorCode=" + code if code else "")
    if isinstance(exc, TransportFailure):
        return str(exc)
    return type(exc).__name__ + (": " + str(exc.reason)[:130] if isinstance(exc, URLError) else
                                   ": " + str(exc)[:130])


class Gate:
    def __init__(self):
        self.checks = []
    def test(self, label, value, details=None):
        good = bool(value)
        self.checks.append({"name": label, "result": "PASS" if good else "FAIL",
                            "details": str(details or "")[:450]})
        print(("PASS" if good else "FAIL") + " | " + label +
              (" | " + str(details)[:200] if details else ""))
        return good
    def failure(self, label, error):
        self.test(label, False, str(error))
    def report(self, mode):
        return {"protocol": "FEE3_R3_QUALIFICATION_V1", "toolRevision": "QA3",
                "transport": "DIRECT_LOOPBACK_NO_PROXY_NO_REDIRECT", "mode": mode,
                "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
                "readOnlyByDesign": True, "financialPosting": False,
                "passed": sum(x["result"] == "PASS" for x in self.checks),
                "failed": sum(x["result"] == "FAIL" for x in self.checks),
                "checks": self.checks}

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def run(label, command, gate, expect=None):
    try:
        result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True,
                                encoding="utf-8", errors="replace", timeout=90)
        output = (result.stdout + "\n" + result.stderr).strip()
        gate.test(label, result.returncode == 0 and (expect is None or expect in output),
                  output[-350:] if result.returncode != 0 else (expect or "exit=0"))
    except (OSError, subprocess.TimeoutExpired) as exc:
        gate.failure(label, exc)

def source_checks(gate):
    manifest_file = ROOT / "tools/fee3-backend/releases/r3/manifest.json"
    try:
        manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
        gate.test("R3 manifest identity", manifest.get("patchId") == "FEE3-BE-R3-20261026")
        for rel, expected in {**manifest["anchors"], **manifest["files"]}.items():
            target = ROOT / rel
            gate.test("sha256 " + rel, target.is_file() and sha(target) == expected)
    except (OSError, ValueError, KeyError, TypeError) as exc:
        gate.failure("R3 manifest and anchors", exc)
        return
    command_set = [
        ("R1 structural", "tools/fee3-backend/verify-r1.mjs", "FEE3_BE_R1_STATIC_PASS=52 FAIL=0"),
        ("R2 structural", "tools/fee3-backend/verify-r2.mjs", "FEE3_BE_R2_STATIC_PASS=48 FAIL=0"),
        ("R2 roundtrip", "tools/fee3-backend/verify-r2-mapping.cjs", "FEE3_BE_R2_MAPPING_PASS=17"),
        ("R3 structural", "tools/fee3-backend/verify-r3.mjs", "FEE3_R3_STATIC_PASS=31 FAIL=0"),
    ]
    if shutil.which("node") is None:
        gate.failure("Node installed", "node command was not found")
    else:
        for label, rel, marker in command_set:
            run(label, ["node", str(ROOT / rel)], gate, marker)
    if shutil.which("java") is None or shutil.which("javac") is None:
        gate.failure("JDK 21+", "java/javac commands unavailable")
    else:
        base = ROOT / "backend/src/main/java/com/behsazan/corebanking/fee3"
        files = [base / "domain/Fee3Models.java", base / "r3/Fee3PreviewEngine.java",
                 base / "r3/Fee3SubmissionPolicy.java",
                 ROOT / "tools/fee3-backend/Fee3R3EngineProbe.java"]
        with tempfile.TemporaryDirectory(prefix="fee3-r3-gate-") as dirname:
            run("Java engine source compiles", ["javac", "-encoding", "UTF-8", "-d", dirname] +
                [str(f) for f in files], gate)
            run("Java preview and governance probe",
                ["java", "-cp", dirname, "Fee3R3EngineProbe"], gate,
                "FEE3_R3_ENGINE_POLICY_CHECKS_PASS=33")

def validate_loopback(url):
    parsed = urlsplit(url)
    if parsed.scheme not in ("http", "https") or parsed.hostname not in ("127.0.0.1", "localhost", "::1"):
        raise ValueError("Only local loopback HTTP(S) is permitted")
    if parsed.username or parsed.password or parsed.query or parsed.fragment or parsed.path not in ("", "/"):
        raise ValueError("Base URL must have no credentials, path or query")
    if parsed.port is None:
        raise ValueError("Base URL must include the local server port")
    return url.rstrip("/")

def fetch(base, path, body=None):
    method = "POST" if body is not None else "GET"
    if (body is None and path not in READ_GET) or (body is not None and path not in READ_POST):
        raise ValueError("Non-readonly endpoint is forbidden by qualification allowlist: " + path)
    payload = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
    headers = {"Accept": "application/json"}
    if payload is not None:
        headers["Content-Type"] = "application/json"
    request = Request(base + API + path, data=payload, method=method, headers=headers)
    print("HTTP_STEP_START " + method + " " + path, flush=True)
    try:
        with DIRECT_OPENER.open(request, timeout=12) as response:
            raw = response.read(MAX_BODY + 1)
            if len(raw) > MAX_BODY:
                raise TransportFailure("Server JSON response exceeds 2 MB")
            ctype = response.headers.get("Content-Type", "").lower()
            if not ("application/json" in ctype or "+json" in ctype):
                raise TransportFailure("Unexpected Content-Type (expected JSON): " + ctype[:70])
            value = json.loads(raw.decode("utf-8"))
            if not isinstance(value, (dict, list)):
                raise TransportFailure("HTTP body is not a JSON object/array")
            print("HTTP_STEP_PASS " + method + " " + path + " HTTP=" + str(response.status), flush=True)
            return value
    except (HTTPError, URLError, HTTPException, ConnectionError, TimeoutError,
            OSError, ValueError, TransportFailure) as exc:
        detail = error_label(exc)
        print("HTTP_STEP_FAIL " + method + " " + path + " | " + detail, flush=True)
        raise TransportFailure(method + " " + path + " | " + detail) from exc

def fixture(op, today):
    return {
        "calcItemId": None, "tariffVersionId": None,
        "expectedItemRecordVersion": None, "expectedVersionRecordVersion": None,
        "item": {"title": "R3 non-mutating qualification", "module": op["module"],
                 "operation": op["code"], "amountKind": "SERVICE_FEE",
                 "variant": None, "ownerDomain": "TARIFF", "description": None},
        "version": {"validFrom": today, "validTo": None, "origin": "BANK_INTERNAL",
                    "sourceId": None, "method": "FIXED", "taxMode": "EXEMPT",
                    "rounding": "HALF_UP", "changeReason": "Non-mutating test",
                    "financialPolicy": {}, "serviceCurrency": "IRR", "feeCurrency": "IRR",
                    "uiContext": {}},
        "scope": {"channels": ["ALL"], "customers": ["ALL"], "groups": ["ALL"],
                  "products": ["ALL"], "contracts": [], "currency": "IRR"},
        "rules": [{"order": 1, "method": "FIXED", "base": "REQUEST_AMOUNT",
                   "rateSource": "RULE", "rate": None, "fixed": 12500,
                   "collateral": None, "coverage": None, "min": None, "max": None,
                   "formulaCode": None, "parameters": {}}],
        "tests": [{"title": "R3 evidence fixture", "request": {"amount": 1000000},
                   "expected": 12500}]
    }

def test_runtime(gate, base):
    # All calls in this function are allowlisted GET or validation/preview POST only.
    initial = fetch(base, "/readiness")
    if not isinstance(initial, dict):
        raise TransportFailure("GET /readiness | Expected JSON object")
    gate.test("FEE3 Oracle reads enabled", initial.get("readsEnabled") is True)
    gate.test("All 11 FEE3 tables present", initial.get("missingTables") == [])
    if not gate.test("Write flag disabled during read-only qualification",
                     initial.get("draftWritesEnabled") is False):
        return
    if initial.get("readsEnabled") is not True or initial.get("missingTables") != []:
        return
    # Real API fields are calcItemCount/tariffVersionCount, not itemCount/versionCount.
    count_keys = ("calcItemCount", "tariffVersionCount")
    if not gate.test("Initial Oracle counts present and nonnegative",
                     all(type(initial.get(k)) is int and initial[k] >= 0 for k in count_keys),
                     {k: initial.get(k) for k in count_keys}):
        return
    oracle_date = initial.get("oracleBusinessDate")
    try:
        today = date.fromisoformat(oracle_date)
        gate.test("Oracle business date is ISO date", True, oracle_date)
    except (TypeError, ValueError) as exc:
        gate.failure("Oracle business date is ISO date", exc)
        return
    # Actual R1/R2 backend contract: Fee3Models.Page<T>(items,total,page,size).
    # The QA2 mock used a non-existent 'rows' property, causing a false failure.
    catalog = fetch(base, "/items?page=0&size=5")
    rows = catalog.get("items") if isinstance(catalog, dict) else None
    metadata = {
        "keys": sorted(k for k in catalog if isinstance(k, str)) if isinstance(catalog, dict) else [],
        "total": catalog.get("total") if isinstance(catalog, dict) else None,
        "page": catalog.get("page") if isinstance(catalog, dict) else None,
        "size": catalog.get("size") if isinstance(catalog, dict) else None,
        "itemCount": len(rows) if isinstance(rows, list) else None,
        "expectedTotal": initial["calcItemCount"],
    }
    catalog_valid = (
        isinstance(catalog, dict) and isinstance(rows, list)
        and type(catalog.get("total")) is int
        and catalog["total"] == initial["calcItemCount"]
        and type(catalog.get("page")) is int and catalog["page"] == 0
        and type(catalog.get("size")) is int and catalog["size"] == 5
        and len(rows) == min(5, catalog["total"])
        and all(isinstance(row, dict)
                and type(row.get("calcItemId")) is int and row["calcItemId"] > 0
                and isinstance(row.get("code"), str) and row["code"].strip()
                for row in rows)
    )
    if not gate.test("Oracle catalog Page<T> items/total/page/size contract", catalog_valid, metadata):
        # A wrong catalog schema invalidates read-only qualification; fail before POST.
        return
    for name in ("/references/formulas", "/references/sources"):
        value = fetch(base, name)
        gate.test("Oracle reference " + name, isinstance(value, list))
    operations = fetch(base, "/references/operations")
    if not gate.test("Oracle operation reference is a JSON array", isinstance(operations, list)):
        return
    op = next((x for x in operations if isinstance(x, dict) and x.get("active") and
               "SERVICE_FEE" in (x.get("allowedAmountKinds") or [])), None)
    if not gate.test("Compatible active service fee operation", op is not None):
        return
    workflow = fetch(base, "/workflow/readiness")
    if not gate.test("Workflow readiness response is a JSON object", isinstance(workflow, dict)):
        return
    workflow_safe = True
    for key in ("workflowEnabled", "checkerDecisionEnabled", "approvalEnabled",
                "postingEnabled", "authenticated", "makerRole", "checkerRole"):
        if not gate.test("Workflow fail-closed: " + key, workflow.get(key) is False):
            workflow_safe = False
    if not workflow_safe:
        return
    draft = fixture(op, oracle_date)
    report = fetch(base, "/validation", draft)
    if not gate.test("Validation response is a JSON object", isinstance(report, dict)):
        return
    gate.test("Valid draft submission readiness", report.get("submissionReady") is True,
              str((report.get("issues") or [])[:2])[:150])
    gate.test("Validation does not certify approval", report.get("approvalReady") is False)
    preview = fetch(base, "/preview", {"draft": draft, "inputs": {"amount": 1000000}})
    if not gate.test("Preview response is a JSON object", isinstance(preview, dict)):
        return
    gate.test("Fixed fee server simulation", preview.get("status") == "SIMULATED" and
              str(preview.get("totalIrr")) in ("12500", "12500.0", "12500.00"))
    gate.test("No financial posting/persistence claimed", preview.get("persisted") is False and
              preview.get("financialPosting") is False)
    bad_date = json.loads(json.dumps(draft))
    bad_date["version"]["validFrom"] = (today - timedelta(days=1)).isoformat()
    bad_report = fetch(base, "/validation", bad_date)
    gate.test("Oracle past business date rejected", isinstance(bad_report, dict) and
              bad_report.get("submissionReady") is False)
    bad_currency = json.loads(json.dumps(draft))
    bad_currency["version"]["feeCurrency"] = "USD"
    bad_report = fetch(base, "/validation", bad_currency)
    gate.test("Non-IRR output rejected by server", isinstance(bad_report, dict) and
              bad_report.get("submissionReady") is False)
    formula = json.loads(json.dumps(draft))
    formula["version"]["method"] = "FORMULA"
    formula["rules"][0]["method"] = "FORMULA"
    formula["rules"][0]["fixed"] = None
    formula["rules"][0]["parameters"] = {"_fceEditableExpressionV1": "AMOUNT*0.02"}
    result = fetch(base, "/preview", {"draft": formula, "inputs": {"amount": 1000000}})
    gate.test("Uncertified editable formula blocked", isinstance(result, dict) and
              result.get("status") == "BLOCKED" and
              result.get("persisted") is False and result.get("financialPosting") is False)
    final = fetch(base, "/readiness")
    same = isinstance(final, dict) and all(
        type(final.get(key)) is int and initial[key] == final[key] for key in count_keys)
    gate.test("Real Oracle item/version counts unchanged during read-only HTTP checks", same,
              {key: {"before": initial.get(key), "after": final.get(key) if isinstance(final, dict) else None}
               for key in count_keys})

def main():
    parser = argparse.ArgumentParser(description="FEE3 R3 non-mutating qualification")
    parser.add_argument("--mode", choices=("source", "runtime", "all"), default="source")
    parser.add_argument("--base-url", default="http://127.0.0.1:8091")
    parser.add_argument("--report", help="Optional JSON report path; no credentials are included")
    args = parser.parse_args()
    # Windows pipes may use a non-UTF-8 codepage; never fail while printing diagnostics.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(errors="backslashreplace")
    gate = Gate()
    try:
        if args.mode in ("source", "all"):
            source_checks(gate)
        if args.mode in ("runtime", "all"):
            base = validate_loopback(args.base_url)
            test_runtime(gate, base)
    except (HTTPError, URLError, KeyError, TypeError, ValueError, RuntimeError,
            HTTPException, ConnectionError, TimeoutError, OSError) as exc:
        gate.failure("Qualification runtime", type(exc).__name__ + ": " + str(exc))
    result = gate.report(args.mode)
    print("FEE3_R3_QUALIFICATION_PASS=" + str(result["passed"]) +
          " FAIL=" + str(result["failed"]))
    if args.report:
        destination = Path(args.report)
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        print("REPORT=" + str(destination))
    return 0 if result["failed"] == 0 and result["passed"] else 1

if __name__ == "__main__":
    sys.exit(main())
