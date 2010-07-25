package com.droidcool.music;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.util.Log;

public class Api8 {
	public static Object getOnAudioFocusChangeListener(final MediaPlaybackService service) {
	  if (Const.sdk >= 8) {
	    return new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            // AudioFocus is a new feature: focus updates are made verbose on purpose
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    if(service.isPlaying()) {
                      service.mPausedByTransientLossOfFocus = false;
                      service.pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if(service.isPlaying()) {
                      service.mPausedByTransientLossOfFocus = true;
                      service.pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if(!service.isPlaying() && service.mPausedByTransientLossOfFocus) {
                      service.mPausedByTransientLossOfFocus = false;
                      service.startAndFadeIn();
                    }
                    break;
                default:
            }
        }
	    };
	  
	  } else {
	    return null;
	  }
	}
	
	public static void abandonAudioFocus(AudioManager manager, Object listerner) {
	  manager.abandonAudioFocus((OnAudioFocusChangeListener) listerner);
	}
	public static void play(final MediaPlaybackService service, AudioManager manager, Object listerner) {
	    manager.requestAudioFocus((OnAudioFocusChangeListener) listerner, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
	    manager.registerMediaButtonEventReceiver(new ComponentName(service.getPackageName(),
                MediaButtonIntentReceiver.class.getName()));

	}
	
	public static AudioManager getAudioManager(final MediaPlaybackService service) {
	    AudioManager audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
	    audioManager.registerMediaButtonEventReceiver(new ComponentName(service.getPackageName(),
                MediaButtonIntentReceiver.class.getName()));
        return audioManager;
	}
}
