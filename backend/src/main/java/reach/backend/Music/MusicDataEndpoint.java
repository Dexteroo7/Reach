package reach.backend.Music;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

import java.util.HashMap;
import java.util.logging.Logger;

import javax.inject.Named;

import reach.backend.ObjectWrappers.MyString;
import reach.backend.User.ReachUser;

import static reach.backend.OfyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p/>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "musicVisibilityApi",
        version = "v1",
        resource = "musicVisibility",
        namespace = @ApiNamespace(
                ownerDomain = "Music.backend.reach",
                ownerName = "Music.backend.reach",
                packagePath = ""
        )
)
public class MusicDataEndpoint {

    private static final Logger logger = Logger.getLogger(MusicDataEndpoint.class.getName());

    /**
     * Returns the {@link MusicData} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     */
    @ApiMethod(
            name = "get",
            path = "musicVisibility/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MusicData get(@Named("id") long id) {
        logger.info("Getting MusicData with ID: " + id);
        return ofy().load().type(MusicData.class).id(id).now();
    }

    /**
     * Inserts a new {@code MusicData}.
     */
    @ApiMethod(
            name = "insert",
            path = "musicData",
            httpMethod = ApiMethod.HttpMethod.POST)
    public void insert(MusicData musicData, @Named("visibleSongs") int visibleSongs) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that musicData.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        //update number of "VISIBLE SONGS"
        final ReachUser user = ofy().load().type(ReachUser.class).id(musicData.getId()).now();
        user.setNumberOfSongs(visibleSongs);
        ofy().save().entities(musicData, user).now();
        logger.info("Created MusicData with ID: " + musicData.getId());
    }

    /**
     * Updates an existing {@code MusicData}.
     *
     * @param id         the ID of the entity to be updated
     * @param musicId    the ID of the Music object
     * @param visibility the desired state of the entity
     * @return false : re-run musicScanner, true : OK
     */
    @ApiMethod(
            name = "update",
            path = "musicVisibility/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public MyString update(@Named("id") long id,
                           @Named("musicId") long musicId,
                           @Named("visibility") boolean visibility) {

        MusicData musicData = ofy().load().type(MusicData.class).id(id).now();
        if (musicData == null || musicData.getVisibility() == null) {

            musicData = new MusicData();
            musicData.setId(id);
            musicData.setVisibility(new HashMap<Long, Boolean>(500));
        }

        musicData.getVisibility().put(musicId, visibility);
        ofy().save().entity(musicData).now();
        logger.info("Updated MusicData: " + musicData);
        return new MyString("true");
    }
}