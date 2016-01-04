package reach.project.coreViews.myProfile.music;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
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
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.florent37.materialviewpager.MaterialViewPagerHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

import reach.backend.music.musicVisibilityApi.model.MyString;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 25/11/15.
 */
public class MyLibraryFragment extends Fragment implements HandOverMessage, LoaderManager.LoaderCallbacks<Cursor> {

    private static WeakReference<MyLibraryFragment> reference = null;
    private static long myUserId = 0;

    public static MyLibraryFragment getInstance(String header) {

        final Bundle args;
        MyLibraryFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new MyLibraryFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing MyLibraryFragment object :)");
            args = fragment.getArguments();
        }
        args.putString("header", header);
        return fragment;
    }

    private ParentAdapter parentAdapter = new ParentAdapter(this, this);
    //handle 2 at a time
    private final ExecutorService visibilityHandler = Executors.unconfigurableExecutorService(Executors.newFixedThreadPool(2));
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_simple_recycler, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final Activity activity = getActivity();

        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(activity));
        mRecyclerView.setAdapter(parentAdapter);
        MaterialViewPagerHelper.registerRecyclerView(activity, mRecyclerView, null);

        final SharedPreferences preferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        myUserId = SharedPrefUtils.getServerId(preferences);

        getLoaderManager().initLoader(StaticData.PRIVACY_DOWNLOADED_LOADER, null, this);
        getLoaderManager().initLoader(StaticData.PRIVACY_MY_LIBRARY_LOADER, null, this);

        return rootView;
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        getLoaderManager().destroyLoader(StaticData.PRIVACY_DOWNLOADED_LOADER);
        getLoaderManager().destroyLoader(StaticData.PRIVACY_MY_LIBRARY_LOADER);
        parentAdapter.close();
    }

    @Override
    public void handOverMessage(@Nonnull Object message) {

        if (message instanceof Cursor) {

            final Cursor cursor = (Cursor) message;
            final boolean visible = cursor.getShort(8) == 1;
            final long songId = cursor.getLong(1);

            updateDatabase(!visible, songId, myUserId, getContext());

            new ToggleVisibility().executeOnExecutor(visibilityHandler,
                    (long) (visible ? 0 : 1), //flip
                    songId,
                    myUserId);

            parentAdapter.updateVisibility(songId, !visible);

        } else if (message instanceof PrivacySongItem) {

            final PrivacySongItem song = (PrivacySongItem) message;
            updateDatabase(!song.visible, song.songId, myUserId, getContext());

            new ToggleVisibility().executeOnExecutor(visibilityHandler,
                    (long) (song.visible ? 0 : 1), //flip
                    song.songId,
                    myUserId);

            parentAdapter.updateVisibility(song.songId, !song.visible);

        } else
            throw new IllegalArgumentException("Unknown type handed over");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.PRIVACY_MY_LIBRARY_LOADER)
            return new CursorLoader(getActivity(),
                    MySongsProvider.CONTENT_URI,
                    projectionMyLibrary,
                    null, null, null); //show all songs !
        else if (id == StaticData.PRIVACY_DOWNLOADED_LOADER)
            return new CursorLoader(getActivity(),
                    ReachDatabaseProvider.CONTENT_URI,
                    projectionDownloaded,
                    ReachDatabaseHelper.COLUMN_STATUS + " = ? and " + //show only finished
                            ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                    new String[]{ReachDatabase.FINISHED + "", "0"}, null);

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed())
            return;

        final int count = data.getCount();

        if (loader.getId() == StaticData.PRIVACY_MY_LIBRARY_LOADER) {

            parentAdapter.setNewMyLibraryCursor(data);
            if (count != parentAdapter.myLibraryCount) //update only if count has changed
                parentAdapter.updateRecentMusic(getRecentMyLibrary());

        } else if (loader.getId() == StaticData.PRIVACY_DOWNLOADED_LOADER) {

            parentAdapter.setNewDownLoadCursor(data);
            if (count != parentAdapter.downloadedCount) //update only if count has changed
                parentAdapter.updateRecentMusic(getRecentDownloaded());

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.PRIVACY_MY_LIBRARY_LOADER)
            parentAdapter.setNewMyLibraryCursor(null);
        else if (loader.getId() == StaticData.PRIVACY_DOWNLOADED_LOADER)
            parentAdapter.setNewDownLoadCursor(null);
    }

    private final String[] projectionMyLibrary =
            {
                    MySongsHelper.COLUMN_ID, //0

                    MySongsHelper.COLUMN_SONG_ID, //1

                    MySongsHelper.COLUMN_DISPLAY_NAME, //2
                    MySongsHelper.COLUMN_ACTUAL_NAME, //3

                    MySongsHelper.COLUMN_ARTIST, //4
                    MySongsHelper.COLUMN_ALBUM, //5

                    MySongsHelper.COLUMN_DURATION, //6
                    MySongsHelper.COLUMN_SIZE, //7

                    MySongsHelper.COLUMN_VISIBILITY, //8
                    MySongsHelper.COLUMN_GENRE //9
            };

    private final String[] projectionDownloaded =
            {
                    ReachDatabaseHelper.COLUMN_ID, //0

                    ReachDatabaseHelper.COLUMN_UNIQUE_ID, //1

                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //2
                    ReachDatabaseHelper.COLUMN_ACTUAL_NAME, //3

                    ReachDatabaseHelper.COLUMN_ARTIST, //4
                    ReachDatabaseHelper.COLUMN_ALBUM, //5

                    ReachDatabaseHelper.COLUMN_DURATION, //6
                    ReachDatabaseHelper.COLUMN_SIZE, //7

                    ReachDatabaseHelper.COLUMN_VISIBILITY, //8
                    ReachDatabaseHelper.COLUMN_GENRE, //9
            };

    @NonNull
    private List<PrivacySongItem> getRecentDownloaded() {

        final Cursor cursor = getContext().getContentResolver().query(ReachDatabaseProvider.CONTENT_URI,
                projectionDownloaded,
                ReachDatabaseHelper.COLUMN_STATUS + " = ? and " + //show only finished
                        ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                new String[]{ReachDatabase.FINISHED + "", "0"},
                ReachDatabaseHelper.COLUMN_DATE_ADDED + " DESC, " +
                        ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " ASC LIMIT 20"); //top 20

        if (cursor == null)
            return Collections.emptyList();

        final List<PrivacySongItem> latestDownloaded = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {

            final PrivacySongItem songItem = new PrivacySongItem();
            songItem.songId = cursor.getLong(1);
            songItem.displayName = cursor.getString(2);
            songItem.actualName = cursor.getString(3);
            songItem.artistName = cursor.getString(4);
            songItem.albumName = cursor.getString(5);
            songItem.duration = cursor.getLong(6);
            songItem.size = cursor.getLong(7);
            songItem.visible = cursor.getShort(8) == 1;

            latestDownloaded.add(songItem);
        }

        cursor.close();

        return latestDownloaded;
    }

    @NonNull
    private List<PrivacySongItem> getRecentMyLibrary() {

        final Cursor cursor = getContext().getContentResolver().query(MySongsProvider.CONTENT_URI,
                projectionMyLibrary,
                null, null,
                MySongsHelper.COLUMN_DATE_ADDED + " DESC, " +
                        MySongsHelper.COLUMN_DISPLAY_NAME + " ASC LIMIT 20"); //top 20

        if (cursor == null)
            return Collections.emptyList();

        final List<PrivacySongItem> latestMyLibrary = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {

            final PrivacySongItem songItem = new PrivacySongItem();
            songItem.songId = cursor.getLong(1);
            songItem.displayName = cursor.getString(2);
            songItem.actualName = cursor.getString(3);
            songItem.artistName = cursor.getString(4);
            songItem.albumName = cursor.getString(5);
            songItem.duration = cursor.getLong(6);
            songItem.size = cursor.getLong(7);
            songItem.visible = cursor.getShort(8) == 1;

            latestMyLibrary.add(songItem);
        }

        cursor.close();

        return latestMyLibrary;
    }

    public static class ToggleVisibility extends AsyncTask<Long, Void, Boolean> {

        /**
         * params[0] = oldVisibility
         * params[1] = songId
         * params[2] = userId
         */

        @Override
        protected Boolean doInBackground(Long... params) {

            boolean failed = false;
            try {
                final MyString response = StaticData.MUSIC_VISIBILITY_API.update(
                        params[2], //serverId
                        params[1], //songId
                        params[0] == 0).execute(); //if 0 (false) make it true and vice-versa
                if (response == null || TextUtils.isEmpty(response.getString()) || response.getString().equals("false"))
                    failed = true; //mark failed
            } catch (IOException e) {
                e.printStackTrace();
                failed = true; //mark failed
            }

            if (failed) {
                MiscUtils.useContextFromFragment(reference, context -> {
                    //reset if failed
                    updateDatabase(params[0] != 1, params[1], params[2], context);
                });
            }

            return failed;
        }

        @Override
        protected void onPostExecute(Boolean failed) {

            super.onPostExecute(failed);
            if (failed)
                MiscUtils.useContextFromFragment(reference, context -> {
                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                });
        }
    }

    public static synchronized void updateDatabase(boolean visibility, long songId, long userId, Context context) {

        //sanity check
        if (context == null || userId == 0)
            return;

        final ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues();
        values.put(MySongsHelper.COLUMN_VISIBILITY, visibility); //flip

        int updated = resolver.update(
                MySongsProvider.CONTENT_URI,
                values,
                MySongsHelper.COLUMN_SONG_ID + " = ?",
                new String[]{songId + ""});

        Log.i("Ayush", "Toggle Visibility " + updated + " " + songId + " " + visibility);

        if (updated == 0) {

            values = new ContentValues();
            values.put(ReachDatabaseHelper.COLUMN_VISIBILITY, visibility); //flip
            updated = resolver.update(
                    ReachDatabaseProvider.CONTENT_URI,
                    values,
                    ReachDatabaseHelper.COLUMN_UNIQUE_ID + " = ? and " + ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ?",
                    new String[]{songId + "", userId + ""});
        }

        Log.i("Ayush", "Toggle Visibility " + updated + " " + songId + " " + visibility);
    }
}
