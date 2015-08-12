package reach.project.coreViews;

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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import com.localytics.android.Localytics;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.project.R;
import reach.project.adapter.ReachContactsAdapter;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.auxiliaryClasses.PushContainer;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.StringCompress;
import reach.project.utils.auxiliaryClasses.TransferSong;

/**
 * Created by Dexter on 11-04-2015.
 */
public class ContactsChooserFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private static long serverId;

    private SharedPreferences preferences;
    private ListView listView;
    private SearchView searchView;
    private ReachContactsAdapter reachContactsAdapter = null;

    private String mCurFilter, selection;
    private String[] selectionArguments;

    private short getNetworkType(Context context) {

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

    private final AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

            final Cursor cursor = (Cursor) reachContactsAdapter.getItem(position);

            final PushContainer pushContainer = new PushContainer(
                    cursor.getLong(0),                             //receiverID
                    SharedPrefUtils.getServerId(preferences),      //senderID
                    getArguments().getString("songs"),             //songData
                    SharedPrefUtils.getUserName(preferences),      //userName
                    cursor.getString(2),                           //receiverName
                    getArguments().getShort("song_count"),         //songCount
                    SharedPrefUtils.getImageId(preferences),       //imageID
                    getArguments().getString("song_name"),         //firstSongName
                    getNetworkType(getActivity()) + "");           //networkType

            ((ReachApplication) getActivity().getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Push song")
                    .setAction("User Name - " + SharedPrefUtils.getUserName(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                    .setLabel("Receiver - " + pushContainer.getReceiverName() + ", Songs - " + pushContainer.getSongCount())
                    .setValue(pushContainer.getSongCount())
                    .build());
            Map<String, String> tagValues = new HashMap<>();
            tagValues.put("User Name", SharedPrefUtils.getUserName(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)));
            tagValues.put("Receiver", pushContainer.getReceiverName());
            tagValues.put("Songs", String.valueOf(pushContainer.getSongCount()));
            Localytics.tagEvent("Push song", tagValues);

            if (isRemoving() || isDetached())
                return;
            try {
                PushDialog.newInstance(pushContainer).show(getChildFragmentManager(), "push_dialog");
            } catch (IllegalStateException ignored) {
            }
        }
    };

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
                toSend = new Gson().toJson(pushContainer, PushContainer.class);
            } catch (IllegalStateException | JsonSyntaxException e) {
                e.printStackTrace();
                return fragment;
            }

            args.putString("pushData", toSend);
            args.putString("receiverName", pushContainer.getReceiverName());
            args.putShort("songsCount", pushContainer.getSongCount());
            return fragment;
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

            final short sCount = getArguments().getShort("songsCount");
            if (sCount == 0) {
                dismiss();
                return rootView;
            }
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<font color=\"#f33b5b\"><b>");
            stringBuilder.append(sCount);
            stringBuilder.append(" song");
            if (sCount > 1)
                stringBuilder.append("s");
            stringBuilder.append("</b></font>");
            stringBuilder.append(" selected");
            textMain.setText(Html.fromHtml(stringBuilder.toString()), TextView.BufferType.SPANNABLE);

            final PushContainer pushContainer;
            try {
                pushContainer = new Gson().fromJson(getArguments().getString("pushData"), PushContainer.class);
            } catch (IllegalStateException | JsonSyntaxException e) {
                e.printStackTrace();
                dismiss();
                return rootView;
            }

            exit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });

            done.setOnClickListener(new View.OnClickListener() {

                final class PushSongs extends AsyncTask<PushContainer, Void, MyBoolean> {

                    @Override
                    protected MyBoolean doInBackground(PushContainer... params) {

                        final String pushContainer;
                        try {
                            pushContainer = Base64.encodeToString(StringCompress.compress(new Gson().toJson(params[0], PushContainer.class)), Base64.DEFAULT);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }

                        //TODO retry !!
                        try {
                            StaticData.notificationApi.addPush(
                                    pushContainer,
                                    params[0].getCustomMessage(),
                                    params[0].getFirstSongName(),
                                    params[0].getReceiverId(),
                                    params[0].getSenderId(),
                                    (int) params[0].getSongCount()).execute();
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }

                        return MiscUtils.sendGCM(
                                "PUSH" + pushContainer,
                                params[0].getReceiverId(),
                                params[0].getSenderId()
                        );
                    }

                    @Override
                    protected void onPostExecute(MyBoolean myBoolean) {
                        super.onPostExecute(myBoolean);

                        if (getActivity() == null || getActivity().isFinishing() || isCancelled())
                            return;
                        if (myBoolean == null || myBoolean.getGcmexpired())
                            Toast.makeText(getActivity(), "Network error while sharing songs. Please try again", Toast.LENGTH_SHORT).show();
                        else if (myBoolean.getOtherGCMExpired())
                            Toast.makeText(getActivity(), "Network error while sharing songs. Please try again", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onClick(View v) {

                    captionText.setVisibility(View.INVISIBLE);
                    checkImage.setImageResource(R.drawable.check_white);
                    done.setText("Send to more");
                    final StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(sCount);
                    stringBuilder2.append(" song");
                    if (sCount > 1)
                        stringBuilder2.append("s");
                    stringBuilder2.append(" pushed to <font color=\"#f33b5b\"><b>");
                    stringBuilder2.append(getArguments().getString("receiverName"));
                    stringBuilder2.append("</b></font>");
                    textMain.setText(Html.fromHtml(stringBuilder2.toString()), TextView.BufferType.SPANNABLE);
                    pushContainer.setCustomMessage(captionText.getText().toString());
                    new PushSongs().executeOnExecutor(StaticData.threadPool, pushContainer);
                    exit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getActivity().onBackPressed();
                            getActivity().onBackPressed();
                        }
                    });
                    done.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dismiss();
                        }
                    });
                }
            });

            return rootView;
        }
    }

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
        return fragment;
    }

    @Override
    public void onDestroyView() {

        getLoaderManager().destroyLoader(StaticData.FRIENDS_LOADER);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        serverId = SharedPrefUtils.getServerId(preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
            actionBar.setTitle("Choose contact");
        }

        if (reachContactsAdapter == null)
            reachContactsAdapter = new ReachContactsAdapter(getActivity(), R.layout.myreach_item, null, 0, serverId);
        selection = ReachFriendsHelper.COLUMN_STATUS + " < ?";
        selectionArguments = new String[]{2 + ""};

        listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setOnItemClickListener(clickListener);
        listView.setAdapter(reachContactsAdapter);

        getLoaderManager().initLoader(StaticData.FRIENDS_LOADER, null, this);
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(getActivity(),
                ReachFriendsProvider.CONTENT_URI,
                ReachContactsAdapter.requiredProjection,
                selection,
                selectionArguments,
                ReachFriendsHelper.COLUMN_USER_NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.i("Ayush", "Resetting Contacts adapter, AUTO");
        if (cursorLoader.getId() == StaticData.FRIENDS_LOADER && cursor != null && !cursor.isClosed()) {
            reachContactsAdapter.swapCursor(cursor);
            if (cursor.getCount() == 0)
                MiscUtils.setEmptyTextforListView(listView, "No friends found");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == StaticData.FRIENDS_LOADER)
            reachContactsAdapter.swapCursor(null);
    }


    @Override
    public boolean onClose() {

        searchView.setQuery(null, true);
        selection = ReachFriendsHelper.COLUMN_STATUS + " < ?";
        selectionArguments = new String[]{2 + ""};
        getLoaderManager().restartLoader(StaticData.FRIENDS_LOADER, null, this);
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
            selectionArguments = new String[]{2 + ""};
        } else {
            selection = ReachFriendsHelper.COLUMN_STATUS + " < ? and " +
                    ReachFriendsHelper.COLUMN_USER_NAME + " LIKE ?";
            selectionArguments = new String[]{2 + "", "%" + mCurFilter + "%"};
        }
        getLoaderManager().restartLoader(StaticData.FRIENDS_LOADER, null, this);
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
        if (getArguments().getInt("type") == 0) {

            inflater.inflate(R.menu.search_menu, menu);
            searchView = (SearchView) menu.findItem(R.id.search_button).getActionView();
            searchView.setOnQueryTextListener(this);
            searchView.setOnCloseListener(this);
        } else
            inflater.inflate(R.menu.menu, menu);
    }
}
