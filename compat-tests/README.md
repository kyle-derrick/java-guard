# Spring Boot compatibility fixtures

`apps` contains five independent Maven projects used to exercise Java Guard against a deliberately broad but compact set of ordinary framework and JDK behaviors. They are not a Maven reactor; build each directory with its matching JDK.

| Project | Spring Boot | Compiler setting | Expected class major | Artifact |
| --- | --- | --- | ---: | --- |
| `apps/jdk8` | `2.1.9.RELEASE` | source/target 1.8 | 52 | `target/compat-jdk8.jar` |
| `apps/jdk11` | `2.7.18` | release 11 | 55 | `target/compat-jdk11.jar` |
| `apps/jdk17` | `3.3.13` | release 17 | 61 | `target/compat-jdk17.jar` |
| `apps/jdk21` | `3.4.13` | release 21 | 65 | `target/compat-jdk21.jar` |
| `apps/jdk25` | `4.1.0` | release 25 | 69 | `target/compat-jdk25.jar` |

Example, using the locally validated Maven 3.9.16 installation:

```shell
export MAVEN_HOME=/d/software/dev/apache-maven-3.9.16
cd compat-tests/apps/jdk21
"$MAVEN_HOME/bin/mvn" clean package
java -jar target/compat-jdk21.jar
```

Each application covers multiple application classes across Spring MVC, dependency injection and configuration properties, Jackson JSON binding, Spring AOP, application events, caching, async execution, `ServiceLoader`, reflection, a JDK dynamic proxy, Java serialization, class lookup, and classpath resource access through `URL.openConnection()`. It also includes stable APIs or language features appropriate to its target (Java 8 Base64, Java 11 string APIs, Java 17 records/sealed types, Java 21 virtual threads, and Java 25 stream gatherers). No fixture requires preview features.

The generated compatibility configuration deliberately uses only `'*'`. Java Guard wildcard `*` maps to `.*` and crosses `/`, so this is a full-archive recursive scope: all fixture application classes and selected resources are checked for encryption, and matched nested JAR entries are recursively processed. This is not a single-class smoke test.

The class-stub path preserves legal unnamed `MethodParameters` entries as unnamed. Because a LocalVariableTable entry cannot have a null name, stub generation omits the corresponding synthesized local-variable entry instead of inventing a parameter name; the Maven unit suite covers this regression.

## Run locally

After building the distribution with Maven 3.9.16, pass that same installation explicitly; `run.py` infers the newest Java Guard JAR, defaults to `jdk21`, infers class major 65, and uses script-relative work/report paths:

```shell
python compat-tests/run.py \
  --jdk-home "$JAVA_HOME" \
  --maven-home /d/software/dev/apache-maven-3.9.16
```

Select another fixture when needed; its class major is inferred from `jdk8`, `jdk11`, `jdk17`, `jdk21`, or `jdk25`:

```shell
python compat-tests/run.py \
  --jdk-home "$JAVA_HOME" \
  --maven-home /d/software/dev/apache-maven-3.9.16 \
  --fixture jdk17
```

Pass `--java-guard-jar` only when automatic discovery is unsuitable. The commands above pin the locally validated Maven 3.9.16 through `--maven-home`; otherwise the runner can use a Maven wrapper or `mvn`/`mvn.cmd` from `PATH`. `--console-output full` is the default and streams redacted child output while retaining logs. `--console-output summary` keeps the full redacted files but limits console output to progress and failure summaries.

The defaults are `compat-tests/work/` and `compat-tests/reports/`. A new run with those same paths cleans work and replaces the report directory's `logs/` and `compat-report.json`; use optional isolated paths only for concurrent lanes or retained history:

```shell
python compat-tests/run.py \
  --jdk-home "$JAVA_HOME" \
  --maven-home /d/software/dev/apache-maven-3.9.16 \
  --fixture jdk17 \
  --work-dir compat-tests/work/local-jdk17 \
  --report-dir compat-tests/reports/local-jdk17
```

Work data contains ephemeral private keys, AES material, rendered configuration, the key-bound launcher, and packaged runtimes. It must remain local to the lane and must never be uploaded or cached. Reports contain redacted logs and JSON, but should still be inspected before sharing.

## Strict token-authenticated E2E contract

Each process launch receives a new random secret through `-Dcompat.launch-token=...`. Every request sends the same value in `X-Compat-Launch-Token`; the endpoint rejects a missing/mismatched token with HTTP 403 and returns `launchTokenSha256` for identity validation. The runner requires that digest plus the exact fixture, Java feature, expected/actual class major, and complete named check set. This binds each response to the intended launch and guards against an unrelated process answering on the test port.

An authenticated `GET /compat/check` returns HTTP 200 and JSON with:

- `ok: true`
- `fixture`, `bootVersion`, `javaVersion`, runtime `javaHome`, and `javaFeature`
- `classMajor` read directly from the running application class and `expectedClassMajor`
- `launchTokenSha256` for the current launch token
- `checks`, whose exact expected keys must all be `true`

Authenticated `GET /compat/check?fail=true` runs the same checks and deterministically returns HTTP 500 with `ok: false` and `error: "requested failure"`. The E2E flow additionally validates signature rejection for a structurally valid tampered JAR and requires a nonzero launcher exit when Java startup fails because the port is occupied.

## Current evidence and validation status

- Maven 3.9.16: the full Java test suite passed in local Windows validation.
- Python runner unit tests cover argument validation, redaction, HTTP identity, process cleanup, and protected-entry validation.
- Rust default features: **13 tests passed**; Rust all-features: **2 tests passed**.
- Maven 3.9.16 on JDK 25: the E2E run **passed** and no longer emitted the prior Maven 3.9.4 Jansi/Guava warnings.
- Complete Windows native E2E: **passed** on JDK 8, 11, 17, 21, and 25. This covers Spring Boot 2.1.9.RELEASE, 2.7.18, 3.3.13, 3.4.13, and 4.1.0 respectively, including strict token-authenticated HTTP and occupied-port startup failure with a required nonzero launcher exit after the propagation fix.
- Strict `cargo clippy` still exposes many broader legacy and generated-code lints after the initial build-script fixes; passing tests are not a clean full-repository lint result. Whole-repository `cargo fmt` still has a legacy formatting diff and is deferred.
- No remote GitHub CI workflow was run. In particular, do not claim warning-free GitHub-hosted Linux output until that lane runs. Docker was unavailable locally, so no local Docker lane was run; macOS was not tested.
