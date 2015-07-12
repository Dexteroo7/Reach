package reach.backend.Servlets;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import reach.backend.User.ReachUser;

import static reach.backend.OfyService.ofy;

public class Worker extends HttpServlet {

    private static final Logger logger = Logger.getLogger(Worker.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);


        QueryResultIterator<ReachUser> userQueryResultIterator;
        String cursorStr = req.getParameter("cursor");
        ReachUser reachUser;

        int totalProcessed = Integer.parseInt(req.getParameter("total"));
        int currentProcessed = 0;

        if (cursorStr != null && !cursorStr.equals(""))
            userQueryResultIterator = ofy().load().type(ReachUser.class)
                    .startAt(Cursor.fromWebSafeString(cursorStr))
                    .limit(100)
                    .iterator();
        else
            userQueryResultIterator = ofy().load().type(ReachUser.class).limit(100).iterator();


        while (userQueryResultIterator.hasNext()) {

            reachUser = userQueryResultIterator.next();
            reachUser.setNumberOfSongs(reachUser.getMegaBytesReceived());
            reachUser.setSplitterId(reachUser.getMegaBytesSent());
            if (reachUser.getGenres().length() > 499)
                reachUser.setGenres(reachUser.getGenres().substring(0, 499));
            ofy().save().entity(reachUser).now();
            currentProcessed++;
        }

        totalProcessed += currentProcessed;
        logger.info("CURRENT " + currentProcessed);
        logger.info("TOTAL " + totalProcessed);

        if (currentProcessed == 100) {
            QueueFactory.getDefaultQueue().add(TaskOptions.Builder
                    .withUrl("/worker")
                    .param("cursor", userQueryResultIterator.getCursor().toWebSafeString())
                    .param("total", totalProcessed + "")
                    .retryOptions(RetryOptions.Builder.withTaskRetryLimit(0)));
        } else
            logger.info("FINISHED");

        userQueryResultIterator = null;
        cursorStr = null;
        reachUser = null;
        System.gc();
    }
}
