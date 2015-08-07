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

    @Override
    public void onReceive(final Context context, Intent intent) {

        context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS).edit().remove("song_hash");
        context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS).edit().remove("play_list_hash");

        final SharedPreferences sharedPreferences = context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        // an Intent broadcast.
        Log.i("Ayush", "Application updated");
        if (MiscUtils.isOnline(context))
        new Thread(new GCMUpdate(new WeakReference<>(context),
                SharedPrefUtils.getServerId(sharedPreferences))).start();
    }

    private static final class GCMUpdate implements Runnable {

        private final WeakReference<Context> reference;
        private final long serverId;

        private GCMUpdate(WeakReference<Context> reference, long serverId) {
            this.reference = reference;
            this.serverId = serverId;
        }

        @Override
        public void run() {

            MiscUtils.updateGCM(serverId, reference);
        }
    }
}
