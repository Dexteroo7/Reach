//package reach.project.database.sql;
//
//import android.content.Context;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteOpenHelper;
//import android.util.Log;
//
//
///**
// * Created by Dexter on 18-06-2015.
// */
//public class ReachNotificationsHelper extends SQLiteOpenHelper {
//
//    public static final String NOTIFICATIONS_TABLE = "notifications";
//    public static final String COLUMN_ID = "_id";
//
//    public static final String COLUMN_NOTIFICATION_TYPE = "notificationType";
//    public static final String COLUMN_RECEIVER_ID = "receiverId";
//    public static final String COLUMN_RECEIVER_NAME = "receiverName";
//    public static final String COLUMN_RECEIVER_IMAGE = "receiverImage";
//
//    public static final String COLUMN_SENDER_ID = "senderId";
//    public static final String COLUMN_SENDER_NAME = "senderName";
//    public static final String COLUMN_SENDER_IMAGE = "senderImage";
//
//    public static final String COLUMN_ACTION_COMPLETE = "actionComplete";
//    public static final String COLUMN_SYSTEM_TIME = "systemTime";
//    public static final String COLUMN_ADDITIONAL_DATA = "additionalData";
//
//    private static final String DATABASE_NAME = "reach.database.sql.ReachNotificationsHelper";
//    private static final int DATABASE_VERSION = 1;
//
//    // Database creation sql statement
//    private static final String DATABASE_CREATE = "create table "
//            + NOTIFICATIONS_TABLE + "(" + COLUMN_ID
//            + " integer primary key autoincrement, " +
//
//            COLUMN_NOTIFICATION_TYPE + " int" + "," +
//            COLUMN_RECEIVER_ID + " long" + "," +
//            COLUMN_RECEIVER_NAME + " text" + "," +
//            COLUMN_RECEIVER_IMAGE + " text" + "," +
//
//            COLUMN_SENDER_ID + " long" + "," +
//            COLUMN_SENDER_NAME + " text" + "," +
//            COLUMN_SENDER_IMAGE + " text" + "," +
//
//            COLUMN_ACTION_COMPLETE + " int" + "," +
//            COLUMN_SYSTEM_TIME + " long" + "," +
//            COLUMN_ADDITIONAL_DATA + " text" + " )";
//
//    public static final String[] projection =
//            {
//                    COLUMN_ID,
//                    COLUMN_NOTIFICATION_TYPE,
//
//                    COLUMN_RECEIVER_ID,
//                    COLUMN_RECEIVER_NAME,
//                    COLUMN_RECEIVER_IMAGE,
//                    //strings
//                    COLUMN_SENDER_ID,
//                    COLUMN_SENDER_NAME,
//                    COLUMN_SENDER_IMAGE,
//
//                    COLUMN_ACTION_COMPLETE,
//                    COLUMN_SYSTEM_TIME,
//                    COLUMN_ADDITIONAL_DATA
//            };
//    /**
//     Operation kind :
//     0 = download;
//     1 = upload;
//     */
//    /*public static Notification cursorToProcess(Cursor cursor) {
//
//        final Notification notification = new Notification();
//
//        notification.setId(cursor.getLong(0));
//        notification.setType(cursor.getInt(1));
//
//        notification.setReceiverId(cursor.getLong(2));
//        notification.setReceiverName(cursor.getString(3));
//        notification.setReceiverImage(cursor.getString(4));
//
//        notification.setSenderId(cursor.getLong(5));
//        notification.setSenderName(cursor.getString(6));
//        notification.setSenderImage(cursor.getString(7));
//
//        notification.setActionComplete(cursor.getInt(8) == 1);
//        notification.setSystemTime(cursor.getLong(9));
//        notification.setAdditionalData(cursor.getString(10));
//
//        return notification;
//    }
//
//    public static ContentValues contentValuesCreator(Notification notification) {
//
//        final ContentValues values = new ContentValues();
//        if(notification.getId() != -1)
//            values.put(COLUMN_ID, notification.getId());
//
//        values.put(COLUMN_NOTIFICATION_TYPE, notification.getType());
//        values.put(COLUMN_RECEIVER_ID, notification.getReceiverId());
//        values.put(COLUMN_RECEIVER_NAME, notification.getReceiverName());
//        values.put(COLUMN_RECEIVER_IMAGE, notification.getReceiverImage());
//
//        values.put(COLUMN_SENDER_ID, notification.getSenderId());
//        values.put(COLUMN_SENDER_NAME, notification.getSenderName());
//        values.put(COLUMN_SENDER_IMAGE, notification.getSenderImage());
//        values.put(COLUMN_ACTION_COMPLETE, notification.getActionComplete());
//        values.put(COLUMN_SYSTEM_TIME, notification.getSystemTime());
//
//        values.put(COLUMN_ADDITIONAL_DATA, notification.getAdditionalData());
//
//        return values;
//    }*/
//
//    public ReachNotificationsHelper (Context context) {
//        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//        db.execSQL(DATABASE_CREATE);
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        Log.w(ReachAlbumHelper.class.getName(),
//                "Upgrading database from version " + oldVersion + " to "
//                        + newVersion + ", which will destroy all old data");
//        db.execSQL("DROP TABLE IF EXISTS " + NOTIFICATIONS_TABLE);
//        onCreate(db);
//    }
//}
