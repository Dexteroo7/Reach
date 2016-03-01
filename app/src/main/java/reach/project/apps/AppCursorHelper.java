package reach.project.apps;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.google.common.base.Function;

import java.util.Set;

import javax.annotation.Nullable;

/**
 * Created by dexter on 23/02/16.
 */
public enum  AppCursorHelper {
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

    public static final Function<App.Builder, App> BUILDER_APP_FUNCTION = new Function<App.Builder, App>() {
        @Nullable
        @Override
        public App apply(@Nullable App.Builder input) {
            return input != null ? input.build() : null;
        }
    };
}