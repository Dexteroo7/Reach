package reach.backend.Servlets;

import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import reach.backend.TextUtils;
import reach.backend.User.ReachUser;
import reach.backend.imageServer.ServingUrl;

import static reach.backend.OfyService.ofy;

//TODO send valid error codes and status

/**
 * Created by dexter on 12/01/16.
 */
public class UserImageEndpoint extends HttpServlet {

    private static final Logger logger = Logger.getLogger(UserImageEndpoint.class.getName());
    private static final String BUCKET_NAME_IMAGE = "able-door-616-images";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        final String hostIdString = req.getParameter("hostIdString");
        final String requestedImage = req.getParameter("requestedImage");

        final String requestedFormat = req.getParameter("requestedFormat");
        final String requestCircularCrop = req.getParameter("requestCircularCrop");
        final String requestedWidthString = req.getParameter("requestedWidth");
        final String requestedHeightString = req.getParameter("requestedHeight");

        ///////////////////////////////Sanity checks

        if (TextUtils.isEmpty(hostIdString) || TextUtils.isEmpty(requestedImage) ||
                TextUtils.isEmpty(requestedWidthString) || TextUtils.isEmpty(requestedHeightString) ||
                TextUtils.isEmpty(requestedFormat) || TextUtils.isEmpty(requestCircularCrop)) {

            resp.sendError(400, "Some parameters are missing");
            return;
        }

        ///////////////////////////////Verify requestedFormat

        if (requestedFormat.equals("rj") || requestedFormat.equals("rp") || requestedFormat.equals("rw"))
            logger.info("Requested format " + requestedFormat);
        else {

            resp.sendError(400, "Illegal requested format " + requestedFormat);
            return;
        }

        ///////////////////////////////Verify other stuff and get the actualImageId

        final long hostId = Long.parseLong(hostIdString);
        final int requestedWidth = Integer.parseInt(requestedWidthString);
        final int requestedHeight = Integer.parseInt(requestedHeightString);
        final boolean circular = Boolean.parseBoolean(requestCircularCrop);

        //validate longs
        if (hostId > 0 && requestedHeight > 0 && requestedWidth > 0)
            logger.info("Requesting from " + hostIdString + " width= " + requestedWidthString + " height= " + requestedHeightString);
        else {

            resp.sendError(400, "requested values must be > 0");
            return;
        }

        final ReachUser reachUser = ofy().load().type(ReachUser.class).id(hostId).now();

        if (reachUser == null) {

            resp.sendError(400, "User not found");
            return;
        }

        final String actualImageId;
        switch (requestedImage) {
            case "imageId":
                actualImageId = reachUser.getImageId();
                break;
            case "coverPicId":
                actualImageId = reachUser.getCoverPicId();
                break;
            default:
                resp.sendError(400, "Only imageId and coverPicId are supported as a requestedImage param");
                return;
        }

        ///////////////////////////////Generate the serving url

        final String servingURL;
        final ServingUrl servingUrlRetriever = ofy().load().type(ServingUrl.class).id(actualImageId).now();
        if (servingUrlRetriever == null || TextUtils.isEmpty(servingUrlRetriever.getServingUrl())) {

            //generate new servingURL, copies the image first
            try {
                servingURL = getServingURL(BUCKET_NAME_IMAGE, actualImageId);
            } catch (IOException | IllegalArgumentException ignored) {

                resp.sendError(500, "Serving url could not be generated");
                return;
            }
            final ServingUrl toSave = new ServingUrl();
            toSave.setId(actualImageId);
            toSave.setServingUrl(servingURL);
            ofy().save().entity(toSave); //save async
        } else
            servingURL = servingUrlRetriever.getServingUrl();

        ///////////////////////////////Process

        final String toReturn = servingURL + //base url
                "=w" + requestedWidth + //set width
                "-h" + requestedHeight + //set height
                "-nu" + //no-upscaling, disables resizing an image to larger than its original resolution
                (circular ? "-cc" : "-s") + //generates a circularly cropped image or stretch image to dimensions
                "-" + requestedFormat; //force requestedFormat

        logger.info("Final url - " + toReturn);

        resp.sendRedirect(toReturn);
    }

    /**
     * Copies the image and gets a serving url over it
     *
     * @param bucketName the bucket where the image is hosted
     * @param imageId    the id of the image to server
     * @return the serving url
     * @throws IOException
     */
    private String getServingURL(String bucketName, String imageId) throws IOException {

        final GcsFilename gcsFilename = new GcsFilename(bucketName, imageId);
        final GcsService gcsService = GcsServiceFactory.createGcsService();

        final ReadableByteChannel readableByteChannel = gcsService.openPrefetchingReadChannel(
                gcsFilename, 0, 4096);
        final GcsOutputChannel gcsOutputChannel = gcsService.createOrReplace(
                gcsFilename, new GcsFileOptions.Builder()
                        .acl("public-read")
                        .contentEncoding("image/webp")
                        .mimeType("image/webp").build());

        //copy the image, includes a second wait
        copy(readableByteChannel, gcsOutputChannel);
        //wait more
        gcsOutputChannel.waitForOutstandingWrites();
        //close
        gcsOutputChannel.close();

        final ServingUrlOptions servingUrlOptions =
                ServingUrlOptions.Builder.withGoogleStorageFileName("/gs/" + BUCKET_NAME_IMAGE + "/" + imageId).secureUrl(true);
        return ImagesServiceFactory.getImagesService().getServingUrl(servingUrlOptions);
    }

    /**
     * Copies all bytes from the readable channel to the writable channel.
     * Does not close or flush either channel.
     *
     * @param from the readable channel to read from
     * @param to   the writable channel to write to
     */
    private void copy(ReadableByteChannel from,
                      WritableByteChannel to) {

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4096);
        boolean fail = false;

        while (!fail) {

            try {
                fail = (from.read(byteBuffer) == -1);
            } catch (IOException ignored) {
                fail = true;
            }

            byteBuffer.flip();
            while (byteBuffer.hasRemaining())
                try {
                    to.write(byteBuffer);
                } catch (IOException ignored) {
                    fail = true;
                }
            byteBuffer.clear();
        }
    }
}