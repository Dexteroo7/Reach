package reach.project.utils.ancillaryClasses;

/**
 * Created by Dexter on 13-04-2015.
 */
public interface SuperInterface {

    void onOpenLibrary(long id);

    void addSongToQueue(long songId, long senderId, long size,
                        String displayName, String actualName,
                        boolean multiple, String userName, String onlineStatus,
                        String networkType, String artistName, long duration,
                        String albumName, String genre); //MusicListFragment

}