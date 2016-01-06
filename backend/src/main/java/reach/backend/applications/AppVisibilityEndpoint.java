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
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;

import reach.backend.ObjectWrappers.MyString;
import reach.backend.TextUtils;
import reach.backend.User.ReachUser;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "appVisibilityApi",
        version = "v1",
        resource = "appVisibility",
        namespace = @ApiNamespace(
                ownerDomain = "applications.backend.reach",
                ownerName = "applications.backend.reach",
                packagePath = ""
        )
)
public class AppVisibilityEndpoint {

    private static final Logger logger = Logger.getLogger(AppVisibilityEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 500;

    /**
     * Returns the {@link AppVisibility} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code AppVisibility} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "appVisibility/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public AppVisibility get(@Named("id") long id) throws NotFoundException {

        logger.info("Getting AppVisibility with ID: " + id);
        AppVisibility appVisibility = ofy().load().type(AppVisibility.class).id(id).now();
        if (appVisibility == null) {
            throw new NotFoundException("Could not find AppVisibility with ID: " + id);
        }
        return appVisibility;
    }

    /**
     * Returns the {@link AppVisibility} with the corresponding ID
     * if given hash has changed
     *
     * @param id                 the ID of the entity to be retrieved
     * @param clientSideHashCode the hashCode according to client
     * @return the entity with the corresponding ID, if response is empty ignore
     */
    @ApiMethod(
            name = "getIfChanged",
            path = "appVisibility/getIfChanged/{id}/{clientSideHashCode}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public VisibilityChangeResponse getIfChanged(@Named("id") long id,
                                                 @Named("clientSideHashCode") int clientSideHashCode) {

        final AppVisibility appVisibility = ofy().load().type(AppVisibility.class).id(id).now();

        if (appVisibility == null || appVisibility.getVisibility() == null || appVisibility.getVisibility().isEmpty())
            return new VisibilityChangeResponse(); //empty response, ignore

        final int hashCode = appVisibility.getVisibility().hashCode();

        if (hashCode != clientSideHashCode)
            return new VisibilityChangeResponse(appVisibility.getVisibility(), hashCode);
        else
            return new VisibilityChangeResponse(); //empty response, ignore
    }

    /**
     * Inserts a new {@code AppVisibility}.
     * Overwrites new values into old, preserving
     * old values if not found in new container
     */
    @ApiMethod(
            name = "insert",
            path = "appVisibility",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public AppVisibility insert(AppVisibility appVisibility) {

        final long id;
        if (appVisibility.getId() == null || (id = appVisibility.getId()) == 0)
            throw new IllegalArgumentException("Please specific the item id");

        AppVisibility oldData = ofy().load().type(AppVisibility.class).id(id).now();
        if (oldData == null || oldData.getVisibility() == null || oldData.getId() == null || oldData.getId() == 0)
            //no previous data found
            oldData = appVisibility;
        else
            //old data found, overwrite new values
            oldData.getVisibility().putAll(appVisibility.getVisibility());

        int visibleApps = 0;
        for (Boolean aBoolean : oldData.getVisibility().values())
            if (aBoolean != null && aBoolean)
                visibleApps++;

        final ReachUser user = ofy().load().type(ReachUser.class).id(id).now();
        user.setNumberOfApps(visibleApps);
        ofy().save().entities(oldData, user).now();

        logger.info("Created AppVisibility with ID: " + id);
        return ofy().load().entity(oldData).now();
    }

    /**
     * Updates an existing {@code appVisibility object}.
     *
     * @param id          the ID of the entity to be updated
     * @param packageName the ID of the app object
     * @param visibility  the desired state of the entity
     * @return false : re-run app scanner, true : OK
     */
    @ApiMethod(
            name = "update",
            path = "appVisibility/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public MyString update(@Named("id") long id,
                           @Named("packageName") String packageName,
                           @Named("visibility") boolean visibility) {

        if (id == 0 || TextUtils.isEmpty(packageName))
            throw new IllegalArgumentException("Insufficient parameters");

        //get visibility map
        AppVisibility appVisibility = ofy().load().type(AppVisibility.class).id(id).now();
        if (appVisibility == null || appVisibility.getVisibility() == null) {

            appVisibility = new AppVisibility();
            appVisibility.setId(id);
            appVisibility.setVisibility(new HashMap<String, Boolean>(500));
        }
        appVisibility.getVisibility().put(packageName, visibility);

        //update new visible apps count
        final ReachUser user = ofy().load().type(ReachUser.class).id(id).now();
        final int oldNumberOfApps = user.getNumberOfApps();
        if (visibility)
            user.setNumberOfApps(oldNumberOfApps + 1); //if visible increment
        else
            user.setNumberOfApps(oldNumberOfApps - 1); // else decrement

        //save latest info
        ofy().save().entities(appVisibility, user).now();
        logger.info("Updated appVisibility: " + id + " " + visibility + " " + packageName);
        return new MyString("true");
    }

    /**
     * Deletes the specified {@code AppVisibility}.
     *
     * @param id the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code AppVisibility}
     */
    @ApiMethod(
            name = "remove",
            path = "appVisibility/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(AppVisibility.class).id(id).now();
        logger.info("Deleted AppVisibility with ID: " + id);
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
            path = "appVisibility",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<AppVisibility> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<AppVisibility> query = ofy().load().type(AppVisibility.class).limit(limit);
        if (cursor != null)
            query = query.startAt(Cursor.fromWebSafeString(cursor));

        QueryResultIterator<AppVisibility> queryIterator = query.iterator();
        List<AppVisibility> appVisibilityList = new ArrayList<>(limit);
        while (queryIterator.hasNext())
            appVisibilityList.add(queryIterator.next());

        return CollectionResponse.<AppVisibility>builder().setItems(appVisibilityList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(long id) throws NotFoundException {
        try {
            ofy().load().type(AppVisibility.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find AppVisibility with ID: " + id);
        }
    }
}