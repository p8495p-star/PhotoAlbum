package ru.photoalbum.app;

import android.content.Context;
import android.content.Intent;

import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;

public class WebAppInterface {

    private final Context context;

    public WebAppInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void openVideo(String url) {
        if (url == null || url.isEmpty()) return;
        String cookie = "";
        try {
            String c = CookieManager.getInstance().getCookie(url);
            if (c != null) cookie = c;
        } catch (Throwable ignored) {
        }
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_URL, url);
        intent.putExtra(VideoPlayerActivity.EXTRA_COOKIE, cookie);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}