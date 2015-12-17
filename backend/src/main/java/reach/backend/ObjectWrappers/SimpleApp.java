package reach.backend.ObjectWrappers;

/**
 * Created by dexter on 15/12/15.
 */
public class SimpleApp {

    public SimpleApp () {

    }

    public Boolean launchIntentFound;
    public Boolean visible;

    public String applicationName;
    public String description;
    public String packageName;
    public String processName;

    public Long installDate;

    public SimpleApp(Boolean launchIntentFound, Boolean visible, String applicationName, String description, String packageName, String processName, Long installDate) {
        this.launchIntentFound = launchIntentFound;
        this.visible = visible;
        this.applicationName = applicationName;
        this.description = description;
        this.packageName = packageName;
        this.processName = processName;
        this.installDate = installDate;
    }

    public Boolean getLaunchIntentFound() {
        return launchIntentFound;
    }

    public void setLaunchIntentFound(Boolean launchIntentFound) {
        this.launchIntentFound = launchIntentFound;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public Long getInstallDate() {
        return installDate;
    }

    public void setInstallDate(Long installDate) {
        this.installDate = installDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleApp)) return false;

        SimpleApp simpleApp = (SimpleApp) o;

        if (applicationName != null ? !applicationName.equals(simpleApp.applicationName) : simpleApp.applicationName != null)
            return false;
        if (packageName != null ? !packageName.equals(simpleApp.packageName) : simpleApp.packageName != null)
            return false;
        return !(processName != null ? !processName.equals(simpleApp.processName) : simpleApp.processName != null);

    }

    @Override
    public int hashCode() {
        int result = applicationName != null ? applicationName.hashCode() : 0;
        result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
        result = 31 * result + (processName != null ? processName.hashCode() : 0);
        return result;
    }
}
