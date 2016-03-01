package reach.project.onBoarding;

import android.net.Uri;

import com.google.common.base.Optional;

import reach.backend.entities.userApi.model.OldUserContainerNew;

/**
 * Created by ashish on 21/12/15.
 */
public interface SplashInterface {

    void onOpenNumberVerification();
    void onOpenCodeVerification(String phoneNumber);
    void onOpenAccountCreation(Optional<OldUserContainerNew> container);
    void onOpenScan(String name, Uri profilePicUri, Uri coverPicUri, String phoneNumber);
}