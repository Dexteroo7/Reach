package reach.backend.applications;

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

import reach.backend.TextUtils;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Api(
        name = "unClassifiedAppsApi",
        version = "v1",
        resource = "unClassifiedApps",
        namespace = @ApiNamespace(
                ownerDomain = "applications.backend.reach",
                ownerName = "applications.backend.reach",
                packagePath = ""
        )
)
public class UnClassifiedAppsEndpoint {

    private static final Logger logger = Logger.getLogger(UnClassifiedAppsEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 500;

    /**
     * Returns the {@link UnClassifiedApps} with the corresponding ID.
     *
     * @param packageName the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code UnClassifiedApps} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "unClassifiedApps/{packageName}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public UnClassifiedApps get(@Named("packageName") String packageName) throws NotFoundException {

        logger.info("Getting UnClassifiedApps with ID: " + packageName);
        UnClassifiedApps unClassifiedApps = ofy().load().type(UnClassifiedApps.class).id(packageName).now();
        if (unClassifiedApps == null) {
            throw new NotFoundException("Could not find UnClassifiedApps with ID: " + packageName);
        }
        return unClassifiedApps;
    }

    /**
     * Inserts a new {@code UnClassifiedApps}.
     */
    @ApiMethod(
            name = "insert",
            path = "unClassifiedApps",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public UnClassifiedApps insert(UnClassifiedApps unClassifiedApps) {

        if (TextUtils.isEmpty(unClassifiedApps.getPackageName()))
            throw new IllegalArgumentException("Please specific package name");

        ofy().save().entity(unClassifiedApps).now();
        logger.info("Created UnClassifiedApps with ID: " + unClassifiedApps.getPackageName());

        return ofy().load().entity(unClassifiedApps).now();
    }

    /**
     * Updates an existing {@code UnClassifiedApps}.
     *
     * @param packageName      the ID of the entity to be updated
     * @param unClassifiedApps the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code packageName} does not correspond to an existing
     *                           {@code UnClassifiedApps}
     */
    @ApiMethod(
            name = "update",
            path = "unClassifiedApps/",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public UnClassifiedApps update(@Named("packageName") String packageName,
                                   UnClassifiedApps unClassifiedApps) throws NotFoundException {

        if (TextUtils.isEmpty(unClassifiedApps.getPackageName()))
            throw new IllegalArgumentException("Please specific package name");

        checkExists(packageName);
        ofy().save().entity(unClassifiedApps).now();
        logger.info("Updated UnClassifiedApps: " + unClassifiedApps);
        return ofy().load().entity(unClassifiedApps).now();
    }

    /**
     * Deletes the specified {@code UnClassifiedApps}.
     *
     * @param packageName the ID of the entity to delete
     * @throws NotFoundException if the {@code packageName} does not correspond to an existing
     *                           {@code UnClassifiedApps}
     */
    @ApiMethod(
            name = "remove",
            path = "unClassifiedApps/{packageName}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("packageName") String packageName) throws NotFoundException {

        checkExists(packageName);
        ofy().delete().type(UnClassifiedApps.class).id(packageName).now();
        logger.info("Deleted UnClassifiedApps with ID: " + packageName);
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
            path = "unClassifiedApps",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<UnClassifiedApps> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {

        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<UnClassifiedApps> query = ofy().load().type(UnClassifiedApps.class).limit(limit);
        if (cursor != null)
            query = query.startAt(Cursor.fromWebSafeString(cursor));

        QueryResultIterator<UnClassifiedApps> queryIterator = query.iterator();
        List<UnClassifiedApps> unClassifiedAppsList = new ArrayList<>(limit);
        while (queryIterator.hasNext())
            unClassifiedAppsList.add(queryIterator.next());

        return CollectionResponse.<UnClassifiedApps>builder().setItems(unClassifiedAppsList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(String packageName) throws NotFoundException {

        try {
            ofy().load().type(UnClassifiedApps.class).id(packageName).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find UnClassifiedApps with ID: " + packageName);
        }
    }
}