package reach.backend.Endpoints;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.repackaged.com.google.common.base.Pair;

import java.io.IOException;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;

import reach.backend.Entities.User;
import reach.backend.ObjectWrappers.MyBoolean;

import static reach.backend.OfyService.ofy;

/**
 * An endpoint to send messages to devices registered with the backend
 * <p/>
 * For more information, see
 * https://developers.google.com/appengine/docs/java/endpoints/
 * <p/>
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

public class MessagingEndpoint {

    private static final Logger log = Logger.getLogger(MessagingEndpoint.class.getName());
    private static final String API_KEY = System.getProperty("gcm.api.key");
//    private static final String API = "AIzaSyAYAjGP-6Xsz06ElmSv8yABvb8u6HFOP7Y";

    //TODO handle errors like 0 serverId on client side
    public MyBoolean sendMessage(@Named("message") final String message,
                                 @Named("id") final long id,
                                 @Named("userId") final long userId) {

        //2 variables self and host
        final ThreadFactory threadFactory = ThreadManager.currentRequestThreadFactory();
        final User client = ofy().load().type(User.class).id(userId).now();
        final User other = ofy().load().type(User.class).id(id).now();

        //I don't remember why I put userName check but lets keep it
        boolean first = (client == null || client.getGcmId() == null || client.getUserName() == null);
        boolean second = (other == null  || other.getGcmId() == null  || other.getUserName() == null);
        if(first || second) {
            log.info("USER DATA NULL " + id + " " + userId);
            return new MyBoolean(first, second);
        }

        final FutureTask<User> self = new FutureTask<>(new Callable<User>() {
            @Override
            public User call() throws Exception {
                return actualSendMessage("Hello world", client);
            }
        });
        final FutureTask<User> host = new FutureTask<>(new Callable<User>() {
            @Override
            public User call() throws Exception {
                return actualSendMessage(message, other);
            }
        });
        threadFactory.newThread(self).start();
        threadFactory.newThread(host).start();

        try {

            final User temp1 = self.get();
            final User temp2 = host.get();

            first = (temp1 == null || temp1.getGcmId() == null || temp1.getGcmId().equals("") || !temp1.getGcmId().equals(client.getGcmId()));
            second = (temp2 == null || temp2.getGcmId() == null || temp2.getGcmId().equals("") || !temp2.getGcmId().equals(other.getGcmId()));
            if(first && temp1 != null) {
                ofy().save().entities(temp1).now();
            }
            if(second && temp2 != null) {
                ofy().save().entities(temp2).now();
            }
            return new MyBoolean(first, second);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private User actualSendMessage (String message, User user) {

        if (message == null || message.equals("") || message.trim().length() == 0) {
            log.info("Not sending message because it is empty");
            return null;
        }

        if(user == null || user.getGcmId() == null || user.getGcmId().equals("")) return null;

        final Sender sender = new Sender(API_KEY);
        final Message msg = new Message.Builder().addData("message", message).build();
        log.info(msg + "");

        final Result result;
        try {
            result = sender.send(msg, user.getGcmId(), 5);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        log.info(result.toString() + " RESULT");

        if (result.getMessageId() != null) {
            log.info("Message sent to " + user.getId());
            final String canonicalRegId = result.getCanonicalRegistrationId();
            if (canonicalRegId != null) {
                // if the regId changed, we have to update the datastore
                log.info("Registration Id changed for " + user.getId() + " updating to " + canonicalRegId);
                user.setGcmId(canonicalRegId);
            }
        } else {
            final String error = result.getErrorCodeName();
            if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                log.info("Registration Id " + user.getId() + " no longer registered with GCM, removing from datastore");
                // if the device is no longer registered with Gcm, remove it from the datastore
                user.setGcmId("");
            } else {
                log.info("Error when sending message : " + error);
            }
        }
        return user;
    }

    protected User sendGCM(String message, User user, int retryLimit) {
        if(retryLimit++ > 6 || user == null) return null;
        final User temp = actualSendMessage(message, user);
        if(temp == null) return sendGCM(message, user, retryLimit);
        else return temp;
    }

    public void handleAnnounce(@Named("hostId") final long hostId) {

        final ThreadFactory threadFactory = ThreadManager.currentRequestThreadFactory();
        final Stack<Future<Pair<User, User>>> pairStack = new Stack<>();

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();

        final String currentTime = System.currentTimeMillis() + "";
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(hostId, currentTime.getBytes(), Expiration.byDeltaSeconds(15 * 60), MemcacheService.SetPolicy.SET_ALWAYS);
        final User hostUser = ofy().load().type(User.class).id(hostId).now();
        if(hostUser == null || hostUser.getFriends() == null || hostUser.getFriends().equals("")) {

            log.info(hostId + "");
            return;
        }

        for(final String friend : hostUser.getFriends().split(" ")) {

            final User user = ofy().load().type(User.class).id(friend).now();
            if(user == null) {
                log.info("USER DATA NULL " + friend);
                continue;
            }
            final FutureTask<Pair<User, User>> futureTask = new FutureTask<>(new Callable<Pair<User, User>>() {
                @Override
                public Pair<User, User> call() throws Exception {
                    return new Pair<>(sendGCM("PONG " + hostId, user, 0), user);
                }
            });
            pairStack.push(futureTask);
            threadFactory.newThread(futureTask).start();
        }

        while (!pairStack.isEmpty()) {

            final Pair<User, User> toCheck;
            try {
                toCheck = pairStack.pop().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                continue;
            }

            final User newUser = toCheck.getFirst();
            final User oldValue = toCheck.getSecond();
            if(newUser == null || oldValue == null) continue;

            if(newUser.getGcmId() == null || newUser.getGcmId().equals("") || !newUser.getGcmId().equals(oldValue.getGcmId())) {
                log.info("GCM expired for " + newUser.getUserName());
                ofy().save().entities(newUser).now();
            }
        }
        pairStack.clear();
    }

    public void handleReply(@Named("clientId") final long clientId,
                            @Named("hostId") final long hostId,
                            @Named("type") final String type) {

        final ThreadFactory threadFactory = ThreadManager.currentRequestThreadFactory();

        final User client = ofy().load().type(User.class).id(clientId).now();
        final User host = ofy().load().type(User.class).id(hostId).now();

        if(client == null || client.getGcmId() == null || client.getUserName() == null ||
           host == null   || host.getGcmId() == null   || host.getUserName() == null) {
            log.info("Error handling reply " + hostId + " " + clientId);
            return;
        }

        final FutureTask<User> futureTask = new FutureTask<>(new Callable<User>() {
            @Override
            public User call() throws Exception {
                return sendGCM(type + "`" + hostId + "`" + host.getUserName(), client, 0);
            }
        });
        threadFactory.newThread(futureTask).start();

        if(client.getWhoCanIAccess().contains(hostId + "")) return;
        if(type.equals("PERMISSION_GRANTED")) client.setWhoCanIAccess(client.getWhoCanIAccess() + " " + hostId);
        ofy().save().entities(client).now();

        try {
            final User newUser = futureTask.get();
            if(newUser == null) return;

            if(newUser.getGcmId() == null || newUser.getGcmId().equals("") || !newUser.getGcmId().equals(client.getGcmId())) {
                ofy().save().entities(newUser).now();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}