package reach.backend.Notifications;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.repackaged.com.google.common.cache.CacheBuilder;
import com.google.appengine.repackaged.com.google.common.cache.CacheLoader;
import com.google.appengine.repackaged.com.google.common.cache.LoadingCache;
import com.google.appengine.repackaged.com.google.common.collect.EvictingQueue;
import com.google.appengine.repackaged.com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.inject.Named;

import reach.backend.User.ReachUser;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p/>
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

    /**
     * Returns the {@link Notification} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     */
    @ApiMethod(
            name = "get",
            path = "notification/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public List<NotificationBase> get(@Named("id") long id) {

        if (id == 0)
            return null;

        logger.info("Getting Notification with ID: " + id);
        final Notification notification = ofy().load().type(Notification.class).id(id).now();
        if (notification == null || notification.getNotifications() == null)
            return null;

        final LoadingCache<Long, ReachUser> cache = CacheBuilder.newBuilder()
                .maximumSize(30)
                .initialCapacity(10)
                .build(new CacheLoader<Long, ReachUser>() {
                    public ReachUser load(@Nonnegative @Nonnull Long id) {
                        return ofy().load().type(ReachUser.class).id(id).now();
                    }
                });

        final ImmutableList.Builder<NotificationBase> builder = new ImmutableList.Builder<>();
        final List<NotificationBase> toReturn;
        boolean needToSave = false;

        for (NotificationBase notificationBase : notification.getNotifications()) {

            final ReachUser reachUser;
            try {
                reachUser = cache.get(notificationBase.getHostId());
                if (reachUser == null)
                    continue;
            } catch (Exception e) {
                logger.info("Error getting notifications " + e.getLocalizedMessage());
                continue;
            }
            //update the notifications
            if (!notificationBase.getImageId().equals(reachUser.getImageId())) {
                notificationBase.setImageId(reachUser.getImageId()); //can change
                needToSave = true;
            }

            if (!notificationBase.getHostName().equals(reachUser.getUserName())) {
                notificationBase.setHostName(reachUser.getUserName()); //can change
                needToSave = true;
            }

            //very imp else if
            if (notificationBase.getRead() == -1) {
                notificationBase.setRead((short) 0); //mark unread but fetched
                needToSave = true;
            } else if (notificationBase.getRead() == 0) {
                notificationBase.setRead((short) 1); //mark read
                needToSave = true;
            }

            builder.add(notificationBase);
        }
        toReturn = builder.build();
        //mark all notifications as read
        if (needToSave)
            ofy().save().entity(notification);

        cache.cleanUp();
        return toReturn;
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

        final ReachUser sender = ofy().load().type(ReachUser.class).id(senderId).now();
        final Like like = new Like();
        like.addBasicData(sender);
        like.setTypes(Types.LIKE);
        like.setSongName(songName);

        ofy().save().entity(getNotification(receiverId, like)).now();
        logger.info("Adding like " + sender.getUserName() + " " + songName);
    }

    /**
     * @param receiverId the person who will receive the push
     * @param senderId   the person who sent the push
     * @param container  blob of required data
     */
    @ApiMethod(
            name = "addPush",
            path = "notification/addPush",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void addPush(@Named("container") String container,
                        @Named("receiverId") long receiverId,
                        @Named("senderId") long senderId) {

        if (container == null || receiverId == 0 || senderId == 0)
            return;

        final ReachUser sender = ofy().load().type(ReachUser.class).id(senderId).now();
        final Push push = new Push();
        push.addBasicData(sender);
        push.setTypes(Types.PUSH);
        push.setPushContainer(container);

        ofy().save().entity(getNotification(receiverId, push)).now();
        logger.info("Adding push " + sender.getUserName());
    }

    /**
     * @param senderId   id of the person who sent the request
     * @param receiverId if of the person who accepted the request
     */
    @ApiMethod(
            name = "addBecameFriends",
            path = "notification/addBecameFriends",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void addBecameFriends(@Named("receiver") long receiverId,
                                 @Named("sender") long senderId) {

        if (receiverId == 0 || senderId == 0)
            return;

        final ReachUser sender = ofy().load().type(ReachUser.class).id(senderId).now();
        final ReachUser receiver = ofy().load().type(ReachUser.class).id(receiverId).now();

        final BecameFriends forSender = new BecameFriends();
        forSender.addBasicData(receiver);
        forSender.setTypes(Types.BECAME_FRIENDS);

        final BecameFriends forReceiver = new BecameFriends();
        forReceiver.addBasicData(sender);
        forReceiver.setTypes(Types.BECAME_FRIENDS);

        //save the notification not the user !!
        ofy().save().entities(getNotification(receiverId, forReceiver),
                getNotification(senderId, forSender)).now();
        logger.info("Adding became friends " + forReceiver.getHostName() + " " + forSender.getHostName());
    }

    /**
     * @param receiverId    the person who accepted the push
     * @param senderId      the person who had made the push
     * @param firstSongName to display in UI
     * @param size          to display in UI
     * @param oldHash       old hash of push container
     * @param firstSongName name of first song for UI shit
     */
    @ApiMethod(
            name = "pushAccepted",
            path = "notification/pushAccepted",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void pushAccepted(@Named("receiverId") long receiverId,
                             @Named("senderId") long senderId,
                             @Named("size") int size,
                             @Named("oldHash") int oldHash,
                             @Named("firstSongName") String firstSongName) {

        if (receiverId == 0 || senderId == 0 || size == 0 || oldHash == 0 || firstSongName == null)
            return;

        final ReachUser sender = ofy().load().type(ReachUser.class).id(senderId).now();
        final PushAccepted pushAccepted = new PushAccepted();
        pushAccepted.addBasicData(sender);
        pushAccepted.setTypes(Types.PUSH_ACCEPTED);
        pushAccepted.setFirstSongName(firstSongName);
        pushAccepted.setSize(size);

        final Notification notification = getNotification(receiverId, pushAccepted);
        final Iterator<NotificationBase> queue = notification.getNotifications().iterator();
        while (queue.hasNext()) {

            final NotificationBase base = queue.next();
            if (base instanceof Push) {
                final int hash = ((Push) base).getPushContainer().hashCode();
                logger.info("iterating " + hash + " " + oldHash);
                if (hash == oldHash) {
                    queue.remove();
                    break;
                }
            }
        }
        ofy().save().entity(notification).now();
        logger.info("Adding push " + sender.getUserName() + " " + firstSongName + " " + size);
    }

    @ApiMethod(
            name = "removePush",
            path = "notification/removePush",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void removePush(@Named("receiverId") long receiverId,
                           @Named("oldHash") int oldHash) {

        if(receiverId == 0 || oldHash == 0)
            return;
        final Notification notification = ofy().load().type(Notification.class).id(receiverId).now();
        if(notification == null || notification.getNotifications() == null || notification.getNotifications().size() == 0)
            return;
        final Iterator<NotificationBase> queue = notification.getNotifications().iterator();
        while (queue.hasNext()) {

            final NotificationBase base = queue.next();
            if (base instanceof Push) {
                final int hash = ((Push) base).getPushContainer().hashCode();
                logger.info("iterating " + hash + " " + oldHash);
                if (hash == oldHash) {
                    queue.remove();
                    break;
                }
            }
        }
        ofy().save().entity(notification).now();
    }

    /**
     * @param id               of the person who will receive this notification
     * @param notificationBase the notification
     * @return the notification entity
     */
    private Notification getNotification(long id, NotificationBase notificationBase) {

        Notification receiverNotification = ofy().load().type(Notification.class).id(id).now();
        if (receiverNotification == null || receiverNotification.getNotifications() == null) {
            receiverNotification = new Notification();
            receiverNotification.setId(id);
            receiverNotification.setNotifications(EvictingQueue.<NotificationBase>create(NotificationBase.DEFAULT_LIST_LIMIT));
        }

        receiverNotification.getNotifications().add(notificationBase);
        return receiverNotification;
    }
}