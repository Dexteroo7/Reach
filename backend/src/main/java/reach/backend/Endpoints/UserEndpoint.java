package reach.backend.Endpoints;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;

import reach.backend.Entities.User;
import reach.backend.ObjectWrappers.MyString;
import reach.backend.ObjectWrappers.MyUser;
import reach.backend.ObjectWrappers.ReachPlayList;
import reach.backend.ObjectWrappers.ReachSong;
import reach.backend.OfyService;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p/>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "userApi",
        version = "v1",
        resource = "user",
        namespace = @ApiNamespace(
                ownerDomain = "Entities.backend.reach",
                ownerName = "Entities.backend.reach",
                packagePath = ""
        )
)
public class UserEndpoint {

    private static final Logger logger = Logger.getLogger(UserEndpoint.class.getName());
    private static final int DEFAULT_LIST_LIMIT = 20;

    static {
        ObjectifyService.register(User.class);
    }

    @ApiMethod(
            name = "returnUsers",
            path = "user/{listOfNumbers}/{clientId}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public List<MyUser> returnUsers(@Named("listOfNumbers") String numbers, @Named("clientId") long clientId) {

        final ThreadFactory threadFactory = ThreadManager.currentRequestThreadFactory();
        final Stack<Future<Pair<User, User>>> pairStack = new Stack<>();

        final List<MyUser> toSend = new ArrayList<>();
        final List<User> users = OfyService.ofy().load().type(User.class).list();

        try {
            for (final User user : users) {

                if(user == null) continue;

                final FutureTask<Pair<User, User>> futureTask = new FutureTask<>(new Callable<Pair<User, User>>() {
                    @Override
                    public Pair<User, User> call() throws Exception {
                        return new Pair<>(new MessagingEndpoint().sendGCM("PING " + user.getId(), user, 0), user);
                    }
                });
                pairStack.push(futureTask);
                threadFactory.newThread(futureTask).start();

                final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
                syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
                final long currentTime = new Date().getTime();
                final byte[] value = (byte[]) syncCache.get(user.getId());
                final String val;

                if (!user.getId().equals(clientId) && (value != null && value.length > 0) && !((val = new String(value)).equals("")))
                    toSend.add(new MyUser(user, currentTime - Long.parseLong(val), false));
                else if(!user.getId().equals(clientId))
                    toSend.add(new MyUser(user, 0, false));
//            for (String number : numbers) {
//                if (user.getPhoneNumber().contains(number) || number.contains(user.getPhoneNumber())) {
//                    toSend.add(user);
//                    LOG.info("added" + user.getPhoneNumber());
//                }
//            }

            }
            while (!pairStack.isEmpty()) {

                final Pair<User, User> toCheck;
                try {
                    toCheck = pairStack.pop().get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    break;
                }
                final User newUser = toCheck.getFirst();
                final User oldValue = toCheck.getSecond();
                if(newUser == null || oldValue == null) break;

                if(newUser.getGcmId() == null || newUser.getGcmId().equals("") || !newUser.getGcmId().equals(oldValue.getGcmId())) {
                    logger.info("GCM expired for " + newUser.getUserName());
                    OfyService.ofy().save().entities(newUser).now();
                }
            }
            return toSend;
        } finally {
            pairStack.clear();
            users.clear();
        }
    }

    @ApiMethod(
            name = "getSongs",
            path = "user/songs/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public List<ReachSong> getSongs(@Named("id") long id) {
        // Implement this function
        return ofy().load().type(User.class).id(id).now().getMySongs();
    }

    @ApiMethod(
            name = "getPlayLists",
            path = "user/playLists/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public List<ReachPlayList> getPlayLists(@Named("id") long id) {
        // Implement this function
        return ofy().load().type(User.class).id(id).now().getMyPlayLists();
    }

    @ApiMethod(
            name = "get",
            path = "user/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public User get(@Named("id") Long id) throws NotFoundException {
        logger.info("Getting User with ID: " + id);
        User user = ofy().load().type(User.class).id(id).now();
        if (user == null) {
            throw new NotFoundException("Could not find User with ID: " + id);
        }
        return user;
    }

    @ApiMethod(
            name = "insert",
            path = "user",
            httpMethod = ApiMethod.HttpMethod.POST)
    public User insert(User user) {

        for (User toCheck : ofy().load().type(User.class).list())
            if (toCheck.getPhoneNumber().contains(user.getPhoneNumber()) ||
                    user.getPhoneNumber().contains(toCheck.getPhoneNumber()) ||
                    user.getDeviceId().equals(toCheck.getDeviceId())) {

                user.setId(toCheck.getId());
                return user;
            }
        logger.info("Created User with ID: " + user.getId());

        return ofy().load().entity(user).now();
    }

    @ApiMethod(
            name = "getGcmId",
            path = "user/gcmId/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MyString getGCMId(@Named("id") long id) {

        User user = OfyService.ofy().load().type(User.class).id(id).now();
        if(user == null) return new MyString("user_deleted");
        else if(user.getGcmId() == null ||
                user.getGcmId().equals("") ||
                user.getGcmId().equals("hello_world")) return new MyString("hello_world");
        else return new MyString(user.getGcmId());
    }

    @ApiMethod(
            name = "updateMusic",
            path = "user/updateMusic",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public User updateMusic(@Nonnull User container) {

        logger.info(container.getId() + " processing for this id");
        User userToSave = OfyService.ofy().load().type(User.class).id(container.getId()).now();

        if(userToSave == null)
            logger.info(container.getId() + " no user found");
        else {
            logger.info("updating user " + userToSave.getUserName());
            userToSave.setGenres(container.getGenres());
            userToSave.setMySongs(container.getMySongs());
            userToSave.setMyPlayLists(container.getMyPlayLists());
            ofy().save().entity(userToSave).now();
        }
        return userToSave;
    }

    @ApiMethod(
            name = "setGCMId",
            path = "user/gcmId/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void setGCMId(@Named("id") long id, @Named("gcmId") String gcmId) {

        logger.info(id + " processing for this id");
        final User user = OfyService.ofy().load().type(User.class).id(id).now();
        user.setGcmId(gcmId);
        ofy().save().entity(user).now();
    }

    @ApiMethod(
            name = "update",
            path = "user/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public User update(@Named("id") Long id, User user) throws NotFoundException {
        checkExists(id);
        ofy().save().entity(user).now();
        logger.info("Updated User: " + user);
        return ofy().load().entity(user).now();
    }

    @ApiMethod(
            name = "toggleVisibility",
            path = "user/visibility/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void toggleVisibility(@Named("id") long id, @Named("fileHash") String fileHash) {

        User user = OfyService.ofy().load().type(User.class).id(id).now();

        for(ReachSong reachSong : user.getMySongs())

            if(reachSong.getFileHash().equals(fileHash)) {
                if(reachSong.getVisibility() == 1) reachSong.setVisibility((short) 0);
                else reachSong.setVisibility((short) 1);
                return;
            }
    }

    @ApiMethod(
            name = "remove",
            path = "user/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(User.class).id(id).now();
        logger.info("Deleted User with ID: " + id);
    }

    @ApiMethod(
            name = "list",
            path = "user",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<User> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<User> query = ofy().load().type(User.class).limit(limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<User> queryIterator = query.iterator();
        List<User> userList = new ArrayList<>(limit);
        while (queryIterator.hasNext()) {
            userList.add(queryIterator.next());
        }
        return CollectionResponse.<User>builder().setItems(userList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(User.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find User with ID: " + id);
        }
    }
}