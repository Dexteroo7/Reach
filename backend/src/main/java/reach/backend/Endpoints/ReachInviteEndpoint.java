package reach.backend.Endpoints;

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

import reach.backend.Entities.ReachInvite;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p/>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "reachInviteApi",
        version = "v1",
        resource = "reachInvite",
        namespace = @ApiNamespace(
                ownerDomain = "Entities.backend.reach",
                ownerName = "Entities.backend.reach",
                packagePath = ""
        )
)
public class ReachInviteEndpoint {

    private static final Logger logger = Logger.getLogger(ReachInviteEndpoint.class.getName());
    private static final int DEFAULT_LIST_LIMIT = 20;

    /**
     * Returns the {@link ReachInvite} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code ReachInvite} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "reachInvite/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public ReachInvite get(@Named("id") Long id) throws NotFoundException {
        logger.info("Getting ReachInvite with ID: " + id);
        ReachInvite reachInvite = ofy().load().type(ReachInvite.class).id(id).now();
        if (reachInvite == null) {
            throw new NotFoundException("Could not find ReachInvite with ID: " + id);
        }
        return reachInvite;
    }

    /**
     * Inserts a new {@code ReachInvite}.
     */
    @ApiMethod(
            name = "insert",
            path = "reachInvite",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ReachInvite insert(ReachInvite reachInvite) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that reachInvite.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(reachInvite).now();
        logger.info("Created ReachInvite with ID: " + reachInvite.getId());

        return ofy().load().entity(reachInvite).now();
    }

    /**
     * Updates an existing {@code ReachInvite}.
     *
     * @param id          the ID of the entity to be updated
     * @param reachInvite the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code ReachInvite}
     */
    @ApiMethod(
            name = "update",
            path = "reachInvite/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public ReachInvite update(@Named("id") Long id, ReachInvite reachInvite) throws NotFoundException {
        // TODO: You should validate your ID parameter against your resource's ID here.
        checkExists(id);
        ofy().save().entity(reachInvite).now();
        logger.info("Updated ReachInvite: " + reachInvite);
        return ofy().load().entity(reachInvite).now();
    }

    /**
     * Deletes the specified {@code ReachInvite}.
     *
     * @param id the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code ReachInvite}
     */
    @ApiMethod(
            name = "remove",
            path = "reachInvite/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(ReachInvite.class).id(id).now();
        logger.info("Deleted ReachInvite with ID: " + id);
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
            path = "reachInvite",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<ReachInvite> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<ReachInvite> query = ofy().load().type(ReachInvite.class).limit(limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<ReachInvite> queryIterator = query.iterator();
        List<ReachInvite> reachInviteList = new ArrayList<ReachInvite>(limit);
        while (queryIterator.hasNext()) {
            reachInviteList.add(queryIterator.next());
        }
        return CollectionResponse.<ReachInvite>builder().setItems(reachInviteList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(ReachInvite.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find ReachInvite with ID: " + id);
        }
    }
}