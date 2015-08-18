package reach.project.core;

import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.database.sql.ReachSongHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.MusicScanner;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.ReachDatabase;

public class UpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        final SharedPreferences sharedPreferences = context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        sharedPreferences.edit().remove("song_hash").apply();
        sharedPreferences.edit().remove("play_list_hash").apply();

        final Cursor cursor = context.getContentResolver().query(
                ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.projection,
                ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?",
                new String[]{"0"}, null);

        final ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        while (cursor.moveToNext()) {

            final ReachDatabase database = ReachDatabaseHelper.cursorToProcess(cursor);
            Log.i("Ayush", "Found " + database.getDisplayName());

            final Cursor songCursor = context.getContentResolver().query(
                    ReachSongProvider.CONTENT_URI,
                    new String[]{
                            ReachSongHelper.COLUMN_ARTIST,
                            ReachSongHelper.COLUMN_DURATION},
                    ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                            ReachSongHelper.COLUMN_SONG_ID + " = ?",
                    new String[]{database.getSenderId() + "", database.getSongId() + ""}, null);

            if (songCursor == null)
                continue;
            if (!songCursor.moveToFirst()) {
                songCursor.close();
                continue;
            }

            final String artistName = songCursor.getString(0);
            final long duration = songCursor.getLong(1);
            songCursor.close();

            final ContentValues values = new ContentValues();
            values.put(ReachDatabaseHelper.COLUMN_ARTIST_NAME, artistName);
            values.put(ReachDatabaseHelper.COLUMN_DURATION, duration);
            values.put(ReachDatabaseHelper.COLUMN_IS_LIKED, "0");
            values.put(ReachDatabaseHelper.COLUMN_ONLINE_STATUS, ReachFriendsHelper.OFFLINE_REQUEST_GRANTED);
            operations.add(ContentProviderOperation.newUpdate(
                    ReachDatabaseProvider.CONTENT_URI)
                    .withValues(values)
                    .withSelection(ReachDatabaseHelper.COLUMN_ID + " = ?", new String[]{database.getId() + ""})
                    .build());
            Log.i("Ayush", "Updated reachDatabase " + artistName + " " + duration);
        }

        if (operations.size() > 0)
            try {
                Log.i("Downloader", "SETTING REACH DATABASE " + operations.size());
                context.getContentResolver().applyBatch(ReachDatabaseProvider.AUTHORITY, operations);
            } catch (RemoteException | OperationApplicationException e) {
                e.printStackTrace();
            }

        //refresh music list
        final Intent musicScannerIntent = new Intent(context, MusicScanner.class);
        musicScannerIntent.putExtra("first", true);
        context.startService(musicScannerIntent);

        // an Intent broadcast.
        Log.i("Ayush", "Application updated");
        if (MiscUtils.isOnline(context))
            new Thread(() -> {
                MiscUtils.updateGCM(SharedPrefUtils.getServerId(sharedPreferences),
                        new WeakReference<>(context));
            }, "GCM_UPDATER").start();
    }
}
