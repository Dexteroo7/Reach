package reach.project.core;

import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.backend.notifications.notificationApi.model.NotificationBase;
import reach.project.R;
import reach.project.adapter.ReachNotificationAdapter;
import reach.project.coreViews.FriendRequestFragment;
import reach.project.coreViews.NotificationFragment;
import reach.project.database.contentProvider.ReachNotificationsProvider;
import reach.project.database.sql.ReachNotificationsHelper;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.viewHelpers.SlidingTabLayout;
import reach.project.viewHelpers.ViewPagerReusable;


public class NotificationActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    private ReachNotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle("Notifications");
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new ViewPagerReusable(
                getSupportFragmentManager(),
                new String[]{"Friend Requests", "Other Notifications"},
                new Fragment[]{
                        FriendRequestFragment.newInstance(),
                        NotificationFragment.newInstance()}));

        SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return Color.parseColor("#FFCC0000");
            }
        });

        adapter = new ReachNotificationAdapter(this, R.layout.notification_item, null, 0);

        slidingTabLayout.setViewPager(viewPager);
        getLoaderManager().initLoader(0, null, this);
        refreshing.set(true);
        new NotificationSync().executeOnExecutor(StaticData.threadPool);
    }

    @Override
    protected void onDestroy() {

        getLoaderManager().destroyLoader(StaticData.NOTIFICATIONS_LOADER);
        if (adapter != null &&
                adapter.getCursor() != null &&
                !adapter.getCursor().isClosed())
            adapter.getCursor().close();
        super.onDestroy();
    }

    private final class NotificationSync extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            final Optional<ImmutableList<NotificationBase>> list = MiscUtils.autoRetry(
                    new DoWork<ImmutableList<NotificationBase>>() {
                        @Override
                        protected ImmutableList<NotificationBase> doWork() throws IOException {
                            return StaticData.notificationApi.get().execute().getItems();
                        }
                    }, Optional.<Predicate<ImmutableList<NotificationBase>>>absent());

            if (!list.isPresent())
                return false;

            final ContentValues[] values = ReachNotificationsHelper.extractValues(list.get());

            final ContentResolver resolver = getContentResolver();
            if (getContentResolver() == null)
                return false;

            //delete all rows
            resolver.delete(ReachNotificationsProvider.CONTENT_URI, null, null);
            //insert new rows
            resolver.bulkInsert(ReachNotificationsProvider.CONTENT_URI, values);
            for (ContentValues value : values)
                value.clear();
            refreshing.set(false);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);

            //TODO use boolean
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new CursorLoader(this,
                ReachNotificationsProvider.CONTENT_URI,
                ReachNotificationsHelper.projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {


        if (cursorLoader.getId() == StaticData.NOTIFICATIONS_LOADER && cursor != null && !cursor.isClosed()) {

            adapter.swapCursor(cursor);
            final int count = cursor.getCount();
            if (count == 0 && uploadList != null) {
                uploadHistoryAdapter.setActive(emptyTV1, true);
                MiscUtils.setEmptyTextforListView(uploadList, "No one is streaming from you currently");
            } else
                uploadHistoryAdapter.setActive(emptyTV1, false);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.UPLOAD_LOADER)
            adapter.swapCursor(null);
    }
}
