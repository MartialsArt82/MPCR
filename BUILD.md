# Building MPCR Viewer from Source

Run these commands in a Linux shell from the repository root directory. The Android APK and the Linux application share the same Java protocol implementation under `common/src`. The user interfaces are located in `android/src` and `linux/src`. Included third-party components and their sources are listed in [Origin and Licenses](THIRD_PARTY.md).

## Prerequisites

- OpenJDK 21 with `javac`, `jar`, and `keytool`. Set `JAVA_HOME` to the JDK directory.
- Bash and standard Linux utilities. Python 3 is required for the protocol tests.
- For RPM packaging, additionally `rpmbuild` and `rpmspec`. On Fedora and Nobara, these are provided by the `rpm-build` package.
- For Android, additionally the [Android SDK](https://developer.android.com/tools), Platform API 34, Build Tools 36.0.0, and `zip`.

## Linux

```sh
./build.sh linux
./build.sh rpm
```

Afterwards, the portable archive, JAR, and RPM are located in `dist/`. To launch the portable build, extract the archive and run `./mpc-remote-screen` inside the extracted directory. A graphical Java runtime and X11 or XWayland must be installed.

## Android

Install the Android SDK components using the SDK Manager and adjust the paths to match your SDK installation. For local testing, you can generate your own signing key.

```sh
sdkmanager 'platforms;android-34' 'build-tools;36.0.0'
export ANDROID_PLATFORM="$ANDROID_HOME/platforms/android-34"
export ANDROID_BUILD_TOOLS="$ANDROID_HOME/build-tools/36.0.0"

keytool -genkeypair -keystore "$HOME/mpcr-local.p12" -storetype PKCS12 \
  -alias mpcr -keyalg RSA -keysize 3072 -validity 3650
export APK_KEYSTORE="$HOME/mpcr-local.p12"
export APK_KEY_ALIAS=mpcr
./build.sh android
```

The signed build is output to `dist/mpc-remote-screen-android.apk`. The signing tool will prompt for the password. The private release key is not part of the source code. A locally signed APK can only update an already installed release APK if the same Android signing key is used.

## Tests

```sh
./tests/check.sh
```

The tests use a local MPC protocol simulator. Among other things, they verify password authentication, connection termination on invalid server proof, screen data, pointer events, and audio messages.

## Editing Graphics

The SVG source files for the monitor and cup, along with their corresponding PNGs, are located in `common/assets/`. The builds use the checked-in PNGs. After making changes, you can re-export them with Inkscape:

```sh
inkscape common/assets/mpc-remote-screen.svg --export-type=png --export-filename=common/assets/mpc-remote-screen.png
inkscape common/assets/support.svg --export-type=png --export-filename=common/assets/support.png
```
