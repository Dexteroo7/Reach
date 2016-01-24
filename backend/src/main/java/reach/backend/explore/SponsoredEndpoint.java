package reach.backend.explore;

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
 * <p>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "sponsoredApi",
        version = "v1",
        resource = "sponsored",
        namespace = @ApiNamespace(
                ownerDomain = "explore.backend.reach",
                ownerName = "explore.backend.reach",
                packagePath = ""
        )
)
public class SponsoredEndpoint {

    private static final Logger logger = Logger.getLogger(SponsoredEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 20;

    /**
     * Returns the {@link Sponsored} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code Sponsored} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "sponsored/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public Sponsored get(@Named("id") Long id) throws NotFoundException {
        logger.info("Getting Sponsored with ID: " + id);
        Sponsored sponsored = ofy().load().type(Sponsored.class).id(id).now();
        if (sponsored == null) {
            throw new NotFoundException("Could not find Sponsored with ID: " + id);
        }
        return sponsored;
    }

    /**
     * Inserts a new {@code Sponsored}.
     */
    @ApiMethod(
            name = "insert",
            path = "sponsored",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Sponsored insert(Sponsored sponsored) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that sponsored.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(sponsored).now();
        logger.info("Created Sponsored with ID: " + sponsored.getId());

        return ofy().load().entity(sponsored).now();
    }

    /**
     * Updates an existing {@code Sponsored}.
     *
     * @param id        the ID of the entity to be updated
     * @param sponsored the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code Sponsored}
     */
    @ApiMethod(
            name = "update",
            path = "sponsored/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public Sponsored update(@Named("id") Long id, Sponsored sponsored) throws NotFoundException {
        // TODO: You should validate your ID parameter against your resource's ID here.
        checkExists(id);
        ofy().save().entity(sponsored).now();
        logger.info("Updated Sponsored: " + sponsored);
        return ofy().load().entity(sponsored).now();
    }

    /**
     * Deletes the specified {@code Sponsored}.
     *
     * @param id the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code Sponsored}
     */
    @ApiMethod(
            name = "remove",
            path = "sponsored/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(Sponsored.class).id(id).now();
        logger.info("Deleted Sponsored with ID: " + id);
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
            path = "sponsored",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<Sponsored> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<Sponsored> query = ofy().load().type(Sponsored.class).limit(limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<Sponsored> queryIterator = query.iterator();
        List<Sponsored> sponsoredList = new ArrayList<>(limit);
        while (queryIterator.hasNext()) {
            sponsoredList.add(queryIterator.next());
        }
        return CollectionResponse.<Sponsored>builder().setItems(sponsoredList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(Sponsored.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find Sponsored with ID: " + id);
        }
    }
}