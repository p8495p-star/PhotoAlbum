package ru.mailcloud.web;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private static final int SCROLL_STEP = 250;
    private static final int SCROLL_REPEAT_DELAY = 80;
    private static final int FOCUS_JS_DELAY = 800;

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
    private boolean scrollActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

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

        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setBackgroundColor(0xFF000000);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.setHorizontalScrollBarEnabled(false);
        web.setVerticalScrollBarEnabled(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                web.requestFocus();
                injectTvFocusScript();
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
                return handleKeyDown(keyCode, event);
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                return handleKeyUp(keyCode, event);
            }
            return false;
        });

        web.loadUrl(getString(R.string.app_url));
        setContentView(web);
    }

    private void injectTvFocusScript() {
        String js = "(function(){"
                + "var style=document.createElement('style');"
                + "style.textContent='"
                + "*:focus{outline:3px solid #4FC3F7 !important;outline-offset:2px !important;}"
                + "a:focus,b:focus,button:focus,input:focus,select:focus,textarea:focus,[tabindex]:focus{"
                + "outline:3px solid #4FC3F7 !important;outline-offset:2px !important;"
                + "box-shadow:0 0 8px rgba(79,191,247,0.6) !important;}"
                + "';"
                + "document.head.appendChild(style);"
                + "var els=document.querySelectorAll('a[href],button,input,select,textarea,[onclick],[role=button],[role=link]');"
                + "for(var i=0;i<els.length;i++){"
                + "if(!els[i].hasAttribute('tabindex'))els[i].setAttribute('tabindex','0');"
                + "}"
                + "window.__tvScroll=function(dy){window.scrollBy(0,dy);};"
                + "window.__tvScrollX=function(dx){window.scrollBy(dx,0);};"
                + "window.__tvClickFocused=function(){"
                + "var el=document.activeElement;"
                + "if(el&&el.tagName!=='BODY'&&el.tagName!=='HTML'){el.click();return true;}"
                + "return false;"
                + "};"
                + "return true;"
                + "})()";
        web.evaluateJavascript(js, null);
    }

    private void reInjectAfterDelay() {
        handler.removeCallbacksAndMessages("reinject");
        handler.postDelayed(() -> {
            if (web != null) {
                injectTvFocusScript();
            }
        }, 1500);
    }

    private boolean handleKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (web != null && web.canGoBack()) {
                web.goBack();
            } else {
                finish();
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }

        if (isMedia(keyCode)) {
            web.dispatchKeyEvent(event);
            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                startScroll(-SCROLL_STEP);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                startScroll(SCROLL_STEP);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                scrollX(-SCROLL_STEP);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                scrollX(SCROLL_STEP);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                web.evaluateJavascript("window.__tvClickFocused&&window.__tvClickFocused()", value -> {
                    if (value == null || value.equals("false")) {
                        web.evaluateJavascript("document.activeElement.click()", null);
                    }
                });
                return true;
        }

        return false;
    }

    private boolean handleKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            stopScroll();
            return true;
        }
        if (isMedia(keyCode)) {
            web.dispatchKeyEvent(event);
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
                    web.evaluateJavascript("window.__tvScroll(" + step + ")", null);
                    handler.postDelayed(this, SCROLL_REPEAT_DELAY);
                }
            }
        };
        handler.post(scrollRunnable);
        scrollActive = true;
    }

    private void scrollX(int step) {
        if (web != null) {
            web.evaluateJavascript("window.__tvScrollX(" + step + ")", null);
        }
    }

    private void stopScroll() {
        if (scrollRunnable != null) {
            handler.removeCallbacks(scrollRunnable);
            scrollRunnable = null;
            scrollActive = false;
        }
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
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        web.requestFocus();
        reInjectAfterDelay();
    }

    @Override
    protected void onPause() {
        stopScroll();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopScroll();
        handler.removeCallbacksAndMessages(null);
        if (web != null) {
            web.destroy();
        }
        super.onDestroy();
    }
}
