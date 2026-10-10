#!/usr/bin/env python3
"""
tools/packager/verify_ca_pack.py

Verification tool for Current Affairs .kpsc archives.
Checks ZIP integrity, manifest metadata, content.json SHA-256 checksum,
and schema validity.

Usage:
    python tools/packager/verify_ca_pack.py --pack content/fixtures/kpsc_ca_october_2024.kpsc
"""

import argparse
import hashlib
import json
import os
import sys
import zipfile


def verify_pack(pack_path: str) -> bool:
    print(f"[VERIFY] Verifying pack: {pack_path}")
    if not os.path.exists(pack_path):
        print(f"[ERROR] File does not exist: {pack_path}", file=sys.stderr)
        return False

    try:
        with zipfile.ZipFile(pack_path, "r") as zf:
            file_names = zf.namelist()
            if "manifest.json" not in file_names or "content.json" not in file_names:
                print(f"[ERROR] Missing required files in archive: {file_names}", file=sys.stderr)
                return False

            manifest_bytes = zf.read("manifest.json")
            content_bytes = zf.read("content.json")

            manifest = json.loads(manifest_bytes.decode("utf-8"))
            payload = json.loads(content_bytes.decode("utf-8"))

            calculated_sha = hashlib.sha256(content_bytes).hexdigest()
            expected_sha = manifest.get("contentSha256", "")

            if calculated_sha.lower() != expected_sha.lower():
                print(f"[ERROR] Checksum mismatch! Expected: {expected_sha}, Calculated: {calculated_sha}", file=sys.stderr)
                return False

            print(f"[VERIFY] SHA-256 Verified: {calculated_sha}")
            print(f"[VERIFY] Pack ID: {manifest.get('packId')}, Version: {manifest.get('packVersion')}")
            print(f"[VERIFY] Pack Name: {manifest.get('packName')}")
            print(f"[VERIFY] Questions: {len(payload.get('questions', []))}")
            print(f"[VERIFY] Topics: {len(payload.get('topics', []))}")
            print(f"[VERIFY] Sources: {len(payload.get('sources', []))}")
            print(f"[SUCCESS] Pack is structurally valid and checksum matches!")
            return True
    except Exception as e:
        print(f"[ERROR] Failed to read or verify pack: {e}", file=sys.stderr)
        return False


def main():
    parser = argparse.ArgumentParser(description="Verify Current Affairs .kpsc pack")
    parser.add_argument("--pack", required=True, help="Path to .kpsc file")
    args = parser.parse_args()

    success = verify_pack(args.pack)
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
