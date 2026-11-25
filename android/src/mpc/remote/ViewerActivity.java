package mpc.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import app.mpcremote.viewer.R;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ViewerActivity extends Activity {
    private static final int BACKGROUND = 0xff19191e;
    private static final int PAPER = 0xfff3f1f3;
    private static final int ACCENT = 0xffff5962;
    private static final int BLACK = 0xff101014;
    private static final int INPUT_BACKGROUND = 0xff26262e;
    private static final int INPUT_BORDER = 0xff575761;
    private static final int MUTED = 0xffc2bfc6;

    private LinearLayout content;
    private LinearLayout header;
    private ScrollView fields;
    private LinearLayout actions;
    private EditText address;
    private EditText password;
    private TextView status;
    private Button connect;
    private Button fullscreenButton;
    private ImageButton koFiButton;
    private ToggleButton control;
    private ToggleButton audio;
    private Display display;
    private volatile RemoteSession session;
    private boolean interactive;
    private boolean soundAvailable;
    private boolean fullscreen;
    private boolean screenReady;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        FrameLayout root = new FrameLayout(this);
        content = new LinearLayout(this);
        content.setBackgroundColor(BACKGROUND);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        header = new LinearLayout(this) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int buttonWidth = dp(48) * (koFiButton.getVisibility() == View.VISIBLE ? 2 : 1);
                int statusWidth = Math.max(0, MeasureSpec.getSize(widthSpec)
                        - getPaddingStart() - getPaddingEnd() - buttonWidth);
                if (status.getMaxWidth() != statusWidth) status.setMaxWidth(statusWidth);
                super.onMeasure(widthSpec, heightSpec);
            }
        };
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPaddingRelative(0, 0, dp(104), 0);
        content.addView(header, new LinearLayout.LayoutParams(-1, -2));
        status = new TextView(this);
        status.setTextColor(PAPER);
        status.setText("MPCR · Inoffizieller Viewer");
        status.setTextSize(17);
        status.setTypeface(getResources().getFont(R.font.barlow_semibold));
        status.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        status.setMinHeight(dp(48));
        status.setPaddingRelative(dp(4), dp(6), 0, dp(8));
        status.setMaxLines(2);
        status.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(status, new LinearLayout.LayoutParams(-2, -2));
        koFiButton = new ImageButton(this);
        koFiButton.setImageResource(R.drawable.support);
        koFiButton.setContentDescription("Auf Ko-fi unterstützen");
        koFiButton.setTooltipText("Auf Ko-fi unterstützen");
        koFiButton.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        koFiButton.setPadding(dp(11), dp(11), dp(11), dp(11));
        TypedValue selectable = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, selectable, true);
        koFiButton.setBackgroundResource(selectable.resourceId);
        koFiButton.setOnClickListener(v -> openKoFi());
        header.addView(koFiButton, new LinearLayout.LayoutParams(dp(48), dp(48)));
        header.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1));
        Button settings = new Button(this, null, android.R.attr.borderlessButtonStyle);
        settings.setText("⚙");
        settings.setTextSize(20);
        settings.setTypeface(Typeface.DEFAULT);
        settings.setPadding(0, 0, 0, 0);
        settings.setMinWidth(0);
        settings.setContentDescription("Einstellungen");
        settings.setTooltipText("Einstellungen");
        settings.setOnClickListener(v -> openSettings());
        header.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(48)));
        updateKoFiVisibility();

        FrameLayout viewer = new FrameLayout(this);
        content.addView(viewer, new LinearLayout.LayoutParams(-1, 0, 1));
        display = new Display(this);
        viewer.addView(display, new FrameLayout.LayoutParams(-1, -1));

        fields = new ScrollView(this);
        fields.setBackgroundColor(BACKGROUND);
        viewer.addView(fields, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        LinearLayout login = new LinearLayout(this);
        login.setOrientation(LinearLayout.VERTICAL);
        login.setPadding(dp(4), dp(8), dp(4), dp(16));
        fields.addView(login);
        address = field(login, "MPC-Adresse", "192.168.1.50",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, EditorInfo.IME_ACTION_NEXT);
        address.setText(getPreferences(MODE_PRIVATE).getString("address", ""));
        password = field(login, "Remote-Screen-Passwort", null,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, EditorInfo.IME_ACTION_DONE);
        password.setSaveEnabled(false);
        password.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        address.setNextFocusForwardId(password.getId());

        actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(actions);
        connect = new Button(this);
        connect.setText("Verbinden");
        connect.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        connect.setTextColor(BLACK);
        actions.addView(connect, new LinearLayout.LayoutParams(0, -2, 1.35f));
        control = toggle(actions, "Steuerung");
        audio = toggle(actions, "Audio");
        fullscreenButton = new Button(this);
        fullscreenButton.setAllCaps(false);
        FrameLayout.LayoutParams buttonPosition = new FrameLayout.LayoutParams(
                dp(100), dp(48), Gravity.TOP | Gravity.END);
        buttonPosition.topMargin = dp(6);
        buttonPosition.setMarginEnd(dp(12));
        root.addView(fullscreenButton, buttonPosition);
        fullscreenButton.setOnClickListener(v -> setFullscreen(!fullscreen));
        connect.setOnClickListener(v -> {
            if (session == null) open();
            else disconnect("Getrennt");
        });
        control.setOnCheckedChangeListener((button, on) -> {
            if (!on) display.cancelGesture();
        });
        audio.setOnCheckedChangeListener((button, on) -> {
            if (session != null && soundAvailable) session.setAudio(on);
        });
        password.setOnEditorActionListener((v, action, event) -> {
            if (session != null) return false;
            open();
            return true;
        });
        updateControls();
        setFullscreen(saved != null && saved.getBoolean("fullscreen"));
    }

    private void setFullscreen(boolean enabled) {
        fullscreen = enabled;
        display.cancelGesture();
        header.setVisibility(enabled ? View.GONE : View.VISIBLE);
        fields.setVisibility(enabled || screenReady ? View.GONE : View.VISIBLE);
        actions.setVisibility(enabled ? View.GONE : View.VISIBLE);
        content.setPadding(enabled ? 0 : dp(12), enabled ? 0 : dp(6),
                enabled ? 0 : dp(12), enabled ? 0 : dp(6));
        fullscreenButton.setText(enabled ? "Fenster" : "Vollbild");
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                if (enabled) controller.hide(WindowInsets.Type.systemBars());
                else controller.show(WindowInsets.Type.systemBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(enabled
                    ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    : 0);
        }
    }

    @Override public void onBackPressed() {
        if (fullscreen) setFullscreen(false);
        else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("fullscreen", fullscreen);
        super.onSaveInstanceState(state);
    }

    private EditText field(LinearLayout login, String name, String hint, int type, int imeAction) {
        EditText field = new EditText(this);
        field.setId(View.generateViewId());
        field.setSingleLine(true);
        field.setInputType(type);
        field.setTypeface(getResources().getFont(R.font.barlow_regular));
        field.setImeOptions(imeAction | EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        field.setTextSize(20);
        field.setTextColor(PAPER);
        field.setHintTextColor(MUTED);
        field.setMinHeight(dp(60));
        field.setHint(hint);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[] {android.R.attr.state_focused}, inputBackground(ACCENT, 2));
        background.addState(new int[0], inputBackground(INPUT_BORDER, 1));
        field.setBackground(background);
        field.setPaddingRelative(dp(14), dp(12), dp(14), dp(12));
        field.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

        TextView label = new TextView(this);
        label.setText(name);
        label.setTextColor(PAPER);
        label.setTextSize(15);
        label.setLabelFor(field.getId());
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.bottomMargin = dp(5);
        login.addView(label, labelParams);
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(-1, -2);
        if (imeAction == EditorInfo.IME_ACTION_NEXT) fieldParams.bottomMargin = dp(14);
        login.addView(field, fieldParams);
        return field;
    }

    private void updateKoFiVisibility() {
        koFiButton.setVisibility(getPreferences(MODE_PRIVATE).getBoolean("showKoFi", true)
                ? View.VISIBLE : View.GONE);
    }

    private void openSettings() {
        boolean showKoFi = getPreferences(MODE_PRIVATE).getBoolean("showKoFi", true);
        new AlertDialog.Builder(this)
                .setTitle("Einstellungen")
                .setMultiChoiceItems(new String[] {"Ko-fi neben dem Status anzeigen"},
                        new boolean[] {showKoFi}, (dialog, item, checked) -> {
                            getPreferences(MODE_PRIVATE).edit().putBoolean("showKoFi", checked).apply();
                            updateKoFiVisibility();
                        })
                .setNeutralButton("Ko-fi öffnen", (dialog, which) -> openKoFi())
                .setNegativeButton("Datenschutz", (dialog, which) -> showPrivacy())
                .setPositiveButton("Fertig", null)
                .show();
    }

    private void showPrivacy() {
        try (Scanner input = new Scanner(getAssets().open("privacy-de.txt"), "UTF-8")) {
            new AlertDialog.Builder(this)
                    .setTitle("Datenschutz")
                    .setMessage(input.useDelimiter("\\A").next())
                    .setNeutralButton("Adresse vergessen", (dialog, which) -> {
                        getPreferences(MODE_PRIVATE).edit().remove("address").apply();
                        if (session == null) address.setText("");
                    })
                    .setPositiveButton("Fertig", null)
                    .show();
        } catch (IOException e) {
            Toast.makeText(this, "Datenschutzhinweis konnte nicht geladen werden.", Toast.LENGTH_LONG).show();
        }
    }

    private void openKoFi() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/paulweigl/tip")));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Kein Browser zum Öffnen von Ko-fi gefunden.", Toast.LENGTH_LONG).show();
        }
    }

    private GradientDrawable inputBackground(int strokeColor, int strokeWidth) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(INPUT_BACKGROUND);
        background.setCornerRadius(dp(10));
        background.setStroke(dp(strokeWidth), strokeColor);
        return background;
    }

    private ToggleButton toggle(LinearLayout parent, String label) {
        ToggleButton toggle = new ToggleButton(this);
        toggle.setTextOn(label + " an");
        toggle.setTextOff(label + " aus");
        toggle.setChecked(false);
        toggle.setText(label + " aus");
        toggle.setAllCaps(false);
        parent.addView(toggle, new LinearLayout.LayoutParams(0, -2, 1));
        return toggle;
    }

    private void open() {
        Endpoint endpoint;
        try {
            endpoint = Endpoint.parse(address.getText().toString());
        } catch (IllegalArgumentException e) {
            status.setText(e.getMessage());
            return;
        }
        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(password.getWindowToken(), 0);
        getPreferences(MODE_PRIVATE).edit().putString("address", address.getText().toString().trim()).apply();
        ConnectionListener listener = new ConnectionListener();
        session = new RemoteSession(endpoint.host, endpoint.port, listener,
                ViewerActivity::decodeJpeg, new PcmPlayer(ViewerActivity::openAudio));
        listener.owner = session;
        session.start(password.getText().toString());
        password.setText("");
        updateControls();
    }

    private final class ConnectionListener implements RemoteSession.Listener {
        RemoteSession owner;

        @Override public void status(String text) {
            runOnUiThread(() -> {
                if (session == owner) status.setText(text);
            });
        }

        @Override public void screen(ScreenBuffer screen) {
            runOnUiThread(() -> {
                if (session != owner) return;
                display.cancelGesture();
                screenReady = true;
                fields.setVisibility(View.GONE);
                interactive = (screen.controls & 3) != 0;
                soundAvailable = screen.audio;
                updateControls();
                display.invalidate();
            });
        }

        @Override public void frame() {
            if (session == owner) display.presentFrame();
        }

        @Override public void ended(String reason) {
            runOnUiThread(() -> {
                if (session == owner) disconnect(reason);
            });
        }
    }

    private void updateControls() {
        boolean active = session != null;
        connect.setText(active ? "Trennen" : "Verbinden");
        address.setEnabled(!active);
        password.setEnabled(!active);
        control.setEnabled(active && interactive);
        if (!control.isEnabled()) control.setChecked(false);
        audio.setEnabled(active && soundAvailable);
        if (!audio.isEnabled()) audio.setChecked(false);
        if (active) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void disconnect(String reason) {
        display.cancelGesture();
        RemoteSession previous = session;
        session = null;
        if (previous != null) previous.close();
        interactive = false;
        soundAvailable = false;
        screenReady = false;
        setFullscreen(false);
        status.setText(reason);
        updateControls();
        display.invalidate();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (!focused && display != null) display.cancelGesture();
    }

    @Override protected void onStop() {
        super.onStop();
        if (session != null) disconnect("Getrennt");
    }

    private int dp(float pixels) {
        return Math.round(pixels * getResources().getDisplayMetrics().density);
    }

    private static int[] decodeJpeg(byte[] data, int offset, int length, int w, int h) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, offset, length, options);
        if (options.outWidth != w || options.outHeight != h || !"image/jpeg".equals(options.outMimeType)) return null;
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, offset, length, options);
        if (bitmap == null) return null;
        try {
            int[] colors = new int[w * h];
            bitmap.getPixels(colors, 0, w, 0, 0, w, h);
            return colors;
        } finally {
            bitmap.recycle();
        }
    }

    private static PcmPlayer.Output openAudio(int rate, int channels) {
        int channelMask = channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int encoding = AudioFormat.ENCODING_PCM_16BIT;
        int minSize = AudioTrack.getMinBufferSize(rate, channelMask, encoding);
        if (minSize <= 0) throw new IllegalArgumentException("Audioformat");
        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(encoding).setSampleRate(rate)
                        .setChannelMask(channelMask).build())
                .setBufferSizeInBytes(Math.max(minSize, rate * channels / 10))
                .setTransferMode(AudioTrack.MODE_STREAM).build();
        try {
            track.play();
        } catch (RuntimeException e) {
            track.release();
            throw e;
        }
        return new PcmPlayer.Output() {
            @Override public void write(byte[] pcm) {
                track.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING);
            }

            @Override public void close() {
                track.release();
            }
        };
    }

    private final class Display extends View {
        private final RectF destination = new RectF();
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final AtomicBoolean invalidatePending = new AtomicBoolean();
        private ScreenBuffer drawn;
        private Bitmap bitmap;
        private int contact = -1;
        private int lastX;
        private int lastY;

        Display(Context context) {
            super(context);
            setBackgroundColor(BLACK);
            setContentDescription("MPC-Bildschirm");
        }

        void presentFrame() {
            if (invalidatePending.compareAndSet(false, true)) {
                post(() -> {
                    invalidatePending.set(false);
                    invalidate();
                });
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            RemoteSession current = session;
            ScreenBuffer frame = current == null ? null : current.latestScreen();
            if (frame == null) {
                bitmap = null;
                drawn = null;
                destination.setEmpty();
                return;
            }
            if (bitmap == null || bitmap.getWidth() != frame.width || bitmap.getHeight() != frame.height) {
                bitmap = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888);
                bitmap.setHasAlpha(false);
            }
            frame.copyTo(bitmap::setPixels, frame != drawn);
            drawn = frame;
            float scale = Math.min((float) getWidth() / frame.width, (float) getHeight() / frame.height);
            float left = (getWidth() - frame.width * scale) / 2;
            float top = (getHeight() - frame.height * scale) / 2;
            destination.set(left, top, getWidth() - left, getHeight() - top);
            canvas.drawBitmap(bitmap, null, destination, paint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (session == null || drawn == null || session.latestScreen() == null || !control.isChecked()
                    || destination.isEmpty()) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_CANCEL) {
                cancelGesture();
                return true;
            }
            int index = action == MotionEvent.ACTION_MOVE ? event.findPointerIndex(contact) : event.getActionIndex();
            if (index < 0) return false;
            float px = event.getX(index);
            float py = event.getY(index);
            int id = event.getPointerId(index);
            if (action == MotionEvent.ACTION_DOWN && destination.contains(px, py)) {
                contact = id;
                sendPointer(0, px, py);
                return true;
            }
            if (contact != id) return contact != -1;
            if (action == MotionEvent.ACTION_MOVE) sendPointer(1, px, py);
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                sendPointer(2, px, py);
                contact = -1;
                performClick();
            }
            return true;
        }

        private void sendPointer(int phase, float x, float y) {
            int remoteX = Math.max(0, Math.min(drawn.width - 1,
                    (int) ((x - destination.left) * drawn.width / destination.width())));
            int remoteY = Math.max(0, Math.min(drawn.height - 1,
                    (int) ((y - destination.top) * drawn.height / destination.height())));
            if (phase != 1 || remoteX != lastX || remoteY != lastY) session.pointer(phase, remoteX, remoteY);
            lastX = remoteX;
            lastY = remoteY;
        }

        void cancelGesture() {
            if (contact >= 0 && session != null) session.releaseInput();
            contact = -1;
        }

        @Override public boolean performClick() {
            return super.performClick();
        }
    }
}
