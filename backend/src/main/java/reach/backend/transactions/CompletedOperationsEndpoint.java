package reach.backend.transactions;

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
        name = "completedOperationsApi",
        version = "v1",
        resource = "completedOperations",
        namespace = @ApiNamespace(
                ownerDomain = "Entities.backend.reach",
                ownerName = "Entities.backend.reach",
                packagePath = ""
        )
)
public class CompletedOperationsEndpoint {

    private static final Logger logger = Logger.getLogger(CompletedOperationsEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 20;

    static {
        // Typically you would register this inside an OfyServive wrapper. See: https://code.google.com/p/objectify-appengine/wiki/BestPractices
        ObjectifyService.register(CompletedOperations.class);
    }

    /**
     * Returns the {@link CompletedOperations} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code CompletedOperations} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "completedOperations/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CompletedOperations get(@Named("id") Long id) throws NotFoundException {
        logger.info("Getting CompletedOperations with ID: " + id);
        CompletedOperations completedOperations = ofy().load().type(CompletedOperations.class).id(id).now();
        if (completedOperations == null) {
            throw new NotFoundException("Could not find CompletedOperations with ID: " + id);
        }
        return completedOperations;
    }

    /**
     * Inserts a new {@code CompletedOperations}.
     */
    @ApiMethod(
            name = "insert",
            path = "completedOperations",
            httpMethod = ApiMethod.HttpMethod.POST)
    public CompletedOperations insert(CompletedOperations completedOperations) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that completedOperations.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(completedOperations).now();
        logger.info("Created CompletedOperations with ID: " + completedOperations.getId());

        return ofy().load().entity(completedOperations).now();
    }

    /**
     * Updates an existing {@code CompletedOperations}.
     *
     * @param id                  the ID of the entity to be updated
     * @param completedOperations the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code CompletedOperations}
     */
    @ApiMethod(
            name = "update",
            path = "completedOperations/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public CompletedOperations update(@Named("id") Long id, CompletedOperations completedOperations) throws NotFoundException {
        // TODO: You should validate your ID parameter against your resource's ID here.
        checkExists(id);
        ofy().save().entity(completedOperations).now();
        logger.info("Updated CompletedOperations: " + completedOperations);
        return ofy().load().entity(completedOperations).now();
    }

    /**
     * Deletes the specified {@code CompletedOperations}.
     *
     * @param id the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code CompletedOperations}
     */
    @ApiMethod(
            name = "remove",
            path = "completedOperations/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(CompletedOperations.class).id(id).now();
        logger.info("Deleted CompletedOperations with ID: " + id);
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
            path = "completedOperations",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<CompletedOperations> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<CompletedOperations> query = ofy().load().type(CompletedOperations.class).limit(limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<CompletedOperations> queryIterator = query.iterator();
        List<CompletedOperations> completedOperationsList = new ArrayList<CompletedOperations>(limit);
        while (queryIterator.hasNext()) {
            completedOperationsList.add(queryIterator.next());
        }
        return CollectionResponse.<CompletedOperations>builder().setItems(completedOperationsList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(CompletedOperations.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find CompletedOperations with ID: " + id);
        }
    }
}