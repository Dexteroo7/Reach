package reach.project.push;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;

import java.io.IOException;

import reach.project.R;
import reach.project.utils.StringCompress;

public class PushActivity extends AppCompatActivity implements ContactChooserInterface {

    public static final String PUSH_CONTAINER = "push_container";
    public static final String FIRST_SONG_NAME = "first_song_name";
    public static final String PUSH_SIZE = "push_size";

    public static Intent getPushActivityIntent(PushContainer pushContainer, Context context) throws IOException {

        final Intent intent = new Intent(context, PushActivity.class);
        final String compressedString = StringCompress.compressBytesToString(pushContainer.toByteArray());

        intent.putExtra(PUSH_CONTAINER, compressedString);
        intent.putExtra(FIRST_SONG_NAME, pushContainer.firstSongName);
        intent.putExtra(PUSH_SIZE, pushContainer.songCount);

        return intent;
    }

    @Nullable
    private static String pushContainer, firstSongName = null;
    private int songCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push);

        //get data to be pushed
        pushContainer = getIntent().getStringExtra(PUSH_CONTAINER);
        firstSongName = getIntent().getStringExtra(FIRST_SONG_NAME);
        songCount = getIntent().getIntExtra(PUSH_SIZE, 0);

        //sanity check
        if (TextUtils.isEmpty(pushContainer) || TextUtils.isEmpty(firstSongName) || songCount == 0) {
            finish();
            return;
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void switchToContactChooser() {

        if (TextUtils.isEmpty(pushContainer))
            finish(); //sanity check
        else
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ContactChooserFragment.getInstance(), "contact_chooser").commit();
    }

    @Override
    public void switchToMessageWriter(long[] serverIds) {

        if (serverIds == null || serverIds.length == 0 || TextUtils.isEmpty(pushContainer) || TextUtils.isEmpty(firstSongName) || songCount == 0)
            finish(); //sanity check
        else
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MessageWriterFragment.getInstance(serverIds, pushContainer, firstSongName, songCount), "message_writer").commit();
    }
}