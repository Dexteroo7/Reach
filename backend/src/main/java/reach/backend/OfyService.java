package reach.backend;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

import reach.backend.Feedback.FeedBack;
import reach.backend.Music.MusicVisibility;
import reach.backend.Notifications.Notification;
import reach.backend.User.CompletedOperations;
import reach.backend.User.MusicSplitter;
import reach.backend.User.ReachUser;
import reach.backend.User.SplitMusicContainer;

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
        ObjectifyService.register(MusicVisibility.class);
        ObjectifyService.register(Notification.class);
    }

    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
}
