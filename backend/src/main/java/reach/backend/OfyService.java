package reach.backend;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

import reach.backend.feedback.FeedBack;
import reach.backend.music.MusicData;
import reach.backend.notifications.BecameFriends;
import reach.backend.notifications.Like;
import reach.backend.notifications.Notification;
import reach.backend.notifications.Push;
import reach.backend.notifications.PushAccepted;
import reach.backend.transactions.CompletedOperations;
import reach.backend.user.MusicSplitter;
import reach.backend.user.ReachUser;
import reach.backend.user.SplitMusicContainer;

/**
 * Objectify service wrapper so we can statically register our persistence classes
 * More on Objectify here : https://code.google.com/p/objectify-appengine/
 */
public class OfyService {

    static {
        ObjectifyService.register(ReachUser.class);
        ObjectifyService.register(FeedBack.class);
        ObjectifyService.register(CompletedOperations.class);
        ObjectifyService.register(SplitMusicContainer.class);
        ObjectifyService.register(MusicSplitter.class);
        ObjectifyService.register(MusicData.class);
        ObjectifyService.register(Notification.class);

        ObjectifyService.register(Like.class);
        ObjectifyService.register(BecameFriends.class);
        ObjectifyService.register(Push.class);
        ObjectifyService.register(PushAccepted.class);
    }

    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
}
