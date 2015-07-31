package reach.project.utils;

import com.google.common.base.Optional;

import java.util.HashSet;

import reach.backend.entities.userApi.model.OldUserContainerNew;

/**
 * Created by Dexter on 13-04-2015.
 */
public interface SuperInterface {

    void onAccountCreated(); //AccountCreationFragment

    void accountCreationError();

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

    void startAccountCreation(Optional<OldUserContainerNew> container); //NumberVerificationFragment

    void startMusicListFragment(long id, String albumName, String artistName, String playListName, int type);

    void onOpenProfile();

    void addNotificationDrawer();

    void onOpenNotificationDrawer();

    void onPrivacyDone(); //PrivacyFragment

    void onPushNext(HashSet<TransferSong> songsList); //PushSongFragment

}
