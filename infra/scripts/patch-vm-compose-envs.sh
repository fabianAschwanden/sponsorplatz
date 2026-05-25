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
# Nach Terraform-Re-Apply / Re-Provisioning ist dieser Helper obsolet.
#
# Usage:
#   ./patch-vm-compose-envs.sh opc@<OCI-VM-IP>           # nur patchen
#   ./patch-vm-compose-envs.sh opc@<OCI-VM-IP> --restart # patchen + Container neu starten
#
# Beide Clouds funktionieren — SSH-User ('opc' für OCI, 'sponsoradmin' für Azure)
# wird transparent durchgereicht.

set -euo pipefail

if [ $# -lt 1 ] || [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  cat <<'USAGE' >&2
Usage: patch-vm-compose-envs.sh <user@host> [--restart] [--key <path>]

Patcht /opt/sponsorplatz/docker-compose.yml idempotent um:
  - Sentry-Block (SENTRY_DSN, SENTRY_RELEASE, SENTRY_ENVIRONMENT)
  - Google-OIDC-Block (SPRING_SECURITY_OAUTH2_CLIENT_*_GOOGLE_*)

Options:
  --restart       Container nach erfolgreichem Patch via docker compose up
                  -d --force-recreate neu starten (sonst nur compose-File-Patch).
  --key <path>    SSH-Identitätsdatei. Auto-Default für 'sponsoradmin@'-User:
                  ~/.ssh/sponsorplatz-azure (falls vorhanden). 'opc@'-User nutzt
                  den Default-Key (keine -i-Option).

Beispiele:
  ./patch-vm-compose-envs.sh opc@144.24.246.244
  ./patch-vm-compose-envs.sh sponsoradmin@135.116.65.6 --restart
  ./patch-vm-compose-envs.sh sponsoradmin@<ip> --key ~/.ssh/other-key
USAGE
  exit 1
fi

SSH_TARGET="$1"
shift
RESTART_FLAG=""
SSH_KEY=""

# Verbleibende Flags parsen — Reihenfolge egal
while [ $# -gt 0 ]; do
  case "$1" in
    --restart)
      RESTART_FLAG="--restart"
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
# (die VM ist mit einem dedizierten Key provisioniert, der nicht der Default ist).
# OCI-VMs (opc@) nutzen den Default-Key — kein Auto-Override hier.
if [ -z "$SSH_KEY" ] && [[ "$SSH_TARGET" == sponsoradmin@* ]] && [ -f "$HOME/.ssh/sponsorplatz-azure" ]; then
  SSH_KEY="$HOME/.ssh/sponsorplatz-azure"
  echo "ℹ Auto-Default Azure-Key: $SSH_KEY"
fi

# SSH_OPTS-Array — leer wenn kein -i, sonst '-i <path>'. Wichtig: Array-Expansion
# damit Pfade mit Spaces korrekt gequotet bleiben.
SSH_OPTS=()
if [ -n "$SSH_KEY" ]; then
  if [ ! -f "$SSH_KEY" ]; then
    echo "✗ SSH-Key nicht gefunden: $SSH_KEY" >&2
    exit 1
  fi
  SSH_OPTS=(-i "$SSH_KEY")
fi

echo "→ SSH zu $SSH_TARGET${SSH_KEY:+ (key: $SSH_KEY)}, patche /opt/sponsorplatz/docker-compose.yml …"

# Kommandos remote ausführen via Heredoc. 'bash -s' liest stdin, 'set -e' propagiert
# Fehler korrekt zum Aufrufer.
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" bash -s <<'REMOTE'
set -euo pipefail
COMPOSE=/opt/sponsorplatz/docker-compose.yml

if [ ! -f "$COMPOSE" ]; then
  echo "✗ $COMPOSE nicht gefunden — ist das eine Sponsorplatz-VM?" >&2
  exit 1
fi

# Backup mit Timestamp (nicht überschreiben, falls in derselben Sekunde mehrfach aufgerufen)
BACKUP="$COMPOSE.bak.$(date +%Y%m%d-%H%M%S)"
sudo cp -a "$COMPOSE" "$BACKUP"
echo "✓ Backup angelegt: $BACKUP"

# UMGEBUNG aus dem bestehenden compose ablesen — pro Cloud unterschiedlich
# ('oci-staging-free' bzw 'azure-staging'), wird als hardcoded SENTRY_ENVIRONMENT
# übernommen damit Sentry-Events pro Cloud filterbar bleiben.
UMGEBUNG=$(sudo grep -oE 'SPONSORPLATZ_UMGEBUNG: [a-z-]+' "$COMPOSE" | head -1 | awk '{print $2}')
if [ -z "$UMGEBUNG" ]; then
  echo "✗ SPONSORPLATZ_UMGEBUNG nicht in $COMPOSE gefunden — Abbruch" >&2
  exit 1
fi
echo "✓ Umgebung erkannt: $UMGEBUNG"

# Insertion-Anchor: vor 'JAVA_OPTS:' einfügen (steht in beiden cloud-init.tftpl
# als letzte env-Zeile vor 'volumes:'). Pattern muss eindeutig matchen.
if ! sudo grep -qE '^[[:space:]]+JAVA_OPTS:' "$COMPOSE"; then
  echo "✗ JAVA_OPTS-Anker nicht gefunden — compose-Struktur unerwartet, Abbruch" >&2
  exit 1
fi

# ── Sentry-Block (idempotent) ────────────────────────────────────────────────
if sudo grep -q '^[[:space:]]*SENTRY_DSN:' "$COMPOSE"; then
  echo "✓ Sentry-ENVs bereits vorhanden — skip"
else
  echo "→ Sentry-ENVs einfügen"
  sudo awk -v env="$UMGEBUNG" '
    /^[[:space:]]+JAVA_OPTS:/ && !inserted {
      print "            SENTRY_DSN: \"${SENTRY_DSN:-}\""
      print "            SENTRY_RELEASE: \"${SENTRY_RELEASE:-}\""
      print "            SENTRY_ENVIRONMENT: " env
      inserted = 1
    }
    { print }
  ' "$COMPOSE" | sudo tee "$COMPOSE.new" > /dev/null
  sudo mv "$COMPOSE.new" "$COMPOSE"
  echo "✓ Sentry-ENVs eingefügt"
fi

# ── Google-OIDC-Block (idempotent) ───────────────────────────────────────────
if sudo grep -q 'GOOGLE_CLIENT_ID' "$COMPOSE"; then
  echo "✓ Google-OIDC-ENVs bereits vorhanden — skip"
else
  echo "→ Google-OIDC-ENVs einfügen"
  sudo awk '
    /^[[:space:]]+JAVA_OPTS:/ && !inserted {
      print "            SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID: \"${GOOGLE_CLIENT_ID:-}\""
      print "            SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET: \"${GOOGLE_CLIENT_SECRET:-}\""
      print "            SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_NAME: Google"
      print "            SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE: openid,profile,email"
      print "            SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_GOOGLE_ISSUER_URI: https://accounts.google.com"
      inserted = 1
    }
    { print }
  ' "$COMPOSE" | sudo tee "$COMPOSE.new" > /dev/null
  sudo mv "$COMPOSE.new" "$COMPOSE"
  echo "✓ Google-OIDC-ENVs eingefügt"
fi

# ── Validate ────────────────────────────────────────────────────────────────
# 'docker compose config' parst die Datei. Bei Syntax-Fehlern Rollback aus Backup.
cd /opt/sponsorplatz
if ! sudo docker compose config -q 2>&1; then
  echo "✗ compose-File invalid nach Patch — Rollback aus $BACKUP" >&2
  sudo cp -a "$BACKUP" "$COMPOSE"
  exit 1
fi
echo "✓ compose-File ist valid"
REMOTE

# ── Optional restart ───────────────────────────────────────────────────────
if [ "$RESTART_FLAG" = "--restart" ]; then
  echo "→ Container neu starten (--restart)"
  ssh "${SSH_OPTS[@]}" "$SSH_TARGET" 'cd /opt/sponsorplatz && sudo docker compose up -d --force-recreate app'
  echo "✓ App-Container neu gestartet"
else
  SSH_HINT="ssh${SSH_KEY:+ -i $SSH_KEY} $SSH_TARGET"
  cat <<HINT
ℹ Container wurde NICHT neu gestartet — ENVs werden erst beim Re-Create wirksam.
   Entweder direkt:
     $SSH_HINT 'cd /opt/sponsorplatz && sudo docker compose up -d --force-recreate app'
   Oder nächsten CD-Run abwarten (--force-recreate ist im Deploy-Step gesetzt).
HINT
fi

echo "✓ Done."
