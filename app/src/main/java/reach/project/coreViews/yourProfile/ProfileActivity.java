package reach.project.coreViews.yourProfile;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.base.Optional;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class ProfileActivity extends AppCompatActivity {

    private static WeakReference<ProfileActivity> reference = null;

    public static void openProfile(long userId, Context context) {
        final Intent intent = new Intent(context, ProfileActivity.class);
        intent.putExtra("userId", userId);
        context.startActivity(intent);
    }
    private TextView sendButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        reference = new WeakReference<>(this);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.editProfileToolbar);

        mToolbar.setTitle("");
        mToolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(ProfileActivity.this));

        final RelativeLayout headerRoot = (RelativeLayout) findViewById(R.id.headerRoot);
        final TextView userName = (TextView) headerRoot.findViewById(R.id.userName);
        final TextView musicCount = (TextView) headerRoot.findViewById(R.id.musicCount);
        final TextView appCount = (TextView) headerRoot.findViewById(R.id.appCount);
        final TextView userHandle = (TextView) headerRoot.findViewById(R.id.userHandle);
        final SimpleDraweeView profilePic = (SimpleDraweeView) headerRoot.findViewById(R.id.profilePic);
        sendButton = (TextView) findViewById(R.id.sendButton);

        final Intent intent = getIntent();
        final long userId = intent.getLongExtra("userId", 0L);
        if (userId == 0) {
            finish();
            return;
        }

        final Cursor cursor = getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                new String[]{ReachFriendsHelper.COLUMN_USER_NAME,
                        ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS,
                        ReachFriendsHelper.COLUMN_IMAGE_ID,
                        ReachFriendsHelper.COLUMN_STATUS,
                        ReachFriendsHelper.COLUMN_NUMBER_OF_APPS},
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""}, null);

        if (cursor != null) {

            cursor.moveToFirst();
            final String uName = cursor.getString(0);
            final int numberOfSongs = cursor.getInt(1);
            final int status = cursor.getInt(3);
            final String imageId = cursor.getString(2);
            final int numberOfApps = cursor.getInt(4);

            userName.setText(uName);
            musicCount.setText(numberOfSongs + "");
            appCount.setText(numberOfApps + "");
            userHandle.setText("@" + uName.toLowerCase().split(" ")[0]);
            profilePic.setImageURI(Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + imageId));
            if (status == ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED)
                setRequestSent();
            else
                sendButton.setOnClickListener(v -> {
                    ProfileActivity.this.setRequestSent();
                    new SendRequest().executeOnExecutor(
                            StaticData.TEMPORARY_FIX,
                            userId, SharedPrefUtils.getServerId(getSharedPreferences("Reach",
                                    MODE_PRIVATE)), (long) status);

                    //Toast.makeText(getActivity(), "Access Request sent", Toast.LENGTH_SHORT).show();
                    final ContentValues values = new ContentValues();
                    values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED);
                    getContentResolver().update(
                            Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                            values,
                            ReachFriendsHelper.COLUMN_ID + " = ?",
                            new String[]{userId + ""});
                });
            cursor.close();
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (reference != null)
            reference.clear();
        reference = null;
    }

    private void setRequestSent() {

        ImageView profileIcon = (ImageView) findViewById(R.id.profileIcon);
        TextView  text1 = (TextView) findViewById(R.id.text1);
        TextView text2 = (TextView) findViewById(R.id.text2);

        profileIcon.setImageResource(R.drawable.icon_pending_invite);
        text1.setText("Looks like the user has not accepted your request yet");
        text2.setText("Friend request pending");
        sendButton.setText("CANCEL FRIEND REQUEST");
    }

    private static final class SendRequest extends AsyncTask<Long, Void, Long> {

        @Override
        protected Long doInBackground(final Long... params) {

            /**
             * params[0] = other id
             * params[1] = my id
             * params[2] = status
             */

            final reach.backend.entities.messaging.model.MyString dataAfterWork = MiscUtils.autoRetry(
                    () -> StaticData.MESSAGING_API.requestAccess(params[1], params[0]).execute(),
                    Optional.of(input -> (input == null || TextUtils.isEmpty(input.getString()) || input.getString().equals("false")))).orNull();

            final String toParse;
            if (dataAfterWork == null || TextUtils.isEmpty(toParse = dataAfterWork.getString()) || toParse.equals("false"))
                return params[0];
            return null;
        }

        @Override
        protected void onPostExecute(final Long response) {

            super.onPostExecute(response);

            if (response != null && response > 0) {

                //response becomes the id of failed person
                MiscUtils.useActivity(reference, context -> {

                    Toast.makeText(context, "Request Failed", Toast.LENGTH_SHORT).show();
                    final ContentValues values = new ContentValues();
                    values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_NOT_SENT);
                    context.getContentResolver().update(
                            Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + response),
                            values,
                            ReachFriendsHelper.COLUMN_ID + " = ?",
                            new String[]{response + ""});
                });
            }

        }
    }
}
