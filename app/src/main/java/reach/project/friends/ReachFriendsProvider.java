package reach.project.friends;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Dexter on 2/14/2015.
 */
public class ReachFriendsProvider extends ContentProvider {

    public static final int FRIENDS = 14;
    private static final int FRIENDS_ID = 24; //+10
    private static final String BASE_PATH = "database/contentProvider/ReachFriendsProvider";
    public static final String AUTHORITY = "reach.project.friends.ReachFriendsProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
    private static final UriMatcher sURIMatcher;
    static {
        sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, FRIENDS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", FRIENDS_ID);
    }

    private ReachFriendsHelper reachFriendsHelper;

    @Override
    public boolean onCreate() {
        reachFriendsHelper = new ReachFriendsHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) throws SQLiteDatabaseLockedException {
        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        // check if the caller has requested a column which does not exists
        try {
            checkColumns(projection);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }        // Set the table
        queryBuilder.setTables(ReachFriendsHelper.FRIENDS_TABLE);
        int uriType = sURIMatcher.match(uri);
        switch (uriType) {

            case FRIENDS:
                break;
            case FRIENDS_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(ReachFriendsHelper.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        final SQLiteDatabase db = reachFriendsHelper.getWritableDatabase();
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
        SQLiteDatabase sqlDB = reachFriendsHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case FRIENDS:
                id = sqlDB.insert(ReachFriendsHelper.FRIENDS_TABLE, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int bulkInsert(Uri uri, @NonNull ContentValues[] values) {

        int done = 0;
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = reachFriendsHelper.getWritableDatabase();
        switch (uriType) {
            case FRIENDS:
                sqlDB.beginTransaction();
                try {
                    //delete everything
                    sqlDB.delete(ReachFriendsHelper.FRIENDS_TABLE, null, null);
                    //bulk insert
                    for(done = 0; done<values.length; done++)
                        sqlDB.insert(ReachFriendsHelper.FRIENDS_TABLE, null, values[done]);
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
        SQLiteDatabase sqlDB = reachFriendsHelper.getWritableDatabase();
        int rowsDeleted = 0;
        try {
            switch (uriType) {
                case FRIENDS:
                    rowsDeleted = sqlDB.delete(ReachFriendsHelper.FRIENDS_TABLE, selection,
                            selectionArgs);
                    break;
                case FRIENDS_ID:
                    String id = uri.getLastPathSegment();
                    if (TextUtils.isEmpty(selection)) {
                        rowsDeleted = sqlDB.delete(ReachFriendsHelper.FRIENDS_TABLE,
                                ReachFriendsHelper.COLUMN_ID + "=" + id,
                                null);
                    } else {
                        rowsDeleted = sqlDB.delete(ReachFriendsHelper.FRIENDS_TABLE,
                                ReachFriendsHelper.COLUMN_ID + "=" + id
                                        + " and " + selection,
                                selectionArgs);
                    }
                    break;
                default:
                    rowsDeleted = 0;
            }
            getContext().getContentResolver().notifyChange(uri, null);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = reachFriendsHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case FRIENDS:
                rowsUpdated = sqlDB.update(ReachFriendsHelper.FRIENDS_TABLE,
                        values,
                        selection,
                        selectionArgs);
                break;
            case FRIENDS_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(ReachFriendsHelper.FRIENDS_TABLE,
                            values,
                            ReachFriendsHelper.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(ReachFriendsHelper.FRIENDS_TABLE,
                            values,
                            ReachFriendsHelper.COLUMN_ID + "=" + id
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
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(ReachFriendsHelper.projection));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
