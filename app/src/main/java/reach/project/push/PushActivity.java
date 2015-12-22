package reach.project.push;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;

import java.util.List;

import reach.project.R;

public class PushActivity extends AppCompatActivity implements ContactChooserInterface {

    public static final String PUSH_CONTAINER = "push_container";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push);

        //get data to be pushed
        final String pushContainer = getIntent().getStringExtra(PUSH_CONTAINER);
        if (TextUtils.isEmpty(pushContainer)) {

            finish();
            return;
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void switchToContactChooser() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, ContactChooserFragment.getInstance(), "contact_chooser").commit();
    }

    @Override
    public void switchToMessageWriter(List<Long> serverIds) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, ContactChooserFragment.getInstance(), "message_writer").commit();
    }
}