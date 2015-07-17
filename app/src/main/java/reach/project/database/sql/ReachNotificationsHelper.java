package reach.project.database.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Optional;

import java.util.List;

import reach.backend.notifications.notificationApi.model.NotificationBase;
import reach.project.database.notifications.BecameFriends;
import reach.project.database.notifications.Like;
import reach.project.database.notifications.Push;
import reach.project.database.notifications.PushAccepted;
import reach.project.database.notifications.Types;

/**
 * Created by Dexter on 18-06-2015.
 */
public class ReachNotificationsHelper extends SQLiteOpenHelper {

    public static final String NOTIFICATIONS_TABLE = "notifications";
    public static final String COLUMN_ID = "_id";

    //base
    public static final String COLUMN_NOTIFICATION_TYPE = "notificationType";
    public static final String COLUMN_HOST_ID = "hostId";
    public static final String COLUMN_HOST_NAME = "hostName";
    public static final String COLUMN_IMAGE_ID = "imageId";
    public static final String COLUMN_SYSTEM_TIME = "systemTime";
    public static final String COLUMN_READ = "read";
    //like
    public static final String COLUMN_SONG_NAME = "songName";
    //push
    public static final String COLUMN_PUSH_CONTAINER = "pushContainer";
    //push accepted
    public static final String COLUMN_FIRST_SONG_NAME = "firstSongName";
    public static final String COLUMN_SIZE = "size";

    private static final String DATABASE_NAME = "reach.database.sql.ReachNotificationsHelper";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + NOTIFICATIONS_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +

            //base
            COLUMN_NOTIFICATION_TYPE + " int" + "," +
            COLUMN_HOST_ID + " long" + "," +
            COLUMN_HOST_NAME + " text" + "," +
            COLUMN_IMAGE_ID + " text" + "," +
            COLUMN_SYSTEM_TIME + " long" + "," +
            COLUMN_READ + " short" + "," +
            //like
            COLUMN_SONG_NAME + " text" + "," +
            //push
            COLUMN_PUSH_CONTAINER + " text" + "," +
            //push accepted
            COLUMN_FIRST_SONG_NAME + " text" + "," +
            COLUMN_SIZE + " int" + " )";

    public static final String[] projection =
            {
                    COLUMN_ID, //0
                    //base
                    COLUMN_NOTIFICATION_TYPE, //1
                    COLUMN_HOST_ID, //2
                    COLUMN_HOST_NAME, //3
                    COLUMN_IMAGE_ID, //4
                    COLUMN_SYSTEM_TIME, //5
                    COLUMN_READ, //6
                    //like
                    COLUMN_SONG_NAME, //7
                    //push
                    COLUMN_PUSH_CONTAINER, //8
                    //push accepted
                    COLUMN_FIRST_SONG_NAME, //9
                    COLUMN_SIZE, //10
            };

    public static ContentValues [] extractValues(List<NotificationBase> dataFromServer) {

        final ContentValues [] values = new ContentValues[dataFromServer.size()];

        int i = 0;
        for (NotificationBase base : dataFromServer) {

            if(base.getTypes().equals(Types.BECAME_FRIENDS.name())) {

                final BecameFriends becameFriends = new BecameFriends();
                becameFriends.portData(base);
                values[i++] =  contentValuesCreator(becameFriends);

            } else if(base.getTypes().equals(Types.LIKE.name())) {

                final Like like = new Like();
                like.portData(base);
                like.setSongName((String) base.get("songName"));
                values[i++] =  contentValuesCreator(like);

            } else if(base.getTypes().equals(Types.PUSH.name())) {

                final Push push = new Push();
                push.portData(base);
                push.setPushContainer((String) base.get("pushContainer"));
                values[i++] =  contentValuesCreator(push);

            } else if(base.getTypes().equals(Types.PUSH_ACCEPTED.name())) {

                final PushAccepted accepted = new PushAccepted();
                accepted.portData(base);
                accepted.setFirstSongName((String) base.get("firstSongName"));
                accepted.setSize(Integer.parseInt(base.get("size").toString()));
                values[i++] =  contentValuesCreator(accepted);

            } else throw new IllegalArgumentException("Wrong notification type received " + base.getTypes());
        }
        return values;
    }


    public static ContentValues contentValuesCreator(Like like) {

        final ContentValues values = new ContentValues();
        values.put(COLUMN_NOTIFICATION_TYPE, like.getTypes().name());
        values.put(COLUMN_HOST_ID, like.getHostId());
        values.put(COLUMN_HOST_NAME, like.getHostName());
        values.put(COLUMN_IMAGE_ID, like.getImageId());
        values.put(COLUMN_SYSTEM_TIME, like.getSystemTime());
        values.put(COLUMN_READ, like.getRead());

        values.put(COLUMN_SONG_NAME, like.getSongName());
        return values;
    }

    public static ContentValues contentValuesCreator(BecameFriends becameFriends) {

        final ContentValues values = new ContentValues();
        values.put(COLUMN_NOTIFICATION_TYPE, becameFriends.getTypes().name());
        values.put(COLUMN_HOST_ID, becameFriends.getHostId());
        values.put(COLUMN_HOST_NAME, becameFriends.getHostName());
        values.put(COLUMN_IMAGE_ID, becameFriends.getImageId());
        values.put(COLUMN_SYSTEM_TIME, becameFriends.getSystemTime());
        values.put(COLUMN_READ, becameFriends.getRead());
        return values;
    }

    public static ContentValues contentValuesCreator(Push push) {

        final ContentValues values = new ContentValues();
        values.put(COLUMN_NOTIFICATION_TYPE, push.getTypes().name());
        values.put(COLUMN_HOST_ID, push.getHostId());
        values.put(COLUMN_HOST_NAME, push.getHostName());
        values.put(COLUMN_IMAGE_ID, push.getImageId());
        values.put(COLUMN_SYSTEM_TIME, push.getSystemTime());
        values.put(COLUMN_READ, push.getRead());

        values.put(COLUMN_PUSH_CONTAINER, push.getPushContainer());
        return values;
    }

    public static ContentValues contentValuesCreator(PushAccepted pushAccepted) {

        final ContentValues values = new ContentValues();
        values.put(COLUMN_NOTIFICATION_TYPE, pushAccepted.getTypes().name());
        values.put(COLUMN_HOST_ID, pushAccepted.getHostId());
        values.put(COLUMN_HOST_NAME, pushAccepted.getHostName());
        values.put(COLUMN_IMAGE_ID, pushAccepted.getImageId());
        values.put(COLUMN_SYSTEM_TIME, pushAccepted.getSystemTime());
        values.put(COLUMN_READ, pushAccepted.getRead());

        values.put(COLUMN_FIRST_SONG_NAME, pushAccepted.getFirstSongName());
        values.put(COLUMN_SIZE, pushAccepted.getSize());
        return values;
    }

    public static Optional<Like> getLike(Cursor cursor) throws IllegalArgumentException {

        final String storedType = cursor.getString(1);
        if(TextUtils.isEmpty(storedType))
            throw new IllegalArgumentException("Wrong Type ! type is empty");
        if(!storedType.equals(Types.LIKE.name()))
            throw new IllegalArgumentException("Wrong Type ! type is " + storedType);

        final Like like = new Like();
        like.setTypes(Types.LIKE);
        like.setHostId(cursor.getLong(2));
        like.setHostName(cursor.getString(3));
        like.setImageId(cursor.getString(4));
        like.setSystemTime(cursor.getLong(5));
        like.setRead(cursor.getShort(6));

        like.setSongName(cursor.getString(7));
        return Optional.of(like);
    }

    public static Optional<Push> getPush(Cursor cursor) throws IllegalArgumentException {

        final String storedType = cursor.getString(1);
        if(TextUtils.isEmpty(storedType))
            throw new IllegalArgumentException("Wrong Type ! type is empty");
        if(!storedType.equals(Types.PUSH.name()))
            throw new IllegalArgumentException("Wrong Type ! type is " + storedType);

        final Push push = new Push();
        push.setTypes(Types.PUSH);
        push.setHostId(cursor.getLong(2));
        push.setHostName(cursor.getString(3));
        push.setImageId(cursor.getString(4));
        push.setSystemTime(cursor.getLong(5));
        push.setRead(cursor.getShort(6));

        push.setPushContainer(cursor.getString(8));
        return Optional.of(push);
    }

    public static Optional<BecameFriends> getBecameFriends(Cursor cursor) throws IllegalArgumentException {

        final String storedType = cursor.getString(1);
        if(TextUtils.isEmpty(storedType))
            throw new IllegalArgumentException("Wrong Type ! type is empty");
        if(!storedType.equals(Types.BECAME_FRIENDS.name()))
            throw new IllegalArgumentException("Wrong Type ! type is " + storedType);

        final BecameFriends becameFriends = new BecameFriends();
        becameFriends.setTypes(Types.BECAME_FRIENDS);
        becameFriends.setHostId(cursor.getLong(2));
        becameFriends.setHostName(cursor.getString(3));
        becameFriends.setImageId(cursor.getString(4));
        becameFriends.setSystemTime(cursor.getLong(5));
        becameFriends.setRead(cursor.getShort(6));
        return Optional.of(becameFriends);
    }

    public static Optional<PushAccepted> getPushAccepted(Cursor cursor) throws IllegalArgumentException {

        final String storedType = cursor.getString(1);
        if(TextUtils.isEmpty(storedType))
            throw new IllegalArgumentException("Wrong Type ! type is empty");
        if(!storedType.equals(Types.PUSH_ACCEPTED.name()))
            throw new IllegalArgumentException("Wrong Type ! type is " + storedType);

        final PushAccepted pushAccepted = new PushAccepted();
        pushAccepted.setTypes(Types.PUSH_ACCEPTED);
        pushAccepted.setHostId(cursor.getLong(2));
        pushAccepted.setHostName(cursor.getString(3));
        pushAccepted.setImageId(cursor.getString(4));
        pushAccepted.setSystemTime(cursor.getLong(5));
        pushAccepted.setRead(cursor.getShort(6));

        pushAccepted.setFirstSongName(cursor.getString(9));
        pushAccepted.setSize(cursor.getInt(10));
        return Optional.of(pushAccepted);
    }

    public ReachNotificationsHelper (Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(ReachAlbumHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + NOTIFICATIONS_TABLE);
        onCreate(db);
    }
}