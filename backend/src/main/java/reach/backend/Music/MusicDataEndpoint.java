package reach.backend.music;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.NotFoundException;

import java.util.logging.Logger;

import javax.inject.Named;

import reach.backend.objectWrappers.MyString;

import static com.googlecode.objectify.ObjectifyService.ofy;

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
                ownerDomain = "music.backend.reach",
                ownerName = "music.backend.reach",
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
     * @throws NotFoundException if there is no {@code MusicData} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "musicVisibility/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public MusicData get(@Named("id") long id) throws NotFoundException {
        logger.info("Getting MusicData with ID: " + id);
        MusicData musicData = ofy().load().type(MusicData.class).id(id).now();
        if (musicData == null) {
            throw new NotFoundException("Could not find MusicData with ID: " + id);
        }
        return musicData;
    }

    /**
     * Inserts a new {@code MusicData}.
     */
    @ApiMethod(
            name = "insert",
            path = "musicData",
            httpMethod = ApiMethod.HttpMethod.POST)
    public MusicData insert(MusicData musicData) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that musicData.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(musicData).now();
        logger.info("Created MusicData with ID: " + musicData.getId());

        return ofy().load().entity(musicData).now();
    }

    /**
     * Updates an existing {@code MusicData}.
     *
     * @param id         the ID of the entity to be updated
     * @param musicId    the ID of the music object
     * @param visibility the desired state of the entity
     * @return false : re-run musicScanner, true : OK
     */
    @ApiMethod(
            name = "update",
            path = "musicVisibility/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public MyString update(@Named("id") long id,
                           @Named("musicId") int musicId,
                           @Named("visibility") boolean visibility) {

        MusicData musicData = ofy().load().type(MusicData.class).id(id).now();

        if (musicData == null || musicData.getVisibility() == null) {
            logger.info("visibility error " + id);
            return new MyString("false"); //not found, run scanner
        }

        musicData.getVisibility().put(musicId, visibility);
        ofy().save().entity(musicData).now();
        logger.info("Updated MusicData: " + musicData);
        return new MyString("true");
    }
}