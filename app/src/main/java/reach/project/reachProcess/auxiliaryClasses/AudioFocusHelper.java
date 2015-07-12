package reach.project.reachProcess.auxiliaryClasses;

/**
 * Created by Dexter on 6/9/2015.
 */

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

/**
 * Convenience class to deal with audio focus. This class deals with everything related to audio
 * focus: it can request and abandon focus, and will intercept focus change events and deliver
 * them to a MusicFocusable interface.
 *
 * This class can only be used on SDK level 8 and above, since it uses API features that are not
 * available on previous SDK's.
 */
public class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {

    private final AudioManager mAM;
    private final MusicFocusable mFocusable;

    public AudioFocusHelper(Context ctx, MusicFocusable focusable) {
        mAM = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        mFocusable = focusable;
    }

    /** Requests audio focus. Returns whether request was successful or not. */
    public boolean requestFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                mAM.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    /** Abandons audio focus. Returns whether request was successful or not. */
    public boolean abandonFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAM.abandonAudioFocus(this);
    }

    /**
     * Called by AudioManager on audio focus changes. We implement this by calling our
     * MusicFocusable appropriately to relay the message.
     */
    @Override
    public void onAudioFocusChange(int focusChange) {

        Log.i("Downloader", "Received focus change");
        if (mFocusable == null) return;
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.i("Downloader", "Received AUDIOFOCUS_GAIN");
                mFocusable.onGainedAudioFocus();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.i("Downloader", "Received AUDIOFOCUS_LOSS");
                mFocusable.onLostAudioFocus(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.i("Downloader", "Received AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                mFocusable.onLostAudioFocus(true);
                break;
            default:
        }
    }
}