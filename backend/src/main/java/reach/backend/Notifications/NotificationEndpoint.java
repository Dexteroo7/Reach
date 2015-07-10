package reach.backend.Notifications;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.repackaged.com.google.common.collect.EvictingQueue;

import java.util.Iterator;
import java.util.logging.Logger;

import javax.inject.Named;

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

    private static final int DEFAULT_LIST_LIMIT = 30;

    /**
     * Returns the {@link Notification} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code Notification} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "notification/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public EvictingQueue<NotificationBase> get(@Named("id") long id) throws NotFoundException {
        logger.info("Getting Notification with ID: " + id);
        Notification notification = ofy().load().type(Notification.class).id(id).now();
        if (notification == null) {
            throw new NotFoundException("Could not find Notification with ID: " + id);
        }
        return notification.getNotifications();
    }

    /**
     * Adds a like notification to the list
     *
     * @param id   person whose notification will be added
     * @param like the notification
     */
    @ApiMethod(
            name = "addLike",
            path = "notification/addLike",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void addLike(Like like, @Named("id") long id) {

        Notification notification = ofy().load().type(Notification.class).id(id).now();
        if (notification == null) {
            notification = new Notification();
            notification.setId(id);
            notification.setNotifications(EvictingQueue.<NotificationBase>create(DEFAULT_LIST_LIMIT));
        }
        notification.getNotifications().add(like);
        ofy().save().entity(notification).now();
        logger.info("Adding Notification with ID " + notification.getId());
    }

    /**
     * Adds a push notification to the list
     *
     * @param id   person whose notification will be added
     * @param push the notification
     */
    @ApiMethod(
            name = "addPush",
            path = "notification/addPush",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void addPush(Push push, @Named("id") long id) {

        Notification notification = ofy().load().type(Notification.class).id(id).now();
        if (notification == null) {
            notification = new Notification();
            notification.setId(id);
            notification.setNotifications(EvictingQueue.<NotificationBase>create(DEFAULT_LIST_LIMIT));
        }
        notification.getNotifications().add(push);
        ofy().save().entity(notification).now();
        logger.info("Adding Notification with ID " + notification.getId());
    }

    /**
     * Adds a becameFriends notification to the list
     *
     * @param id            person whose notification will be added
     * @param becameFriends the notification
     */
    @ApiMethod(
            name = "addBecameFriends",
            path = "notification/addBecameFriends",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void addBecameFriends(BecameFriends becameFriends, @Named("id") long id) {

        Notification notification = ofy().load().type(Notification.class).id(id).now();
        if (notification == null) {
            notification = new Notification();
            notification.setId(id);
            notification.setNotifications(EvictingQueue.<NotificationBase>create(DEFAULT_LIST_LIMIT));
        }
        notification.getNotifications().add(becameFriends);
        ofy().save().entity(notification).now();
        logger.info("Adding Notification with ID " + notification.getId());
    }

    /**
     * Adds a pushAccepted notification to the list
     *
     * @param id           person whose notification will be added
     * @param oldHash      person whose notification will be added
     * @param pushAccepted the notification
     */
    @ApiMethod(
            name = "pushAccepted",
            path = "notification/pushAccepted",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void pushAccepted(PushAccepted pushAccepted,
                             @Named("oldHash") int oldHash,
                             @Named("id") long id) {

        Notification notification = ofy().load().type(Notification.class).id(id).now();
        if (notification == null) {
            notification = new Notification();
            notification.setId(id);
            notification.setNotifications(EvictingQueue.<NotificationBase>create(DEFAULT_LIST_LIMIT));
        }

        //first remove old shit
        final Iterator<NotificationBase> baseIterator = notification.getNotifications().iterator();
        while (baseIterator.hasNext()) {

            logger.info("iterating notifications");
            if (baseIterator.next().hashCode() == oldHash) {
                baseIterator.remove();
                logger.info("found and removed old push");
            }
        }

        notification.getNotifications().add(pushAccepted);
        ofy().save().entity(notification).now();
        logger.info("Adding Notification with ID " + notification.getId());
    }

}