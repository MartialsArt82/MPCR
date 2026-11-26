# MPCR-Screen

**Unofficial open-source viewer for the Remote Screen feature of compatible Akai MPC devices.** MPCR is an independent project and is neither published by nor affiliated with Akai Professional or inMusic.

The viewer displays the MPC screen on Android or Linux. Remote control and audio can be enabled if allowed on the MPC.

Check **Marks** to preview the red circle. With **Control** enabled, it highlights clicks, taps and drags made in the viewer. The marker follows the input and briefly stays visible after release. The documented Remote Screen protocol does not report touches on the MPC itself or clicks from a mouse connected directly to it.

## Download and Connection

You can find the signed Android APK, the Linux RPM for Fedora and Nobara, and the portable Linux archive in the [GitHub Releases](https://github.com/MartialsArt82/MPCR/releases).

1. On the MPC, enable **Preferences → Enhancements → Remote Screen**. Enable **Allow Remote Control** or **Share Audio** if needed.
2. Connect the MPC and the viewer to the same trusted network. Enter the displayed MPC address and the Remote Screen password in the viewer.
3. Select **Connect**. When in fullscreen mode, **Window** returns you to the normal view. On Linux, you can also press **F11** or **Esc**.

The protocol transmits video, audio, and control data without encryption. For more information, see the [Remote Screen documentation from inMusic](https://support.inmusicstore.com/en/support/solutions/articles/69000885284-mpc-3-10-1-enhancements-frequently-asked-questions).

## Source Code and Releases

- [Build instructions for Android and Linux](BUILD.md)
- [MIT License for project-owned components](LICENSE) and [Origin and third-party licenses](THIRD_PARTY.md)
- [Privacy Notice](PRIVACY.md) and [Legal Notice](IMPRESSUM.md)

The Android version requires Android 8.0 or newer. On Fedora and Nobara, you can install the RPM package with `sudo dnf install ./mpcr-screen-1.7.1-1.noarch.rpm`. After extracting the portable archive, launch it with `./mpcr-screen`. If you encounter startup issues on Linux, `mpcr-screen --diagnose` displays information about the Java runtime and display.
