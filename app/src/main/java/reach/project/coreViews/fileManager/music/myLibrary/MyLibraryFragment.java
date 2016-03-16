package reach.project.coreViews.fileManager.music.myLibrary;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
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
public class MyLibraryFragment extends Fragment implements HandOverMessage, ParentAdapter.HandOverVisibilityToggle,
        LoaderManager.LoaderCallbacks<Cursor> {

    private EmptyRecyclerView mRecyclerView;
    private static long myUserId = 0;
    private static final String TAG = MyLibraryFragment.class.getSimpleName();
    private SharedPreferences preferences;

    public static MyLibraryFragment getInstance(String header) {

        final Bundle args = new Bundle();
        args.putString("header", header);
        MyLibraryFragment fragment = new MyLibraryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private final ExecutorService visibilityHandler = Executors.unconfigurableExecutorService(Executors.newFixedThreadPool(2));

    @Nullable
    private ParentAdapter parentAdapter;

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
        parentAdapter = new ParentAdapter(this, this, this);
        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(context));
        mRecyclerView.setAdapter(parentAdapter);

        getLoaderManager().initLoader(StaticData.MY_LIBRARY_LOADER, null, this);
        return rootView;
    }


    @Override
    public void onDestroyView() {

        super.onDestroyView();
        getLoaderManager().destroyLoader(StaticData.MY_LIBRARY_LOADER);
        if (parentAdapter != null)
            parentAdapter.close();
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

            // To play songs of the user (not the downloaded ones)
            /*if (count == MySongsHelper.DISK_LIST.length) {*//*

                final Song musicData = SongCursorHelper.SONG_HELPER.parse(cursor);
                MiscUtils.playSong(musicData, getContext());
            }
            //To play the songs downloaded from reach
            else if (count == SongHelper.MUSIC_DATA_LIST.length) {

                final MusicData musicData = SongHelper.getMusicData(cursor);
                MiscUtils.playSong(musicData, getContext());
            } else
                throw new IllegalArgumentException("Unknown column count found");*/
            // Music Data is used for recent list songs
        } else if (message instanceof Song) {

            final Song musicData = (Song) message;
            //musicData.setProcessed(musicData.size);
            MiscUtils.playSong(musicData, getContext());
        } else
            throw new IllegalArgumentException("Unknown type handed over");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.MY_LIBRARY_LOADER)
            return new CursorLoader(getActivity(),
                    SongProvider.CONTENT_URI,
                    SongCursorHelper.SONG_HELPER.getProjection(),
                    "(" + SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_STATUS + " = ?) or " +
                            SongHelper.COLUMN_OPERATION_KIND + " = ?",
                    new String[]{
                            ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                            ReachDatabase.Status.FINISHED.getString(),
                            ReachDatabase.OperationKind.OWN.getString()},
                    SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed() || parentAdapter == null)
            return;

        final int count = data.getCount();
        if (loader.getId() == StaticData.MY_LIBRARY_LOADER) {

            Log.i("Ayush", "MyLibrary file manager " + count);
            //if(StaticData.librarySongsCount < data.getCount()){
            //MyProfileActivity.countChanged = true;
            StaticData.librarySongsCount = count;
            //}

            if (count != parentAdapter.getItemCount() - 1) //update only if count has changed
                parentAdapter.updateRecentMusic(getRecentMyLibrary());
            parentAdapter.setNewMyLibraryCursor(data);
        }
        mRecyclerView.checkIfEmpty(parentAdapter.getItemCount());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (parentAdapter == null)
            return;

        if (loader.getId() == StaticData.MY_LIBRARY_LOADER)
            parentAdapter.setNewMyLibraryCursor(null);
    }

    @NonNull
    private List<Song> getRecentMyLibrary() {

        final Cursor cursor = getContext().getContentResolver().query(
                SongProvider.CONTENT_URI,
                SongCursorHelper.SONG_HELPER.getProjection(),
                "(" + SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_STATUS + " = ?) or " +
                        SongHelper.COLUMN_OPERATION_KIND + " = ?",
                new String[]{
                        ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                        ReachDatabase.Status.FINISHED.getString(),
                        ReachDatabase.OperationKind.OWN.getString()}, //all songs
                SongHelper.COLUMN_DATE_ADDED + " DESC, " +
                        SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC LIMIT 20");

        if (cursor == null)
            return Collections.emptyList();

        /*final List<MusicData> latestMyLibrary = new ArrayList<>(cursor.getCount());*/
        final List<Song> latestMyLibrary = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            /*latestMyLibrary.add(MySongsHelper.getMusicData(cursor, userId));*/
            latestMyLibrary.add(SongCursorHelper.SONG_HELPER.parse(cursor));
        }

        cursor.close();

        return latestMyLibrary;
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

            new ToggleVisibility(MyLibraryFragment.this,
                    visible,
                    metaHash,
                    myUserId,
                    StaticData.zeroByte)
                    .executeOnExecutor(visibilityHandler);
            //flip
            updateDatabase(new WeakReference<>(this), !visible, metaHash, myUserId,StaticData.zeroByte);

        } else if (message instanceof Song) {

            Log.d(TAG, "Object is of song type, change visibility");

            final Song song = (Song) message;

            new ToggleVisibility(MyLibraryFragment.this,
                    song.visibility,
                    song.getFileHash(),
                    myUserId,
                    StaticData.oneByte)
                    .executeOnExecutor(visibilityHandler);
            //flip
            updateDatabase(new WeakReference<>(this), !song.visibility, song.getFileHash(), myUserId,StaticData.oneByte);

        } else
            throw new IllegalArgumentException("Unknown type handed over");


    }


    private static class ToggleVisibility extends AsyncTask<Void, Void, Boolean> {

        private final boolean visible;
        private final String metaHash;
        private final long userId;
        private final byte type;

        private WeakReference<MyLibraryFragment> myLibraryFragmentWeakReference;

        public ToggleVisibility(MyLibraryFragment myLibraryFragment, boolean visible, String metaHash, long userId, byte type) {
            this.myLibraryFragmentWeakReference = new WeakReference<>(myLibraryFragment);
            this.visible = visible;
            this.metaHash = metaHash;
            this.userId = userId;
            this.type = type;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            return MiscUtils.useFragment(myLibraryFragmentWeakReference, fragment -> {
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
                updateDatabase(myLibraryFragmentWeakReference, visible, metaHash, userId, type);
                MiscUtils.useContextFromFragment(myLibraryFragmentWeakReference, context -> {
                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }


    // type == 0 for cursor object, type == 1 for Song object
    private static synchronized void updateDatabase(WeakReference<MyLibraryFragment> myLibraryFragmentWeakReference,
                                                    final boolean newVisibility,
                                                    String metaHash,
                                                    long userId, byte type) {

        final ContentResolver resolver = MiscUtils.useContextFromFragment(myLibraryFragmentWeakReference, ContextWrapper::getContentResolver).orNull();
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

        if (type == StaticData.zeroByte) {
            //flip in recent list
            MiscUtils.runOnUiThreadFragment(myLibraryFragmentWeakReference, (MyLibraryFragment fragment) -> {
                if (fragment.parentAdapter != null) {
                    fragment.parentAdapter.updateVisibility(metaHash,newVisibility);
                }
            });
        }

        Log.i("Ayush", "Toggle Visibility " + updated + " " + metaHash + " " + newVisibility);
    }
}