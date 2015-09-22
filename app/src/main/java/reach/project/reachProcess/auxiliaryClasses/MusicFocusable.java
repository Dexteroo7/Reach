package reach.project.reachProcess.auxiliaryClasses;

/**
 * Created by Dexter on 6/9/2015.
 */
public interface MusicFocusable {
    /** Signals that audio focus was gained. */
    void onGainedAudioFocus();
    /**
     * Signals that audio focus was lost.
     *
     * @param canDuck If true, audio can continue in "ducked" mode (low volume). Otherwise, all
     * audio must stop.
     */
    void onLostAudioFocus(boolean canDuck);
}