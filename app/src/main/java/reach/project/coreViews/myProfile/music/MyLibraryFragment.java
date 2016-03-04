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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.florent37.materialviewpager.MaterialViewPagerHelper;

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
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.music.ReachDatabase;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 25/11/15.
 */

// Fragment which displays songs of the user
public class MyLibraryFragment extends Fragment implements HandOverMessage, LoaderManager.LoaderCallbacks<Cursor> {

    private static long myUserId = 0;
    private EmptyRecyclerView mRecyclerView;
    private View emptyView;

    public static MyLibraryFragment getInstance(String header) {

        final Bundle args = new Bundle();
        args.putString("header", header);
        MyLibraryFragment fragment = new MyLibraryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private ParentAdapter parentAdapter;
    //handle 2 at a time, for thread queue
    private final ExecutorService visibilityHandler = Executors.unconfigurableExecutorService(Executors.newFixedThreadPool(2));

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_mylibrary_music, container, false);
         mRecyclerView = (EmptyRecyclerView) rootView.findViewById(R.id.recyclerView);
        final Activity activity = getActivity();

        parentAdapter = new ParentAdapter(this, this);
        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(activity));
        mRecyclerView.setAdapter(parentAdapter);
        final TextView emptyViewText = (TextView) rootView.findViewById(R.id.empty_textView);
        emptyViewText.setText(StaticData.NO_SONGS_TEXT);
         emptyView = rootView.findViewById(R.id.empty_imageView);

        mRecyclerView.setEmptyView(emptyView);
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
        if (parentAdapter != null)
            parentAdapter.close();
    }

    @Override
    public void handOverMessage(@Nonnull Object message) {

        if (message instanceof Cursor) {

            final Cursor cursor = (Cursor) message;
            final boolean visible = cursor.getShort(8) == 1;
            final long songId = cursor.getLong(1);

            new ToggleVisibility(MyLibraryFragment.this).executeOnExecutor(visibilityHandler,
                    (long) (visible ? 0 : 1), //flip
                    songId,
                    myUserId);
            //flip
            updateDatabase(MyLibraryFragment.this, !visible, songId, myUserId, getContext().getContentResolver());

        } else if (message instanceof PrivacySongItem) {

            final PrivacySongItem song = (PrivacySongItem) message;

            new ToggleVisibility(MyLibraryFragment.this).executeOnExecutor(visibilityHandler,
                    (long) (song.visible ? 0 : 1), //flip
                    song.songId,
                    myUserId);
            //flip
            updateDatabase(MyLibraryFragment.this, !song.visible, song.songId, myUserId, getContext().getContentResolver());

        } else
            throw new IllegalArgumentException("Unknown type handed over");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.PRIVACY_MY_LIBRARY_LOADER)
            return new CursorLoader(getActivity(),
                    MySongsProvider.CONTENT_URI,
                    projectionMyLibrary,
                    null, null, //show all songs !
                    MySongsHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");
        else if (id == StaticData.PRIVACY_DOWNLOADED_LOADER)
            return new CursorLoader(getActivity(),
                    SongProvider.CONTENT_URI,
                    projectionDownloaded,
                    SongHelper.COLUMN_STATUS + " = ? and " + //show only finished
                            SongHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                    new String[]{ReachDatabase.Status.FINISHED.getString(), "0"},
                    SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed() || parentAdapter == null)
            return;

        final int count = data.getCount();

        if (loader.getId() == StaticData.PRIVACY_MY_LIBRARY_LOADER) {

//            Log.i("Ayush", "MyLibrary my profile " + count);

            parentAdapter.setNewMyLibraryCursor(data);

            if (count != parentAdapter.myLibraryCount) //update only if count has changed
                parentAdapter.updateRecentMusic(getRecentMyLibrary());

        } else if (loader.getId() == StaticData.PRIVACY_DOWNLOADED_LOADER) {

//            Log.i("Ayush", "Downloaded my profile " + count);

            parentAdapter.setNewDownLoadCursor(data);
            if (count != parentAdapter.downloadedCount) //update only if count has changed
                parentAdapter.updateRecentMusic(getRecentDownloaded());

        }
        mRecyclerView.checkIfEmpty(parentAdapter.getItemCount()-1);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (parentAdapter == null)
            return;
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
                    SongHelper.COLUMN_ID, //0

                    SongHelper.COLUMN_UNIQUE_ID, //1

                    SongHelper.COLUMN_DISPLAY_NAME, //2
                    SongHelper.COLUMN_ACTUAL_NAME, //3

                    SongHelper.COLUMN_ARTIST, //4
                    SongHelper.COLUMN_ALBUM, //5

                    SongHelper.COLUMN_DURATION, //6
                    SongHelper.COLUMN_SIZE, //7

                    SongHelper.COLUMN_VISIBILITY, //8
                    SongHelper.COLUMN_GENRE, //9
            };

    @NonNull
    private List<PrivacySongItem> getRecentDownloaded() {

        final Cursor cursor = getContext().getContentResolver().query(SongProvider.CONTENT_URI,
                projectionDownloaded,
                SongHelper.COLUMN_STATUS + " = ? and " + //show only finished
                        SongHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                new String[]{ReachDatabase.Status.FINISHED.getString(), "0"},
                SongHelper.COLUMN_DATE_ADDED + " DESC, " +
                        SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC LIMIT 20"); //top 20

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
                        MySongsHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC LIMIT 20"); //top 20

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

    private static class ToggleVisibility extends AsyncTask<Long, Void, Boolean> {

        private WeakReference<MyLibraryFragment> myLibraryFragmentWeakReference;

        public ToggleVisibility(MyLibraryFragment myLibraryFragment) {
            this.myLibraryFragmentWeakReference = new WeakReference<>(myLibraryFragment);
        }

        /**
         * params[0] = oldVisibility
         * params[1] = songId
         * params[2] = userId
         */

        @Override
        protected Boolean doInBackground(Long... params) {

            boolean failed = false;
            //TODO
//            try {
//                final MyString response = StaticData.MUSIC_VISIBILITY_API.update(
//                        params[2], //serverId
//                        params[1], //songId
//                        params[0] == 1).execute(); //translate to boolean
//                if (response == null || TextUtils.isEmpty(response.getString()) || response.getString().equals("false"))
//                    failed = true; //mark failed
//            } catch (IOException e) {
//                e.printStackTrace();
//                failed = true; //mark failed
//            }

            if (failed) {
                MiscUtils.useFragment(myLibraryFragmentWeakReference, fragment -> {
                    //reset if failed, flip visibility
                    updateDatabase(fragment, params[0] != 1, params[1], params[2], fragment.getContext().getContentResolver());
                });
            }

            return failed;
        }

        @Override
        protected void onPostExecute(Boolean failed) {

            super.onPostExecute(failed);
            if (failed)
                MiscUtils.useContextFromFragment(myLibraryFragmentWeakReference, context -> {
                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                });
        }
    }

    private static synchronized void updateDatabase(MyLibraryFragment myLibraryFragment, final boolean visibility, long songId, long userId, ContentResolver resolver) {

        final WeakReference<MyLibraryFragment> myLibraryFragmentWeakReference = new WeakReference<>(myLibraryFragment);
        //sanity check
        if (resolver == null || userId == 0)
            return;

        ContentValues values = new ContentValues();
        values.put(MySongsHelper.COLUMN_VISIBILITY, visibility);

        int updated = resolver.update(
                MySongsProvider.CONTENT_URI,
                values,
                MySongsHelper.COLUMN_SONG_ID + " = ?",
                new String[]{songId + ""});

        Log.i("Ayush", "Toggle Visibility " + updated + " " + songId + " " + visibility);

        if (updated == 0) {

            values = new ContentValues();
            values.put(SongHelper.COLUMN_VISIBILITY, visibility);
            updated = resolver.update(
                    SongProvider.CONTENT_URI,
                    values,
                    SongHelper.COLUMN_UNIQUE_ID + " = ? and " + SongHelper.COLUMN_RECEIVER_ID + " = ?",
                    new String[]{songId + "", userId + ""});
        }

        //flip in recent list
        MiscUtils.runOnUiThreadFragment(myLibraryFragmentWeakReference, (MyLibraryFragment fragment) -> {
            if (fragment.parentAdapter != null)
                fragment.parentAdapter.updateVisibility(songId, visibility);
        });

        Log.i("Ayush", "Toggle Visibility " + updated + " " + songId + " " + visibility);
    }
}
