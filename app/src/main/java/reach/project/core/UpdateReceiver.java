package reach.project.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.ref.WeakReference;

import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class UpdateReceiver extends BroadcastReceiver {
    public UpdateReceiver() {
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        // an Intent broadcast.
        Log.i("Ayush", "Application updated");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final SharedPreferences sharedPreferences = context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
                MiscUtils.updateGCM(SharedPrefUtils.getServerId(sharedPreferences), new WeakReference<>(context));
            }
        }).start();
        context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS).edit().remove("song_hash");
        context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS).edit().remove("play_list_hash");
    }
}
