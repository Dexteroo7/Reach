package reach.project.core;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.squareup.picasso.Picasso;

import java.io.IOException;

import reach.backend.entities.messaging.model.MyString;
import reach.project.R;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.viewHelpers.CircleTransform;

public class ReachNotificationActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reach_notification);
        final Bundle bundle = getIntent().getExtras();
        final long myId = SharedPrefUtils.getServerId(getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
        final String imageId = bundle.getString("image_id");

        ((TextView)findViewById(R.id.userName)).setText(bundle.getString("user_name"));
        ((TextView)findViewById(R.id.userMusic)).setText(bundle.getLong("number_of_songs") + "");
        if (!(TextUtils.isEmpty(imageId) || imageId.equals("hello_world")))
            Picasso.with(this).load(StaticData.cloudStorageImageBaseUrl + imageId).transform(new CircleTransform()).into((ImageView) findViewById(R.id.userImage));

        final HandleReply handleReply = new HandleReply(bundle.getInt("notification_id"), NotificationManagerCompat.from(this), this);
        findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleReply.execute(myId + "", bundle.getLong("host_id") + "", "PERMISSION_GRANTED");
            }
        });
        findViewById(R.id.reject).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleReply.execute(myId + "", bundle.getLong("host_id") + "", "PERMISSION_REJECTED");
            }
        });
    }

    private final class HandleReply extends AsyncTask<String, Void, Boolean> {

        private final int notificationId;
        private final NotificationManagerCompat managerCompat;
        private final Context context;

        private HandleReply(int notificationId, NotificationManagerCompat managerCompat, Context context) {
            this.notificationId = notificationId;
            this.managerCompat = managerCompat;
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(String... params) {

            final long clientId = Long.parseLong(params[0]);
            final long hostId = Long.parseLong(params[1]);
            final String message = params[2];
            final MyString myString = MiscUtils.autoRetry(new DoWork<MyString>() {
                @Override
                protected MyString doWork() throws IOException {

                    StaticData.notificationApi.addBecameFriends(clientId, hostId).execute();

                    return StaticData.messagingEndpoint.messagingEndpoint().handleReply(clientId, hostId, message).execute();
                }
            }, Optional.<Predicate<MyString>>of(new Predicate<MyString>() {
                @Override
                public boolean apply(MyString input) {
                    return (input == null || TextUtils.isEmpty(input.getString()) || input.getString().equals("false"));
                }
            })).orNull();
            if(myString == null || TextUtils.isEmpty(myString.getString()) || myString.getString().equals("false"))
                return false;
            else if(message.equals("PERMISSION_GRANTED") && getContentResolver() != null) {

                final ContentValues values = new ContentValues();
                values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.ONLINE_REQUEST_GRANTED);
                getContentResolver().update(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + hostId),
                        values,
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{hostId + ""});
            }
            return true;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            managerCompat.cancel(notificationId);
            finish();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {

            super.onPostExecute(aBoolean);
            if(isCancelled() || isFinishing() || context == null)
                return;
            if(!aBoolean)
                Toast.makeText(context, "Network Error on reply", Toast.LENGTH_SHORT).show();
        }
    }

}
