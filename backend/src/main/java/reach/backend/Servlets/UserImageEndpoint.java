package reach.backend.Servlets;

import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import reach.backend.TextUtils;
import reach.backend.User.ReachUser;

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

        super.doGet(req, resp);

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
            throw new IllegalArgumentException("Some parameters are missing");
        }

        ///////////////////////////////Verify requestedFormat

        if (requestedFormat.equals("rj") || requestedFormat.equals("rp") || requestedFormat.equals("rw"))
            logger.info("Requested format " + requestedFormat);
        else
            throw new IllegalArgumentException("Illegal requested format " + requestedFormat);

        ///////////////////////////////Verify other stuff and get the actualImageId

        final long hostId = Long.parseLong(hostIdString);
        final int requestedWidth = Integer.parseInt(requestedWidthString);
        final int requestedHeight = Integer.parseInt(requestedHeightString);
        final boolean circular = Boolean.parseBoolean(requestCircularCrop);

        //validate longs
        if (hostId > 0 && requestedHeight > 0 && requestedWidth > 0)
            logger.info("Requesting from " + hostIdString + " width= " + requestedWidthString + " height= " + requestedHeightString);
        else
            throw new IllegalArgumentException("requested values must be > 0");

        final ReachUser reachUser = ofy().load().type(ReachUser.class).id(hostId).now();

        if (reachUser == null)
            throw new NotFoundException(Key.create(ReachUser.class, hostId));

        final String actualImageId;
        switch (requestedImage) {
            case "imageId":
                actualImageId = reachUser.getImageId();
                break;
            case "coverPicId":
                actualImageId = reachUser.getCoverPicId();
                break;
            default:
                throw new IllegalArgumentException("Only imageId and coverPicId are supported as a requestedImage param");
        }
        logger.info("Requesting " + requestedImage + " = " + actualImageId);

        ///////////////////////////////Generate the serving url

        final String cacheKey = getCacheKey(actualImageId, hostId);

        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));

        final String servingURL;
        final Object object = syncCache.get(cacheKey);
        if (object instanceof String && !TextUtils.isEmpty((String) object)) {

            //use this url only
            servingURL = (String) object;
        } else {

            final ServingUrlOptions servingUrlOptions = ServingUrlOptions.Builder.withGoogleStorageFileName("/gs/" + BUCKET_NAME_IMAGE + "/" + actualImageId).secureUrl(true);
            servingURL = ImagesServiceFactory.getImagesService().getServingUrl(servingUrlOptions);
            syncCache.put(cacheKey, servingURL);
        }

        ///////////////////////////////Process

        logger.info("Base url - " + servingURL);

        final String toReturn = servingURL + //base url
                "=w" + requestedWidth + //set width
                "-h" + requestedHeight + //set height
                "-nu" + //no-upscaling, disables resizing an image to larger than its original resolution
                (circular ? "-cc" : "") + //generates a circularly cropped image
                "-" + requestedFormat; //force requestedFormat

        logger.info("Final url - " + toReturn);

        resp.sendRedirect(toReturn);

    }

    private String getCacheKey(String actualImageId,
                               long hostId) {

        return hostId + "_" + actualImageId;
    }
}
