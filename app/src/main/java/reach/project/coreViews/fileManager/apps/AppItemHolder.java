package reach.project.coreViews.fileManager.apps;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.apps.App;
import reach.project.coreViews.fileManager.HandOverMessageExtra;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ancillaryClasses.UseReferenceWithResult;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 26/11/15.
 */
class AppItemHolder extends SingleItemViewHolder {

    final ImageView appIcon, extraButton;
    final TextView appName;
    public final MenuData menuData;
    final ImageView toggleImage;

    public AppItemHolder(View itemView, HandOverMessageExtra<App> handOverMessageExtra) {

        super(itemView, handOverMessageExtra);
        menuData = new MenuData(-1, handOverMessageExtra);

        this.appIcon = (ImageView) itemView.findViewById(R.id.appIcon);
        this.appName = (TextView) itemView.findViewById(R.id.appName);
        this.toggleImage = (ImageView) itemView.findViewById(R.id.toggleImage);
        this.extraButton = (ImageView) itemView.findViewById(R.id.extraButton);
        this.extraButton.setTag(menuData);
        this.extraButton.setOnClickListener(EXTRA_CLICK);
    }

    private static final View.OnClickListener EXTRA_CLICK = view -> {

        final Object packageNameObject = view.getTag();
        if (packageNameObject == null || !(packageNameObject instanceof MenuData))
            return;

        final MenuData menuData = (MenuData) packageNameObject;
        final int position = menuData.position;
        final HandOverMessageExtra<App> handOver =
                MiscUtils.useReference(menuData.handOverMessageExtra, (UseReferenceWithResult<HandOverMessageExtra<App>, HandOverMessageExtra<App>>) hand -> hand).orNull();

        if (position == -1 || handOver == null)
            return;

        final String packageName = handOver.getExtra(position).packageName;
        final Context context = view.getContext();
        final PopupMenu popupMenu = new PopupMenu(context, view);

        popupMenu.inflate(R.menu.app_manager_menu);
        popupMenu.setOnMenuItemClickListener(item -> {

            switch (item.getItemId()) {
                case R.id.hide:
                    //hide
                    final App app = handOver.getExtra(position);
                    handOver.handOverAppVisibilityMessage(position, !app.visible, packageName);
                    item.setTitle(!app.visible?"Visible":"Locked");

                    return true;
                case R.id.uninstall:
                    //uninstall
                    final Uri packageURI = Uri.parse("package:" + packageName);
                    final Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                    context.startActivity(uninstallIntent);
                    //TODO update apps list
                    return true;
                default:
                    return false;
            }
        });

        popupMenu.show();
    };

    static final class MenuData {

        private int position;
        private final WeakReference<HandOverMessageExtra<App>> handOverMessageExtra;

        public MenuData(int position, HandOverMessageExtra<App> handOverMessageExtra) {
            this.position = position;
            this.handOverMessageExtra = new WeakReference<>(handOverMessageExtra);

        }
        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public WeakReference<HandOverMessageExtra<App>> getHandOverMessageExtra() {
            return handOverMessageExtra;
        }


    }


}