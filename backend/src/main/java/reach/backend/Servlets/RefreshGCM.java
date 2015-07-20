package reach.backend.Servlets;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
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
                    result = result && sendMultiCastMessage("hello_world", users, regIds);
                    if (result)
                        logger.info("refreshed gcm of " + totalSize);
                    else
                        logger.log(Level.SEVERE, "multi-cast failed");
                } else
                    logger.info("size conflict");

                try {
                    //wait a little
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
            result = result && sendMultiCastMessage("hello_world", users, regIds);
            if (result)
                logger.info("refreshed gcm of " + totalSize);
            else
                logger.log(Level.SEVERE, "multi-cast failed");
        }

        logger.info("finished");
    }

    private boolean sendMultiCastMessage(@Nonnull String message,
                                         @Nonnull List<ReachUser> users,
                                         @Nonnull List<String> regIds) {


        if (users.size() > 1000 || regIds.size() > 1000)
            return false;

        //ensure that the user is not null and message is not shit, beforehand
        final MulticastResult multicastResult;

        try {
            multicastResult = new Sender(API_KEY).send(new Message.Builder().addData("message", message).build(), regIds, 5);
        } catch (IOException | IllegalArgumentException e) {

            e.printStackTrace();
            logger.log(Level.SEVERE, e.getLocalizedMessage() + " error");
            return false;
        }

        if (multicastResult.getResults().size() != users.size()) {
            logger.log(Level.SEVERE, "Multi-part messages size different error");
            return false;
        }

        int index = 0;
        for (Result result : multicastResult.getResults()) {

            if (result.getMessageId() != null) {

                if (result.getCanonicalRegistrationId() != null && !result.getCanonicalRegistrationId().equals("")) {
                    // if the regId changed, we have to update the data-store
                    logger.info("Registration Id changed for " + users.get(index).getId() + " updating to " + result.getCanonicalRegistrationId());
                    final ReachUser completeUser = ofy().load().type(ReachUser.class).id(users.get(index).getId()).now();
                    completeUser.setGcmId(result.getCanonicalRegistrationId());
                    ofy().save().entities(completeUser).now();
                }
            } else {

                if (result.getErrorCodeName().equals(Constants.ERROR_NOT_REGISTERED) ||
                        result.getErrorCodeName().equals(Constants.ERROR_INVALID_REGISTRATION) ||
                        result.getErrorCodeName().equals(Constants.ERROR_MISMATCH_SENDER_ID) ||
                        result.getErrorCodeName().equals(Constants.ERROR_MISSING_REGISTRATION)) {

                    logger.info("Registration Id " + users.get(index).getUserName() + " no longer registered with GCM, removing from data-store");
                    // if the device is no longer registered with Gcm, remove it from the data-store
                    final ReachUser completeUser = ofy().load().type(ReachUser.class).id(users.get(index).getId()).now();
                    completeUser.setGcmId("");
                    ofy().save().entities(completeUser).now();
                } else {
                    logger.info("Error when sending message : " + result.getErrorCodeName());
                }
            }
            index++;
        }
        return true;
    }
}
