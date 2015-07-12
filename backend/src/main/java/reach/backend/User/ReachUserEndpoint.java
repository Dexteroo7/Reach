package reach.backend.User;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.repackaged.com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;

import reach.backend.ObjectWrappers.MyString;
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
public class ReachUserEndpoint {

    private static final Logger logger = Logger.getLogger(ReachUserEndpoint.class.getName());

    static {
        ObjectifyService.register(ReachUser.class);
    }

    private ReachFriend getReachFriendFromUser(ReachUser host, MemcacheService service, ReachUser client) {

        final long currentTime = System.currentTimeMillis();
        final byte[] value = (byte[]) service.get(host.getId());
        final String val;
        final ReachFriend reachFriend;
        if (value != null && value.length > 0 && !((val = new String(value)).equals("")))
            reachFriend = new ReachFriend(host, currentTime - Long.parseLong(val), client);
        else if (!host.getId().equals(client.getId()))
            reachFriend = new ReachFriend(host, currentTime, client);
        else
            reachFriend = null;
        return reachFriend;
    }

    @ApiMethod(
            name = "returnUsersNew",
            path = "user/returnUsersNew/{clientId}/",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public Set<ReachFriend> returnUsersNew(@Named("clientId") final long clientId,
                                           final ContactsWrapper contactsWrapper) {

        if (clientId == 0)
            return null;

        final Set<ReachFriend> toSend = new HashSet<>(100);
        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final ReachUser client = ofy().load().type(ReachUser.class).id(clientId).now();
        if (client == null || client.getUserName() == null)
            return null;
        logger.info(client.getUserName());
        //////////////////////////////////////
        final Map<Long, ReachUser> myReach;
        if (client.getMyReach() != null && client.getMyReach().size() > 0)
            myReach = ofy().load().type(ReachUser.class).ids(client.getMyReach());
        else
            myReach = null;
        contactsWrapper.getContacts().add("8860872102");

        //////////////////////////////////////
        try {
            /**
             * Process the PhoneBookContacts
             */
            for (ReachUser host : ofy().load().type(ReachUser.class)
                    .filter("phoneNumber in", contactsWrapper.getContacts())
                    .filter("gcmId !=", "")) {

                if (host == null || host.getGcmId() == null || host.getGcmId().equals("") || host.getUserName() == null || host.getUserName().equals("") || host.getId() == clientId) {
                    continue;
                }
                final ReachFriend reachFriend = getReachFriendFromUser(host, syncCache, client);
                if (reachFriend != null)
                    toSend.add(reachFriend);
            }
            contactsWrapper.getContacts().clear();
            /**
             * Complete The asynchronous fetch by id call
             */
            if (myReach != null) {

                for (ReachUser host : myReach.values()) {

                    if (host == null || host.getGcmId() == null || host.getGcmId().equals("") ||
                            host.getUserName() == null || host.getUserName().equals("") || host.getId() == clientId)
                        continue;
                    final ReachFriend reachFriend = getReachFriendFromUser(host, syncCache, client);
                    if (reachFriend != null)
                        toSend.add(reachFriend);
                }
                myReach.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return toSend;
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
        final ReachUser clientUser = OfyService.ofy().load().type(ReachUser.class).id(clientId).now();
        if (clientUser == null || clientUser.getMyReach() == null || clientUser.getMyReach().size() == 0)
            return null;
        logger.info(clientUser.getUserName() + " sending PONG");
        final ImmutableList.Builder<Key> keysBuilder = new ImmutableList.Builder<>();
        for (Long id : clientUser.getMyReach())
            keysBuilder.add(Key.create(ReachUser.class, id));
        keysBuilder.add(Key.create(ReachUser.class, clientId));

        return new MyString(new MessagingEndpoint().sendMultiCastMessage("PING",
                OfyService.ofy().load().type(ReachUser.class)
                        .filterKey("in", keysBuilder.build())
                        .filter("gcmId !=", "")
                        .project("gcmId")) + "");
    }

    @ApiMethod(
            name = "getMusicWrapper",
            path = "user/songs/{hostId}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MusicContainer getMusicWrapper(@Named("hostId") long hostId,
                                          @Named("clientId") long clientId,
                                          @Named("songCode") int songCode,
                                          @Named("playListCode") int playListCode) {

        if (hostId == 0 || clientId == 0)
            return null;

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final ReachUser reachUser = ofy().load().type(ReachUser.class).id(hostId).now();
        if (reachUser == null) {
            logger.info("Fetching songs failed user was null");
            return null;
        }

        final SplitMusicContainer musicContainer;
        if (reachUser.getMegaBytesSent() == 0)
            musicContainer = null;
        else
            musicContainer = ofy().load().type(SplitMusicContainer.class).id(reachUser.getMegaBytesSent()).now();
        if (musicContainer != null) {

            final Collection<MusicSplitter> musicSplitters = ofy().load().type(MusicSplitter.class).ids(musicContainer.getSplitIds()).values();
            if (reachUser.getMySongs() == null)
                reachUser.setMySongs(new HashSet<ReachSong>());
            for (MusicSplitter musicSplitter : musicSplitters) {
                if (musicSplitter.getReachSongList() != null) {
                    logger.info("loading split songs " + musicSplitter.getReachSongList().size());
                    reachUser.getMySongs().addAll(musicSplitter.getReachSongList());
                }
            }
        }

        final boolean isSongsChanged;
        if (reachUser.getMySongs() == null || reachUser.getMySongs().size() == 0)
            isSongsChanged = songCode != 0;
        else isSongsChanged = reachUser.getMySongs().hashCode() != songCode;

        final boolean isPlayListsChanged;
        if (reachUser.getMyPlayLists() == null || reachUser.getMyPlayLists().size() == 0)
            isPlayListsChanged = playListCode != 0;
        else isPlayListsChanged = reachUser.getMyPlayLists().hashCode() != playListCode;

        final HashSet<ReachSong> reachSongs = (!isSongsChanged) ? null : reachUser.getMySongs();
        final HashSet<ReachPlayList> reachPlayLists = (!isPlayListsChanged) ? null : reachUser.getMyPlayLists();

        return new MusicContainer(reachSongs, reachPlayLists, isSongsChanged, isPlayListsChanged,
                (reachSongs == null) ? 0 : reachSongs.hashCode(),
                (reachPlayLists == null) ? 0 : reachPlayLists.hashCode());
    }

    @ApiMethod(
            name = "updateMusic",
            path = "user/updateMusic",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void updateMusic(MusicContainer musicContainer) {

        final ReachUser userToSave = ofy().load().type(ReachUser.class).id(musicContainer.getClientId()).now();
        if (userToSave == null) {
            logger.info(musicContainer.getClientId() + " no user found");
            return;
        }
        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(userToSave.getId(), (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);
        logger.info("updating user music " + userToSave.getUserName() + " " + userToSave.getPhoneNumber());

        ///////////////////////////////////
        userToSave.setMegaBytesReceived(musicContainer.getReachSongs().size());
        if (musicContainer.getGenres().length() > 499)
            musicContainer.setGenres(musicContainer.getGenres().substring(0, 499));
        userToSave.setGenres(musicContainer.getGenres());
        userToSave.setMyPlayLists(musicContainer.getReachPlayLists());

        if (musicContainer.getReachSongs().size() > 500) {

            final ArrayList<MusicSplitter> splitter = Lists.newArrayList(getSplitter(userToSave, true));
            final List<List<ReachSong>> splits = Lists.partition(ImmutableList.copyOf(musicContainer.getReachSongs()), musicContainer.getReachSongs().size() / 5);
            if (splits.size() > splitter.size() + 1) {
                logger.info("TOO MANY SONGS " + splits.size() + " " + musicContainer.getReachSongs().size());
                return;
            }
            userToSave.setMySongs(new HashSet<>(splits.get(0)));
            logger.info("Splitting into " + userToSave.getMySongs().size());
            for (int i = 0; i < 5 && i < splits.size() - 1; i++) {
                logger.info("Splitting into " + splits.get(i + 1).size());
                splitter.get(i).setReachSongList(new HashSet<>(splits.get(i + 1)));
            }
            ofy().save().entities(splitter);
        } else
            userToSave.setMySongs(musicContainer.getReachSongs());

        ofy().save().entity(userToSave).now();
    }

    @ApiMethod(
            name = "getStats",
            path = "user/getStats",
            httpMethod = ApiMethod.HttpMethod.GET)
    public DataCall getStats(@Named("cursor") String cursor) {

        final ImmutableList.Builder<DataCall.Statistics> builder = new ImmutableList.Builder<>();
        QueryResultIterator<ReachUser> userQueryResultIterator;
        if (cursor != null && !cursor.trim().equals(""))
            userQueryResultIterator = OfyService.ofy().load().type(ReachUser.class)
                    .startAt(Cursor.fromWebSafeString(cursor))
                    .limit(100)
                    .iterator();
        else
            userQueryResultIterator = OfyService.ofy().load().type(ReachUser.class).limit(100).iterator();
        ReachUser reachUser;
        int count = 0;

        while (userQueryResultIterator.hasNext()) {

            reachUser = userQueryResultIterator.next();

            final long friends;
            if (reachUser.getMyReach() != null)
                friends = reachUser.getMyReach().size();
            else
                friends = 0;

            builder.add(new DataCall.Statistics(
                    reachUser.getUserName(),
                    reachUser.getPhoneNumber(),
                    reachUser.getNumberOfSongs(),
                    friends,
                    reachUser.getGcmId() != null && !reachUser.getGcmId().equals("")));
            count++;
        }

        //if no more friends return null cursor
        return new DataCall(
                builder.build(),
                (count == 100) ? userQueryResultIterator.getCursor().toWebSafeString() : "");
    }

    @ApiMethod(
            name = "getStatsNew",
            path = "user/getStatsNew",
            httpMethod = ApiMethod.HttpMethod.GET)
    public DataCall getStatsNew(@Named("cursor") String cursor) {

        final ImmutableList.Builder<DataCall.Statistics> builder = new ImmutableList.Builder<>();
        final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        final QueryResultIterator<ReachUser> userQueryResultIterator;
        final Iterator<Entity> friendsIterator;
        final Query friendsQuery = new Query();
        friendsQuery.addProjection(new PropertyProjection("myReach", ReachUser.class));

        if (cursor != null && !cursor.trim().equals("")) {

            userQueryResultIterator = ofy().load().type(ReachUser.class)
                    .startAt(Cursor.fromWebSafeString(cursor))
                    .project("userName", "phoneNumber", "megaBytesReceived", "gcmId")
                    .limit(200)
                    .iterator();
            friendsIterator = datastoreService.prepare(friendsQuery).asIterator(FetchOptions.Builder
                    .withLimit(200)
                    .startCursor(Cursor.fromWebSafeString(cursor)));
        } else {
            userQueryResultIterator = ofy().load().type(ReachUser.class)
                    .project("userName", "phoneNumber", "megaBytesReceived", "gcmId")
                    .limit(200)
                    .iterator();
            friendsIterator = datastoreService.prepare(friendsQuery).asIterator(FetchOptions.Builder.withLimit(200));
        }
        ReachUser reachUser;
        int count = 0;

        while (userQueryResultIterator.hasNext() && friendsIterator.hasNext()) {

            reachUser = userQueryResultIterator.next();
            final Entity friend = friendsIterator.next();
            final int friendsCount;
            if (friend == null)
                friendsCount = 0;
            else {

                final Iterator ff = ((Iterable) friend.getProperty("myReach")).iterator();
                int i = 0;
                while (ff.hasNext() && ff.next() != null)
                    i++;
                friendsCount = i;
            }

            builder.add(new DataCall.Statistics(
                    reachUser.getUserName(),
                    reachUser.getPhoneNumber(),
                    reachUser.getNumberOfSongs(),
                    friendsCount,
                    reachUser.getGcmId() != null && !reachUser.getGcmId().equals("")));
            count++;
        }

        //if no more friends return null cursor
        return new DataCall(
                builder.build(),
                (count == 200) ? userQueryResultIterator.getCursor().toWebSafeString() : "");
    }

    @ApiMethod(
            name = "getReachUser",
            path = "user/{hostId}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public ReachUser getReachUser(@Named("hostId") long hostId, @Named("clientId") long clientId) throws NotFoundException {

        if (hostId == 0 || clientId == 0)
            return null;

        logger.info("Getting ReachUser with ID: " + hostId);
        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);
        final ReachUser user = ofy().load().type(ReachUser.class).id(hostId).now();
        if (user == null) {
            throw new NotFoundException("Could not find ReachUser with ID: " + hostId);
        }
        return user;
    }

    @ApiMethod(
            name = "getReachFriend",
            path = "user/friend/{hostId}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public ReachFriend getReachFriend(@Named("hostId") long hostId, @Named("clientId") long clientId) throws NotFoundException {

        if (hostId == 0 || clientId == 0)
            return null;

        logger.info("Getting ReachUser with ID: " + hostId);
        final ReachUser host = ofy().load().type(ReachUser.class).id(hostId).now();
        final ReachUser client = ofy().load().type(ReachUser.class).id(clientId).now();
        if (host == null || client == null)
            throw new NotFoundException("Could not find ReachUser with ID: " + hostId);

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        final long currentTime = System.currentTimeMillis();
        final byte[] value = (byte[]) syncCache.get(hostId);
        final String val;

        if (value != null && value.length > 0 &&
                !((val = new String(value)).equals("")))
            return new ReachFriend(host, currentTime - Long.parseLong(val), client);
        else if (!host.getId().equals(clientId))
            return new ReachFriend(host, currentTime, client);
        return null;
    }

    @ApiMethod(
            name = "insert",
            path = "user",
            httpMethod = ApiMethod.HttpMethod.POST)
    public MyString insert(ReachUser user) {

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
            user.setPromoCode(oldUser.getPromoCode());
            ofy().delete().entity(oldUser);
        }

        getSplitter(user, false);
        ofy().save().entity(user).now();
        logger.info("Created ReachUser with ID: " + user.getUserName() + " " + new MessagingEndpoint().sendMessage("PING", user));
        return new MyString(user.getId() + "");
    }

    private Collection<MusicSplitter> getSplitter(ReachUser reachUser, boolean wait) {

        SplitMusicContainer splitMusicContainer;
        if (reachUser.getMegaBytesSent() == 0)
            splitMusicContainer = null;
        else
            splitMusicContainer = ofy().load().type(SplitMusicContainer.class).id(reachUser.getMegaBytesSent()).now();

        final Collection<MusicSplitter> musicSplitters;

        if (splitMusicContainer == null) {

            logger.info("Creating new Splitters");
            splitMusicContainer = new SplitMusicContainer();
            musicSplitters = Arrays.asList(
                    new MusicSplitter(),
                    new MusicSplitter(),
                    new MusicSplitter(),
                    new MusicSplitter(),
                    new MusicSplitter());
            ofy().save().entities(musicSplitters).now();
            splitMusicContainer.setSplitIds(new HashSet<Long>());
            for (MusicSplitter splitter : musicSplitters)
                splitMusicContainer.getSplitIds().add(splitter.getId());
            ofy().save().entity(splitMusicContainer).now();
            reachUser.setMegaBytesSent(splitMusicContainer.getId());
        } else {

            logger.info("Reusing existing splitters");
            musicSplitters = ofy().load().type(MusicSplitter.class).ids(splitMusicContainer.getSplitIds()).values();
            for (MusicSplitter musicSplitter : musicSplitters)
                musicSplitter.setReachSongList(null);
            if (!wait)
                ofy().save().entities(musicSplitters).now();
        }
        return musicSplitters;
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
    public void updateUserDetails(@Named("clientId") long clientId, @Named("details") String[] details) {

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

        return getCompleted(reachUser);
    }

    private HashSet<CompletedOperation> getCompleted(ReachUser reachUser) {

        final HashMap<Integer, CompletedOperation> operationHashMap = new HashMap<>();
        ////////////////////////////////////
        for (CompletedOperations completedOperations : ofy().load().type(CompletedOperations.class)
                .filter("senderId =", reachUser.getId())) {

            CompletedOperation completedOperation = operationHashMap.get(completedOperations.hashCode());
            if (completedOperation == null) {

                completedOperation = new CompletedOperation(
                        completedOperations.getSongName(),
                        completedOperations.getSongSize(),
                        completedOperations.getSenderId());
                completedOperation.setReceiver(new HashSet<Long>());
                completedOperation.setHits(completedOperations.getHits());
                completedOperation.setTime(completedOperations.getTime());
                completedOperation.getReceiver().add(completedOperations.getReceiver());
                operationHashMap.put(completedOperation.hashCode(), completedOperation);
            } else {

                completedOperation.setHits(completedOperation.getHits() + 1);
                //update the time only if newer
                if (completedOperations.getTime() > completedOperation.getTime())
                    completedOperation.setTime(completedOperations.getTime());
                completedOperation.getReceiver().add(completedOperations.getReceiver());
            }
        }
        return new HashSet<>(operationHashMap.values());
    }

    @ApiMethod(
            name = "setGCMId",
            path = "user/gcmId",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void setGCMId(@Named("clientId") long clientId, @Named("gcmId") String gcmId) {

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
                    oldUser.getUserName(), oldUser.getPromoCode(), oldUser.getImageId());
        return null;
    }

    @ApiMethod(
            name = "storePromoCode",
            path = "user/storePromoCode/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MyString storePromoCode(@Named("id") long id, @Named("promoCode") String promoCode) {

        final ReachUser oldUser = ofy().load().type(ReachUser.class).id(id).now();
        if (oldUser == null)
            return new MyString("false");
        oldUser.setPromoCode(promoCode);
        ofy().save().entity(oldUser).now();
        return new MyString("true");
    }

    @ApiMethod(
            name = "toggleVisibility",
            path = "user/visibility/{clientId}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public MyString toggleVisibility(@Named("clientId") long clientId, @Named("songId") long songId) {

        if (clientId == 0)
            return null;

        final ReachUser reachUser = ofy().load().type(ReachUser.class).id(clientId).now();
        if (reachUser == null || reachUser.getMySongs() == null || reachUser.getMySongs().size() == 0) {
            logger.info("toggleVisibility for invalid user");
            return new MyString("false");
        }
        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(clientId, (System.currentTimeMillis() + "").getBytes(),
                Expiration.byDeltaSeconds(30 * 60), MemcacheService.SetPolicy.SET_ALWAYS);

        for (ReachSong reachSong : reachUser.getMySongs()) {

            if (reachSong != null && reachSong.getSongId() == songId) {

                logger.info("Song found for toggle in main list " + reachUser.getUserName());
                if (reachSong.getVisibility() == 1)
                    reachSong.setVisibility((short) 0);
                else
                    reachSong.setVisibility((short) 1);
                ofy().save().entities(reachUser).now();
                return new MyString("true");
            }
        }
        logger.info("Song not found for toggle in main list " + reachUser.getUserName());

        if (reachUser.getMegaBytesSent() == 0)
            return new MyString("false");

        final SplitMusicContainer musicContainer = ofy().load().type(SplitMusicContainer.class).id(reachUser.getMegaBytesSent()).now();
        if (musicContainer == null || musicContainer.getSplitIds() == null || musicContainer.getSplitIds().isEmpty())
            return new MyString("false");
        final Collection<MusicSplitter> musicSplitters = ofy().load().type(MusicSplitter.class).ids(musicContainer.getSplitIds()).values();

        for (MusicSplitter musicSplitter : musicSplitters) {

            if (musicSplitter.getReachSongList() == null || musicSplitter.getReachSongList().isEmpty())
                continue;
            for (ReachSong reachSong : musicSplitter.getReachSongList()) {

                if (reachSong != null && reachSong.getSongId() == songId) {
                    logger.info("Found song for toggle in extended list " + reachUser.getUserName());
                    if (reachSong.getVisibility() == 1)
                        reachSong.setVisibility((short) 0);
                    else
                        reachSong.setVisibility((short) 1);
                    ofy().save().entities(musicSplitter).now();
                    return new MyString("true");
                }
            }
        }
        logger.info("Song not found for toggle " + reachUser.getUserName());
        return new MyString("false");
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

    ///stuff to remove
    @ApiMethod(
            name = "isAccountPresent",
            path = "user/isAccountPresent/{phoneNumber}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public OldUserContainer isAccountPresent(@Named("phoneNumber") String phoneNumber) {

        final ReachUser oldUser = ofy().load().type(ReachUser.class)
                .filter("phoneNumber in", Collections.singletonList(phoneNumber)).first().now();
        if (oldUser != null) {
            final String[] userName = oldUser.getUserName().trim().split(" ");
            if (userName.length == 0)
                return null;
            if (userName.length == 1)
                return new OldUserContainer(userName[0], "", oldUser.getImageId());
            else
                return new OldUserContainer(userName[0], userName[1], oldUser.getImageId());
        }
        return null;
    }
}