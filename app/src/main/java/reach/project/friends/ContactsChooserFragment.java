package reach.project.friends;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;

import reach.backend.notifications.notificationApi.NotificationApi;
import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.music.PushContainer;
import reach.project.music.TransferSong;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.StringCompress;

/**
 * Created by Dexter on 11-04-2015.
 */
public class ContactsChooserFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private ListView listView;
    private SearchView searchView;
    private ReachContactsChooserAdapter reachContactsAdapter = null;

    private String mCurFilter, selection;
    private String[] selectionArguments;

    private static WeakReference<ContactsChooserFragment> reference = null;

    public static ContactsChooserFragment newInstance(HashSet<TransferSong> songs) {

        final Bundle args;
        ContactsChooserFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new ContactsChooserFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing album list fragment object :)");
            args = fragment.getArguments();
        }
        args.putShort("song_count", (short) songs.size());
        args.putString("song_name", songs.iterator().next().getDisplayName());
        args.putString("songs", new Gson().toJson(songs, new TypeToken<HashSet<TransferSong>>() {
        }.getType()));

        Log.i("Ayush", "Pushing " + songs.size() + " songs");
        return fragment;
    }

    @Override
    public void onDestroyView() {

        getLoaderManager().destroyLoader(StaticData.FRIENDS_VERTICAL_LOADER);
        if (reachContactsAdapter != null && reachContactsAdapter.getCursor() != null && !reachContactsAdapter.getCursor().isClosed())
            reachContactsAdapter.getCursor().close();

        listView = null;
        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        searchView = null;
        reachContactsAdapter = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        final Toolbar mToolbar = (Toolbar) rootView.findViewById(R.id.listToolbar);

        mToolbar.setTitle("Choose contact");
        mToolbar.inflateMenu(R.menu.search_menu);
        mToolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        searchView = (SearchView) MenuItemCompat.getActionView(mToolbar.getMenu().findItem(R.id.search_button));
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);

        reachContactsAdapter = new ReachContactsChooserAdapter(getActivity(), R.layout.contacts_chooser_item, null, 0);
        selection = ReachFriendsHelper.COLUMN_STATUS + " < ?";
        selectionArguments = new String[]{"2"};

        listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        listView.setOnItemClickListener(clickListener);
        listView.setAdapter(reachContactsAdapter);

        getLoaderManager().initLoader(StaticData.FRIENDS_VERTICAL_LOADER, null, this);
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(getActivity(),
                ReachFriendsProvider.CONTENT_URI,
                ReachContactsAdapter.requiredProjection,
                selection,
                selectionArguments,
                ReachFriendsHelper.COLUMN_STATUS + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.i("Ayush", "Resetting Contacts adapter, AUTO");
        if (cursorLoader.getId() == StaticData.FRIENDS_VERTICAL_LOADER && cursor != null && !cursor.isClosed()) {
            reachContactsAdapter.swapCursor(cursor);
            if (cursor.getCount() == 0)
                MiscUtils.setEmptyTextForListView(listView, "No friends found");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == StaticData.FRIENDS_VERTICAL_LOADER)
            reachContactsAdapter.swapCursor(null);
    }


    @Override
    public boolean onClose() {

//        searchView.setQuery(null, true);
//        selection = ReachFriendsHelper.COLUMN_STATUS + " < ?";
//        selectionArguments = new String[]{"2"};
//        getLoaderManager().restartLoader(StaticData.FRIENDS_LOADER, null, this);

        if (searchView != null) {
            searchView.setQuery(null, true);
            searchView.clearFocus();
        }
        onQueryTextChange(null);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        if (searchView == null)
            return false;

        // Called when the action bar search text has changed.  Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        final String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        // Don't do anything if the filter hasn't actually changed.
        // Prevents restarting the loader when restoring state.
        if (mCurFilter == null && newFilter == null) {
            return true;
        }
        if (mCurFilter != null && mCurFilter.equals(newFilter)) {
            return true;
        }
        mCurFilter = newFilter;

        if (TextUtils.isEmpty(newText)) {
            selection = ReachFriendsHelper.COLUMN_STATUS + " < ?";
            selectionArguments = new String[]{"2"};
        } else {
            selection = ReachFriendsHelper.COLUMN_STATUS + " < ? and " +
                    ReachFriendsHelper.COLUMN_USER_NAME + " LIKE ?";
            selectionArguments = new String[]{"2", "%" + mCurFilter + "%"};
        }
        getLoaderManager().restartLoader(StaticData.FRIENDS_VERTICAL_LOADER, null, this);
        return true;
    }

    public static class PushDialog extends DialogFragment {

        private static WeakReference<PushDialog> reference;

        public static PushDialog newInstance(PushContainer pushContainer) {

            final Bundle args;
            PushDialog fragment;
            if (reference == null || (fragment = reference.get()) == null) {
                reference = new WeakReference<>(fragment = new PushDialog());
                fragment.setArguments(args = new Bundle());
            } else {
                Log.i("Ayush", "Reusing PushDialog object :)");
                args = fragment.getArguments();
            }

            final String toSend;
            try {
                toSend = Base64.encodeToString(StringCompress.compress(new Gson().toJson(pushContainer, PushContainer.class)), Base64.DEFAULT);
            } catch (IllegalStateException | JsonSyntaxException | IOException e) {
                e.printStackTrace();
                return fragment;
            }

            args.putString("pushData", toSend);

            args.putString("receiverName", pushContainer.getReceiverName());
            args.putLong("receiverId", pushContainer.getReceiverId());
            args.putLong("senderId", pushContainer.getSenderId());
            args.putString("firstSongName", pushContainer.getFirstSongName());
            args.putShort("songsCount", pushContainer.getSongCount());
            return fragment;
        }

        private static final class PushSongs extends AsyncTask<NotificationApi.AddPush, Void, Boolean> {

            @Override
            protected Boolean doInBackground(NotificationApi.AddPush... params) {

                try {

                    params[0].execute();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return false;
            }

            @Override
            protected void onPostExecute(Boolean myBoolean) {

                super.onPostExecute(myBoolean);
                if (myBoolean == null || !myBoolean)
                    MiscUtils.useContextFromFragment(reference, context -> {
                        Toast.makeText(context, "Network error while sharing songs. Please try again", Toast.LENGTH_SHORT).show();
                    });
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            final View rootView = inflater.inflate(R.layout.push_dialog, container, false);
            final TextView textMain = (TextView) rootView.findViewById(R.id.textMain);
            final ImageView checkImage = (ImageView) rootView.findViewById(R.id.checkImage);
            final EditText captionText = (EditText) rootView.findViewById(R.id.captionText);
            final TextView done = (TextView) rootView.findViewById(R.id.done);
            final TextView exit = (TextView) rootView.findViewById(R.id.exit);

            final Bundle arguments = getArguments();

            final String receiverName = arguments.getString("receiverName", "");
            final long receiverId = arguments.getLong("receiverId", 0);
            final long senderId = arguments.getLong("senderId", 0);
            final String firstSongName = arguments.getString("firstSongName", "");
            final String pushData = arguments.getString("pushData", "");
            final short songCount = arguments.getShort("songsCount");

            if (songCount == 0 || receiverId == 0 || senderId == 0 || TextUtils.isEmpty(firstSongName) || TextUtils.isEmpty(receiverName) || TextUtils.isEmpty(pushData)) {
                dismiss();
                return rootView;
            }

            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<font color=\"#f33b5b\"><b>");
            stringBuilder.append(songCount);
            stringBuilder.append(" song");
            if (songCount > 1)
                stringBuilder.append("s");
            stringBuilder.append("</b></font>");
            stringBuilder.append(" selected");
            textMain.setText(Html.fromHtml(stringBuilder.toString()), TextView.BufferType.SPANNABLE);
            exit.setOnClickListener(v -> dismiss());

            done.setOnClickListener(v -> {

                captionText.setVisibility(View.INVISIBLE);
                checkImage.setImageResource(R.drawable.check_white);
                done.setText("Send to more");
                stringBuilder.setLength(0);
                stringBuilder.append(songCount);
                stringBuilder.append(" song");
                if (songCount > 1)
                    stringBuilder.append("s");
                stringBuilder.append(" pushed to <font color=\"#f33b5b\"><b>");
                stringBuilder.append(getArguments().getString("receiverName"));
                stringBuilder.append("</b></font>");
                textMain.setText(Html.fromHtml(stringBuilder.toString()), TextView.BufferType.SPANNABLE);

                final NotificationApi.AddPush addPush;
                try {
                    addPush = StaticData.notificationApi.addPush(
                            pushData,
                            captionText.getText().toString(),
                            firstSongName,
                            receiverId,
                            senderId,
                            (int) songCount);
                } catch (IOException e) {
                    e.printStackTrace();
                    getActivity().onBackPressed();
                    getActivity().onBackPressed();
                    return;
                }

                Log.i("Ayush", "Sending push data " + pushData);

                new PushSongs().executeOnExecutor(StaticData.temporaryFix, addPush);

                exit.setOnClickListener(v1 -> {

                    getActivity().onBackPressed();
                    getActivity().onBackPressed();
                });
                done.setOnClickListener(v1 -> dismiss());
            });

            return rootView;
        }
    }

    private static short getNetworkType(Context context) {

        if (context == null)
            return 0;
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo info = cm.getActiveNetworkInfo();
        final short netType;

        if (info != null && info.isConnected()) {

            final int type = info.getType();
            /**
             * WIFI
             */
            if (type == ConnectivityManager.TYPE_WIFI)
                netType = 1;
            /**
             * MOBILE DATA
             */
            else if (type == ConnectivityManager.TYPE_MOBILE) {

                final int subtype = info.getSubtype();
                if (subtype == TelephonyManager.NETWORK_TYPE_1xRTT ||
                        subtype == TelephonyManager.NETWORK_TYPE_CDMA ||
                        subtype == TelephonyManager.NETWORK_TYPE_EDGE ||
                        subtype == TelephonyManager.NETWORK_TYPE_GPRS ||
                        subtype == TelephonyManager.NETWORK_TYPE_IDEN) {
                    netType = 2;
                } else if (subtype == TelephonyManager.NETWORK_TYPE_EVDO_0 ||
                        subtype == TelephonyManager.NETWORK_TYPE_EVDO_A ||
                        subtype == TelephonyManager.NETWORK_TYPE_HSDPA ||
                        subtype == TelephonyManager.NETWORK_TYPE_HSPA ||
                        subtype == TelephonyManager.NETWORK_TYPE_HSUPA ||
                        subtype == TelephonyManager.NETWORK_TYPE_UMTS ||
                        subtype == TelephonyManager.NETWORK_TYPE_EHRPD ||
                        subtype == TelephonyManager.NETWORK_TYPE_EVDO_B ||
                        subtype == TelephonyManager.NETWORK_TYPE_HSPAP) {
                    netType = 3;
                } else if (subtype == TelephonyManager.NETWORK_TYPE_LTE) {
                    netType = 4;
                } else netType = 0;
            } else netType = 0;
        } else netType = 0;

        return netType;
    }

    private static final AdapterView.OnItemClickListener clickListener = (adapterView, view, position, l) -> {

        final Bundle arguments = MiscUtils.useFragment(reference, Fragment::getArguments).orNull();
        final Activity activity = MiscUtils.useFragment(reference, Fragment::getActivity).orNull();

        if (arguments == null || activity == null)
            return;

        final Cursor cursor = (Cursor) adapterView.getAdapter().getItem(position);
        final SharedPreferences preferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);

        final PushContainer pushContainer = new PushContainer(
                cursor.getLong(0),                             //receiverID
                SharedPrefUtils.getServerId(preferences),      //senderID
                arguments.getString("songs"),             //songData
                SharedPrefUtils.getUserName(preferences),      //userName
                cursor.getString(2),                           //receiverName
                arguments.getShort("song_count"),         //songCount
                SharedPrefUtils.getImageId(preferences),       //imageID
                arguments.getString("song_name"),         //firstSongName
                getNetworkType(activity) + "");           //networkType

        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                .setCategory("Push song")
                .setAction("User Name - " + SharedPrefUtils.getUserName(preferences))
                .setLabel("Receiver - " + pushContainer.getReceiverName() + ", Songs - " + pushContainer.getSongCount())
                .setValue(pushContainer.getSongCount())
                .build());

        MiscUtils.useFragment(reference, fragment -> {
            if (activity.isFinishing())
                return;
            try {
                PushDialog.newInstance(pushContainer).show(fragment.getChildFragmentManager(), "push_dialog");
            } catch (IllegalStateException ignored) {
            }
        });
    };
}
