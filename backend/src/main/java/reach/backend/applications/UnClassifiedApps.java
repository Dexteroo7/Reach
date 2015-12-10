package reach.backend.applications;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * Created by dexter on 19/11/15.
 */

@Entity
@Cache
@Index
public class UnClassifiedApps {

    @Id
    private String packageName;

    private String applicationName;
    private String description;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnClassifiedApps)) return false;

        UnClassifiedApps that = (UnClassifiedApps) o;

        if (packageName != null ? !packageName.equals(that.packageName) : that.packageName != null)
            return false;
        return !(applicationName != null ? !applicationName.equals(that.applicationName) : that.applicationName != null);

    }

    @Override
    public int hashCode() {
        int result = packageName != null ? packageName.hashCode() : 0;
        result = 31 * result + (applicationName != null ? applicationName.hashCode() : 0);
        return result;
    }
}
