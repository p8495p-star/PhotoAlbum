package ru.photoalbum.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import java.util.HashMap;
import java.util.Map;

public class VideoPlayerActivity extends Activity implements Player.Listener {

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_COOKIE = "cookie";

    private PlayerView playerView;
    private ExoPlayer player;
    private boolean endNotified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.isEmpty()) {
            finish();
            return;
        }
        String cookie = getIntent().getStringExtra(EXTRA_COOKIE);

        playerView = new PlayerView(this);
        playerView.setBackgroundColor(Color.BLACK);
        playerView.setUseController(true);
        playerView.setControllerShowTimeoutMs(4000);
        setContentView(playerView);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build();

        ExoPlayer.Builder playerBuilder = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setLoadControl(new DefaultLoadControl.Builder()
                        .setBufferDurationsMs(60000, 300000, 15000, 15000)
                        .setTargetBufferBytes(300 * 1024 * 1024)
                        .build());
        if (cookie != null && !cookie.isEmpty()) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", cookie);
            DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
            dataSourceFactory.setDefaultRequestProperties(headers);
            playerBuilder.setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory));
        }
        player = playerBuilder.build();
        player.addListener(this);

        playerView.setPlayer(player);

        MediaItem item = new MediaItem.Builder()
                .setUri(Uri.parse(url))
                .setMediaMetadata(new MediaMetadata.Builder().setTitle(url).build())
                .build();
        player.setMediaItem(item);
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        if (player != null) {
            player.stop();
        }
        finish();
    }

    @Override
    public void onIsEndedChanged(boolean isEnded) {
        if (isEnded) {
            finish();
        }
    }

    @Override
    public void finish() {
        notifyVideoClosed();
        super.finish();
    }

    private void notifyVideoClosed() {
        if (endNotified) return;
        endNotified = true;
        MainActivity.notifyVideoFinished();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    seekRelative(-15000);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    seekRelative(15000);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    seekRelative(30000);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    seekRelative(-30000);
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                togglePlay();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                player.play();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                player.pause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                player.stop();
                finish();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void togglePlay() {
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
    }

    private void seekRelative(long deltaMs) {
        if (player == null) return;
        long position = player.getCurrentPosition();
        long duration = player.getDuration();
        long target = position + deltaMs;
        if (duration > 0) {
            target = Math.max(0, Math.min(target, duration));
        } else if (target < 0) {
            target = 0;
        }
        player.seekTo(target);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (playerView != null) {
            playerView.setPlayer(null);
        }
        if (player != null) {
            player.removeListener(this);
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}