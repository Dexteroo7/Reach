package reach.backend.User;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
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
        name = "musicSplitterApi",
        version = "v1",
        resource = "musicSplitter",
        namespace = @ApiNamespace(
                ownerDomain = "Entities.backend.reach",
                ownerName = "Entities.backend.reach",
                packagePath = ""
        )
)
public class MusicSplitterEndpoint {

    private static final Logger logger = Logger.getLogger(MusicSplitterEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 20;

    static {
        // Typically you would register this inside an OfyServive wrapper. See: https://code.google.com/p/objectify-appengine/wiki/BestPractices
        ObjectifyService.register(MusicSplitter.class);
    }

    /**
     * Returns the {@link MusicSplitter} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code MusicSplitter} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "musicSplitter/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MusicSplitter get(@Named("id") Long id) throws NotFoundException {
        logger.info("Getting MusicSplitter with ID: " + id);
        MusicSplitter musicSplitter = ofy().load().type(MusicSplitter.class).id(id).now();
        if (musicSplitter == null) {
            throw new NotFoundException("Could not find MusicSplitter with ID: " + id);
        }
        return musicSplitter;
    }

    /**
     * Inserts a new {@code MusicSplitter}.
     */
    @ApiMethod(
            name = "insert",
            path = "musicSplitter",
            httpMethod = ApiMethod.HttpMethod.POST)
    public MusicSplitter insert(MusicSplitter musicSplitter) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that musicSplitter.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(musicSplitter).now();
        logger.info("Created MusicSplitter with ID: " + musicSplitter.getId());

        return ofy().load().entity(musicSplitter).now();
    }

    /**
     * Updates an existing {@code MusicSplitter}.
     *
     * @param id            the ID of the entity to be updated
     * @param musicSplitter the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code MusicSplitter}
     */
    @ApiMethod(
            name = "update",
            path = "musicSplitter/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public MusicSplitter update(@Named("id") Long id, MusicSplitter musicSplitter) throws NotFoundException {
        // TODO: You should validate your ID parameter against your resource's ID here.
        checkExists(id);
        ofy().save().entity(musicSplitter).now();
        logger.info("Updated MusicSplitter: " + musicSplitter);
        return ofy().load().entity(musicSplitter).now();
    }

    /**
     * Deletes the specified {@code MusicSplitter}.
     *
     * @param id the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code MusicSplitter}
     */
    @ApiMethod(
            name = "remove",
            path = "musicSplitter/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(MusicSplitter.class).id(id).now();
        logger.info("Deleted MusicSplitter with ID: " + id);
    }

    /**
     * List all entities.
     *
     * @param cursor used for pagination to determine which page to return
     * @param limit  the maximum number of entries to return
     * @return a response that encapsulates the result list and the next page token/cursor
     */
    @ApiMethod(
            name = "list",
            path = "musicSplitter",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<MusicSplitter> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<MusicSplitter> query = ofy().load().type(MusicSplitter.class).limit(limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<MusicSplitter> queryIterator = query.iterator();
        List<MusicSplitter> musicSplitterList = new ArrayList<MusicSplitter>(limit);
        while (queryIterator.hasNext()) {
            musicSplitterList.add(queryIterator.next());
        }
        return CollectionResponse.<MusicSplitter>builder().setItems(musicSplitterList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(MusicSplitter.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find MusicSplitter with ID: " + id);
        }
    }
}