# Datenschutzhinweise für MPCR Viewer

Stand 25. November 2026. Diese Hinweise beschreiben MPCR ab Version 1.5 und unterscheiden den Betrieb des Viewers von der Nutzung externer Angebote.

## Kontakt

Projektbetreiber ist Paul Weigl. Name, Anschrift und Kontaktmöglichkeiten stehen in der [Anbieterkennzeichnung](https://www.paulweigl.de/impressum-1042906.html). Wenn es um persönliche Daten geht, nutze bitte den dort genannten Kontakt. GitHub-Issues sind öffentlich.

## Verwendung der App

| Daten | Zweck und Empfänger | Speicherung |
| --- | --- | --- |
| Eingetragene IP-Adresse oder Hostname und Port | Verbindungsaufbau zur ausgewählten MPC. Bei Hostnamen kann das Betriebssystem seinen konfigurierten DNS-Dienst nutzen. | Android speichert den zuletzt für einen Verbindungsversuch eingegebenen Eintrag nur, wenn du **MPC-Adresse merken** aktiviert hast. Die Option ist beim ersten Start und nach dem Update von einer früheren Version ausgeschaltet. Linux speichert ihn nicht in App-Einstellungen. Ein in der Shell eingegebener Aufruf kann in der Shell-Historie stehen. |
| Remote-Screen-Passwort | Die App verarbeitet das Passwort für die Anmeldung und sendet die im Protokoll vorgesehenen Prüfdaten an die MPC. | Verarbeitung im Arbeitsspeicher. Die App speichert das Passwort weder dauerhaft noch im Diagnoseprotokoll. |
| Bild, optional Audio und Fernsteuerung | Die MPC sendet die Bilddaten und bei aktiviertem Audio auch Audio. Bei aktivierter Steuerung gehen Maus- bzw. Touch-Ereignisse an die MPC. | Die App verarbeitet diese Daten für Anzeige, Wiedergabe und Bedienung im Arbeitsspeicher. Sie erstellt keine Aufzeichnungen. |
| Anzeige des Support-Buttons | Android merkt sich die gewählte Sichtbarkeit lokal. | Bis zur Änderung oder bis die lokalen App-Daten gelöscht werden. |
| Diagnose beim Menüstart unter Linux | Linux kann Startfehler mit Informationen zur Java- und Anzeigeumgebung lokal protokollieren. | Das Protokoll liegt unter `${XDG_STATE_HOME:-$HOME/.local/state}/mpc-remote-screen/startup.log` und wird beim nächsten Menüstart überschrieben. Du kannst die Datei auch manuell löschen. |

MPCR verwendet App-Daten ausschließlich für den oben beschriebenen Betrieb des Viewers. Die App verwendet sie weder für Newsletter noch für Werbung oder andere Zwecke. Es gibt keine Newsletter-Anmeldung in der App. Der Viewer betreibt keinen Projektserver und sendet keine Telemetrie, Werbedaten oder Fehlerberichte an den Projektbetreiber. Remote Screen authentifiziert die Anmeldung. Der Bild-, Audio- und Steuerdatenverkehr ist unverschlüsselt. Verwende MPCR in einem vertrauenswürdigen Netzwerk.

Auf Android löscht **Einstellungen → Datenschutz → Adresse vergessen** den gespeicherten Eintrag und schaltet das Merken aus. Auch das Ausschalten von **MPC-Adresse merken** löscht den Eintrag. Über die Android-Systemeinstellungen kannst du alle App-Daten löschen. Die App fordert ausschließlich die Berechtigung `INTERNET` an. Sie verwendet weder Mikrofon noch Kamera oder Standort.

## GitHub und Ko-fi

- **GitHub:** GitHub betreibt das Repository und stellt die Downloads bereit. Beim Aufruf oder Download verarbeitet GitHub die für die Plattform anfallenden Nutzungsdaten. Beiträge in öffentlichen Issues und Pull Requests werden öffentlich angezeigt. Zusätzliche personenbezogene Inhalte solltest du dort nur bewusst veröffentlichen. [GitHub-Datenschutzerklärung](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement)
- **Ko-fi:** Der freiwillige Support-Link öffnet Ko-fi im Browser. MPCR übergibt dabei lediglich die Zieladresse des Links. MPC-Adressen, Passwörter, Bilder, Audio und Steuereingaben werden dabei nicht mitgegeben. Ko-fi und der gewählte Zahlungsdienst verarbeiten Daten nach ihren eigenen Datenschutzhinweisen. Bei einer Zahlung können dem Empfänger Angaben zur Transaktion zugänglich werden. [Ko-fi-Datenschutzerklärung](https://more.ko-fi.com/privacy) · [Ko-fi-Hinweise zu Zahlungen](https://help.ko-fi.com/hc/en-us/articles/360013140633-Supporter-payments-FAQ)

### Freiwillige Trinkgelder

Über Ko-fi kannst du das Projekt mit einem freiwilligen Trinkgeld ohne Gegenleistung unterstützen. Ko-fi oder der Zahlungsdienst kann Paul Weigl dabei Angaben wie Name oder Nutzername, E-Mail-Adresse und Transaktionsdaten (zum Beispiel Betrag, Datum, Zahlungsstatus) zur Verfügung stellen. Verantwortlich für die eigene Verarbeitung dieser erhaltenen Daten ist Paul Weigl. Die Kontaktdaten stehen in der oben verlinkten Anbieterkennzeichnung.

Paul Weigl verwendet die erhaltenen Angaben ausschließlich zur Abwicklung des Trinkgelds und zur Erfüllung gesetzlicher Aufbewahrungspflichten. Rechtsgrundlage für die Abwicklung ist Art. 6 Abs. 1 lit. b DSGVO. Soweit personenbezogene Daten aufgrund gesetzlicher Pflichten aufzubewahren sind, ist die Rechtsgrundlage Art. 6 Abs. 1 lit. c DSGVO. Die Daten werden für die Dauer der Abwicklung und gegebenenfalls bis zum Ablauf einschlägiger gesetzlicher Aufbewahrungsfristen aufbewahrt. Soweit gesetzlich erforderlich, können zuständige Behörden Daten erhalten. Die Bereitstellung der Zahlungsdaten ist freiwillig. Sie ist für ein Trinkgeld erforderlich und hat keinen Einfluss auf die Nutzung von MPCR.

App-Daten und Daten aus Trinkgeldern werden nicht für Newsletter oder Werbung verwendet. [DSGVO, Art. 6 und 13](https://eur-lex.europa.eu/eli/reg/2016/679/oj?locale=de)

## Datenschutzrechte

Soweit die DSGVO auf die jeweilige Verarbeitung Anwendung findet, kannst du beim Verantwortlichen Auskunft, Berichtigung, Löschung oder Einschränkung verlangen. Je nach Voraussetzungen kommen Widerspruch und Datenübertragbarkeit hinzu. Du kannst dich außerdem bei einer Datenschutzaufsichtsbehörde beschweren. Bei Fragen zur Verarbeitung durch GitHub, Ko-fi oder einen Zahlungsdienst wende dich direkt an den jeweiligen Anbieter. [DSGVO, insbesondere Art. 15–21 und 77](https://eur-lex.europa.eu/eli/reg/2016/679/oj?locale=de)
