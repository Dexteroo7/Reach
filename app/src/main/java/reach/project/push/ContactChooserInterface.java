package reach.project.push;

import java.util.List;

/**
 * Created by dexter on 22/12/15.
 */
public interface ContactChooserInterface {

    void switchToContactChooser();

    //users to make the push to
    void switchToMessageWriter(List<Long> serverIds);
}
