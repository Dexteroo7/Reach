package reach.project.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

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
//        final Intent musicScannerIntent = new Intent(context, MusicScanner.class);
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
