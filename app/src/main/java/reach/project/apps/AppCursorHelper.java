package reach.project.apps;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.hash.Hashing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.project.utils.ContentType;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 23/02/16.
 */
public enum AppCursorHelper {
    ;

    public static Function<ApplicationInfo, App.Builder> getParser(PackageManager packageManager,
                                                                   Set<String> packageVisibility) {

        return new Function<ApplicationInfo, App.Builder>() {
            @Nullable
            @Override
            public App.Builder apply(@Nullable ApplicationInfo input) {

                if (input == null)
                    return null;

                final App.Builder appBuilder = new App.Builder();
                appBuilder.launchIntentFound(packageManager.getLaunchIntentForPackage(input.packageName) != null);
                appBuilder.applicationName(input.loadLabel(packageManager) + "");
                appBuilder.description(input.loadDescription(packageManager) + "");
                appBuilder.packageName(input.packageName);
                appBuilder.processName(input.processName);

                try {
                    appBuilder.installDate(
                            packageManager.getPackageInfo(input.packageName, 0).firstInstallTime);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                appBuilder.visible(packageVisibility.contains(input.packageName));
                return appBuilder;
            }
        };
    }

    public static List<App.Builder> getApps(@Nonnull List<ApplicationInfo> installedApps,
                                            @Nonnull PackageManager packageManager,
                                            @Nonnull HandOverMessage<Integer> handOverMessage,

                                            @Nullable Map<String, EnumSet<ContentType.State>> oldStates) {

        final Function<App.Builder, App.Builder> oldStatePersister;
        if (oldStates != null && oldStates.size() > 0)
            oldStatePersister = getOldStatePersister(oldStates);
        else
            oldStatePersister = Functions.identity();

        final List<App.Builder> toReturn = new ArrayList<>();
        int counter = 0;
        for (ApplicationInfo applicationInfo : installedApps) {

            App.Builder appBuilder = getParser(packageManager, Collections.emptySet()).apply(applicationInfo);
            appBuilder = oldStatePersister.apply(appBuilder);
            if (appBuilder != null) {
                handOverMessage.handOverMessage(++counter);
                toReturn.add(appBuilder);
            }

        }
        Log.i("Ayush", "Reading apps " + installedApps.size());
        return toReturn;
    }

    private static Function<App.Builder, App.Builder> getOldStatePersister(final Map<String, EnumSet<ContentType.State>> persistStates) {

        return new Function<App.Builder, App.Builder>() {
            @Nullable
            @Override
            public App.Builder apply(@Nullable App.Builder input) {

                if (input == null)
                    return null;

                final String metaHash = MiscUtils.calculateAppHash(input.packageName, Hashing.sipHash24());
                final EnumSet<ContentType.State> oldStates = persistStates.get(metaHash);
                if (oldStates != null)
                    input.visible(oldStates.contains(ContentType.State.VISIBLE));
                return input;
            }
        };
    }

    public static final Function<App.Builder, App> BUILDER_APP_FUNCTION = new Function<App.Builder, App>() {
        @Nullable
        @Override
        public App apply(@Nullable App.Builder input) {
            return input != null ? input.build() : null;
        }
    };
}