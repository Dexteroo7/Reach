package reach.project.utils;

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;

import reach.project.core.ReachApplication;

/**
 * Created by dexter on 09/10/15.
 */
public enum Tracker {
    ;

    private static final HitBuilders.EventBuilder builder = new HitBuilders.EventBuilder();

    public static synchronized void track(ReachApplication application,
                                          Optional<String> category,
                                          Optional<String> action,
                                          Optional<String> label,
                                          int value) {

        application.getTracker().send(builder
                .setCategory(category.isPresent() ? category.get() : null)
                .setAction(action.isPresent() ? action.get() : null)
                .setLabel(label.isPresent() ? label.get() : null)
                .setValue(value)
                .build());

        //reset
        builder.setCategory(null);
        builder.setAction(null);
        builder.setLabel(null);
    }
}