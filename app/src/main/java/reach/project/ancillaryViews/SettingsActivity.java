package reach.project.ancillaryViews;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
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
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.utils.SharedPrefUtils;

public class SettingsActivity extends AppCompatActivity {

    //private static WeakReference<SettingsActivity> reference;

    private final static String [] titles = new String [] {
            "Enable sharing on mobile data",
            "Live Help",
            "Terms and Conditions",
            "Rate our App"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //reference = new WeakReference<>(this);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.settingsToolbar);

        mToolbar.setTitle("Settings");
        mToolbar.setNavigationOnClickListener(v -> {
            NavUtils.navigateUpFromSameTask(SettingsActivity.this);
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.settingsRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplication()));
        recyclerView.setAdapter(new SettingsAdapter());

    }

    private class SettingsHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public final TextView settingsTitle;
        public SwitchCompat switchCompat;
        private int position;

        public SettingsHolder(View itemView) {
            super(itemView);
            this.settingsTitle = (TextView) itemView.findViewById(R.id.settingsTitle);
            this.switchCompat = (SwitchCompat) itemView.findViewById(R.id.settingsSwitch);
            itemView.setOnClickListener(this);
        }

        public void bindPosition(int pos) {
            position = pos;
        }

        @Override
        public void onClick(View v) {
            switch (position) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
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
        public SettingsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 0)
                return new SettingsHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.settings_item_toggle, parent, false));
            else
                return new SettingsHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.settings_item, parent, false));
        }

        @Override
        public void onBindViewHolder(SettingsHolder holder, int position) {
            holder.settingsTitle.setText(titles[position]);
            holder.bindPosition(position);
            if (position == 0) {
                SharedPreferences preferences = getSharedPreferences("Reach", Context.MODE_APPEND);
                if (SharedPrefUtils.getMobileData(preferences))
                    holder.switchCompat.setChecked(true);
                else
                    holder.switchCompat.setChecked(false);

                holder.switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {

                    if (isChecked)
                        SharedPrefUtils.setDataOn(preferences);
                    else {
                        SharedPrefUtils.setDataOff(preferences);
                        ////////////////////purge all upload operations, but retain paused operations
                        //TODO
                        getContentResolver().delete(
                                ReachDatabaseProvider.CONTENT_URI,
                                ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                                        ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                                new String[]{"1", ReachDatabase.PAUSED_BY_USER + ""});
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return titles.length;
        }
    }
}
