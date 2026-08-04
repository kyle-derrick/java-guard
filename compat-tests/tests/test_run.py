import contextlib
import hashlib
import http.server
import importlib.util
import io
import json
import os
import subprocess
import sys
import tarfile
import tempfile
import threading
import time
import unittest
import zipfile
from pathlib import Path


RUNNER_PATH = Path(__file__).resolve().parents[1] / "run.py"
SPEC = importlib.util.spec_from_file_location("compat_runner", RUNNER_PATH)
runner = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(runner)


class RunnerHelpersTest(unittest.TestCase):
    def test_infer_supported_fixture_majors(self):
        expected = {"jdk8": 52, "jdk11": 55, "jdk17": 61, "jdk21": 65, "jdk25": 69}
        for name, major in expected.items():
            self.assertEqual(major, runner.infer_class_major(Path(name)))
        self.assertIsNone(runner.infer_class_major(Path("custom")))

    def test_default_directories_are_script_relative(self):
        args = runner.parse_args(["--jdk-home", str(Path.home())])
        root = RUNNER_PATH.parent
        self.assertEqual(root / "work", args.work_dir)
        self.assertEqual(root / "reports", args.report_dir)
        self.assertEqual("full", args.console_output)

    def test_output_directories_must_not_overlap(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            repo = root / "repo"
            repo.mkdir()
            with self.assertRaises(runner.CompatError):
                runner.validate_output_paths(repo, root / "same", root / "same")
            with self.assertRaises(runner.CompatError):
                runner.validate_output_paths(repo, repo, root / "reports")
            runner.validate_output_paths(repo, root / "work", root / "reports")

    def test_runner_rejects_removed_secret_transport_options(self):
        for option in ("--launcher-seed", "--launcher-bundle"):
            with self.subTest(option=option), contextlib.redirect_stderr(io.StringIO()):
                with self.assertRaises(SystemExit):
                    runner.parse_args(["--jdk-home", str(Path.home()), option, "bundle"])

    def test_launcher_only_requires_matching_package_archive(self):
        with contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                runner.parse_args(["--jdk-home", str(Path.home()), "--launcher-only",
                                   "--protected-jar", "protected.jar"])
        args = runner.parse_args(["--jdk-home", str(Path.home()), "--launcher-only",
                                  "--skip-package", "--package-archive", "runtime.zip",
                                  "--protected-jar", "protected.jar"])
        self.assertTrue(args.launcher_only)

    @unittest.skipIf(runner.IS_WINDOWS, "symbolic-link runtime layout is Unix-specific")
    def test_build_view_materializes_runtime_symlinks(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            packaged = root / "packaged"
            build_view = root / "build-view"
            bin_dir = packaged / "bin"
            security = packaged / "lib" / "security"
            bin_dir.mkdir(parents=True)
            security.mkdir(parents=True)
            launcher = bin_dir / runner.java_name()
            java_ori = bin_dir / runner.java_ori_name()
            launcher.write_bytes(b"launcher")
            java_ori.write_bytes(b"java-original")
            real_cacerts = security / "real-cacerts"
            real_cacerts.write_bytes(b"unit trust store")
            (security / "cacerts").symlink_to("real-cacerts")

            fake = object.__new__(runner.Runner)
            fake._packaged_root = packaged
            fake._packaged_launcher = launcher
            fake._packaged_launcher_hash = runner.sha256(launcher)
            fake.build_view = build_view
            fake.work = root
            fake.command = lambda *_args, **_kwargs: None

            fake._create_build_view()

            copied_cacerts = build_view / "lib" / "security" / "cacerts"
            self.assertFalse(copied_cacerts.is_symlink())
            self.assertEqual(b"unit trust store", copied_cacerts.read_bytes())
            self.assertTrue((security / "cacerts").is_symlink())
            self.assertEqual(b"java-original", (build_view / "bin" / runner.java_name()).read_bytes())
            self.assertEqual(runner.sha256(launcher), fake._packaged_launcher_hash)

    @unittest.skipIf(runner.IS_WINDOWS, "TAR symbolic links are Unix-specific")
    def test_safe_extract_preserves_contained_symlink(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            archive = root / "runtime.tar.gz"
            payload = b"unit trust store"
            with tarfile.open(archive, "w:gz") as target:
                data = tarfile.TarInfo("runtime/lib/security/real-cacerts")
                data.size = len(payload)
                target.addfile(data, io.BytesIO(payload))
                link = tarfile.TarInfo("runtime/lib/security/cacerts")
                link.type = tarfile.SYMTYPE
                link.linkname = "real-cacerts"
                target.addfile(link)
            extracted = root / "extracted"
            runner.safe_extract(archive, extracted)
            cacerts = extracted / "runtime" / "lib" / "security" / "cacerts"
            self.assertTrue(cacerts.is_symlink())
            self.assertEqual(payload, cacerts.read_bytes())

    def test_safe_extract_rejects_escaping_symlink(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            archive = root / "runtime.tar.gz"
            with tarfile.open(archive, "w:gz") as target:
                link = tarfile.TarInfo("runtime/lib/security/cacerts")
                link.type = tarfile.SYMTYPE
                link.linkname = "../../../../outside"
                target.addfile(link)
            with self.assertRaisesRegex(runner.CompatError, "archive link escapes destination"):
                runner.safe_extract(archive, root / "extracted")

    def test_output_pump_redacts_log_and_console(self):
        with tempfile.TemporaryDirectory() as directory:
            log = Path(directory) / "child.log"
            command = [sys.executable, "-c", "print('visible SECRET value')"]
            process = runner.subprocess.Popen(command, stdout=runner.subprocess.PIPE,
                                               stderr=runner.subprocess.STDOUT)
            redact = lambda text: text.replace("SECRET", "<redacted>")
            pump = runner.OutputPump(process, log, redact, True, "unit")
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                pump.start()
                self.assertEqual(0, process.wait(timeout=10))
                pump.join(timeout=10)
            self.assertNotIn("SECRET", log.read_text(encoding="utf-8"))
            self.assertNotIn("SECRET", output.getvalue())
            self.assertIn("<redacted>", output.getvalue())

    def test_atomic_report_replaces_stale_report(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            report_dir = root / "reports"
            logs = report_dir / "logs"
            logs.mkdir(parents=True)
            (report_dir / "compat-report.json").write_text("stale", encoding="utf-8")
            fake = object.__new__(runner.Runner)
            fake.logs = logs
            fake.report_dir = report_dir
            fake._secrets = ["summary-secret"]
            fake.result = {"status": "passed", "error": "contains summary-secret"}
            fake._write_report()
            report_text = (report_dir / "compat-report.json").read_text()
            self.assertEqual("passed", json.loads(report_text)["status"])
            self.assertNotIn("summary-secret", report_text)
            self.assertIn("<redacted>", report_text)
            self.assertFalse((report_dir / "compat-report.json.tmp").exists())

    def test_report_redacts_json_escaped_windows_path(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            report_dir = root / "reports"
            logs = report_dir / "logs"
            logs.mkdir(parents=True)
            secret = r"C:\private\keys\id_ed25519"
            fake = object.__new__(runner.Runner)
            fake.logs = logs
            fake.report_dir = report_dir
            fake._secrets = [secret]
            fake.result = {"status": "failed", "error": secret}
            fake._write_report()
            report_text = (report_dir / "compat-report.json").read_text()
            self.assertNotIn(secret, report_text)
            self.assertNotIn(secret.replace("\\", "\\\\"), report_text)
            self.assertIn("<redacted>", report_text)

    def test_request_http_sends_launch_token_header(self):
        seen = {}

        class Handler(http.server.BaseHTTPRequestHandler):
            def do_GET(self):
                seen["token"] = self.headers.get(runner.LAUNCH_TOKEN_HEADER)
                body = b'{"ok":true}'
                self.send_response(200)
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, _format, *_args):
                pass

        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            response = runner.request_http("127.0.0.1", server.server_port, "/compat/check",
                                           {runner.LAUNCH_TOKEN_HEADER: "unit-secret"})
            self.assertEqual(200, response["status"])
            self.assertEqual("unit-secret", seen["token"])
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)

    def test_wait_http_returns_fake_server_response_with_headers(self):
        class Handler(http.server.BaseHTTPRequestHandler):
            def do_GET(self):
                token = self.headers.get(runner.LAUNCH_TOKEN_HEADER)
                status = 200 if token == "expected" else 403
                body = b'{"ok":true}' if status == 200 else b'{"ok":false}'
                self.send_response(status)
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, _format, *_args):
                pass

        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        process = subprocess.Popen([sys.executable, "-c", "import time; time.sleep(30)"])
        thread.start()
        try:
            response = runner.wait_http("127.0.0.1", server.server_port, "/compat/check", 3,
                                        200, '"ok":true', process,
                                        {runner.LAUNCH_TOKEN_HEADER: "expected"})
            self.assertEqual(200, response["status"])
            self.assertTrue(response["body_matched"])
        finally:
            process.terminate()
            process.wait(timeout=5)
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)

    def test_wait_http_fails_immediately_when_process_exits(self):
        process = subprocess.Popen([sys.executable, "-c", "raise SystemExit(7)"])
        process.wait(timeout=5)
        started = time.monotonic()
        with self.assertRaisesRegex(runner.CompatError, "exited before HTTP readiness with code 7"):
            runner.wait_http("127.0.0.1", runner.reserve_port("127.0.0.1"), "/", 10,
                             200, "ok", process, {})
        self.assertLess(time.monotonic() - started, 1)

    def valid_response_fixture(self, root, pid=1234):
        fake = object.__new__(runner.Runner)
        fake.expected_class_major = 65
        fake._packaged_root = root
        process = type("Process", (), {"pid": pid})()
        checks = {name: True for name in runner.EXPECTED_CHECKS}
        payload = {"ok": True, "fixture": "jdk21", "bootVersion": "3.4.13",
                   "javaHome": str(root), "javaVersion": "21.0.7", "javaFeature": 21,
                   "pid": pid, "expectedClassMajor": 65, "classMajor": 65,
                   "launchTokenSha256": hashlib.sha256(b"token").hexdigest(), "checks": checks}
        return fake, process, payload

    def test_validate_response_requires_exact_schema(self):
        with tempfile.TemporaryDirectory() as directory:
            fake, process, payload = self.valid_response_fixture(Path(directory))
            response = {"body": json.dumps(payload, separators=(",", ":"))}
            fake._validate_check_response(response, process, "token")
            self.assertNotIn("body", response)
            payload["unexpected"] = True
            with self.assertRaisesRegex(runner.CompatError, "schema mismatch"):
                fake._validate_check_response({"body": json.dumps(payload)}, process, "token")
            payload.pop("unexpected")
            payload["ok"] = False
            payload["error"] = "requested failure"
            fake._validate_check_response({"body": json.dumps(payload)}, process, "token", expected_ok=False)

    def test_validate_response_rejects_pid_runtime_and_boot_mismatches(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            fake, process, payload = self.valid_response_fixture(root)
            for key, value in (("pid", 9999), ("bootVersion", "3.4.12"),
                               ("javaFeature", 17), ("javaVersion", "17.0.12"),
                               ("javaHome", str(root.parent / "other"))):
                changed = dict(payload)
                changed[key] = value
                with self.subTest(key=key), self.assertRaisesRegex(runner.CompatError, "mismatch|outside"):
                    fake._validate_check_response({"body": json.dumps(changed)}, process, "token")

    def test_java_home_allows_jdk8_nested_jre_only(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / "jdk"
            nested = root / "jre"
            nested.mkdir(parents=True)
            self.assertTrue(runner.java_home_belongs_to_runtime(str(root), root))
            self.assertTrue(runner.java_home_belongs_to_runtime(str(nested), root))
            self.assertFalse(runner.java_home_belongs_to_runtime(str(root / "other"), root))

    def test_nonzero_exit_validator_rejects_zero(self):
        runner.validate_nonzero_exit(7, "failed")
        with self.assertRaisesRegex(runner.CompatError, "failed"):
            runner.validate_nonzero_exit(0, "failed")

    def test_stopped_process_port_validator_requires_dead_pid_and_closed_listener(self):
        port = runner.reserve_port("127.0.0.1")
        process = type("Process", (), {"pid": 1234, "poll": lambda self: 0})()
        runner.validate_stopped_process_and_port(process, "127.0.0.1", port)
        occupied = runner.reserve_listening_socket("127.0.0.1")
        try:
            with self.assertRaisesRegex(runner.CompatError, "still accepts connections"):
                runner.validate_stopped_process_and_port(
                    process, "127.0.0.1", occupied.getsockname()[1])
        finally:
            occupied.close()
        alive = type("Process", (), {"pid": 1234, "poll": lambda self: None})()
        with self.assertRaisesRegex(runner.CompatError, "remained alive"):
            runner.validate_stopped_process_and_port(alive, "127.0.0.1", port)

    def test_validate_protected_entries_checks_every_resource_and_nested_jar(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            original = root / "original.jar"
            protected = root / "protected.jar"
            self._write_outer_jar(original, protected=False)
            self._write_outer_jar(protected, protected=True)
            runner.validate_protected_entries(original, protected)

            self._write_outer_jar(protected, protected=True,
                                  plain_entry="BOOT-INF/classes/unlisted-resource.bin")
            with self.assertRaisesRegex(runner.CompatError, "unlisted-resource.bin"):
                runner.validate_protected_entries(original, protected)

            self._write_outer_jar(protected, protected=True,
                                  plain_entry="BOOT-INF/lib/example.jar!/sample/data.txt")
            with self.assertRaisesRegex(runner.CompatError, "example.jar!/sample/data.txt"):
                runner.validate_protected_entries(original, protected)

            self._write_outer_jar(protected, protected=True,
                                  plain_entry="BOOT-INF/lib/example.jar!/sample/Example.class")
            with self.assertRaisesRegex(runner.CompatError, "example.jar!/sample/Example.class"):
                runner.validate_protected_entries(original, protected)

    def test_validate_protected_entries_rejects_nested_jar_wrapped_as_resource(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            original = root / "original.jar"
            protected = root / "protected.jar"
            self._write_outer_jar(original, protected=False)
            self._write_outer_jar(protected, protected=True, wrap_nested=True)
            with self.assertRaisesRegex(runner.CompatError, "opaque JGR resource"):
                runner.validate_protected_entries(original, protected)

    def _write_outer_jar(self, path, protected, plain_entry=None, wrap_nested=False):
        nested = io.BytesIO()
        with zipfile.ZipFile(nested, "w") as archive:
            archive.writestr("META-INF/MANIFEST.MF", b"Manifest-Version: 1.0\n")
            nested_entries = {
                "sample/Example.class": b"class-bytes",
                "sample/data.txt": b"nested-resource",
            }
            for name, data in nested_entries.items():
                transformed = data
                if protected:
                    transformed = (data + runner.ENCRYPT_CLASS_SUFFIX if name.endswith(".class")
                                   else runner.ENCRYPT_RESOURCE_HEADER + b"encrypted")
                if plain_entry == "BOOT-INF/lib/example.jar!/" + name:
                    transformed = data
                archive.writestr(name, transformed)
        nested_data = nested.getvalue()
        if protected and wrap_nested:
            nested_data = runner.ENCRYPT_RESOURCE_HEADER + nested_data

        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("META-INF/MANIFEST.MF", b"Manifest-Version: 1.0\n")
            entries = {
                "BOOT-INF/classes/example/App.class": b"app-class",
                "BOOT-INF/classes/application.properties": b"property=value",
                "BOOT-INF/classes/unlisted-resource.bin": b"other-resource",
            }
            for name, data in entries.items():
                transformed = data
                if protected:
                    transformed = (data + runner.ENCRYPT_CLASS_SUFFIX if name.endswith(".class")
                                   else runner.ENCRYPT_RESOURCE_HEADER + b"encrypted")
                if plain_entry == name:
                    transformed = data
                archive.writestr(name, transformed)
            archive.writestr("BOOT-INF/lib/example.jar", nested_data)


if __name__ == "__main__":
    unittest.main()
