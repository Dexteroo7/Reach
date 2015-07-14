package reach.project.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.squareup.picasso.Picasso;

import java.io.IOException;

import reach.project.R;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.viewHelpers.CircleTransform;

public class PushActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push);

        final TextView userName = (TextView) findViewById(R.id.userName);
        final TextView textView1 = (TextView) findViewById(R.id.textView1);
        final TextView exit = (TextView) findViewById(R.id.exit);
        final LinearLayout accept = (LinearLayout) findViewById(R.id.accept);
        final LinearLayout reject = (LinearLayout) findViewById(R.id.reject);
        final ImageView userImageView = (ImageView) findViewById(R.id.userImage);
        final int type = getIntent().getIntExtra("type",0);
        if (type == 0) {
            final String userImage = getIntent().getStringExtra("user_image");
            final short song_count = getIntent().getShortExtra("song_count", (short) 0);
            final long receiverId = getIntent().getLongExtra("receiver_id", 0);
            final long senderId = getIntent().getLongExtra("sender_id", 0);
            final int hashCode = getIntent().getIntExtra("hash", 0);
            final String cMessage = getIntent().getStringExtra("custom_message");
            final Intent pushAddSong = new Intent(this, ReachActivity.class);

            if (song_count == 0 || receiverId == 0 || senderId == 0) {
                finish();
                return;
            }

            userName.setText(getIntent().getStringExtra("user_name"));
            String fSong = getIntent().getStringExtra("first_song");
            if (fSong.length() > 20)
                fSong = fSong.substring(0, 20) + "...";

            final String firstSongName = fSong;
            String text;
            if (cMessage != null && cMessage.length() > 0)
                text = cMessage + ". Start listening to ";
            else
                text = "wants you to listen to ";
            text = text + "<font color=\"#F33B5B\"><b>" + fSong + "</b></font>";
            if (song_count > 1)
                text = text + " and <font color=\"#F33B5B\"><b>" + String.valueOf(song_count - 1) + "</b></font> other songs";
            textView1.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
            if (!(TextUtils.isEmpty(userImage) || userImage.equals("hello_world")))
                Picasso.with(this).load(StaticData.cloudStorageImageBaseUrl + userImage).transform(new CircleTransform()).into(userImageView);

            accept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    MiscUtils.autoRetryAsync(new DoWork<Void>() {
                        @Override
                        protected Void doWork() throws IOException {
                            return StaticData.notificationApi.pushAccepted(firstSongName,
                                    hashCode,
                                    receiverId,
                                    senderId,
                                    (int) song_count).execute();
                        }
                    }, Optional.<Predicate<Void>>absent());

                    if (!StaticData.debugMode) {
                        ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                .setCategory("Accept - Pushed song")
                                .setAction("User - " + SharedPrefUtils.getServerId(getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                                .setAction("User Name - " + SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                                .setLabel("Sender - " + getIntent().getStringExtra("user_name") + ", Songs - " + song_count)
                                .setValue(1)
                                .build());
                    }
                    pushAddSong.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    pushAddSong.setAction("process_multiple");
                    pushAddSong.putExtra("data", getIntent().getStringExtra("data"));
                    //start the Activity
                    startActivity(pushAddSong);
                    finish();
                }
            });
            reject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
        else {

            final int px = MiscUtils.dpToPx(50);
            Picasso.with(PushActivity.this)
                    .load("https://scontent-sin1-1.xx.fbcdn.net/hphotos-xap1/v/t1.0-9/1011255_638449632916744_321328860_n.jpg?oh=5c1daa8d7d015f7ce698ee1793d5a929&oe=55EECF36&dl=1")
                    .centerCrop()
                    .resize(px, px)
                    .transform(new CircleTransform())
                    .into(userImageView);
            reject.setVisibility(View.GONE);
            accept.setVisibility(View.GONE);
            exit.setVisibility(View.VISIBLE);
            exit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            if (type == 1) {
                userName.setText("Hey!");
                textView1.setText("I am Devika from Team Reach! \n" +
                        "Send me an access request by clicking on the lock icon beside my name to view my music collection. \n" +
                        "Keep Reaching ;)");
            }
            else if (type == 2) {
                userName.setText("Click and Grab!");
                textView1.setText("You can add multiple songs instantly to your Reach Queue by just clicking on the songs here.");
            }
        }
    }
}