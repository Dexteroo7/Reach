package reach.project.onBoarding.smsRelated;

import android.text.TextUtils;

/**
 * Created by dexter on 24/12/15.
 */
public enum Status {

    AWAITED_DLR,
    DND_NUMBER,
    OPT_OUT_REJECTION,
    INVALID_NUMBER,
    ERROR;

    public static Status parseStatus(String toParse) {

        if (TextUtils.isEmpty(toParse))
            return ERROR;

        if (toParse.contains("AWAITED"))
            return AWAITED_DLR;

        if (toParse.contains("DND"))
            return AWAITED_DLR;

        if (toParse.contains("OPT"))
            return AWAITED_DLR;

        if (toParse.contains("INVALID"))
            return AWAITED_DLR;

        return ERROR;
    }
}
