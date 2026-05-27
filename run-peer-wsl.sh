#!/usr/bin/env bash
set -euo pipefail

# Run one peer from WSL/Linux.
# Defaults are kept separate from Windows so the two peers do not reuse one profile.
cd "$(dirname "${BASH_SOURCE[0]}")"

if [[ "${P2P_SKIP_GUI_LIB_CHECK:-0}" != "1" ]]; then
    REQUIRED_GUI_LIBS=(libXext.so.6)
    MISSING_GUI_LIBS=()
    for lib in "${REQUIRED_GUI_LIBS[@]}"; do
        if ! ldconfig -p 2>/dev/null | grep -q "$lib"; then
            MISSING_GUI_LIBS+=("$lib")
        fi
    done
    if ((${#MISSING_GUI_LIBS[@]} > 0)); then
        echo "[ERROR] Missing WSL GUI library: ${MISSING_GUI_LIBS[*]}"
        echo "[INFO] This Swing app needs X11/WSLg libraries inside the WSL distro."
        if command -v apt >/dev/null 2>&1; then
            echo "[FIX] sudo apt update && sudo apt install -y libxext6 libxi6 libxrender1 libxtst6 libxrandr2 libfontconfig1"
        elif command -v pacman >/dev/null 2>&1; then
            echo "[FIX] sudo pacman -S --needed libxext libxi libxrender libxtst libxrandr fontconfig"
        elif command -v dnf >/dev/null 2>&1; then
            echo "[FIX] sudo dnf install libXext libXi libXrender libXtst libXrandr fontconfig"
        else
            echo "[FIX] Install the package that provides libXext.so.6 for your distro."
        fi
        exit 1
    fi
fi

if [[ "${P2P_SKIP_FONT_CHECK:-0}" != "1" ]]; then
    if ! command -v fc-match >/dev/null 2>&1; then
        echo "[ERROR] fontconfig command fc-match was not found."
        echo "[FIX] sudo pacman -S --needed fontconfig ttf-dejavu noto-fonts"
        exit 1
    fi
    if [[ -z "$(fc-match -f '%{file}' sans-serif 2>/dev/null)" ]]; then
        echo "[ERROR] No usable system font found for Java Swing."
        echo "[INFO] Fontconfig is installed, but the WSL distro has no font package available."
        if command -v pacman >/dev/null 2>&1; then
            echo "[FIX] sudo pacman -S --needed ttf-dejavu noto-fonts && fc-cache -fv"
        elif command -v apt >/dev/null 2>&1; then
            echo "[FIX] sudo apt update && sudo apt install -y fonts-dejavu fonts-noto-core && fc-cache -fv"
        elif command -v dnf >/dev/null 2>&1; then
            echo "[FIX] sudo dnf install dejavu-sans-fonts google-noto-sans-fonts && fc-cache -fv"
        else
            echo "[FIX] Install a TrueType font package, then run fc-cache -fv."
        fi
        exit 1
    fi
fi

DEFAULT_DATA_ROOT="${P2P_WSL_DATA_DIR:-runtime-data/peer-node-wsl}"
BOOTSTRAP_PORT_VALUE="${BOOTSTRAP_PORT:-${BOOTSTRAP_SERVER_PORT:-9000}}"
BOOTSTRAP_HOST_VALUE="${BOOTSTRAP_HOST:-}"

if [[ -z "$BOOTSTRAP_HOST_VALUE" ]]; then
    if [[ -n "${WSL_DISTRO_NAME:-}" ]] || grep -qiE '(microsoft|wsl)' /proc/version 2>/dev/null; then
        BOOTSTRAP_HOST_VALUE="$(awk '/^nameserver[[:space:]]+/ { print $2; exit }' /etc/resolv.conf 2>/dev/null || true)"
    fi
    BOOTSTRAP_HOST_VALUE="${BOOTSTRAP_HOST_VALUE:-localhost}"
fi

APP_ARGS=("$@")
DATA_ROOT="$DEFAULT_DATA_ROOT"
HAS_DATA_DIR=0

for ((i = 0; i < ${#APP_ARGS[@]}; i++)); do
    case "${APP_ARGS[$i]}" in
        --data-dir=*)
            DATA_ROOT="${APP_ARGS[$i]#--data-dir=}"
            HAS_DATA_DIR=1
            ;;
        --data-dir)
            if ((i + 1 < ${#APP_ARGS[@]})); then
                DATA_ROOT="${APP_ARGS[$((i + 1))]}"
                HAS_DATA_DIR=1
            fi
            ;;
    esac
done

if ((HAS_DATA_DIR == 0)); then
    APP_ARGS=("--data-dir=$DATA_ROOT" "${APP_ARGS[@]}")
fi

if [[ "${P2P_WRITE_BOOTSTRAP_CONFIG:-1}" != "0" ]]; then
    mkdir -p "$DATA_ROOT"
    {
        printf 'bootstrap.host=%s\n' "$BOOTSTRAP_HOST_VALUE"
        printf 'bootstrap.port=%s\n' "$BOOTSTRAP_PORT_VALUE"
    } > "$DATA_ROOT/config.properties"
fi

if ((${#APP_ARGS[@]} == 1)); then
    echo "[INFO] No peer args supplied. Opening app profile selection."
    echo "[TIP] WSL demo: bash run-peer-wsl.sh --peer-name=WSL --peer-port=5002"
fi

echo "[INFO] WSL data root: $DATA_ROOT"
echo "[INFO] Bootstrap: $BOOTSTRAP_HOST_VALUE:$BOOTSTRAP_PORT_VALUE"
echo "[INFO] Peer args: ${APP_ARGS[*]}"

echo "[INFO] Preparing bootstrap-server test artifact for Maven resolution. This does not start bootstrap-server."
mvn -pl bootstrap-server -am -DskipTests install

mvn -pl peer-node -DskipTests compile exec:java \
    -Dexec.mainClass="dungcony.ds.App" \
    -Dexec.args="${APP_ARGS[*]}"
