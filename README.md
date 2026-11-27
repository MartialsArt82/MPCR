# MPCR-Screen

**Unofficial open-source viewer for the Remote Screen feature of compatible Akai MPC devices.** MPCR is an independent project and is neither published by nor affiliated with Akai Professional or inMusic.

The viewer displays the MPC screen on Android or Linux. Remote control and audio can be enabled if allowed on the MPC.

Check **Marks** to preview the red circle. With **Control** enabled, it highlights clicks, taps and drags made in the viewer. The marker follows the input and briefly stays visible after release. The documented Remote Screen protocol does not report touches on the MPC itself or clicks from a mouse connected directly to it.

## Download and Connection

You can find the signed Android APK, the Linux RPM for Fedora and Nobara, and the portable Linux archive in the [GitHub Releases](https://github.com/MartialsArt82/MPCR/releases).

1. On the MPC, enable **Preferences → Enhancements → Remote Screen**. Enable **Allow Remote Control** or **Share Audio** if needed.
2. Connect the MPC and the viewer to the same trusted network. Enter the displayed MPC address and the Remote Screen password in the connection dialog. Use the eye button to reveal or hide the password. Check **Save Device** and enter a name to add the address to the dropdown for next time. Passwords are entered afresh for each connection.
3. Select **Connect**. When in fullscreen mode, **Window** returns you to the normal view. On Linux, you can also press **F11** or **Esc**.

The protocol transmits video, audio, and control data without encryption. For more information, see the [Remote Screen documentation from inMusic](https://support.inmusicstore.com/en/support/solutions/articles/69000885284-mpc-3-10-1-enhancements-frequently-asked-questions).

## Native resolution and OBS

- **1:1**, beside **Fullscreen**, displays one MPC pixel per screen pixel. Choose it again to return to fit mode. If the window or display is smaller than the native image, it shows the center at its original scale.
- On Linux, select **OBS Output** once the MPC image is visible. MPCR-Screen switches to a small preview and shows a local URL and the native dimensions. In OBS, add a **Browser Source**, paste the URL and set its width and height to the reported size. Set the OBS base canvas and output resolution to that size and leave the source transform at its original scale.
- OBS receives the original decoded video pixels without an additional encoding or resize step in MPCR-Screen. The local output is video-only and excludes app controls, input marks, and audio. Select **Stop OBS** or disconnect the MPC to end the output.

## Source Code and Releases

- [Build instructions for Android and Linux](BUILD.md)
- [MIT License for project-owned components](LICENSE) and [Origin and third-party licenses](THIRD_PARTY.md)
- [Privacy Notice](PRIVACY.md) and [Legal Notice](IMPRESSUM.md)

The Android version requires Android 8.0 or newer. On Fedora and Nobara, you can install the RPM package with `sudo dnf install ./mpcr-screen-1.8.1-1.noarch.rpm`. After extracting the portable archive, launch it with `./mpcr-screen`. If you encounter startup issues on Linux, `mpcr-screen --diagnose` displays information about the Java runtime and display.
