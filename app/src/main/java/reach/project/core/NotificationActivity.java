package reach.project.core;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.widget.ListView;

import reach.project.R;


public class NotificationActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setTitle("Notifications");
        final ListView notificationsList = (ListView) findViewById(R.id.notificationsList);
        //ReachNotificationAdapter reachNotificationAdapter = new ReachNotificationAdapter(this,R.layout.notification_item, , )
    }
}
