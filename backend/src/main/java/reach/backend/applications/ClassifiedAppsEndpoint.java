package reach.backend.applications;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;

import reach.backend.ObjectWrappers.StringList;
import reach.backend.TextUtils;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Api(
        name = "classifiedAppsApi",
        version = "v1",
        resource = "classifiedApps",
        namespace = @ApiNamespace(
                ownerDomain = "applications.backend.reach",
                ownerName = "applications.backend.reach",
                packagePath = ""
        )
)
public class ClassifiedAppsEndpoint {

    private static final Logger logger = Logger.getLogger(ClassifiedAppsEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 500;

    /**
     * @param packageNames list of package names to verify
     * @return list of hidden package names
     */
    @ApiMethod(
            name = "getDefaultState",
            path = "classifiedApps/getDefaultState/",
            httpMethod = ApiMethod.HttpMethod.POST)
    public StringList getDefaultState(StringList packageNames) {

        //sanity check
        final List<String> toProcess;
        if (packageNames == null ||
                (toProcess = packageNames.getStringList()) == null ||
                toProcess.isEmpty())
            return null;

        final List<Key> filterKeys = new ArrayList<>();
        for (String packageName : toProcess)
            filterKeys.add(Key.create(ClassifiedApps.class, packageName));

        //fetch from classified apps list
        final List<String> defaultHiddenPackages = new ArrayList<>();
        for (ClassifiedApps classifiedApp : ofy().load().type(ClassifiedApps.class)
                .filterKey("in", filterKeys)) {

            if (!classifiedApp.isVisible()) //add as hidden
                defaultHiddenPackages.add(classifiedApp.getPackageName());

            //mark as processed by removing
            toProcess.remove(classifiedApp.getPackageName());
        }

        //TODO fetch application names and description

        filterKeys.clear();
        for (String packageName : toProcess)
            filterKeys.add(Key.create(UnClassifiedApps.class, packageName));

        //remove packages that have already been added to queue un-classified
        for (UnClassifiedApps unClassifiedApp : ofy().load().type(UnClassifiedApps.class)
                .filterKey("in", filterKeys))
            toProcess.remove(unClassifiedApp.getPackageName());

        //add unclassified packages to list
        final List<UnClassifiedApps> toSave = new ArrayList<>();
        for (String packageName : toProcess) {

            final UnClassifiedApps unClassifiedApp = new UnClassifiedApps();
            unClassifiedApp.setPackageName(packageName);
            unClassifiedApp.setApplicationName("Not set yet");
            unClassifiedApp.setDescription("Not set yet");
            toSave.add(unClassifiedApp);
        }

        if (!toSave.isEmpty())
            ofy().save().entities(toSave);

        //reuse list
        packageNames.setStringList(defaultHiddenPackages);
        return packageNames;
    }

    /**
     * Returns the {@link ClassifiedApps} with the corresponding ID.
     *
     * @param packageName the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code ClassifiedApps} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "classifiedApps/{packageName}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public ClassifiedApps get(@Named("packageName") String packageName) throws NotFoundException {

        logger.info("Getting ClassifiedApps with ID: " + packageName);
        ClassifiedApps classifiedApps = ofy().load().type(ClassifiedApps.class).id(packageName).now();
        if (classifiedApps == null)
            throw new NotFoundException("Could not find ClassifiedApps with ID: " + packageName);

        return classifiedApps;
    }

    /**
     * Inserts a new {@code ClassifiedApps}.
     */
    @ApiMethod(
            name = "insert",
            path = "classifiedApps",
            httpMethod = ApiMethod.HttpMethod.POST)
    public ClassifiedApps insert(ClassifiedApps classifiedApps) {

        if (TextUtils.isEmpty(classifiedApps.getPackageName()))
            throw new IllegalArgumentException("Please specific package name");

        ofy().save().entity(classifiedApps).now();
        logger.info("Created ClassifiedApps with ID: " + classifiedApps.getPackageName());

        return ofy().load().entity(classifiedApps).now();
    }

    /**
     * Updates an existing {@code ClassifiedApps}.
     *
     * @param packageName    the ID of the entity to be updated
     * @param classifiedApps the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code packageName} does not correspond to an existing
     *                           {@code ClassifiedApps}
     */
    @ApiMethod(
            name = "update",
            path = "classifiedApps/{packageName}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public ClassifiedApps update(@Named("packageName") String packageName,
                                 ClassifiedApps classifiedApps) throws NotFoundException {

        if (TextUtils.isEmpty(classifiedApps.getPackageName()))
            throw new IllegalArgumentException("Please specific package name");

        checkExists(packageName);
        ofy().save().entity(classifiedApps).now();
        logger.info("Updated ClassifiedApps: " + classifiedApps);
        return ofy().load().entity(classifiedApps).now();
    }

    /**
     * Deletes the specified {@code ClassifiedApps}.
     *
     * @param packageName the ID of the entity to delete
     * @throws NotFoundException if the {@code packageName} does not correspond to an existing
     *                           {@code ClassifiedApps}
     */
    @ApiMethod(
            name = "remove",
            path = "classifiedApps/{packageName}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("packageName") String packageName) throws NotFoundException {

        checkExists(packageName);
        ofy().delete().type(ClassifiedApps.class).id(packageName).now();
        logger.info("Deleted ClassifiedApps with ID: " + packageName);
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
            path = "classifiedApps",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<ClassifiedApps> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {

        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<ClassifiedApps> query = ofy().load().type(ClassifiedApps.class).limit(limit);
        if (cursor != null)
            query = query.startAt(Cursor.fromWebSafeString(cursor));

        QueryResultIterator<ClassifiedApps> queryIterator = query.iterator();
        List<ClassifiedApps> classifiedAppsList = new ArrayList<>(limit);
        while (queryIterator.hasNext())
            classifiedAppsList.add(queryIterator.next());

        return CollectionResponse.<ClassifiedApps>builder().setItems(classifiedAppsList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(String packageName) throws NotFoundException {

        try {
            ofy().load().type(ClassifiedApps.class).id(packageName).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find ClassifiedApps with ID: " + packageName);
        }
    }
}