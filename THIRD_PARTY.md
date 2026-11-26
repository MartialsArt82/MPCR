# Origin and Licenses

The [MIT License](LICENSE) applies to the project-owned source code, build scripts and tests, project documentation, and custom graphics in this repository. The following third-party components are distributed under their own licenses.

| Component | Usage | License and Origin |
| --- | --- | --- |
| [Barlow](https://github.com/jpt/barlow), Regular and SemiBold | Android and Linux | SIL Open Font License 1.1 with [full license text and copyright](common/assets/Barlow-OFL.txt). The TTF files originate from the [Google Fonts repository](https://github.com/google/fonts/tree/b46ed7e334ae7aaf3c6b9c446e087d57de39ff7d/ofl/barlow). |
| [FlatLaf 3.6.2](https://github.com/JFormDesigner/FlatLaf/tree/3.6.2) | Linux UI, including the native libraries bundled in the JAR | Apache License 2.0. The [unmodified Maven artifact](https://repo.maven.apache.org/maven2/com/formdev/flatlaf/3.6.2/flatlaf-3.6.2.jar) contains the original license notices. A copy of the [license text](licenses/FlatLaf-Apache-2.0.txt) is included. |

## Independent Implementation

The Android and Linux viewers were written in Java for this project. References used were the [inMusic documentation](https://github.com/inmusic-software/mpc-remote-screen/tree/fdd9f08cf1e666dffc9b48a4df9223b8b85b400f) and the publicly readable [community specification of the Remote Screen protocol](https://github.com/simonas-notcat/mpc-remote-screen-viewer-gpui/blob/44ef1480870fc45316528546cb86d55b1195add5/SPEC.md). The Java code implements the documented message format and connection handling. The community specification and its Rust code are not part of the source package. inMusic software, firmware, and EULA are not included. The Android SDK tools and Java runtime are external build or runtime prerequisites.

The monitor icon with 16 red pads and the neutral cup were created specifically for MPCR. The editable source files are located in [common/assets](common/assets). Older app packages included a graphic from the official [Ko-fi Creator Kit](https://more.ko-fi.com/brand-assets). This graphic is replaced by the custom cup in the MIT source package and in app packages starting with version 1.5.

## Trademarks

MPCR is an independent, unofficial project. The names Akai Professional, MPC, and inMusic designate compatible devices, features, and their providers. Trademarks belong to their respective owners. MPCR is neither published by nor affiliated with Akai Professional or inMusic. Ko-fi designates the external service to which the support link leads.
