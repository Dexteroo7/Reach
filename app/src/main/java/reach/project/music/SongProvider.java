package reach.project.music;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Dexter on 2/14/2015.
 */
public class SongProvider extends ContentProvider {

    public static final int DATABASE = 12;
    private static final int DATABASE_ID = 22; //+10
    private static final int DATABASE_META_HASH = 32; //+10

    private static final String BASE_PATH = "database/contentProvider/SongProvider";
    public static final String AUTHORITY = "reach.project.music.SongProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static final UriMatcher sURIMatcher;
    private static final String TAG = SongProvider.class.getSimpleName();

    static {
        sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, DATABASE);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", DATABASE_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/*", DATABASE_META_HASH);
    }

    private SongHelper songHelper;

    @Override
    public boolean onCreate() {
        songHelper = new SongHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        // check if the caller has requested a column which does not exists
        checkColumns(projection);
        // Set the table
        queryBuilder.setTables(SongHelper.REACH_TABLE);
        int uriType = sURIMatcher.match(uri);
        switch (uriType) {

            case DATABASE:
                break;
            case DATABASE_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(SongHelper.COLUMN_ID + "="
                        + uri.getLastPathSegment());

                break;
            case DATABASE_META_HASH:
                queryBuilder.appendWhere(SongHelper.COLUMN_META_HASH + "=" + uri.getLastPathSegment());
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        //TODO: Check whether it should be readable database

        final SQLiteDatabase db = songHelper.getWritableDatabase();
        final Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        Log.d(TAG, "query: " + queryBuilder.buildQuery(projection,selection,
                null,null,
                sortOrder,
                null
                ));
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = songHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case DATABASE:
                id = sqlDB.insert(SongHelper.REACH_TABLE, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {

        int done = 0;
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = songHelper.getWritableDatabase();
        switch (uriType) {
            case DATABASE:
                sqlDB.beginTransaction();
                try {
                    //delete everything
                    sqlDB.delete(SongHelper.REACH_TABLE, null, null);
                    //bulk insert
                    for (ContentValues contentValues : values) {
                        sqlDB.insert(SongHelper.REACH_TABLE, null, contentValues);
                        done++;
                    }
                    sqlDB.setTransactionSuccessful();
                } finally {
                    sqlDB.endTransaction();
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return done;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = songHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case DATABASE:
                rowsDeleted = sqlDB.delete(SongHelper.REACH_TABLE, selection,
                        selectionArgs);
                break;
            case DATABASE_ID: {
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(SongHelper.REACH_TABLE,
                            SongHelper.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(SongHelper.REACH_TABLE,
                            SongHelper.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            }

            case DATABASE_META_HASH: {
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(SongHelper.REACH_TABLE,
                            SongHelper.COLUMN_META_HASH + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(SongHelper.REACH_TABLE,
                            SongHelper.COLUMN_META_HASH + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            }
            default:
                rowsDeleted = 0;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = songHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case DATABASE:
                Log.d(TAG, "Updating database with no parameter");
                rowsUpdated = sqlDB.update(SongHelper.REACH_TABLE,
                        values,
                        selection,
                        selectionArgs);
                break;
            case DATABASE_ID: {
                Log.d(TAG, "Updating database with id in URI");
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(SongHelper.REACH_TABLE,
                            values,
                            SongHelper.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(SongHelper.REACH_TABLE,
                            values,
                            SongHelper.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            }
            case DATABASE_META_HASH: {
                Log.d(TAG, "Updating database with meta hash in URI");
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(SongHelper.REACH_TABLE,
                            values,
                            SongHelper.COLUMN_META_HASH + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(SongHelper.REACH_TABLE,
                            values,
                            SongHelper.COLUMN_META_HASH + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            }
            default:
                rowsUpdated = 0;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(String[] projection) {


        if (projection != null) {

            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(SongCursorHelper.DOWNLOADING_HELPER.getProjection()));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
