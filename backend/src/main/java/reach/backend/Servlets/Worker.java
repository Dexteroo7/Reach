package reach.backend.Servlets;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Worker extends HttpServlet {

    private static final Logger logger = Logger.getLogger(Worker.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);

        logger.info("Hello_world");
//        QueryResultIterator<ReachUser> userQueryResultIterator;
//        String cursorStr = req.getParameter("cursor");
//        ReachUser reachUser;
//
//        int totalProcessed = Integer.parseInt(req.getParameter("total"));
//        int currentProcessed = 0;
//
//        if (cursorStr != null && !cursorStr.equals(""))
//            userQueryResultIterator = ofy().load().type(ReachUser.class)
//                    .startAt(Cursor.fromWebSafeString(cursorStr))
//                    .limit(100)
//                    .iterator();
//        else
//            userQueryResultIterator = ofy().load().type(ReachUser.class).limit(100).iterator();
//
//
//        while (userQueryResultIterator.hasNext()) {
//
//            reachUser = userQueryResultIterator.next();
//
//            reachUser.setNumberOfSongs((int) reachUser.getMegaBytesReceived());
//            reachUser.setSplitterId(reachUser.getMegaBytesSent());
//            reachUser.setTimeCreated(System.currentTimeMillis());
//            if (reachUser.getGenres().length() > 499)
//                reachUser.setGenres(reachUser.getGenres().substring(0, 499));
//
//            ofy().save().entity(reachUser).now();
//            currentProcessed++;
//        }
//
//        totalProcessed += currentProcessed;
//        logger.info("CURRENT " + currentProcessed);
//        logger.info("TOTAL " + totalProcessed);
//
//        if (currentProcessed == 100)
//            QueueFactory.getDefaultQueue().add(TaskOptions.Builder
//                    .withUrl("/worker")
//                    .param("cursor", userQueryResultIterator.getCursor().toWebSafeString())
//                    .param("total", totalProcessed + "")
//                    .retryOptions(RetryOptions.Builder.withTaskRetryLimit(0)));
//        else
//            logger.info("FINISHED");
//
//        userQueryResultIterator = null;
//        cursorStr = null;
//        reachUser = null;
//        System.gc();
    }
}
