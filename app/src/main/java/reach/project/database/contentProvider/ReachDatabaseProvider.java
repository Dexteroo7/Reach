package reach.project.database.contentProvider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

import reach.project.database.sql.ReachDatabaseHelper;

/**
 * Created by Dexter on 2/14/2015.
 */
public class ReachDatabaseProvider extends ContentProvider {

    public static final int DATABASE = 12;
    private static final int DATABASE_ID = 22; //+10
    private static final String BASE_PATH = "database/contentProvider/ReachDatabaseProvider";
    public static final String AUTHORITY = "reach.project.database.contentProvider.ReachDatabaseProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
    private static final UriMatcher sURIMatcher;
    static {
        sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, DATABASE);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", DATABASE_ID);
    }

    private ReachDatabaseHelper reachDatabaseHelper;

    @Override
    public boolean onCreate() {
        reachDatabaseHelper = new ReachDatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        // check if the caller has requested a column which does not exists
        checkColumns(projection);
        // Set the table
        queryBuilder.setTables(ReachDatabaseHelper.REACH_TABLE);
        int uriType = sURIMatcher.match(uri);
        switch (uriType) {

            case DATABASE:
                break;
            case DATABASE_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(ReachDatabaseHelper.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        final SQLiteDatabase db = reachDatabaseHelper.getWritableDatabase();
        final Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
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
        SQLiteDatabase sqlDB = reachDatabaseHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case DATABASE:
                id = sqlDB.insert(ReachDatabaseHelper.REACH_TABLE, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

//    @Override
//    public int bulkInsert(Uri uri, ContentValues[] values) {
//        return super.bulkInsert(uri, values);
//    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = reachDatabaseHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case DATABASE:
                rowsDeleted = sqlDB.delete(ReachDatabaseHelper.REACH_TABLE, selection,
                        selectionArgs);
                break;
            case DATABASE_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(ReachDatabaseHelper.REACH_TABLE,
                            ReachDatabaseHelper.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(ReachDatabaseHelper.REACH_TABLE,
                            ReachDatabaseHelper.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            default: rowsDeleted =0;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = reachDatabaseHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case DATABASE:
                rowsUpdated = sqlDB.update(ReachDatabaseHelper.REACH_TABLE,
                        values,
                        selection,
                        selectionArgs);
                break;
            case DATABASE_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(ReachDatabaseHelper.REACH_TABLE,
                            values,
                            ReachDatabaseHelper.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(ReachDatabaseHelper.REACH_TABLE,
                            values,
                            ReachDatabaseHelper.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            default: rowsUpdated = 0;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(String[] projection) {


        if (projection != null) {

            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(ReachDatabaseHelper.projection));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
