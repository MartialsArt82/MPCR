# Privacy Notice for MPCR-Screen

Last updated November 25, 2026. This notice describes MPCR from version 1.5 onward and distinguishes operating the viewer from using external services.

## Contact

The project maintainer is Paul Weigl. Name, postal address, and contact details are provided in the [Legal Notice](https://www.paulweigl.de/impressum-1042906.html). For matters concerning personal data, please use the contact details listed there. GitHub issues are public.

## Using the App

| Data | Purpose and Recipients | Retention |
| --- | --- | --- |
| Entered IP address or hostname and port | Establishing a connection to the selected MPC. For hostnames, the operating system may use its configured DNS service. | Android and Linux store the address and optional device name locally when you select **Save Device** and choose **Connect**. The saved list is retained until cleared or until the local app data is deleted. Android carries forward an address that you previously opted to remember. An invocation entered in a shell may remain in your shell history. |
| Remote Screen password | The app processes the password for authentication and sends the verification data specified by the protocol to the MPC. | Processed in memory. The app does not store the password persistently or in the diagnostic log. |
| Video, optional audio, and remote control | The MPC sends video data and, when audio is enabled, audio data. When control is enabled, mouse or touch events are sent to the MPC. | The app processes this data in memory for display, playback, and input. It does not create recordings. |
| Support button visibility | Android remembers your chosen visibility setting locally. | Until changed or until local app data is deleted. |
| Diagnostic log on Linux menu launch | Linux may locally log startup errors along with information about the Java and display environment. | The log is located at `${XDG_STATE_HOME:-$HOME/.local/state}/mpcr-screen/startup.log` and is overwritten on the next menu launch. You can also delete the file manually. |

MPCR uses app data solely for operating the viewer as described above. The app never uses it for newsletters, advertising, or other purposes. There is no newsletter signup in the app. The viewer does not operate a project server and does not send telemetry, advertising data, or crash reports to the project maintainer. Remote Screen authenticates the login. Video, audio, and control traffic is unencrypted. Use MPCR on a trusted network.

Use **Clear saved devices** in the connection dialog’s saved-device dropdown to delete the saved list. On Linux, **OBS Output** makes decoded video frames available to recording software on the same computer for as long as the output is on. It listens on the loopback interface and uses a per-session URL. The output contains only screen pixels. It closes on disconnect or when stopped.

On Android, that action is also available in **Settings → Privacy**, and you can delete all app data via the Android system settings. On Linux, the device list is saved in `${XDG_CONFIG_HOME:-$HOME/.config}/mpcr-screen/devices.properties`. The app requests only the `INTERNET` permission. It does not access the microphone, camera, or location.

## GitHub and Ko-fi

- **GitHub:** GitHub operates the repository and hosts the downloads. When you visit pages or download files, GitHub processes usage data generated for the platform. Contributions in public issues and pull requests are displayed publicly. You should only publish additional personal information there deliberately. [GitHub Privacy Statement](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement)
- **Ko-fi:** The voluntary support link opens Ko-fi in your browser. MPCR only passes the target URL of the link. MPC addresses, passwords, video, audio, and control inputs are not included. Ko-fi and the selected payment provider process data according to their own privacy policies. When a payment is made, transaction details may become accessible to the recipient. [Ko-fi Privacy Policy](https://more.ko-fi.com/privacy) · [Ko-fi Supporter Payments FAQ](https://help.ko-fi.com/hc/en-us/articles/360013140633-Supporter-payments-FAQ)

### Voluntary Tips

Through Ko-fi, you can support the project with a voluntary tip without consideration. Ko-fi or the payment provider may make details such as name or username, email address, and transaction data (for example amount, date, payment status) available to Paul Weigl. Paul Weigl is the controller for the processing of these received details. Contact details are available in the Legal Notice linked above.

Paul Weigl uses the received details solely to process the tip and to comply with statutory retention obligations. The legal basis for processing is Art. 6(1)(b) GDPR. Where personal data must be retained due to legal obligations, the legal basis is Art. 6(1)(c) GDPR. The data is stored for the duration of processing and, where applicable, until the expiry of relevant statutory retention periods. Where required by law, competent authorities may receive data. Providing payment details is voluntary. It is required to make a tip and has no effect on using MPCR.

App data and data from tips are not used for newsletters or advertising. [GDPR, Art. 6 and 13](https://eur-lex.europa.eu/eli/reg/2016/679/oj?locale=en)

## Data Protection Rights

Where the GDPR applies to the respective processing, you may request access, rectification, erasure, or restriction from the controller. Depending on the requirements, the rights to object and to data portability also apply. You also have the right to lodge a complaint with a data protection supervisory authority. For questions regarding processing by GitHub, Ko-fi, or a payment provider, please contact the respective provider directly. [GDPR, in particular Art. 15–21 and 77](https://eur-lex.europa.eu/eli/reg/2016/679/oj?locale=en)
