package reach.project.onBoarding;

import com.google.common.base.Optional;

import reach.backend.entities.userApi.model.OldUserContainerNew;

/**
 * Created by ashish on 21/12/15.
 */
public interface SplashInterface {
    void onOpenNumberVerification();
    void onOpenCodeVerification(String key);
    void onOpenAccountCreation(Optional<OldUserContainerNew> container);
}
