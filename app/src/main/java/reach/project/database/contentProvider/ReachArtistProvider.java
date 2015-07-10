package reach.project.database.contentProvider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

import reach.project.database.sql.ReachArtistHelper;

/**
 * Created by Dexter on 2/14/2015.
 */
public class ReachArtistProvider extends ContentProvider {

    public static final int ARTIST = 12;
    private static final int ARTIST_ID = 22; //+10
    private static final String BASE_PATH = "database/contentProvider/ReachArtistProvider";
    public static final String AUTHORITY = "reach.project.database.contentProvider.ReachArtistProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    private static final UriMatcher sURIMatcher;
    static {
        sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, ARTIST);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", ARTIST_ID);
    }

    private ReachArtistHelper reachArtistHelper;

    @Override
    public boolean onCreate() {
        reachArtistHelper = new ReachArtistHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        // check if the caller has requested a column which does not exists
        try {
            checkColumns(projection);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }        // Set the table
        queryBuilder.setTables(ReachArtistHelper.ARTIST_TABLE);
        int uriType = sURIMatcher.match(uri);
        switch (uriType) {

            case ARTIST:
                break;
            case ARTIST_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(ReachArtistHelper.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        final SQLiteDatabase db = reachArtistHelper.getWritableDatabase();
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
        SQLiteDatabase sqlDB = reachArtistHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case ARTIST:
                id = sqlDB.insert(ReachArtistHelper.ARTIST_TABLE, null, values);
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
        SQLiteDatabase sqlDB = reachArtistHelper.getWritableDatabase();
        switch (uriType) {
            case ARTIST:
                sqlDB.beginTransaction();
                try {
                    //delete everything
                    sqlDB.delete(ReachArtistHelper.ARTIST_TABLE,
                            ReachArtistHelper.COLUMN_USER_ID + " = ?",
                            new String[]{values[0].get(ReachArtistHelper.COLUMN_USER_ID)+""});
                    //bulk insert
                    for(ContentValues contentValues : values) {
                        sqlDB.insert(ReachArtistHelper.ARTIST_TABLE, null, contentValues);
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
        SQLiteDatabase sqlDB = reachArtistHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case ARTIST:
                rowsDeleted = sqlDB.delete(ReachArtistHelper.ARTIST_TABLE, selection,
                        selectionArgs);
                break;
            case ARTIST_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(ReachArtistHelper.ARTIST_TABLE,
                            ReachArtistHelper.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(ReachArtistHelper.ARTIST_TABLE,
                            ReachArtistHelper.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            default: rowsDeleted = 0;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = reachArtistHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case ARTIST:
                rowsUpdated = sqlDB.update(ReachArtistHelper.ARTIST_TABLE,
                        values,
                        selection,
                        selectionArgs);
                break;
            case ARTIST_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(ReachArtistHelper.ARTIST_TABLE,
                            values,
                            ReachArtistHelper.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(ReachArtistHelper.ARTIST_TABLE,
                            values,
                            ReachArtistHelper.COLUMN_ID + "=" + id
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
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(ReachArtistHelper.projection));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}