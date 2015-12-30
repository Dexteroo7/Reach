package reach.project.onBoarding.smsRelated;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by dexter on 24/12/15.
 */
public enum Status implements Parcelable {

    AWAITED_DLR,
    DND_NUMBER,
    OPT_OUT_REJECTION,
    INVALID_NUMBER,
    DELIVERED,
    ERROR;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(name());
    }

    public static final Creator<Status> CREATOR = new Creator<Status>() {

        @Override
        public Status createFromParcel(Parcel in) {
            return Status.valueOf(in.readString());
        }

        @Override
        public Status[] newArray(int size) {
            return new Status[size];
        }
    };


    public static Status parseStatus(String toParse) {

        Log.d("Ashish", toParse);

        if (TextUtils.isEmpty(toParse))
            return ERROR;

        if (toParse.contains("AWAITED"))
            return AWAITED_DLR;

        if (toParse.contains("DND"))
            return DND_NUMBER;

        if (toParse.contains("OPT"))
            return OPT_OUT_REJECTION;

        if (toParse.contains("INV"))
            return INVALID_NUMBER;

        if (toParse.contains("DELIVRD"))
            return DELIVERED;

        return ERROR;
    }
}
