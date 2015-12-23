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
import com.google.appengine.repackaged.com.google.common.hash.HashCode;
import com.google.appengine.repackaged.com.google.common.hash.HashFunction;
import com.google.appengine.repackaged.com.google.common.hash.Hashing;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.cmd.Query;
import com.squareup.wire.Wire;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;

import reach.backend.Constants;
import reach.backend.MiscUtils;
import reach.backend.ObjectWrappers.App;
import reach.backend.ObjectWrappers.AppList;
import reach.backend.ObjectWrappers.LongArray;
import reach.backend.ObjectWrappers.MusicList;
import reach.backend.ObjectWrappers.MyString;
import reach.backend.ObjectWrappers.SimpleApp;
import reach.backend.ObjectWrappers.SimpleSong;
import reach.backend.ObjectWrappers.Song;
import reach.backend.ObjectWrappers.StringList;
import reach.backend.TextUtils;
import reach.backend.Transactions.CompletedOperation;
import reach.backend.Transactions.CompletedOperations;
import reach.backend.User.FriendContainers.Friend;
import reach.backend.User.FriendContainers.QuickSync;
import reach.backend.User.FriendContainers.ReceivedRequest;
import reach.backend.campaign.BackLog;

import static reach.backend.OfyService.ofy;

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

    /**
     * TODO remove
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

        if (phoneNumbers.contains(Constants.devikaPhoneNumber)) {

            final ReachUser devika = ofy().load().type(ReachUser.class).id(Constants.devikaId).now();
            friends.add(new Friend(devika, false, 0));
        }

        return friends;
    }

    /**
     * TODO remove
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

        if (phoneNumbers.contains(Constants.devikaPhoneNumber)) {

            final ReachUser devika = ofy().load().type(ReachUser.class).id(Constants.devikaId).now();
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

            final URL url = new URL("http://52.74.117.248:8080/reach/addPhoneBook");
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

        if (phoneNumbers.getStringList().contains(Constants.devikaPhoneNumber)) {

            final ReachUser devika = ofy().load().type(ReachUser.class).id(Constants.devikaId).now();
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

            final URL url = new URL("http://52.74.117.248:8080/reach/addPhoneBook");
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

            keysBuilder.add(Key.create(ReachUser.class, Constants.devikaId));
            for (long id : client.getMyReach())
                keysBuilder.add(Key.create(ReachUser.class, id));
        }

        if (sentRequestsCheck) {

            keysBuilder.add(Key.create(ReachUser.class, Constants.devikaId));
            for (long id : client.getSentRequests())
                keysBuilder.add(Key.create(ReachUser.class, id));
        }

        for (ReachUser user : ofy().load().type(ReachUser.class)
                .filterKey("in", keysBuilder.build())
                .filter("gcmId !=", "")
                .project(Friend.projectNewFriend)) {

            final long lastSeen;
            if (user.getId() != Constants.devikaId)
                lastSeen = MiscUtils.computeLastSeen(syncCache, user.getId());
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
                .filterKey("in", MiscUtils.getKeyBuilder(ids).build())
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
                    if (id == Constants.devikaId)
                        newStatus.put(id, (short) 0); //always online !
                    else
                        newStatus.put(id, (short) (MiscUtils.computeLastSeen(syncCache, id) > ReachUser.ONLINE_LIMIT ? 1 : 0));
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
                    .filterKey("in", MiscUtils.getKeyBuilder(newIds).build())
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
                if (!(reachUser.getPhoneNumber().equals(Constants.devikaPhoneNumber) || reachUser.getId() == Constants.devikaId)) {
                    logger.info("Marking dead ! " + reachUser.getUserName());
                    toUpdate.add(new Friend(reachUser.getId(), true)); //mark dead
                }
            } else {

                logger.info("Marking for update ! " + reachUser.getUserName());
                toUpdate.add(new Friend(reachUser, myReach, sentRequests,
                        (short) (MiscUtils.computeLastSeen(syncCache, reachUser.getId()) > ReachUser.ONLINE_LIMIT ? 1 : 0)));
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
                    MiscUtils.computeLastSeen(syncCache, user.getId())));
        }

        if (newIds.contains(Constants.devikaId)) {

            final ReachUser devika = ofy().load().type(ReachUser.class).id(Constants.devikaId).now();
            newFriends.add(new Friend(
                    devika,
                    myReach,
                    sentRequests,
                    0));
        }
        logger.info("Slow " + client.getUserName());
        return new QuickSync(newStatus, newFriends, toUpdate);
    }

    //////////////////////////

    @ApiMethod(
            name = "getFriendFromId",
            path = "user/getFriendFromId/{clientId}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public Friend getFriendFromId(@Named("clientId") long clientId,
                                  @Named("hostId") long hostId) {

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));

        final ReachUser client = ofy().load().type(ReachUser.class).id(clientId).now();
        if (!client.getMyReach().contains(hostId))
            return null;

        final ReachUser host = ofy().load().type(ReachUser.class).id(hostId).now();
        return new Friend(host, true, MiscUtils.computeLastSeen(syncCache, hostId));
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
                .filterKey("in", MiscUtils.getKeyBuilder(client.getReceivedRequests()).build())
                .filter("gcmId !=", "")
                .project(ReceivedRequest.projectReceivedRequest)) {

            requests.add(new ReceivedRequest(
                    user.getId(),
                    user.getNumberOfSongs(),
                    user.getNumberOfApps(),
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

                    reachUser.getImageId(),
                    reachUser.getCoverPicId(),
                    reachUser.getStatusSong(),
                    reachUser.getEmailId(),
                    reachUser.getBirthday(),

                    reachUser.getNumberOfSongs(),
                    reachUser.getNumberOfApps(),
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

            user.setStatusSong(oldUser.getStatusSong());
            user.setEmailId(oldUser.getEmailId());
            user.setBirthday(oldUser.getBirthday());

            user.setPromoCode(oldUser.getPromoCode());
            user.setChatToken(oldUser.getChatToken());

            user.setMegaBytesReceived(oldUser.getMegaBytesReceived());
            user.setMegaBytesSent(oldUser.getMegaBytesSent());
            user.setTimeCreated(oldUser.getTimeCreated());

            user.setReceivedRequests(oldUser.getReceivedRequests());
            user.setSentRequests(oldUser.getSentRequests());
            user.setMyReach(oldUser.getMyReach());

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

        //new stuff
        final String coverPic, statusSong, emailId;
        final Date birthday;
        if (details.length == 6) {

            coverPic = details[2];
            statusSong = details[3];
            emailId = details[4];
            birthday = new Date(Long.parseLong(details[5]));
        } else {

            coverPic = "hello_world";
            statusSong = "hello_world";
            emailId = "hello_world";
            birthday = new Date(0);
        }

        final ReachUser userToSave = ofy().load().type(ReachUser.class).id(clientId).now();
        if (userToSave == null)
            return;

        userToSave.setUserName(newName);
        userToSave.setImageId(newImageId);

        userToSave.setCoverPicId(coverPic);
        userToSave.setStatusSong(statusSong);
        userToSave.setEmailId(emailId);
        userToSave.setBirthday(birthday);

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

        final HashFunction hashFunction = Hashing.md5();
        final HashCode hashCode = hashFunction.newHasher()
                .putString(songName, Charset.defaultCharset())
                .putLong(songSize).hash();

        //noinspection StringBufferReplaceableByString
        final StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(Constants.BASE_LOG_TRANSACTION)
                .append(Constants.USER_ID).append("=")
                .append(clientId).append("&")
                .append(Constants.FRIEND_ID).append("=")
                .append(hostId).append("&")
                .append(Constants.SONG_ID).append("=")
                .append(hashCode.toString());

        final String log = urlBuilder.toString();

        logger.info(log);

        boolean success;

        try {
            final URL url = new URL(log);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(false);
            connection.setRequestMethod("GET");
            connection.connect();

            final int statusCode = connection.getResponseCode();
            success = statusCode == HttpURLConnection.HTTP_OK;
            logger.info("status code " + statusCode);

        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        logger.info("logging " + success);

        if (!success) {

            final BackLog backLog = new BackLog();
            backLog.setFailedUrl(log);
            backLog.setId(MiscUtils.longHash(log));
            ofy().save().entities(backLog);
        }
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
                    oldUser.getId(),

                    //new stuff
                    oldUser.getCoverPicId(),
                    oldUser.getStatusSong(),
                    oldUser.getEmailId(),
                    oldUser.getBirthday());
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
    public MyString port(@Named("keys") String[] keys,
                         @Named("values") String[] values,
                         @Named("taskName") String taskName) {

        final TaskOptions builder = TaskOptions.Builder.withUrl("/worker");

        if (keys != null && values != null) {

            if (keys.length != values.length)
                throw new IllegalArgumentException("Keys and values do not have same length");

            for (int index = 0; index < keys.length; index++)
                builder.param(keys[index], values[index]);
        }

        QueueFactory.getDefaultQueue().add(builder
                .taskName(taskName)
                .retryOptions(RetryOptions.Builder.withTaskRetryLimit(0).taskAgeLimitSeconds(0)));
        return new MyString("submitted");
    }

    @ApiMethod(
            name = "resetStatus",
            path = "user/resetStatus/{serverId}/{friendId}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void resetStatus(@Named("serverId") long serverId,
                            @Named("friendId") long friendId) {

        final ReachUser client = ofy().load().type(ReachUser.class).id(serverId).now();
        final ReachUser host = ofy().load().type(ReachUser.class).id(friendId).now();

        if (client == null || host == null)
            return;

        //removing friend from myReach
        if (client.getMyReach() != null && !client.getMyReach().isEmpty())
            client.getMyReach().remove(friendId);

        //removing friend from sent
        if (client.getSentRequests() != null && !client.getSentRequests().isEmpty())
            client.getSentRequests().remove(friendId);

        //removing friend from received
        if (client.getReceivedRequests() != null && !client.getReceivedRequests().isEmpty())
            client.getReceivedRequests().remove(friendId);

        //////////////////Remove from host also

        //removing friend from myReach
        if (host.getMyReach() != null && !host.getMyReach().isEmpty())
            host.getMyReach().remove(serverId);

        //removing friend from sent
        if (host.getSentRequests() != null && !host.getSentRequests().isEmpty())
            host.getSentRequests().remove(serverId);

        //removing friend from received
        if (host.getReceivedRequests() != null && !host.getReceivedRequests().isEmpty())
            host.getReceivedRequests().remove(serverId);

        ofy().save().entities(host, client);

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(serverId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);
    }

    @ApiMethod(
            name = "bulkAliveCheck",
            path = "user/bulkAliveCheck",
            httpMethod = ApiMethod.HttpMethod.POST)
    public AliveCheck bulkAliveCheck(LongArray userIds) {

        final Long[] ids;
        if (userIds == null || (ids = userIds.getList()) == null || ids.length == 0)
            throw new IllegalArgumentException("U noob don't send empty data");

        final boolean[] isAlive = new boolean[ids.length];

        int index = 0;
        for (ReachUser user : ofy().load().type(ReachUser.class)
                .filterKey("in", MiscUtils.getKeyBuilder(ids).build())
                .project("gcmId")) {

            ids[index] = user.getId();
            isAlive[index] = !TextUtils.isEmpty(user.getGcmId());
            ++index;
        }

        final AliveCheck aliveCheck = new AliveCheck();
        aliveCheck.setAlive(isAlive);
        aliveCheck.setId(ids);
        return aliveCheck;
    }

    @ApiMethod(
            name = "devikaSendFriendRequestToAll",
            path = "user/devikaSendFriendRequestToAll",
            httpMethod = ApiMethod.HttpMethod.POST)
    public MyString devikaSendFriendRequestToAll(LongArray userIds) {

        final Long[] ids;
        if (userIds == null || (ids = userIds.getList()) == null || ids.length == 0)
            throw new IllegalArgumentException("U noob don't send empty data");

        final Map<Long, ReachUser> hostMap = ofy().load().type(ReachUser.class).ids(ids);
        final ReachUser devika = ofy().load().type(ReachUser.class).id(Constants.devikaId).now();
        final List<ReachUser> hostsToSave = new ArrayList<>();

        final HashSet<Long> devikaSent = devika.getSentRequests() == null ? new HashSet<Long>() : devika.getSentRequests();
        final HashSet<Long> devikaReach = devika.getMyReach() == null ? new HashSet<Long>() : devika.getMyReach();

        int processCount = 0;

        for (Map.Entry<Long, ReachUser> userEntry : hostMap.entrySet()) {

            final ReachUser host = userEntry.getValue();
            final long hostId = userEntry.getKey();

            if (host == null)
                continue;

            if ((!devikaReach.contains(hostId)) && !devikaSent.contains(hostId))
                logger.info("Saving Sent Request on " + devika.getUserName() + " " + devikaSent.add(hostId));

            if ((host.getMyReach() == null || !host.getMyReach().contains(Constants.devikaId)) &&
                    (host.getReceivedRequests() == null || !host.getReceivedRequests().contains(Constants.devikaId))) {

                if (host.getReceivedRequests() == null)
                    host.setReceivedRequests(new HashSet<Long>());
                logger.info("Saving Received Request on " + host.getUserName() + " " + host.getReceivedRequests().add(Constants.devikaId));
                hostsToSave.add(host);
            }

            if (TextUtils.isEmpty(host.getGcmId()))
                logger.info("Error handling reply " + hostId + " " + host.getUserName());
            else
                processCount++;
        }

        if (hostsToSave.isEmpty())
            return new MyString("Result : " + processCount);

        logger.info("Batch save of " + hostsToSave.size() + " users");

        //save "new" user collections asynchronously
        ofy().save().entities(hostsToSave);
        ofy().save().entities(devika);

        logger.info("Send notification to reach user");

        //send bulk to required users !
        final MessagingEndpoint messagingEndpoint = MessagingEndpoint.getInstance();
        final Message message = new Message.Builder()
                .addData("message", "PERMISSION_REQUEST`" + Constants.devikaId + "`" + devika.getUserName())
                .build();
        final Sender sender = new Sender(MessagingEndpoint.API_KEY);
        final ImmutableList.Builder<Key> keysBuilder = new ImmutableList.Builder<>();
        for (ReachUser reachUser : hostsToSave)
            keysBuilder.add(Key.create(ReachUser.class, reachUser.getId()));

        messagingEndpoint.sendMultiCastMessage(message, //the message that needs to be sent
                sender, //the sender id
                ofy().load().type(ReachUser.class)
                        .filterKey("in", keysBuilder.build()) //required users
                        .filter("gcmId !=", "") //not if gcm id is dead
                        .project("gcmId")); //project only the gcmId

        return new MyString("Result : " + processCount);
    }

    @ApiMethod(
            name = "cleanUpDevika",
            path = "user/cleanUpDevika",
            httpMethod = ApiMethod.HttpMethod.POST)
    public void cleanUpDevika() {

        final ReachUser devika = ofy().load().type(ReachUser.class).id(Constants.devikaId).now();

        devika.getMyReach().clear();
        devika.getSentRequests().clear();
        ofy().save().entities(devika).now();
    }

    @ApiMethod(
            name = "sendBulkNotification",
            path = "user/sendBulkNotification/{message}/{heading}",
            httpMethod = ApiMethod.HttpMethod.POST)
    public MyString sendBulkNotification(@Named("message") String message,
                                         @Named("heading") String heading,
                                         LongArray ids) {

        final String notification = "MANUAL`" + 0 + "`" + heading + "`" + message;

        final Sender sender = new Sender(MessagingEndpoint.API_KEY);
        final Message toSend = new Message.Builder().addData("message", notification).build();
        final ImmutableList.Builder<Key> keysBuilder = new ImmutableList.Builder<>();
        for (Long id : ids.getList())
            if (id != null)
                keysBuilder.add(Key.create(ReachUser.class, id));

        return new MyString(MessagingEndpoint.getInstance().sendMultiCastMessage(
                toSend,
                sender,
                ofy().load().type(ReachUser.class)
                        .filterKey("in", keysBuilder.build())
                        .filter("gcmId !=", "")
                        .project("gcmId")) + "");
    }

    @ApiMethod(
            name = "fetchRecentSongs",
            path = "user/fetchRecentSongs/{serverId}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public List<SimpleSong> fetchRecentSongs(@Named("serverId") long serverId) {

        final String FILE_NAME = serverId + "MUSIC";
        final String BUCKET_NAME_MUSIC_DATA = "able-door-616-music-data";

        final GcsFilename gcsFilename = new GcsFilename(BUCKET_NAME_MUSIC_DATA, FILE_NAME);
        final GcsService gcsService = GcsServiceFactory.createGcsService();

        //to close
        final GcsInputChannel inputChannel;
        final BufferedInputStream bufferedInputStream;
        final GZIPInputStream compressedData;
        final InputStream inputStream;

        final MusicList musicList;

        logger.info("Receiving the object");
        try {

            inputChannel = gcsService.openReadChannel(gcsFilename, 0);
            bufferedInputStream = new BufferedInputStream(inputStream = Channels.newInputStream(inputChannel));
            compressedData = new GZIPInputStream(bufferedInputStream);
            musicList = new Wire(MusicList.class).parseFrom(compressedData, MusicList.class);

        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList(); //fail
        }

        MiscUtils.closeQuietly(bufferedInputStream, inputChannel, compressedData, inputStream);

        final List<Song> songs;
        //sanity checks
        if (musicList == null || (songs = musicList.song) == null || songs.isEmpty())
            return Collections.emptyList();

        logger.info("Copying to array before sort");

        final int totalSongs = songs.size();
        final Song[] songArray = new Song[totalSongs];

        int index = 0;
        for (Song song : songs)
            songArray[index++] = song;

        logger.info("Sorting");
        //sort the song array
        Arrays.sort(songArray, new Comparator<Song>() {
            @Override
            public int compare(Song lhs, Song rhs) {

                final Long a = lhs.dateAdded == null ? 0 : lhs.dateAdded;
                final Long b = rhs.dateAdded == null ? 0 : rhs.dateAdded;
                return a.compareTo(b);
            }
        });

        final List<SimpleSong> toReturn = new ArrayList<>();
        //generate return list of max 20 songs
        for (index = 0; index < 20 && index < totalSongs; index++) {

            final SimpleSong simpleSong = new SimpleSong();
            final Song song = songArray[index];

            simpleSong.actualName = song.actualName;
            simpleSong.album = song.album;
            simpleSong.artist = song.artist;
            simpleSong.dateAdded = song.dateAdded;
            simpleSong.displayName = song.displayName;
            simpleSong.duration = song.duration;
            simpleSong.fileHash = song.fileHash;
            simpleSong.genre = song.genre;
            simpleSong.path = song.path;
            simpleSong.isLiked = song.isLiked;
            simpleSong.size = song.size;
            simpleSong.songId = song.songId;
            simpleSong.visibility = song.visibility;
            simpleSong.year = song.year;

            toReturn.add(simpleSong);
        }

        return toReturn;
    }

    @ApiMethod(
            name = "fetchRecentApps",
            path = "user/fetchRecentApps/{serverId}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public List<SimpleApp> fetchRecentApps(@Named("serverId") long serverId) {

        final String FILE_NAME = serverId + "APP";
        final String BUCKET_NAME_APP_DATA = "able-door-616-app-data";

        final GcsFilename gcsFilename = new GcsFilename(BUCKET_NAME_APP_DATA, FILE_NAME);
        final GcsService gcsService = GcsServiceFactory.createGcsService();

        //to close
        final GcsInputChannel inputChannel;
        final BufferedInputStream bufferedInputStream;
        final GZIPInputStream compressedData;
        final InputStream inputStream;

        final AppList appList;

        logger.info("Receiving the object");
        try {

            inputChannel = gcsService.openReadChannel(gcsFilename, 0);
            bufferedInputStream = new BufferedInputStream(inputStream = Channels.newInputStream(inputChannel));
            compressedData = new GZIPInputStream(bufferedInputStream);
            appList = new Wire(AppList.class).parseFrom(compressedData, AppList.class);

        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList(); //fail
        }

        MiscUtils.closeQuietly(bufferedInputStream, inputChannel, compressedData, inputStream);

        final List<App> apps;
        //sanity checks
        if (appList == null || (apps = appList.app) == null || apps.isEmpty())
            return Collections.emptyList();

        logger.info("Copying to array before sort");

        final int totalApps = apps.size();
        final App[] appArray = new App[totalApps];

        int index = 0;
        for (App app : apps)
            appArray[index++] = app;

        logger.info("Sorting");
        //sort the app array
        Arrays.sort(appArray, new Comparator<App>() {
            @Override
            public int compare(App lhs, App rhs) {

                final Long a = lhs.installDate == null ? 0 : lhs.installDate;
                final Long b = rhs.installDate == null ? 0 : rhs.installDate;
                return a.compareTo(b);
            }
        });

        final List<SimpleApp> toReturn = new ArrayList<>();
        //generate return list of max 20 apps
        for (index = 0; index < 20 && index < totalApps; index++) {

            final SimpleApp simpleApp = new SimpleApp();
            final App app = appArray[index];

            simpleApp.applicationName = app.applicationName;
            simpleApp.description = app.description;
            simpleApp.installDate = app.installDate;
            simpleApp.launchIntentFound = app.launchIntentFound;
            simpleApp.packageName = app.packageName;
            simpleApp.processName = app.processName;
            simpleApp.visible = app.visible;

            toReturn.add(simpleApp);
        }

        return toReturn;
    }
}