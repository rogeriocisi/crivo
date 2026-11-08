#!/usr/bin/env bash
#
# Crivo (AIR Lab) — script unico de inicializacao, Linux e macOS.
#
# Sobe todos os componentes necessarios:
#   - PostgreSQL: no perfil local, embarcado pela propria aplicacao (binarios reais do
#     PostgreSQL, sem Docker); no perfil prod, o PostgreSQL do sistema operacional;
#   - migracoes Flyway dos esquemas research, identity e audit;
#   - a aplicacao Spring Boot, com servidor web embutido.
#
# Pre-requisito unico: um JDK 21 ou mais novo. O Maven vem pelo wrapper do repositorio.
#
# Uso:
#   ./scripts/start.sh                    # perfil local (padrao), http://127.0.0.1:8080
#   ./scripts/start.sh --profile prod     # PostgreSQL do sistema, segredos do ambiente
#   ./scripts/start.sh --port 9090
#   ./scripts/start.sh --skip-build       # usa o JAR que ja existe
#   ./scripts/start.sh --rebuild          # forca novo build, com testes
#   ./scripts/start.sh -- --qualquer.prop=valor
#
# Ctrl+C encerra a aplicacao e, com ela, o PostgreSQL embarcado.
#
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

PROFILE="local"
PORT=""
SKIP_BUILD="false"
REBUILD="false"
EXTRA_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --profile)    PROFILE="${2:?--profile exige um valor}"; shift 2 ;;
    --profile=*)  PROFILE="${1#*=}"; shift ;;
    --port)       PORT="${2:?--port exige um valor}"; shift 2 ;;
    --port=*)     PORT="${1#*=}"; shift ;;
    --skip-build) SKIP_BUILD="true"; shift ;;
    --rebuild)    REBUILD="true"; shift ;;
    --help|-h)    sed -n '2,24p' "${BASH_SOURCE[0]}"; exit 0 ;;
    --)           shift; EXTRA_ARGS=("$@"); break ;;
    *)            echo "Opcao desconhecida: $1 (use --help)" >&2; exit 2 ;;
  esac
done

JAR="target/airlab.jar"

info()  { printf '\033[1;32m==>\033[0m %s\n' "$*"; }
warn()  { printf '\033[1;33m==>\033[0m %s\n' "$*" >&2; }
fail()  { printf '\033[1;31merro:\033[0m %s\n' "$*" >&2; exit 1; }

# 1. JDK -----------------------------------------------------------------------
if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
  JAVA="${JAVA_HOME}/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVA="$(command -v java)"
else
  fail "nenhum JDK encontrado. Instale um JDK 21 (ou mais novo) ou defina JAVA_HOME."
fi

JAVA_MAJOR="$("$JAVA" -version 2>&1 | head -n 1 | sed -E 's/.*version "([0-9]+).*/\1/')"
if [[ ! "$JAVA_MAJOR" =~ ^[0-9]+$ ]] || (( JAVA_MAJOR < 21 )); then
  fail "e necessario um JDK 21 ou mais novo (encontrado: ${JAVA_MAJOR:-desconhecido})."
fi
info "JDK $JAVA_MAJOR em $JAVA"

# 2. Segredos e pre-condicoes do perfil ----------------------------------------
if [[ -f .env ]]; then
  info "Lendo variaveis de .env"
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

if [[ "$PROFILE" == "prod" ]]; then
  for required in AIRLAB_DB_URL AIRLAB_DB_USER AIRLAB_DB_PASSWORD AIRLAB_IDENTITY_KEY \
                  AIRLAB_RESEARCHER_PASSWORD_HASH; do
    if [[ -z "${!required:-}" ]]; then
      fail "$required nao definida. No perfil prod os segredos vem do ambiente ou do .env; a aplicacao nao inicia sem eles."
    fi
  done
  info "Perfil prod: PostgreSQL do sistema operacional, em ${AIRLAB_DB_URL%%\?*}"
else
  mkdir -p data
  info "Perfil $PROFILE: PostgreSQL embarcado em ${AIRLAB_LOCAL_DB_DIR:-$HOME/.airlab/pgdata} (somente dados ficticios)"
fi

# 3. Build ---------------------------------------------------------------------
if [[ "$REBUILD" == "true" ]]; then
  info "Construindo o JAR com os testes"
  ./mvnw -B clean package
elif [[ "$SKIP_BUILD" == "true" ]]; then
  [[ -f "$JAR" ]] || fail "$JAR nao existe; rode sem --skip-build."
  info "Usando o JAR existente"
else
  needs_build="false"
  if [[ ! -f "$JAR" ]]; then
    needs_build="true"
  elif [[ -n "$(find src pom.xml -newer "$JAR" -print -quit 2>/dev/null)" ]]; then
    needs_build="true"
  fi
  if [[ "$needs_build" == "true" ]]; then
    info "Construindo o JAR (sem testes; use --rebuild para rodar a suite)"
    ./mvnw -B -q package -DskipTests
  else
    info "JAR atualizado"
  fi
fi

# 4. Aplicacao -----------------------------------------------------------------
APP_ARGS=("--spring.profiles.active=$PROFILE")
[[ -n "$PORT" ]] && APP_ARGS+=("--server.port=$PORT")
(( ${#EXTRA_ARGS[@]} )) && APP_ARGS+=("${EXTRA_ARGS[@]}")

info "Iniciando o Crivo (perfil $PROFILE). Ctrl+C encerra tudo."
exec "$JAVA" -jar "$JAR" "${APP_ARGS[@]}"
