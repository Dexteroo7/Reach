package reach.project.coreViews.fileManager.myfiles_search;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.appspot.able_door_616.contentStateApi.ContentStateApi;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.collect.Ordering;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import reach.project.R;
import reach.project.apps.App;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.music.myLibrary.MyLibraryFragment;
import reach.project.coreViews.myProfile.EmptyRecyclerView;
import reach.project.music.ReachDatabase;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.utils.CloudEndPointsUtils;
import reach.project.utils.ContentType;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 25/11/15.
 */
public class MyFilesSearchFragment extends Fragment implements HandOverMessage, SearchAdapter.HandOverVisibilityToggle,
        LoaderManager.LoaderCallbacks<Cursor> {

    private EmptyRecyclerView mRecyclerView;
    private static long myUserId = 0;
    private static final String TAG = MyFilesSearchFragment.class.getSimpleName();
    private SharedPreferences preferences;
    private final ExecutorService applicationsFetcher = Executors.newSingleThreadExecutor();
    private List<App> appData;


    public static MyFilesSearchFragment getInstance(String header) {

        final Bundle args = new Bundle();
        args.putString("header", header);
        MyFilesSearchFragment fragment = new MyFilesSearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private final ExecutorService visibilityHandler = Executors.unconfigurableExecutorService(Executors.newFixedThreadPool(2));

    @Nullable
    private SearchAdapter searchAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_filemanager_music_mylibrary, container, false);
        final Context context = rootView.getContext();
        final TextView emptyViewText = (TextView) rootView.findViewById(R.id.empty_textView);
        emptyViewText.setText("No songs");

        preferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);
        myUserId = SharedPrefUtils.getServerId(preferences);

        mRecyclerView = (EmptyRecyclerView) rootView.findViewById(R.id.recyclerView);
        mRecyclerView.setEmptyView(rootView.findViewById(R.id.empty_imageView));
        searchAdapter = new SearchAdapter(this,this,this,getActivity());
        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(context));
        mRecyclerView.setAdapter(searchAdapter);
        //new GetApplications(this).executeOnExecutor(applicationsFetcher, getContext());
        getLoaderManager().initLoader(StaticData.MY_LIBRARY_LOADER, null, this);
        return rootView;
    }


    @Override
    public void onDestroyView() {

        super.onDestroyView();
        getLoaderManager().destroyLoader(StaticData.MY_LIBRARY_LOADER);
        if (searchAdapter != null)
            searchAdapter.close();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!searchAdapter.appsDataPresent()){
            if((appData = (List<App>) ReachApplication.readCachedFile(getActivity(),StaticData.APP_DATA_CACHE_KEY))==null)
            new GetApplications(this).executeOnExecutor(applicationsFetcher, getContext());
            else{

                if(searchAdapter == null)
                    return;
                else {
                    searchAdapter.updateRecentApps(appData);
                    Log.d(TAG, "onResume: Application Data Fetched From Cache");
                }
            }
        }
    }

    // The song is being played here
    @Override
    public void handOverMessage(@Nonnull Object message) {
        // Cursor is used for full list songs
        if (message instanceof Cursor) {

            final Cursor cursor = (Cursor) message;
            final int count = cursor.getColumnCount();
            final Song musicData = SongCursorHelper.SONG_HELPER.parse(cursor);
            //musicData.setProcessed(musicData.size);
            MiscUtils.playSong(musicData, getContext());
            // Music Data is used for recent list songs
        }
        //TODO: Change Song to APP
        else if (message instanceof App) {

            final App appData = (App) message;
            //musicData.setProcessed(musicData.size);
            MiscUtils.openApp(getContext(), appData.packageName);
        } else
            throw new IllegalArgumentException("Unknown type handed over");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {



        if (id == StaticData.MY_LIBRARY_LOADER)
            if(args == null) {
                return new CursorLoader(getActivity(),
                        SongProvider.CONTENT_URI,
                        SongCursorHelper.SONG_HELPER.getProjection(),
                        "((" + SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_STATUS + " = ?) or " +
                                SongHelper.COLUMN_OPERATION_KIND + " = ?) and " + SongHelper.COLUMN_META_HASH + " != ?",
                        new String[]{
                                ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                                ReachDatabase.Status.FINISHED.getString(),
                                ReachDatabase.OperationKind.OWN.getString(),
                                "NULL"
                        },
                        SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");
            }
        else{
                final String constraint = "%"+args.getString("filter")+"%";
                return new CursorLoader(getActivity(),
                        SongProvider.CONTENT_URI,
                        SongCursorHelper.SONG_HELPER.getProjection(),
                        "((" + SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_STATUS + " = ? and " +
                                SongHelper.COLUMN_DISPLAY_NAME +
                                " like ? ) or ( " +
                                SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_DISPLAY_NAME +
                        " like ? )) and " + SongHelper.COLUMN_META_HASH + " != ? ",
                        new String[]{
                                ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                                ReachDatabase.Status.FINISHED.getString(),
                                constraint,
                                ReachDatabase.OperationKind.OWN.getString(),
                                constraint,
                                "NULL"
                        },
                        SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");

            }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed() || searchAdapter == null)
            return;

        final int count = data.getCount();
        if (loader.getId() == StaticData.MY_LIBRARY_LOADER) {

            Log.i("Ayush", "MyLibrary file manager " + count);
            StaticData.librarySongsCount = count;

            searchAdapter.setNewMyLibraryCursor(data);
        }
        //mRecyclerView.checkIfEmpty(searchAdapter.getItemCount());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (searchAdapter == null)
            return;

        if (loader.getId() == StaticData.MY_LIBRARY_LOADER)
            searchAdapter.setNewMyLibraryCursor(null);
    }

    //OnClick Visibility Toggle From Extra Button Menu
    //Position is provided in case it's needed
    @Override
    public void HandoverMessage(int position, @NonNull Object message) {

        if (message instanceof Cursor) {
            Log.d(TAG, "Object is of Cursor type, change visibility");

            final Cursor cursor = (Cursor) message;
            final boolean visible = cursor.getShort(12) == 1;
            final String metaHash = cursor.getString(2);

            new ToggleVisibility(MyFilesSearchFragment.this,
                    !visible,
                    metaHash,
                    myUserId,
                    StaticData.zeroByte)
                    .executeOnExecutor(visibilityHandler);
            //flip
            updateDatabase(new WeakReference<>(this), !visible, metaHash, myUserId,StaticData.zeroByte);

        }
        else if(message instanceof App){
            
        }


    }



    private static class ToggleVisibility extends AsyncTask<Void, Void, Boolean> {

        private final boolean visible;
        private final String metaHash;
        private final long userId;
        private final byte type;

        private WeakReference<MyFilesSearchFragment> MyFilesSearchFragmentWeakReference;

        public ToggleVisibility(MyFilesSearchFragment MyFilesSearchFragment, boolean visible, String metaHash, long userId, byte type) {
            this.MyFilesSearchFragmentWeakReference = new WeakReference<>(MyFilesSearchFragment);
            this.visible = visible;
            this.metaHash = metaHash;
            this.userId = userId;
            this.type = type;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            return MiscUtils.useFragment(MyFilesSearchFragmentWeakReference, fragment -> {
                boolean failed = false;

                final HttpTransport transport = new NetHttpTransport();
                final JsonFactory factory = new JacksonFactory();
                final GoogleAccountCredential credential = GoogleAccountCredential
                        .usingAudience(fragment.getContext(), StaticData.SCOPE)
                        .setSelectedAccountName(SharedPrefUtils.getEmailId(fragment.preferences));

                final ContentStateApi contentStateApi = CloudEndPointsUtils.updateBuilder(new ContentStateApi.Builder(transport, factory, credential))
                        .setRootUrl("https://1-dot-client-module-dot-able-door-616.appspot.com/_ah/api/").build();

                try {
                    contentStateApi.update(userId, ContentType.MUSIC.name(), metaHash, ContentType.State.VISIBLE.name(), visible).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    failed = true; //mark failed
                }
                return failed;
            }).or(Boolean.TRUE);

        }

        @Override
        protected void onPostExecute(Boolean failed) {

            super.onPostExecute(failed);
            if (failed) {
                updateDatabase(MyFilesSearchFragmentWeakReference, visible, metaHash, userId, type);
                MiscUtils.useContextFromFragment(MyFilesSearchFragmentWeakReference, context -> {
                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }


    // type == 0 for cursor object, type == 1 for Song object
    private static synchronized void updateDatabase(WeakReference<MyFilesSearchFragment> MyFilesSearchFragmentWeakReference,
                                                    final boolean newVisibility,
                                                    String metaHash,
                                                    long userId, byte type) {

        final ContentResolver resolver = MiscUtils.useContextFromFragment(MyFilesSearchFragmentWeakReference, ContextWrapper::getContentResolver).orNull();
        //sanity check
        if (resolver == null || userId == 0)
            return;

        ContentValues values = new ContentValues();
        values.put(SongHelper.COLUMN_VISIBILITY, newVisibility);

        int updated = resolver.update(
                SongProvider.CONTENT_URI,
                values,
                SongHelper.COLUMN_META_HASH + " = ?",
                new String[]{metaHash + ""});

        MyLibraryFragment.SONGS_DATA_CHANNGED = true;

        Log.i("Ayush", "Toggle Visibility " + updated + " " + metaHash + " " + newVisibility);
    }
    
    


    private static final class GetApplications extends AsyncTask<Context, Void, List<App>> {

        private WeakReference<MyFilesSearchFragment> applicationFragmentWeakReference;

        public GetApplications(MyFilesSearchFragment applicationFragment) {
            this.applicationFragmentWeakReference = new WeakReference<>(applicationFragment);
        }

        @Override
        protected List<App> doInBackground(Context... params) {

            final SharedPreferences preferences = params[0].getSharedPreferences("Reach", Context.MODE_PRIVATE);
            final PackageManager packageManager = params[0].getPackageManager();

            final List<App> apps = MiscUtils.getApplications(packageManager, preferences);
           /* final List<App> recentApps = Ordering
                    .from(StaticData.byInstallDate)
                    .compound(StaticData.byName)
                    .greatestOf(apps, 20);
            Collections.sort(apps, StaticData.byName);*/
            return apps;
        }

        @Override
        protected void onPostExecute(List<App> appData) {
            super.onPostExecute(appData);
            MyFilesSearchFragment context = applicationFragmentWeakReference.get();
            /*if(context!=null){
                context.loadingProgress.setVisibility(View.GONE);
            }*/


            MiscUtils.useFragment(applicationFragmentWeakReference, fragment -> {
                if (fragment.searchAdapter != null) {
                    fragment.searchAdapter.updateRecentApps(appData);
                }
            });
        }
    }


    public void filter(String constraint){
        if(!isAdded())
            return;
        Bundle bundle = new Bundle();
        bundle.putString("filter",constraint);
        getLoaderManager().restartLoader(StaticData.MY_LIBRARY_LOADER,bundle,this);
        if(searchAdapter!=null)
        searchAdapter.filterApps(constraint);
    }


}
