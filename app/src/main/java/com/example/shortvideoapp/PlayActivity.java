package com.example.shortvideoapp;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class PlayActivity extends AppCompatActivity {
    private static final String TAG = "PlayActivity";
    MediaPlayer mediaPlayer;
    SurfaceView surfaceView;
    private SurfaceHolder holder;
    boolean isPlaying = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        mediaPlayer = new MediaPlayer();
        surfaceView = findViewById(R.id.surfaceView);
//        ijkPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
//        ijkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "madiacodec",1);
        try {
            Uri uri = Uri.parse(getIntent().getStringExtra("url"));
            mediaPlayer.setDataSource(PlayActivity.this, uri);
            Log.d(TAG, "Uri "+uri.toString());
        } catch (IOException e) {
            Log.d("playactivity", "setDataSource fail: "+e.getMessage().toString());
            e.printStackTrace();
        }
        holder = surfaceView.getHolder();
        holder.addCallback(new PlayerCallBack());
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d(TAG, "mediaPlayer onPrepared ");
                mediaPlayer.start();
                mediaPlayer.setLooping(true);
            }
        });

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlaying){
                    Log.d(TAG, "ijkPlayer pause ");
                    mediaPlayer.pause();
                    isPlaying = false;
                }else{
                    Log.d(TAG, "ijkPlayer start ");
                    mediaPlayer.start();
                    isPlaying = true;
                }
            }
        });
        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class PlayerCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mediaPlayer.setDisplay(holder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }
}
