package com.sun_asterisk.music_65.screen.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import com.sun_asterisk.music_65.data.model.Song;
import com.sun_asterisk.music_65.screen.notification.SongNotification;
import com.sun_asterisk.music_65.utils.CommonUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SongService extends Service implements MediaPlayer.OnPreparedListener {
    private SongBinder mSongBinder;
    private SongNotification mSongNotification;
    private final static String EXTRA_SONGS_LIST = "EXTRA_SONGS_LIST";
    private final static String EXTRA_SONG_POSITION = "EXTRA_SONG_POSITION";
    private final static int DEFAULT_POSITION = 0;
    private final static int POSITION_VALUE_ONE = 1;
    private List<Song> mSongs;
    private int mPosition;
    private MediaPlayer mMediaPlayer;

    public static Intent getServiceIntent(Context context, List<Song> songs, int position) {
        Intent intent = new Intent(context, SongService.class);
        intent.putParcelableArrayListExtra(EXTRA_SONGS_LIST,
            (ArrayList<? extends Parcelable>) songs);
        intent.putExtra(EXTRA_SONG_POSITION, position);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSongBinder = new SongBinder();
        mSongNotification = new SongNotification(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            List<Song> songs = intent.getParcelableArrayListExtra(EXTRA_SONGS_LIST);
            if (songs != null) {
                mSongs = songs;
                mPosition = intent.getIntExtra(EXTRA_SONG_POSITION, DEFAULT_POSITION);
                if (mSongNotification != null) {
                    mSongNotification.initNotification();
                }
                playSong();
            }
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case CommonUtils.Action.ACTION_PLAY_AND_PAUSE:
                        pauseSong();
                        break;
                    case CommonUtils.Action.ACTION_NEXT:
                        nextSong();
                        break;
                    case CommonUtils.Action.ACTION_PREVIOUS:
                        prevSong();
                        break;
                }
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mSongBinder;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        mSongNotification.updatePlayPauseNotification(true);
        startForeground(SongNotification.NOTIFICATION_INT_ID,
            mSongNotification.getBuilder().build());
    }

    public void playSong() {
        Song song = mSongs.get(mPosition);
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setDataSource(song.getStreamUrl());
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSongNotification.updateNotificationSong(song);
    }

    public void pauseSong() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        } else {
            mMediaPlayer.start();
        }
        mSongNotification.updatePlayPauseNotification(mMediaPlayer.isPlaying());
        startForeground(SongNotification.NOTIFICATION_INT_ID,
            mSongNotification.getBuilder().build());
    }

    public void nextSong() {
        mPosition++;
        if (mPosition == mSongs.size()) {
            mPosition = DEFAULT_POSITION;
        }
        playSong();
    }

    public void prevSong() {
        mPosition--;
        if (mPosition < DEFAULT_POSITION) {
            mPosition = mSongs.size() - POSITION_VALUE_ONE;
        }
        playSong();
    }

    public class SongBinder extends Binder {
        public SongService getService() {
            return SongService.this;
        }
    }
}
