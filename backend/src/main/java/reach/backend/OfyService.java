package reach.backend;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;

/**
 * Objectify service wrapper so we can statically register our persistence classes
 * More on Objectify here : https://code.google.com/p/objectify-appengine/
 */
public class OfyService {

//    static {
//        ObjectifyService.register(ReachUser.class);
//        ObjectifyService.register(FeedBack.class);
//        ObjectifyService.register(CompletedOperations.class);
//        ObjectifyService.register(SplitMusicContainer.class);
//        ObjectifyService.register(MusicSplitter.class);
//        ObjectifyService.register(MusicData.class);
//        ObjectifyService.register(Notification.class);
//
//        ObjectifyService.register(Like.class);
//        ObjectifyService.register(BecameFriends.class);
//        ObjectifyService.register(Push.class);
//        ObjectifyService.register(PushAccepted.class);
//    }

    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

//    private static ObjectifyFactory factory() {
//        return ObjectifyService.factory();
//    }
}
