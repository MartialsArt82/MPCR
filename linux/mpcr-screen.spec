Name:           mpcr-screen
Version:        1.7.1
Release:        1
Summary:        Unofficial remote viewer for compatible MPC devices
License:        MIT AND Apache-2.0 AND OFL-1.1
BuildArch:      noarch
Source0:        mpcr-screen-linux.tar.gz
Requires:       java >= 1.8.0
Requires:       /bin/sh
Requires:       /usr/bin/readlink
Recommends:     xorg-x11-server-Xwayland
Recommends:     xdg-utils
Provides:       mpc-remote-screen = %{version}-%{release}
Obsoletes:      mpc-remote-screen < %{version}-%{release}
AutoReqProv:    no

%description
MPCR is an unofficial, independent remote screen viewer. It displays the
screen of a compatible MPC over the local network and supports remote pointer
input and PCM audio on devices that offer these features.

%prep
%setup -q -n mpcr-screen-linux

%install
install -Dm755 mpcr-screen "%{buildroot}%{_datadir}/%{name}/mpcr-screen"
install -Dm644 mpcr-screen-linux.jar "%{buildroot}%{_datadir}/%{name}/mpcr-screen-linux.jar"
install -Dm644 flatlaf.jar "%{buildroot}%{_datadir}/%{name}/flatlaf.jar"
install -Dm644 mpcr-screen.png "%{buildroot}%{_datadir}/icons/hicolor/256x256/apps/mpcr-screen.png"
install -d "%{buildroot}%{_bindir}" "%{buildroot}%{_datadir}/applications"
ln -s "../share/%{name}/mpcr-screen" "%{buildroot}%{_bindir}/mpcr-screen"
ln -s mpcr-screen "%{buildroot}%{_bindir}/mpc-remote-screen"
cat > "%{buildroot}%{_datadir}/applications/mpcr-screen.desktop" <<'EOF'
[Desktop Entry]
Type=Application
Name=MPCR-Screen
Comment=Unofficial remote viewer for compatible MPC devices
Exec=%{_bindir}/mpcr-screen --desktop
Icon=mpcr-screen
Terminal=false
Categories=AudioVideo;Audio;
EOF

%files
%{_bindir}/mpcr-screen
%{_bindir}/mpc-remote-screen
%{_datadir}/%{name}/
%{_datadir}/applications/mpcr-screen.desktop
%{_datadir}/icons/hicolor/256x256/apps/mpcr-screen.png
%doc README.md THIRD_PARTY.md PRIVACY.md IMPRESSUM.md
%license LICENSE Barlow-OFL.txt FlatLaf-Apache-2.0.txt
