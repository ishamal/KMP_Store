#!/usr/bin/env bash
#
# Scaffolds a new store: common config + Android flavor branding (folder, colors, strings).
#
# Usage:
#   ./scripts/new-store.sh <storeName> [comma,separated,features]
#
# Examples:
#   ./scripts/new-store.sh storeD
#   ./scripts/new-store.sh storeD login,settings,orders,rebate
#
set -euo pipefail

STORE="${1:-}"
FEATURES="${2:-login,settings}"   # every store needs at least the login gate + settings tab

if [ -z "$STORE" ]; then
  echo "Usage: $0 <storeName> [comma,separated,features]" >&2
  echo "Example: $0 storeD login,settings,orders" >&2
  exit 1
fi

# Repo root (script can be run from anywhere)
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PKG_PATH="com/isharaw/kmpproj"

LOWER="$(printf '%s' "$STORE" | tr '[:upper:]' '[:lower:]')"
CAP="$(printf '%s' "$STORE" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')"
APP_ID="com.isharaw.kmpproj.$LOWER"

CONFIG="$ROOT/config/stores/$STORE.properties"
FLAVOR_DIR="$ROOT/androidApp/src/$STORE"
BRAND_DIR="$FLAVOR_DIR/kotlin/$PKG_PATH/branding"
RES_DIR="$FLAVOR_DIR/res/values"

if [ -f "$CONFIG" ] || [ -d "$FLAVOR_DIR" ]; then
  echo "Error: store '$STORE' already exists ($CONFIG or $FLAVOR_DIR)." >&2
  exit 1
fi

mkdir -p "$BRAND_DIR" "$RES_DIR" "$(dirname "$CONFIG")"

# 1) Common store config (read by buildSrc/StoreManifest.kt → drives Android flavor + iOS export)
cat > "$CONFIG" <<EOF
# $STORE config — read at build time by buildSrc/StoreManifest.kt
applicationId=$APP_ID
features=$FEATURES
EOF

# 2) Android brand colors (Compose) — flavor source set
cat > "$BRAND_DIR/BrandColors.kt" <<EOF
package com.isharaw.kmpproj.branding

import androidx.compose.ui.graphics.Color

/** $STORE brand colors. Customize to brand the app. */
object BrandColors {
    val primary = Color(0xFF6750A4)
    val secondary = Color(0xFF625B71)
}
EOF

# 3) Android wordings — flavor source set
cat > "$RES_DIR/strings.xml" <<EOF
<resources>
    <string name="app_name">$STORE</string>
    <string name="welcome_message">Welcome to $STORE</string>
</resources>
EOF

echo "Created store '$STORE':"
echo "  - $CONFIG"
echo "  - $BRAND_DIR/BrandColors.kt"
echo "  - $RES_DIR/strings.xml"
echo
echo "Next:"
echo "  1. Adjust features in:    config/stores/$STORE.properties   (keep at least: login,settings)"
echo "  2. Customize colors:      androidApp/src/$STORE/kotlin/.../branding/BrandColors.kt"
echo "  3. Customize wordings:    androidApp/src/$STORE/res/values/strings.xml"
echo "  4. Build the new flavor (config cache must refresh for the new store):"
echo "       ./gradlew :androidApp:assemble${CAP}Debug --no-configuration-cache"
