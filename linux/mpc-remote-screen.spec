Name:           mpc-remote-screen
Version:        1.5
Release:        1
Summary:        Unofficial remote viewer for compatible MPC devices
License:        MIT AND Apache-2.0 AND OFL-1.1
BuildArch:      noarch
Source0:        mpc-remote-screen-linux.tar.gz
Requires:       java >= 1.8.0
Requires:       /bin/sh
Requires:       /usr/bin/readlink
Recommends:     xorg-x11-server-Xwayland
Recommends:     xdg-utils
AutoReqProv:    no

%description
MPCR is an unofficial, independent remote screen viewer. It displays the
screen of a compatible MPC over the local network and supports remote pointer
input and PCM audio on devices that offer these features.

%prep
%setup -q -n mpc-remote-screen-linux

%install
install -Dm755 mpc-remote-screen "%{buildroot}%{_datadir}/%{name}/mpc-remote-screen"
install -Dm644 mpc-remote-screen-linux.jar "%{buildroot}%{_datadir}/%{name}/mpc-remote-screen-linux.jar"
install -Dm644 flatlaf.jar "%{buildroot}%{_datadir}/%{name}/flatlaf.jar"
install -Dm644 mpc-remote-screen.png "%{buildroot}%{_datadir}/icons/hicolor/256x256/apps/mpc-remote-screen.png"
install -d "%{buildroot}%{_bindir}" "%{buildroot}%{_datadir}/applications"
ln -s "../share/%{name}/mpc-remote-screen" "%{buildroot}%{_bindir}/mpc-remote-screen"
cat > "%{buildroot}%{_datadir}/applications/mpc-remote-screen.desktop" <<'EOF'
[Desktop Entry]
Type=Application
Name=MPCR Viewer
Comment=Unofficial remote viewer for compatible MPC devices
Exec=%{_bindir}/mpc-remote-screen --desktop
Icon=mpc-remote-screen
Terminal=false
Categories=AudioVideo;Audio;
EOF

%files
%{_bindir}/mpc-remote-screen
%{_datadir}/%{name}/
%{_datadir}/applications/mpc-remote-screen.desktop
%{_datadir}/icons/hicolor/256x256/apps/mpc-remote-screen.png
%doc README.md THIRD_PARTY.md PRIVACY.md IMPRESSUM.md
%license LICENSE Barlow-OFL.txt FlatLaf-Apache-2.0.txt
