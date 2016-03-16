package reach.project.ancillaryViews;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import reach.project.R;
import reach.project.music.ReachDatabase;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class SettingsActivity extends AppCompatActivity {

    //private static WeakReference<SettingsActivity> reference;
    private SharedPreferences preferences;

    private final static String [] titles = new String [] {
            "Enable sharing on mobile data",
            "Terms and Conditions",
            "Rate our App"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        preferences = getSharedPreferences("Reach", Context.MODE_PRIVATE);
        //reference = new WeakReference<>(this);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.settingsToolbar);

        mToolbar.setTitle("Settings");
        mToolbar.setNavigationOnClickListener(v -> onBackPressed());

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.settingsRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplication()));
        recyclerView.setAdapter(new SettingsAdapter());

    }

    @Override
    public void onBackPressed() {
        MiscUtils.navigateUp(this);
    }

    private class SettingsHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public final TextView settingsTitle;
        public SwitchCompat switchCompat;

        public SettingsHolder(View itemView) {
            super(itemView);
            this.settingsTitle = (TextView) itemView.findViewById(R.id.settingsTitle);
            this.switchCompat = (SwitchCompat) itemView.findViewById(R.id.settingsSwitch);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (getAdapterPosition()) {
                case 0:
                    switchCompat.toggle();
                    if (switchCompat.isChecked()) {
                        SharedPrefUtils.setDataOn(preferences);
                    }
                    else {
                        SharedPrefUtils.setDataOff(preferences);
                        ////////////////////purge all upload operations, but retain paused operations
                        //TODO
                        getContentResolver().delete(
                                SongProvider.CONTENT_URI,
                                SongHelper.COLUMN_OPERATION_KIND + " = ? and " +
                                        SongHelper.COLUMN_STATUS + " != ?",
                                new String[]{ReachDatabase.OperationKind.UPLOAD_OP.getString(), ReachDatabase.Status.PAUSED_BY_USER.getString()});
                    }
                    break;
                case 1:
                    startActivity(new Intent(SettingsActivity.this, TermsActivity.class));
                    break;
                case 2:
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=reach.project")));
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(SettingsActivity.this, "Play store app not installed", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }

    private class SettingsAdapter extends RecyclerView.Adapter<SettingsHolder> {

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 0 : 1;
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }

        @Override
        public SettingsHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            if (viewType == 0)
                return new SettingsHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.settings_item_toggle, parent, false));
            else
                return new SettingsHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.settings_item, parent, false));
        }

        @Override
        public void onBindViewHolder(SettingsHolder holder, int position) {
            holder.settingsTitle.setText(titles[position]);
            if (position == 0)
                holder.switchCompat.setChecked(SharedPrefUtils.getMobileData(preferences));
        }

        @Override
        public int getItemCount() {
            return titles.length;
        }
    }
}
