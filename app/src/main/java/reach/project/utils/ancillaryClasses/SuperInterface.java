package reach.project.utils.ancillaryClasses;

import android.support.v7.widget.Toolbar;

/**
 * Created by Dexter on 13-04-2015.
 */
public interface SuperInterface {

    Toolbar.OnMenuItemClickListener getMenuClickListener();

    void addSongToQueue(long songId, long senderId, long size,
                        String displayName, String actualName,
                        boolean multiple, String userName, String onlineStatus,
                        String networkType, String artistName, long duration,
                        String albumName, String genre); //MusicListFragment

    void showSwipeCoach();
}
