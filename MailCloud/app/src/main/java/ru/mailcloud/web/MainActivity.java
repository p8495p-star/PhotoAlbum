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

public class MainActivity extends Activity {

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

    private TextView btnLeft;
    private TextView btnRight;
    private TextView btnClose;

    private long lastSwipeTime;

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
                web.requestFocus();
                injectTvScript();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient());

        web.loadUrl(getString(R.string.app_url));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        btnLeft = makeArrowButton("\u276E", Gravity.LEFT | Gravity.CENTER_VERTICAL, 26, 24, true, v -> {
            swipe(-1);
        });
        btnRight = makeArrowButton("\u276F", Gravity.RIGHT | Gravity.CENTER_VERTICAL, 26, 24, true, v -> {
            swipe(1);
        });
        btnClose = makeArrowButton("\u2715", Gravity.RIGHT | Gravity.TOP, 34, 16, true, v -> {
            web.goBack();
        });

        root.addView(btnLeft);
        root.addView(btnRight);
        root.addView(btnClose);

        setContentView(root);
    }

    private TextView makeArrowButton(String glyph, int gravity, int sizeDp, int marginDp,
                                     boolean focusable, View.OnClickListener listener) {
        TextView tv = new TextView(this);
        tv.setText(glyph);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(sizeDp);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setFocusable(focusable);
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

        tv.setVisibility(View.GONE);
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
        String js = "(function(){"
                + "var style=document.createElement('style');"
                + "style.textContent='"
                + "*:focus{outline:3px solid #4FC3F7 !important;outline-offset:2px !important;}"
                + "a:focus,button:focus,input:focus,select:focus,textarea:focus,[tabindex]:focus{"
                + "outline:3px solid #4FC3F7 !important;outline-offset:2px !important;}"
                + "';"
                + "document.head.appendChild(style);"
                + "function findPhotoTarget(){"
                + "var hits=document.querySelectorAll('[class*=photo],[class*=image],[class*=fullscreen],[class*=preview],[class*=viewer],[class*=lightbox]');"
                + "for(var i=0;i<hits.length;i++){var r=hits[i].getBoundingClientRect();"
                + "if(r.width>0&&r.height>0)return hits[i];}"
                + "return null;"
                + "}"
                + "window.__tvSwipe=function(dir){"
                + "var target=findPhotoTarget()||document.body;"
                + "var w=window.innerWidth,h=window.innerHeight;"
                + "var off=Math.min(w,h)*0.35;"
                + "var sx=w/2+dir*off,ex=w/2-dir*off,sy=h/2,ey=h/2;"
                + "dispatchTouch(target,'touchstart',sx,sy);"
                + "setTimeout(function(){dispatchTouch(target,'touchmove',(sx+ex)/2,(sy+ey)/2);},30);"
                + "setTimeout(function(){"
                + "dispatchTouch(target,'touchend',ex,ey);"
                + "dispatchMouse(target,'mousedown',ex,ey);"
                + "dispatchMouse(target,'mouseup',ex,ey);"
                + "dispatchMouse(target,'click',ex,ey);"
                + "},90);"
                + "};"
                + "function dispatchTouch(t,name,cx,cy){"
                + "var ev;"
                + "try{var touch=new Touch({identifier:1,target:t,clientX:cx,clientY:cy,pageX:cx,pageY:cy});"
                + "ev=new TouchEvent(name,{bubbles:true,cancelable:true,touches:name==='touchend'?[]:[touch],targetTouches:[],changedTouches:[touch]});}"
                + "catch(e){"
                + "ev=new Event(name,{bubbles:true,cancelable:true});"
                + "Object.defineProperty(ev,'touches',{value:name==='touchend'?[]:[{clientX:cx,clientY:cy}]});"
                + "Object.defineProperty(ev,'changedTouches',{value:[{clientX:cx,clientY:cy}]});"
                + "}"
                + "t.dispatchEvent(ev);"
                + "}"
                + "function dispatchMouse(t,name,cx,cy){"
                + "var ev=new MouseEvent(name,{bubbles:true,cancelable:true,clientX:cx,clientY:cy});"
                + "t.dispatchEvent(ev);"
                + "}"
                + "window.__tvClick=function(){"
                + "var el=document.activeElement;"
                + "if(el&&el.tagName!=='BODY'&&el.tagName!=='HTML'){"
                + "el.click();var r=el.getBoundingClientRect();"
                + "dispatchMouse(el,'mousedown',r.left+r.width/2,r.top+r.height/2);"
                + "dispatchMouse(el,'mouseup',r.left+r.width/2,r.top+r.height/2);"
                + "return true;}"
                + "return false;"
                + "};"
                + "return true;"
                + "})()";
        web.evaluateJavascript(js, null);
    }

    private void reInjectAfterDelay() {
        handler.removeCallbacksAndMessages("reinject");
        handler.postDelayed(() -> {
            if (web != null) injectTvScript();
        }, 1500);
    }

    private void swipe(int dir) {
        long now = System.currentTimeMillis();
        if (now - lastSwipeTime < 200) return;
        lastSwipeTime = now;
        showButtons();
        if (web != null) {
            web.evaluateJavascript("window.__tvSwipe&&window.__tvSwipe(" + dir + ")", null);
            handler.removeCallbacksAndMessages("hide");
            handler.postDelayed(this::hideButtons, 1600);
        }
    }

    private void showButtons() {
        btnLeft.setVisibility(View.VISIBLE);
        btnRight.setVisibility(View.VISIBLE);
        btnClose.setVisibility(View.VISIBLE);
    }

    private void hideButtons() {
        btnLeft.setVisibility(View.GONE);
        btnRight.setVisibility(View.GONE);
        btnClose.setVisibility(View.GONE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
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
            case KeyEvent.KEYCODE_DPAD_LEFT:
                swipe(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                swipe(1);
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
        handler.removeCallbacksAndMessages(null);
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
