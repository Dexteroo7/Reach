//package reach.project.apps;
//
//import android.content.Context;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteOpenHelper;
//
///**
// * Created by dexter on 20/02/16.
// */
//public class MyAppsHelper extends SQLiteOpenHelper {
//
//    private static final String DATABASE_NAME = "reach.database.sql.MyAppsHelper";
//    private static final int DATABASE_VERSION = 0;
//
//    public static final String APP_TABLE = "apps";
//    public static final String COLUMN_ID = "_id";
//
//
//
//    public static final String COLUMN_VISIBILITY = "visibility";
//    public static final String COLUMN_IS_LIKED = "isLiked";
//
//    public MyAppsHelper(Context context) {
//        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//
//    }
//}
