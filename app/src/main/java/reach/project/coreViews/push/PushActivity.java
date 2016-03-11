package reach.project.coreViews.push;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import java.io.IOException;

import reach.project.R;
import reach.project.coreViews.push.friends.ContactChooserFragment;
import reach.project.utils.StringCompress;



/**
 * Called after the user selects songs in the push.music.MyLibraryFragment
 */
public class PushActivity extends AppCompatActivity implements ContactChooserInterface {

    public static final String PUSH_CONTAINER = "PUSH_CONTAINER";
    public static final String FIRST_CONTENT_NAME = "FIRST_CONTENT_NAME";
    public static final String PUSH_SIZE = "PUSH_SIZE";

    public static void startPushActivity(PushContainer pushContainer, Context context) throws IOException {

        final Intent intent = new Intent(context, PushActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        final String compressedString = StringCompress.compressBytesToString(pushContainer.toByteArray());

        intent.putExtra(PUSH_CONTAINER, compressedString);

        if (TextUtils.isEmpty(pushContainer.firstSongName))
            intent.putExtra(FIRST_CONTENT_NAME, pushContainer.firstAppName);
        else
            intent.putExtra(FIRST_CONTENT_NAME, pushContainer.firstSongName);

        intent.putExtra(PUSH_SIZE, pushContainer.songCount + pushContainer.appCount);

        context.startActivity(intent);
    }

    @Nullable
    private static String pushContainer, firstContentName = null;
    private int contentCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push);

        //get data to be pushed
        pushContainer = getIntent().getStringExtra(PUSH_CONTAINER);
        firstContentName = getIntent().getStringExtra(FIRST_CONTENT_NAME);
        contentCount = getIntent().getIntExtra(PUSH_SIZE, 0);

        //sanity check
        if (TextUtils.isEmpty(pushContainer) || TextUtils.isEmpty(firstContentName) || contentCount == 0) {
            finish();
            return;
        }
        switchToContactChooser();
    }

    //Opens a fragment to select which friends the user wants to share the song with
    @Override
    public void switchToContactChooser() {

        if (TextUtils.isEmpty(pushContainer))
            finish(); //sanity check
        else
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ContactChooserFragment.getInstance(), "contact_chooser").commit();
    }

    //Opens message writing fragment. Called after you have chosen contacts to push
    @Override
    public void switchToMessageWriter(long[] serverIds) {

        if (serverIds == null || serverIds.length == 0)
            finish(); //sanity check
        else
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MessageWriterFragment.getInstance(serverIds, pushContainer, firstContentName, contentCount), "message_writer").commit();
    }
}