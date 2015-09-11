package reach.project.userProfile;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.localytics.android.Localytics;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import reach.backend.music.musicVisibilityApi.model.JsonMap;
import reach.backend.music.musicVisibilityApi.model.MusicData;
import reach.project.R;
import reach.project.core.DialogActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.database.sql.ReachSongHelper;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.viewHelpers.CircleTransform;
import reach.project.viewHelpers.TextDrawable;
import reach.project.viewHelpers.ViewPagerReusable;

public class UserMusicLibrary extends Fragment {

    private Toolbar toolbar;

    private SearchView searchView;

    private static WeakReference<UserMusicLibrary> reference = null;

    public static UserMusicLibrary newInstance(long id) {

        final Bundle args;
        UserMusicLibrary fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new UserMusicLibrary());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing UserMusicLibrary object :)");
            args = fragment.getArguments();
        }
        args.putLong("id", id);
        return fragment;
    }

    @Override
    public void onDestroyView() {
        toolbar.setSubtitle("");
        toolbar = null;
        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        searchView = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        final long userId = getArguments().getLong("id");
        final Activity activity = getActivity();

        if (MiscUtils.isOnline(activity))
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new GetMusic(userId));

        final View rootView = inflater.inflate(R.layout.library_view_pager, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout myReachFrame = (FrameLayout) rootView.findViewById(R.id.libraryFrame);
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) myReachFrame.getLayoutParams();
            layoutParams.setMargins(0,0,0,0);
            myReachFrame.setLayoutParams(layoutParams);
        }

        final ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
        final Cursor cursor = rootView.getContext().getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                new String[]{ReachFriendsHelper.COLUMN_PHONE_NUMBER,
                        ReachFriendsHelper.COLUMN_USER_NAME,
                        ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS,
                        ReachFriendsHelper.COLUMN_IMAGE_ID},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""}, null);
        if (cursor == null) return rootView;
        if (!cursor.moveToFirst()) {

            cursor.close();
            return rootView;
        }

        final String phoneNumber = cursor.getString(0);
        final String userName = cursor.getString(1);
        final int numberOfSongs = cursor.getInt(2);
        final String imageId = cursor.getString(3);

        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);

        if (phoneNumber.equals("8860872102") && !SharedPrefUtils.getSecondIntroSeen(sharedPreferences)) {

            SharedPrefUtils.setSecondIntroSeen(sharedPreferences);
            AsyncTask.THREAD_POOL_EXECUTOR.execute(devikaSendMeSomeLove);
        }
        toolbar = ((Toolbar)rootView.findViewById(R.id.libraryToolbar));
        toolbar.setTitle(" " + userName);
        toolbar.setSubtitle(" " + numberOfSongs + " Songs");
        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        toolbar.inflateMenu(R.menu.search_menu);
        MenuItem searchItem = toolbar.getMenu().findItem(R.id.search_button);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);


        final String path;
        if (!TextUtils.isEmpty(imageId) && !imageId.equals("hello_world"))
            path = StaticData.cloudStorageImageBaseUrl + imageId;
        else
            path = "default";

        new SetIcon(userName).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path);

        viewPager.setAdapter(new ViewPagerReusable(
                getChildFragmentManager(),
                new String[]{"Tracks", "Playlists", "Albums", "Artists"},
                new Fragment[]{
                        MusicListFragment.newPagerInstance(userId, 0, searchView), // SONGS
                        PlayListListFragment.newInstance(userId), // PLAY LISTS
                        AlbumListFragment.newInstance(userId), // ALBUMS
                        ArtistListFragment.newInstance(userId)})); // ARTISTS

        final TabLayout slidingTabLayout = (TabLayout) rootView.findViewById(R.id.sliding_tabs);
        slidingTabLayout.post(() -> slidingTabLayout.setupWithViewPager(viewPager));

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position != 0)
                    searchItem.setVisible(false);
                else
                    searchItem.setVisible(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        if (!StaticData.debugMode) {
            ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Browsing Library")
                    .setAction("User Name - " + SharedPrefUtils.getUserName(sharedPreferences))
                    .setLabel("Friend - " + userId)
                    .setValue(1)
                    .build());
            Map<String, String> tagValues = new HashMap<>();
            tagValues.put("User Name", SharedPrefUtils.getUserName(sharedPreferences));
            tagValues.put("Friend", String.valueOf(userId));
            Localytics.tagEvent("Browsing Library", tagValues);
        }

        return rootView;
    }

    private static final Runnable devikaSendMeSomeLove = () -> {

        final Bitmap bmp = MiscUtils.useContextFromFragment(reference, activity -> {
            try {
                return Picasso.with(activity)
                        .load("https://scontent-sin1-1.xx.fbcdn.net/hphotos-xap1/v/t1.0-9/1011255_638449632916744_321328860_n.jpg?oh=5c1daa8d7d015f7ce698ee1793d5a929&oe=55EECF36&dl=1")
                        .centerCrop()
                        .resize(96, 96)
                        .get();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).orNull();

        ////
        MiscUtils.useContextFromFragment(reference, context -> {

            final NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
            final Intent intent = new Intent(context, DialogActivity.class);
            intent.putExtra("type", 2);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                    .setSmallIcon(R.drawable.ic_icon_notif)
                    .setLargeIcon(bmp)
                    .setContentIntent(pendingIntent)
                            //.addAction(0, "Okay! I got it", pendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("You can add multiple songs instantly to your Reach Queue by just clicking on the songs here."))
                    .setContentTitle("Click and Grab!")
                    .setTicker("Click and Grab! You can add multiple songs instantly to your Reach Queue by just clicking on the songs here.")
                    .setContentText("You can add multiple songs instantly to your Reach Queue by just clicking on the songs here.")
                    .setPriority(NotificationCompat.PRIORITY_MAX);
            managerCompat.notify(99911, builder.build());
            return null;
        });
    };

    private final class GetMusic implements Runnable {

        private final long hostId;

        private GetMusic(long hostId) {
            this.hostId = hostId;
        }

        @Override
        public void run() {

            //fetch Music
            final Boolean aBoolean = MiscUtils.useContextFromFragment(reference, (Activity context) -> CloudStorageUtils.getMusicData(hostId, context)).orNull();

            //do we check for visibility ?
            final boolean exit = aBoolean == null || !aBoolean; //reverse because false means exit
            if (exit) {
                Log.i("Ayush", "do not check for visibility");
                return;
            }

            //fetch visibility data
            final MusicData visibility = MiscUtils.autoRetry(() -> StaticData.musicVisibility.get(hostId).execute(), Optional.<Predicate<MusicData>>absent()).orNull();
            final JsonMap visibilityMap;
            if (visibility == null || (visibilityMap = visibility.getVisibility()) == null || visibilityMap.isEmpty()) {
                Log.i("Ayush", "no visibility data found");
                return; //no visibility data found
            }

            //parse visibility data
            final ArrayList<ContentProviderOperation> operations = new ArrayList<>(visibilityMap.size());
            for (Map.Entry<String, Object> objectEntry : visibilityMap.entrySet()) {

                if (objectEntry == null) {
                    Log.i("Ayush", "objectEntry was null");
                    continue;
                }

                final String key = objectEntry.getKey();
                final Object value = objectEntry.getValue();

                if (TextUtils.isEmpty(key) || !TextUtils.isDigitsOnly(key) || value == null || !(value instanceof Boolean)) {
                    Log.i("Ayush", "Found shit data inside visibilityMap " + key + " " + value);
                    continue;
                }

                final long songId = Long.parseLong(key);
                final ContentValues values = new ContentValues();
                values.put(ReachSongHelper.COLUMN_VISIBILITY, (short) ((Boolean) value ? 1 : 0));

                operations.add(ContentProviderOperation
                        .newUpdate(ReachSongProvider.CONTENT_URI)
                        .withValues(values)
                        .withSelection(
                                ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                                        ReachSongHelper.COLUMN_SONG_ID + " = ?",
                                new String[]{hostId + "", songId + ""}).build());
            }

            //update visibility into database
            if (operations.size() > 0) {
                MiscUtils.useContextFromFragment(reference, (Activity context) -> {
                    try {
                        context.getContentResolver().applyBatch(ReachSongProvider.AUTHORITY, operations);
                        Log.i("Ayush", "Visibility updated !");
                    } catch (RemoteException | OperationApplicationException e) {
                        e.printStackTrace();
                    }
                    return null;
                });
            }
        }
    }

    private static class SetIcon extends AsyncTask<String, Void, Bitmap> {

        private final int margin = MiscUtils.dpToPx(44);
        private final String name;

        public SetIcon(String userName) {
            this.name = userName;
        }

        @Override
        protected Bitmap doInBackground(final String... params) {

            if (params[0].equals("default"))
                return null;

            return MiscUtils.useContextFromFragment(reference, context -> {

                try {
                    return Picasso.with(context).load(params[0])
                            .resize(margin, margin)
                            .centerCrop()
                            .transform(new CircleTransform()).get();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }).orNull();
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {

            super.onPostExecute(bitmap);

            MiscUtils.useFragment(reference, fragment -> {

                final Toolbar toolbar = fragment.toolbar;

                if (bitmap == null)

                    toolbar.setLogo(TextDrawable.builder()
                            .beginConfig()
                            .width(margin)
                            .height(margin)
                            .endConfig()
                            .buildRound(MiscUtils.generateInitials(name), fragment.getResources().getColor(R.color.reach_grey)));
                else
                    toolbar.setLogo(new BitmapDrawable(fragment.getResources(), bitmap));

                return null;
            });
        }
    }
}
