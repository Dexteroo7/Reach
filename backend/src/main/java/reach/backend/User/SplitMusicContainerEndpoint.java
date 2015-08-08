package reach.backend.User;

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

import static reach.backend.OfyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p/>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "splitMusicContainerApi",
        version = "v1",
        resource = "splitMusicContainer",
        namespace = @ApiNamespace(
                ownerDomain = "Entities.backend.reach",
                ownerName = "Entities.backend.reach",
                packagePath = ""
        )
)
public class SplitMusicContainerEndpoint {

    private static final Logger logger = Logger.getLogger(SplitMusicContainerEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 20;

    /**
     * Returns the {@link SplitMusicContainer} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code SplitMusicContainer} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "splitMusicContainer/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public SplitMusicContainer get(@Named("id") Long id) throws NotFoundException {
        logger.info("Getting SplitMusicContainer with ID: " + id);
        SplitMusicContainer splitMusicContainer = ofy().load().type(SplitMusicContainer.class).id(id).now();
        if (splitMusicContainer == null) {
            throw new NotFoundException("Could not find SplitMusicContainer with ID: " + id);
        }
        return splitMusicContainer;
    }

    /**
     * Inserts a new {@code SplitMusicContainer}.
     */
    @ApiMethod(
            name = "insert",
            path = "splitMusicContainer",
            httpMethod = ApiMethod.HttpMethod.POST)
    public SplitMusicContainer insert(SplitMusicContainer splitMusicContainer) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that splitMusicContainer.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(splitMusicContainer).now();
        logger.info("Created SplitMusicContainer with ID: " + splitMusicContainer.getId());

        return ofy().load().entity(splitMusicContainer).now();
    }

    /**
     * Updates an existing {@code SplitMusicContainer}.
     *
     * @param id                  the ID of the entity to be updated
     * @param splitMusicContainer the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code SplitMusicContainer}
     */
    @ApiMethod(
            name = "update",
            path = "splitMusicContainer/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public SplitMusicContainer update(@Named("id") Long id, SplitMusicContainer splitMusicContainer) throws NotFoundException {
        // TODO: You should validate your ID parameter against your resource's ID here.
        checkExists(id);
        ofy().save().entity(splitMusicContainer).now();
        logger.info("Updated SplitMusicContainer: " + splitMusicContainer);
        return ofy().load().entity(splitMusicContainer).now();
    }

    /**
     * Deletes the specified {@code SplitMusicContainer}.
     *
     * @param id the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code SplitMusicContainer}
     */
    @ApiMethod(
            name = "remove",
            path = "splitMusicContainer/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(SplitMusicContainer.class).id(id).now();
        logger.info("Deleted SplitMusicContainer with ID: " + id);
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
            path = "splitMusicContainer",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<SplitMusicContainer> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<SplitMusicContainer> query = ofy().load().type(SplitMusicContainer.class).limit(limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<SplitMusicContainer> queryIterator = query.iterator();
        List<SplitMusicContainer> splitMusicContainerList = new ArrayList<SplitMusicContainer>(limit);
        while (queryIterator.hasNext()) {
            splitMusicContainerList.add(queryIterator.next());
        }
        return CollectionResponse.<SplitMusicContainer>builder().setItems(splitMusicContainerList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(SplitMusicContainer.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find SplitMusicContainer with ID: " + id);
        }
    }
}