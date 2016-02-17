package reach.project.coreViews.fileManager.apps;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.PopupMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import reach.project.R;
import reach.project.apps.App;
import reach.project.coreViews.friends.HandOverMessageExtra;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 26/11/15.
 */
class AppItemHolder extends SingleItemViewHolder {

    public final ImageView appIcon, extraButton, likeButton, toggleButton;
    public final TextView appName, toggleText;
    private static PopupMenu popupMenu;

    //must set this position
    int position = -1;

    public AppItemHolder(View itemView, HandOverMessageExtra<App> handOverMessageExtra) {
        super(itemView, handOverMessageExtra);

        final Context context = itemView.getContext();

        this.appIcon = (ImageView) itemView.findViewById(R.id.appIcon);
        this.appName = (TextView) itemView.findViewById(R.id.appName);
        this.extraButton = (ImageView) itemView.findViewById(R.id.extraButton);
        this.toggleButton = (ImageView) itemView.findViewById(R.id.toggleButton);
        this.toggleText = (TextView) itemView.findViewById(R.id.toggleText);
        this.likeButton = (ImageView) itemView.findViewById(R.id.likeButton);
        this.likeButton.setOnClickListener(v -> ((ImageView) v).setImageResource(R.drawable.icon_heart_pink));

        this.extraButton.setOnClickListener(v -> {
            if (position == -1)
                throw new IllegalArgumentException("Position not set for the view holder");

            popupMenu = new PopupMenu(context, this.extraButton);
            popupMenu.inflate(R.menu.manager_popup_menu);
            popupMenu.setOnMenuItemClickListener(item -> {

                switch (item.getItemId()) {
                    case R.id.manager_menu_1:
                        //send
                        return true;
                    case R.id.manager_menu_2:
                        //hide
                        return true;
                    case R.id.manager_menu_3:
                        //uninstall
                        Uri packageURI = Uri.parse("package:" + handOverMessageExtra.getExtra(position).packageName);
                        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                        context.startActivity(uninstallIntent);
                        //TODO update apps list
                        return true;
                    default:
                        return false;
                }
            });

            popupMenu.getMenu().findItem(R.id.manager_menu_3).setTitle("Uninstall");
            popupMenu.show();
        });
    }
}
