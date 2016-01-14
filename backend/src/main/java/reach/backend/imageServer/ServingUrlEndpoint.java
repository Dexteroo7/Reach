package reach.backend.imageServer;

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
        name = "servingUrlApi",
        version = "v1",
        resource = "servingUrl",
        namespace = @ApiNamespace(
                ownerDomain = "imageServer.backend.reach",
                ownerName = "imageServer.backend.reach",
                packagePath = ""
        )
)
public class ServingUrlEndpoint {

    private static final Logger logger = Logger.getLogger(ServingUrlEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 20;

    static {
        // Typically you would register this inside an OfyServive wrapper. See: https://code.google.com/p/objectify-appengine/wiki/BestPractices
        ObjectifyService.register(ServingUrl.class);
    }

    /**
     * Returns the {@link ServingUrl} with the corresponding ID.
     *
     * @param imageId the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code ServingUrl} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "servingUrl/{imageId}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public ServingUrl get(@Named("imageId") String imageId) throws NotFoundException {
        logger.info("Getting ServingUrl with ID: " + imageId);
        ServingUrl servingUrl = ofy().load().type(ServingUrl.class).id(imageId).now();
        if (servingUrl == null) {
            throw new NotFoundException("Could not find ServingUrl with ID: " + imageId);
        }
        return servingUrl;
    }

    /**
     * Inserts a new {@code ServingUrl}.
     */
    @ApiMethod(
            name = "insert",
            path = "servingUrl",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ServingUrl insert(ServingUrl servingUrl) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that servingUrl.imageId has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(servingUrl).now();
        logger.info("Created ServingUrl.");

        return ofy().load().entity(servingUrl).now();
    }

    /**
     * Updates an existing {@code ServingUrl}.
     *
     * @param imageId    the ID of the entity to be updated
     * @param servingUrl the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code imageId} does not correspond to an existing
     *                           {@code ServingUrl}
     */
    @ApiMethod(
            name = "update",
            path = "servingUrl/{imageId}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public ServingUrl update(@Named("imageId") String imageId, ServingUrl servingUrl) throws NotFoundException {
        // TODO: You should validate your ID parameter against your resource's ID here.
        checkExists(imageId);
        ofy().save().entity(servingUrl).now();
        logger.info("Updated ServingUrl: " + servingUrl);
        return ofy().load().entity(servingUrl).now();
    }

    /**
     * Deletes the specified {@code ServingUrl}.
     *
     * @param imageId the ID of the entity to delete
     * @throws NotFoundException if the {@code imageId} does not correspond to an existing
     *                           {@code ServingUrl}
     */
    @ApiMethod(
            name = "remove",
            path = "servingUrl/{imageId}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("imageId") String imageId) throws NotFoundException {
        checkExists(imageId);
        ofy().delete().type(ServingUrl.class).id(imageId).now();
        logger.info("Deleted ServingUrl with ID: " + imageId);
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
            path = "servingUrl",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<ServingUrl> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<ServingUrl> query = ofy().load().type(ServingUrl.class).limit(limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<ServingUrl> queryIterator = query.iterator();
        List<ServingUrl> servingUrlList = new ArrayList<ServingUrl>(limit);
        while (queryIterator.hasNext()) {
            servingUrlList.add(queryIterator.next());
        }
        return CollectionResponse.<ServingUrl>builder().setItems(servingUrlList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(String imageId) throws NotFoundException {
        try {
            ofy().load().type(ServingUrl.class).id(imageId).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find ServingUrl with ID: " + imageId);
        }
    }
}