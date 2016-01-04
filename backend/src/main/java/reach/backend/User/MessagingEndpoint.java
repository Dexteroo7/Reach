package reach.backend.User;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.log.InvalidRequestException;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.cmd.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.inject.Named;

import reach.backend.MiscUtils;
import reach.backend.Notifications.Notification;
import reach.backend.Notifications.NotificationBase;
import reach.backend.Notifications.NotificationEndpoint;
import reach.backend.Notifications.Push;
import reach.backend.Notifications.Types;
import reach.backend.ObjectWrappers.MyBoolean;
import reach.backend.ObjectWrappers.MyString;
import reach.backend.TextUtils;

import static reach.backend.OfyService.ofy;

/**
 * An endpoint to send messages to devices registered with the backend
 * <p>
 * For more information, see
 * https://developers.google.com/appengine/docs/java/endpoints/
 * <p>
 * NOTE: This endpoint does not use any form of authorization or
 * authentication! If this app is deployed, anyone can access this endpoint! If
 * you'd like to add authentication, take a look at the documentation.
 */
@Api(
        name = "messaging",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "Entities.backend.reach",
                ownerName = "Entities.backend.reach",
                packagePath = ""
        )
)
/*
1) CLIENT IS ALWAYS THE PERSON WHO HAS REST CALLED
2) HOST IS ALWAYS THE PERSON WHO THE CLIENT WANTS TO INTERACT WITH
 */
public class MessagingEndpoint {

    private static final Logger log = Logger.getLogger(MessagingEndpoint.class.getName());
    public static final String API_KEY = System.getProperty("gcm.api.key");
//    private static final String API = "AIzaSyAYAjGP-6Xsz06ElmSv8yABvb8u6HFOP7Y";

    public static MessagingEndpoint messagingEndpoint = null;

    public static MessagingEndpoint getInstance() {
        if (messagingEndpoint == null)
            messagingEndpoint = new MessagingEndpoint();
        return messagingEndpoint;
    }

    private boolean isDevika(String phoneNumber, long id) {

        return phoneNumber.equals("8860872102") || id == 5666668701286400L;
    }

    public MyBoolean sendMessage(@Named("message") final String message,
                                 @Named("hostId") final long hostId,
                                 @Named("clientId") final long clientId) {

        //2 variables self and host
        final ReachUser client = ofy().load().type(ReachUser.class).id(clientId).now();
        final ReachUser host = ofy().load().type(ReachUser.class).id(hostId).now();

        //I don't remember why I put userName check but lets keep it
        boolean first = (client == null || client.getGcmId() == null || client.getUserName() == null);
        boolean second = (host == null || host.getGcmId() == null || host.getUserName() == null);
        if (first || second) {
            log.info("USER DATA NULL " + hostId + " " + clientId);
            return new MyBoolean(first, second);
        }

        final FutureTask<Boolean> hello = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {

                log.info("Sending message to client " + client.getUserName());
                return sendMessage("Hello world", client);
            }
        });
        final FutureTask<Boolean> sendMessage = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                log.info("Sending message to host " + host.getUserName());
                return sendMessage(message, host);
            }
        });
        //random gcm back and forth
        ThreadManager.createThreadForCurrentRequest(hello).start();
        //actual message
        ThreadManager.createThreadForCurrentRequest(sendMessage).start();
        try {
            return new MyBoolean(!hello.get(), !sendMessage.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        log.info("GCM FAILED " + host.getUserName() + " " + client.getUserName());
        return null;
    }

    public MyString requestAccess(@Named("clientId") final long clientId,
                                  @Named("hostId") final long hostId) {

        if (clientId == 0 || hostId == 0)
            return null;

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final ReachUser client = ofy().load().type(ReachUser.class).id(clientId).now();
        final ReachUser host = ofy().load().type(ReachUser.class).id(hostId).now();
        if (client == null || host == null)
            return new MyString("false");

        if ((client.getMyReach() == null || !client.getMyReach().contains(hostId)) &&
                (client.getSentRequests() == null || !client.getSentRequests().contains(hostId))) {

            if (client.getSentRequests() == null)
                client.setSentRequests(new HashSet<Long>());
            log.info("Saving Sent Request on " + client.getUserName() + " " + client.getSentRequests().add(hostId));
            ofy().save().entities(client).now();
        }

        if ((host.getMyReach() == null || !host.getMyReach().contains(clientId)) &&
                (host.getReceivedRequests() == null || !host.getReceivedRequests().contains(clientId))) {

            if (host.getReceivedRequests() == null)
                host.setReceivedRequests(new HashSet<Long>());
            log.info("Saving Received Request on " + host.getUserName() + " " + host.getReceivedRequests().add(clientId));
            ofy().save().entities(host).now();
        }

        if (isDevika(host.getPhoneNumber(), hostId)) {
            //DO NOT SEND GCM, Devika does not have a gcm id !
            NotificationEndpoint.getInstance().addBecameFriends(hostId, clientId, true);
            return new MyString("true");
        }

        if (host.getGcmId() == null || host.getGcmId().equals("")) {
            log.info("Error handling reply " + hostId + " " + clientId);
            return new MyString("false");
        }

        return new MyString(sendMessage("PERMISSION_REQUEST`" + clientId + "`" + client.getUserName(), host) + "");
    }

    public MyString handleReplyNew(ReachUser sender,
                                   final @Named("clientId") long clientId,
                                   @Named("type") String type) {

        final ReachUser client = ofy().load().type(ReachUser.class).id(clientId).now();
        if (client == null)
            return null;

        //remove request from both received and sent
        if (client.getReceivedRequests() != null)
            client.getReceivedRequests().remove(sender.getId());
        if (client.getSentRequests() != null)
            client.getSentRequests().remove(sender.getId());

        //remove request from both received and sent
        if (sender.getSentRequests() != null)
            sender.getSentRequests().remove(clientId);
        if (sender.getReceivedRequests() != null)
            sender.getReceivedRequests().remove(clientId);

        if (type.equals("PERMISSION_GRANTED")) {

            //adding both parties to each others reach :)
            if (client.getMyReach() == null)
                client.setMyReach(new HashSet<Long>());
            log.info("Adding MyReach To " + client.getUserName() + " " + client.getMyReach().add(sender.getId()));
            if (sender.getMyReach() == null)
                sender.setMyReach(new HashSet<Long>());
            log.info("Adding MyReach To " + sender.getUserName() + " " + sender.getMyReach().add(clientId));
        } else {

            //remove from friends
            if (client.getMyReach() == null)
                client.setMyReach(new HashSet<Long>());
            log.severe("Removing from reach " + client.getUserName() + " " + client.getMyReach().remove(sender.getId()));
            if (sender.getMyReach() == null)
                sender.setMyReach(new HashSet<Long>());
            log.severe("Removing from reach " + sender.getUserName() + " " + sender.getMyReach().remove(clientId));
        }

        ofy().save().entities(client, sender).now();

        //DO NOT SEND GCM, Devika does not have a gcm id !
        if (isDevika(sender.getPhoneNumber(), sender.getId()))
            return new MyString("true");

        if (sender.getGcmId() == null || sender.getGcmId().equals("")) {

            log.info("Error handling reply " + sender.getId() + " " + clientId);
            return new MyString("false");
        }

        //send message to the "sender" (the person who made the request)
        return new MyString(sendMessage(type + "`" + clientId + "`" + client.getUserName(), sender) + "");
    }

    public MyString sendManualNotification(@Named("hostId") long hostId,
                                           @Named("type") int type,
                                           @Named("message") String message,
                                           @Named("heading") String heading) {
        return new MyString(sendMessage("MANUAL`" + type + "`" + heading + "`" + message, ofy().load().type(ReachUser.class).id(hostId).now()) + "");
    }

    public MyString sendChat(@Named("hostId") long hostId) {

        final ReachUser user = ofy().load().type(ReachUser.class).id(hostId).now();
        return new MyString(sendMessage("CHAT", user) + "");
    }

    public MyString sendBulkChat() {

        final Sender sender = new Sender(API_KEY);
        final Message actualMessage = new Message.Builder()
                .addData("message", "CHAT")
                .build();

        return new MyString(sendMultiCastMessage(
                actualMessage,
                sender,
                ofy().load().type(ReachUser.class)
                        .filter("gcmId !=", "")
                        .project("gcmId")) + "");
    }

    protected boolean sendMessage(@Nonnull String message,
                                  @Nonnull ReachUser user) {

        //ensure that the User is not null and message is not shit, beforehand
        final Result result;
        try {
            result = new Sender(API_KEY).send(new Message.Builder().addData("message", message).build(), user.getGcmId(), 5);
        } catch (IOException | IllegalArgumentException | InvalidRequestException e) {
            /*
            Because of the possibility of an IOException which in no way implies that
            the gcmIds WERE corrupt we have to recurse.
             */
            e.printStackTrace();
            log.info(e.getLocalizedMessage() + " error");
            return false;
        }
        if (result.getMessageId() != null) {

            log.info("Message sent to " + user.getId());
            if (result.getCanonicalRegistrationId() != null && !result.getCanonicalRegistrationId().equals("")) {
                // if the regId changed, we have to update the data-store
                log.info("Registration Id changed for " + user.getId() + " updating to " + result.getCanonicalRegistrationId());
                user.setGcmId(result.getCanonicalRegistrationId());
                ofy().save().entities(user);
            }
        } else {

            if (result.getErrorCodeName().equals(Constants.ERROR_NOT_REGISTERED) ||
                    result.getErrorCodeName().equals(Constants.ERROR_INVALID_REGISTRATION) ||
                    result.getErrorCodeName().equals(Constants.ERROR_MISMATCH_SENDER_ID) ||
                    result.getErrorCodeName().equals(Constants.ERROR_MISSING_REGISTRATION)) {

                log.info("Registration Id " + user.getId() + " no longer registered with GCM, removing from data-store");
                // if the device is no longer registered with Gcm, remove it from the data-store
                user.setGcmId("");
                ofy().save().entities(user);
            } else {
                log.info("Error when sending message : " + result.getErrorCodeName());
            }
        }
        return result.getMessageId() != null;
    }

    //MultiCast messages
    public MyString notifyAll(@Named("type") int type,
                              @Named("message") String message,
                              @Named("heading") String heading) {

        final Sender sender = new Sender(API_KEY);
        final Message actualMessage = new Message.Builder()
                .addData("message", "MANUAL`" + type + "`" + heading + "`" + message)
                .build();

        return new MyString(sendMultiCastMessage(
                actualMessage,
                sender,
                ofy().load().type(ReachUser.class)
                        .filter("gcmId !=", "")
                        .project("gcmId")) + " Result");
    }

    public void handleAnnounce(@Named("clientId") final long clientId,
                               @Named("networkType") final String networkType) {

        if (clientId == 0)
            return;
        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final ReachUser clientUser = ofy().load().type(ReachUser.class).id(clientId).now();
        if (clientUser == null || clientUser.getMyReach() == null || clientUser.getMyReach().isEmpty())
            return;

        log.info(clientUser.getUserName() + " sending PONG");
        final ImmutableList.Builder<Key> keysBuilder = new ImmutableList.Builder<>();
        for (Long id : clientUser.getMyReach())
            keysBuilder.add(Key.create(ReachUser.class, id));

        final Sender sender = new Sender(API_KEY);
        final Message message = new Message.Builder()
                .addData("message", "PONG " + clientId + " " + networkType)
                .build();

        sendMultiCastMessage(
                message,
                sender,
                ofy().load().type(ReachUser.class)
                        .filterKey("in", keysBuilder.build())
                        .filter("gcmId !=", "")
                        .project("gcmId"));
    }

    protected boolean sendMultiCastMessage(@Nonnull Message message,
                                           @Nonnull Sender sender,
                                           @Nonnull Query<ReachUser> userQuery) {

        final List<ReachUser> users = new ArrayList<>(1000);
        final List<String> regIds = new ArrayList<>(1000);
        int totalSize = 0;
        boolean result = true;

        for (ReachUser reachUser : userQuery) {

            users.add(reachUser);
            regIds.add(reachUser.getGcmId());
            totalSize++;

            //sendMultiCast doesn't take more than 1000 at a time
            if (totalSize > 999) {

                if (regIds.size() > 0 && users.size() > 0 && regIds.size() == users.size()) {

                    result = result && actualSendMultiCastMessage(message, users, regIds, sender);
                    if (result)
                        log.info("refreshed gcm of " + totalSize);
                    else
                        log.log(Level.SEVERE, "multi-cast failed");
                } else
                    log.info("size conflict");

                try {
                    //wait a little
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                } finally {
                    totalSize = 0;
                    users.clear();
                    regIds.clear();
                }
            }
        }

        if (regIds.size() > 0 && users.size() > 0 && regIds.size() == users.size()) {

            result = result && actualSendMultiCastMessage(message, users, regIds, sender);
            if (result)
                log.info("refreshed gcm of " + totalSize);
            else
                log.log(Level.SEVERE, "multi-cast failed");
        }
        log.info("finished");
        return result;
    }

    private boolean actualSendMultiCastMessage(@Nonnull Message message,
                                               @Nonnull List<ReachUser> users,
                                               @Nonnull List<String> regIds,
                                               @Nonnull Sender sender) {

        if (users.size() > 1000 || regIds.size() > 1000)
            return false;

        //ensure that the User is not null and message is not shit, beforehand
        final MulticastResult multicastResult;

        try {
            multicastResult = sender.send(message, regIds, 5);
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            log.log(Level.SEVERE, e.getLocalizedMessage() + " error");
            return false;
        }

        final List<Result> results = multicastResult.getResults();
        final int resultLength = results.size();

        if (resultLength != users.size()) {
            log.log(Level.SEVERE, "Multi-part messages size different error");
            return false;
        }

        final List<LoadResult<ReachUser>> toChange = new ArrayList<>(100);
        final List<String> toPut = new ArrayList<>(100);

        for (int index = 0; index < resultLength; index++) {

            final Result result = results.get(index);

            if (!TextUtils.isEmpty(result.getMessageId())) {

                final String canonicalRegistrationId = result.getCanonicalRegistrationId();
                if (!TextUtils.isEmpty(canonicalRegistrationId)) {
                    // if the regId changed, we have to update the data-store
                    log.info("Registration Id changed for " + users.get(index).getId() + " updating to " + canonicalRegistrationId);

                    final LoadResult<ReachUser> loadResult = ofy().load().type(ReachUser.class).id(users.get(index).getId());
                    toChange.add(loadResult);
                    toPut.add(canonicalRegistrationId);
                }

            } else {

                if (result.getErrorCodeName().equals(Constants.ERROR_NOT_REGISTERED) ||
                        result.getErrorCodeName().equals(Constants.ERROR_INVALID_REGISTRATION) ||
                        result.getErrorCodeName().equals(Constants.ERROR_MISMATCH_SENDER_ID) ||
                        result.getErrorCodeName().equals(Constants.ERROR_MISSING_REGISTRATION)) {

                    log.info("Registration Id " + users.get(index).getUserName() + " no longer registered with GCM, removing from data-store");
                    // if the device is no longer registered with Gcm, remove it from the data-store
                    final LoadResult<ReachUser> loadResult = ofy().load().type(ReachUser.class).id(users.get(index).getId());
                    toChange.add(loadResult);
                    toPut.add(""); //empty string
                } else {
                    log.info("Error when sending message : " + result.getErrorCodeName());
                }
            }
        }

        final int changeListSize = toChange.size();

        if (changeListSize == 0)
            return true;

        final List<ReachUser> toBatch = new ArrayList<>(changeListSize);

        for (int index = 0; index < changeListSize; index++) {

            final ReachUser reachUser = toChange.get(index).now();
            reachUser.setGcmId(toPut.get(index));
            toBatch.add(reachUser);
        }

        log.info("Changing gcmIds of " + toBatch.size() + " users");
        ofy().save().entities(toBatch);
        return true;
    }

    /**
     * Used for sending a push song to every user from Devika, with love :)
     */

    public MyString devikaPushToAll(@Named("cursor") String cursor,
                                    @Named("limit") int limit,

                                    @Named("container") String container,
                                    @Named("firstSongName") final String firstSongName,
                                    @Named("customMessage") String customMessage,
                                    @Named("size") final int size) {

        log.info("Starting devika push to all");

        final QueryResultIterator<ReachUser> usersToPushTo;
        if (TextUtils.isEmpty(cursor))
            usersToPushTo = ofy().load().type(ReachUser.class)
                    .limit(limit)
                    .filter("gcmId !=", "")
                    .project("gcmId").iterator();
        else
            usersToPushTo = ofy().load().type(ReachUser.class)
                    .limit(limit)
                    .filter("gcmId !=", "")
                    .project("gcmId")
                    .startAt(Cursor.fromWebSafeString(cursor)).iterator();

        //create the push notification
        final Push push = new Push();
        push.setRead(NotificationBase.NEW);
        push.setHostId(reach.backend.Constants.devikaId); //sender is Devika
        push.setSystemTime(System.currentTimeMillis());
        push.setTypes(Types.PUSH);

        push.setCustomMessage(customMessage);
        push.setFirstSongName(firstSongName);
        push.setPushContainer(container);
        push.setSize(size);

        final List<Notification> notificationToSave = new ArrayList<>(limit);
        final List<Long> receiverIds = new ArrayList<>(limit);
        while (usersToPushTo.hasNext())
            receiverIds.add(usersToPushTo.next().getId());

        if (receiverIds.isEmpty())
            return new MyString("-1");

        log.info("Adding push to " + MiscUtils.seqToString(receiverIds) + " users");

        int processCount = 0;
        //load all notifications and add push to each
        final Map<Long, Notification> notificationMap = ofy().load().type(Notification.class).ids(receiverIds);
        for (Map.Entry<Long, Notification> entry : notificationMap.entrySet()) {

            final Long receiverId = entry.getKey();
            Notification notification = entry.getValue();

            processCount++;
            //instantiate if null / not present
            notification = getNotification(notification, receiverId);
            //add the push notification
            if (notification.getNotifications().add(push)) {

                //add to save batch
                notificationToSave.add(notification);
                log.info("Adding push " + reach.backend.Constants.devikaId + " " + firstSongName + " " + size);
            }
        }

        if (notificationToSave.isEmpty())
            return new MyString(usersToPushTo.getCursor().toWebSafeString());

        log.info("Batch save of " + notificationToSave.size() + " notifications");

        //save "new" notification collections asynchronously
        ofy().save().entities(notificationToSave);

        log.info("Send notification to reach user");

        //send bulk to required users !
        final Message message = new Message.Builder()
                .addData("message", "SYNC" + 1) //fixed 1 un-read, perf matters !
                .build();
        final Sender sender = new Sender(API_KEY);
        final ImmutableList.Builder<Key> keysBuilder = new ImmutableList.Builder<>();
        for (Notification id : notificationToSave)
            keysBuilder.add(Key.create(ReachUser.class, id.getId()));

        sendMultiCastMessage(message, //the message that needs to be sent
                sender, //the sender id
                ofy().load().type(ReachUser.class)
                        .filterKey("in", keysBuilder.build()) //required users
                        .filter("gcmId !=", "") //not if gcm id is dead
                        .project("gcmId")); //project only the gcmId

        log.info("processed " + processCount);

        return new MyString(usersToPushTo.getCursor().toWebSafeString());
    }

    private Notification getNotification(Notification notification, long id) {

        if (notification == null || notification.getNotifications() == null) {
            notification = new Notification();
            notification.setId(id);
            notification.initializeCollection();
        }
        return notification;
    }
}