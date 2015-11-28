package reach.backend.Servlets;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import reach.backend.OfyService;
import reach.backend.User.ReachUser;

import static reach.backend.OfyService.ofy;

public class Worker extends HttpServlet {

    private static final Logger logger = Logger.getLogger(Worker.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);

        logger.info("Hello_world");
        final ReachUser devika = ofy().load().type(ReachUser.class).id(OfyService.devikaId).now();

        devika.getMyReach().clear();
        devika.getSentRequests().clear();
        ofy().save().entities(devika).now();
    }
}
