# MPCR Viewer aus dem Quellcode bauen

Die Befehle laufen in einer Linux-Shell ab dem Repository-Wurzelverzeichnis. Die Android-APK und die Linux-Anwendung verwenden dieselbe Java-Protokollimplementierung unter `common/src`. Die Oberflächen liegen in `android/src` und `linux/src`. Die mitgelieferten Drittkomponenten und deren Quellen sind unter [Herkunft und Lizenzen](THIRD_PARTY.md) aufgeführt.

## Voraussetzungen

- OpenJDK 21 mit `javac`, `jar` und `keytool`. Setze `JAVA_HOME` auf das JDK-Verzeichnis.
- Bash und die üblichen Linux-Werkzeuge. Für die Protokolltests wird Python 3 benötigt.
- Für RPM zusätzlich `rpmbuild` und `rpmspec`. Unter Fedora und Nobara liefert sie das Paket `rpm-build`.
- Für Android zusätzlich [Android SDK](https://developer.android.com/tools), Plattform API 34, Build Tools 36.0.0 und `zip`.

## Linux

```sh
./build.sh linux
./build.sh rpm
```

Das portable Archiv, das JAR und das RPM liegen danach in `dist/`. Zum Start des portablen Builds das Archiv entpacken und im entpackten Verzeichnis `./mpc-remote-screen` ausführen. Eine grafische Java-Laufzeit und X11 oder XWayland müssen installiert sein.

## Android

Installiere die Android-SDK-Komponenten mit dem SDK Manager und passe die Pfade an deine SDK-Installation an. Zum lokalen Testen kannst du einen eigenen Schlüssel erzeugen.

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

Der signierte Build liegt unter `dist/mpc-remote-screen-android.apk`. Das Signierwerkzeug fragt nach dem Passwort. Der private Veröffentlichungsschlüssel gehört nicht zum Quellcode. Eine lokal signierte APK kann eine bereits installierte Release-APK nur aktualisieren, wenn dieselbe Android-Signatur verwendet wird.

## Tests

```sh
./tests/check.sh
```

Die Tests verwenden einen lokalen MPC-Protokollsimulator. Sie prüfen unter anderem Passwortanmeldung, Verbindungsabbruch bei falschem Servernachweis, Bilddaten, Zeigerereignisse und Audio-Nachrichten.

## Grafiken bearbeiten

Die SVG-Quelldateien für Monitor und Tasse und die zugehörigen PNGs liegen in `common/assets/`. Die Builds verwenden die eingecheckten PNGs. Nach einer Änderung lassen sich diese mit Inkscape neu exportieren:

```sh
inkscape common/assets/mpc-remote-screen.svg --export-type=png --export-filename=common/assets/mpc-remote-screen.png
inkscape common/assets/support.svg --export-type=png --export-filename=common/assets/support.png
```
