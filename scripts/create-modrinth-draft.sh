#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ROOT}/../local.env"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

: "${MODRINTH_AGENT_TOKEN:=${MODRINTH_TOKEN:-}}"
: "${MODRINTH_AGENT_TOKEN:?Set MODRINTH_AGENT_TOKEN in minecraft-mods/local.env (see local.env.example)}"

MOD_ID="day_night_bed"
BODY='## Day Bed - master of the Night Bed

Fabric mod for Minecraft **1.21.11** that lets players use a vanilla bed at any time to toggle world time between day and night.

- Daytime rest advances to night; nighttime sleep advances to day
- Storms skip to morning (vanilla behavior)
- Configurable targets at `config/day_night_bed.json`
- Rest vs Sleep overlay copy for day/night transitions

Source: https://github.com/spicato-minecraft/day_night_bed'

DATA=$(jq -n \
  --arg slug "$MOD_ID" \
  --arg title "Day Bed - master of the Night Bed" \
  --arg description "Use a vanilla bed at any time to toggle world time between day and night." \
  --arg body "$BODY" \
  --arg license_id "CC0-1.0" \
  --arg source_url "https://github.com/spicato-minecraft/${MOD_ID}" \
  '{
    slug: $slug,
    title: $title,
    description: $description,
    body: $body,
    project_type: "mod",
    categories: ["utility", "game-mechanics"],
    license_id: $license_id,
    client_side: "required",
    server_side: "required",
    source_url: $source_url,
    is_draft: true,
    initial_versions: []
  }')

OUT="/tmp/modrinth-${MOD_ID}-create.json"
DATA_FILE="/tmp/modrinth-${MOD_ID}-payload.json"
echo "$DATA" > "$DATA_FILE"

HTTP=$(curl -sS --max-time 60 -X POST "https://api.modrinth.com/v2/project" \
  -H "Authorization: Bearer $MODRINTH_AGENT_TOKEN" \
  -F "data=@${DATA_FILE};type=application/json" \
  -o "$OUT" \
  -w "%{http_code}")

echo "HTTP: $HTTP"
jq '{id, slug, title, status, error, description}' "$OUT"

if [[ "$HTTP" != "200" ]]; then
  exit 1
fi

SLUG=$(jq -r .slug "$OUT")
gh variable set MODRINTH_ID --body "$SLUG" --repo "spicato-minecraft/${MOD_ID}"
echo "MODRINTH_ID set to $SLUG"
echo "https://modrinth.com/mod/${SLUG}"
