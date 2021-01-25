package com.stringee.chat.ui.kit.commons;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.stringee.messaging.Message;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class MediaPlayerManager implements AudioManager.OnAudioFocusChangeListener {
    private static MediaPlayerManager myObj;
    private ImageView playIconImageView;
    Map<String, MediaPlayer> pool = new HashMap<>();
    Context context;
    AudioManager audioManager;
    int maxsize = 5;
    String audio_duration;
    int hours, minute, second, duration;
    private Message message;

    private MediaPlayerManager(Context context) {
        this.context = context;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    public static MediaPlayerManager getInstance(Context context) {
        if (myObj == null) {
            myObj = new MediaPlayerManager(context.getApplicationContext());
        }
        return myObj;
    }

    public void play(final Uri uri, final Message message, ImageView playIconImageView, SeekBar seekBar, final TextView durationTextView) {
        this.playIconImageView = playIconImageView;
        this.message = message;
        String key = message.getLocalId();
        if (message.getMsgType() == Message.MESSAGE_TYPE_RECEIVE) {
            key = message.getId();
        }
        MediaPlayer mp = pool.get(key);
        if (mp != null) {
            if (mp.isPlaying()) {
                mp.pause();
                return;
            } else {
                mp.seekTo(mp.getCurrentPosition());
                if (requestAudioFocus()) {
                    mp.start();
                }
            }
        } else {
            mp = new MediaPlayer();
            if (pool.size() >= maxsize) {
                Map.Entry<String, MediaPlayer> entry = pool.entrySet().iterator().next();
                String first = entry.getKey();
                pool.remove(first);
            }
            pool.put(key, mp);
        }
        pauseOthersifPlaying();
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(context, R.string.cannot_play_audio_file, Toast.LENGTH_LONG).show();
                return false;
            }
        });
        try {
            if (context != null) {
                mp.setDataSource(context, uri);
                if (requestAudioFocus()) {
                    mp.prepare();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mp.start();
        setAudioIcons();
        final String finalKey = key;
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                pool.remove(finalKey);
                setAudioIcons();
                durationTextView.setText(Utils.getAudioTime(message.getDuration()));
            }
        });

        final String finalKey1 = key;
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int duration = progress / 1000;
                int min = duration / 60;
                int seconds = duration % 60;
                if (durationTextView != null && (min > 0 || seconds > 0)) {
                    durationTextView.setText(String.format("%02d:%02d", min, seconds));
                }

                if (fromUser) {
                    if (getMediaPlayer(finalKey1) != null) {
                        getMediaPlayer(finalKey1).seekTo(progress);
                    }
                }
            }
        });
    }

    public void pauseOthersifPlaying() {
        MediaPlayer m;
        Iterator it = pool.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            {
                m = (MediaPlayer) pair.getValue();
                if (m.isPlaying()) {
                    m.pause();
                }
            }
        }
    }

    private void pauseIfPlaying() {
        MediaPlayer m;
        Iterator it = pool.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            {
                m = (MediaPlayer) pair.getValue();
                if (m.isPlaying()) {
                    m.pause();
                }
            }
        }
    }

    public int getAudioState(String key) {
        MediaPlayer mp = pool.get(key);
        if (mp != null) {
            if (mp.isPlaying()) {
                return 1;
            }
            return 0;
        }
        return -1;
    }

    public MediaPlayer getMediaPlayer(String key) {
        if (key == null) {
            return null;
        }
        return pool.get(key);
    }

    public void audiostop() {
        if (pool != null) {
            MediaPlayer temp;
            Iterator it = pool.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                {
                    temp = (MediaPlayer) pair.getValue();
                    temp.stop();
                    temp.release();
                }
            }
            pool.clear();
        }
    }

    public String refreshAudioDuration(String filePath) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            duration = mediaPlayer.getDuration();
            duration = duration / 1000;
            hours = duration / 3600;
            minute = duration / 60;
            second = (duration % 60) + 1;
            mediaPlayer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        audio_duration = String.format("%02d:%02d", minute, second);
        return audio_duration;
    }

    @Override
    public void onAudioFocusChange(int i) {
        switch (i) {
            case AudioManager.AUDIOFOCUS_GAIN:
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                pauseIfPlaying();
                setAudioIcons();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                pauseIfPlaying();
                setAudioIcons();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pauseIfPlaying();
                setAudioIcons();
                break;
        }
    }

    private boolean requestAudioFocus() {
        boolean gotFocus;
        int audioFocus = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (audioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            gotFocus = true;
        } else {
            gotFocus = false;
        }
        return gotFocus;
    }

    private void setAudioIcons() {
        String key = message.getLocalId();
        if (message.getMsgType() == Message.MESSAGE_TYPE_RECEIVE) {
            key = message.getId();
        }
        int state = MediaPlayerManager.getInstance(context).getAudioState(key);
        playIconImageView.setVisibility(View.VISIBLE);
        if (state == 1) {
            playIconImageView.setImageResource(R.drawable.ic_pause_circle_outline);
        } else {
            playIconImageView.setImageResource(R.drawable.ic_play_circle_outline);
        }
    }
}