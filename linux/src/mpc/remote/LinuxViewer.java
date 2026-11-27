package mpc.remote;

import java.awt.AWTError;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.datatransfer.StringSelection;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BorderFactory;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import com.formdev.flatlaf.FlatDarkLaf;

public final class LinuxViewer extends JFrame {
    private static final Color BACKGROUND = new Color(0x19191e);
    private static final Color PAPER = new Color(0xf3f1f3);
    private static final Color ACCENT = new Color(0xff5962);
    private static final int MARK_LINGER_MS = 350;
    private static final int MARK_PREVIEW_MS = 1200;
    private static final Color MARK_FILL = new Color((ACCENT.getRGB() & 0xffffff) | 0x2d000000, true);
    private static final Color MARK_HATCH = new Color((ACCENT.getRGB() & 0xffffff) | 0x5a000000, true);
    private static final Color MARK_OUTLINE = new Color((ACCENT.getRGB() & 0xffffff) | 0xd2000000, true);
    private static final Color BLACK = new Color(0x101014);
    private static final Color INPUT_BACKGROUND = new Color(0x26262e);
    private static final Color INPUT_BORDER = new Color(0x575761);
    private static final URI KO_FI = URI.create("https://ko-fi.com/paulweigl/tip");
    private static final URI IMPRINT = URI.create("https://www.paulweigl.de/impressum-1042906.html");

    private final Path deviceFile = savedDeviceFile();
    private SavedDevices devices = readDevices();
    private String lastAddress;
    private final JLabel status = new JLabel("Ready to connect");
    private final JButton connect = new JButton("Connect");
    private final JCheckBox control = new JCheckBox("Control");
    private final JCheckBox inputMarks = new JCheckBox("Marks");
    private final JCheckBox audio = new JCheckBox("Audio");
    private final JButton koFiButton = new JButton();
    private final JToggleButton nativeButton = new JToggleButton("1:1");
    private final JButton recordingButton = new JButton("OBS Output");
    private final JButton windowButton = new JButton("Window");
    private final Display display = new Display();
    private final JPanel root = new JPanel(new BorderLayout(0, 10));
    private final JPanel toolbar = new JPanel(new BorderLayout());
    private GraphicsDevice fullScreenDevice;
    private int windowState;
    private int decorationStyle;
    private RemoteSession session;
    private boolean interactive;
    private boolean soundAvailable;
    private RecordingOutput recordingOutput;
    private Dimension sizeBeforeRecording;
    private int stateBeforeRecording;

    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && "--help".equals(args[0]))) {
            System.out.println("Usage: mpcr-screen [MPC-address[:port] | --diagnose | --version]");
            return;
        }
        if (args.length == 1 && "--version".equals(args[0])) {
            System.out.println("MPCR-Screen " + LinuxViewer.class.getPackage().getImplementationVersion());
            return;
        }

        boolean diagnose = args.length == 1 && "--diagnose".equals(args[0]);
        if (!checkDesktop()) {
            System.exit(1);
        }
        if (diagnose) {
            System.out.println("Java graphics and X11 display are available.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                setupAppearance();
                LinuxViewer viewer = new LinuxViewer(args.length == 0 ? "" : args[0]);
                viewer.setVisible(true);
                viewer.showLogin();
            } catch (RuntimeException | LinkageError | AWTError error) {
                System.err.println("Could not open the viewer window.");
                error.printStackTrace(System.err);
                System.exit(1);
            }
        });
    }

    private static boolean checkDesktop() {
        if (GraphicsEnvironment.isHeadless() && (System.getenv("DISPLAY") == null
                || System.getenv("DISPLAY").isEmpty())) {
            System.err.println("No X11 display found. Start the viewer in a desktop session."
                    + " XWayland is required under Wayland.");
            return false;
        }
        try {
            // Opening a display verifies native AWT and its system libraries before showing the window.
            Toolkit.getDefaultToolkit().getScreenSize();
            return true;
        } catch (HeadlessException | LinkageError | AWTError error) {
            System.err.println("Java could not open the display."
                    + " Check the full OpenJDK runtime and access to X11 or XWayland.");
            System.err.println(error);
            return false;
        }
    }

    private static void setupAppearance() {
        FlatDarkLaf.setup();
        try (InputStream file = LinuxViewer.class.getResourceAsStream("/assets/Barlow-Regular.ttf")) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(15f);
            UIManager.put("defaultFont", new FontUIResource(font));
        } catch (IOException | java.awt.FontFormatException e) {
            System.err.println("Could not load Barlow font: " + e.getMessage());
        }
        UIManager.put("Component.arc", 12);
        UIManager.put("Component.borderColor", INPUT_BORDER);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.focusedBorderColor", ACCENT);
        UIManager.put("Button.arc", 12);
        UIManager.put("TitlePane.background", new Color(0x222228));
        UIManager.put("TitlePane.buttonBackground", new Color(0x383841));
        UIManager.put("TitlePane.buttonHoverBackground", new Color(0x50505a));
        UIManager.put("TitlePane.closeHoverBackground", new Color(0xbe303b));
        UIManager.put("TitlePane.centerTitle", true);
        JFrame.setDefaultLookAndFeelDecorated(true);
    }

    private LinuxViewer(String host) {
        super("MPCR-Screen · Unofficial Viewer");
        setIconImage(new ImageIcon(getClass().getResource("/assets/mpcr-screen.png")).getImage());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        root.setBackground(BACKGROUND);
        setContentPane(root);
        root.add(display, BorderLayout.CENTER);

        toolbar.setOpaque(false);
        status.setForeground(PAPER);
        toolbar.add(status, BorderLayout.CENTER);
        ImageIcon cup = new ImageIcon(getClass().getResource("/assets/support.png"));
        koFiButton.setIcon(new ImageIcon(cup.getImage().getScaledInstance(22, 18, java.awt.Image.SCALE_SMOOTH)));
        koFiButton.setToolTipText("Support on Ko-fi");
        koFiButton.getAccessibleContext().setAccessibleName("Support on Ko-fi");
        koFiButton.putClientProperty("JButton.buttonType", "toolBarButton");
        koFiButton.addActionListener(e -> openKoFi());
        JPanel options = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        options.setOpaque(false);
        connect.setBackground(ACCENT);
        connect.setForeground(BLACK);
        options.add(connect);
        inputMarks.setToolTipText("Mark clicks and taps made in this viewer");
        inputMarks.addActionListener(e -> {
            if (inputMarks.isSelected()) display.previewMarker();
            else display.clearMarker();
        });
        for (JCheckBox box : new JCheckBox[] {control, inputMarks, audio}) {
            box.setOpaque(false);
            box.setForeground(PAPER);
            options.add(box);
        }
        JButton fullscreen = new JButton("Fullscreen");
        fullscreen.addActionListener(e -> setFullscreen(true));
        options.add(fullscreen);
        nativeButton.setToolTipText("Native resolution");
        nativeButton.getAccessibleContext().setAccessibleName("Native resolution");
        nativeButton.addActionListener(e -> setNativeResolution());
        options.add(nativeButton);
        recordingButton.addActionListener(e -> toggleRecordingOutput());
        options.add(recordingButton);
        JButton privacy = new JButton("Privacy");
        privacy.addActionListener(e -> showPrivacy());
        options.add(privacy);
        options.add(koFiButton);
        toolbar.add(options, BorderLayout.EAST);
        root.add(toolbar, BorderLayout.SOUTH);
        windowButton.addActionListener(e -> setFullscreen(false));
        windowButton.setVisible(false);
        display.add(windowButton);
        bindFullscreen("F11", "toggleFullscreen", () -> setFullscreen(fullScreenDevice == null));
        bindFullscreen("ESCAPE", "exitFullscreen", () -> setFullscreen(false));

        lastAddress = host;
        connect.addActionListener(e -> {
            if (session == null) showLogin();
            else disconnect("Disconnected");
        });
        control.addActionListener(e -> {
            if (!control.isSelected()) display.cancelGesture();
        });
        audio.addActionListener(e -> {
            if (session != null && soundAvailable) session.setAudio(audio.isSelected());
        });
        addWindowFocusListener(new WindowAdapter() {
            @Override public void windowLostFocus(WindowEvent e) { display.cancelGesture(); }
        });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { disconnect("Disconnected"); }
        });
        updateControls();
        setMinimumSize(new Dimension(800, 300));
        setSize(980, 600);
        setLocationByPlatform(true);
    }

    private void bindFullscreen(String key, String name, Runnable action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }

    private void setFullscreen(boolean enabled) {
        if (enabled == (fullScreenDevice != null)) return;
        display.cancelGesture();
        if (enabled) {
            windowState = getExtendedState();
            decorationStyle = getRootPane().getWindowDecorationStyle();
            getRootPane().setWindowDecorationStyle(JRootPane.NONE);
            fullScreenDevice = getGraphicsConfiguration().getDevice();
            fullScreenDevice.setFullScreenWindow(this);
        } else {
            fullScreenDevice.setFullScreenWindow(null);
            fullScreenDevice = null;
            getRootPane().setWindowDecorationStyle(decorationStyle);
            setExtendedState(windowState);
        }
        root.setBorder(BorderFactory.createEmptyBorder(enabled ? 0 : 10, enabled ? 0 : 12,
                enabled ? 0 : 10, enabled ? 0 : 12));
        toolbar.setVisible(!enabled);
        windowButton.setVisible(enabled);
        root.revalidate();
        root.repaint();
    }

    private void showPrivacy() {
        try (Scanner input = new Scanner(getClass().getResourceAsStream("/assets/privacy-en.txt"), "UTF-8")) {
            JTextArea text = new JTextArea(input.useDelimiter("\\A").next(), 15, 54);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setCaretPosition(0);
            Object[] buttons = {"Close", "Legal Notice"};
            int selection = JOptionPane.showOptionDialog(this, new JScrollPane(text), "Privacy",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, buttons, buttons[0]);
            if (selection == 1) openBrowser(IMPRINT);
        }
    }

    private void openKoFi() {
        openBrowser(KO_FI);
    }

    private void openBrowser(URI address) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(address);
            } else {
                new ProcessBuilder("xdg-open", address.toString()).start();
            }
        } catch (IOException | UnsupportedOperationException error) {
            JOptionPane.showMessageDialog(this, "Could not open the browser.\n" + address,
                    "External Link", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static Path savedDeviceFile() {
        String configHome = System.getenv("XDG_CONFIG_HOME");
        Path config = configHome == null ? Paths.get("") : Paths.get(configHome);
        if (!config.isAbsolute()) config = Paths.get(System.getProperty("user.home"), ".config");
        return config.resolve("mpcr-screen").resolve("devices.properties");
    }

    private SavedDevices readDevices() {
        try {
            return SavedDevices.read(new String(Files.readAllBytes(deviceFile), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new SavedDevices();
        }
    }

    private boolean storeDevices(JDialog dialog, SavedDevices updated) {
        try {
            Path directory = Files.createDirectories(deviceFile.getParent());
            Path staged = Files.createTempFile(directory, "devices-", ".properties");
            try {
                Files.write(staged, updated.serialize().getBytes(StandardCharsets.UTF_8));
                Files.move(staged, deviceFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(staged);
            }
            devices = updated;
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(dialog, "Could not save the device list.",
                    "Saved devices", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void showLogin() {
        JDialog dialog = new JDialog(this, "Connect to MPC", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        panel.setBackground(BACKGROUND);
        dialog.setContentPane(panel);

        JTextField address = new JTextField(lastAddress, 22);
        JButton select = new JButton(new FolderIcon());
        select.setToolTipText("Saved devices");
        select.getAccessibleContext().setAccessibleName("Saved devices");
        JPasswordField password = new JPasswordField(22);
        char mask = password.getEchoChar();
        JToggleButton eye = new JToggleButton(new EyeIcon(false));
        eye.setSelectedIcon(new EyeIcon(true));
        select.setPreferredSize(eye.getPreferredSize());
        setPasswordVisible(password, eye, mask, false);
        eye.addActionListener(e -> setPasswordVisible(password, eye, mask, eye.isSelected()));
        JCheckBox save = new JCheckBox("Save Device");
        JTextField name = new JTextField(22);
        JPanel deviceName = new JPanel(new BorderLayout(0, 6));
        deviceName.setOpaque(false);
        deviceName.add(label("Device Name", name), BorderLayout.NORTH);
        deviceName.add(inputRow(name, null), BorderLayout.CENTER);
        deviceName.setVisible(false);
        save.addItemListener(e -> {
            deviceName.setVisible(save.isSelected());
            dialog.pack();
        });

        GridBagConstraints row = new GridBagConstraints();
        row.gridx = 0;
        row.gridy = 0;
        row.weightx = 1;
        row.fill = GridBagConstraints.HORIZONTAL;
        row.insets = new Insets(0, 0, 6, 0);
        panel.add(label("MPC Address", address), row);
        row.gridy++;
        JPanel addressRow = inputRow(address, select);
        panel.add(addressRow, row);
        row.gridy++;
        panel.add(label("Remote Screen Password", password), row);
        row.gridy++;
        panel.add(inputRow(password, eye), row);
        row.gridy++;
        panel.add(save, row);
        row.gridy++;
        panel.add(deviceName, row);
        row.gridy++;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        JButton submit = new JButton("Connect");
        submit.setBackground(ACCENT);
        submit.setForeground(BLACK);
        buttons.add(cancel);
        buttons.add(submit);
        panel.add(buttons, row);

        select.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            java.util.List<SavedDevices.Device> saved = devices.list();
            if (saved.isEmpty()) menu.add(new JMenuItem("No saved devices")).setEnabled(false);
            for (SavedDevices.Device device : saved) {
                menu.add(device.label()).addActionListener(choice -> {
                    address.setText(device.address);
                    name.setText(device.name);
                    save.setSelected(false);
                    password.setText("");
                    setPasswordVisible(password, eye, mask, false);
                    password.requestFocusInWindow();
                });
            }
            if (!saved.isEmpty()) {
                menu.addSeparator();
                menu.add("Clear saved devices…").addActionListener(choice -> {
                    int confirm = JOptionPane.showConfirmDialog(dialog,
                            "Remove all device names and addresses saved on this computer?",
                            "Clear saved devices?", JOptionPane.OK_CANCEL_OPTION);
                    if (confirm == JOptionPane.OK_OPTION && storeDevices(dialog, new SavedDevices())) {
                        lastAddress = "";
                        address.setText("");
                        name.setText("");
                        save.setSelected(false);
                    }
                });
            }
            menu.setPopupSize(addressRow.getWidth(), menu.getPreferredSize().height);
            menu.show(addressRow, 0, addressRow.getHeight());
        });
        cancel.addActionListener(e -> dialog.dispose());
        submit.addActionListener(e -> {
            String enteredAddress = address.getText().trim();
            Endpoint endpoint;
            try {
                endpoint = Endpoint.parse(enteredAddress);
            } catch (IllegalArgumentException error) {
                JOptionPane.showMessageDialog(dialog, error.getMessage(), "MPC Address", JOptionPane.WARNING_MESSAGE);
                address.requestFocusInWindow();
                return;
            }
            if (save.isSelected() && !storeDevices(dialog, devices.with(enteredAddress, name.getText()))) return;
            lastAddress = enteredAddress;
            char[] secret = password.getPassword();
            try {
                password.setText("");
                dialog.dispose();
                open(endpoint, new String(secret));
            } finally {
                Arrays.fill(secret, '\0');
            }
        });
        address.addActionListener(e -> password.requestFocusInWindow());
        dialog.getRootPane().setDefaultButton(submit);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        password.setText("");
    }

    private static JPanel inputRow(JTextField field, JComponent accessory) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setOpaque(false);
        styleInput(field);
        panel.add(field, BorderLayout.CENTER);
        if (accessory != null) panel.add(accessory, BorderLayout.EAST);
        return panel;
    }

    private static void setPasswordVisible(JPasswordField password, JToggleButton eye, char mask, boolean visible) {
        password.setEchoChar(visible ? (char) 0 : mask);
        eye.setSelected(visible);
        String label = visible ? "Hide password" : "Show password";
        eye.setToolTipText(label);
        eye.getAccessibleContext().setAccessibleName(label);
    }

    private static final class FolderIcon implements Icon {
        @Override public int getIconWidth() { return 24; }
        @Override public int getIconHeight() { return 24; }

        @Override public void paintIcon(java.awt.Component component, Graphics g, int x, int y) {
            Graphics2D canvas = (Graphics2D) g.create();
            canvas.translate(x, y);
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setColor(PAPER);
            canvas.setStroke(new java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            Path2D folder = new Path2D.Float();
            folder.moveTo(3, 5);
            folder.lineTo(9, 5);
            folder.lineTo(11, 7);
            folder.lineTo(21, 7);
            folder.lineTo(21, 19);
            folder.lineTo(3, 19);
            folder.closePath();
            canvas.draw(folder);
            canvas.dispose();
        }
    }

    private static final class EyeIcon implements Icon {
        private final boolean visible;

        EyeIcon(boolean visible) { this.visible = visible; }

        @Override public int getIconWidth() { return 24; }
        @Override public int getIconHeight() { return 24; }

        @Override public void paintIcon(java.awt.Component component, Graphics g, int x, int y) {
            Graphics2D canvas = (Graphics2D) g.create();
            canvas.translate(x, y);
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setColor(PAPER);
            canvas.setStroke(new java.awt.BasicStroke(1.8f));
            Path2D eye = new Path2D.Float();
            eye.moveTo(2, 12);
            eye.quadTo(12, -1, 22, 12);
            eye.quadTo(12, 25, 2, 12);
            canvas.draw(eye);
            canvas.drawOval(9, 9, 6, 6);
            if (visible) canvas.drawLine(4, 4, 20, 20);
            canvas.dispose();
        }
    }

    private static void styleInput(JTextField input) {
        input.setFont(input.getFont().deriveFont(18f));
        input.setBackground(INPUT_BACKGROUND);
        input.setForeground(PAPER);
        input.setCaretColor(PAPER);
        input.setMargin(new Insets(10, 12, 10, 12));
    }

    private static JLabel label(String value, JTextField input) {
        JLabel label = new JLabel(value);
        label.setLabelFor(input);
        label.setForeground(PAPER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        return label;
    }

    private void open(Endpoint endpoint, String secret) {
        class ConnectionListener implements RemoteSession.Listener {
            RemoteSession owner;

            @Override public void status(String text) {
                SwingUtilities.invokeLater(() -> {
                    if (session == owner) status.setText(text);
                });
            }

            @Override public void screen(ScreenBuffer screen) {
                SwingUtilities.invokeLater(() -> {
                    if (session != owner) return;
                    display.cancelGesture();
                    interactive = (screen.controls & 3) != 0;
                    soundAvailable = screen.audio;
                    updateControls();
                    display.repaint();
                });
            }

            @Override public void frame() {
                display.presentFrame();
            }

            @Override public void ended(String reason) {
                SwingUtilities.invokeLater(() -> {
                    if (session == owner) disconnect(reason);
                });
            }

            void launch() {
                owner = new RemoteSession(endpoint.host, endpoint.port, this,
                        LinuxViewer::decodeJpeg, new PcmPlayer(LinuxViewer::openAudio));
                session = owner;
                updateControls();
                owner.start(secret);
            }
        }
        new ConnectionListener().launch();
    }

    private void updateControls() {
        boolean active = session != null;
        connect.setText(active ? "Disconnect" : "Connect");
        control.setEnabled(active && interactive);
        if (!control.isEnabled()) control.setSelected(false);
        recordingButton.setEnabled(active);
        audio.setEnabled(active && soundAvailable);
        if (!audio.isEnabled()) audio.setSelected(false);
    }

    private void disconnect(String reason) {
        stopRecordingOutput();
        display.cancelGesture();
        if (session != null) session.close();
        session = null;
        interactive = false;
        soundAvailable = false;
        status.setText(reason);
        updateControls();
        display.repaint();
    }

    private void setNativeResolution() {
        display.cancelGesture();
        ScreenBuffer frame = session == null ? null : session.latestScreen();
        if (nativeButton.isSelected() && frame != null && fullScreenDevice == null
                && (getExtendedState() & JFrame.MAXIMIZED_BOTH) == 0) {
            java.awt.GraphicsConfiguration screen = getGraphicsConfiguration();
            AffineTransform scale = screen.getDefaultTransform();
            Rectangle available = screen.getBounds();
            Insets reserved = Toolkit.getDefaultToolkit().getScreenInsets(screen);
            available.width -= reserved.left + reserved.right;
            available.height -= reserved.top + reserved.bottom;
            int width = (int) Math.ceil(frame.width / scale.getScaleX()) + getWidth() - display.getWidth();
            int height = (int) Math.ceil(frame.height / scale.getScaleY()) + getHeight() - display.getHeight();
            setSize(Math.min(available.width, Math.max(width, getMinimumSize().width)),
                    Math.min(available.height, Math.max(height, getMinimumSize().height)));
        }
        display.repaint();
    }

    private void toggleRecordingOutput() {
        if (recordingOutput != null) {
            stopRecordingOutput();
            return;
        }
        RemoteSession source = session;
        ScreenBuffer frame = source == null ? null : source.latestScreen();
        if (frame == null) {
            JOptionPane.showMessageDialog(this, "Wait until the MPC screen is visible.",
                    "OBS Output", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            recordingOutput = new RecordingOutput(source::latestScreen);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not start the local video output.",
                    "OBS Output", JOptionPane.ERROR_MESSAGE);
            return;
        }

        sizeBeforeRecording = getSize();
        stateBeforeRecording = getExtendedState();
        if ((stateBeforeRecording & JFrame.MAXIMIZED_BOTH) != 0) setExtendedState(JFrame.NORMAL);
        setSize(Math.min(getWidth(), 880), Math.min(getHeight(), 330));
        nativeButton.setEnabled(false);
        recordingButton.setText("Stop OBS");
        status.setToolTipText(recordingOutput.url());
        display.repaint();

        JTextField url = new JTextField(recordingOutput.url(), 28);
        url.setEditable(false);
        Object[] instructions = {"Add a Browser Source in OBS and paste this local URL.",
                url, "Source size: " + frame.width + " × " + frame.height + " pixels.",
                "Set the OBS canvas and output to that size for 1:1 recordings."};
        Object[] buttons = {"Copy URL", "Close"};
        int option = JOptionPane.showOptionDialog(this, instructions, "OBS Output", JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, buttons, buttons[0]);
        if (option == 0) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url.getText()), null);
        }
    }

    private void stopRecordingOutput() {
        if (recordingOutput == null) return;
        recordingOutput.close();
        recordingOutput = null;
        recordingButton.setText("OBS Output");
        status.setToolTipText(null);
        nativeButton.setEnabled(true);
        if (sizeBeforeRecording != null && fullScreenDevice == null) {
            setSize(sizeBeforeRecording);
            setExtendedState(stateBeforeRecording);
        }
        sizeBeforeRecording = null;
        display.repaint();
    }

    private static int[] decodeJpeg(byte[] data, int offset, int length, int w, int h) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");
        if (!readers.hasNext()) return null;
        ImageReader jpeg = readers.next();
        try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(data, offset, length))) {
            jpeg.setInput(input, true, true);
            if (jpeg.getWidth(0) != w || jpeg.getHeight(0) != h) return null;
            return jpeg.read(0).getRGB(0, 0, w, h, null, 0, w);
        } finally {
            jpeg.dispose();
        }
    }

    private static PcmPlayer.Output openAudio(int rate, int channels) throws Exception {
        AudioFormat format = new AudioFormat(rate, 16, channels, true, false);
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, (rate / 20) * channels * 2);
        line.start();
        return new PcmPlayer.Output() {
            @Override public void write(byte[] pcm) { line.write(pcm, 0, pcm.length); }
            @Override public void close() { line.close(); }
        };
    }

    private final class Display extends JPanel {
        private final Rectangle2D.Double destination = new Rectangle2D.Double();
        private final AtomicBoolean paintScheduled = new AtomicBoolean();
        private final Timer hideMarker = new Timer(MARK_LINGER_MS, e -> clearMarker());
        private final Ellipse2D.Float markerCircle = new Ellipse2D.Float();
        private final Line2D.Float markerStripe = new Line2D.Float();
        private boolean markerVisible;
        private boolean markerPreview;
        private ScreenBuffer drawn;
        private BufferedImage bitmap;
        private boolean pressed;
        private int lastX;
        private int lastY;

        Display() {
            setBackground(BLACK);
            setLayout(null);
            hideMarker.setRepeats(false);
            getAccessibleContext().setAccessibleName("MPC Screen");
            MouseAdapter pointer = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (session == null || drawn == null || !control.isSelected()
                            || !SwingUtilities.isLeftMouseButton(e) || !destination.contains(e.getPoint())) return;
                    pressed = true;
                    sendPointer(0, e);
                }

                @Override public void mouseDragged(MouseEvent e) {
                    if (pressed) sendPointer(1, e);
                }

                @Override public void mouseReleased(MouseEvent e) {
                    if (pressed && SwingUtilities.isLeftMouseButton(e)) {
                        sendPointer(2, e);
                        pressed = false;
                    }
                }
            };
            addMouseListener(pointer);
            addMouseMotionListener(pointer);
        }

        void presentFrame() {
            if (paintScheduled.compareAndSet(false, true)) {
                SwingUtilities.invokeLater(() -> {
                    paintScheduled.set(false);
                    if (!isShowing()) return;
                    paintImmediately(0, 0, getWidth(), getHeight());
                    // Flush the painted window to X11 instead of waiting for the toolkit's idle flush.
                    Toolkit.getDefaultToolkit().sync();
                });
            }
        }

        @Override public void doLayout() {
            Dimension size = windowButton.getPreferredSize();
            windowButton.setBounds(getWidth() - size.width - 12, 12, size.width, size.height);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            ScreenBuffer frame = session == null ? null : session.latestScreen();
            if (frame == null) {
                bitmap = null;
                drawn = null;
                destination.setRect(0, 0, 0, 0);
                if (markerPreview) {
                    Graphics2D canvas = (Graphics2D) g.create();
                    drawMarker(canvas, getWidth() / 2f, getHeight() / 2f);
                    canvas.dispose();
                }
                return;
            }
            boolean changedScreen = frame != drawn;
            if (bitmap == null || bitmap.getWidth() != frame.width || bitmap.getHeight() != frame.height) {
                bitmap = new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB);
            }
            int[] pixels = ((DataBufferInt) bitmap.getRaster().getDataBuffer()).getData();
            frame.copyTo(pixels, changedScreen);
            drawn = frame;
            Graphics2D canvas = (Graphics2D) g.create();
            boolean nativeResolution = nativeButton.isSelected() && recordingOutput == null;
            if (nativeResolution) {
                AffineTransform transform = canvas.getTransform();
                double sx = transform.getScaleX(), sy = transform.getScaleY();
                double width = frame.width / sx, height = frame.height / sy;
                double x = (Math.floor((getWidth() - width) * sx / 2 + transform.getTranslateX())
                        - transform.getTranslateX()) / sx;
                double y = (Math.floor((getHeight() - height) * sy / 2 + transform.getTranslateY())
                        - transform.getTranslateY()) / sy;
                destination.setRect(x, y, width, height);
                AffineTransform placement = AffineTransform.getTranslateInstance(x, y);
                placement.scale(1 / sx, 1 / sy);
                canvas.drawImage(bitmap, placement, null);
            } else {
                double scale = Math.min((double) getWidth() / frame.width, (double) getHeight() / frame.height);
                int width = (int) Math.round(frame.width * scale);
                int height = (int) Math.round(frame.height * scale);
                int x = (getWidth() - width) / 2, y = (getHeight() - height) / 2;
                destination.setRect(x, y, width, height);
                canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                canvas.drawImage(bitmap, x, y, width, height, null);
            }
            if (markerPreview) {
                drawMarker(canvas, (float) destination.getCenterX(), (float) destination.getCenterY());
            } else if (markerVisible) {
                float x = (float) (destination.x + (lastX + .5) * destination.width / frame.width);
                float y = (float) (destination.y + (lastY + .5) * destination.height / frame.height);
                drawMarker(canvas, x, y);
            }
            canvas.dispose();
        }

        private void drawMarker(Graphics2D canvas, float x, float y) {
            float radius = 10;
            markerCircle.setFrame(x - radius, y - radius, radius * 2, radius * 2);
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!destination.isEmpty()) canvas.clip(destination);
            canvas.setColor(MARK_FILL);
            canvas.fill(markerCircle);

            Shape imageClip = canvas.getClip();
            canvas.clip(markerCircle);
            canvas.setColor(MARK_HATCH);
            for (int stripe = -2; stripe <= 2; stripe++) {
                float shift = stripe * radius / 2;
                markerStripe.setLine(x - radius + shift, y + radius, x + radius + shift, y - radius);
                canvas.draw(markerStripe);
            }
            canvas.setClip(imageClip);
            canvas.setColor(MARK_OUTLINE);
            canvas.draw(markerCircle);
        }

        private void sendPointer(int phase, MouseEvent e) {
            if (session == null || drawn == null || destination.isEmpty()) return;
            int x = Math.max(0, Math.min(drawn.width - 1, (int) ((e.getX() - destination.x) * drawn.width / destination.width)));
            int y = Math.max(0, Math.min(drawn.height - 1, (int) ((e.getY() - destination.y) * drawn.height / destination.height)));
            if (phase == 1 && x == lastX && y == lastY) return;
            session.pointer(phase, x, y);
            lastX = x;
            lastY = y;
            if (inputMarks.isSelected()) {
                if (phase == 0 || markerPreview) hideMarker.stop();
                markerPreview = false;
                if (phase == 2) {
                    hideMarker.setInitialDelay(MARK_LINGER_MS);
                    hideMarker.restart();
                }
                markerVisible = true;
                repaint();
            }
        }

        void cancelGesture() {
            if (pressed && session != null) session.releaseInput();
            pressed = false;
            clearMarker();
        }

        void previewMarker() {
            markerPreview = true;
            markerVisible = true;
            hideMarker.setInitialDelay(MARK_PREVIEW_MS);
            hideMarker.restart();
            repaint();
        }

        void clearMarker() {
            hideMarker.stop();
            markerPreview = false;
            if (markerVisible) {
                markerVisible = false;
                repaint();
            }
        }
    }
}
