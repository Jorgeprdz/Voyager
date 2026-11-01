#!/data/data/com.termux/files/usr/bin/bash
# FinderDex R3: exact SHA and verified signed APK downloader for an authorized Termux session.
# Downloads to Android Downloads; deliberately never installs and never overwrites differing files.
set -euo pipefail
REPO="Jorgeprdz/Voyager"
COMMIT="${1:-}"
DEST="/storage/emulated/0/Download/FinderDex-R3-debug.apk"
[[ "$COMMIT" =~ ^[a-fA-F0-9]{40}$ ]] || {
  echo "Uso: bash finderdex-r3-download.sh <SHA de 40 caracteres del commit>" >&2
  exit 2
}
for cmd in gh sha256sum grep awk mktemp cp; do
  command -v "$cmd" >/dev/null || { echo "Falta $cmd en Termux" >&2; exit 2; }
done
gh auth status >/dev/null || { echo "Primero autentica GitHub con: gh auth login" >&2; exit 2; }
if [[ ! -w /storage/emulated/0/Download ]]; then
  command -v termux-setup-storage >/dev/null || { echo "Termux Storage no disponible" >&2; exit 2; }
  termux-setup-storage
  [[ -w /storage/emulated/0/Download ]] || { echo "Autoriza Descargas y vuelve a ejecutar" >&2; exit 2; }
fi
RUN=""
for ((i=0;i<90;i++)); do
  RUN="$(gh run list -R "$REPO" --workflow finder-dex.yml --commit "$COMMIT" --limit 10     --json databaseId,headSha --jq '.[0].databaseId // empty')"
  [[ -n "$RUN" ]] && break
  sleep 10
done
[[ -n "$RUN" ]] || { echo "No hay workflow para ese commit" >&2; exit 1; }
gh run watch "$RUN" -R "$REPO" --exit-status
INFO="$(gh run view "$RUN" -R "$REPO" --json headSha,conclusion --jq '[.headSha,.conclusion] | join(" ")')"
[[ "$INFO" == "$COMMIT success" ]] || { echo "No coincide SHA/CI: $INFO" >&2; exit 1; }
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
gh run download "$RUN" -R "$REPO" --name FinderDex-R3-APK --dir "$TMP"
APK="$TMP/app-debug.apk"
[[ -s "$APK" && ! -e "$TMP/app-debug-androidTest.apk" ]] || {
  echo "El artifact no contiene exclusivamente la APK principal" >&2; exit 1;
}
EXPECTED="$(gh run view "$RUN" -R "$REPO" --log   | grep -Eo '[a-f0-9]{64}  app/build/outputs/apk/debug/app-debug.apk'   | awk '{print $1}' | tail -n 1)"
[[ "$EXPECTED" =~ ^[a-f0-9]{64}$ ]] || { echo "No hay hash verificable en el workflow" >&2; exit 1; }
ACTUAL="$(sha256sum "$APK" | awk '{print $1}')"
[[ "$ACTUAL" == "$EXPECTED" ]] || { echo "SHA256 incorrecto" >&2; exit 1; }
if command -v apksigner >/dev/null; then
  apksigner verify "$APK"
else
  echo "Verificación criptográfica local omitida; la firma fue verificada en GitHub Actions"
fi
if command -v aapt >/dev/null; then
  aapt dump badging "$APK" | grep -q "package: name='com.finderdex.preview'"
fi
if [[ -e "$DEST" ]]; then
  [[ "$(sha256sum "$DEST" | awk '{print $1}')" == "$EXPECTED" ]] && {
    printf 'APK idéntico ya presente: %s\nSHA256: %s\n' "$DEST" "$EXPECTED"; exit 0;
  }
  echo "Ya hay otra APK en $DEST. No se sobrescribe" >&2
  exit 2
fi
cp "$APK" "$DEST"
[[ "$(sha256sum "$DEST" | awk '{print $1}')" == "$EXPECTED" ]]
printf 'APK descargada en %s\nSHA256: %s\n' "$DEST" "$EXPECTED"
echo "No se instaló."
