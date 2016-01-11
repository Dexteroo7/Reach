package reach.project.core;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.MiscUtils;

//TODO shift to where required
public class DialogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devika_dialog);

        final TextView userName = (TextView) findViewById(R.id.userName);
        final TextView textView1 = (TextView) findViewById(R.id.textView1);
        final TextView exit = (TextView) findViewById(R.id.exit);
        final LinearLayout accept = (LinearLayout) findViewById(R.id.accept);
        final LinearLayout reject = (LinearLayout) findViewById(R.id.reject);
        final int type = getIntent().getIntExtra("type", 0);

        final SimpleDraweeView userImageView = (SimpleDraweeView) findViewById(R.id.userImage);
        userImageView.setController(MiscUtils.getControllerwithResize(userImageView.getController(),
                Uri.parse(StaticData.DROP_BOX_MANAGER), 100, 100));

        reject.setVisibility(View.GONE);
        accept.setVisibility(View.GONE);
        exit.setVisibility(View.VISIBLE);

        exit.setOnClickListener(v -> {
            ReachActivity.openActivity(v.getContext());
            finish();
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
            userName.setText(getIntent().getStringExtra("manual_title"));
            textView1.setText(getIntent().getStringExtra("manual_text"));
        }
    }
}