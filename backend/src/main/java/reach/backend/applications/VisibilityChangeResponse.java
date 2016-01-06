package reach.backend.applications;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dexter on 06/01/16.
 */
class VisibilityChangeResponse {

    private Map<String, Boolean> visibility = new HashMap<>(500);
    private int hashCode = 0;

    public VisibilityChangeResponse() {

    }

    public VisibilityChangeResponse(Map<String, Boolean> visibility, int hashCode) {
        this.visibility = visibility;
        this.hashCode = hashCode;
    }

    public Map<String, Boolean> getVisibility() {
        return visibility;
    }

    public void setVisibility(Map<String, Boolean> visibility) {
        this.visibility = visibility;
    }

    public int getHashCode() {
        return hashCode;
    }

    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }
}
