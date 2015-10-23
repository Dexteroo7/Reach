package reach.backend.ActiveCountLog;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.ObjectifyService;

import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "activeCountLogApi",
        version = "v1",
        resource = "activeCountLog",
        namespace = @ApiNamespace(
                ownerDomain = "ActiveCountLog.backend.reach",
                ownerName = "ActiveCountLog.backend.reach",
                packagePath = ""
        )
)
public class ActiveCountLogEndpoint {

    private static final Logger logger = Logger.getLogger(ActiveCountLogEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 20;

    static {
        // Typically you would register this inside an OfyServive wrapper. See: https://code.google.com/p/objectify-appengine/wiki/BestPractices
        ObjectifyService.register(ActiveCountLog.class);
    }

    /**
     * Returns the {@link ActiveCountLog} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code ActiveCountLog} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "activeCountLog/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public ActiveCountLog get(@Named("id") Long id) throws NotFoundException {
        logger.info("Getting ActiveCountLog with ID: " + id);
        ActiveCountLog activeCountLog = ofy().load().type(ActiveCountLog.class).id(id).now();
        if (activeCountLog == null) {
            throw new NotFoundException("Could not find ActiveCountLog with ID: " + id);
        }
        return activeCountLog;
    }

    /**
     * Inserts a new {@code ActiveCountLog}.
     */
    @ApiMethod(
            name = "insert",
            path = "activeCountLog",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ActiveCountLog insert(ActiveCountLog activeCountLog) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that activeCountLog.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(activeCountLog).now();
        logger.info("Created ActiveCountLog with ID: " + activeCountLog.getId());

        return ofy().load().entity(activeCountLog).now();
    }

    /**
     * Updates an existing {@code ActiveCountLog}.
     *
     * @param id             the ID of the entity to be updated
     * @param activeCountLog the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code ActiveCountLog}
     */
    @ApiMethod(
            name = "update",
            path = "activeCountLog/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public ActiveCountLog update(@Named("id") Long id, ActiveCountLog activeCountLog) throws NotFoundException {
        // TODO: You should validate your ID parameter against your resource's ID here.
        checkExists(id);
        ofy().save().entity(activeCountLog).now();
        logger.info("Updated ActiveCountLog: " + activeCountLog);
        return ofy().load().entity(activeCountLog).now();
    }

    /**
     * Deletes the specified {@code ActiveCountLog}.
     *
     * @param id the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code ActiveCountLog}
     */
    @ApiMethod(
            name = "remove",
            path = "activeCountLog/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(ActiveCountLog.class).id(id).now();
        logger.info("Deleted ActiveCountLog with ID: " + id);
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
            path = "activeCountLog",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<ActiveCountLog> list(@Nullable @Named("cursor") String cursor,
                                                   @Nullable @Named("limit") Integer limit) {

        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        final ImmutableList.Builder<ActiveCountLog> builder = new ImmutableList.Builder<>();
        final QueryResultIterator<ActiveCountLog> queryIterator;

        if (cursor != null && !(cursor = cursor.trim()).equals(""))
            queryIterator = ofy().load().type(ActiveCountLog.class)
                    .limit(limit)
                    .order("-currentTime")
                    .startAt(Cursor.fromWebSafeString(cursor)).iterator();
        else
            queryIterator = ofy().load().type(ActiveCountLog.class)
                    .limit(limit)
                    .order("-currentTime").iterator();

        while (queryIterator.hasNext())
            builder.add(queryIterator.next());

        return CollectionResponse.<ActiveCountLog>builder().
                setItems(builder.build())
                .setNextPageToken(queryIterator.getCursor().toWebSafeString())
                .build();
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(ActiveCountLog.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find ActiveCountLog with ID: " + id);
        }
    }
}