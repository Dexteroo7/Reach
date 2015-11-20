package reach.project.apps;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by dexter on 20/11/15.
 */
public class AppVisibility implements Parcelable {

    final String packageName;
    final boolean visibility;

    public AppVisibility(String packageName, boolean visibility) {

        this.packageName = packageName;
        this.visibility = visibility;
    }

    protected AppVisibility(Parcel in) {

        this.packageName = in.readString();
        this.visibility = in.readByte() == 1;
    }

    public static final Creator<AppVisibility> CREATOR = new Creator<AppVisibility>() {

        @Override
        public AppVisibility createFromParcel(Parcel in) {
            return new AppVisibility(in);
        }

        @Override
        public AppVisibility[] newArray(int size) {
            return new AppVisibility[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        if (visibility)
            dest.writeInt(1);
        else
            dest.writeInt(0);
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isVisibility() {
        return visibility;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppVisibility)) return false;

        AppVisibility that = (AppVisibility) o;

        return !(packageName != null ? !packageName.equals(that.packageName) : that.packageName != null);

    }

    @Override
    public int hashCode() {
        return packageName != null ? packageName.hashCode() : 0;
    }
}
