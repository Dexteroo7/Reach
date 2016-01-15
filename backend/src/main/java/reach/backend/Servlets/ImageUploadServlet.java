package reach.backend.Servlets;

import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ImagesServiceFailureException;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.tools.cloudstorage.GcsFileMetadata;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Date;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import reach.backend.MiscUtils;
import reach.backend.TextUtils;
import reach.backend.imageServer.ServingUrl;

import static reach.backend.OfyService.ofy;

/**
 * Created by dexter on 15/01/16.
 */
public class ImageUploadServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(ImageUploadServlet.class.getName());
    private static final String BUCKET_NAME_IMAGE = "able-door-616-images";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        final String hostIdString = req.getParameter("hostIdString");
        final String imageHash = req.getParameter("imageHash");

        //sanity check
        if (TextUtils.isEmpty(hostIdString) || TextUtils.isEmpty(imageHash)) {

            resp.sendError(400, "Some parameters are missing");
            return;
        }

        final long hostId = Long.parseLong(hostIdString);

        logger.info("Saving image for " + hostIdString + " " + imageHash);

        final ServingUrl servingUrlRetriever = ofy().load().type(ServingUrl.class).id(imageHash).now();
        final GcsFilename gcsFilename = new GcsFilename(BUCKET_NAME_IMAGE, imageHash);
        final GcsService gcsService = GcsServiceFactory.createGcsService();
        final GcsFileMetadata gcsFileMetadata = gcsService.getMetadata(gcsFilename);

        //check if file and serving url exist !
        if (servingUrlRetriever == null || TextUtils.isEmpty(servingUrlRetriever.getServingUrl()) ||
                gcsFileMetadata == null || gcsFileMetadata.getLength() == 0) {

            //Generate new servingURL, copies the image first
            final String servingURL;

            try {
                servingURL = getServingURL(gcsFilename, gcsService, req.getInputStream());
            } catch (IOException | IllegalArgumentException | ImagesServiceFailureException ignored) {

                resp.sendError(500, "Serving url could not be generated");
                return;
            }

            final ServingUrl toSave = new ServingUrl();
            toSave.setUserId(hostId);
            toSave.setId(imageHash);
            toSave.setServingUrl(servingURL);
            toSave.setDateOfCreation(new Date(System.currentTimeMillis()));
            ofy().save().entity(toSave); //save async
            resp.setStatus(HttpServletResponse.SC_CREATED);
            logger.info("Serving url and image created " + servingURL);

        } else {
            //The serving url AND file already exist
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            logger.info("Serving url and image already exists");
        }
    }

    /**
     * Saves the image and returns the serving url to save
     *
     * @param gcsFilename the file to save the image into
     * @param inputStream the image stream
     * @return the serving url string
     * @throws IOException
     */
    @Nonnull
    private String getServingURL(GcsFilename gcsFilename, GcsService gcsService, InputStream inputStream) throws IOException {


        final GcsOutputChannel gcsOutputChannel = gcsService.createOrReplace(
                gcsFilename, new GcsFileOptions.Builder()
                        .acl("public-read")
                        .contentEncoding("image/webp")
                        .mimeType("image/webp").build());
        final ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);

        //copy and close
        MiscUtils.copy(readableByteChannel, gcsOutputChannel);
        gcsOutputChannel.waitForOutstandingWrites();
        MiscUtils.closeQuietly(gcsOutputChannel, readableByteChannel, inputStream);

        //get the serving url
        final ServingUrlOptions servingUrlOptions =
                ServingUrlOptions.Builder.withGoogleStorageFileName("/gs/" + gcsFilename.getBucketName() + "/" + gcsFilename.getObjectName()).secureUrl(true);
        return ImagesServiceFactory.getImagesService().getServingUrl(servingUrlOptions);
    }
}
