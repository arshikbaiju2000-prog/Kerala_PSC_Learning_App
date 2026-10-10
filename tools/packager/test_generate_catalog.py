#!/usr/bin/env python3
"""
tools/packager/test_generate_catalog.py

Unit and regression test suite for generate_catalog.py.
Verifies timestamp semantics, deterministic field generation, pack validation,
reference values, and error handling for malformed or duplicate packs.

Usage:
    python tools/packager/test_generate_catalog.py
"""

import hashlib
import json
import os
import shutil
import sys
import tempfile
import time
import unittest
import zipfile
from pathlib import Path

# Add project root to sys.path
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(PROJECT_ROOT / "tools" / "packager"))

from generate_catalog import generate_catalog, inspect_pack, validate_catalog


class TestGenerateCatalog(unittest.TestCase):

    def setUp(self):
        self.packs_dir = PROJECT_ROOT / "content" / "catalog" / "packs"
        self.fixture_path = PROJECT_ROOT / "content" / "fixtures" / "kpsc_ca_october_2024.kpsc"
        self.candidate_pack_path = self.packs_dir / "kpsc_ca_october_2024.kpsc"
        self.pilot_path = PROJECT_ROOT / "content" / "pilot_250" / "kpsc_pilot_10th_ldc_v1.kpsc"
        self.temp_dir = tempfile.mkdtemp(prefix="test_catalog_")

    def tearDown(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_candidate_pack_reference_values_and_fixture_identity(self):
        """Verify candidate pack against frozen reference values and fixture identity."""
        self.assertTrue(self.candidate_pack_path.exists(), "Candidate pack must exist")
        self.assertTrue(self.fixture_path.exists(), "Fixture pack must exist")

        cand_bytes = self.candidate_pack_path.read_bytes()
        fixt_bytes = self.fixture_path.read_bytes()

        # Byte identity
        self.assertEqual(cand_bytes, fixt_bytes, "Candidate pack and fixture must be byte-identical")

        # Byte count: exactly 4299
        self.assertEqual(len(cand_bytes), 4299, "Pack size must be exactly 4299 bytes")

        # Whole-file SHA-256
        cand_sha256 = hashlib.sha256(cand_bytes).hexdigest().upper()
        expected_sha256 = "DD26618978CE5971284CFF26164D5E865B0C5BF238AF21B9581B1FCD3330CC94"
        self.assertEqual(cand_sha256, expected_sha256, "Archive SHA-256 mismatch")

        # Inner content.json SHA-256
        with zipfile.ZipFile(self.candidate_pack_path, "r") as zf:
            content_bytes = zf.read("content.json")
            inner_sha256 = hashlib.sha256(content_bytes).hexdigest().lower()
            expected_inner_sha = "0086aa9f24ffffe52fb90b7bc4372d2ba1430ffd1168f701263a6c54ef5c805c"
            self.assertEqual(inner_sha256, expected_inner_sha, "Inner content.json SHA-256 mismatch")

        # Pilot pack SHA-256 integrity
        self.assertTrue(self.pilot_path.exists(), "Pilot pack must exist")
        pilot_bytes = self.pilot_path.read_bytes()
        pilot_sha = hashlib.sha256(pilot_bytes).hexdigest().upper()
        self.assertEqual(
            pilot_sha,
            "53EA956C3224F277A8170C4DAF6053E1710A0504797576A4E11669C96613E1BF",
            "Pilot pack SHA-256 must remain unchanged",
        )

    def test_default_timestamp_is_plausible_current_time(self):
        """Default generation must use current UTC epoch milliseconds."""
        out_file = Path(self.temp_dir) / "catalog_default_time.json"
        before_ms = int(time.time() * 1000)
        catalog = generate_catalog(self.packs_dir, out_file)
        after_ms = int(time.time() * 1000)

        gen_time = catalog.get("generatedAt")
        self.assertIsInstance(gen_time, int)
        self.assertTrue(
            before_ms - 2000 <= gen_time <= after_ms + 2000,
            f"generatedAt {gen_time} should be within [{before_ms}, {after_ms}]",
        )

    def test_explicit_test_timestamp_is_reproduced_exactly(self):
        """Explicitly passed --timestamp must be used verbatim."""
        out_file = Path(self.temp_dir) / "catalog_fixed_time.json"
        test_timestamp = 1730419200000
        catalog = generate_catalog(self.packs_dir, out_file, timestamp=test_timestamp)
        self.assertEqual(catalog.get("generatedAt"), test_timestamp)

    def test_determinism_of_non_timestamp_fields(self):
        """Identical inputs produce byte-identical catalogs when timestamp is held constant."""
        out_file1 = Path(self.temp_dir) / "catalog_run1.json"
        out_file2 = Path(self.temp_dir) / "catalog_run2.json"
        test_timestamp = 1730419200000

        cat1 = generate_catalog(self.packs_dir, out_file1, timestamp=test_timestamp)
        cat2 = generate_catalog(self.packs_dir, out_file2, timestamp=test_timestamp)

        self.assertEqual(out_file1.read_text(encoding="utf-8"), out_file2.read_text(encoding="utf-8"))
        self.assertEqual(cat1["packs"], cat2["packs"])

    def test_rejection_of_corrupted_zip(self):
        """Archive with invalid ZIP data must be rejected with ValueError."""
        bad_dir = Path(self.temp_dir) / "corrupt_packs"
        bad_dir.mkdir(parents=True, exist_ok=True)
        bad_file = bad_dir / "bad_pack.kpsc"
        bad_file.write_bytes(b"NOT A VALID ZIP ARCHIVE")

        out_file = Path(self.temp_dir) / "out.json"
        with self.assertRaises(ValueError):
            generate_catalog(bad_dir, out_file)

    def test_rejection_of_missing_manifest_or_content(self):
        """Pack missing manifest.json or content.json must be rejected."""
        bad_dir = Path(self.temp_dir) / "missing_files_packs"
        bad_dir.mkdir(parents=True, exist_ok=True)
        bad_file = bad_dir / "incomplete.kpsc"

        with zipfile.ZipFile(bad_file, "w") as zf:
            zf.writestr("manifest.json", "{}")
            # content.json missing

        out_file = Path(self.temp_dir) / "out.json"
        with self.assertRaises(ValueError):
            generate_catalog(bad_dir, out_file)

    def test_rejection_of_inner_content_sha_mismatch(self):
        """Pack with tampered content.json must fail verification."""
        bad_dir = Path(self.temp_dir) / "sha_mismatch_packs"
        bad_dir.mkdir(parents=True, exist_ok=True)
        bad_file = bad_dir / "tampered.kpsc"

        manifest = {
            "packId": "tampered_pack",
            "packVersion": 1,
            "formatVersion": 1,
            "packName": "Tampered Pack",
            "questionCount": 1,
            "contentSha256": "0000000000000000000000000000000000000000000000000000000000000000",
        }
        with zipfile.ZipFile(bad_file, "w") as zf:
            zf.writestr("manifest.json", json.dumps(manifest))
            zf.writestr("content.json", json.dumps({"questions": [{"id": "q1"}]}))

        out_file = Path(self.temp_dir) / "out.json"
        with self.assertRaises(ValueError):
            generate_catalog(bad_dir, out_file)

    def test_rejection_of_duplicate_pack_identities(self):
        """Catalog generator must reject duplicate (packId, packVersion) packs."""
        dup_dir = Path(self.temp_dir) / "dup_packs"
        dup_dir.mkdir(parents=True, exist_ok=True)

        # Copy candidate pack twice under different filenames
        shutil.copy2(self.candidate_pack_path, dup_dir / "pack1.kpsc")
        shutil.copy2(self.candidate_pack_path, dup_dir / "pack2.kpsc")

        out_file = Path(self.temp_dir) / "out.json"
        with self.assertRaises(ValueError) as ctx:
            generate_catalog(dup_dir, out_file)
        self.assertIn("Duplicate pack identity", str(ctx.exception))

    def test_catalog_validation_rejects_insecure_urls(self):
        """validate_catalog must reject non-HTTPS download URLs."""
        bad_catalog = {
            "catalogVersion": 1,
            "generatedAt": 1730419200000,
            "packs": [
                {
                    "packId": "kpsc_ca_2024_10",
                    "packVersion": 1,
                    "formatVersion": 1,
                    "minimumAppVersionCode": 1,
                    "packName": "Test",
                    "questionCount": 10,
                    "sizeBytes": 4299,
                    "sha256": "DD26618978CE5971284CFF26164D5E865B0C5BF238AF21B9581B1FCD3330CC94",
                    "downloadUrl": "http://insecure.example.com/pack.kpsc",
                    "replacesPreviousVersion": False,
                }
            ],
        }
        with self.assertRaises(ValueError) as ctx:
            validate_catalog(bad_catalog)
        self.assertIn("must start with https://", str(ctx.exception))


if __name__ == "__main__":
    unittest.main()
