#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ROOT}/../local.env"
TAG="${1:?Usage: $0 <minecraft_version-mod_version>}"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

: "${MODRINTH_AGENT_TOKEN:=${MODRINTH_TOKEN:-}}"
: "${MODRINTH_AGENT_TOKEN:?Set MODRINTH_AGENT_TOKEN in minecraft-mods/local.env}"

MOD_ID="day_night_bed"
PROJECT_SLUG="${MODRINTH_ID:-day_night_bed}"
PROJECT_ID=$(curl -sf "https://api.modrinth.com/v2/project/${PROJECT_SLUG}" \
  -H "Authorization: Bearer $MODRINTH_AGENT_TOKEN" | jq -r .id)
[[ -n "$PROJECT_ID" && "$PROJECT_ID" != "null" ]] || { echo "Could not resolve Modrinth project: $PROJECT_SLUG"; exit 1; }
CHANGELOG_FILE="${ROOT}/releases/${TAG}.md"
MAIN_JAR="${ROOT}/build/libs/${MOD_ID}-$(grep '^mod_version=' "${ROOT}/gradle.properties" | cut -d= -f2).jar"
SOURCES_JAR="${ROOT}/build/libs/${MOD_ID}-$(grep '^mod_version=' "${ROOT}/gradle.properties" | cut -d= -f2)-sources.jar"
MC_VERSION=$(grep '^minecraft_version=' "${ROOT}/gradle.properties" | cut -d= -f2)

[[ -f "$CHANGELOG_FILE" ]] || { echo "Missing changelog: $CHANGELOG_FILE"; exit 1; }
[[ -f "$MAIN_JAR" ]] || { echo "Missing JAR: $MAIN_JAR (run ./gradlew build first)"; exit 1; }

CHANGELOG=$(cat "$CHANGELOG_FILE")

DATA=$(jq -n \
  --arg project_id "$PROJECT_ID" \
  --arg name "$TAG" \
  --arg version_number "$TAG" \
  --arg changelog "$CHANGELOG" \
  --argjson game_versions "[\"${MC_VERSION}\"]" \
  '{
    project_id: $project_id,
    name: $name,
    version_number: $version_number,
    changelog: $changelog,
    dependencies: [],
    game_versions: $game_versions,
    version_type: "release",
    loaders: ["fabric"],
    featured: false,
    file_parts: ["mod_jar", "sources_jar"],
    primary_file: "mod_jar",
    file_types: {
      sources_jar: "sources-jar"
    }
  }')

DATA_FILE="/tmp/modrinth-${MOD_ID}-version.json"
OUT="/tmp/modrinth-${MOD_ID}-version-response.json"
echo "$DATA" > "$DATA_FILE"

CURL_ARGS=(
  -sS --max-time 120 -X POST "https://api.modrinth.com/v2/version"
  -H "Authorization: Bearer $MODRINTH_AGENT_TOKEN"
  -F "data=@${DATA_FILE};type=application/json"
  -F "mod_jar=@${MAIN_JAR}"
)

if [[ -f "$SOURCES_JAR" ]]; then
  CURL_ARGS+=(-F "sources_jar=@${SOURCES_JAR}")
else
  DATA=$(echo "$DATA" | jq 'del(.file_types) | .file_parts = ["mod_jar"]')
  echo "$DATA" > "$DATA_FILE"
fi

HTTP=$(curl "${CURL_ARGS[@]}" -o "$OUT" -w "%{http_code}")
echo "HTTP: $HTTP"
jq '{id, project_id, version_number, game_versions, loaders, date_published}' "$OUT"

if [[ "$HTTP" != "200" ]]; then
  jq '{error, description}' "$OUT" 2>/dev/null || cat "$OUT"
  exit 1
fi

echo "https://modrinth.com/mod/${MOD_ID}/version/$(jq -r .id "$OUT")"
