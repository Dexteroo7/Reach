package reach.backend.Servlets;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.googlecode.objectify.LoadResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import reach.backend.User.ReachUser;

import static reach.backend.OfyService.ofy;

public class RefreshGCM extends HttpServlet {

    private static final Logger logger = Logger.getLogger(RefreshGCM.class.getName());
    private static final String API_KEY = System.getProperty("gcm.api.key");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        final List<ReachUser> users = new ArrayList<>(1000);
        final List<String> regIds = new ArrayList<>(1000);

        final Sender sender = new Sender(API_KEY);

        int totalSize = 0;
        boolean result = true;

        for (ReachUser reachUser : ofy().load().type(ReachUser.class)
                .filter("gcmId !=", "")
                .project("gcmId")) {

            users.add(reachUser);
            regIds.add(reachUser.getGcmId());
            totalSize++;

            //sendMultiCast doesn't take more than 1000 at a time
            if (totalSize > 999) {

                if (regIds.size() > 0 && users.size() > 0 && regIds.size() == users.size()) {
                    result = result && sendMultiCastMessage("hello_world", users, regIds, sender);
                    if (result)
                        logger.info("refreshed gcm of " + totalSize);
                    else
                        logger.log(Level.SEVERE, "multi-cast failed");
                } else
                    logger.info("size conflict");

                try {
                    //wait a little, let multi-cast shoot out
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                } finally {
                    totalSize = 0;
                    users.clear();
                    regIds.clear();
                }
            }
        }

        if (regIds.size() > 0 && users.size() > 0 && regIds.size() == users.size()) {
            result = result && sendMultiCastMessage("hello_world", users, regIds, sender);
            if (result)
                logger.info("refreshed gcm of " + totalSize);
            else
                logger.log(Level.SEVERE, "multi-cast failed");
        }

        logger.info("finished");
    }

    private boolean sendMultiCastMessage(@Nonnull String message,
                                         @Nonnull List<ReachUser> users,
                                         @Nonnull List<String> regIds,
                                         @Nonnull Sender sender) {

        if (users.size() > 1000 || regIds.size() > 1000)
            return false;

        //ensure that the User is not null and message is not shit, beforehand
        final MulticastResult multicastResult;

        try {
            multicastResult = sender.send(new Message.Builder()
                    .addData("message", message)
                    .build(), regIds, 5);
        } catch (IOException | IllegalArgumentException e) {

            e.printStackTrace();
            logger.log(Level.SEVERE, e.getLocalizedMessage() + " error");
            return false;
        }

        final List<Result> results = multicastResult.getResults();
        final int resultLength = results.size();

        if (resultLength != users.size()) {
            logger.log(Level.SEVERE, "Multi-part messages size different error");
            return false;
        }

        final List<LoadResult<ReachUser>> toChange = new ArrayList<>(100);
        final List<String> toPut = new ArrayList<>(100);

        for (int index = 0; index < resultLength; index++) {

            final Result result = results.get(index);

            if (!isEmpty(result.getMessageId())) {

                final String canonicalRegistrationId = result.getCanonicalRegistrationId();
                if (!isEmpty(canonicalRegistrationId)) {
                    // if the regId changed, we have to update the data-store
                    logger.info("Registration Id changed for " + users.get(index).getId() + " updating to " + canonicalRegistrationId);

                    final LoadResult<ReachUser> loadResult = ofy().load().type(ReachUser.class).id(users.get(index).getId());
                    toChange.add(loadResult);
                    toPut.add(canonicalRegistrationId);
                }

            } else {

                if (result.getErrorCodeName().equals(Constants.ERROR_NOT_REGISTERED) ||
                        result.getErrorCodeName().equals(Constants.ERROR_INVALID_REGISTRATION) ||
                        result.getErrorCodeName().equals(Constants.ERROR_MISMATCH_SENDER_ID) ||
                        result.getErrorCodeName().equals(Constants.ERROR_MISSING_REGISTRATION)) {

                    logger.info("Registration Id " + users.get(index).getUserName() + " no longer registered with GCM, removing from data-store");
                    // if the device is no longer registered with Gcm, remove it from the data-store
                    final LoadResult<ReachUser> loadResult = ofy().load().type(ReachUser.class).id(users.get(index).getId());
                    toChange.add(loadResult);
                    toPut.add(""); //empty string
                } else {
                    logger.info("Error when sending message : " + result.getErrorCodeName());
                }
            }
        }

        final int changeListSize = toChange.size();

        if (changeListSize == 0)
            return true;

        final List<ReachUser> toBatch = new ArrayList<>(changeListSize);

        for (int index = 0; index < changeListSize; index++) {

            final ReachUser reachUser = toChange.get(index).now();
            reachUser.setGcmId(toPut.get(index));
            toBatch.add(reachUser);
        }

        ofy().save().entities(toBatch);
        return true;
    }


    /**
     * Returns true if the string is null or 0-length.
     *
     * @param str the string to be examined
     * @return true if str is null or zero length
     */
    public static boolean isEmpty(@Nullable CharSequence str) {
        if (str == null || str.length() == 0)
            return true;
        else
            return false;
    }
}
