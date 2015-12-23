package reach.project.core;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.util.Map;

import reach.project.R;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.CircleTransform;

//TODO shift to where required
public class DialogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push);

        final TextView userName = (TextView) findViewById(R.id.userName);
        final TextView textView1 = (TextView) findViewById(R.id.textView1);
        final TextView exit = (TextView) findViewById(R.id.exit);
        final LinearLayout accept = (LinearLayout) findViewById(R.id.accept);
        final LinearLayout reject = (LinearLayout) findViewById(R.id.reject);
        final int type = getIntent().getIntExtra("type", 0);

        final ImageView userImageView = (ImageView) findViewById(R.id.userImage);
        Picasso.with(userImageView.getContext()).load(Uri.parse(StaticData.dropBoxManager)).transform(new CircleTransform()).fit().centerCrop().into(userImageView);

        reject.setVisibility(View.GONE);
        accept.setVisibility(View.GONE);
        exit.setVisibility(View.VISIBLE);

        exit.setOnClickListener(v -> {
            Intent viewIntent = new Intent(v.getContext(), ReachActivity.class);
            viewIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(viewIntent);
            DialogActivity.this.finish();
        });

        if (type == 1) {
            userName.setText("Hey!");
            textView1.setText("I am Devika from Team Reach! \n" +
                    "Send me an access request by clicking on the lock icon beside my name to view my Music collection. \n" +
                    "Keep Reaching ;)");
        }
        else if (type == 2) {
            userName.setText("Click and Grab!");
            textView1.setText("You can add multiple songs instantly to your Reach Queue by just clicking on the songs here.");
        }
        else if (type == 3) {
            String title = getIntent().getStringExtra("manual_title");
            String subtitle = getIntent().getStringExtra("manual_text");
            userName.setText(title);
            textView1.setText(subtitle);

            SharedPreferences sharedPreferences = getSharedPreferences("Reach",MODE_PRIVATE);

            ((ReachApplication)getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Dialog Opened")
                    .setAction("User Name - " + SharedPrefUtils.getUserName(sharedPreferences))
                    .setLabel("Message - " + title + subtitle)
                    .setValue(1)
                    .build());

            final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
            simpleParams.put(PostParams.USER_ID, SharedPrefUtils.getServerId(sharedPreferences) + "");
            simpleParams.put(PostParams.DEVICE_ID, MiscUtils.getDeviceId(this));
            simpleParams.put(PostParams.OS, MiscUtils.getOsName());
            simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
            simpleParams.put(PostParams.SCREEN_NAME, "dialog");
            simpleParams.put(PostParams.MESSAGE, title + subtitle);
            try {
                simpleParams.put(PostParams.APP_VERSION,
                        getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            try {
                UsageTracker.trackEvent(simpleParams, UsageTracker.DIALOG_OPENED);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}