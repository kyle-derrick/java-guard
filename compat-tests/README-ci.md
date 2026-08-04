# Compatibility CI

`.github/workflows/compat.yml` first performs one shared Java Guard distribution build on JDK 8 and reuses that JAR artifact, then runs independent native JDK lanes in parallel. Checkouts include recursive submodules. Each parallel lane installs Rust, generates a fresh AES key and ED25519 key pair locally, compiles its own launcher bound to those keys, and packages its selected JDK.

Launcher binaries are key-bound, so neither launchers nor generated AES/private/public keys may be shared through CI artifacts or caches. The workflow has no launcher seed jobs or launcher/key bundle artifacts. It uploads only the common Java Guard JAR and redacted compatibility reports. Cargo caching is intentionally limited to downloaded registry indexes, registry archives, and Git database objects; it does not cache build targets, generated work directories, rendered configuration, keys, packaged runtimes, or final launchers.

Pull requests use a smaller representative matrix: Ubuntu on JDK 8 and 21, plus Windows on JDK 17. Those three jobs compile independently and in parallel. The weekly schedule runs JDK 8, 11, 17, 21, and 25 on both Ubuntu and Windows; those ten jobs likewise compile independently and in parallel. A manual run can select `full_matrix` to run that same matrix. The optional Docker lane runs on the weekly schedule or when `docker` is selected manually. This describes configuration only: no remote GitHub workflow was run for the current evidence, and configured lanes must not be reported as remotely passed.

## Native runner invocation

`actions/setup-java` exports the selected installation as `JAVA_HOME`. Each native lane verifies that Maven is available with `mvn --version`; `compat-tests/run.py` then discovers `mvn`/`mvn.cmd` from `PATH`, which works on both GitHub-hosted Linux and Windows runners. If a self-hosted runner does not put Maven on `PATH`, install it and pass its installation directory with `--maven-home`.

Each native matrix lane invokes the runner directly:

```shell
python compat-tests/run.py \
  --java-guard-jar "dist/java-guard.jar" \
  --jdk-home "$JAVA_HOME" \
  --fixture "jdk${{ matrix.java }}" \
  --expected-class-major "${{ matrix.class_major }}" \
  --work-dir "compat-tests/work/${{ runner.os }}-jdk${{ matrix.java }}" \
  --report-dir "compat-tests/reports/${{ runner.os }}-jdk${{ matrix.java }}"
```

The expected class-file major mappings are JDK 8 to 52, JDK 11 to 55, JDK 17 to 61, JDK 21 to 65, and JDK 25 to 69.

CI uses explicit per-lane work/report paths so parallel jobs remain isolated. For a single local run these arguments are optional: the script-relative defaults are `compat-tests/work/` and `compat-tests/reports/`. Reusing default or explicit paths cleans the work directory and replaces the report directory's `logs/` and `compat-report.json`; use unique paths only when preserving history or running concurrently.

`--console-output full` is the runner default and streams redacted child output while writing complete logs. `--console-output summary` keeps complete redacted log files but shows only step progress and failure summaries on the console. Each CI runner keeps private material and its key-bound launcher below its work directory and uploads only the corresponding redacted report directory, even when the command fails.

Every E2E launch is strict and multi-class: the `'*'` configuration recursively encrypts the full archive, and each HTTP request must authenticate with the launch-specific token and match its returned SHA-256, fixture, Java feature, class major, and complete check set. The failure phase rejects signed-JAR tampering and requires a nonzero launcher exit for occupied-port Java startup failure.

## Current evidence and gate

Current evidence is local Windows evidence only. The full Maven Java test suite and Python runner unit suite passed with Maven 3.9.16/Python 3, while Rust default features report **13 tests passed** and Rust all-features report **2 tests passed**. The complete Windows native E2E suite passed on JDK 8, 11, 17, 21, and 25, covering Spring Boot 2.1.9.RELEASE, 2.7.18, 3.3.13, 3.4.13, and 4.1.0 respectively; the strict occupied-port check now receives the required nonzero launcher exit after the propagation fix. The Maven 3.9.16 JDK 25 E2E run passed and no longer emitted the prior Maven 3.9.4 Jansi/Guava warnings.

Strict `cargo clippy` still exposes many broader legacy and generated-code lints after the initial build-script fixes; passing tests are not a clean full-repository lint result. Whole-repository `cargo fmt` still has a legacy formatting diff and is deferred. No remote GitHub CI workflow was run, so do not claim warning-free GitHub-hosted Linux output until that lane runs. Docker was unavailable locally, so the Docker lane was not run locally; macOS was not tested.

## Docker lane

Docker is driven by the definition in `compat-tests/docker/compose.yml`, rather than by runner options that do not exist:

```shell
COMPAT_FIXTURE=jdk21 \
COMPAT_EXTRA_ARGS='--java-guard-jar /workspace/dist/java-guard.jar --expected-class-major 65 --work-dir /workspace/compat-tests/work/docker-jdk21 --report-dir /workspace/compat-tests/reports/docker-jdk21' \
docker compose -f compat-tests/docker/compose.yml run --rm compat
```

The Compose service supplies `--jdk-home /opt/java/openjdk`, uses its image's Maven installation from `PATH`, and bind-mounts the repository at `/workspace`. Keep these arguments synchronized with `compat-tests/run.py` and the Compose definition if either interface changes.
