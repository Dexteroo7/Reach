package reach.project.coreViews.yourProfile;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;

public class ProfileActivity extends AppCompatActivity {

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

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.editProfileToolbar);

        mToolbar.setTitle("");
        mToolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(ProfileActivity.this));

        final RelativeLayout headerRoot = (RelativeLayout) findViewById(R.id.headerRoot);
        final TextView userName = (TextView) headerRoot.findViewById(R.id.userName);
        final TextView musicCount = (TextView) headerRoot.findViewById(R.id.musicCount);
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
                        ReachFriendsHelper.COLUMN_STATUS},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""}, null);

        if (cursor != null) {

            cursor.moveToFirst();
            final String uName = cursor.getString(0);
            final int numberOfSongs = cursor.getInt(1);
            final int status = cursor.getInt(3);
            final String imageId = cursor.getString(2);

            userName.setText(uName);
            musicCount.setText(numberOfSongs + "");
            userHandle.setText("@" + uName.toLowerCase().split(" ")[0]);
            profilePic.setImageURI(Uri.parse(StaticData.cloudStorageImageBaseUrl + imageId));
            if (status == ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED)
                setRequestSent();
            else
                sendButton.setOnClickListener(v -> setRequestSent());
            cursor.close();
        }
    }

    private void setRequestSent() {
        ImageView profileIcon = (ImageView) findViewById(R.id.profileIcon);
        TextView  text1 = (TextView) findViewById(R.id.text1);
        TextView text2 = (TextView) findViewById(R.id.text2);

        profileIcon.setImageResource(R.drawable.icon_pending_invite);
        text1.setText("Looks like the user has not accepted your request yet");
        text2.setText("Friend request pending");
        sendButton.setText("CANCEL FRIEND REQUEST");
        sendButton.setOnClickListener(v -> {

        });
    }
}
