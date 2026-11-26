#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${JAVA_HOME:?Set JAVA_HOME to a JDK, for example JDK 21}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
mkdir -p build dist

compile() {
    local target=$1
    shift
    rm -rf "build/$target/classes"
    mkdir -p "build/$target/classes"
    local -a sources
    mapfile -d '' sources < <(find common/src "$target/src" -name '*.java' -print0)
    javac --release 8 -Xlint:-options -encoding UTF-8 "$@" -d "build/$target/classes" "${sources[@]}"
}

build_linux() {
    compile linux -classpath linux/lib/flatlaf.jar
    printf 'Main-Class: mpc.remote.LinuxViewer\nClass-Path: flatlaf.jar\nImplementation-Version: %s\n' \
        "$(awk '$1 == "Version:" {print $2}' linux/mpc-remote-screen.spec)" > build/linux/MANIFEST.MF
    jar cfm dist/mpc-remote-screen-linux.jar build/linux/MANIFEST.MF -C build/linux/classes . -C common assets
    cp linux/lib/flatlaf.jar dist/
    local portable=build/linux/mpc-remote-screen-linux
    mkdir -p "$portable"
    cp dist/mpc-remote-screen-linux.jar dist/flatlaf.jar linux/mpc-remote-screen \
        README.md LICENSE THIRD_PARTY.md PRIVACY.md IMPRESSUM.md common/assets/mpc-remote-screen.png \
        common/assets/Barlow-OFL.txt licenses/FlatLaf-Apache-2.0.txt "$portable/"
    tar czf dist/mpc-remote-screen-linux.tar.gz -C build/linux mpc-remote-screen-linux
}

build_rpm() {
    build_linux
    local topdir="$PWD/build/rpm"
    local package
    package=$(rpmspec -q --qf '%{name}-%{version}-%{release}.%{arch}.rpm\n' linux/mpc-remote-screen.spec)
    mkdir -p "$topdir/SOURCES"
    cp dist/mpc-remote-screen-linux.tar.gz "$topdir/SOURCES/"
    rpmbuild -bb --nodeps --define "_topdir $topdir" \
        --define "_tmppath ${TMPDIR:-/tmp}" linux/mpc-remote-screen.spec
    cp "$topdir/RPMS/noarch/$package" dist/
}

build_android() {
    : "${ANDROID_PLATFORM:?Set ANDROID_PLATFORM to an SDK platform, for example platforms/android-34}"
    : "${ANDROID_BUILD_TOOLS:?Set ANDROID_BUILD_TOOLS to the Android build-tools directory}"
    : "${APK_KEYSTORE:?Set APK_KEYSTORE to your APK signing keystore}"
    : "${APK_KEY_ALIAS:?Set APK_KEY_ALIAS to its key alias}"
    local android_jar="$ANDROID_PLATFORM/android.jar"
    local bt="$ANDROID_BUILD_TOOLS"
    local resources=build/android/res
    mkdir -p build/android
    rm -rf "$resources"
    cp -R android/res "$resources"
    mkdir -p "$resources/drawable-nodpi" "$resources/font" build/android/assets
    cp common/assets/mpc-remote-screen.png "$resources/drawable-nodpi/mpc_viewer.png"
    cp common/assets/support.png "$resources/drawable-nodpi/support.png"
    cp common/assets/Barlow-Regular.ttf "$resources/font/barlow_regular.ttf"
    cp common/assets/Barlow-SemiBold.ttf "$resources/font/barlow_semibold.ttf"
    cp LICENSE THIRD_PARTY.md PRIVACY.md IMPRESSUM.md common/assets/Barlow-OFL.txt \
        common/assets/privacy-en.txt build/android/assets/
    "$bt/aapt2" compile --dir "$resources" -o build/android/res.zip
    "$bt/aapt2" link -I "$android_jar" --manifest android/AndroidManifest.xml \
        --java build/android/generated -A build/android/assets \
        -o build/android/unsigned.apk build/android/res.zip
    compile android -classpath "$android_jar" -sourcepath build/android/generated
    jar cf build/android/classes.jar -C build/android/classes .
    "$bt/d8" --lib "$android_jar" --min-api 26 --output build/android build/android/classes.jar
    (cd build/android && zip -q -0 unsigned.apk classes.dex)
    "$bt/zipalign" -f 4 build/android/unsigned.apk build/android/aligned.apk
    "$bt/apksigner" sign --ks "$APK_KEYSTORE" --ks-key-alias "$APK_KEY_ALIAS" \
        --out dist/mpc-remote-screen-android.apk build/android/aligned.apk
    "$bt/apksigner" verify dist/mpc-remote-screen-android.apk
}

case "${1:-}" in
    linux) build_linux ;;
    rpm) build_rpm ;;
    android) build_android ;;
    *) printf '%s\n' 'Usage: ./build.sh linux|rpm|android' >&2; exit 2 ;;
esac
