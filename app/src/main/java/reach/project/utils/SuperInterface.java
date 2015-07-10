package reach.project.utils;

import java.util.HashSet;

/**
 * Created by Dexter on 13-04-2015.
 */
public interface SuperInterface {

    void onNavigationDrawerItemSelected(int position); //NavigationDrawerFragment

    void onAccountCreated(); //AccountCreationFragment

    void startNumberVerification();

    /**
     * ContactsListFragment
     */
    void onOpenProfileView(long id);

    void onOpenLibrary(long id);

    void onOpenPushLibrary();

    void setUpDrawer();

    void toggleDrawer(boolean lock);

    void toggleSliding(boolean show);

    void anchorFooter(boolean first);

    void setUpNavigationViews();

    void addSongToQueue(long songId,
                        long senderId,
                        long size,
                        String displayName,
                        String actualName,
                        boolean multiple,
                        String userName,
                        String onlineStatus,
                        String networkType,
                        String artistName,
                        long duration); //MusicListFragment

    void startAccountCreation(); //NumberVerificationFragment

    void startMusicListFragment(long id, String albumName, String artistName, String playListName, int type);

    void onNextClicked(); //InviteFragment

    void OnSplash(); //SplashFragment

    void onOpenProfile();

    void onPrivacyDone(); //PrivacyFragment

    void onPushNext(HashSet<TransferSong> songsList); //PushSongFragment

    void goLibrary(long id); //UserProfileView

}
