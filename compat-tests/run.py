#!/usr/bin/env python3
"""Cross-platform end-to-end compatibility runner for Java Guard.

Only the Python standard library is used.  Generated keys, the AES key and the
rendered Java Guard configuration stay below the private work directory and are
never copied into reports.
"""

from __future__ import annotations

import argparse
import base64
import binascii
import contextlib
import datetime as dt
import hashlib
import http.client
import io
import json
import os
import platform
import re
import secrets
import shutil
import signal
import stat
import struct
import subprocess
import sys
import tarfile
import tempfile
import threading
import time
import traceback
import urllib.parse
import zipfile
from pathlib import Path, PurePosixPath
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

IS_WINDOWS = os.name == "nt"
CLASS_MAGIC = b"\xca\xfe\xba\xbe"
ENCRYPT_CLASS_SUFFIX = b"\x00JGC\x00"
ENCRYPT_RESOURCE_HEADER = b"\x00JGR\x00"
DEFAULT_EXPECTED_BODY = '"ok":true'
LAUNCH_TOKEN_HEADER = "X-Compat-Launch-Token"
EXPECTED_CHECKS = {
    "diConfig", "aop", "cache", "async", "event", "json", "serviceLoader",
    "reflection", "proxy", "serialization", "resourceUrlConnection",
    "nestedPropertiesResource", "unicodeResource", "structuredResource",
    "classLookup", "classMajor", "languageApi",
}
EXPECTED_BOOT_VERSIONS = {
    8: "2.1.9.RELEASE",
    11: "2.7.18",
    17: "3.3.13",
    21: "3.4.13",
    25: "4.1.0",
}
SUCCESS_RESPONSE_KEYS = {
    "ok", "fixture", "bootVersion", "javaHome", "javaVersion", "javaFeature",
    "pid", "classMajor", "expectedClassMajor", "launchTokenSha256", "checks",
}
FAILURE_RESPONSE_KEYS = SUCCESS_RESPONSE_KEYS | {"error"}
SECRET_NAMES = ("aes_key", "private_key", "public_key", "config", "launch_token")
AES_KEY_NAME = "aes.key"
PRIVATE_KEY_NAME = "id_ed25519"
PUBLIC_KEY_NAME = "id_ed25519.pub"


class CompatError(RuntimeError):
    pass


class Runner:
    def __init__(self, args: argparse.Namespace) -> None:
        self.args = args
        self.repo = args.repo.resolve()
        self.fixture = self._resolve_fixture(args.fixture)
        self.work = args.work_dir.resolve()
        self.report_dir = args.report_dir.resolve()
        self.private = self.work / "private"
        self.logs = self.report_dir / "logs"
        self.package_dir = self.work / "package"
        self.build_view = self.work / "build-view"
        self.guard_out = self.work / "guard-output"
        self.result: Dict[str, object] = {
            "schema": 1,
            "started_at": utc_now(),
            "status": "running",
            "platform": {
                "system": platform.system(),
                "release": platform.release(),
                "machine": platform.machine(),
                "python": platform.python_version(),
            },
            "fixture": str(self.fixture),
            "steps": [],
            "artifacts": {},
        }
        self._step_start = 0.0
        self._processes: List[subprocess.Popen] = []
        self._secrets: List[str] = []
        self._package_archive: Optional[Path] = None
        self._packaged_root: Optional[Path] = None
        self._packaged_launcher: Optional[Path] = None
        self._build_java_home: Optional[Path] = None
        self._fixture_jar: Optional[Path] = None
        self._protected_jar: Optional[Path] = None
        self._launcher_only = bool(args.launcher_only)
        self._started = time.monotonic()

    def run(self) -> int:
        try:
            self._prepare_dirs()
            self.step("validate-inputs", self._validate_inputs)
            if not self._launcher_only:
                self.step("create-ephemeral-secrets", self._create_secrets)
            if self.args.skip_package:
                self.step("use-existing-package", self._use_existing_package)
            else:
                self.step("package-launcher-runtime", self._package)
            self.step("extract-packaged-runtime", self._extract_runtime)
            self.step("create-build-view", self._create_build_view)
            self.step("build-fixture", self._build_fixture)
            self.step("check-original-jar", lambda: self._check_jar(self._fixture_jar, encrypted=False))
            if self.args.build_only:
                self.result["status"] = "passed"
                return 0
            if not self._launcher_only:
                self.step("encrypt-and-sign-fixture", self._protect_fixture)
            else:
                self.step("locate-protected-fixture", self._locate_existing_protected_fixture)
            self.step("check-protected-jar", lambda: self._check_jar(self._protected_jar, encrypted=True))
            self.step("launcher-startup-and-http", self._test_startup)
            self.step("forced-http-failure", self._test_forced_http_failure)
            self.step("forced-process-failures", self._test_failures)
            self.result["status"] = "passed"
            return 0
        except KeyboardInterrupt:
            self.result["status"] = "interrupted"
            self.result["error"] = "interrupted"
            print("[compat] INTERRUPTED", file=sys.stderr, flush=True)
            return 130
        except Exception as exc:
            self.result["status"] = "failed"
            self.result["error"] = self._redact(str(exc))
            self._write_traceback(exc)
            print("[compat] FAILED: " + self._redact(str(exc)), file=sys.stderr, flush=True)
            self._print_failure_tail()
            return 1
        finally:
            self._cleanup_processes()
            self.result["finished_at"] = utc_now()
            self.result["duration_seconds"] = round(time.monotonic() - self._started, 3)
            self._write_report()
            if self.result.get("status") == "passed":
                print("[compat] PASSED in %.3fs" % self.result["duration_seconds"], flush=True)

    def step(self, name: str, operation) -> None:
        print("[compat] START " + name, flush=True)
        record: Dict[str, object] = {"name": name, "started_at": utc_now()}
        self.result["steps"].append(record)
        started = time.monotonic()
        try:
            operation()
            record["status"] = "passed"
        except Exception as exc:
            record["status"] = "failed"
            record["error"] = self._redact(str(exc))
            raise
        finally:
            duration = round(time.monotonic() - started, 3)
            record["duration_seconds"] = duration
            print("[compat] %s %s (%.3fs)" % (record.get("status", "interrupted").upper(), name, duration), flush=True)

    def _prepare_dirs(self) -> None:
        validate_output_paths(self.repo, self.work, self.report_dir)
        if self.args.clean and self.work.exists():
            shutil.rmtree(self.work, onerror=remove_readonly)
        self.work.mkdir(parents=True, exist_ok=True)
        self.report_dir.mkdir(parents=True, exist_ok=True)
        if self.logs.exists():
            shutil.rmtree(self.logs, onerror=remove_readonly)
        old_report = self.report_dir / "compat-report.json"
        with contextlib.suppress(FileNotFoundError):
            old_report.unlink()
        self.logs.mkdir(parents=True, exist_ok=True)
        self.private.mkdir(parents=True, exist_ok=True)
        restrict_directory(self.private)

    def _resolve_fixture(self, value: str) -> Path:
        candidate = Path(value)
        if candidate.is_absolute():
            return candidate.resolve()
        fixture_root = self.repo / "compat-tests" / "apps"
        direct = fixture_root / candidate
        return direct.resolve() if direct.exists() else (self.repo / candidate).resolve()

    def _validate_inputs(self) -> None:
        if not self.repo.is_dir():
            raise CompatError("repository does not exist: " + str(self.repo))
        self.guard_jar = find_guard_jar(self.args.java_guard_jar, self.repo)
        if not self.guard_jar.is_file():
            raise CompatError("Java Guard JAR does not exist: " + str(self.guard_jar))
        self.jdk_source = self.args.jdk_home.resolve()
        if not self.jdk_source.exists():
            raise CompatError("target JDK directory/archive does not exist: " + str(self.jdk_source))
        if not self._launcher_only:
            self.ssh_keygen = require_tool("ssh-keygen")
        if not self.fixture.is_dir() or not (self.fixture / "pom.xml").is_file():
            raise CompatError("fixture must be a Maven project under compat-tests/apps or a supplied path: " + str(self.fixture))
        self.expected_class_major = (self.args.expected_class_major if self.args.expected_class_major is not None
                                     else infer_class_major(self.fixture))
        if self.expected_class_major is None:
            raise CompatError("cannot infer class major from fixture name; pass --expected-class-major")
        if not 45 <= self.expected_class_major <= 100:
            raise CompatError("expected class major must be between 45 and 100")
        print("[compat]   expected class major: %d" % self.expected_class_major, flush=True)
        self.result["expected_class_major"] = self.expected_class_major
        self.maven = find_maven(self.args.maven_home, self.fixture, self.repo)
        self.result["artifacts"].update({
            "java_guard_jar": describe_file(self.guard_jar),
            "jdk_source": str(self.jdk_source),
        })

    def _create_secrets(self) -> None:
        key_path = self.private / PRIVATE_KEY_NAME
        cmd = [self.ssh_keygen, "-q", "-t", "ed25519", "-N", "", "-C", "java-guard-compat", "-f", str(key_path)]
        self.command("ssh-keygen", cmd, cwd=self.private, timeout=self.args.command_timeout, redact_command=True)
        aes_key = secrets.token_urlsafe(48)
        aes_path = self.private / AES_KEY_NAME
        aes_path.write_text(aes_key + "\n", encoding="utf-8")
        restrict_file(aes_path)
        public_key_path = self.private / PUBLIC_KEY_NAME
        self._register_secrets(aes_key, key_path, public_key_path)
        self._render_config(aes_key, key_path, public_key_path)

    def _register_secrets(self, aes_key: str, private_key: Path, public_key: Path) -> None:
        key_contents = private_key.read_text(encoding="utf-8", errors="replace")
        public_contents = public_key.read_text(encoding="utf-8", errors="replace")
        self._secrets.extend([aes_key, str(private_key), str(public_key), key_contents, public_contents])
        self._secrets.extend(line for text in (key_contents, public_contents)
                             for line in text.splitlines() if len(line) >= 8)

    def _render_config(self, aes_key: str, private_key: Path, public_key: Path) -> None:
        template = self.args.config_template.resolve().read_text(encoding="utf-8")
        values = {
            "AES_KEY": yaml_quote(aes_key),
            "PRIVATE_KEY": yaml_quote(str(private_key)),
            "PUBLIC_KEY": yaml_quote(str(public_key)),
            "ORIGINAL_JAVA": yaml_quote(str(self.jdk_source)),
            "OUTPUT_DIR": yaml_quote(str(self.guard_out)),
        }
        for name, value in values.items():
            template = template.replace("@" + name + "@", value)
        unresolved = re.findall(r"@[A-Z][A-Z0-9_]+@", template)
        if unresolved:
            raise CompatError("unresolved config placeholders: " + ", ".join(sorted(set(unresolved))))
        self.config = self.private / "config.yml"
        self.config.write_text(template, encoding="utf-8")
        restrict_file(self.config)
        self._secrets.extend([template, self.config.read_text(encoding="utf-8")])

    def _package(self) -> None:
        if self.guard_out.exists():
            shutil.rmtree(self.guard_out, onerror=remove_readonly)
        self.guard_out.mkdir(parents=True)
        java = java_executable(self.args.tool_jdk_home or self.args.jdk_home)
        cmd = [str(java), "-jar", str(self.guard_jar), "-c", str(self.config), "-o", str(self.guard_out), "-l"]
        if self.args.skip_deps:
            cmd.append("--skip-deps")
        env = self.tool_env(self.args.tool_jdk_home or self.args.jdk_home)
        self.command("package", cmd, cwd=self.repo, env=env, timeout=self.args.package_timeout, redact_command=True)
        self._package_archive = select_runtime_archive(self.guard_out)
        launcher = self.guard_out / "bin" / launcher_name()
        if not launcher.is_file():
            raise CompatError("Java Guard did not create launcher: " + str(launcher))
        self.result["artifacts"]["runtime_archive"] = describe_file(self._package_archive)

    def _use_existing_package(self) -> None:
        source = self.args.package_archive
        if source is None:
            raise CompatError("--skip-package requires --package-archive")
        source = source.resolve()
        if not source.is_file():
            raise CompatError("package archive does not exist: " + str(source))
        self._package_archive = source
        self.result["artifacts"]["runtime_archive"] = describe_file(source)

    def _extract_runtime(self) -> None:
        assert self._package_archive is not None
        if self.package_dir.exists():
            shutil.rmtree(self.package_dir, onerror=remove_readonly)
        self.package_dir.mkdir(parents=True)
        safe_extract(self._package_archive, self.package_dir)
        root = find_runtime_root(self.package_dir)
        java = root / "bin" / java_name()
        java_ori = root / "bin" / java_ori_name()
        if not java.is_file() or not java_ori.is_file():
            raise CompatError("packaged runtime must contain bin/java launcher and bin/java_ori: " + str(root))
        if not IS_WINDOWS:
            make_executable(java)
            make_executable(java_ori)
        self._packaged_root = root
        self._packaged_launcher = java
        before = sha256(java)
        self._packaged_launcher_hash = before
        self.result["artifacts"]["packaged_launcher"] = {"path": str(java), "sha256": before}
        self.result["artifacts"]["packaged_java_ori"] = describe_file(java_ori)

    def _create_build_view(self) -> None:
        assert self._packaged_root is not None
        if self.build_view.exists():
            shutil.rmtree(self.build_view, onerror=remove_readonly)
        # The extracted package remains immutable test input. Maven gets a
        # self-contained copy with runtime symlinks materialized, then java_ori
        # is restored as java to avoid accidental launcher use. This prevents
        # setup-java trust-store links from becoming invalid in the copied JDK.
        shutil.copytree(self._packaged_root, self.build_view)
        java = self.build_view / "bin" / java_name()
        java_ori = self.build_view / "bin" / java_ori_name()
        shutil.copy2(java_ori, java)
        if not IS_WINDOWS:
            make_executable(java)
        self._build_java_home = self.build_view
        if sha256(self._packaged_launcher) != self._packaged_launcher_hash:
            raise CompatError("immutable packaged launcher changed while creating build view")
        self.command("build-java-version", [str(java), "-version"], cwd=self.work, timeout=30)

    def _build_fixture(self) -> None:
        assert self._build_java_home is not None
        env = self.tool_env(self._build_java_home)
        cmd = self.maven + ["--batch-mode", "--no-transfer-progress", "clean", "package"]
        if self.args.maven_args:
            cmd.extend(self.args.maven_args)
        self.command("maven-build", cmd, cwd=self.fixture, env=env, timeout=self.args.build_timeout)
        self._fixture_jar = find_fixture_jar(self.fixture)
        self.result["artifacts"]["fixture_jar"] = describe_file(self._fixture_jar)

    def _check_jar(self, jar: Optional[Path], encrypted: bool) -> None:
        if jar is None or not jar.is_file():
            raise CompatError("JAR is missing")
        with zipfile.ZipFile(jar) as archive:
            if encrypted:
                validate_signature_comment(archive.comment)
            bad = archive.testzip()
            if bad:
                raise CompatError("corrupt JAR entry: " + bad)
            names = archive.namelist()
            if "META-INF/MANIFEST.MF" not in names:
                raise CompatError("JAR has no manifest")
            classes = [name for name in names if name.endswith(".class")]
            if not classes:
                raise CompatError("JAR contains no class files")
            boot_classes = [name for name in classes if name.startswith("BOOT-INF/classes/")]
            is_boot = any(name.startswith("BOOT-INF/") for name in names)
            if is_boot and not boot_classes:
                raise CompatError("Spring Boot JAR has no BOOT-INF/classes/*.class entries")
            inspected = boot_classes or classes
            majors: Dict[str, int] = {}
            for name in inspected:
                data = archive.read(name)
                header = data[:8]
                if len(header) < 8 or header[:4] != CLASS_MAGIC:
                    raise CompatError("invalid class header: " + name)
                major = struct.unpack(">H", header[6:8])[0]
                majors[name] = major
                if major != self.expected_class_major:
                    raise CompatError("class major mismatch for %s: expected %d, found %d" % (name, self.expected_class_major, major))
                if encrypted and name.startswith("BOOT-INF/classes/") and not data.endswith(ENCRYPT_CLASS_SUFFIX):
                    raise CompatError("protected application class has no Java Guard marker: " + name)
            protected_resources = [
                name for name in (
                    "BOOT-INF/classes/compat-marker.txt",
                    "BOOT-INF/classes/compat/data/fixture.json",
                    "BOOT-INF/classes/compat/data/profile.properties",
                    "BOOT-INF/classes/compat/data/unicode.txt",
                ) if name in names
            ]
            if encrypted:
                for name in protected_resources:
                    if not archive.read(name).startswith(ENCRYPT_RESOURCE_HEADER):
                        raise CompatError("protected resource has no Java Guard header: " + name)
                if is_boot and not protected_resources:
                    raise CompatError("protected fixture contains none of the expected Java Guard resources")
            summary = {
                "path": str(jar),
                "entries": len(names),
                "classes": len(classes),
                "boot_classes": len(boot_classes),
                "class_majors": sorted(set(majors.values())),
                "is_spring_boot": is_boot,
            }
            if encrypted and is_boot:
                summary["match_rule"] = "*"
                summary["whole_archive_checked"] = True
            self.result["artifacts"]["protected_jar" if encrypted else "checked_fixture_jar"] = summary

    def _protect_fixture(self) -> None:
        assert self._fixture_jar is not None
        protect_out = self.work / "protected"
        if protect_out.exists():
            shutil.rmtree(protect_out, onerror=remove_readonly)
        protect_out.mkdir(parents=True)
        config_text = self.config.read_text(encoding="utf-8")
        if not re.search(r"(?m)^\s*-\s*['\"]\*['\"]\s*$", config_text):
            raise CompatError("compat config must contain the whole-archive match '*' before encryption")
        java = java_executable(self.args.tool_jdk_home or self._build_java_home)
        cmd = [str(java), "-jar", str(self.guard_jar), "-c", str(self.config),
               "-o", str(protect_out), str(self._fixture_jar)]
        self.command("java-guard-encrypt", cmd, cwd=self.repo, env=self.tool_env(self.args.tool_jdk_home or self._build_java_home), timeout=self.args.command_timeout, redact_command=True)
        protected = protect_out / self._fixture_jar.name
        if not protected.is_file():
            raise CompatError("protected fixture was not created: " + str(protected))
        compare_jar_structure(self._fixture_jar, protected)
        validate_protected_entries(self._fixture_jar, protected)
        self._protected_jar = protected
        self.result["artifacts"]["protected_jar_file"] = describe_file(protected)

    def _locate_existing_protected_fixture(self) -> None:
        source = self.args.protected_jar
        if source is None:
            raise CompatError("launcher-only mode (-l) requires --protected-jar")
        self._protected_jar = source.resolve()
        if not self._protected_jar.is_file():
            raise CompatError("protected JAR does not exist: " + str(self._protected_jar))

    def _new_launch_identity(self) -> Tuple[str, Dict[str, str]]:
        token = secrets.token_urlsafe(32)
        self._secrets.append(token)
        return token, {LAUNCH_TOKEN_HEADER: token}

    def _test_startup(self) -> None:
        port = self.args.port or reserve_port(self.args.host)
        token, headers = self._new_launch_identity()
        process = self.start_launcher(self._protected_jar, port, "launcher", token)
        try:
            response = wait_http(self.args.host, port, self.args.check_path, self.args.startup_timeout,
                                 self.args.expected_status, self.args.expected_body, process, headers)
            self._validate_check_response(response, process, token)
            self.result["http"] = response
            if process.poll() is not None:
                raise CompatError("launcher exited after health check with code %s" % process.returncode)
        finally:
            self.stop_process(process)
            validate_stopped_process_and_port(process, self.args.host, port)

    def _validate_check_response(self, response: Dict[str, object], process: subprocess.Popen,
                                 launch_token: str, expected_ok: bool = True) -> None:
        body = str(response.get("body", ""))
        try:
            payload = json.loads(body)
        except (TypeError, ValueError) as exc:
            raise CompatError("compatibility endpoint did not return valid JSON") from exc
        if not isinstance(payload, dict):
            raise CompatError("compatibility endpoint JSON must be an object")
        expected_keys = SUCCESS_RESPONSE_KEYS if expected_ok else FAILURE_RESPONSE_KEYS
        if set(payload) != expected_keys:
            raise CompatError("compatibility response schema mismatch: missing=%r extra=%r" %
                              (sorted(expected_keys - set(payload)), sorted(set(payload) - expected_keys)))
        if payload.get("ok") is not expected_ok:
            raise CompatError("compatibility endpoint ok mismatch: expected %r, found %r" %
                              (expected_ok, payload.get("ok")))
        checks = payload.get("checks")
        if not isinstance(checks, dict) or set(checks) != EXPECTED_CHECKS:
            found = set(checks) if isinstance(checks, dict) else set()
            raise CompatError("compatibility check set mismatch: missing=%r extra=%r" %
                              (sorted(EXPECTED_CHECKS - found), sorted(found - EXPECTED_CHECKS)))
        failed = sorted(str(name) for name, value in checks.items() if value is not True)
        if failed:
            raise CompatError("compatibility checks failed: " + ", ".join(failed))
        java_feature = self.expected_class_major - 44
        exact = {
            "fixture": "jdk%d" % java_feature,
            "bootVersion": EXPECTED_BOOT_VERSIONS.get(java_feature),
            "expectedClassMajor": self.expected_class_major,
            "classMajor": self.expected_class_major,
            "javaFeature": java_feature,
            "pid": process.pid,
            "launchTokenSha256": hashlib.sha256(launch_token.encode("utf-8")).hexdigest(),
        }
        mismatches = ["%s expected=%r actual=%r" % (key, value, payload.get(key))
                      for key, value in exact.items() if payload.get(key) != value]
        if mismatches:
            raise CompatError("HTTP response identity/runtime mismatch: " + "; ".join(mismatches))
        version = payload.get("javaVersion")
        if not isinstance(version, str) or java_version_feature(version) != java_feature:
            raise CompatError("HTTP Java version mismatch: expected feature %d, found %r" %
                              (java_feature, version))
        if self._packaged_root is None or not java_home_belongs_to_runtime(payload.get("javaHome"), self._packaged_root):
            raise CompatError("HTTP javaHome is outside packaged runtime: %r" % payload.get("javaHome"))
        response["json"] = payload
        response["checks"] = checks
        response.pop("body", None)

    def _test_forced_http_failure(self) -> None:
        port = self.args.port or reserve_port(self.args.host)
        token, headers = self._new_launch_identity()
        process = self.start_launcher(self._protected_jar, port, "forced-http-failure", token)
        try:
            normal = wait_http(self.args.host, port, self.args.check_path, self.args.startup_timeout,
                               self.args.expected_status, self.args.expected_body, process, headers)
            self._validate_check_response(normal, process, token)
            separator = "&" if "?" in self.args.check_path else "?"
            failure_path = self.args.check_path + separator + "fail=true"
            forced = request_http(self.args.host, port, failure_path, headers)
            if forced["status"] != self.args.failure_status:
                raise CompatError("forced HTTP failure returned %s, expected %s" % (forced["status"], self.args.failure_status))
            self._validate_check_response(forced, process, token, expected_ok=False)
            payload = forced["json"]
            if payload.get("error") != "requested failure":
                raise CompatError("forced HTTP failure returned unexpected error: %r" % payload.get("error"))
            if process.poll() is not None:
                raise CompatError("launcher exited during forced HTTP failure check")
            self.result["forced_http"] = {
                "normal_status": normal["status"], "failure_path": failure_path,
                "failure_status": forced["status"], "json_validated": True,
                "launch_token_sha256": hashlib.sha256(token.encode("utf-8")).hexdigest(),
            }
        finally:
            self.stop_process(process)
            validate_stopped_process_and_port(process, self.args.host, port)

    def _test_failures(self) -> None:
        assert self._protected_jar is not None
        tampered = self.work / "tampered.jar"
        shutil.copy2(self._protected_jar, tampered)
        tampered_entry = tamper_zip_entry(tampered)
        validate_tampered_jar(tampered, tampered_entry)
        port = self.args.port or reserve_port(self.args.host)
        tampered_code = self.run_launcher_expect_failure(tampered, port, "tampered-signature")
        validate_nonzero_exit(tampered_code, "tampered signed JAR unexpectedly launched successfully")
        tampered_log_path = self.logs / "tampered-signature.log"
        tampered_output = tampered_log_path.read_text(encoding="utf-8", errors="replace")
        validate_signature_rejection(tampered_output)
        occupied = reserve_listening_socket(self.args.host)
        occupied_port = occupied.getsockname()[1]
        process: Optional[subprocess.Popen] = None
        try:
            process = self.start_launcher(self._protected_jar, occupied_port, "occupied-port")
            code = wait_for_exit(process, self.args.failure_timeout)
            pump = getattr(process, "_compat_pump", None)
            if pump:
                pump.join(timeout=5)
            log_path = self.logs / "occupied-port.log"
            output = log_path.read_text(encoding="utf-8", errors="replace")
            validate_occupied_port_failure(output, occupied_port)
            validate_nonzero_exit(code, "occupied-port application failed but launcher returned exit code 0")
        finally:
            if process is not None:
                self.stop_process(process)
            occupied.close()
        assert process is not None
        validate_stopped_process_and_port(process, self.args.host, occupied_port)
        self.result["forced_failures"] = {
            "tampered_jar_exit": tampered_code,
            "tampered_entry": tampered_entry,
            "tampered_jar_structurally_valid": True,
            "tampered_signature_failure_evidence": True,
            "occupied_port_exit": code,
            "occupied_port_ready": False,
            "occupied_port_failure_evidence": True,
        }

    def start_launcher(self, jar: Optional[Path], port: int, log_name: str,
                       launch_token: Optional[str] = None) -> subprocess.Popen:
        if jar is None or self._packaged_launcher is None:
            raise CompatError("launcher or protected JAR is missing")
        if launch_token is None:
            launch_token, _ = self._new_launch_identity()
        elif launch_token not in self._secrets:
            self._secrets.append(launch_token)
        log_path = self.logs / (log_name + ".log")
        cmd = [str(self._packaged_launcher), "-Dserver.address=" + self.args.host,
               "-Dserver.port=" + str(port), "-Dcompat.launch-token=" + launch_token,
               "-jar", str(jar)]
        print("[compat]   launcher %s:%d (log: %s)" % (self.args.host, port, log_path), flush=True)
        kwargs = process_group_kwargs()
        process = subprocess.Popen(cmd, cwd=str(self._packaged_root), stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL, **kwargs)
        pump = OutputPump(process, log_path, self._redact, self.args.console_output == "full", log_name)
        pump.start()
        process._compat_pump = pump  # type: ignore[attr-defined]
        process._compat_log = log_path  # type: ignore[attr-defined]
        process._compat_port = port  # type: ignore[attr-defined]
        self.result.setdefault("launches", []).append({
            "name": log_name, "pid": process.pid, "host": self.args.host,
            "port": port, "log": str(log_path),
        })
        self._processes.append(process)
        self._last_failed_log = log_path
        return process

    def run_launcher_expect_failure(self, jar: Path, port: int, log_name: str) -> int:
        process = self.start_launcher(jar, port, log_name)
        try:
            return wait_for_exit(process, self.args.failure_timeout)
        finally:
            self.stop_process(process)
            validate_stopped_process_and_port(process, self.args.host, port)

    def stop_process(self, process: subprocess.Popen) -> None:
        if process.poll() is None:
            terminate_tree(process, self.args.cleanup_timeout)
        with contextlib.suppress(ValueError):
            self._processes.remove(process)
        pump = getattr(process, "_compat_pump", None)
        if pump:
            pump.join(timeout=max(1, self.args.cleanup_timeout))

    def _cleanup_processes(self) -> None:
        for process in list(reversed(self._processes)):
            with contextlib.suppress(Exception):
                self.stop_process(process)

    def command(self, name: str, cmd: Sequence[str], cwd: Path, timeout: int,
                env: Optional[Dict[str, str]] = None, redact_command: bool = False) -> None:
        log_path = self.logs / (name + ".log")
        displayed = "<redacted command>" if redact_command else self._redact(subprocess.list2cmdline([str(x) for x in cmd]))
        print("[compat]   %s (cwd: %s, log: %s)" % (displayed, cwd, log_path), flush=True)
        kwargs = process_group_kwargs()
        process = subprocess.Popen([str(x) for x in cmd], cwd=str(cwd), env=env, stdin=subprocess.DEVNULL,
                                   stdout=subprocess.PIPE, stderr=subprocess.STDOUT, **kwargs)
        self._processes.append(process)
        pump = OutputPump(process, log_path, self._redact, self.args.console_output == "full", name)
        pump.start()
        try:
            try:
                code = process.wait(timeout=timeout)
            except subprocess.TimeoutExpired:
                terminate_tree(process, self.args.cleanup_timeout)
                raise CompatError("command timed out after %ds: %s (log: %s)" % (timeout, name, log_path))
            finally:
                pump.join(timeout=max(1, self.args.cleanup_timeout))
                with contextlib.suppress(ValueError):
                    self._processes.remove(process)
            if code != 0:
                raise CompatError("command failed with exit code %d: %s (log: %s)" % (code, name, log_path))
            print("[compat]   command %s exited 0" % name, flush=True)
        except Exception:
            self._last_failed_log = log_path
            raise

    def tool_env(self, java_home: Optional[Path]) -> Dict[str, str]:
        env = os.environ.copy()
        if java_home and java_home.is_dir():
            home = java_home.resolve()
            env["JAVA_HOME"] = str(home)
            env["PATH"] = str(home / "bin") + os.pathsep + env.get("PATH", "")
        env["ORI_JAVA"] = str(self.jdk_source)
        return env

    def _print_failure_tail(self) -> None:
        path = getattr(self, "_last_failed_log", None)
        if not path or not path.is_file():
            return
        lines = path.read_text(encoding="utf-8", errors="replace").splitlines()[-100:]
        print("[compat] last output from %s:" % path, file=sys.stderr)
        for line in lines:
            print("[compat] | " + self._redact(line), file=sys.stderr)

    def _write_traceback(self, exc: Exception) -> None:
        path = self.logs / "runner-error.log"
        text = "".join(traceback.format_exception(type(exc), exc, exc.__traceback__))
        path.write_text(self._redact(text), encoding="utf-8")

    def _redact(self, text: str) -> str:
        secrets_to_redact = set(secret for secret in self._secrets if secret)
        secrets_to_redact.update(json.dumps(secret)[1:-1] for secret in list(secrets_to_redact))
        for secret in sorted(secrets_to_redact, key=len, reverse=True):
            text = text.replace(secret, "<redacted>")
        return text

    def _write_report(self) -> None:
        for path in self.logs.glob("*"):
            if path.is_file():
                redact_file(path, self._secrets)
        report = self.report_dir / "compat-report.json"
        temporary = report.with_suffix(".json.tmp")
        serialized = json.dumps(self.result, indent=2, sort_keys=True) + "\n"
        temporary.write_text(self._redact(serialized), encoding="utf-8")
        os.replace(str(temporary), str(report))
        print("[compat] report: " + str(report), flush=True)


def parse_args(argv: Optional[Sequence[str]] = None) -> argparse.Namespace:
    here = Path(__file__).resolve().parent
    default_repo = here.parent
    parser = argparse.ArgumentParser(description="Build and exercise Java Guard against a Maven compatibility fixture.")
    parser.add_argument("-l", "--launcher-only", action="store_true", help="reuse a protected JAR and its matching key-bound --package-archive")
    parser.add_argument("--repo", type=Path, default=default_repo, help="Java Guard repository (default: parent of this script)")
    parser.add_argument("--java-guard-jar", type=Path, help="Java Guard executable JAR (default: newest target/java-guard-*.jar)")
    parser.add_argument("--jdk-home", type=Path, required=True, help="target JDK/JRE directory or .zip/.tar.gz/.tgz archive to package")
    parser.add_argument("--tool-jdk-home", type=Path, help="JDK used to execute Java Guard; defaults to --jdk-home or its extracted build view")
    parser.add_argument("--fixture", default="jdk21", help="Maven fixture name below compat-tests/apps, or a path")
    parser.add_argument("--maven-home", type=Path, help="Maven installation; defaults to mvnw or PATH")
    parser.add_argument("--work-dir", type=Path, default=here / "work", help="private generated work directory")
    parser.add_argument("--report-dir", type=Path, default=here / "reports", help="redacted JSON/log output directory")
    parser.add_argument("--console-output", choices=("full", "summary"), default="full",
                        help="stream full redacted child output or only progress/failure summaries (default: full)")
    parser.add_argument("--config-template", type=Path, default=here / "config" / "compat.yml.in")
    parser.add_argument("--skip-package", action="store_true", help="reuse --package-archive instead of invoking Java Guard -l")
    parser.add_argument("--package-archive", type=Path, help="existing packaged runtime for --skip-package")
    parser.add_argument("--protected-jar", type=Path, help="existing signed/encrypted fixture, required with -l")
    parser.add_argument("--build-only", action="store_true", help="stop after package extraction, fixture build, and JAR checks")
    parser.add_argument("--skip-deps", action="store_true", help="pass --skip-deps to Java Guard launcher generation")
    parser.add_argument("--clean", action=argparse.BooleanOptionalAction, default=True, help="clean work directory first (default: true)")
    parser.add_argument("--maven-arg", dest="maven_args", action="append", default=[], help="extra Maven argument; repeatable")
    parser.add_argument("--expected-class-major", type=int, help="require this class-file major (for example 52 for Java 8)")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=0, help="fixed application port; 0 chooses a free port")
    parser.add_argument("--check-path", default="/compat/check")
    parser.add_argument("--expected-status", type=int, default=200)
    parser.add_argument("--expected-body", default=DEFAULT_EXPECTED_BODY)
    parser.add_argument("--failure-status", type=int, default=500, help="expected status for /compat/check?fail=true")
    parser.add_argument("--command-timeout", type=int, default=300)
    parser.add_argument("--package-timeout", type=int, default=1800)
    parser.add_argument("--build-timeout", type=int, default=900)
    parser.add_argument("--startup-timeout", type=int, default=120)
    parser.add_argument("--failure-timeout", type=int, default=45)
    parser.add_argument("--cleanup-timeout", type=int, default=10)
    args = parser.parse_args(argv)
    if args.launcher_only:
        if not args.skip_package:
            parser.error("-l/--launcher-only requires --skip-package with the matching key-bound runtime archive")
        if not args.package_archive:
            parser.error("-l/--launcher-only requires --package-archive")
        if not args.protected_jar and not args.build_only:
            parser.error("-l/--launcher-only requires --protected-jar unless --build-only is used")
    if args.tool_jdk_home and not args.tool_jdk_home.is_dir():
        parser.error("--tool-jdk-home must be an extracted JDK directory")
    return args


def infer_class_major(fixture: Path) -> Optional[int]:
    match = re.fullmatch(r"jdk(\d+)", fixture.name.lower())
    if not match:
        return None
    version = int(match.group(1))
    return 44 + version if version >= 2 else None


def validate_output_paths(repo: Path, work: Path, report: Path) -> None:
    repo = repo.resolve()
    work = work.resolve()
    report = report.resolve()
    roots = {Path(work.anchor).resolve(), Path(report.anchor).resolve()}
    if work in roots or report in roots or work == repo or report == repo:
        raise CompatError("work/report directory must not be a filesystem or repository root")
    if work == report or work in report.parents or report in work.parents:
        raise CompatError("work and report directories must not overlap")
    if work in repo.parents or report in repo.parents:
        raise CompatError("work/report directory must not contain the repository")


class OutputPump(threading.Thread):
    def __init__(self, process: subprocess.Popen, path: Path, redact, show_console: bool, name: str) -> None:
        super().__init__(name="compat-output-" + name, daemon=True)
        self.process = process
        self.path = path
        self.redact = redact
        self.show_console = show_console

    def run(self) -> None:
        stream = self.process.stdout
        if stream is None:
            return
        with open(self.path, "w", encoding="utf-8", errors="replace", newline="") as log:
            while True:
                raw = stream.readline()
                if not raw:
                    break
                line = self.redact(raw.decode("utf-8", errors="replace"))
                log.write(line)
                log.flush()
                if self.show_console:
                    print("[compat] | " + line.rstrip("\r\n"), flush=True)
        stream.close()


def find_guard_jar(explicit: Optional[Path], repo: Path) -> Path:
    if explicit:
        return explicit.resolve()
    jars = [p for p in (repo / "target").glob("java-guard-*.jar") if "sources" not in p.name and "javadoc" not in p.name]
    if not jars:
        raise CompatError("no Java Guard JAR found; pass --java-guard-jar")
    return max(jars, key=lambda p: p.stat().st_mtime).resolve()


def find_maven(home: Optional[Path], fixture: Path, repo: Path) -> List[str]:
    if home:
        executable = home.resolve() / "bin" / ("mvn.cmd" if IS_WINDOWS else "mvn")
        if not executable.is_file():
            raise CompatError("Maven executable not found: " + str(executable))
        return [str(executable)]
    wrapper_name = "mvnw.cmd" if IS_WINDOWS else "mvnw"
    for root in (fixture, repo):
        wrapper = root / wrapper_name
        if wrapper.is_file():
            return [str(wrapper)]
    name = "mvn.cmd" if IS_WINDOWS else "mvn"
    found = shutil.which(name) or shutil.which("mvn")
    if not found:
        raise CompatError("Maven not found; pass --maven-home")
    return [found]


def require_tool(name: str) -> str:
    found = shutil.which(name)
    if not found:
        raise CompatError("required tool not found on PATH: " + name)
    return found


def java_executable(home: Optional[Path]) -> Path:
    if home is None:
        found = shutil.which(java_name()) or shutil.which("java")
        if not found:
            raise CompatError("java not found")
        return Path(found)
    home = home.resolve()
    if home.is_file():
        found = shutil.which(java_name()) or shutil.which("java")
        if not found:
            raise CompatError("--jdk-home is an archive; pass --tool-jdk-home or put java on PATH")
        return Path(found)
    executable = home / "bin" / java_name()
    if not executable.is_file():
        raise CompatError("java executable not found: " + str(executable))
    return executable


def select_runtime_archive(directory: Path) -> Path:
    archives = sorted(list(directory.glob("jg-*.zip")) + list(directory.glob("jg-*.tar.gz")) + list(directory.glob("jg-*.tgz")), key=lambda p: p.stat().st_mtime)
    if len(archives) != 1:
        raise CompatError("expected exactly one jg-* runtime archive in %s, found %d" % (directory, len(archives)))
    return archives[0].resolve()


def safe_extract(archive: Path, destination: Path) -> None:
    destination = destination.resolve()
    if archive.name.lower().endswith(".zip"):
        with zipfile.ZipFile(archive) as source:
            for info in source.infolist():
                target = safe_member(destination, info.filename)
                if info.is_dir():
                    target.mkdir(parents=True, exist_ok=True)
                    continue
                target.parent.mkdir(parents=True, exist_ok=True)
                with source.open(info) as src, open(target, "wb") as dst:
                    shutil.copyfileobj(src, dst)
                mode = info.external_attr >> 16
                if mode:
                    with contextlib.suppress(OSError):
                        target.chmod(mode)
    elif archive.name.lower().endswith((".tar.gz", ".tgz")):
        with tarfile.open(archive, "r:gz") as source:
            hardlinks: List[Tuple[Path, Path]] = []
            for member in source.getmembers():
                target = safe_member(destination, member.name)
                if member.isdir():
                    target.mkdir(parents=True, exist_ok=True)
                elif member.isfile():
                    target.parent.mkdir(parents=True, exist_ok=True)
                    stream = source.extractfile(member)
                    if stream is None:
                        raise CompatError("cannot read archive member: " + member.name)
                    with stream, open(target, "wb") as dst:
                        shutil.copyfileobj(stream, dst)
                    target.chmod(member.mode)
                elif member.issym():
                    validate_link_target(destination, target.parent, member.linkname, member.name)
                    target.parent.mkdir(parents=True, exist_ok=True)
                    os.symlink(member.linkname, target)
                elif member.islnk():
                    hardlinks.append((target, safe_member(destination, member.linkname)))
                else:
                    raise CompatError("unsupported archive member: " + member.name)
            for target, link_target in hardlinks:
                if not link_target.is_file():
                    raise CompatError("archive hard-link target is missing: " + str(link_target))
                target.parent.mkdir(parents=True, exist_ok=True)
                os.link(link_target, target)
    else:
        raise CompatError("unsupported runtime archive: " + str(archive))


def safe_member(root: Path, name: str) -> Path:
    normalized = PurePosixPath(name.replace("\\", "/"))
    if normalized.is_absolute() or ".." in normalized.parts or (normalized.parts and ":" in normalized.parts[0]):
        raise CompatError("unsafe archive path: " + name)
    target = (root / Path(*normalized.parts)).resolve()
    try:
        target.relative_to(root)
    except ValueError:
        raise CompatError("archive path escapes destination: " + name)
    return target


def validate_link_target(root: Path, parent: Path, link_name: str, member_name: str) -> None:
    link = PurePosixPath(link_name.replace("\\", "/"))
    if link.is_absolute() or (link.parts and ":" in link.parts[0]):
        raise CompatError("unsafe archive link: " + member_name)
    resolved = (parent / Path(*link.parts)).resolve()
    try:
        resolved.relative_to(root)
    except ValueError:
        raise CompatError("archive link escapes destination: " + member_name)


def find_runtime_root(extracted: Path) -> Path:
    candidates = sorted({
        java_ori.parent.parent
        for java_ori in extracted.rglob(java_ori_name())
        if java_ori.parent.name == "bin"
    }, key=lambda path: (len(path.parts), str(path)))
    if len(candidates) == 1:
        return candidates[0]

    # JDK 8 packages can contain launchers in both the full JDK and its nested
    # JRE. Prefer the full JDK only when it contains every other candidate;
    # candidates in unrelated directory trees remain ambiguous.
    containing = [
        root for root in candidates
        if all(root == candidate or root in candidate.parents for candidate in candidates)
    ]
    if len(containing) == 1:
        return containing[0]
    raise CompatError("expected one packaged runtime root, found %d" % len(candidates))


def find_fixture_jar(fixture: Path) -> Path:
    jars = []
    for jar in (fixture / "target").glob("*.jar"):
        if jar.name.startswith("original-") or jar.name.endswith(("-sources.jar", "-javadoc.jar")):
            continue
        jars.append(jar)
    if not jars:
        raise CompatError("Maven did not create a fixture JAR in " + str(fixture / "target"))
    boot = []
    for jar in jars:
        try:
            with zipfile.ZipFile(jar) as archive:
                if any(name.startswith("BOOT-INF/classes/") for name in archive.namelist()):
                    boot.append(jar)
        except zipfile.BadZipFile:
            continue
    selected = boot or jars
    if len(selected) != 1:
        raise CompatError("expected exactly one executable fixture JAR, found: " + ", ".join(p.name for p in selected))
    return selected[0].resolve()


def compare_jar_structure(original: Path, protected: Path) -> None:
    with zipfile.ZipFile(original) as before, zipfile.ZipFile(protected) as after:
        before_names = set(before.namelist())
        after_names = set(after.namelist())
        before_boot = {n for n in before_names if n.startswith("BOOT-INF/classes/") and n.endswith(".class")}
        after_boot = {n for n in after_names if n.startswith("BOOT-INF/classes/") and n.endswith(".class")}
        if before_boot and before_boot != after_boot:
            missing = sorted(before_boot - after_boot)[:5]
            extra = sorted(after_boot - before_boot)[:5]
            raise CompatError("BOOT-INF/classes/* mismatch after encryption; missing=%r extra=%r" % (missing, extra))
        if "META-INF/MANIFEST.MF" not in after_names:
            raise CompatError("protected JAR lost its manifest")


def validate_protected_entries(original: Path, protected: Path) -> None:
    if sha256(original) == sha256(protected):
        raise CompatError("protected JAR is byte-for-byte identical to the original")
    with zipfile.ZipFile(original) as before, zipfile.ZipFile(protected) as after:
        before_names = before.namelist()
        after_names = set(after.namelist())
        classes = sorted(name for name in before_names
                         if name.startswith("BOOT-INF/classes/") and name.endswith(".class"))
        if not classes:
            raise CompatError("fixture has no application classes to verify as encrypted")
        for name in classes:
            if name not in after_names:
                raise CompatError("protected application class is missing: " + name)
            if not after.read(name).endswith(ENCRYPT_CLASS_SUFFIX):
                raise CompatError("protected application class has no Java Guard marker: " + name)

        resources = sorted(name for name in before_names
                           if name.startswith("BOOT-INF/classes/")
                           and not name.endswith("/")
                           and not name.endswith(".class")
                           and not is_manifest_entry(name))
        if not resources:
            raise CompatError("fixture has no application resources to verify as encrypted")
        for name in resources:
            if name not in after_names:
                raise CompatError("protected application resource is missing: " + name)
            if not after.read(name).startswith(ENCRYPT_RESOURCE_HEADER):
                raise CompatError("protected resource has no Java Guard header: " + name)

        nested_jars = sorted(name for name in before_names
                             if name.startswith("BOOT-INF/lib/")
                             and name.endswith(".jar")
                             and not name.endswith("/"))
        if not nested_jars:
            raise CompatError("fixture has no BOOT-INF/lib nested JAR to verify")
        nested_errors = []
        for name in nested_jars:
            if name not in after_names:
                raise CompatError("protected nested JAR is missing: " + name)
            try:
                if validate_protected_nested_jar(name, after.read(name)):
                    break
            except (OSError, zipfile.BadZipFile, RuntimeError) as exc:
                nested_errors.append("%s: %s" % (name, exc))
        else:
            detail = "; ".join(nested_errors[:3])
            suffix = " (" + detail + ")" if detail else ""
            raise CompatError("no representative BOOT-INF/lib nested JAR contains both classes and resources" + suffix)


def is_manifest_entry(name: str) -> bool:
    normalized = name.replace("\\", "/").upper()
    return normalized == "META-INF/MANIFEST.MF" or normalized.endswith("/META-INF/MANIFEST.MF")


def validate_protected_nested_jar(name: str, data: bytes) -> bool:
    """Validate one recursively transformed nested JAR; its bytes remain a ZIP, not a JGR resource."""
    if data.startswith(ENCRYPT_RESOURCE_HEADER):
        raise CompatError("protected nested JAR was transformed as an opaque JGR resource: " + name)
    with zipfile.ZipFile(io.BytesIO(data)) as nested:
        bad = nested.testzip()
        if bad:
            raise CompatError("corrupt entry %s in protected nested JAR %s" % (bad, name))
        entries = nested.namelist()
        classes = sorted(entry for entry in entries if entry.endswith(".class"))
        resources = sorted(entry for entry in entries
                           if not entry.endswith("/")
                           and not entry.endswith(".class")
                           and not is_manifest_entry(entry))
        if not classes or not resources:
            return False
        for entry in classes:
            if not nested.read(entry).endswith(ENCRYPT_CLASS_SUFFIX):
                raise CompatError("protected nested class has no Java Guard marker: %s!/%s" % (name, entry))
        for entry in resources:
            if not nested.read(entry).startswith(ENCRYPT_RESOURCE_HEADER):
                raise CompatError("protected nested resource has no Java Guard header: %s!/%s" % (name, entry))
        return True


def validate_signature_comment(comment: bytes) -> None:
    trailer_length = 4
    signature_magic = b"jgs-v1:"
    if len(comment) <= trailer_length or not re.fullmatch(rb"[0-9a-f]{4}", comment[-trailer_length:]):
        raise CompatError("protected JAR has no canonical Java Guard signature trailer")
    # ZipSignUtils hex-encodes the two-byte little-endian signature length;
    # Rust jar_info decodes those bytes with u16::from_le_bytes.
    encoded_length = int.from_bytes(bytes.fromhex(comment[-trailer_length:].decode("ascii")), "little")
    encoded_end = len(comment) - trailer_length
    encoded_start = encoded_end - encoded_length
    marker_start = encoded_start - len(signature_magic)
    if encoded_length <= 0 or marker_start < 0:
        raise CompatError("protected JAR signature trailer length is invalid")
    if comment[marker_start:encoded_start] != signature_magic:
        raise CompatError("protected JAR has no canonical Java Guard signature marker")
    encoded = comment[encoded_start:encoded_end]
    if not re.fullmatch(rb"[A-Za-z0-9_-]+", encoded):
        raise CompatError("protected JAR signature encoding is invalid")
    try:
        signature = base64.b64decode(encoded + b"=" * (-len(encoded) % 4), altchars=b"-_", validate=True)
    except ValueError as exc:
        raise CompatError("protected JAR signature encoding is invalid") from exc
    if len(signature) != 64 or base64.urlsafe_b64encode(signature).rstrip(b"=") != encoded:
        raise CompatError("protected JAR signature encoding is not canonical Ed25519")


def tamper_zip_entry(path: Path) -> str:
    """Change one STORED entry while keeping ZIP metadata and the signature comment valid."""
    with zipfile.ZipFile(path) as archive:
        original_comment = archive.comment
        validate_signature_comment(original_comment)
        candidates = [
            info for info in archive.infolist()
            if info.compress_type == zipfile.ZIP_STORED
            and info.file_size > 0
            and not info.is_dir()
            and not (info.flag_bits & 0x09)  # no encryption or data descriptor
        ]
        candidates.sort(key=lambda info: (
            not info.filename.startswith("BOOT-INF/lib/"),
            info.filename == "META-INF/MANIFEST.MF",
            -info.file_size,
            info.filename,
        ))
        if not candidates:
            raise CompatError("protected JAR has no suitable STORED entry to tamper safely")
        target = candidates[0]
        payload = bytearray(archive.read(target))

    # STORED data can be changed in place. Updating both CRC fields keeps the
    # archive readable without rewriting unrelated bytes or its signed comment.
    payload[len(payload) // 2] ^= 1
    crc = binascii.crc32(payload) & 0xFFFFFFFF
    with open(path, "r+b") as stream:
        stream.seek(target.header_offset)
        local_header = stream.read(30)
        if len(local_header) != 30 or local_header[:4] != b"PK\x03\x04":
            raise CompatError("invalid local ZIP header for tamper target: " + target.filename)
        name_length, extra_length = struct.unpack_from("<HH", local_header, 26)
        data_offset = target.header_offset + 30 + name_length + extra_length
        stream.seek(data_offset)
        stream.write(payload)
        stream.seek(target.header_offset + 14)
        stream.write(struct.pack("<I", crc))

        central_offset = find_central_directory_entry(stream, target.header_offset)
        stream.seek(central_offset + 16)
        stream.write(struct.pack("<I", crc))

    with zipfile.ZipFile(path) as archive:
        if archive.comment != original_comment:
            raise CompatError("tampering changed the Java Guard signature comment")
    return target.filename


def find_central_directory_entry(stream, local_header_offset: int) -> int:
    stream.seek(0, os.SEEK_END)
    size = stream.tell()
    tail_length = min(size, 65535 + 22)
    stream.seek(size - tail_length)
    tail = stream.read(tail_length)
    eocd_index = tail.rfind(b"PK\x05\x06")
    while eocd_index >= 0:
        if len(tail) - eocd_index >= 22:
            comment_length = struct.unpack_from("<H", tail, eocd_index + 20)[0]
            if eocd_index + 22 + comment_length == len(tail):
                break
        eocd_index = tail.rfind(b"PK\x05\x06", 0, eocd_index)
    if eocd_index < 0:
        raise CompatError("cannot locate ZIP end-of-central-directory record")
    central_size, central_offset = struct.unpack_from("<II", tail, eocd_index + 12)
    if central_offset == 0xFFFFFFFF or central_size == 0xFFFFFFFF:
        raise CompatError("ZIP64 JAR tampering is not supported")

    cursor = central_offset
    central_end = central_offset + central_size
    while cursor < central_end:
        stream.seek(cursor)
        header = stream.read(46)
        if len(header) != 46 or header[:4] != b"PK\x01\x02":
            raise CompatError("invalid ZIP central-directory entry")
        name_length, extra_length, comment_length = struct.unpack_from("<HHH", header, 28)
        entry_local_offset = struct.unpack_from("<I", header, 42)[0]
        if entry_local_offset == local_header_offset:
            return cursor
        cursor += 46 + name_length + extra_length + comment_length
    raise CompatError("tamper target is missing from ZIP central directory")


def validate_tampered_jar(path: Path, expected_entry: str) -> None:
    try:
        with zipfile.ZipFile(path) as archive:
            validate_signature_comment(archive.comment)
            if expected_entry not in archive.namelist():
                raise CompatError("tampered JAR lost target entry: " + expected_entry)
            bad = archive.testzip()
            if bad:
                raise CompatError("tampered JAR failed structural CRC check: " + bad)
            if "META-INF/MANIFEST.MF" not in archive.namelist():
                raise CompatError("tampered JAR lost its manifest")
            archive.read("META-INF/MANIFEST.MF")
    except (OSError, zipfile.BadZipFile, RuntimeError) as exc:
        raise CompatError("tampered JAR is not a structurally valid ZIP: %s" % exc) from exc


def validate_signature_rejection(output: str) -> None:
    patterns = (
        r"\bjar signature verify failed\b",
        r"\bjar signature verification failed\b",
        r"\bsignature verification (?:failed|failure)\b",
        r"\b(?:verify|verification) of (?:the )?jar signature failed\b",
    )
    if not any(re.search(pattern, output, re.IGNORECASE) for pattern in patterns):
        raise CompatError("tampered JAR launcher log has no signature-verification rejection evidence")


def validate_occupied_port_failure(output: str, port: int) -> None:
    success_patterns = (
        r"\bStarted CompatApplication\b",
        r"\bTomcat started on port(?:\(s\))?\b",
        r"\b(?:Netty|Undertow) started on port\b",
    )
    found_success = [pattern for pattern in success_patterns if re.search(pattern, output, re.IGNORECASE)]
    if found_success:
        raise CompatError("occupied-port application unexpectedly reported startup success/readiness")

    if "APPLICATION FAILED TO START" not in output:
        raise CompatError("occupied-port application did not report APPLICATION FAILED TO START")

    escaped_port = re.escape(str(port))
    port_failure_patterns = (
        r"\bPort\s+%s\s+(?:was|is) already in use\b" % escaped_port,
        r"\bconnector configured to listen on port\s+%s\s+failed to start\b" % escaped_port,
        r"\bFailed to start component \[Connector\[[^\]\r\n]*[-:]%s\]\]" % escaped_port,
    )
    if not any(re.search(pattern, output, re.IGNORECASE) for pattern in port_failure_patterns):
        raise CompatError("occupied-port application did not report port-specific bind failure evidence for port %d" % port)


def request_http(host: str, port: int, path: str,
                 headers: Optional[Dict[str, str]] = None) -> Dict[str, object]:
    connection = http.client.HTTPConnection(host, port, timeout=5)
    request_headers = {"Connection": "close"}
    if headers:
        request_headers.update(headers)
    try:
        connection.request("GET", path, headers=request_headers)
        response = connection.getresponse()
        body = response.read(1024 * 1024).decode("utf-8", errors="replace")
        return {"status": response.status, "body": body}
    finally:
        connection.close()


def wait_http(host: str, port: int, path: str, timeout: int, expected_status: int,
              expected_body: str, process: subprocess.Popen,
              headers: Optional[Dict[str, str]] = None) -> Dict[str, object]:
    deadline = time.monotonic() + timeout
    last_error = "not attempted"
    while time.monotonic() < deadline:
        code = process.poll()
        if code is not None:
            log_path = getattr(process, "_compat_log", None)
            suffix = " (log: %s)" % log_path if log_path else ""
            raise CompatError("launcher exited before HTTP readiness with code %s%s" % (code, suffix))
        try:
            response = request_http(host, port, path, headers)
            body = str(response["body"])
            if response["status"] == expected_status and expected_body in body:
                response.update({"host": host, "port": port, "path": path, "body_matched": True})
                return response
            last_error = "status=%d body=%r" % (response["status"], body[:200])
        except (OSError, http.client.HTTPException) as exc:
            last_error = str(exc)
        remaining = deadline - time.monotonic()
        if remaining > 0:
            time.sleep(min(0.5, remaining))
    code = process.poll()
    if code is not None:
        log_path = getattr(process, "_compat_log", None)
        suffix = " (log: %s)" % log_path if log_path else ""
        raise CompatError("launcher exited before HTTP readiness with code %s%s" % (code, suffix))
    raise CompatError("HTTP check timed out after %ds: %s" % (timeout, last_error))


def reserve_port(host: str) -> int:
    sock = reserve_listening_socket(host)
    try:
        return sock.getsockname()[1]
    finally:
        sock.close()


def reserve_listening_socket(host: str):
    import socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.bind((host, 0))
    sock.listen(1)
    return sock


def validate_nonzero_exit(code: int, message: str) -> None:
    if code == 0:
        raise CompatError(message)


def java_version_feature(version: str) -> Optional[int]:
    match = re.match(r"^(?:1\.)?(\d+)(?:[._+-]|$)", version)
    return int(match.group(1)) if match else None


def java_home_belongs_to_runtime(java_home: object, packaged_root: Path) -> bool:
    if not isinstance(java_home, str) or not java_home:
        return False
    home = Path(os.path.normcase(os.path.realpath(os.path.normpath(java_home))))
    root = Path(os.path.normcase(os.path.realpath(os.path.normpath(str(packaged_root)))))
    try:
        relative = home.relative_to(root)
    except ValueError:
        return False
    return relative == Path() or relative.parts == ("jre",)


def validate_stopped_process_and_port(process: subprocess.Popen, host: str, port: int) -> None:
    import socket
    if process.poll() is None:
        raise CompatError("launcher PID %d remained alive after stop" % process.pid)
    probe = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    probe.settimeout(0.25)
    try:
        if probe.connect_ex((host, port)) == 0:
            raise CompatError("tested port %d still accepts connections after launcher stop" % port)
    finally:
        probe.close()


def wait_for_exit(process: subprocess.Popen, timeout: int) -> int:
    try:
        return process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        raise CompatError("process did not fail within %ds" % timeout)


def process_group_kwargs() -> Dict[str, object]:
    if IS_WINDOWS:
        return {"creationflags": subprocess.CREATE_NEW_PROCESS_GROUP}
    return {"start_new_session": True}


def terminate_tree(process: subprocess.Popen, timeout: int) -> None:
    if process.poll() is not None:
        return
    if IS_WINDOWS:
        completed = subprocess.run(
            ["taskkill", "/PID", str(process.pid), "/T", "/F"],
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout,
            text=True, errors="replace",
        )
        if completed.returncode != 0 and process.poll() is None:
            raise CompatError("taskkill failed for PID %d with exit code %d: %s" %
                              (process.pid, completed.returncode, completed.stdout.strip()))
        try:
            process.wait(timeout=timeout)
        except subprocess.TimeoutExpired as exc:
            raise CompatError("process PID %d remained alive after taskkill" % process.pid) from exc
    else:
        with contextlib.suppress(ProcessLookupError):
            os.killpg(process.pid, signal.SIGTERM)
        try:
            process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            with contextlib.suppress(ProcessLookupError):
                os.killpg(process.pid, signal.SIGKILL)
            with contextlib.suppress(subprocess.TimeoutExpired):
                process.wait(timeout=timeout)


def describe_file(path: Path) -> Dict[str, object]:
    return {"path": str(path.resolve()), "size": path.stat().st_size, "sha256": sha256(path)}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def redact_file(path: Path, secrets_to_remove: Iterable[str]) -> None:
    try:
        data = path.read_bytes()
    except OSError:
        return
    for secret in secrets_to_remove:
        if secret:
            data = data.replace(secret.encode("utf-8"), b"<redacted>")
    path.write_bytes(data)


def yaml_quote(value: str) -> str:
    return json.dumps(value, ensure_ascii=True)


def restrict_directory(path: Path) -> None:
    with contextlib.suppress(OSError):
        path.chmod(stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)


def restrict_file(path: Path) -> None:
    with contextlib.suppress(OSError):
        path.chmod(stat.S_IRUSR | stat.S_IWUSR)


def make_executable(path: Path) -> None:
    path.chmod(path.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)


def remove_readonly(function, path, _exc_info) -> None:
    os.chmod(path, stat.S_IWRITE | stat.S_IREAD)
    function(path)


def java_name() -> str:
    return "java.exe" if IS_WINDOWS else "java"


def java_ori_name() -> str:
    return "java_ori.exe" if IS_WINDOWS else "java_ori"


def launcher_name() -> str:
    return "jg-launcher.exe" if IS_WINDOWS else "jg-launcher"


def utc_now() -> str:
    return dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = parse_args(argv)
    return Runner(args).run()


if __name__ == "__main__":
    sys.exit(main())
