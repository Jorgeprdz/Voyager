#!/data/data/com.termux/files/usr/bin/bash
# FinderDex R2.2 exact-build verified downloader.
set -euo pipefail
REPO="Jorgeprdz/Voyager"
SHA="${1:-}"
DEST="/storage/emulated/0/Download/FinderDex-R2.2-debug.apk"
[[ "$SHA" =~ ^[a-fA-F0-9]{40}$ ]] || { echo "Uso: bash scripts/finderdex-r2.2-download.sh <SHA del commit>" >&2; exit 2; }
for cmd in gh sha256sum grep awk mktemp; do
  command -v "$cmd" >/dev/null || { echo "Falta $cmd en Termux." >&2; exit 2; }
done
gh auth status >/dev/null
if [[ ! -w /storage/emulated/0/Download ]]; then
  termux-setup-storage
  [[ -w /storage/emulated/0/Download ]] || { echo "Autoriza acceso a almacenamiento y reejecuta." >&2; exit 2; }
fi
RUN=""
for ((i=0;i<90;i++)); do
  RUN="$(gh run list -R "$REPO" --workflow finder-dex.yml --commit "$SHA" --limit 10 \
      --json databaseId,headSha --jq '.[0].databaseId // empty')"
  [[ -n "$RUN" ]] && break
  sleep 10
done
[[ -n "$RUN" ]] || { echo "No existe workflow para $SHA" >&2; exit 1; }
gh run watch "$RUN" -R "$REPO" --exit-status
ACTUAL="$(gh run view "$RUN" -R "$REPO" --json headSha,conclusion --jq '[.headSha,.conclusion] | join(" ")')"
[[ "$ACTUAL" == "$SHA success" ]] || { echo "No coincide SHA / workflow SUCCESS: $ACTUAL" >&2; exit 1; }
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
gh run download "$RUN" -R "$REPO" --name FinderDex-R2.2-APK --dir "$TMP"
APK="$TMP/app-debug.apk"
[[ -s "$APK" && ! -e "$TMP/app-debug-androidTest.apk" ]] || { echo "Artifact no contiene solo app-debug.apk." >&2; exit 1; }
EXPECTED="$(gh run view "$RUN" -R "$REPO" --log | grep -Eo '[a-f0-9]{64}  app/build/outputs/apk/debug/app-debug.apk' | awk '{print $1}' | tail -n 1)"
[[ "$EXPECTED" =~ ^[a-f0-9]{64}$ ]] || { echo "No se halló SHA256 de APK en logs." >&2; exit 1; }
ACTUAL_SHA="$(sha256sum "$APK" | awk '{print $1}')"
[[ "$EXPECTED" == "$ACTUAL_SHA" ]] || { echo "Checksum no coincide." >&2; exit 1; }
if command -v apksigner >/dev/null; then
  apksigner verify "$APK"
else
  echo "Firma verificada en GitHub Actions (apksigner local no disponible)."
fi
if command -v aapt >/dev/null; then
  aapt dump badging "$APK" | grep -q "package: name='com.finderdex.preview'"
else
  echo "Package com.finderdex.preview verificado en GitHub Actions."
fi
if [[ -e "$DEST" ]]; then
  [[ "$(sha256sum "$DEST" | awk '{print $1}')" == "$EXPECTED" ]] && { echo "APK idéntico ya existe: $DEST"; exit 0; }
  echo "El destino contiene un archivo diferente: no sobrescribir." >&2
  exit 2
fi
cp "$APK" "$DEST"
[[ "$(sha256sum "$DEST" | awk '{print $1}')" == "$EXPECTED" ]]
printf 'APK descargado: %s\nSHA256: %s\n' "$DEST" "$EXPECTED"
echo "Sin instalación automática."
