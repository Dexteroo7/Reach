package reach.project.core;

import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

//TODO
public class UpdateReceiver extends BroadcastReceiver {
    public UpdateReceiver() {
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        final Cursor cursor = context.getContentResolver().query(
                ReachDatabaseProvider.CONTENT_URI,
                new String[]{
                        ReachDatabaseHelper.COLUMN_SENDER_NAME,
                        ReachDatabaseHelper.COLUMN_ID,
                        ReachDatabaseHelper.COLUMN_SENDER_ID,
                        ReachDatabaseHelper.COLUMN_OPERATION_KIND},
                ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                ReachDatabaseHelper.COLUMN_SENDER_NAME + " = ?",
                new String[]{0+"", "hello_world"}, null);

        if(cursor == null)
            return;
        final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        while (cursor.moveToNext()) {

            final long id = cursor.getLong(1);
            final long senderId = cursor.getLong(2);

            final Cursor friend = context.getContentResolver().query(
                    Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + senderId),
                    new String[]{
                            ReachFriendsHelper.COLUMN_ID,
                            ReachFriendsHelper.COLUMN_USER_NAME},
                    ReachFriendsHelper.COLUMN_ID + " = ?",
                    new String[]{senderId + ""}, null);

            final String actualName;
            if(friend == null)
                actualName = "";
            else if(!friend.moveToFirst()) {
                friend.close();
                actualName = "";
            } else {
                actualName = friend.getString(1);
                friend.close();
            }
            Log.i("Ayush", "New name = " + actualName);
            operations.add(ContentProviderOperation
                    .newUpdate(Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id))
                    .withSelection(ReachDatabaseHelper.COLUMN_ID + " = ?", new String[]{id + ""})
                    .withValue(ReachDatabaseHelper.COLUMN_SENDER_NAME, actualName)
                    .build());
        }

        if(operations.size() > 0)
            try {
                context.getContentResolver().applyBatch(ReachDatabaseProvider.AUTHORITY, operations);
            } catch (RemoteException | OperationApplicationException e) {
                e.printStackTrace();
            } finally {
                cursor.close();
                operations.clear();
            }

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
