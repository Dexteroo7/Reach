package reach.project.coreViews.myProfile.music;

import android.app.Activity;
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
import android.text.TextUtils;
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
import reach.project.music.ReachDatabase;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
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

    private static final String TAG = MyLibraryFragment.class.getSimpleName();
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
        emptyViewText.setText("Dawg");
        emptyView = rootView.findViewById(R.id.empty_imageView);


        mRecyclerView.setEmptyView(emptyView);
        MaterialViewPagerHelper.registerRecyclerView(activity, mRecyclerView, null);

        final SharedPreferences preferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        myUserId = SharedPrefUtils.getServerId(preferences);

        getLoaderManager().initLoader(StaticData.PRIVACY_MY_LIBRARY_LOADER, null, this);

        return rootView;
    }

    
    @Override
    public void onDestroyView() {

        super.onDestroyView();
        getLoaderManager().destroyLoader(StaticData.PRIVACY_MY_LIBRARY_LOADER);
        if (parentAdapter != null)
            parentAdapter.close();
    }

    @Override
    public void handOverMessage(@Nonnull Object message) {

        if (message instanceof Cursor) {

            final Cursor cursor = (Cursor) message;
            final boolean visible = cursor.getShort(12) == 1;
            final String metaHash = cursor.getString(2);

            new ToggleVisibility(MyLibraryFragment.this,
                    (long) (visible ? 0 : 1),
                    metaHash,
                    myUserId
                    ).executeOnExecutor(visibilityHandler
                    );
            //flip
            updateDatabase(new WeakReference<>(this), !visible, metaHash, myUserId);

        } else if (message instanceof Song) {

            final Song song = (Song) message;

            new ToggleVisibility(MyLibraryFragment.this,
                    (long) (song.visibility ? 0 : 1),
                    song.getFileHash(),
                    myUserId
                    ).executeOnExecutor(visibilityHandler
                     //flip
                   );
            //flip
            updateDatabase(new WeakReference<>(this), !song.visibility, song.getFileHash(), myUserId);

        } else
            throw new IllegalArgumentException("Unknown type handed over");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.PRIVACY_MY_LIBRARY_LOADER)
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

        if (loader.getId() == StaticData.PRIVACY_MY_LIBRARY_LOADER) {

            parentAdapter.setNewMyLibraryCursor(data);
            final int count = data.getCount();
            if (count != parentAdapter.myLibraryCount) //update only if count has changed
                parentAdapter.updateRecentMusic(getRecentMyLibrary());

        }
        mRecyclerView.checkIfEmpty(parentAdapter.getItemCount() - 1);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (parentAdapter == null)
            return;
        if (loader.getId() == StaticData.PRIVACY_MY_LIBRARY_LOADER)
            parentAdapter.setNewMyLibraryCursor(null);
    }

    @NonNull
    private List<Song> getRecentMyLibrary() {

        /*final Cursor cursor = new CursorLoader(getActivity(),
                SongProvider.CONTENT_URI,
                SongCursorHelper.SONG_HELPER.getProjection(),
                "(" + SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_STATUS + " = ?) or " +
                        SongHelper.COLUMN_OPERATION_KIND + " = ?",
                new String[]{
                        ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                        ReachDatabase.Status.FINISHED.getString(),
                        ReachDatabase.OperationKind.OWN.getString()},
                SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");*/

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

        /*final Cursor cursor = getContext().getContentResolver().query(MySongsProvider.CONTENT_URI,
                SongCuHelper.DISK_LIST,
                null, null, //all songs
                MySongsHelper.COLUMN_DATE_ADDED + " DESC, " +
                        MySongsHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC LIMIT 20");
*/
        if (cursor == null)
            return Collections.emptyList();

        /*final List<MusicData> latestMyLibrary = new ArrayList<>(cursor.getCount());*/
        final List<Song> latestMyLibrary = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            if (TextUtils.isEmpty(cursor.getString(2)))
                continue;
            /*latestMyLibrary.add(MySongsHelper.getMusicData(cursor, userId));*/
            latestMyLibrary.add(SongCursorHelper.SONG_HELPER.parse(cursor));
        }

        cursor.close();

        return latestMyLibrary;
    }

    private static class ToggleVisibility extends AsyncTask<Void, Void, Boolean> {

        private final long param1;
        private final String param2;
        private final long param3;

        private WeakReference<MyLibraryFragment> myLibraryFragmentWeakReference;

        public ToggleVisibility(MyLibraryFragment myLibraryFragment, long param1, String param2, long param3) {
            this.myLibraryFragmentWeakReference = new WeakReference<>(myLibraryFragment);
            this.param1 = param1;
            this.param2 = param2;
            this.param3 = param3;

        }

        /**
         * params[0] = oldVisibility
         * params[1] = songId
         * params[2] = userId
         */

        @Override
        protected Boolean doInBackground(Void... params) {

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
                updateDatabase(myLibraryFragmentWeakReference, param1 != 1, param2, param3);
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

    private static synchronized void updateDatabase(WeakReference<MyLibraryFragment> myLibraryFragmentWeakReference,
                                                    final boolean visibility,
                                                    String metaHash,
                                                    long userId) {

        final ContentResolver resolver = MiscUtils.useContextFromFragment(myLibraryFragmentWeakReference, ContextWrapper::getContentResolver).orNull();
        //sanity check
        if (resolver == null || userId == 0)
            return;

        ContentValues values = new ContentValues();
        values.put(SongHelper.COLUMN_VISIBILITY, visibility);

        int updated = resolver.update(
                SongProvider.CONTENT_URI,
                values,
                SongHelper.COLUMN_META_HASH + " = ?",
                new String[]{metaHash + ""});

        /*if (updated == 0) {

            values = new ContentValues();
            values.put(SongHelper.COLUMN_VISIBILITY, visibility);
            updated = resolver.update(
                    SongProvider.CONTENT_URI,
                    values,
                    SongHelper.COLUMN_UNIQUE_ID + " = ? and " + SongHelper.COLUMN_RECEIVER_ID + " = ?",
                    new String[]{songId + "", userId + ""});
        }*/

        //flip in recent list
        MiscUtils.runOnUiThreadFragment(myLibraryFragmentWeakReference, (MyLibraryFragment fragment) -> {
            if (fragment.parentAdapter != null)
                fragment.parentAdapter.updateVisibility(metaHash, visibility);
        });

        Log.i("Ayush", "Toggle Visibility " + updated + " " + metaHash + " " + visibility);
    }
}
