FROM eclipse-temurin:21-jdk-jammy

ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       build-essential ca-certificates clang cmake curl git maven openssh-client \
       pkg-config python3 \
    && rm -rf /var/lib/apt/lists/* \
    && curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs \
       | sh -s -- -y --profile minimal --default-toolchain stable

WORKDIR /workspace
ENV PATH="/root/.cargo/bin:${PATH}" \
    CARGO_TERM_COLOR=never \
    MAVEN_OPTS="-Djava.awt.headless=true" \
    PYTHONUNBUFFERED=1

# The repository is bind-mounted by compose. This command deliberately calls
# the same cross-platform runner used directly on Windows and Linux hosts.
CMD ["python3", "/workspace/compat-tests/run.py", "--repo", "/workspace", "--jdk-home", "/opt/java/openjdk"]
