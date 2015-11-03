package reach.backend.User;

import com.firebase.security.token.TokenGenerator;
import com.firebase.security.token.TokenOptions;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.cmd.Query;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;

import reach.backend.ObjectWrappers.MyString;
import reach.backend.OfyService;
import reach.backend.TextUtils;
import reach.backend.Transactions.CompletedOperation;
import reach.backend.Transactions.CompletedOperations;
import reach.backend.User.FriendContainers.Friend;
import reach.backend.User.FriendContainers.QuickSync;
import reach.backend.User.FriendContainers.ReceivedRequest;

import static reach.backend.OfyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p>
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
public class ReachUserEndpoint {

    private static final Logger logger = Logger.getLogger(ReachUserEndpoint.class.getName());
    @SuppressWarnings("SpellCheckingInspection")
    private static final String FIRE_BASE_SECRET = "0bGwoCDidft2U0aDuK2L6UKi92EfGARMtAP9iC0s";
    private static final int DEFAULT_LIST_LIMIT = 20;

    /**
     * Use this to sync up the phoneBook, only send those numbers which are not known / are new
     *
     * @param contactsWrapper list of phoneNumbers
     * @return list of people on reach
     */
    @ApiMethod(
            name = "phoneBookSync",
            path = "user/phoneBookSync/",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public Set<Friend> phoneBookSync(ContactsWrapper contactsWrapper) {

        final HashSet<String> phoneNumbers;
        if (contactsWrapper == null || (phoneNumbers = contactsWrapper.getContacts()) == null || phoneNumbers.isEmpty())
            return null;

        logger.info(phoneNumbers.size() + " total");
        final HashSet<Friend> friends = new HashSet<>();
        for (ReachUser user : ofy().load().type(ReachUser.class)
                .filter("phoneNumber in", phoneNumbers)
                .filter("gcmId !=", ""))
            friends.add(new Friend(user, false, 0));

        if (phoneNumbers.contains(OfyService.devikaPhoneNumber)) {

            final ReachUser devika = ofy().load().type(ReachUser.class).id(OfyService.devikaId).now();
            friends.add(new Friend(devika, false, 0));
        }

        return friends;
    }

    /**
     * Use this to sync up the phoneBook, only send those numbers which are not known / are new
     *
     * @param phoneNumbers list of phoneNumbers
     * @param serverId     of person making the request
     * @return list of people on reach
     */
    @ApiMethod(
            name = "phoneBookSyncNew",
            path = "user/phoneBookSyncNew/",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public Set<Friend> phoneBookSyncNew(@Named("phoneNumbers") Collection<String> phoneNumbers,
                                        @Named("serverId") long serverId) {

        if (phoneNumbers == null || phoneNumbers.isEmpty())
            return null;
        logger.info(phoneNumbers.size() + " total");

        final Map<String, Boolean> statusMap = new HashMap<>(phoneNumbers.size());

        final HashSet<Friend> friends = new HashSet<>();
        for (ReachUser user : ofy().load().type(ReachUser.class)
                .filter("phoneNumber in", phoneNumbers)
                .filter("gcmId !=", "")) {

            friends.add(new Friend(user, false, 0));
            statusMap.put(user.getPhoneNumber(), true);
        }

        if (phoneNumbers.contains(OfyService.devikaPhoneNumber)) {

            final ReachUser devika = ofy().load().type(ReachUser.class).id(OfyService.devikaId).now();
            friends.add(new Friend(devika, false, 0));
        }

        //do not log phone book if serverId is null
        if (serverId == 0)
            return friends;

        //////////////
        try {

            final JSONArray arrayOfPhoneNumbers = new JSONArray();
            for (String phoneNumber : phoneNumbers) {

                final JSONObject object = new JSONObject();
                object.put("phoneNumber", phoneNumber);
                final Boolean aBoolean = statusMap.get(phoneNumber);
                object.put("status", aBoolean == null ? false : aBoolean);
                arrayOfPhoneNumbers.put(object);
            }

            final ReachUser user = ofy().load().type(ReachUser.class).id(serverId).now();
            final JSONObject phoneBookPost = new JSONObject();

            phoneBookPost.put("secureKey", "ECeQsMORJ1W1yPJ9D9nIy6FwE1rgS1p7");
            phoneBookPost.put("userId", serverId);
            phoneBookPost.put("userName", user.getUserName());
            phoneBookPost.put("phoneNumber", user.getPhoneNumber());
            phoneBookPost.put("phoneBook", arrayOfPhoneNumbers);

            final URL url = new URL("http://img-1052310213.ap-southeast-1.elb.amazonaws.com/reach/addPhoneBook");
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            final OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(phoneBookPost.toString());
            writer.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                logger.info("connection result success");
            else
                logger.info("connection result fail " +
                        connection.getResponseCode() + " " +
                        connection.getResponseMessage());

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            logger.info("Error on post " + e.getLocalizedMessage());
        }

        return friends;
    }

    /**
     * Use this to sync up the phoneBook, only send those numbers which are not known / are new
     *
     * @param phoneNumbers list of phoneNumbers
     * @return list of people on reach
     */
    @ApiMethod(
            name = "phoneBookSyncEvenNew",
            path = "user/phoneBookSyncEvenNew/",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Set<Friend> phoneBookSyncEvenNew(@Nonnull StringList phoneNumbers) {

        logger.info("Starting phoneBookSyncEvenNew");


        if (phoneNumbers.getStringList().isEmpty())
            return null;
        logger.info(phoneNumbers.getStringList().size() + " total");

        final Map<String, Boolean> statusMap = new HashMap<>(phoneNumbers.getStringList().size());

        final HashSet<Friend> friends = new HashSet<>();
        for (ReachUser user : ofy().load().type(ReachUser.class)
                .filter("phoneNumber in", phoneNumbers.getStringList())
                .filter("gcmId !=", "")) {

            friends.add(new Friend(user, false, 0));
            statusMap.put(user.getPhoneNumber(), true);
        }

        if (phoneNumbers.getStringList().contains(OfyService.devikaPhoneNumber)) {

            final ReachUser devika = ofy().load().type(ReachUser.class).id(OfyService.devikaId).now();
            friends.add(new Friend(devika, false, 0));
        }

        //do not log phone book if serverId is null
        final long serverId = phoneNumbers.getUserId();
        if (serverId == 0)
            return friends;
        final LoadResult<ReachUser> userLoadResult = ofy().load().type(ReachUser.class).id(serverId);

        //////////////
        try {

            final JSONArray arrayOfPhoneNumbers = new JSONArray();
            for (String phoneNumber : phoneNumbers.getStringList()) {

                final JSONObject object = new JSONObject();
                object.put("phoneNumber", phoneNumber);
                final Boolean aBoolean = statusMap.get(phoneNumber);
                object.put("status", aBoolean == null ? false : aBoolean);
                arrayOfPhoneNumbers.put(object);
            }

            final ReachUser user = userLoadResult.now();
            final JSONObject phoneBookPost = new JSONObject();

            phoneBookPost.put("secureKey", "ECeQsMORJ1W1yPJ9D9nIy6FwE1rgS1p7");
            phoneBookPost.put("userId", serverId);
            phoneBookPost.put("userName", user.getUserName());
            phoneBookPost.put("phoneNumber", user.getPhoneNumber());
            phoneBookPost.put("phoneBook", arrayOfPhoneNumbers);

            final URL url = new URL("http://img-1052310213.ap-southeast-1.elb.amazonaws.com/reach/addPhoneBook");
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            final OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(phoneBookPost.toString());
            writer.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                logger.info("connection result success");
            else
                logger.info("connection result fail " +
                        connection.getResponseCode() + " " +
                        connection.getResponseMessage());

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            logger.info("Error on post " + e.getLocalizedMessage());
        }

        return friends;
    }

    /**
     * To be used for first time sync purpose (bulk insert)
     *
     * @param clientId the id of the client
     * @return list of known friends
     */
    @ApiMethod(
            name = "longSync",
            path = "user/longSync/{clientId}/",
            httpMethod = ApiMethod.HttpMethod.GET)
    public Set<Friend> longSync(@Named("clientId") final long clientId) {

        //sanity checks
        if (clientId == 0)
            return null;

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final ReachUser client = ofy().load().type(ReachUser.class).id(clientId).now();
        if (client == null)
            return null;
        logger.info(client.getUserName());

        final boolean myReachCheck = (client.getMyReach() != null &&
                client.getMyReach().size() > 0);
        final boolean sentRequestsCheck = (client.getSentRequests() != null &&
                client.getSentRequests().size() > 0);

        //no known friends
        if (!(myReachCheck || sentRequestsCheck))
            return null;

        final HashSet<Friend> friends = new HashSet<>();
        final ImmutableList.Builder<Key> keysBuilder = new ImmutableList.Builder<>();

        if (myReachCheck) {

            keysBuilder.add(Key.create(ReachUser.class, OfyService.devikaId));
            for (long id : client.getMyReach())
                keysBuilder.add(Key.create(ReachUser.class, id));
        }

        if (sentRequestsCheck) {

            keysBuilder.add(Key.create(ReachUser.class, OfyService.devikaId));
            for (long id : client.getSentRequests())
                keysBuilder.add(Key.create(ReachUser.class, id));
        }

        for (ReachUser user : ofy().load().type(ReachUser.class)
                .filterKey("in", keysBuilder.build())
                .filter("gcmId !=", "")
                .project(Friend.projectNewFriend)) {

            final long lastSeen;
            if (user.getId() != OfyService.devikaId)
                lastSeen = computeLastSeen((byte[]) syncCache.get(user.getId()));
            else
                lastSeen = 0;

            friends.add(new Friend(
                    user,
                    client.getMyReach(),
                    client.getSentRequests(),
                    lastSeen));
        }

        return friends;
    }

    /**
     * To be used if friends are already loaded
     *
     * @param clientId the client
     * @param ids      pair of id - hash, to be used to updating friends data (IF changed, i.e. hash different)
     * @param hashes   pair of id - hash, to be used to updating friends data (IF changed, i.e. hash different)
     * @return QuickSync object
     */
    @ApiMethod(
            name = "quickSync",
            path = "user/quickSync/{clientId}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public QuickSync quickSync(@Named("clientId") final long clientId,
                               @Named("ids") List<Long> ids,
                               @Named("hashes") List<Integer> hashes) {

        //sanity checks
        if (clientId == 0 || ids == null || hashes == null)
            return null;
        final int size = ids.size();
        if (size == 0 || size != hashes.size())
            return null;
        final Map<Long, Integer> pair = new HashMap<>(size);
        for (int i = 0; i < size; i++)
            pair.put(ids.get(i), hashes.get(i));

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final ReachUser client = ofy().load().type(ReachUser.class).id(clientId).now();
        if (client == null)
            return null;
        logger.info(client.getUserName());

        //collections to return
        final Map<Long, Short> newStatus = new HashMap<>();
        final List<Friend> toUpdate = new ArrayList<>();
        final Set<Friend> newFriends = new HashSet<>();

        //helpers
        final List<Long> newIds = new ArrayList<>();
        final HashSet<Long> myReach = client.getMyReach();
        final HashSet<Long> sentRequests = client.getSentRequests();

        //dirty check query
        final QueryResultIterable<ReachUser> dirtyCheckQuery = ofy().load().type(ReachUser.class)
                .filterKey("in", getKeyBuilder(ids).build())
                .project("dirtyCheck").iterable(); //will load async

        //new friend query
        final QueryResultIterable<ReachUser> newIdQuery;
        //////////////////////////////////////
        //fill 3 by-default
        for (long id : ids)
            newStatus.put(id, (short) 3);
        //sync-up myReach
        if (myReach != null && myReach.size() > 0)
            for (long id : myReach)
                if (pair.containsKey(id))
                    if (id == OfyService.devikaId)
                        newStatus.put(id, (short) 0); //always online !
                    else
                        newStatus.put(id, (short) (computeLastSeen((byte[]) syncCache.get(id)) > ReachUser.ONLINE_LIMIT ? 1 : 0));
                else //if this id was not originally present !
                    newIds.add(id);
        //sync-up sentRequests
        if (sentRequests != null && sentRequests.size() > 0)
            for (long id : sentRequests)
                if (pair.containsKey(id))
                    newStatus.put(id, (short) 2);
                else //if this id was not originally present !
                    newIds.add(id);

        //newStatus built
        if (newIds.size() > 0)
            newIdQuery = ofy().load().type(ReachUser.class)
                    .filterKey("in", getKeyBuilder(newIds).build())
                    .filter("gcmId !=", "")
                    .project(Friend.projectNewFriend).iterable(); //will load async
        else
            newIdQuery = null;
        //////////////////////////////////////
        //sync up dirtyHashes
        for (ReachUser user : dirtyCheckQuery) {

            if (!pair.containsKey(user.getId()))
                throw new IllegalArgumentException("Parameter pair did not contain the id !");

            if (pair.get(user.getId()) == user.getDirtyCheck())
                continue; //no change happened !

            final ReachUser reachUser = ofy().load().type(ReachUser.class).id(user.getId()).now();
            if ((reachUser.getGcmId() == null || reachUser.getGcmId().equals(""))) {

                //DO NOT MARK AS DEAD IF DEVIKA !
                if (!(reachUser.getPhoneNumber().equals(OfyService.devikaPhoneNumber) || reachUser.getId() == OfyService.devikaId)) {
                    logger.info("Marking dead ! " + reachUser.getUserName());
                    toUpdate.add(new Friend(reachUser.getId(), true)); //mark dead
                }
            } else {

                logger.info("Marking for update ! " + reachUser.getUserName());
                toUpdate.add(new Friend(reachUser, myReach, sentRequests,
                        (short) (computeLastSeen((byte[]) syncCache.get(reachUser.getId())) > ReachUser.ONLINE_LIMIT ? 1 : 0)));
            }
        }

        //toUpdate built
        if (newIds.size() == 0 || newIdQuery == null) {
            logger.info("No new friends " + client.getUserName());
            return new QuickSync(newStatus, null, toUpdate);
        }
        //////////////////////////////////////
        //sync-up newIds
        for (ReachUser user : newIdQuery) {

            newFriends.add(new Friend(
                    user,
                    myReach,
                    sentRequests,
                    computeLastSeen((byte[]) syncCache.get(user.getId()))));
        }

        if (newIds.contains(OfyService.devikaId)) {

            final ReachUser devika = ofy().load().type(ReachUser.class).id(OfyService.devikaId).now();
            newFriends.add(new Friend(
                    devika,
                    myReach,
                    sentRequests,
                    0));
        }
        logger.info("Slow " + client.getUserName());
        return new QuickSync(newStatus, newFriends, toUpdate);
    }

    //////////////////////////Optimized friend sync operations

//    private Set<Friend> syncMyReach(long[] myReach,
//                                    int[] dirtyHash,
//                                    long clientId) {
//
//        if (clientId == 0)
//            return null; //illegal
//
//        final ReachUser client = ofy().load().type(ReachUser.class).id(clientId).now();
//        if (client == null || client.getMyReach() == null || client.getMyReach().isEmpty())
//            return null; //no friends :(
//
//        //mark as online
//        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
//        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
//        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
//                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);
//
//        //create map
//        final int oldReachSize = myReach == null ? 0 : myReach.length;
//        final Map<Long, Integer> pair = new HashMap<>(oldReachSize);
//        if (oldReachSize > 0)
//            for (int index = 0; index < oldReachSize; index++)
//                pair.put(myReach[index], dirtyHash[index]);
//
//        //process myReach
//        final List<Long> toLoad = new ArrayList<>();
//        for (ReachUser user : ofy().load().type(ReachUser.class)
//                .filterKey("in", getKeyBuilder(client.getMyReach()).build())
//                .project("dirtyCheck")) {
//
//            final long userId = user.getId();
//            //add if new friend or data was changed
//            if (!pair.containsKey(userId) || pair.get(userId) != user.getDirtyCheck())
//                toLoad.add(userId);
//        }
//
//        if (toLoad.isEmpty())
//            return null; //nothing to do
//
//        final Set<Friend> newFriends = new HashSet<>();
//        for (ReachUser user : ofy().load().type(ReachUser.class)
//                .filterKey("in", getKeyBuilder(toLoad).build())
//                .filter("gcmId !=", "")
//                .project(Friend.projectNewFriend)) {
//
//            final long lastSeen = computeLastSeen((byte[]) syncCache.get(user.getId()));
//            newFriends.add(new Friend(client, true, lastSeen)); //parse into friend
//        }
//
//        return newFriends;
//    }
//
//    private Set<Friend> syncSentRequests(Collection<Long> sentRequests,
//                                         long clientId) {
//
//        if (clientId == 0)
//            return null; //illegal
//
//        final ReachUser client = ofy().load().type(ReachUser.class).id(clientId).now();
//        if (client == null || client.getSentRequests() == null || client.getSentRequests().isEmpty())
//            return null; //no requests sent :(
//
//        //mark as online
//        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
//        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
//        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
//                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);
//    }
//
//    private Set<Friend> syncReceivedRequests(Collection<Long> receivedRequests,
//                                             long clientId) {
//
//
//    }

    //////////////////////////

    @ApiMethod(
            name = "getFriendFromId",
            path = "user/getFriendFromId/{clientId}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public Friend getFriendFromId(@Named("clientId") long clientId,
                                  @Named("hostId") long hostId) {

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(), Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final ReachUser client = ofy().load().type(ReachUser.class).id(clientId).now();
        if (!client.getMyReach().contains(hostId))
            return null;

        final ReachUser host = ofy().load().type(ReachUser.class).id(hostId).now();
        return new Friend(host, true, computeLastSeen((byte[]) syncCache.get(host)));
    }

    private ImmutableList.Builder<Key> getKeyBuilder(Iterable<Long> ids) {

        final ImmutableList.Builder<Key> dirtyCheck = new ImmutableList.Builder<>();
        for (Long id : ids)
            dirtyCheck.add(Key.create(ReachUser.class, id));
        return dirtyCheck;
    }

    private ImmutableList.Builder<Key> getKeyBuilder(long... ids) {

        final ImmutableList.Builder<Key> dirtyCheck = new ImmutableList.Builder<>();
        for (Long id : ids)
            dirtyCheck.add(Key.create(ReachUser.class, id));
        return dirtyCheck;
    }

    private long computeLastSeen(byte[] value) {

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
        return lastSeen;
    }

    @ApiMethod(
            name = "getUserData",
            path = "user/getUserData/",
            httpMethod = ApiMethod.HttpMethod.GET)
    public List<ReachUser> getUserData(@Named("userIds") Collection<Long> userIds,
                                       @Nullable @Named("projection") String[] projection) {

        final Query<ReachUser> baseQuery;
        if (projection != null && projection.length > 0)
            baseQuery = ofy().load().type(ReachUser.class).project(projection);
        else
            baseQuery = ofy().load().type(ReachUser.class);

        final ImmutableList.Builder<Key> keysBuilder = new ImmutableList.Builder<>();
        for (Long id : userIds)
            keysBuilder.add(Key.create(ReachUser.class, id));

        final QueryResultIterator<ReachUser> queryIterator = baseQuery.filterKey("in", keysBuilder.build()).iterator();

        final List<ReachUser> toReturn = new ArrayList<>();
        while (queryIterator.hasNext())
            toReturn.add(queryIterator.next());

        return toReturn;
    }

    @ApiMethod(
            name = "getReceivedRequests",
            path = "user/getReceivedRequests/{clientId}/",
            httpMethod = ApiMethod.HttpMethod.GET)
    public List<ReceivedRequest> getReceivedRequests(@Named("clientId") final long clientId) {

        if (clientId == 0)
            return null;

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final ReachUser client = ofy().load().type(ReachUser.class).id(clientId).now();
        if (client == null || client.getReceivedRequests() == null || client.getReceivedRequests().isEmpty())
            return null;
        //////////////////////////////////////

        final List<ReceivedRequest> requests = new ArrayList<>();
        for (ReachUser user : ofy().load().type(ReachUser.class)
                .filterKey("in", getKeyBuilder(client.getReceivedRequests()).build())
                .filter("gcmId !=", "")
                .project(ReceivedRequest.projectReceivedRequest)) {

            requests.add(new ReceivedRequest(
                    user.getId(),
                    user.getNumberOfSongs(),
                    user.getPhoneNumber(),
                    user.getUserName(),
                    user.getImageId()));
        }
        return requests;
    }

    @ApiMethod(
            name = "pingMyReach",
            path = "user/ping/{clientId}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MyString pingMyReach(@Named("clientId") long clientId) {

        if (clientId == 0)
            return null;

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final ReachUser clientUser = ofy().load().type(ReachUser.class).id(clientId).now();
        if (clientUser == null || clientUser.getMyReach() == null || clientUser.getMyReach().size() == 0)
            return null;
        logger.info(clientUser.getUserName() + " sending PONG");
        final ImmutableList.Builder<Key> keysBuilder = new ImmutableList.Builder<>();
        for (Long id : clientUser.getMyReach())
            keysBuilder.add(Key.create(ReachUser.class, id));
        keysBuilder.add(Key.create(ReachUser.class, clientId));

        final Sender sender = new Sender(MessagingEndpoint.API_KEY);
        final Message actualMessage = new Message.Builder()
                .addData("message", "PING")
                .build();

        return new MyString(new MessagingEndpoint().sendMultiCastMessage(
                actualMessage,
                sender,
                ofy().load().type(ReachUser.class)
                        .filterKey("in", keysBuilder.build())
                        .filter("gcmId !=", "")
                        .project("gcmId")) + "");
    }

    @ApiMethod(
            name = "getStats",
            path = "user/getStats",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<Statistics> getStats(@Named("cursor") String cursor,
                                                   @Nullable @Named("limit") Integer limit) {

        limit = limit == null ? 300 : limit;
        final ImmutableList.Builder<Statistics> builder = new ImmutableList.Builder<>();
        final QueryResultIterator<ReachUser> userQueryResultIterator;

        if (TextUtils.isEmpty(cursor))
            userQueryResultIterator = ofy().load().type(ReachUser.class)
                    .limit(limit)
                    .iterator();
        else
            userQueryResultIterator = ofy().load().type(ReachUser.class)
                    .startAt(Cursor.fromWebSafeString(cursor))
                    .limit(limit)
                    .iterator();


        ReachUser reachUser;
        int count = 0;

        while (userQueryResultIterator.hasNext()) {

            reachUser = userQueryResultIterator.next();
            builder.add(new Statistics(
                    reachUser.getId(),
                    reachUser.getTimeCreated(),
                    reachUser.getUserName(),
                    reachUser.getPhoneNumber(),
                    reachUser.getNumberOfSongs(),
                    reachUser.getMyReach() == null ? 0 : reachUser.getMyReach().size(),
                    reachUser.getSentRequests() == null ? 0 : reachUser.getSentRequests().size(),
                    reachUser.getReceivedRequests() == null ? 0 : reachUser.getReceivedRequests().size(),
                    ofy().load().type(CompletedOperations.class).filter("senderId =", reachUser.getId()).count(),
                    ofy().load().type(CompletedOperations.class).filter("receiver =", reachUser.getId()).count(),
                    reachUser.getGcmId() != null && !reachUser.getGcmId().equals("")));
            count++;
        }

        //count < limit means this was possibly the last call
        final String cursorToReturn = (count >= limit) ? userQueryResultIterator.getCursor().toWebSafeString() : "";
        logger.info("Get stats fetched " + count + " " + cursorToReturn);

        return CollectionResponse.<Statistics>builder()
                .setItems(builder.build())
                .setNextPageToken(cursorToReturn)
                .build();
    }

    @ApiMethod(
            name = "insertNew",
            path = "user/insertNew",
            httpMethod = ApiMethod.HttpMethod.POST)
    public InsertContainer insertNew(ReachUser user) {

        final ReachUser oldUser = ofy().load().type(ReachUser.class)
                .filter("phoneNumber in", Collections.singletonList(user.getPhoneNumber())).first().now();

        if (oldUser != null) {

            /**
             * We re-use same accounts on the basis of phone numbers,
             * we don't re-use same accounts on the basis of device ID
             */
            user.setId(oldUser.getId());
            user.setReceivedRequests(oldUser.getReceivedRequests());
            user.setSentRequests(oldUser.getSentRequests());
            user.setMyReach(oldUser.getMyReach());
            user.setMegaBytesReceived(oldUser.getMegaBytesReceived());
            user.setMegaBytesSent(oldUser.getMegaBytesSent());
            user.setTimeCreated(oldUser.getTimeCreated());
            user.setPromoCode(oldUser.getPromoCode());
            user.setChatToken(oldUser.getChatToken());
            ofy().delete().entity(oldUser).now();
        } else
            user.setTimeCreated(System.currentTimeMillis());

        ofy().save().entity(user).now();
        logger.info("Created ReachUser with ID: " + user.getUserName() + " " + new MessagingEndpoint().sendMessage("hello_world", user));

        //generate devikaChat token
        if (user.getChatToken() == null || user.getChatToken().equals("hello_world") || user.getChatToken().equals("")) {

            final Map<String, Object> authPayload = new HashMap<>(2);
            authPayload.put("uid", user.getId() + "");
            authPayload.put("phoneNumber", user.getPhoneNumber());
            authPayload.put("userName", user.getUserName());
            authPayload.put("imageId", user.getImageId());

            final TokenOptions tokenOptions = new TokenOptions();
            final TokenGenerator tokenGenerator = new TokenGenerator(FIRE_BASE_SECRET);
            final String token = tokenGenerator.createToken(authPayload, tokenOptions);
            if (token != null && !token.equals("")) {
                //save if token was generated
                user.setChatToken(token);
                ofy().save().entities(user).now();
            }
        }

        return new InsertContainer(user.getId(), user.getChatToken());
    }

    @ApiMethod(
            name = "getChatToken",
            path = "user/getChatToken/{userId}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MyString getChatToken(@Named("userId") long userId) {

        final ReachUser user = ofy().load().type(ReachUser.class).id(userId).now();

        String chatToken = user.getChatToken();
        if (chatToken == null || chatToken.equals("hello_world") || chatToken.equals("")) {

            final Map<String, Object> authPayload = new HashMap<>(2);
            authPayload.put("uid", user.getId() + "");
            authPayload.put("phoneNumber", user.getPhoneNumber());
            authPayload.put("userName", user.getUserName());
            authPayload.put("imageId", user.getImageId());

            final TokenOptions tokenOptions = new TokenOptions();
            final TokenGenerator tokenGenerator = new TokenGenerator(FIRE_BASE_SECRET);
            chatToken = tokenGenerator.createToken(authPayload, tokenOptions);
            if (chatToken != null && !chatToken.equals("")) {
                //save if token was generated
                user.setChatToken(chatToken);
                ofy().save().entities(user).now();
            }
        }

        return new MyString(chatToken);
    }

    @ApiMethod(
            name = "getGcmId",
            path = "user/gcmId/{clientId}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MyString getGCMId(@Named("clientId") long clientId) {

        if (clientId == 0)
            return null;

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);
        final ReachUser user = ofy().load().type(ReachUser.class).id(clientId).now();

        if (user == null)
            return new MyString("user_deleted");
        else if (user.getGcmId() == null ||
                user.getGcmId().equals("") ||
                user.getGcmId().equals("hello_world"))
            return new MyString("hello_world");
        else
            return new MyString(user.getGcmId());
    }

    @ApiMethod(
            name = "updateUserDetails",
            path = "user/updateUserDetails",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void updateUserDetails(@Named("clientId") long clientId,
                                  @Named("details") String[] details) {

        if (clientId == 0)
            return;

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        logger.info("Trying to update " + Arrays.toString(details));
        final String newName = details[0];
        final String newImageId = details[1];
        final ReachUser userToSave = ofy().load().type(ReachUser.class).id(clientId).now();
        if (userToSave == null)
            return;
        userToSave.setUserName(newName);
        userToSave.setImageId(newImageId);
        ofy().save().entities(userToSave).now();
    }

    @ApiMethod(
            name = "updateCompletedOperations",
            path = "user/updateCompletedOperations",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void addToCompletedOperations(@Named("id") long clientId,
                                         @Named("senderId") long hostId,
                                         @Named("songSize") long songSize,
                                         @Named("songName") String songName) {

        if (clientId == 0)
            return;

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        final long time = System.currentTimeMillis();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (time + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final CompletedOperations completedOperations = new CompletedOperations();
        completedOperations.setHits(1);
        completedOperations.setTime(time);
        completedOperations.setReceiver(clientId);
        completedOperations.setSenderId(hostId);
        completedOperations.setSongName(songName);
        completedOperations.setSongSize(songSize);
        ofy().save().entities(completedOperations).now();
        logger.info("Adding new UploadHistory" + songName + " sender " + ofy().load().type(ReachUser.class).id(hostId).now().getUserName() + " receiver " +
                ofy().load().type(ReachUser.class).id(clientId).now().getUserName());
    }

    @ApiMethod(
            name = "getCompletedOperations",
            path = "user/getCompletedOperations/{clientId}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public HashSet<CompletedOperation> getCompletedOperations(@Named("clientId") long clientId) {

        if (clientId == 0)
            return null;

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final ReachUser reachUser = ofy().load().type(ReachUser.class).id(clientId).now();
        if (reachUser == null)
            return null;

        final HashMap<Integer, CompletedOperation> operationHashMap = new HashMap<>();

        ////////////////////////////////////
        for (CompletedOperations event : ofy().load().type(CompletedOperations.class)
                .filter("senderId =", reachUser.getId())) {

            CompletedOperation history = operationHashMap.get(event.hashCode());
            if (history == null) {

                history = new CompletedOperation(
                        event.getSongName(),
                        event.getSongSize(),
                        event.getSenderId());
                history.setReceiver(new HashSet<Long>());
                history.setHits(event.getHits());
                history.setTime(event.getTime());
                history.getReceiver().add(event.getReceiver());
                operationHashMap.put(history.hashCode(), history);
            } else {

                history.setHits(history.getHits() + 1);
                //update the time only if newer
                if (event.getTime() > history.getTime())
                    history.setTime(event.getTime());
                history.getReceiver().add(event.getReceiver());
            }
        }
        return new HashSet<>(operationHashMap.values());
    }

    @ApiMethod(
            name = "setGCMId",
            path = "user/gcmId",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void setGCMId(@Named("clientId") long clientId,
                         @Named("gcmId") String gcmId) {

        if (clientId == 0)
            return;

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);
        final ReachUser user = ofy().load().type(ReachUser.class).id(clientId).now();
        if (user == null) {
            logger.info(clientId + " setting GCmId failed");
            return;
        }
        user.setGcmId(gcmId);
        ofy().save().entity(user).now();
        logger.info("New Gcm ID for " + user.getUserName() + " " + new MessagingEndpoint().sendMessage("PING", user));
    }

    @ApiMethod(
            name = "isAccountPresentNew",
            path = "user/isAccountPresentNew/{phoneNumber}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public OldUserContainerNew isAccountPresentNew(@Named("phoneNumber") String phoneNumber) {

        final ReachUser oldUser = ofy().load().type(ReachUser.class)
                .filter("phoneNumber in", Collections.singletonList(phoneNumber))
                .first().now();
        if (oldUser != null)
            return new OldUserContainerNew(
                    oldUser.getUserName(),
                    oldUser.getPromoCode(),
                    oldUser.getImageId(),
                    oldUser.getId());
        return null;
    }

    @ApiMethod(
            name = "storePromoCode",
            path = "user/storePromoCode/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MyString storePromoCode(@Named("id") long id,
                                   @Named("promoCode") String promoCode) {

        final ReachUser oldUser = ofy().load().type(ReachUser.class).id(id).now();
        if (oldUser == null)
            return new MyString("false");
        oldUser.setPromoCode(promoCode);
        ofy().save().entity(oldUser).now();
        return new MyString("true");
    }

    @ApiMethod(
            name = "updateDatabase",
            path = "user/updateDatabase",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MyString port() {

        QueueFactory.getDefaultQueue().add(TaskOptions.Builder
                .withUrl("/worker")
                .param("cursor", "")
                .param("total", 0 + "")
                .retryOptions(RetryOptions.Builder.withTaskRetryLimit(0)));
        return new MyString("submitted");
    }

    @ApiMethod(
            name = "devikaSendFriendRequestToAll",
            path = "user/devikaSendFriendRequestToAll",
            httpMethod = ApiMethod.HttpMethod.POST)
    public MyString devikaSendFriendRequestToAll(LongList userIds) {

        int successCounter = 0;
        int failCounter = 0;

        final MessagingEndpoint messagingEndpoint = MessagingEndpoint.getInstance();
        for (Long hostId : userIds.getList()) {

            final MyString result = messagingEndpoint.requestAccess(OfyService.devikaId, hostId);
            if (result == null || TextUtils.isEmpty(result.getString()) || result.getString().equals("false"))
                failCounter++;
            else
                successCounter++;
        }

        return new MyString("Result : " + failCounter + "failed " + successCounter + " successful");
    }
}