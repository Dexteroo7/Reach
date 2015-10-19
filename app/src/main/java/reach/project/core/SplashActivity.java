package reach.project.core;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.appvirality.android.AppviralityAPI;

/**
 * Created by ashish on 19/10/15.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppviralityAPI.init(getApplicationContext());
        Intent intent = new Intent(this, ReachActivity.class);
        startActivity(intent);
        finish();
    }
}