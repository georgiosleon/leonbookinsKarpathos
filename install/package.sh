#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TYPE=""
INSTALL_DEPS=false
PACKAGE_MANAGER=""

while (($#)); do
  arg="$1"
  case "$arg" in
    deb|linux|dmg) TYPE="$arg" ;;
    --install-deps) INSTALL_DEPS=true ;;
    --manager)
      (($# >= 2)) || { echo "--manager requires dnf, apt-get, or brew." >&2; exit 2; }
      PACKAGE_MANAGER="$2"
      shift
      ;;
    --manager=*) PACKAGE_MANAGER="${arg#--manager=}" ;;
    -h|--help)
      echo "Usage: $0 [deb|linux|dmg] [--install-deps] [--manager dnf|apt-get|brew]"
      echo "  --install-deps  Install missing build dependencies using the selected package manager."
      echo "  --manager       Explicitly select a package manager when distro detection is unavailable."
      exit 0
      ;;
    *) echo "Unknown argument: $arg" >&2; exit 2 ;;
  esac
  shift
done

OS="$(uname -s)"
if [[ "$OS" == "Linux" ]]; then
  if [[ -z "$PACKAGE_MANAGER" ]]; then
    if command -v dnf >/dev/null 2>&1; then
      PACKAGE_MANAGER=dnf
    elif command -v apt-get >/dev/null 2>&1; then
      PACKAGE_MANAGER=apt-get
    fi
  fi
  if [[ -z "$TYPE" ]]; then
    case "$PACKAGE_MANAGER" in
      dnf) TYPE=linux ;;
      apt-get) TYPE=deb ;;
      *)
        RELEASE_ID="$(sed -n 's/^ID=//p' /etc/os-release 2>/dev/null | tr -d '\"' | head -n 1)"
        RELEASE_LIKE="$(sed -n 's/^ID_LIKE=//p' /etc/os-release 2>/dev/null | tr -d '\"' | head -n 1)"
        case " $RELEASE_ID $RELEASE_LIKE " in
          *" fedora "*|*" rhel "*|*" centos "*) TYPE=linux ;;
          *" debian "*|*" ubuntu "*) TYPE=deb ;;
          *)
            echo "Could not detect the Linux package type or package manager. Specify linux or deb; optionally choose --manager dnf or --manager apt-get." >&2
            exit 2
            ;;
        esac
        ;;
    esac
  fi
fi

if [[ -z "$TYPE" ]]; then
  case "$OS" in
    Darwin) TYPE=dmg ;;
    Linux) TYPE=linux ;;
    *) echo "Specify a package type: deb, linux, or dmg." >&2; exit 2 ;;
  esac
fi

case "$OS:$TYPE" in
  Linux:deb|Linux:linux|Darwin:dmg) ;;
  *) echo "Package type '$TYPE' must be built on its matching operating system." >&2; exit 2 ;;
esac

fail() {
  echo "ERROR: $1" >&2
  exit 1
}

if [[ -n "$PACKAGE_MANAGER" ]]; then
  case "$OS:$PACKAGE_MANAGER" in
    Linux:dnf|Linux:apt-get|Darwin:brew) ;;
    *)
      fail "Package manager '$PACKAGE_MANAGER' is not supported on $OS. Use dnf or apt-get on Linux and brew on macOS."
      ;;
  esac
  command -v "$PACKAGE_MANAGER" >/dev/null 2>&1 || fail \
    "'$PACKAGE_MANAGER' is not available in this shell. Run the script in the host OS terminal or install that package manager first."
fi

install_dependencies() {
  case "$OS" in
    Linux)
      if [[ "$PACKAGE_MANAGER" == dnf ]]; then
        packages=(java-25-openjdk-devel java-25-openjdk-jmods)
        case "$TYPE" in
          deb) packages+=(dpkg) ;;
        esac
        if [[ "$EUID" -eq 0 ]]; then
          dnf install -y "${packages[@]}"
        elif command -v sudo >/dev/null 2>&1; then
          sudo dnf install -y "${packages[@]}"
        else
          fail "Install dependencies as root: dnf install ${packages[*]}"
        fi
      elif [[ "$PACKAGE_MANAGER" == apt-get ]]; then
        packages=(openjdk-25-jdk)
        [[ "$TYPE" != deb ]] || packages+=(dpkg-dev)
        if [[ "$EUID" -eq 0 ]]; then
          apt-get update
          apt-get install -y "${packages[@]}"
        elif command -v sudo >/dev/null 2>&1; then
          sudo apt-get update
          sudo apt-get install -y "${packages[@]}"
        else
          fail "Install dependencies as root: apt-get install ${packages[*]}"
        fi
      else
        fail "Could not determine an available Linux package manager. Use --manager dnf or --manager apt-get, or install prerequisites manually."
      fi
      ;;
    Darwin)
      [[ -z "$PACKAGE_MANAGER" || "$PACKAGE_MANAGER" == brew ]] || fail \
        "Use Homebrew on macOS ( --manager brew )."
      PACKAGE_MANAGER=brew
      command -v brew >/dev/null 2>&1 || fail \
        "Homebrew is required for automatic JDK setup. Install JDK 25 manually; Maven is provided by the project wrapper."
      brew install --cask temurin@25
      ;;
  esac
}

java_home() {
  java -XshowSettings:properties -version 2>&1 \
    | sed -n 's/^[[:space:]]*java.home = //p' \
    | head -n 1
}

missing_tools=()
add_missing_tool() {
  local requested="$1"
  local existing
  for existing in "${missing_tools[@]}"; do
    [[ "$existing" != "$requested" ]] || return
  done
  missing_tools+=("$requested")
}
for tool in java javac jpackage; do
  command -v "$tool" >/dev/null 2>&1 || add_missing_tool "$tool"
done
if command -v java >/dev/null 2>&1; then
  JAVA_VERSION="$(java -version 2>&1 | sed -n '1s/.*version "\(.*\)".*/\1/p')"
  [[ "$JAVA_VERSION" == 25* ]] || add_missing_tool "JDK 25"
fi
if command -v javac >/dev/null 2>&1; then
  JAVAC_VERSION="$(javac -version 2>&1 | awk '{ print $2 }')"
  [[ "$JAVAC_VERSION" == 25* ]] || add_missing_tool "JDK 25"
fi
if command -v jpackage >/dev/null 2>&1; then
  JPACKAGE_VERSION="$(jpackage --version 2>&1 | head -n 1)"
  [[ "$JPACKAGE_VERSION" == 25* ]] || add_missing_tool "JDK 25"
fi
if command -v java >/dev/null 2>&1; then
  JAVA_HOME_DETECTED="$(java_home)"
  [[ -d "$JAVA_HOME_DETECTED/jmods" ]] || add_missing_tool "JDK module files (jmods)"
fi
case "$TYPE" in
  deb) command -v dpkg-deb >/dev/null 2>&1 || add_missing_tool "dpkg-deb" ;;
  linux) command -v tar >/dev/null 2>&1 || add_missing_tool "tar" ;;
  dmg) command -v hdiutil >/dev/null 2>&1 || add_missing_tool "hdiutil" ;;
esac

if ((${#missing_tools[@]})); then
  if [[ "$INSTALL_DEPS" != true ]]; then
    fail "Missing tools: ${missing_tools[*]}. Re-run with --install-deps to install supported dependencies, or see install/README.md."
  fi
  install_dependencies
fi

for tool in java javac jpackage; do
  command -v "$tool" >/dev/null 2>&1 || fail \
    "$tool is still missing after dependency setup. Check PATH and install JDK 25 (including jpackage)."
done

[[ -x "$ROOT/mvnw" ]] || fail "Maven Wrapper is missing or not executable. Restore the mvnw script and .mvn/wrapper files."

JAVA_HOME_DETECTED="$(java_home)"
[[ -d "$JAVA_HOME_DETECTED/jmods" ]] || fail \
  "JDK module files are missing from $JAVA_HOME_DETECTED/jmods. Fedora users: run 'sudo dnf install java-25-openjdk-jmods'. jpackage needs these files to create the bundled runtime."

JAVA_VERSION="$(java -version 2>&1 | sed -n '1s/.*version "\(.*\)".*/\1/p')"
JAVAC_VERSION="$(javac -version 2>&1 | awk '{ print $2 }')"
JPACKAGE_VERSION="$(jpackage --version 2>&1 | head -n 1)"

[[ "$JAVA_VERSION" == 25* ]] || fail "java must be JDK 25 (found '${JAVA_VERSION:-unknown}')."
[[ "$JAVAC_VERSION" == 25* ]] || fail "javac must be JDK 25 (found '${JAVAC_VERSION:-unknown}')."
[[ "$JPACKAGE_VERSION" == 25* ]] || fail "jpackage must be from JDK 25 (found '${JPACKAGE_VERSION:-unknown}')."

case "$TYPE" in
  deb)
    command -v dpkg-deb >/dev/null 2>&1 || fail "dpkg-deb is required to build .deb packages."
    ;;
  linux)
    command -v tar >/dev/null 2>&1 || fail "tar is required to create the Linux application archive."
    ;;
  dmg)
    command -v hdiutil >/dev/null 2>&1 || fail \
      "hdiutil is required to build macOS .dmg packages; use the native macOS build."
    ;;
esac

VERSION="$(sed -n '/<\/parent>/,$s/.*<version>\([^<]*\)<\/version>.*/\1/p' "$ROOT/pom.xml" | head -n 1)"
if [[ -z "$VERSION" ]]; then
  echo "Could not read the project version from pom.xml." >&2
  exit 1
fi

cd "$ROOT"
./mvnw -B package

APP_JAR="target/leonbookinsKarpathos-${VERSION}.jar"
if [[ ! -f "$APP_JAR" ]]; then
  echo "Maven package not found: $APP_JAR" >&2
  exit 1
fi

LAUNCHER_CLASSES="target/desktop-launcher/classes"
PACKAGE_INPUT="target/desktop-package"
mkdir -p "$LAUNCHER_CLASSES" "$PACKAGE_INPUT" "install/dist/$TYPE"
OUTPUT="$(mktemp -d "$ROOT/target/desktop-output.XXXXXX")"
javac --release 25 -d "$LAUNCHER_CLASSES" \
  install/desktop-launcher/src/com/leonsoft/desktop/DesktopLauncher.java
jar --create --file "$PACKAGE_INPUT/desktop-launcher.jar" \
  --main-class com.leonsoft.desktop.DesktopLauncher -C "$LAUNCHER_CLASSES" .
cp "$APP_JAR" "$PACKAGE_INPUT/leonbookinsKarpathos.jar"

PACKAGE_ARGS=()
case "$TYPE" in
  deb)
    PACKAGE_ARGS+=(--linux-package-name leon-bookings --linux-app-category Office --linux-shortcut)
    ;;
  linux)
    ;;
  dmg)
    PACKAGE_ARGS+=(--mac-package-name LeonBookings --mac-package-identifier com.leonsoft.leonbookings)
    ;;
  *)
    echo "Supported package types on this platform: deb, linux, dmg." >&2
    exit 2
    ;;
esac

jpackage_type="$TYPE"
if [[ "$TYPE" == linux ]]; then
  jpackage_type=app-image
fi

jpackage \
  --type "$jpackage_type" \
  --name LeonBookings \
  --app-version "$VERSION" \
  --input "$PACKAGE_INPUT" \
  --main-jar desktop-launcher.jar \
  --main-class com.leonsoft.desktop.DesktopLauncher \
  --add-modules ALL-MODULE-PATH \
  --dest "$OUTPUT" \
  "${PACKAGE_ARGS[@]}"

if [[ "$TYPE" == linux ]]; then
  archive="install/dist/linux/LeonBookings-${VERSION}-linux-$(uname -m).tar.gz"
  mkdir -p install/dist/linux
  tar -C "$OUTPUT" -czf "$archive" .
  echo "Portable Linux application created at $archive"
else
  mkdir -p "install/dist/$TYPE"
  cp "$OUTPUT"/* "install/dist/$TYPE/"
  echo "Installer created in install/dist/$TYPE/"
fi
