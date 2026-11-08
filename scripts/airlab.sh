#!/usr/bin/env bash
#
# Crivo (AIR Lab) — CLI de verificacao, Linux e macOS.
#
# Roda o mesmo artefato da aplicacao, sem servidor web, para conferir:
#   - a cadeia de hashes da trilha de auditoria (P4);
#   - o hash de cada versao do pre-registro (P2);
#   - a reproducao do sorteio a partir da semente revelada (P5).
#
# Uso:
#   ./scripts/airlab.sh verify
#   ./scripts/airlab.sh verify --study=STD-7Q4K2M9B
#   AIRLAB_PROFILE=prod ./scripts/airlab.sh verify
#
# Sai com codigo 0 se tudo confere e 1 caso contrario.
#
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

PROFILE="${AIRLAB_PROFILE:-local}"
JAR="target/airlab.jar"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

if [[ ! -f "$JAR" ]]; then
  echo "==> Construindo o JAR" >&2
  ./mvnw -B -q package -DskipTests
fi

JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

exec "$JAVA" -jar "$JAR" \
  "${@:-verify}" \
  --spring.profiles.active="$PROFILE" \
  --spring.main.web-application-type=none \
  --spring.main.banner-mode=off \
  --logging.level.root=WARN \
  --logging.level.io.github.rogeriocisi.airlab=WARN
