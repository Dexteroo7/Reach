package reach.project.coreViews;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;

import reach.backend.entities.userApi.model.ReachFriend;
import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.TextDrawable;
import reach.project.viewHelpers.CircleTransform;
import reach.project.viewHelpers.SlidingTabLayout;
import reach.project.viewHelpers.ViewPagerReusable;

public class UserMusicLibrary extends Fragment {

    private ActionBar actionBar;
    private SlidingTabLayout slidingTabLayout;
    private View rootView;
    private ViewPager viewPager;

    private static WeakReference<UserMusicLibrary> reference = null;
    public static UserMusicLibrary newInstance(long id) {

        final Bundle args;
        UserMusicLibrary fragment;
        if(reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new UserMusicLibrary());
            fragment.setArguments(args = new Bundle());
        }
        else {
            Log.i("Ayush", "Reusing UserMusicLibrary object :)");
            args = fragment.getArguments();
        }
        args.putLong("id", id);
        return fragment;
    }

    @Override
    public void onDestroyView() {

        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setSubtitle("");
        }

        rootView = null;
        viewPager.setAdapter(null);
        viewPager = null;
        slidingTabLayout = null;
        actionBar = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewPager!=null)
            viewPager.setCurrentItem(0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.library_view_pager, container, false);
        viewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
        final long userId = getArguments().getLong("id");
        final Cursor cursor = getActivity().getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                ReachFriendsHelper.projection,
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""}, null);
        if(cursor == null) return rootView;
        if(!cursor.moveToFirst()) {

            cursor.close();
            return rootView;
        }
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        final ReachFriend reachFriend = ReachFriendsHelper.cursorToProcess(cursor);
        if (reachFriend.getPhoneNumber().equals("8860872102")) {
            if (!SharedPrefUtils.getSecondIntroSeen(sharedPreferences)) {
                StaticData.threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bmp = null;
                        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(getActivity());
                        try {
                            bmp = Picasso.with(getActivity())
                                    .load("https://scontent-sin1-1.xx.fbcdn.net/hphotos-xap1/v/t1.0-9/1011255_638449632916744_321328860_n.jpg?oh=5c1daa8d7d015f7ce698ee1793d5a929&oe=55EECF36&dl=1")
                                    .centerCrop()
                                    .resize(96, 96)
                                    .get();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(getActivity(), ReachActivity.class);
                        //intent.putExtra("notifID", 99911);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
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
                    }
                });
                SharedPrefUtils.setSecondIntroSeen(sharedPreferences.edit());
            }
        }
        if(reachFriend == null)
            return rootView;
        actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        if(actionBar!= null) {

            actionBar.setTitle(reachFriend.getUserName());
            actionBar.setSubtitle(reachFriend.getNumberofSongs()+ " Songs");
        }

        final String path;
        if (!TextUtils.isEmpty(reachFriend.getImageId()) && !reachFriend.getImageId().equals("hello_world"))
            path = StaticData.cloudStorageImageBaseUrl + reachFriend.getImageId();
        else
            path = "default";

        new SetIcon(reachFriend.getUserName()).executeOnExecutor(StaticData.threadPool, path);

        viewPager.setAdapter(new ViewPagerReusable(
                getChildFragmentManager(),
                new String[]{"TRACKS", "PLAYLISTS", "ALBUMS", "ARTISTS"},
                new Fragment[]{
                        MusicListFragment.newPagerInstance(userId, "", "", "", 0), // SONGS
                        PlayListListFragment.newInstance(userId), // PLAYLISTS
                        AlbumListFragment.newInstance(userId), // ALBUMS
                        ArtistListFragment.newInstance(userId)})); // ARTISTS

        slidingTabLayout = (SlidingTabLayout) rootView.findViewById(R.id.sliding_tabs);
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return Color.parseColor("#FFCC0000");
            }
        });
        slidingTabLayout.setViewPager(viewPager);

        if (!StaticData.debugMode) {
            ((ReachApplication) getActivity().getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Browsing Library")
                    .setAction("User - " + SharedPrefUtils.getUserName(sharedPreferences))
                    .setAction("Friend - " + userId)
                    .setValue(1)
                    .build());
        }

        return rootView;
    }

    private class SetIcon extends AsyncTask<String, Void, Bitmap> {

        private final int margin = MiscUtils.dpToPx(44);
        private final String name;

        public SetIcon(String userName){
            this.name = userName;
        }

        @Override
        protected Bitmap doInBackground(String... params) {

            Bitmap bmp = null;
            /*TypedArray styledAttributes = getActivity().getTheme().obtainStyledAttributes(
                    new int[] { R.attr.actionBarSize });
            int mActionBarSize = (int) styledAttributes.getDimension(0, 0);
            styledAttributes.recycle();*/
            try {
                if (!params[0].equals("default"))
                    bmp = Picasso.with(rootView.getContext()).load(params[0])
                            .resize(margin, margin)
                            .centerCrop()
                            .transform(new CircleTransform()).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if(actionBar == null || isCancelled() || getActivity() == null || getActivity().isFinishing() || getResources() == null)
                return;
            if (bitmap == null)
                actionBar.setIcon(TextDrawable.builder()
                        .beginConfig()
                        .width(margin)
                        .height(margin)
                        .endConfig()
                        .buildRound(MiscUtils.generateInitials(name), Color.parseColor("#FF56BADA")));
            else
                actionBar.setIcon(new BitmapDrawable(getResources(), bitmap));
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }
}
