package reach.project.coreViews.friends;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.common.base.Optional;

import java.lang.ref.WeakReference;

import reach.backend.entities.messaging.model.MyString;
import reach.project.R;
import reach.project.ancillaryViews.SettingsActivity;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.friendsAdapters.FriendsAdapter;
import reach.project.coreViews.friends.invite.InviteActivity;
import reach.project.coreViews.yourProfile.ProfileActivity;
import reach.project.coreViews.yourProfile.YourProfileActivity;
import reach.project.notificationCentre.NotificationActivity;
import reach.project.player.PlayerActivity;
import reach.project.utils.FireOnce;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.HandOverMessage;


public class ContactsListFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, HandOverMessage<FriendsAdapter.ClickData> {

    private static String phoneNumber = "";

    private static WeakReference<ContactsListFragment> reference = null;

    private static long serverId;

    public static ContactsListFragment newInstance() {

        ContactsListFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            Log.i("Ayush", "Creating new instance of contacts list fragment");
            reference = new WeakReference<>(fragment = new ContactsListFragment());
        } else
            Log.i("Ayush", "Reusing contacts list fragment object :)");

        return fragment;
    }

    private final FriendsAdapter friendsAdapter = new FriendsAdapter(this);

    private View rootView;

    @Override
    public void onDestroyView() {

        getLoaderManager().destroyLoader(StaticData.FRIENDS_VERTICAL_LOADER);
        getLoaderManager().destroyLoader(StaticData.FRIENDS_HORIZONTAL_LOADER);
        friendsAdapter.setHorizontalCursor(null);
        friendsAdapter.setVerticalCursor(null);

//        if (inviteAdapter != null)
//            inviteAdapter.cleanUp();

        //listView.setOnScrollListener(null);
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();
        if (activity == null || activity.isFinishing())
            return;

        /*if (getArguments().getBoolean("first", false))
            new InfoDialog().show(getChildFragmentManager(),"info_dialog");*/
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        serverId = SharedPrefUtils.getServerId(sharedPreferences);
        phoneNumber = SharedPrefUtils.getPhoneNumber(sharedPreferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_contacts, container, false);

        if (serverId == 0 || TextUtils.isEmpty(phoneNumber))
            return null;

        final Activity activity = getActivity();

        final Toolbar mToolbar = (Toolbar) rootView.findViewById(R.id.myReachToolbar);
        mToolbar.setTitle("Friends");
        mToolbar.inflateMenu(R.menu.myreach_menu);
        final Menu menu = mToolbar.getMenu();
        mToolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case  R.id.settings_button:
                    startActivity(new Intent(activity, SettingsActivity.class));
                    return true;
                case R.id.player_button:
                    startActivity(new Intent(activity, PlayerActivity.class));
                    return true;
            }
            return false;
        });

        final MenuItem notificationButton = menu.findItem(R.id.notif_button);
        if (notificationButton != null) {

            MenuItemCompat.setActionView(notificationButton, R.layout.reach_queue_counter);
            final View notificationContainer = MenuItemCompat.getActionView(notificationButton).findViewById(R.id.counterContainer);
            notificationContainer.setOnClickListener(v -> startActivity(new Intent(activity, NotificationActivity.class)));
//            notificationCount = (TextView) notificationContainer.findViewById(R.id.reach_q_count);
        }

        //gridView = MiscUtils.addLoadingToGridView((GridView) rootView.findViewById(R.id.contactsList));
        //gridView.setOnItemClickListener(LocalUtils.clickListener);
        //gridView.setOnScrollListener(scrollListener);

        final RelativeLayout inviteContainer = (RelativeLayout) rootView.findViewById(R.id.inviteContainer);
        inviteContainer.setOnClickListener(LocalUtils.inviteListener);
        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.contactsList);
        final GridLayoutManager manager = new GridLayoutManager(activity, 2);

        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {

                final int itemType = friendsAdapter.getItemViewType(position);
                if (itemType == FriendsAdapter.VIEW_TYPE_FRIEND_LARGE || itemType == FriendsAdapter.VIEW_TYPE_LOCKED)
                    return 2;
                else
                    return 1;
            }
        });
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(friendsAdapter);

        if (MiscUtils.isOnline(activity))
            FireOnce.sendPing(
                    null,
                    new WeakReference<>(getActivity().getContentResolver()),
                    serverId);

        getLoaderManager().initLoader(StaticData.FRIENDS_VERTICAL_LOADER, null, this);
        getLoaderManager().initLoader(StaticData.FRIENDS_HORIZONTAL_LOADER, null, this);
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.FRIENDS_VERTICAL_LOADER)
            return new CursorLoader(getActivity(),
                    ReachFriendsProvider.CONTENT_URI,
                    FriendsAdapter.requiredProjection,
                    ReachFriendsHelper.COLUMN_STATUS + " != ?",
                    new String[]{ReachFriendsHelper.REQUEST_NOT_SENT + ""},
                    ReachFriendsHelper.COLUMN_STATUS + " ASC, " + ReachFriendsHelper.COLUMN_USER_NAME + " COLLATE NOCASE ASC");
        else if (id == StaticData.FRIENDS_HORIZONTAL_LOADER)
            return new CursorLoader(getActivity(),
                    ReachFriendsProvider.CONTENT_URI,
                    FriendsAdapter.requiredProjection,
                    ReachFriendsHelper.COLUMN_STATUS + " = ?",
                    new String[]{ReachFriendsHelper.REQUEST_NOT_SENT + ""}, null);
        else
            return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed())
            return;

        if (loader.getId() == StaticData.FRIENDS_VERTICAL_LOADER)
            friendsAdapter.setVerticalCursor(data);

        else if (loader.getId() == StaticData.FRIENDS_HORIZONTAL_LOADER)
            friendsAdapter.setHorizontalCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.FRIENDS_VERTICAL_LOADER)
            friendsAdapter.setVerticalCursor(null);
        else if (loader.getId() == StaticData.FRIENDS_HORIZONTAL_LOADER)
            friendsAdapter.setHorizontalCursor(null);
    }

    @Override
    public void handOverMessage(@NonNull FriendsAdapter.ClickData clickData) {

        if (rootView == null)
            return;

        if (clickData.status < ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED) {

            Log.i("Ayush", "Detected status" + clickData.status);
            YourProfileActivity.openProfile(clickData.friendId, getActivity());
            if (clickData.networkType == 5)
                Snackbar.make(rootView, "The user has disabled Uploads", Snackbar.LENGTH_LONG).show();

        } else {

            ProfileActivity.openProfile(clickData.friendId, getActivity());
        }
        /*else if (clickData.status == 3) {

            final AlertDialog alertDialog = new AlertDialog.Builder(rootView.getContext())
                    .setMessage("Send a friend request to " + clickData.userName + " ?")
                    .setPositiveButton("Yes", LocalUtils.positiveButton)
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    }).create();

            alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag(
                    //set tag to use when positive button click
                    new Object[]{clickData.friendId, clickData.status, new WeakReference<>(rootView)}
            ));
            alertDialog.show();
        }*/
    }

    private enum LocalUtils {
        ;

        public static final SwipeRefreshLayout.OnRefreshListener refreshListener = () -> {

            //TODO
//            if (MiscUtils.isOnline(activity))
//                FireOnce.sendPing(
//                        null,
//                        new WeakReference<>(getActivity().getContentResolver()),
//                        serverId);
        };

        public static final View.OnClickListener inviteListener = v ->
                MiscUtils.useContextFromFragment(reference, context -> {
            context.startActivity(new Intent(context, InviteActivity.class));
        });

        private static final Dialog.OnClickListener positiveButton = (dialog, which) -> {

            final AlertDialog alertDialog = (AlertDialog) dialog;

            final Object[] tag = (Object[]) alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).getTag();

            if (tag == null || tag.length != 3)
                return;

            final long clientId = (long) tag[0];
            final short status = (short) tag[1];
            if (tag[2] != null && tag[2] instanceof WeakReference) {

                final Object object = ((WeakReference) tag[2]).get();
                if (object != null && object instanceof View)
                    Snackbar.make((View) object, "Access Request sent", Snackbar.LENGTH_SHORT).show();
            }

            new SendRequest().executeOnExecutor(
                    StaticData.TEMPORARY_FIX,
                    clientId, serverId, (long) status);

            //Toast.makeText(getActivity(), "Access Request sent", Toast.LENGTH_SHORT).show();
            final ContentValues values = new ContentValues();
            values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED);
            alertDialog.getContext().getContentResolver().update(
                    Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + clientId),
                    values,
                    ReachFriendsHelper.COLUMN_ID + " = ?",
                    new String[]{clientId + ""});
            dialog.dismiss();
        };

        private static final class SendRequest extends AsyncTask<Long, Void, Long> {

            @Override
            protected Long doInBackground(final Long... params) {

                /**
                 * params[0] = other id
                 * params[1] = my id
                 * params[2] = status
                 */

                final MyString dataAfterWork = MiscUtils.autoRetry(() -> StaticData.MESSAGING_API.requestAccess(params[1], params[0]).execute(),
                        Optional.of(input -> (input == null || TextUtils.isEmpty(input.getString()) || input.getString().equals("false")))).orNull();

                final String toParse;
                if (dataAfterWork == null || TextUtils.isEmpty(toParse = dataAfterWork.getString()) || toParse.equals("false"))
                    return params[0];
                return null;
            }

            @Override
            protected void onPostExecute(final Long response) {

                super.onPostExecute(response);

                if (response != null && response > 0) {

                    //response becomes the id of failed person
                    MiscUtils.useContextFromFragment(reference, context -> {

                        Toast.makeText(context, "Request Failed", Toast.LENGTH_SHORT).show();
                        final ContentValues values = new ContentValues();
                        values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_NOT_SENT);
                        context.getContentResolver().update(
                                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + response),
                                values,
                                ReachFriendsHelper.COLUMN_ID + " = ?",
                                new String[]{response + ""});
                        return null;
                    });
                }

            }
        }
        ///////
    }
}