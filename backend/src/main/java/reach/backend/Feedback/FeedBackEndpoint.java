package reach.backend.Feedback;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
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
        name = "feedBackApi",
        version = "v1",
        resource = "feedBack",
        namespace = @ApiNamespace(
                ownerDomain = "Entities.backend.reach",
                ownerName = "Entities.backend.reach",
                packagePath = ""
        )
)
public class FeedBackEndpoint {

    private static final Logger logger = Logger.getLogger(FeedBackEndpoint.class.getName());
    private static final int DEFAULT_LIST_LIMIT = 20;

    /**
     * Returns the {@link FeedBack} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code FeedBack} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "feedBack/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public FeedBack get(@Named("id") Long id) throws NotFoundException {
        logger.info("Getting FeedBack with ID: " + id);
        FeedBack feedBack = ofy().load().type(FeedBack.class).id(id).now();
        if (feedBack == null) {
            throw new NotFoundException("Could not find FeedBack with ID: " + id);
        }
        return feedBack;
    }

    /**
     * Inserts a new {@code FeedBack}.
     */
    @ApiMethod(
            name = "insert",
            path = "feedBack",
            httpMethod = ApiMethod.HttpMethod.POST)
    public FeedBack insert(FeedBack feedBack) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that feedBack.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(feedBack).now();
        logger.info("Created FeedBack with ID: " + feedBack.getId());

        return ofy().load().entity(feedBack).now();
    }

    /**
     * Updates an existing {@code FeedBack}.
     *
     * @param id       the ID of the entity to be updated
     * @param feedBack the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code FeedBack}
     */
    @ApiMethod(
            name = "update",
            path = "feedBack/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public FeedBack update(@Named("id") Long id, FeedBack feedBack) throws NotFoundException {
        // TODO: You should validate your ID parameter against your resource's ID here.
        checkExists(id);
        ofy().save().entity(feedBack).now();
        logger.info("Updated FeedBack: " + feedBack);
        return ofy().load().entity(feedBack).now();
    }

    /**
     * Deletes the specified {@code FeedBack}.
     *
     * @param id the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code FeedBack}
     */
    @ApiMethod(
            name = "remove",
            path = "feedBack/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(FeedBack.class).id(id).now();
        logger.info("Deleted FeedBack with ID: " + id);
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
            path = "feedBack",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<FeedBack> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<FeedBack> query = ofy().load().type(FeedBack.class).limit(limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<FeedBack> queryIterator = query.iterator();
        List<FeedBack> feedBackList = new ArrayList<FeedBack>(limit);
        while (queryIterator.hasNext()) {
            feedBackList.add(queryIterator.next());
        }
        return CollectionResponse.<FeedBack>builder().setItems(feedBackList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(FeedBack.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find FeedBack with ID: " + id);
        }
    }
}