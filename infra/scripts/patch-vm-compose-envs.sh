#!/usr/bin/env bash
# infra/scripts/patch-vm-compose-envs.sh
#
# Patcht die /opt/sponsorplatz/docker-compose.yml einer laufenden Sponsorplatz-VM
# idempotent um die CD-managed ENV-Blöcke (Sentry + Google-OIDC), die in der
# cloud-init.yaml.tftpl nachträglich hinzugekommen sind. Bestehende ENVs werden
# nicht angerührt; mehrfache Ausführung ist no-op.
#
# Notwendig wenn die VM vor Phase 14.2 (Sentry-Release-Tagging) oder vor
# Google-OIDC-Wiring (2026-05-25) provisioniert wurde und nicht neu via
# Terraform/cloud-init bereitgestellt werden soll.
#
# Indent wird zur Laufzeit aus der existierenden 'JAVA_OPTS:'-Zeile abgelesen —
# funktioniert sowohl mit 6-Space- als auch 12-Space-Einrückung. Standardmäßig
# wird der App-Container nach dem Patch via 'docker compose up -d
# --force-recreate' neu gestartet und ein Container-ENV-Check verifiziert dass
# Spring die neuen Variablen tatsächlich sieht.
#
# Usage:
#   ./patch-vm-compose-envs.sh opc@<OCI-VM-IP>
#   ./patch-vm-compose-envs.sh sponsoradmin@<AZURE-VM-IP>
#   ./patch-vm-compose-envs.sh sponsoradmin@<ip> --key ~/.ssh/other-key
#   ./patch-vm-compose-envs.sh opc@<ip> --no-restart    # nur patchen

set -euo pipefail

if [ $# -lt 1 ] || [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  cat <<'USAGE' >&2
Usage: patch-vm-compose-envs.sh <user@host> [--no-restart] [--key <path>]

Patcht /opt/sponsorplatz/docker-compose.yml idempotent um:
  - Sentry-Block (SENTRY_DSN, SENTRY_RELEASE, SENTRY_ENVIRONMENT)
  - Google-OIDC-Block (SPRING_SECURITY_OAUTH2_CLIENT_*_GOOGLE_*)

Default: Container wird nach erfolgreichem Patch neu gestartet und die
Container-ENVs werden via 'docker exec env' verifiziert. --no-restart
deaktiviert den Restart-Schritt (z.B. wenn der nächste CD-Run das übernehmen
soll). Bei jedem Lauf wird ein Backup mit Timestamp angelegt — Rollback bei
docker-compose-Syntaxfehler erfolgt automatisch.

Options:
  --no-restart    Container nicht neu starten (nur compose-File-Patch).
  --key <path>    SSH-Identitätsdatei. Auto-Default für 'sponsoradmin@'-User:
                  ~/.ssh/sponsorplatz-azure (falls vorhanden). 'opc@'-User nutzt
                  den Default-Key (keine -i-Option).

Beispiele:
  ./patch-vm-compose-envs.sh opc@144.24.246.244
  ./patch-vm-compose-envs.sh sponsoradmin@135.116.65.6
  ./patch-vm-compose-envs.sh sponsoradmin@<ip> --key ~/.ssh/other-key
USAGE
  exit 1
fi

SSH_TARGET="$1"
shift
RESTART=true
SSH_KEY=""

while [ $# -gt 0 ]; do
  case "$1" in
    --no-restart)
      RESTART=false
      shift
      ;;
    --restart)
      # Backward-Compat: war früher der Default-Flag, jetzt ein no-op
      RESTART=true
      shift
      ;;
    --key)
      SSH_KEY="$2"
      shift 2
      ;;
    *)
      echo "✗ Unbekanntes Argument: $1" >&2
      exit 1
      ;;
  esac
done

# Auto-Default für Azure-User: sponsoradmin@ → ~/.ssh/sponsorplatz-azure
if [ -z "$SSH_KEY" ] && [[ "$SSH_TARGET" == sponsoradmin@* ]] && [ -f "$HOME/.ssh/sponsorplatz-azure" ]; then
  SSH_KEY="$HOME/.ssh/sponsorplatz-azure"
  echo "ℹ Auto-Default Azure-Key: $SSH_KEY"
fi

SSH_OPTS=()
if [ -n "$SSH_KEY" ]; then
  if [ ! -f "$SSH_KEY" ]; then
    echo "✗ SSH-Key nicht gefunden: $SSH_KEY" >&2
    exit 1
  fi
  SSH_OPTS=(-i "$SSH_KEY")
fi

echo "→ SSH zu $SSH_TARGET${SSH_KEY:+ (key: $SSH_KEY)}, patche /opt/sponsorplatz/docker-compose.yml …"
echo

# Übergibt RESTART als Umgebungsvariable an die remote-Seite.
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "RESTART=$RESTART bash -s" <<'REMOTE'
set -euo pipefail
COMPOSE=/opt/sponsorplatz/docker-compose.yml

if [ ! -f "$COMPOSE" ]; then
  echo "✗ $COMPOSE nicht gefunden — ist das eine Sponsorplatz-VM?" >&2
  exit 1
fi

# ── 0. Vorab-Analyse: Indent + Umgebung aus dem laufenden compose ablesen ──
# Indent ist nicht universal: cloud-init's 'content: |'-Heredoc kann je nach
# Template-Einrückung 6 oder 12 Spaces im finalen File liefern. Wir lesen den
# tatsächlichen Indent der 'JAVA_OPTS:'-Zeile und benutzen ihn für die Inserts —
# damit ist YAML-Konsistenz garantiert.
JAVA_LINE=$(sudo grep -E '^[[:space:]]+JAVA_OPTS:' "$COMPOSE" | head -1)
if [ -z "$JAVA_LINE" ]; then
  echo "✗ 'JAVA_OPTS:'-Anker nicht in $COMPOSE gefunden — Abbruch" >&2
  exit 1
fi
INDENT=$(printf '%s\n' "$JAVA_LINE" | sed -E 's/^([[:space:]]*).*/\1/')
INDENT_LEN=${#INDENT}
echo "✓ Detected indent: $INDENT_LEN spaces"

# SENTRY_ENVIRONMENT pro Cloud — aus dem bestehenden SPONSORPLATZ_UMGEBUNG-Wert.
UMGEBUNG=$(sudo grep -oE 'SPONSORPLATZ_UMGEBUNG: [a-z-]+' "$COMPOSE" | head -1 | awk '{print $2}')
if [ -z "$UMGEBUNG" ]; then
  echo "✗ SPONSORPLATZ_UMGEBUNG nicht in $COMPOSE gefunden — Abbruch" >&2
  exit 1
fi
echo "✓ Cloud-Zone: $UMGEBUNG"

# ── 1. Backup ─────────────────────────────────────────────────────────────
BACKUP="$COMPOSE.bak.$(date +%Y%m%d-%H%M%S)"
sudo cp -a "$COMPOSE" "$BACKUP"
echo "✓ Backup: $BACKUP"
echo

# ── 2. Sentry-Block einfügen (idempotent) ─────────────────────────────────
if sudo grep -q '^[[:space:]]*SENTRY_DSN:' "$COMPOSE"; then
  echo "✓ Sentry-ENVs bereits vorhanden — skip"
else
  echo "→ Sentry-ENVs einfügen ($INDENT_LEN-Space-Indent)"
  sudo awk -v indent="$INDENT" -v env="$UMGEBUNG" '
    /^[[:space:]]+JAVA_OPTS:/ && !inserted {
      print indent "SENTRY_DSN: \"${SENTRY_DSN:-}\""
      print indent "SENTRY_RELEASE: \"${SENTRY_RELEASE:-}\""
      print indent "SENTRY_ENVIRONMENT: " env
      inserted = 1
    }
    { print }
  ' "$COMPOSE" | sudo tee "$COMPOSE.new" > /dev/null
  sudo mv "$COMPOSE.new" "$COMPOSE"
fi

# ── 3. Google-OIDC-Block einfügen (idempotent) ────────────────────────────
if sudo grep -q 'GOOGLE_CLIENT_ID' "$COMPOSE"; then
  echo "✓ Google-OIDC-ENVs bereits vorhanden — skip"
else
  echo "→ Google-OIDC-ENVs einfügen ($INDENT_LEN-Space-Indent)"
  sudo awk -v indent="$INDENT" '
    /^[[:space:]]+JAVA_OPTS:/ && !inserted {
      print indent "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID: \"${GOOGLE_CLIENT_ID:-}\""
      print indent "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET: \"${GOOGLE_CLIENT_SECRET:-}\""
      print indent "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_NAME: Google"
      print indent "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE: openid,profile,email"
      print indent "SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_GOOGLE_ISSUER_URI: https://accounts.google.com"
      inserted = 1
    }
    { print }
  ' "$COMPOSE" | sudo tee "$COMPOSE.new" > /dev/null
  sudo mv "$COMPOSE.new" "$COMPOSE"
fi

# ── 4. Validate compose ──────────────────────────────────────────────────
cd /opt/sponsorplatz
if ! sudo docker compose config -q 2>/tmp/compose-err; then
  echo
  echo "✗ compose-File invalid nach Patch:" >&2
  sed 's/^/    /' /tmp/compose-err >&2
  echo "→ Rollback aus $BACKUP" >&2
  sudo cp -a "$BACKUP" "$COMPOSE"
  exit 1
fi
echo "✓ compose-File ist valid"

# ── 5. Post-Patch Verifikation der Refs ───────────────────────────────────
# Wenn aus irgendeinem Grund das awk-Insert silent fehlgeschlagen ist (z.B.
# Anchor nicht gematcht), schlagen wir hier hart fehl statt false success.
FAILED=0
for KEY in SENTRY_DSN SENTRY_ENVIRONMENT GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET; do
  if ! sudo grep -q "$KEY" "$COMPOSE"; then
    echo "✗ Ref '$KEY' NICHT im compose-File nach Patch — etwas ist schiefgegangen" >&2
    FAILED=1
  fi
done
if [ "$FAILED" = "1" ]; then
  echo "→ Rollback aus $BACKUP" >&2
  sudo cp -a "$BACKUP" "$COMPOSE"
  exit 1
fi
echo "✓ Alle 4 erwarteten ENV-Refs im compose-File"
echo

# ── 6. Optional Restart + Container-ENV-Sanity ────────────────────────────
if [ "$RESTART" != "true" ]; then
  echo "ℹ --no-restart gesetzt — Container nicht neu gestartet"
  echo "  Manuell: cd /opt/sponsorplatz && sudo docker compose up -d --force-recreate app"
  exit 0
fi

echo "→ App-Container neu starten (--force-recreate)"
sudo docker compose up -d --force-recreate app
echo "✓ Container neu gestartet"
echo

# Kurz warten bis der Container hochkommt (5–15s je nach Cloud)
echo "→ Warte bis Container hochgekommen ist …"
for i in $(seq 1 30); do
  if sudo docker exec sponsorplatz-app env 2>/dev/null | grep -q '^SPRING_PROFILES_ACTIVE='; then
    break
  fi
  sleep 1
done

# Sanity: Spring-ENVs für Sentry + Google im laufenden Container
echo "→ Verifiziere Container-ENVs:"
if sudo docker exec sponsorplatz-app env 2>/dev/null | grep -qE '^SENTRY_'; then
  echo "  ✓ Sentry-ENVs sichtbar"
else
  echo "  ⚠ Sentry-ENVs NICHT sichtbar im Container — docker-compose-Substitution prüfen"
fi
GOOGLE_ID=$(sudo docker exec sponsorplatz-app env 2>/dev/null | grep '^SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=' || true)
if [ -n "$GOOGLE_ID" ]; then
  # Print nur die ersten 20 Zeichen der ID — Logging-safe
  echo "  ✓ Google CLIENT_ID-ENV sichtbar: ${GOOGLE_ID:0:60}…"
else
  echo "  ⚠ Google CLIENT_ID-ENV NICHT sichtbar — entweder Secret nicht in .env oder Substitution greift nicht"
  echo "    Check: sudo grep GOOGLE /opt/sponsorplatz/.env"
fi
echo
echo "✓ Done — teste /login (Google-Button sollte erscheinen)"
REMOTE
