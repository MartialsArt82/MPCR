# Herkunft und Lizenzen

Die [MIT-Lizenz](LICENSE) gilt für den eigenen Quellcode, die Build-Skripte und Tests, die Projektdokumentation und die eigenen Grafiken in diesem Repository. Die folgenden Drittbestandteile werden unter ihren eigenen Lizenzen weitergegeben.

| Bestandteil | Verwendung | Lizenz und Herkunft |
| --- | --- | --- |
| [Barlow](https://github.com/jpt/barlow), Regular und SemiBold | Android und Linux | SIL Open Font License 1.1 mit [vollständigem Lizenztext und Copyright](common/assets/Barlow-OFL.txt). Die TTF-Dateien stammen aus dem [Google-Fonts-Repository](https://github.com/google/fonts/tree/b46ed7e334ae7aaf3c6b9c446e087d57de39ff7d/ofl/barlow). |
| [FlatLaf 3.6.2](https://github.com/JFormDesigner/FlatLaf/tree/3.6.2) | Linux-Oberfläche, einschließlich der im JAR enthaltenen nativen Bibliotheken | Apache License 2.0. Das [unveränderte Maven-Artefakt](https://repo.maven.apache.org/maven2/com/formdev/flatlaf/3.6.2/flatlaf-3.6.2.jar) enthält die ursprünglichen Lizenzhinweise. Eine Kopie des [Lizenztexts](licenses/FlatLaf-Apache-2.0.txt) liegt bei. |

## Eigenständige Implementierung

Der Android- und der Linux-Viewer wurden für dieses Projekt in Java geschrieben. Als Referenzen dienten die [inMusic-Dokumentation](https://github.com/inmusic-software/mpc-remote-screen/tree/fdd9f08cf1e666dffc9b48a4df9223b8b85b400f) und die öffentlich lesbare [Community-Beschreibung des Remote-Screen-Protokolls](https://github.com/simonas-notcat/mpc-remote-screen-viewer-gpui/blob/44ef1480870fc45316528546cb86d55b1195add5/SPEC.md). Der Java-Code implementiert das beschriebene Nachrichtenformat und die Verbindung. Die Community-Beschreibung und ihr Rust-Code gehören nicht zum Quellpaket. Die inMusic-Software, Firmware und EULA werden nicht mitgeliefert. Die Android-SDK-Werkzeuge und die Java-Laufzeit sind externe Build- oder Laufzeitvoraussetzungen.

Das Monitor-Symbol mit 16 roten Pads und die neutrale Tasse wurden eigens für MPCR erstellt. Die bearbeitbaren Dateien liegen unter [common/assets](common/assets). Ältere App-Pakete hatten eine Grafik aus dem offiziellen [Ko-fi Creator Kit](https://more.ko-fi.com/brand-assets). Diese Grafik wird im MIT-Quellpaket und in den App-Paketen ab Version 1.5 durch die eigene Tasse ersetzt.

## Marken

MPCR ist ein unabhängiges, inoffizielles Projekt. Die Namen Akai Professional, MPC und inMusic bezeichnen kompatible Geräte, Funktionen und deren Anbieter. Markenzeichen gehören ihren jeweiligen Inhabern. MPCR ist weder von Akai Professional oder inMusic herausgegeben noch mit ihnen verbunden. Ko-fi bezeichnet den externen Dienst, zu dem der Support-Link führt.
