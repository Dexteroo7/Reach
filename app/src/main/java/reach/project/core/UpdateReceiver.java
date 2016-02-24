package reach.project.core;

import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import com.google.common.hash.Hashing;

import java.util.ArrayList;

import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.utils.MiscUtils;

// For updating db on app update
public class UpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        //adding metaHash entry for reachDataBase table

        final ContentResolver resolver = context.getContentResolver();

        final Cursor cursor = resolver.query(
                ReachDatabaseProvider.CONTENT_URI,
                new String[]{
                        ReachDatabaseHelper.COLUMN_ID,
                        ReachDatabaseHelper.COLUMN_RECEIVER_ID,
                        ReachDatabaseHelper.COLUMN_DURATION,
                        ReachDatabaseHelper.COLUMN_SIZE,
                        ReachDatabaseHelper.COLUMN_DISPLAY_NAME,
                },
                ReachDatabaseHelper.COLUMN_META_HASH + " = ? OR " +
                        ReachDatabaseHelper.COLUMN_META_HASH + " = NULL", new String[]{""}, null);

        if (cursor == null)
            return;

        final ArrayList<ContentProviderOperation> operations = new ArrayList<>(cursor.getCount());

        while (cursor.moveToNext()) {

            final long entryId = cursor.getLong(0);
            final String metaHash = MiscUtils.songHashCalculator(
                    cursor.getLong(1), //userID
                    cursor.getLong(2), //duration
                    cursor.getLong(3), //size
                    cursor.getString(4), //title
                    Hashing.sipHash24());

            final ContentValues contentValues = new ContentValues(1);
            contentValues.put(ReachDatabaseHelper.COLUMN_META_HASH, metaHash);
            operations.add(ContentProviderOperation
                    .newUpdate(Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + entryId))
                    .withValues(contentValues)
                    .withSelection(ReachDatabaseHelper.COLUMN_ID + " = ?",
                            new String[]{entryId + ""})
                    .build());
        }
        cursor.close();

        if (operations.size() > 0)
            try {
                resolver.applyBatch(ReachDatabaseProvider.AUTHORITY, operations);
            } catch (RemoteException | OperationApplicationException e) {
                e.printStackTrace();
            }

        //cleanse the songs table to remove songs of friends
//        context.getContentResolver().delete(MySongsProvider.CONTENT_URI, null, null);

//        final Cursor cursor = context.getContentResolver().query(
//                ReachDatabaseProvider.CONTENT_URI,
//                new String[]{ReachDatabaseHelper.COLUMN_ID,
//                        ReachDatabaseHelper.COLUMN_UNIQUE_ID},
//                ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?",
//                new String[]{"0"}, null);
//
//        if (cursor == null)
//            return;
//
//        final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
//        final SecureRandom secureRandom = new SecureRandom();
//
//        while (cursor.moveToNext()) {
//
//            final long localId = cursor.getLong(0);
//            final long uniqueId = cursor.getLong(1);
//            if (uniqueId == 0 || uniqueId == -1) {
//
//                final ContentValues values = new ContentValues(2);
//                values.put(ReachDatabaseHelper.COLUMN_UNIQUE_ID, secureRandom.nextInt(Integer.MAX_VALUE));
//                operations.add(ContentProviderOperation
//                        .newUpdate(ReachDatabaseProvider.CONTENT_URI)
//                        .withValues(values)
//                        .withSelection(ReachDatabaseHelper.COLUMN_ID + " = ?", new String[]{localId + ""}).build());
//            }
//        }
//        cursor.close();
//
//        if (operations.size() > 0)
//            try {
//                context.getContentResolver().applyBatch(ReachDatabaseProvider.AUTHORITY, operations);
//            } catch (RemoteException | OperationApplicationException e) {
//                e.printStackTrace();
//            }

//        final SharedPreferences sharedPreferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
//        sharedPreferences.edit().remove("song_hash").apply();
//        sharedPreferences.edit().remove("play_list_hash").apply();
//
//        final Cursor cursor = context.getContentResolver().query(
//                ReachDatabaseProvider.CONTENT_URI,
//                ReachDatabaseHelper.projection,
//                ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?",
//                new String[]{"0"}, null);
//
//        final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
//
//        while (cursor.moveToNext()) {
//
//            final ReachDatabase database = ReachDatabaseHelper.cursorToProcess(cursor);
//            Log.i("Ayush", "Found " + database.getDisplayName());
//
//            final Cursor songCursor = context.getContentResolver().query(
//                    MySongsProvider.CONTENT_URI,
//                    new String[]{
//                            MySongsHelper.COLUMN_ARTIST,
//                            MySongsHelper.COLUMN_DURATION},
//                    MySongsHelper.COLUMN_USER_ID + " = ? and " +
//                            MySongsHelper.COLUMN_SONG_ID + " = ?",
//                    new String[]{database.getSenderId() + "", database.getSongId() + ""}, null);
//
//            if (songCursor == null)
//                continue;
//            if (!songCursor.moveToFirst()) {
//                songCursor.close();
//                continue;
//            }
//
//            final String artistName = songCursor.getString(0);
//            final long duration = songCursor.getLong(1);
//            songCursor.close();
//
//            final ContentValues values = new ContentValues();
//            values.put(ReachDatabaseHelper.COLUMN_ARTIST_NAME, artistName);
//            values.put(ReachDatabaseHelper.COLUMN_DURATION, duration);
//            values.put(ReachDatabaseHelper.COLUMN_IS_LIKED, "0");
//            values.put(ReachDatabaseHelper.COLUMN_ONLINE_STATUS, ReachFriendsHelper.OFFLINE_REQUEST_GRANTED);
//            operations.add(ContentProviderOperation.newUpdate(
//                    ReachDatabaseProvider.CONTENT_URI)
//                    .withValues(values)
//                    .withSelection(ReachDatabaseHelper.COLUMN_ID + " = ?", new String[]{database.getId() + ""})
//                    .build());
//            Log.i("Ayush", "Updated reachDatabase " + artistName + " " + duration);
//        }
//
//        if (operations.size() > 0)
//            try {
//                Log.i("Downloader", "SETTING REACH DATABASE " + operations.size());
//                context.getContentResolver().applyBatch(ReachDatabaseProvider.AUTHORITY, operations);
//            } catch (RemoteException | OperationApplicationException e) {
//                e.printStackTrace();
//            }
//
//        //refresh music list
//        final Intent musicScannerIntent = new Intent(context, MetaDataScanner.class);
//        musicScannerIntent.putExtra("first", true);
//        context.startService(musicScannerIntent);

        // an Intent broadcast.
//        Log.i("Ayush", "Application updated");
//        if (MiscUtils.isOnline(context))
//            new Thread(() -> {
//                MiscUtils.updateGCM(SharedPrefUtils.getServerId(sharedPreferences),
//                        new WeakReference<>(context));
//            }, "GCM_UPDATER").start();
    }
}
