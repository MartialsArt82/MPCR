# MPCR Viewer

**Inoffizieller Open-Source-Viewer für die Remote-Screen-Funktion kompatibler Akai MPC-Geräte.** MPCR ist ein unabhängiges Projekt und weder von Akai Professional oder inMusic herausgegeben noch mit ihnen verbunden.

Der Viewer zeigt den Bildschirm der MPC auf Android oder Linux. Fernsteuerung und Audio lassen sich bei Freigabe auf der MPC zuschalten.

## Download und Verbindung

Die signierte Android-APK, das Linux-RPM für Fedora und Nobara sowie das portable Linux-Archiv findest du in den [GitHub Releases](https://github.com/MartialsArt82/MPCR/releases).

1. Aktiviere auf der MPC **Preferences → Enhancements → Remote Screen**. Schalte bei Bedarf **Allow Remote Control** oder **Share Audio** ein.
2. Verbinde die MPC und den Viewer mit demselben vertrauenswürdigen Netzwerk. Gib im Viewer die angezeigte MPC-Adresse und das Remote-Screen-Passwort ein.
3. Wähle **Verbinden**. Beim Vollbildwechsel bringt dich **Fenster** zur normalen Ansicht zurück. Unter Linux kannst du zusätzlich **F11** oder **Esc** verwenden.

Das Protokoll überträgt Bild-, Audio- und Steuerdaten ohne Verschlüsselung. Weitere Informationen findest du in der [Remote-Screen-Dokumentation von inMusic](https://support.inmusicstore.com/en/support/solutions/articles/69000885284-mpc-3-10-1-enhancements-frequently-asked-questions).

## Quellcode und Veröffentlichung

- [Build-Anleitung für Android und Linux](BUILD.md)
- [MIT-Lizenz für die eigenen Bestandteile](LICENSE) und [Herkunft und Drittanbieter-Lizenzen](THIRD_PARTY.md)
- [Datenschutzhinweise](PRIVACY.md) und [Anbieterkennzeichnung](IMPRESSUM.md)

Die Android-Version benötigt Android 8.0 oder neuer. Unter Fedora und Nobara kannst du die RPM-Datei mit `sudo dnf install ./mpc-remote-screen-1.5-1.noarch.rpm` installieren. Das portable Archiv startest du nach dem Entpacken mit `./mpc-remote-screen`. Bei Linux-Startproblemen zeigt `mpc-remote-screen --diagnose` Informationen zur Java-Laufzeit und Anzeige an.
