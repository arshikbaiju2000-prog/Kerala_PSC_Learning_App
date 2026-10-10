#!/usr/bin/env python3
"""
tools/packager/generate_catalog.py

Generates the remote content catalog (catalog_v1.json) for the Kerala PSC Prep app
by scanning verified .kpsc packages in content/catalog/packs/.

Computes SHA-256 directly from complete archive file bytes, validates internal
archive structure (CRC, manifest.json, content.json, inner content SHA-256),
and produces a deterministic JSON catalog matching RemoteContentCatalog.kt schema.

Usage:
    python tools/packager/generate_catalog.py
    python tools/packager/generate_catalog.py --timestamp 1730419200000 --output test_catalog.json
"""

import argparse
import hashlib
import json
import os
import sys
import time
import zipfile
from pathlib import Path

BASE_DOWNLOAD_URL = (
    "https://raw.githubusercontent.com/arshikbaiju2000-prog/Kerala_PSC_Learning_App/main/content/catalog/packs"
)


def inspect_pack(pack_path: Path) -> dict:
    """
    Inspects and strictly validates a .kpsc pack archive.
    Validates ZIP CRC, required files, safe paths, manifest schema,
    inner content SHA-256, question counts, and computes whole-archive SHA-256.
    """
    if not pack_path.exists():
        raise FileNotFoundError(f"Pack not found: {pack_path}")

    pack_bytes = pack_path.read_bytes()
    size_bytes = len(pack_bytes)
    if size_bytes == 0:
        raise ValueError(f"Pack file is empty: {pack_path}")

    pack_sha256 = hashlib.sha256(pack_bytes).hexdigest().upper()

    try:
        zf = zipfile.ZipFile(pack_path, "r")
    except Exception as e:
        raise ValueError(f"Failed to open {pack_path} as ZIP archive: {e}")

    with zf:
        # 1. Test ZIP CRC integrity
        bad_file = zf.testzip()
        if bad_file is not None:
            raise ValueError(f"Corrupt file CRC in archive {pack_path}: {bad_file}")

        file_names = zf.namelist()

        # 2. Path safety check (no traversal or absolute paths)
        for name in file_names:
            if name.startswith("/") or name.startswith("\\") or ".." in name:
                raise ValueError(f"Insecure path '{name}' detected in pack {pack_path}")

        # 3. Required file presence
        if "manifest.json" not in file_names:
            raise ValueError(f"Archive missing manifest.json: {pack_path}")
        if "content.json" not in file_names:
            raise ValueError(f"Archive missing content.json: {pack_path}")

        # 4. Manifest schema & validation
        try:
            manifest_raw = zf.read("manifest.json").decode("utf-8")
            manifest = json.loads(manifest_raw)
        except Exception as e:
            raise ValueError(f"Invalid manifest.json JSON in {pack_path}: {e}")

        pack_id = manifest.get("packId")
        pack_version = manifest.get("packVersion")
        format_version = manifest.get("formatVersion", 1)
        minimum_app_version = manifest.get("minimumAppVersionCode", 1)
        pack_name = manifest.get("packName")
        question_count = manifest.get("questionCount")
        exam_count = manifest.get("examCount", 0)
        expected_content_sha = manifest.get("contentSha256")

        if not isinstance(pack_id, str) or not pack_id.strip():
            raise ValueError(f"Invalid or missing packId in {pack_path}")
        if not isinstance(pack_version, int) or pack_version < 1:
            raise ValueError(f"Invalid packVersion (must be integer >= 1) in {pack_path}")
        if not isinstance(format_version, int) or format_version != 1:
            raise ValueError(f"Unsupported formatVersion {format_version} (expected 1) in {pack_path}")
        if not isinstance(minimum_app_version, int) or minimum_app_version < 1:
            raise ValueError(f"Invalid minimumAppVersionCode in {pack_path}")
        if not isinstance(pack_name, str) or not pack_name.strip():
            raise ValueError(f"Invalid or missing packName in {pack_path}")
        if not isinstance(question_count, int) or question_count <= 0:
            raise ValueError(f"Invalid questionCount in {pack_path}")
        if not isinstance(exam_count, int) or exam_count < 0:
            raise ValueError(f"Invalid examCount in {pack_path}")
        if not isinstance(expected_content_sha, str) or len(expected_content_sha) != 64:
            raise ValueError(f"Invalid contentSha256 in manifest of {pack_path}")

        # 5. Inner content.json verification
        content_bytes = zf.read("content.json")
        calculated_content_sha = hashlib.sha256(content_bytes).hexdigest().lower()
        if calculated_content_sha != expected_content_sha.lower():
            raise ValueError(
                f"Inner content.json SHA-256 mismatch in {pack_path}. "
                f"Expected: {expected_content_sha}, Calculated: {calculated_content_sha}"
            )

        try:
            content_payload = json.loads(content_bytes.decode("utf-8"))
        except Exception as e:
            raise ValueError(f"Invalid content.json JSON in {pack_path}: {e}")

        payload_questions = content_payload.get("questions", [])
        if len(payload_questions) != question_count:
            raise ValueError(
                f"Question count mismatch in {pack_path}: manifest claims {question_count}, "
                f"content.json has {len(payload_questions)}"
            )

    download_url = f"{BASE_DOWNLOAD_URL}/{pack_path.name}"

    return {
        "packId": pack_id,
        "packVersion": int(pack_version),
        "formatVersion": int(format_version),
        "minimumAppVersionCode": int(minimum_app_version),
        "packName": pack_name,
        "description": manifest.get("description"),
        "questionCount": int(question_count),
        "examCount": int(exam_count),
        "sizeBytes": size_bytes,
        "sha256": pack_sha256,
        "downloadUrl": download_url,
        "replacesPreviousVersion": bool(manifest.get("replacesPreviousVersion", False)),
    }


def validate_catalog(catalog: dict):
    """
    Validates the generated catalog dictionary against schema invariants.
    Raises ValueError on any violation without relying on Python assert statements.
    """
    if not isinstance(catalog, dict):
        raise ValueError("Catalog must be a dictionary")

    catalog_version = catalog.get("catalogVersion")
    if catalog_version != 1:
        raise ValueError(f"Expected catalogVersion 1, got {catalog_version}")

    generated_at = catalog.get("generatedAt")
    if not isinstance(generated_at, int) or generated_at <= 0:
        raise ValueError(f"generatedAt must be a positive integer, got {generated_at}")

    packs = catalog.get("packs")
    if not isinstance(packs, list):
        raise ValueError("packs must be a list")

    seen_identities = set()
    for idx, entry in enumerate(packs):
        if not isinstance(entry, dict):
            raise ValueError(f"Pack entry at index {idx} must be a dictionary")

        pack_id = entry.get("packId")
        pack_version = entry.get("packVersion")
        format_version = entry.get("formatVersion")
        min_app_ver = entry.get("minimumAppVersionCode")
        pack_name = entry.get("packName")
        q_count = entry.get("questionCount")
        exam_count = entry.get("examCount", 0)
        size_bytes = entry.get("sizeBytes")
        sha256_hex = entry.get("sha256")
        download_url = entry.get("downloadUrl")
        replaces_prev = entry.get("replacesPreviousVersion")

        if not isinstance(pack_id, str) or not pack_id.strip():
            raise ValueError(f"Pack entry {idx}: invalid packId")
        if not isinstance(pack_version, int) or pack_version < 1:
            raise ValueError(f"Pack entry {idx}: packVersion must be integer >= 1")

        identity = (pack_id, pack_version)
        if identity in seen_identities:
            raise ValueError(f"Duplicate pack identity detected: {pack_id} v{pack_version}")
        seen_identities.add(identity)

        if format_version != 1:
            raise ValueError(f"Pack entry {pack_id}: formatVersion must be 1, got {format_version}")
        if not isinstance(min_app_ver, int) or min_app_ver < 1:
            raise ValueError(f"Pack entry {pack_id}: minimumAppVersionCode must be >= 1")
        if not isinstance(pack_name, str) or not pack_name.strip():
            raise ValueError(f"Pack entry {pack_id}: invalid packName")
        if not isinstance(q_count, int) or q_count <= 0:
            raise ValueError(f"Pack entry {pack_id}: questionCount must be > 0")
        if not isinstance(exam_count, int) or exam_count < 0:
            raise ValueError(f"Pack entry {pack_id}: examCount must be >= 0")
        if not isinstance(size_bytes, int) or size_bytes <= 0:
            raise ValueError(f"Pack entry {pack_id}: sizeBytes must be > 0")

        if not isinstance(sha256_hex, str) or len(sha256_hex) != 64:
            raise ValueError(f"Pack entry {pack_id}: sha256 must be 64-character hex string")
        try:
            int(sha256_hex, 16)
        except ValueError:
            raise ValueError(f"Pack entry {pack_id}: sha256 contains non-hex characters")

        if not isinstance(download_url, str) or not download_url.startswith("https://"):
            raise ValueError(f"Pack entry {pack_id}: downloadUrl must start with https://")
        if not isinstance(replaces_prev, bool):
            raise ValueError(f"Pack entry {pack_id}: replacesPreviousVersion must be a boolean")


def generate_catalog(packs_dir: Path, output_file: Path, timestamp: int = None) -> dict:
    """
    Generates and validates catalog_v1.json from verified packs in packs_dir.
    Packs are ordered deterministically by (packId, packVersion).
    """
    if not packs_dir.exists():
        raise FileNotFoundError(f"Packs directory not found: {packs_dir}")

    pack_files = sorted(packs_dir.glob("*.kpsc"))
    if not pack_files:
        raise ValueError(f"No .kpsc pack files found in {packs_dir}")

    entries = []
    seen_ids = set()
    for p in pack_files:
        entry = inspect_pack(p)
        ident = (entry["packId"], entry["packVersion"])
        if ident in seen_ids:
            raise ValueError(f"Duplicate pack identity across files: {ident}")
        seen_ids.add(ident)
        entries.append(entry)
        print(f"[CATALOG ENTRY] {entry['packId']} v{entry['packVersion']} -> SHA256={entry['sha256']} ({entry['sizeBytes']} bytes)")

    # Sort deterministically by packId, then packVersion
    entries.sort(key=lambda x: (x["packId"], x["packVersion"]))

    actual_timestamp = timestamp if timestamp is not None else int(time.time() * 1000)

    catalog = {
        "catalogVersion": 1,
        "generatedAt": actual_timestamp,
        "packs": entries,
    }

    validate_catalog(catalog)

    output_file.parent.mkdir(parents=True, exist_ok=True)
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(catalog, f, indent=2)
        f.write("\n")

    print(f"[SUCCESS] Generated catalog with {len(entries)} pack(s) at {output_file} (generatedAt={actual_timestamp})")
    return catalog


def main():
    parser = argparse.ArgumentParser(description="Generate remote content catalog")
    parser.add_argument(
        "--packs-dir",
        type=Path,
        default=Path("content/catalog/packs"),
        help="Directory containing verified .kpsc packs",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("content/catalog/catalog_v1.json"),
        help="Path for generated catalog_v1.json",
    )
    parser.add_argument(
        "--timestamp",
        type=int,
        default=None,
        help="Optional explicit epoch ms timestamp for reproducible testing. Defaults to current time.",
    )
    args = parser.parse_args()

    generate_catalog(args.packs_dir, args.output, args.timestamp)
    print("[SUCCESS] All catalog validation invariants satisfied!")


if __name__ == "__main__":
    main()
