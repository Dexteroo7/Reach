package reach.backend.campaign;

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
 * <p>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "backLogApi",
        version = "v1",
        resource = "backLog",
        namespace = @ApiNamespace(
                ownerDomain = "campaign.backend.reach",
                ownerName = "campaign.backend.reach",
                packagePath = ""
        )
)
public class BackLogEndpoint {

    private static final Logger logger = Logger.getLogger(BackLogEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 20;

    static {
        // Typically you would register this inside an OfyServive wrapper. See: https://code.google.com/p/objectify-appengine/wiki/BestPractices
        ObjectifyService.register(BackLog.class);
    }

    /**
     * Returns the {@link BackLog} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code BackLog} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "backLog/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public BackLog get(@Named("id") Long id) throws NotFoundException {
        logger.info("Getting BackLog with ID: " + id);
        BackLog backLog = ofy().load().type(BackLog.class).id(id).now();
        if (backLog == null) {
            throw new NotFoundException("Could not find BackLog with ID: " + id);
        }
        return backLog;
    }

    /**
     * Inserts a new {@code BackLog}.
     */
    @ApiMethod(
            name = "insert",
            path = "backLog",
            httpMethod = ApiMethod.HttpMethod.POST)
    public BackLog insert(BackLog backLog) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that backLog.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(backLog).now();
        logger.info("Created BackLog with ID: " + backLog.getId());

        return ofy().load().entity(backLog).now();
    }

    /**
     * Updates an existing {@code BackLog}.
     *
     * @param id      the ID of the entity to be updated
     * @param backLog the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code BackLog}
     */
    @ApiMethod(
            name = "update",
            path = "backLog/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public BackLog update(@Named("id") Long id, BackLog backLog) throws NotFoundException {
        // TODO: You should validate your ID parameter against your resource's ID here.
        checkExists(id);
        ofy().save().entity(backLog).now();
        logger.info("Updated BackLog: " + backLog);
        return ofy().load().entity(backLog).now();
    }

    /**
     * Deletes the specified {@code BackLog}.
     *
     * @param id the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code BackLog}
     */
    @ApiMethod(
            name = "remove",
            path = "backLog/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(BackLog.class).id(id).now();
        logger.info("Deleted BackLog with ID: " + id);
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
            path = "backLog",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<BackLog> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<BackLog> query = ofy().load().type(BackLog.class).limit(limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<BackLog> queryIterator = query.iterator();
        List<BackLog> backLogList = new ArrayList<>(limit);
        while (queryIterator.hasNext()) {
            backLogList.add(queryIterator.next());
        }
        return CollectionResponse.<BackLog>builder().setItems(backLogList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(BackLog.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find BackLog with ID: " + id);
        }
    }
}