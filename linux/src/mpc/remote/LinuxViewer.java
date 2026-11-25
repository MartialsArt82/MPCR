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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import com.formdev.flatlaf.FlatDarkLaf;

public final class LinuxViewer extends JFrame {
    private static final Color BACKGROUND = new Color(0x19191e);
    private static final Color PAPER = new Color(0xf3f1f3);
    private static final Color ACCENT = new Color(0xff5962);
    private static final Color BLACK = new Color(0x101014);
    private static final Color INPUT_BACKGROUND = new Color(0x26262e);
    private static final Color INPUT_BORDER = new Color(0x575761);
    private static final URI KO_FI = URI.create("https://ko-fi.com/paulweigl/tip");
    private static final URI IMPRINT = URI.create("https://www.paulweigl.de/impressum-1042906.html");

    private final JTextField address = new JTextField(16);
    private final JPasswordField password = new JPasswordField(13);
    private final JLabel status = new JLabel("MPC-Adresse und Remote-Screen-Passwort eingeben.");
    private final JButton connect = new JButton("Verbinden");
    private final JCheckBox control = new JCheckBox("Steuerung");
    private final JCheckBox audio = new JCheckBox("Audio");
    private final JButton koFiButton = new JButton();
    private final JButton windowButton = new JButton("Fenster");
    private final Display display = new Display();
    private final JPanel root = new JPanel(new BorderLayout(0, 10));
    private final JPanel login;
    private final JPanel toolbar = new JPanel(new BorderLayout());
    private GraphicsDevice fullScreenDevice;
    private int windowState;
    private int decorationStyle;
    private RemoteSession session;
    private boolean interactive;
    private boolean soundAvailable;

    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && "--help".equals(args[0]))) {
            System.out.println("Aufruf: mpc-remote-screen [MPC-Adresse[:Port] | --diagnose | --version]");
            return;
        }
        if (args.length == 1 && "--version".equals(args[0])) {
            System.out.println("MPCR Viewer " + LinuxViewer.class.getPackage().getImplementationVersion());
            return;
        }

        boolean diagnose = args.length == 1 && "--diagnose".equals(args[0]);
        if (!checkDesktop()) {
            System.exit(1);
        }
        if (diagnose) {
            System.out.println("Java-Grafik und X11-Anzeige sind verfügbar.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                setupAppearance();
                LinuxViewer viewer = new LinuxViewer(args.length == 0 ? "" : args[0]);
                viewer.setVisible(true);
            } catch (RuntimeException | LinkageError | AWTError error) {
                System.err.println("Das Viewer-Fenster konnte nicht geöffnet werden.");
                error.printStackTrace(System.err);
                System.exit(1);
            }
        });
    }

    private static boolean checkDesktop() {
        if (GraphicsEnvironment.isHeadless() && (System.getenv("DISPLAY") == null
                || System.getenv("DISPLAY").isEmpty())) {
            System.err.println("Keine X11-Anzeige gefunden. Starte den Viewer in einer Desktop-Sitzung."
                    + " Unter Wayland wird XWayland benötigt.");
            return false;
        }
        try {
            // Opening a display verifies native AWT and its system libraries before showing the window.
            Toolkit.getDefaultToolkit().getScreenSize();
            return true;
        } catch (HeadlessException | LinkageError | AWTError error) {
            System.err.println("Java konnte die Anzeige nicht öffnen."
                    + " Prüfe die vollständige OpenJDK-Laufzeit und den Zugriff auf X11 oder XWayland.");
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
            System.err.println("Barlow konnte nicht geladen werden: " + e.getMessage());
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
        super("MPCR · Inoffizieller Viewer");
        setIconImage(new ImageIcon(getClass().getResource("/assets/mpc-remote-screen.png")).getImage());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        root.setBackground(BACKGROUND);
        setContentPane(root);
        login = loginPanel();
        root.add(login, BorderLayout.NORTH);
        root.add(display, BorderLayout.CENTER);

        toolbar.setOpaque(false);
        status.setForeground(PAPER);
        toolbar.add(status, BorderLayout.CENTER);
        ImageIcon cup = new ImageIcon(getClass().getResource("/assets/support.png"));
        koFiButton.setIcon(new ImageIcon(cup.getImage().getScaledInstance(22, 18, java.awt.Image.SCALE_SMOOTH)));
        koFiButton.setToolTipText("Auf Ko-fi unterstützen");
        koFiButton.getAccessibleContext().setAccessibleName("Auf Ko-fi unterstützen");
        koFiButton.putClientProperty("JButton.buttonType", "toolBarButton");
        koFiButton.addActionListener(e -> openKoFi());
        JPanel options = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        options.setOpaque(false);
        for (JCheckBox box : new JCheckBox[] {control, audio}) {
            box.setOpaque(false);
            box.setForeground(PAPER);
            options.add(box);
        }
        JButton fullscreen = new JButton("Vollbild");
        fullscreen.addActionListener(e -> setFullscreen(true));
        options.add(fullscreen);
        JButton privacy = new JButton("Datenschutz");
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

        address.setText(host);
        connect.addActionListener(e -> {
            if (session == null) open();
            else disconnect("Getrennt");
        });
        address.addActionListener(e -> password.requestFocusInWindow());
        password.addActionListener(e -> {
            if (session == null) open();
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
            @Override public void windowClosing(WindowEvent e) { disconnect("Getrennt"); }
        });
        updateControls();
        setMinimumSize(new Dimension(700, 330));
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
        login.setVisible(!enabled);
        toolbar.setVisible(!enabled);
        windowButton.setVisible(enabled);
        root.revalidate();
        root.repaint();
    }

    private void showPrivacy() {
        try (Scanner input = new Scanner(getClass().getResourceAsStream("/assets/privacy-de.txt"), "UTF-8")) {
            JTextArea text = new JTextArea(input.useDelimiter("\\A").next(), 15, 54);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setCaretPosition(0);
            Object[] buttons = {"Schließen", "Impressum"};
            int selection = JOptionPane.showOptionDialog(this, new JScrollPane(text), "Datenschutz",
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
            JOptionPane.showMessageDialog(this, "Der Browser konnte nicht geöffnet werden.\n" + address,
                    "Externer Link", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JPanel loginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 4));
        GridBagConstraints cell = new GridBagConstraints();
        cell.anchor = GridBagConstraints.LINE_START;
        cell.fill = GridBagConstraints.HORIZONTAL;
        cell.weightx = 1;
        cell.insets = new Insets(0, 0, 5, 12);
        cell.gridx = 0;
        cell.gridy = 0;
        panel.add(label("MPC-Adresse", address), cell);
        cell.gridx = 1;
        panel.add(label("Remote-Screen-Passwort", password), cell);
        cell.gridy = 1;
        cell.gridx = 0;
        cell.insets.bottom = 0;
        styleInput(address);
        panel.add(address, cell);
        cell.gridx = 1;
        styleInput(password);
        panel.add(password, cell);
        cell.gridx = 2;
        cell.weightx = 0;
        cell.insets.right = 0;
        connect.setBackground(ACCENT);
        connect.setForeground(BLACK);
        connect.setFont(connect.getFont().deriveFont(Font.BOLD, 16f));
        connect.setMargin(new Insets(9, 16, 9, 16));
        panel.add(connect, cell);
        return panel;
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

    private void open() {
        Endpoint endpoint;
        try {
            endpoint = Endpoint.parse(address.getText());
        } catch (IllegalArgumentException e) {
            status.setText(e.getMessage());
            return;
        }
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
                char[] secret = password.getPassword();
                try {
                    owner.start(new String(secret));
                } finally {
                    Arrays.fill(secret, '\0');
                    password.setText("");
                }
            }
        }
        new ConnectionListener().launch();
    }

    private void updateControls() {
        boolean active = session != null;
        connect.setText(active ? "Trennen" : "Verbinden");
        address.setEnabled(!active);
        password.setEnabled(!active);
        control.setEnabled(active && interactive);
        if (!control.isEnabled()) control.setSelected(false);
        audio.setEnabled(active && soundAvailable);
        if (!audio.isEnabled()) audio.setSelected(false);
    }

    private void disconnect(String reason) {
        display.cancelGesture();
        if (session != null) session.close();
        session = null;
        interactive = false;
        soundAvailable = false;
        status.setText(reason);
        updateControls();
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
        private final Rectangle destination = new Rectangle();
        private final AtomicBoolean paintScheduled = new AtomicBoolean();
        private ScreenBuffer drawn;
        private BufferedImage bitmap;
        private boolean pressed;
        private int lastX;
        private int lastY;

        Display() {
            setBackground(BLACK);
            setLayout(null);
            getAccessibleContext().setAccessibleName("MPC-Bildschirm");
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
                destination.setBounds(0, 0, 0, 0);
                return;
            }
            boolean changedScreen = frame != drawn;
            if (bitmap == null || bitmap.getWidth() != frame.width || bitmap.getHeight() != frame.height) {
                bitmap = new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB);
            }
            int[] pixels = ((DataBufferInt) bitmap.getRaster().getDataBuffer()).getData();
            frame.copyTo(pixels, changedScreen);
            drawn = frame;
            double scale = Math.min((double) getWidth() / frame.width, (double) getHeight() / frame.height);
            int width = (int) Math.round(frame.width * scale);
            int height = (int) Math.round(frame.height * scale);
            destination.setBounds((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
            Graphics2D canvas = (Graphics2D) g.create();
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            canvas.drawImage(bitmap, destination.x, destination.y, width, height, null);
            canvas.dispose();
        }

        private void sendPointer(int phase, MouseEvent e) {
            if (session == null || drawn == null || destination.isEmpty()) return;
            int x = Math.max(0, Math.min(drawn.width - 1, (e.getX() - destination.x) * drawn.width / destination.width));
            int y = Math.max(0, Math.min(drawn.height - 1, (e.getY() - destination.y) * drawn.height / destination.height));
            if (phase != 1 || x != lastX || y != lastY) session.pointer(phase, x, y);
            lastX = x;
            lastY = y;
        }

        void cancelGesture() {
            if (pressed && session != null) session.releaseInput();
            pressed = false;
        }
    }
}
