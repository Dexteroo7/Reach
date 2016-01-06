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
 * <p>
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
     * Returns the {@link MusicData} with the corresponding ID
     * if given hash has changed
     *
     * @param id                 the ID of the entity to be retrieved
     * @param clientSideHashCode the hashCode according to client
     * @return the entity with the corresponding ID, if response is empty ignore
     */
    @ApiMethod(
            name = "getIfChanged",
            path = "musicVisibility/getIfChanged/{id}/{clientSideHashCode}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public VisibilityChangeResponse getIfChanged(@Named("id") long id,
                                                 @Named("clientSideHashCode") int clientSideHashCode) {

        final MusicData musicData = ofy().load().type(MusicData.class).id(id).now();

        if (musicData == null || musicData.getVisibility() == null || musicData.getVisibility().isEmpty())
            return new VisibilityChangeResponse(); //empty response, ignore

        final int hashCode = musicData.getVisibility().hashCode();

        if (hashCode != clientSideHashCode)
            return new VisibilityChangeResponse(musicData.getVisibility(), hashCode);
        else
            return new VisibilityChangeResponse(); //empty response, ignore
    }

    /**
     * Inserts a new {@code MusicData}.
     * Overwrites new values into old, preserving
     * old values if not found in new container
     */
    @ApiMethod(
            name = "insert",
            path = "musicData",
            httpMethod = ApiMethod.HttpMethod.POST)
    public void insert(MusicData musicData,
                       @Named("visibleSongs") int visibleSongs) {

        final long id = musicData.getId();
        if (id == 0)
            throw new IllegalArgumentException("Please specific the item id");

        MusicData oldData = ofy().load().type(MusicData.class).id(id).now();
        if (oldData == null || oldData.getVisibility() == null || oldData.getId() == 0)
            //no previous data found
            oldData = musicData;
        else
            //old data found, overwrite new values
            oldData.getVisibility().putAll(musicData.getVisibility());

        visibleSongs = 0;
        for (Boolean aBoolean : oldData.getVisibility().values())
            if (aBoolean != null && aBoolean)
                visibleSongs++;

        final ReachUser user = ofy().load().type(ReachUser.class).id(id).now();
        user.setNumberOfSongs(visibleSongs);
        ofy().save().entities(oldData, user).now();

        logger.info("Created MusicVisibility with ID: " + id);
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

        if (id == 0)
            throw new IllegalArgumentException("Please give id");

        MusicData musicData = ofy().load().type(MusicData.class).id(id).now();
        if (musicData == null || musicData.getVisibility() == null) {

            musicData = new MusicData();
            musicData.setId(id);
            musicData.setVisibility(new HashMap<Long, Boolean>(500));
        }
        musicData.getVisibility().put(musicId, visibility);

        //update new visible songs count
        final ReachUser user = ofy().load().type(ReachUser.class).id(id).now();
        final int oldNumberOfSongs = user.getNumberOfSongs();
        if (visibility)
            user.setNumberOfSongs(oldNumberOfSongs + 1); //if visible increment
        else
            user.setNumberOfSongs(oldNumberOfSongs - 1); // else decrement

        ofy().save().entities(musicData, user).now();
        logger.info("Updated MusicData: " + id + " " + visibility);
        return new MyString("true");
    }
}