package reach.project.onBoarding;

import android.net.Uri;

import com.appspot.able_door_616.userApi.model.UserDataPersistence;
import com.google.common.base.Optional;

import java.io.Serializable;

/**
 * Created by ashish on 21/12/15.
 */
public interface SplashInterface {

    void onOpenNumberVerification();

    void onOpenCodeVerification(String phoneNumber);

    void onOpenAccountCreation(Optional<UserDataPersistence> container);

    void onOpenScan(String name,
                    long oldUserId,
                    String oldProfilePicId,
                    String oldCoverPicId,
                    Uri newProfilePicUri,
                    Uri newCoverPicUri,
                    Serializable contentState);
}