#!/usr/bin/env bash
# =============================================================================
# install.sh — Installs JUnit Guardian into a Java/Gradle project
# =============================================================================
# Usage:
#   ./scripts/install.sh [/path/to/your-java-project]
#
# If no path is given, installs into the current directory.
# =============================================================================

set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'
info()    { echo -e "${GREEN}  ✓${NC}  $*"; }
warn()    { echo -e "${YELLOW}  ⚠${NC}  $*"; }
error()   { echo -e "${RED}  ✗${NC}  $*" >&2; }
header()  { echo -e "\n${CYAN}${BOLD}$*${NC}"; }
step()    { echo -e "\n${BOLD}$*${NC}"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GUARDIAN_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TARGET="${1:-$(pwd)}"

echo -e "${CYAN}${BOLD}"
echo "  ╔══════════════════════════════════════════╗"
echo "  ║        JUnit Guardian  Installer         ║"
echo "  ║   Gradle · Ollama · Warn-only mode       ║"
echo "  ╚══════════════════════════════════════════╝"
echo -e "${NC}"
echo "  Target project : $TARGET"
echo "  Guardian source: $GUARDIAN_ROOT"

# ---- Guard: must be a git repo -----------------------------------------------
if [ ! -d "$TARGET/.git" ]; then
    error "$TARGET is not a Git repository."
    exit 1
fi

# ---- Step 1: Build the fat JAR with Gradle -----------------------------------
step "1/4  Building junit-guardian fat JAR..."

JAR_PATH=$(find "$GUARDIAN_ROOT/build/libs" -name "junit-guardian-*.jar" \
           -not -name "*-sources.jar" 2>/dev/null | head -1 || true)

if [ -z "$JAR_PATH" ]; then
    if command -v ./gradlew &>/dev/null; then
        GRADLE_CMD="./gradlew"
    elif command -v gradle &>/dev/null; then
        GRADLE_CMD="gradle"
    else
        error "Gradle wrapper (./gradlew) not found and 'gradle' not in PATH."
        error "Run from the junit-guardian directory:  ./gradlew shadowJar"
        exit 1
    fi

    echo "     Running: $GRADLE_CMD shadowJar"
    (cd "$GUARDIAN_ROOT" && $GRADLE_CMD shadowJar -q)

    JAR_PATH=$(find "$GUARDIAN_ROOT/build/libs" -name "junit-guardian-*.jar" \
               -not -name "*-sources.jar" | head -1)
fi

info "JAR ready: $JAR_PATH"

# ---- Step 2: Install the pre-commit hook ------------------------------------
step "2/4  Installing Git pre-commit hook..."

HOOKS_DIR="$TARGET/.git/hooks"
mkdir -p "$HOOKS_DIR"

cp "$SCRIPT_DIR/pre-commit" "$HOOKS_DIR/pre-commit"
chmod +x "$HOOKS_DIR/pre-commit"

info "Hook installed: $HOOKS_DIR/pre-commit"

# ---- Step 3: Write .guardian.env config -------------------------------------
step "3/4  Writing .guardian.env..."

CONFIG="$TARGET/.guardian.env"

if [ ! -f "$CONFIG" ]; then
cat > "$CONFIG" << EOF
# ---------------------------------------------------------------
# JUnit Guardian — environment configuration
# Source this file before committing:
#   source .guardian.env
# Or add to your shell profile for permanent effect.
# ---------------------------------------------------------------

# Path to the guardian fat JAR (required)
export JUNIT_GUARDIAN_JAR="${JAR_PATH}"

# Ollama base URL (change if running on a remote host)
export JUNIT_GUARDIAN_OLLAMA="http://localhost:11434"

# Ollama model to use for test generation
# Good options: codellama, deepseek-coder, qwen2.5-coder, mistral
export JUNIT_GUARDIAN_MODEL="codellama"

# Mode: scan (report only) | generate (write test files)
export JUNIT_GUARDIAN_MODE="generate"

# Coverage % at which a warning is shown (commit still proceeds)
export JUNIT_GUARDIAN_MIN_COV="80"
EOF
    info "Config created: $CONFIG"
else
    warn "Config already exists (not overwritten): $CONFIG"
fi

# ---- Step 4: Check Ollama availability --------------------------------------
step "4/4  Checking Ollama..."

OLLAMA_URL="http://localhost:11434"

if curl -sf --max-time 3 "${OLLAMA_URL}/api/tags" > /dev/null 2>&1; then
    info "Ollama is running at $OLLAMA_URL"

    MODEL_COUNT=$(curl -sf "${OLLAMA_URL}/api/tags" \
        | grep -c '"name":"codellama' 2>/dev/null || echo "0")

    if [ "$MODEL_COUNT" -gt 0 ]; then
        info "Model 'codellama' is available."
    else
        warn "Model 'codellama' not found locally."
        warn "Pull it with:  ollama pull codellama"
        warn "(or set a different model in $CONFIG)"
    fi
else
    warn "Ollama is not running. Start it with:  ollama serve"
    warn "The hook will skip gracefully if Ollama is offline."
fi

# ---- Done -------------------------------------------------------------------
echo ""
echo -e "${GREEN}${BOLD}╔══════════════════════════════════════════════════════╗"
echo -e "║  JUnit Guardian installed successfully ✓             ║"
echo -e "╚══════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  ${BOLD}Next steps:${NC}"
echo ""
echo "  1. Source the config (or add to your shell profile):"
echo "        source $CONFIG"
echo ""
echo "  2. Make sure Ollama is running:"
echo "        ollama serve"
echo "        ollama pull codellama   # if not already pulled"
echo ""
echo "  3. Stage some Java files and commit — the hook fires automatically."
echo "     It will WARN if coverage is low but will never block your commit."
echo ""
echo -e "  ${BOLD}Manual commands:${NC}"
echo "    Scan only:       java -jar $JAR_PATH --mode scan    ."
echo "    Generate tests:  java -jar $JAR_PATH --mode generate ."
echo "    Full report:     java -jar $JAR_PATH --mode report   ."
echo ""
