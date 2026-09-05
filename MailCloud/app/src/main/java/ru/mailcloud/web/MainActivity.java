package ru.mailcloud.web;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final int SCROLL_STEP = 200;
    private static final int SCROLL_REPEAT_DELAY = 60;
    private static final long SWIPE_MIN_INTERVAL = 220;

    private static final int[] MEDIA_KEYS = {
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
    };

    private WebView web;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Runnable scrollRunnable;
    private long lastSwipeTime;

    private TextView btnLeft;
    private TextView btnRight;
    private TextView btnClose;
    private boolean buttonsMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setImmersiveMode();

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);

        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setBackgroundColor(0xFF000000);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.setHorizontalScrollBarEnabled(false);
        web.setVerticalScrollBarEnabled(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                injectTvScript();
                web.requestFocus();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient());

        web.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return handleWebKeyDown(keyCode);
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                return handleWebKeyUp(keyCode);
            }
            return false;
        });

        web.loadUrl(getString(R.string.app_url));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        btnLeft = makeArrowButton("\u276E", Gravity.LEFT | Gravity.CENTER_VERTICAL, 26, 24, v -> {
            swipe(-1);
        });
        btnRight = makeArrowButton("\u276F", Gravity.RIGHT | Gravity.CENTER_VERTICAL, 26, 24, v -> {
            swipe(1);
        });
        btnClose = makeArrowButton("\u2715", Gravity.RIGHT | Gravity.TOP, 34, 16, v -> {
            if (web != null && web.canGoBack()) {
                web.goBack();
            } else {
                finish();
            }
            exitButtonsMode();
        });

        root.addView(btnLeft);
        root.addView(btnRight);
        root.addView(btnClose);

        setContentView(root);
    }

    private TextView makeArrowButton(String glyph, int gravity, int sizeDp, int marginDp,
                                     View.OnClickListener listener) {
        TextView tv = new TextView(this);
        tv.setText(glyph);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(sizeDp);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setFocusable(true);
        tv.setClickable(true);
        int pad = dp(12);
        tv.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xCC000000);
        bg.setStroke(dp(2), 0xFFFFD740);
        tv.setBackground(bg);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(60), dp(60));
        lp.gravity = gravity;
        if ((gravity & Gravity.LEFT) != 0) lp.leftMargin = dp(marginDp);
        if ((gravity & Gravity.RIGHT) != 0) lp.rightMargin = dp(marginDp);
        if ((gravity & Gravity.TOP) != 0) lp.topMargin = dp(marginDp);
        tv.setLayoutParams(lp);
        tv.setOnClickListener(listener);

        tv.setOnFocusChangeListener((v, hasFocus) -> {
            v.animate().scaleX(hasFocus ? 1.3f : 1f)
                    .scaleY(hasFocus ? 1.3f : 1f)
                    .setDuration(120).start();
        });

        return tv;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void setImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void injectTvScript() {
        String js = loadAsset("tv_inject.js");
        if (!js.isEmpty()) {
            web.evaluateJavascript(js, null);
        }
    }

    private void reInjectAfterDelay() {
        handler.removeCallbacksAndMessages("reinject");
        handler.postDelayed(() -> {
            if (web != null) injectTvScript();
        }, 1200);
    }

    private String loadAsset(String name) {
        try {
            InputStream is = getAssets().open(name);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            is.close();
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private void swipe(int dir) {
        long now = System.currentTimeMillis();
        if (now - lastSwipeTime < SWIPE_MIN_INTERVAL) return;
        lastSwipeTime = now;
        if (web != null) {
            web.evaluateJavascript("window.__tvSwipe&&window.__tvSwipe(" + dir + ")", null);
        }
    }

    private boolean handleWebKeyDown(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                swipe(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                swipe(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                startScroll(-SCROLL_STEP);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                startScroll(SCROLL_STEP);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                web.evaluateJavascript("window.__tvClick&&window.__tvClick()", value -> {
                    if (value == null || value.equals("false")) {
                        web.evaluateJavascript(
                                "(document.activeElement&&document.activeElement.click?document.activeElement.click():null)", null);
                    }
                });
                return true;
        }
        return false;
    }

    private boolean handleWebKeyUp(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            stopScroll();
            return true;
        }
        return false;
    }

    private void startScroll(int step) {
        stopScroll();
        scrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (web != null) {
                    web.evaluateJavascript("window.scrollBy(0," + step + ")", null);
                    handler.postDelayed(this, SCROLL_REPEAT_DELAY);
                }
            }
        };
        handler.post(scrollRunnable);
    }

    private void stopScroll() {
        if (scrollRunnable != null) {
            handler.removeCallbacks(scrollRunnable);
            scrollRunnable = null;
        }
    }

    private void enterButtonsMode() {
        buttonsMode = true;
        btnClose.setFocusable(true);
        btnLeft.setFocusable(true);
        btnRight.setFocusable(true);
        btnClose.requestFocus();
        btnLeft.setAlpha(1f);
        btnRight.setAlpha(1f);
        btnClose.setAlpha(1f);
    }

    private void exitButtonsMode() {
        buttonsMode = false;
        web.requestFocus();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (buttonsMode) {
                exitButtonsMode();
            } else {
                enterButtonsMode();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (buttonsMode) {
                exitButtonsMode();
            } else if (web != null && web.canGoBack()) {
                web.goBack();
            } else {
                finish();
            }
            return true;
        }
        if (isMedia(keyCode)) {
            web.dispatchKeyEvent(event);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isMedia(keyCode)) {
            web.dispatchKeyEvent(event);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private static boolean isMedia(int keyCode) {
        for (int k : MEDIA_KEYS) {
            if (k == keyCode) return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setImmersiveMode();
        web.requestFocus();
        reInjectAfterDelay();
    }

    @Override
    protected void onDestroy() {
        stopScroll();
        handler.removeCallbacksAndMessages(null);
        if (web != null) web.destroy();
        super.onDestroy();
    }
}