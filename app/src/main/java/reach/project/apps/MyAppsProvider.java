//package reach.project.apps;
//
//import android.content.ContentProvider;
//import android.content.ContentValues;
//import android.content.UriMatcher;
//import android.database.Cursor;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteQueryBuilder;
//import android.net.Uri;
//import android.support.annotation.NonNull;
//import android.text.TextUtils;
//
//import java.util.Arrays;
//import java.util.HashSet;
//
///**
// * Created by Dexter on 2/14/2015.
// */
//public class MyAppsProvider extends ContentProvider {
//
//    public static final int APP = 17;
//    private static final int APP_ID = 27; //+10
//    private static final String BASE_PATH = "database/contentProvider/MyAppsProvider";
//    public static final String AUTHORITY = "reach.project.music.MyAppsProvider";
//    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
//    private static final UriMatcher sURIMatcher;
//    static {
//        sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
//        sURIMatcher.addURI(AUTHORITY, BASE_PATH, APP);
//        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", APP_ID);
//    }
//
//    private MyAppsHelper myAppsHelper;
//
//    @Override
//    public boolean onCreate() {
//        myAppsHelper = new MyAppsHelper(getContext());
//        return false;
//    }
//
//    @Override
//    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
//        // Using SQLiteQueryBuilder instead of query() method
//        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
//        // check if the caller has requested a column which does not exists
//        try {
//            checkColumns(projection);
//        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
//        }
//        // Set the table
//        queryBuilder.setTables(MyAppsHelper.APP_TABLE);
//        int uriType = sURIMatcher.match(uri);
//        switch (uriType) {
//
//            case APP:
//                break;
//            case APP_ID:
//                // adding the ID to the original query
//                queryBuilder.appendWhere(MyAppsHelper.COLUMN_ID + "="
//                        + uri.getLastPathSegment());
//                break;
//            default:
//                throw new IllegalArgumentException("Unknown URI: " + uri);
//        }
//
//        final SQLiteDatabase db = myAppsHelper.getWritableDatabase();
//        final Cursor cursor = queryBuilder.query(db, projection, selection,
//                selectionArgs, null, null, sortOrder);
//        // make sure that potential listeners are getting notified
//        cursor.setNotificationUri(getContext().getContentResolver(), uri);
//        return cursor;
//    }
//
//    @Override
//    public String getType(@NonNull Uri uri) {
//        return null;
//    }
//
//    @Override
//    public Uri insert(@NonNull Uri uri, ContentValues values) {
//
//        int uriType = sURIMatcher.match(uri);
//        SQLiteDatabase sqlDB = myAppsHelper.getWritableDatabase();
//        long id;
//        switch (uriType) {
//            case APP:
//                id = sqlDB.insert(MyAppsHelper.APP_TABLE, null, values);
//                break;
//            default:
//                throw new IllegalArgumentException("Unknown URI: " + uri);
//        }
//        getContext().getContentResolver().notifyChange(uri, null);
//        return Uri.parse(BASE_PATH + "/" + id);
//    }
//
//    @Override
//    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
//
//        int done = 0;
//        int uriType = sURIMatcher.match(uri);
//        SQLiteDatabase sqlDB = myAppsHelper.getWritableDatabase();
//        switch (uriType) {
//            case APP:
//                sqlDB.beginTransaction();
//                try {
//                    //delete everything
//                    sqlDB.delete(MyAppsHelper.APP_TABLE, null, null);
//                    //bulk insert
//                    for(ContentValues contentValues : values) {
//                        sqlDB.insert(MyAppsHelper.APP_TABLE, null, contentValues);
//                        done++;
//                    }
//                    sqlDB.setTransactionSuccessful();
//                } finally {
//                    sqlDB.endTransaction();
//                }
//                break;
//            default:
//                throw new IllegalArgumentException("Unknown URI: " + uri);
//        }
//        getContext().getContentResolver().notifyChange(uri, null);
//        return done;
//    }
//
//    @Override
//    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
//
//        int uriType = sURIMatcher.match(uri);
//        SQLiteDatabase sqlDB = myAppsHelper.getWritableDatabase();
//        int rowsDeleted;
//        switch (uriType) {
//            case APP:
//                rowsDeleted = sqlDB.delete(MyAppsHelper.APP_TABLE, selection,
//                        selectionArgs);
//                break;
//            case APP_ID:
//                String id = uri.getLastPathSegment();
//                if (TextUtils.isEmpty(selection)) {
//                    rowsDeleted = sqlDB.delete(MyAppsHelper.APP_TABLE,
//                            MyAppsHelper.COLUMN_ID + "=" + id,
//                            null);
//                } else {
//                    rowsDeleted = sqlDB.delete(MyAppsHelper.APP_TABLE,
//                            MyAppsHelper.COLUMN_ID + "=" + id
//                                    + " and " + selection,
//                            selectionArgs);
//                }
//                break;
//            default: rowsDeleted = 0;
//        }
//        getContext().getContentResolver().notifyChange(uri, null);
//        return rowsDeleted;
//    }
//
//    @Override
//    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
//
//        int uriType = sURIMatcher.match(uri);
//        SQLiteDatabase sqlDB = myAppsHelper.getWritableDatabase();
//        int rowsUpdated;
//        switch (uriType) {
//            case APP:
//                rowsUpdated = sqlDB.update(MyAppsHelper.APP_TABLE,
//                        values,
//                        selection,
//                        selectionArgs);
//                break;
//            case APP_ID:
//                String id = uri.getLastPathSegment();
//                if (TextUtils.isEmpty(selection)) {
//                    rowsUpdated = sqlDB.update(MyAppsHelper.APP_TABLE,
//                            values,
//                            MyAppsHelper.COLUMN_ID + "=" + id,
//                            null);
//                } else {
//                    rowsUpdated = sqlDB.update(MyAppsHelper.APP_TABLE,
//                            values,
//                            MyAppsHelper.COLUMN_ID + "=" + id
//                                    + " and "
//                                    + selection,
//                            selectionArgs);
//                }
//                break;
//            default: rowsUpdated = 0;
//        }
//        getContext().getContentResolver().notifyChange(uri, null);
//        return rowsUpdated;
//    }
//
//    private void checkColumns(String[] projection) {
//
//        if (projection != null) {
//
//            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
//            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(MyAppsHelper.projection));
//            // check if all columns which are requested are available
//            if (!availableColumns.containsAll(requestedColumns)) {
//                throw new IllegalArgumentException("Unknown columns in projection");
//            }
//        }
//    }
//}