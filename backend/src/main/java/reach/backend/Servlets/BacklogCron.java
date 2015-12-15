package reach.backend.Servlets;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import reach.backend.campaign.BackLog;

import static reach.backend.OfyService.ofy;

/**
 * Created by dexter on 14/12/15.
 */
public class BacklogCron extends HttpServlet {

    private static final Logger logger = Logger.getLogger(BacklogCron.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        final int count = ofy().load().type(BackLog.class).count();
        if (count == 0)
            return; //no backlog found

        final List<Long> idsToRemove = new ArrayList<>(1000);
        for (BackLog backLog : ofy().load().type(BackLog.class).limit(1000)) {

            final String urlString = backLog.getFailedUrl();
            final long backLogId = backLog.getId();

            if (urlString.startsWith("http://54.169.227.37:8080/")) {
                idsToRemove.add(backLogId); //remove this back-log as invalid IP
                continue;
            }

            boolean success;
            HttpURLConnection connection = null;
            try {

                final URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(false);
                connection.setRequestMethod("GET");
                connection.connect();

                final int statusCode = connection.getResponseCode();
                success = statusCode == HttpURLConnection.HTTP_OK;
                logger.info("status code " + statusCode);

            } catch (IOException e) {
                e.printStackTrace();
                success = false;
            } finally {
                if (connection != null)
                    connection.disconnect();
            }

            logger.info("logging " + success);

            if (success)
                idsToRemove.add(backLogId); //success, remove from backlog
        }

        //bulk remove ids
        ofy().delete().type(BackLog.class).ids(idsToRemove).now();
    }
}
