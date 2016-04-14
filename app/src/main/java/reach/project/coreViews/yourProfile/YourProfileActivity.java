package reach.project.coreViews.yourProfile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import reach.project.R;
import reach.project.ancillaryViews.SettingsActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.coreViews.yourProfile.apps.YourProfileAppFragment;
import reach.project.coreViews.yourProfile.music.YourProfileMusicFragment;
import reach.project.notificationCentre.NotificationActivity;
import reach.project.player.PlayerActivity;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ancillaryClasses.SuperInterface;

//TODO: Currently this is opened from notifications, but this will not play songs
// If a friend is added, then this activity is displayed
public class YourProfileActivity extends AppCompatActivity{

    private static final String OPEN_MY_PROFILE_APPS = "OPEN_MY_PROFILE_APPS";
    private static final String OPEN_MY_PROFILE_SONGS = "OPEN_MY_PROFILE_SONGS";
    private static final String TAG = YourProfileActivity.class.getSimpleName();

    private SharedPreferences sharedPreferences;

    public static void openProfile(long userId, Context context) {

        final Intent intent = new Intent(context, YourProfileActivity.class);
        //intent.setAction(OPEN_PROFILE);
        intent.putExtra("userId", userId);

        context.startActivity(intent);
    }

    public static void openProfileWithPlayer(long userId, Context context, int time, String ytId) {

        final Intent intent = new Intent(context, YourProfileActivity.class);
        //intent.setAction(OPEN_PROFILE);
        intent.putExtra("userId", userId);
        intent.putExtra("time", time);
        intent.putExtra("ytId", ytId);

        context.startActivity(intent);
    }

    private static WeakReference<YourProfileActivity> reference = null;

    /*private YouTubePlayer player = null;
    private YouTubePlayerSupportFragment ytFragment;
    private LinearLayout ytLayout;
    private String currentYTId;*/

    @Override
    public void onBackPressed() {
        //MiscUtils.navigateUpWithPlayer(this, player.getCurrentTimeMillis(), currentYTId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_profile);

        reference = new WeakReference<>(this);

        sharedPreferences = getSharedPreferences("Reach", MODE_PRIVATE);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setTitle("Profile");
            toolbar.setTitleTextColor(Color.WHITE);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: " + "Inside navigation click listener" );
                onBackPressed();
            }
        });

        toolbar.inflateMenu(R.menu.yourprofile_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                /*case R.id.player_button:
                    PlayerActivity.openActivity(this);
                    return true;*/
                case R.id.notif_button:
                    NotificationActivity.openActivity(this, NotificationActivity.OPEN_NOTIFICATIONS);
                    return true;
                case R.id.settings_button:
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                default:
                    return false;
            }
        });


        final Intent intent = getIntent();
        final long userId = intent.getLongExtra("userId", 0L);
        if (userId == 0) {
            finish();
            return;
        }


        if(savedInstanceState == null) {

            YourProfileMusicFragment frag = YourProfileMusicFragment.newInstance(userId);
            /*CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ;
            params.setBehavior( null );*/
            getSupportFragmentManager().beginTransaction().replace(R.id.your_profile_music_frag_container, frag).commit();


        }
        final Cursor cursor = getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                new String[]{ReachFriendsHelper.COLUMN_PHONE_NUMBER,
                        ReachFriendsHelper.COLUMN_USER_NAME,
                        ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS,
                        ReachFriendsHelper.COLUMN_IMAGE_ID,
                        ReachFriendsHelper.COLUMN_NETWORK_TYPE,
                        ReachFriendsHelper.COLUMN_STATUS,
                        ReachFriendsHelper.COLUMN_NUMBER_OF_APPS,
                        ReachFriendsHelper.COLUMN_COVER_PIC_ID},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""}, null);
        int numberOfSongs = 0;

        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {

            final String uName = cursor.getString(1);
            numberOfSongs = cursor.getInt(2);
            //final int numberOfApps = cursor.getInt(6);
            final String imageId = cursor.getString(3);
            /*final short status = cursor.getShort(5);
            if (status == ReachFriendsHelper.Status.OFFLINE_REQUEST_GRANTED.getValue())
                Snackbar.make(findViewById(R.id.root_layout), uName + " is currently offline. You will be able to transfer music when the user comes online.", Snackbar.LENGTH_INDEFINITE).show();
            if (userId != StaticData.DEVIKA) {
                final View rootView = findViewById(R.id.root_layout);
                if (status == ReachFriendsHelper.UPLOADS_DISABLED)
                    Snackbar.make(rootView, uName + " has disabled uploads. You will be only be able to transfer music when the user enables it", Snackbar.LENGTH_INDEFINITE).show();
                else if (status == ReachFriendsHelper.OFFLINE_REQUEST_GRANTED)
                    Snackbar.make(rootView, uName + " is currently offline. You will be able to transfer music when the user comes online.", Snackbar.LENGTH_INDEFINITE).show();
            }*/

            //final RelativeLayout headerRoot = (RelativeLayout) materialViewPager.findViewById(R.id.headerRoot);
            final TextView userName = (TextView) findViewById(R.id.userName);
            final TextView musicCount = (TextView) findViewById(R.id.musicCount);
            //final TextView appCount = (TextView) findViewById(R.id.appCount);
            final TextView userHandle = (TextView) findViewById(R.id.userHandle);
            final SimpleDraweeView profilePic = (SimpleDraweeView) findViewById(R.id.profilePic);
            final SimpleDraweeView coverPic = (SimpleDraweeView) findViewById(R.id.coverPic);

            userName.setText(uName);
            musicCount.setText(numberOfSongs + "");
            //appCount.setText(numberOfApps + "");
            userHandle.setText("@" + uName.toLowerCase().split(" ")[0]);
            profilePic.setController(MiscUtils.getControllerResize(profilePic.getController(),
                    Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + imageId), 100, 100));
            coverPic.setController(MiscUtils.getControllerResize(coverPic.getController(),
                    Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + cursor.getString(7)), 500, 300));
//            coverPic.setController(MiscUtils.getControllerResize(coverPic.getController(),
//                    Uri.parse(MiscUtils.getRandomPic()), 500, 500));

            cursor.close();
        }

        /*final int finalNumberOfSongs = numberOfSongs;
        final int time = intent.getIntExtra("time", 0);
        final String ytId = intent.getStringExtra("ytId");*/

        /*ytLayout = (LinearLayout) findViewById(R.id.ytLayout);
        ImageView ytCloseBtn = (ImageView) findViewById(R.id.ytCloseBtn);

        ytFragment = (YouTubePlayerSupportFragment) getSupportFragmentManager().findFragmentById(R.id.video_fragment_container);
        ytFragment.initialize("AIzaSyAYH8mcrHrqG7HJwjyGUuwxMeV7tZP6nmY", new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                player = youTubePlayer;
                player.setShowFullscreenButton(false);
                player.setPlaybackEventListener(new YouTubePlayer.PlaybackEventListener() {
                    @Override
                    public void onPlaying() {
                        ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                .setCategory("Play song")
                                .setAction("User Name - " + SharedPrefUtils.getUserName(sharedPreferences))
                                .setLabel("YOUTUBE - FRIEND PROFILE")
                                .setValue(1)
                                .build());
                    }

                    @Override
                    public void onPaused() {

                    }

                    @Override
                    public void onStopped() {

                    }

                    @Override
                    public void onBuffering(boolean b) {

                    }

                    @Override
                    public void onSeekTo(int i) {

                    }
                });
                //player.setPlayerStyle(YouTubePlayer.PlayerStyle.MINIMAL);
                //player.cueVideo("CuH3tJPiP-U");

                if (TextUtils.isEmpty(ytId) || time == 0) {
                    if (isFinishing())
                        return;
                    try {
                        getSupportFragmentManager().beginTransaction().hide(ytFragment).commit();
                    } catch (IllegalStateException ignored) {
                    }
                    ytLayout.setVisibility(View.GONE);
                }
                else {
                    ytLayout.setVisibility(View.VISIBLE);
                    if (isFinishing())
                        return;
                    try {
                        getSupportFragmentManager().beginTransaction().show(ytFragment).commit();
                    } catch (IllegalStateException ignored) {
                    }
                    player.loadVideo(ytId, time);
                    currentYTId = ytId;
                }

                if (ytCloseBtn != null)
                    ytCloseBtn.setOnClickListener(v -> {
                        ytLayout.setVisibility(View.GONE);
                        if (isFinishing())
                            return;
                        try {
                            getSupportFragmentManager().beginTransaction().hide(ytFragment).commit();
                        } catch (IllegalStateException ignored) {
                        }
                        player.pause();
                    });
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

            }
        });*/

        final String action = intent.getAction();
        if (TextUtils.isEmpty(action))
            return;


    }

    /*private static class YTTest extends AsyncTask<String, Void, SearchResult> {
        @Override
        protected SearchResult doInBackground(String... params) {
            try {
                final HttpTransport transport = new NetHttpTransport();
                final JsonFactory factory = new JacksonFactory();
                final HttpRequestInitializer initialize = request -> {
                    request.setConnectTimeout(request.getConnectTimeout() * 2);
                    request.setReadTimeout(request.getReadTimeout() * 2);
                };
                final YouTube youTube = new YouTube.Builder(transport, factory, initialize).build();
                // Define the API request for retrieving search results.
                final YouTube.Search.List search = youTube.search().list("id");

                // Set your developer key from the Google Developers Console for
                // non-authenticated requests. See:
                // https://console.developers.google.com/
                final String apiKey = "AIzaSyAYH8mcrHrqG7HJwjyGUuwxMeV7tZP6nmY";
                search.setKey(apiKey);

                search.setQ(params[0]);

                // Restrict the search results to only include videos. See:
                // https://developers.google.com/youtube/v3/docs/search/list#type
                search.setType("video");

                search.setVideoCategoryId("10");

                // To increase efficiency, only retrieve the fields that the
                // application uses.
                search.setFields("items/id/videoId");
                search.setMaxResults(1L);

                // Call the API and print results.
                final SearchListResponse searchResponse = search.execute();
                final List<SearchResult> searchResultList = searchResponse.getItems();
                *//*final StringBuilder stringBuilder = new StringBuilder();
                for (SearchResult searchResult : searchResultList)
                    stringBuilder.append(searchResult.getSnippet().getTitle()).append("\n\n");
                return stringBuilder.toString();*//*
                if (searchResultList == null || searchResultList.isEmpty())
                    return null;
                return searchResultList.get(0);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(SearchResult searchResult) {
            super.onPostExecute(searchResult);
            *//*MiscUtils.useContextFromFragment(reference, activity -> {
                new AlertDialog.Builder(activity).setMessage(s).setTitle("Youtube").create().show();
            });*//*
            if (searchResult == null)
                return;
            MiscUtils.useActivity(reference, activity -> {
                if (activity.ytLayout.getVisibility() != View.VISIBLE)
                    activity.ytLayout.setVisibility(View.VISIBLE);
                if (activity.ytFragment.isHidden()) {
                    if (activity.isFinishing())
                        return;
                    try {
                        activity.getSupportFragmentManager().beginTransaction().show(activity.ytFragment).commit();
                    } catch (IllegalStateException ignored) {
                    }
                }
                activity.currentYTId = searchResult.getId().getVideoId();
                activity.player.loadVideo(activity.currentYTId);
            });
        }
    }*/

    private String fastSanitize(String str) {

        final StringBuilder stringBuilder = new StringBuilder();

        str = replace(str, "MP3Khan", "", -1, stringBuilder);
        str = replace(str, "_Full-HD", "", -1, stringBuilder);
        str = replace(str, "songsweb", "", -1, stringBuilder);
        str = replace(str, "www.", "", -1, stringBuilder);
        str = replace(str, ".com", "", -1, stringBuilder);

        str = replace(str, ".Mobi", "", -1, stringBuilder);
        str = replace(str, ".mobi", "", -1, stringBuilder);
        str = replace(str, "[]", "", -1, stringBuilder);
        str = replace(str, "pagalworld", "", -1, stringBuilder);
        str = replace(str, "DownloadMing", "", -1, stringBuilder);
        str = replace(str, "  ", "", -1, stringBuilder);
        str = replace(str, "skymaza", "", -1, stringBuilder);
        str = replace(str, "DjGol", "", -1, stringBuilder);
        str = replace(str, "<unknown>", "", -1, stringBuilder);
        str = replace(str, "DJBoss", "", -1, stringBuilder);
        str = replace(str, "iPendu", "", -1, stringBuilder);
        str = replace(str, "SongPK", "", -1, stringBuilder);
        str = replace(str, "Songspk", "", -1, stringBuilder);
        str = replace(str, "DJJOhAL", "", -1, stringBuilder);
        str = replace(str, "Mobway", "", -1, stringBuilder);
        str = replace(str, "downloadming", "", -1, stringBuilder);
        str = replace(str, "DjPunjab", "", -1, stringBuilder);
        str = replace(str, "Bestwap", "", -1, stringBuilder);
        str = replace(str, "MyMp3Song", "", -1, stringBuilder);
        str = replace(str, "PagalWorld", "", -1, stringBuilder);
        str = replace(str, "KrazyWAP", "", -1, stringBuilder);
        str = replace(str, "lebewafa", "", -1, stringBuilder);
        str = replace(str, "Mp3Singer", "", -1, stringBuilder);
        str = replace(str, "Songspk", "", -1, stringBuilder);
        str = replace(str, "Mr-Jatt", "", -1, stringBuilder);
        str = replace(str, "MastiCity", "", -1, stringBuilder);
        str = replace(str, "finewap", "", -1, stringBuilder);
        str = replace(str, "hotmentos", "", -1, stringBuilder);
        str = replace(str, "MirchiFun", "", -1, stringBuilder);
        str = replace(str, "MyMp3Singer", "", -1, stringBuilder);
        str = replace(str, "FreshMaZa", "", -1, stringBuilder);
        str = replace(str, ".songs", "", -1, stringBuilder);
        str = replace(str, "SongsLover", "", -1, stringBuilder);
        str = replace(str, "Mixmp3", "", -1, stringBuilder);
        str = replace(str, "wapking", "", -1, stringBuilder);
        str = replace(str, "BDLovE24", "", -1, stringBuilder);
        str = replace(str, "DJMaza", "", -1, stringBuilder);
        str = replace(str, "RoyalJatt", "", -1, stringBuilder);
        str = replace(str, "SongPK", "", -1, stringBuilder);
        str = replace(str, "KrazyWap", "", -1, stringBuilder);
        str = replace(str, ".link", "", -1, stringBuilder);
        str = replace(str, "MobMaza", "", -1, stringBuilder);
        str = replace(str, "Mobway", "", -1, stringBuilder);
        str = replace(str, "youtube", "", -1, stringBuilder);
        str = replace(str, "MP3Juices", "", -1, stringBuilder);

        str = replace(str, "+", "", -1, stringBuilder);
        str = replace(str, ".name", "", -1, stringBuilder);
        str = replace(str, "^0[1-9] ", "", -1, stringBuilder);
        str = replace(str, ".pk", "", -1, stringBuilder);
        str = replace(str, ".in", "", -1, stringBuilder);
        str = replace(str, "-", "", -1, stringBuilder);
        str = replace(str, ".Com", "", -1, stringBuilder);
        str = replace(str, ".net", "", -1, stringBuilder);
        str = replace(str, ".", "", -1, stringBuilder);
        str = replace(str, ":", "", -1, stringBuilder);
        str = replace(str, ".fm", "", -1, stringBuilder);
        str = replace(str, "_", "", -1, stringBuilder);
        str = replace(str, ".In", "", -1, stringBuilder);
        str = replace(str, ".Net", "", -1, stringBuilder);
        str = replace(str, "()", "", -1, stringBuilder);


        return str;
    }

    private String replace(final String text, final String searchString, final String replacement, int max, StringBuilder stringBuilder) {

        stringBuilder.setLength(0);
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(searchString) || replacement == null || max == 0) {
            return text;
        }

        int start = 0;
        int end = text.indexOf(searchString, start);
        if (end < 0) {
            return text;
        }
        final int replLength = searchString.length();
        int increase = replacement.length() - replLength;
        increase = increase < 0 ? 0 : increase;
        increase *= max < 0 ? 16 : max > 64 ? 64 : max;

        stringBuilder.ensureCapacity(text.length() + increase);
        while (end > 0) {
            stringBuilder.append(text.substring(start, end)).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        stringBuilder.append(text.substring(start));
        return stringBuilder.toString();
    }
}