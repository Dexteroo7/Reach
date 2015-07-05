package reach.project.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.squareup.picasso.Picasso;

import reach.project.R;
import reach.project.utils.SharedPrefUtils;
import reach.project.viewHelpers.CircleTransform;

public class PushActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push);

        final String userImage = getIntent().getStringExtra("user_image");
        final String customMessage = getIntent().getStringExtra("custom_message");
        final short song_count = getIntent().getShortExtra("song_count", (short) 0);
        final long receiverId = getIntent().getLongExtra("receiver_id", 0);
        final long senderId = getIntent().getLongExtra("sender_id", 0);
        final String cMessage = getIntent().getStringExtra("custom_message");
        final Intent pushAddSong = new Intent(this, ReachActivity.class);

        if(song_count == 0 || receiverId == 0 || senderId == 0) {
            finish();
            return;
        }

        ((TextView)findViewById(R.id.userName)).setText(getIntent().getStringExtra("user_name"));
        String fSong = getIntent().getStringExtra("first_song");
        if (fSong.length()>20)
            fSong = fSong.substring(0,20)+"...";

        String text;
        if (cMessage!=null&&cMessage.length()>0)
            text = cMessage + ". Start listening to ";
        else
            text = "wants you to listen to ";
        text = text + "<font color=\"#F33B5B\"><b>" + fSong + "</b></font>";
        if(song_count > 1)
            text = text + " and <font color=\"#F33B5B\"><b>"+ String.valueOf(song_count-1) + "</b></font> other songs";
        ((TextView)findViewById(R.id.textView1)).setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
        if (!(TextUtils.isEmpty(userImage) || userImage.equals("hello_world")))
            Picasso.with(this).load(StaticData.cloudStorageImageBaseUrl + userImage).transform(new CircleTransform()).into((ImageView) findViewById(R.id.userImage));

        findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!StaticData.debugMode) {
                    ((ReachApplication)getApplication()).getTracker().send(new HitBuilders.EventBuilder()
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
                Bundle bundle = new Bundle();
                bundle.putBoolean("oP",true);
                pushAddSong.putExtras(bundle);
                //start the Activity
                startActivity(pushAddSong);
                finish();
            }
        });
        findViewById(R.id.reject).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}