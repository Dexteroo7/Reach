package reach.backend.Notifications;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.repackaged.com.google.common.cache.CacheBuilder;
import com.google.appengine.repackaged.com.google.common.cache.CacheLoader;
import com.google.appengine.repackaged.com.google.common.cache.LoadingCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.inject.Named;

import reach.backend.ObjectWrappers.MyString;
import reach.backend.User.FriendContainers.Friend;
import reach.backend.User.MessagingEndpoint;
import reach.backend.User.ReachUser;

import static reach.backend.OfyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "notificationApi",
        version = "v1",
        resource = "notification",
        namespace = @ApiNamespace(
                ownerDomain = "Notifications.backend.reach",
                ownerName = "Notifications.backend.reach",
                packagePath = ""
        )
)

public class NotificationEndpoint {

    private static final Logger logger = Logger.getLogger(NotificationEndpoint.class.getName());

    private static final int ALL = 0;
    private static final int READ = 1;
    private static final int UN_READ = 2;

    public static NotificationEndpoint notificationEndpoint = null;

    public static NotificationEndpoint getInstance() {
        if (notificationEndpoint == null)
            notificationEndpoint = new NotificationEndpoint();
        return notificationEndpoint;
    }

    /**
     * Returns all the {@link Notification} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     */
    @ApiMethod(
            name = "getNewCount",
            path = "notification/getNewCount/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MyString getNewCount(@Named("id") long id) {

        if (id == 0)
            return null;

        logger.info("Getting Notification count with ID: " + id);
        final Notification database = ofy().load().type(Notification.class).id(id).now();
        if (database == null || database.getNotifications() == null)
            return null;
        return new MyString(getNewCount(database.getNotifications()) + "");
    }

    private int getNewCount(Iterable<NotificationBase> notifications) {

        int count = 0;
        for (NotificationBase base : notifications)
            if (base.getRead() == NotificationBase.NEW)
                count++;

        return count;
    }

    /**
     * Returns all the {@link Notification} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     */
    @ApiMethod(
            name = "getNotifications",
            path = "notification/getNotifications/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public List<NotificationBase> getNotifications(@Named("id") long id, @Named("condition") int condition) {

        if (id == 0)
            return null;

        logger.info("Getting Notification with ID: " + id);
        final Notification database = ofy().load().type(Notification.class).id(id).now();
        if (database == null || database.getNotifications() == null)
            return null;

        final List<NotificationBase> toSend = new ArrayList<>();
        final LoadingCache<Long, ReachUser> cache = CacheBuilder.newBuilder()
                .initialCapacity(10)
                .build(new CacheLoader<Long, ReachUser>() {
                    public ReachUser load(@Nonnegative @Nonnull Long id) {
                        return ofy().load().type(ReachUser.class).id(id).now();
                    }
                });

        for (NotificationBase notificationBase : database.getNotifications()) {

            //check notification
            switch (condition) {
                case ALL:
                    break;
                case READ: //send old Notifications
                    if (notificationBase.getRead() == NotificationBase.UN_READ ||
                            notificationBase.getRead() == NotificationBase.NEW) continue;
                    break;
                case UN_READ: //send (NEW + UN_READ) Notifications
                    if (notificationBase.getRead() == NotificationBase.READ) continue;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown condition " + condition);
            }

            //get User
            final ReachUser reachUser;
            try {
                reachUser = cache.get(notificationBase.getHostId());
                if (reachUser == null)
                    continue; //TODO handle null case
            } catch (Exception e) {
                logger.info("Error getting Notifications " + e.getLocalizedMessage());
                continue;
            }

            //update the Notifications
            notificationBase.setImageId(reachUser.getImageId());
            notificationBase.setHostName(reachUser.getUserName());
            if (notificationBase.getRead() == NotificationBase.NEW) //NEW is only for server
                notificationBase.setRead(NotificationBase.UN_READ);
            toSend.add(notificationBase);
        }

        cache.cleanUp();

        Collections.sort(toSend, new Comparator<NotificationBase>() {
            @Override
            public int compare(NotificationBase o1, NotificationBase o2) {
                if (o1.getSystemTime() < o2.getSystemTime())
                    return 1;
                if (o1.getSystemTime() > o2.getSystemTime())
                    return -1;
                return 0;
            }
        });

        markAllRead(id);
        return toSend;
    }

    private void markAllRead(long id) {

        final Notification database = ofy().load().type(Notification.class).id(id).now();
        if (database == null || database.getNotifications() == null)
            return;

        boolean needToUpdate = false;
        int count = 0;
        for (NotificationBase base : database.getNotifications())
            if (base.getRead() == NotificationBase.NEW) {
                base.setRead(NotificationBase.UN_READ);
                needToUpdate = true;
                count++;
            }

        ofy().save().entity(database).now();
        logger.info("Marking as read " + count + " " + needToUpdate);
    }

    /**
     * @param receiverId the person whose song was liked
     * @param senderId   the person who liked the song
     * @param songName   the name of the liked song
     */
    @ApiMethod(
            name = "addLike",
            path = "notification/addLike",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void addLike(@Named("receiverId") long receiverId,
                        @Named("senderId") long senderId,
                        @Named("songName") String songName) {

        if (receiverId == 0 || senderId == 0 || songName == null)
            return;

        final Like like = new Like();
        like.setRead(NotificationBase.NEW);
        like.setHostId(senderId);
        like.setSystemTime(System.currentTimeMillis());
        like.setTypes(Types.LIKE);

        like.setSongName(songName);

        final Notification notification = getNotification(receiverId);
        if (notification.getNotifications().add(like)) {

            ofy().save().entity(notification).now();
            logger.info("Adding like " + senderId + " " + songName);
            final int count = getNewCount(notification.getNotifications());
            if (count > 0) {

                final String message = "SYNC" + count;
                MessagingEndpoint.getInstance().sendMessage(message, receiverId, senderId);
            }
        }
    }

    @ApiMethod(
            name = "addPush",
            path = "notification/addPush",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void addPush(@Named("container") String container,
                        @Named("firstSongName") String firstSongName,
                        @Named("customMessage") String customMessage,
                        @Named("size") int size,
                        @Named("receiverId") long receiverId,
                        @Named("senderId") long senderId) {

        if (container == null || receiverId == 0 || senderId == 0)
            return;

        final Push push = new Push();
        push.setRead(NotificationBase.NEW);
        push.setHostId(senderId);
        push.setSystemTime(System.currentTimeMillis());
        push.setTypes(Types.PUSH);

        push.setCustomMessage(customMessage);
        push.setFirstSongName(firstSongName);
        push.setPushContainer(container);
        push.setSize(size);

        final Notification notification = getNotification(receiverId);
        if (notification.getNotifications().add(push)) {

            ofy().save().entity(notification).now();
            logger.info("Adding push " + senderId + " " + firstSongName + " " + size);
            final int count = getNewCount(notification.getNotifications());
            if (count > 0) {

                final String message = "SYNC" + count;
                MessagingEndpoint.getInstance().sendMessage(message, receiverId, senderId);
            }
        }
    }

    /**
     * @param senderId   id of the person who sent the request
     * @param receiverId if of the person who accepted the request
     */
    @ApiMethod(
            name = "addBecameFriends",
            path = "notification/addBecameFriends",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public Friend addBecameFriends(@Named("receiver") long receiverId,
                                   @Named("sender") long senderId,
                                   @Named("accepted") boolean accepted) {

        if (receiverId == 0 || senderId == 0)
            return null;

        final ReachUser sender = ofy().load().type(ReachUser.class).id(senderId).now();
        if (sender == null)
            return null;

        if (!accepted) {
            //permission request got rejected
            final MyString string = MessagingEndpoint.getInstance().handleReplyNew(
                    sender,
                    receiverId,
                    "PERMISSION_REJECTED");
            if (string == null)
                logger.severe("Permission rejection failed");
            return null; //return null always as rejection should not fail (UX)
        }

        final MyString string = MessagingEndpoint.getInstance().handleReplyNew(
                sender,
                receiverId,
                "PERMISSION_GRANTED");
        if (string == null) {
            //if null total failure, "false" is ok
            logger.severe("Permission accept failed");
            return null;
        }

        final BecameFriends forSender = new BecameFriends();
        forSender.setRead(NotificationBase.NEW);
        forSender.setHostId(receiverId);
        forSender.setSystemTime(System.currentTimeMillis());
        forSender.setTypes(Types.BECAME_FRIENDS);

        final BecameFriends forReceiver = new BecameFriends();
        forReceiver.setRead(NotificationBase.NEW);
        forReceiver.setHostId(senderId);
        forReceiver.setSystemTime(System.currentTimeMillis());
        forReceiver.setTypes(Types.BECAME_FRIENDS);

        final Notification n1 = getNotification(receiverId);
        n1.getNotifications().add(forReceiver);

        final Notification n2 = getNotification(senderId);
        n2.getNotifications().add(forSender);
        ofy().save().entities(n1, n2).now();

        logger.info("Adding became friends " + receiverId + " " + senderId);

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(receiverId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final byte[] value = (byte[]) syncCache.get(receiverId);
        final long currentTime = System.currentTimeMillis();
        final long lastSeen;
        if (value == null || value.length == 0)
            lastSeen = currentTime;
        else {
            final String val = new String(value);
            if (val.equals(""))
                lastSeen = currentTime;
            else
                lastSeen = currentTime - Long.parseLong(val);
        }

        return new Friend(sender, true, lastSeen);
    }

    /**
     * @param receiverId     the person who accepted the push
     * @param senderId       the person who had made the push
     * @param firstSongName  to display in UI
     * @param size           to display in UI
     * @param notificationId old hash of push container
     */
    @ApiMethod(
            name = "pushAccepted",
            path = "notification/pushAccepted",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void pushAccepted(@Named("receiverId") long receiverId,
                             @Named("senderId") long senderId,
                             @Named("size") int size,
                             @Named("notificationId") int notificationId,
                             @Named("firstSongName") String firstSongName) {

        if (receiverId == 0 || senderId == 0 || size == 0 || notificationId == 0 || firstSongName == null)
            return;

        final PushAccepted pushAccepted = new PushAccepted();
        pushAccepted.setRead(NotificationBase.NEW);
        pushAccepted.setHostId(senderId);
        pushAccepted.setSystemTime(System.currentTimeMillis());
        pushAccepted.setTypes(Types.PUSH_ACCEPTED);

        pushAccepted.setFirstSongName(firstSongName);
        pushAccepted.setSize(size);

        //also remove
        final Notification notification = getNotification(receiverId);
        final Iterator<NotificationBase> queue = notification.getNotifications().iterator();
        while (queue.hasNext()) {
            final NotificationBase base = queue.next();
            logger.info(base.getNotificationId() + " " + notificationId);
            if (base.getNotificationId() == notificationId) {
                queue.remove();
                break;
            }
        }

        notification.getNotifications().add(pushAccepted);
        ofy().save().entity(notification).now();
        logger.info("Adding pushAccepted " + senderId + " " + firstSongName + " " + size);
    }

    @ApiMethod(
            name = "removeNotification",
            path = "notification/removeNotification",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void removeNotification(@Named("receiverId") long receiverId,
                                   @Named("notificationId") int notificationId) {

        if (receiverId == 0 || notificationId == 0)
            return;
        final Notification notification = getNotification(receiverId);
        final Iterator<NotificationBase> queue = notification.getNotifications().iterator();
        while (queue.hasNext()) {
            final NotificationBase base = queue.next();
            logger.info(base.getNotificationId() + " " + notificationId);
            if (base.getNotificationId() == notificationId) {
                queue.remove();
                break;
            }
        }

        ofy().save().entity(notification).now();
    }

    @ApiMethod(
            name = "markRead",
            path = "notification/markRead",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void markRead(@Named("receiverId") long receiverId,
                         @Named("notificationId") int notificationId) {

        if (receiverId == 0 || notificationId == 0)
            return;
        final Notification notification = getNotification(receiverId);
        for (NotificationBase base : notification.getNotifications()) {
            logger.info(base.getNotificationId() + " " + notificationId);
            if (base.getNotificationId() == notificationId) {
                base.setRead(NotificationBase.READ);
                break;
            }
        }

        ofy().save().entity(notification).now();
    }

    @ApiMethod(
            name = "markAllRead",
            path = "notification/markAllRead",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void markAllNotificationsRead(@Named("receiverId") long receiverId) {

        if (receiverId == 0)
            return;
        final Notification notification = getNotification(receiverId);
        for (NotificationBase base : notification.getNotifications())
            base.setRead(NotificationBase.READ);

        ofy().save().entity(notification).now();
    }

    private Notification getNotification(long id) {

        Notification notification = ofy().load().type(Notification.class).id(id).now();
        if (notification == null || notification.getNotifications() == null) {
            notification = new Notification();
            notification.setId(id);
            notification.initializeCollection();
        }
        return notification;
    }
}