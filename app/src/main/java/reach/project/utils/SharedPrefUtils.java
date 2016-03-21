package reach.project.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.wire.Wire;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import reach.project.music.Song;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.onBoarding.OnboardingData;

/**
 * Created by Dexter on 28-03-2015.
 */
public enum SharedPrefUtils {
    ;


    //TODO centralize all keys,

    private static final String TAG = SharedPrefUtils.class.getSimpleName();
    private static final String IS_A_SONG_CURRENTLY_PLAYING_KEY = "is_a_song_currently_playing";


    public synchronized static void storeReachUser(SharedPreferences sharedPreferences,
                                                   OnboardingData onboardingData,
                                                   String chatToken,
                                                   String imageId,
                                                   long serverId) {

        sharedPreferences.edit()
                .putString("phoneNumber", onboardingData.phoneNumber)
                .putString("userName", onboardingData.userName)
                .putString("deviceId", onboardingData.deviceId)
                .putString("coverImageId", onboardingData.coverPicUri)
                .putString("promoCode", onboardingData.promoCode)
                .putString("emailId", onboardingData.emailId)
                .putString("imageId", imageId)
                .putString("chatToken", chatToken)
                .putLong("serverId", serverId)
                .apply();
    }

    public synchronized static String getPhoneNumber(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("phoneNumber", "");
    }

    public synchronized static String getPromoCode(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("promoCode", "");
    }

    public synchronized static void storePromoCode(SharedPreferences sharedPreferences, String promoCode) {
        sharedPreferences.edit().putString("promoCode", promoCode).apply();
    }

    public synchronized static String getChatToken(SharedPreferences sharedPreferences) {

        final String chatToken = sharedPreferences.getString("chatToken", "");
        Log.i("Ayush", "Found chat token " + chatToken);
        return chatToken;
    }

    public synchronized static void storeChatToken(SharedPreferences sharedPreferences, String chatToken) {

        Log.i("Ayush", "Storing chat token " + chatToken);
        sharedPreferences.edit().putString("chatToken", chatToken).apply();
    }

    public synchronized static String getChatUUID(SharedPreferences sharedPreferences) {

        final String chatUUID = sharedPreferences.getString("chatUUID", "");
        Log.i("Ayush", "Found chatUUID " + chatUUID);
        return chatUUID;
    }

    public synchronized static void storeChatUUID(SharedPreferences sharedPreferences, String chatUUID) {

        Log.i("Ayush", "Storing chatUUID " + chatUUID);
        sharedPreferences.edit().putString("chatUUID", chatUUID).apply();
    }

    public synchronized static String getCloudStorageFileHash(SharedPreferences sharedPreferences, String fileName) {
        return sharedPreferences.getString(fileName, "");
    }

    public synchronized static void storeCloudStorageFileHash(SharedPreferences sharedPreferences, String fileName, String hash) {
        sharedPreferences.edit().putString(fileName, hash).apply();
    }

    public synchronized static void removeMusicHash(SharedPreferences sharedPreferences, String fileName) {
        sharedPreferences.edit().remove(fileName).apply();
    }

    public synchronized static void storePhoneNumber(SharedPreferences sharedPreferences, String number) {
        sharedPreferences.edit().putString("phoneNumber", number).apply();
    }

    public synchronized static void storeGenres(SharedPreferences editor, Set<String> genres) {
        editor.edit().putStringSet("genres", genres).apply();
    }

    public synchronized static void storeUserName(SharedPreferences sharedPreferences, String userName) {
        sharedPreferences.edit().putString("userName", userName).apply();
    }

    public synchronized static void storeImageId(SharedPreferences sharedPreferences, String imageId) {
        sharedPreferences.edit().putString("imageId", imageId).apply();
    }

    public synchronized static void storeCoverImageId(SharedPreferences sharedPreferences, String coverImageId) {
        sharedPreferences.edit().putString("coverImageId", coverImageId).apply();
    }

    public synchronized static void setReachQueueSeen(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("reachQueueSeen", true).apply();
    }

    public synchronized static void setFirstIntroSeen(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("firstIntroSeen", true).apply();
    }

    public synchronized static void setDataOn(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("mobileDataOn", true).apply();
    }

    public synchronized static void setDataOff(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("mobileDataOn", false).apply();
    }

    public synchronized static boolean getMobileData(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("mobileDataOn", true);
    }

    public synchronized static boolean getFirstIntroSeen(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("firstIntroSeen", false);
    }

    public synchronized static boolean getReachQueueSeen(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("reachQueueSeen", false);
    }

    public synchronized static long getServerId(SharedPreferences sharedPreferences) {
        return sharedPreferences.getLong("serverId", 0);
    }

    public synchronized static String getUserName(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("userName", "");
    }

    public synchronized static String getImageId(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("imageId", "");
    }

    public synchronized static String getCoverImageId(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("coverImageId", "");
    }

    public static synchronized void storeLastRequestTime(SharedPreferences preferences) {
        preferences.edit().putLong("lastRequestTime", System.currentTimeMillis()).apply();
    }

    public static long getLastRequestTime(SharedPreferences preferences) {
        return preferences.getLong("lastRequestTime", 0);
    }

    public synchronized static void setMyProfileCoach1Seen(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("myProfileCoach1", true).apply();
    }

    public synchronized static boolean getMyProfileCoach1Seen(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("myProfileCoach1", false);
    }

    public synchronized static void setFriendsCoach1Seen(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("friendsCoach1", true).apply();
    }

    public synchronized static boolean getFriendsCoach1Seen(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("friendsCoach1", false);
    }

    public synchronized static void setExploreCoach1Seen(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("exploreCoach1", true).apply();
    }

    public synchronized static boolean getExploreCoach1Seen(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("exploreCoach1", false);
    }

    public synchronized static void storeEmailId(SharedPreferences sharedPreferences, String emailId) {
        sharedPreferences.edit().putString("emailId", emailId).apply();
    }

    public synchronized static String getEmailId(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("emailId", "");
    }

    public synchronized static boolean isItFirstTimeDownload(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("first_time_download", true);
    }

    public synchronized static void putFirstTimeDownload(SharedPreferences sharedPreferences, boolean value) {
        sharedPreferences.edit().putBoolean("first_time_download", value).apply();
    }

    /*public synchronized static void toggleIsASongCurrentlyPlaying(SharedPreferences sharedPreferences, boolean b) {
        sharedPreferences.edit().putBoolean(IS_A_SONG_CURRENTLY_PLAYING_KEY, b).apply();
    }

    public synchronized static boolean getIsASongCurrentlyPlaying(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(IS_A_SONG_CURRENTLY_PLAYING_KEY, false);
    }*/

    ////////////////////////////////////

    public synchronized static boolean toggleRepeat(@Nonnull Context context) {

        RandomAccessFile randomAccessFile = null;

        try {
            randomAccessFile = new RandomAccessFile(context.getCacheDir() + "/" + "repeat_boolean", "rwd");
            final boolean newRepeat = randomAccessFile.length() <= 0 || !randomAccessFile.readBoolean();
            randomAccessFile.setLength(0);
            randomAccessFile.writeBoolean(newRepeat); //write new repeat
            return newRepeat;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            MiscUtils.closeQuietly(randomAccessFile);
        }
    }

    public static boolean getRepeat(@Nonnull Context context) {

        RandomAccessFile randomAccessFile = null;

        try {
            randomAccessFile = new RandomAccessFile(context.getCacheDir() + "/" + "repeat_boolean", "r");
            return randomAccessFile.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            MiscUtils.closeQuietly(randomAccessFile);
        }
    }

    ////////////////////////////////////

    public synchronized static boolean toggleShuffle(@Nonnull Context context) {

        RandomAccessFile randomAccessFile = null;

        try {
            randomAccessFile = new RandomAccessFile(context.getCacheDir() + "/" + "shuffle_boolean", "rwd");
            final boolean newShuffle = randomAccessFile.length() <= 0 || !randomAccessFile.readBoolean();
            randomAccessFile.setLength(0);
            randomAccessFile.writeBoolean(newShuffle); //write new shuffle
            return newShuffle;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            MiscUtils.closeQuietly(randomAccessFile);
        }
    }

    public static boolean getShuffle(@Nonnull Context context) {

        RandomAccessFile randomAccessFile = null;

        try {
            randomAccessFile = new RandomAccessFile(context.getCacheDir() + "/" + "shuffle_boolean", "r");
            return randomAccessFile.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            MiscUtils.closeQuietly(randomAccessFile);
        }
    }

    ////////////////////////////////////

    public static Optional<Song> getLastPlayed(@Nonnull Context context) {

        RandomAccessFile randomAccessFile = null;
        Song toReturn = null;

        try {

            randomAccessFile = new RandomAccessFile(context.getCacheDir() + "/" + "last_played", "r");
            final int size = randomAccessFile.readInt();
            final byte[] stuff = new byte[size];
            randomAccessFile.readFully(stuff); //read fully
            toReturn = new Wire(Song.class).parseFrom(stuff, Song.class);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.absent();
        } finally {
            MiscUtils.closeQuietly(randomAccessFile);
        }

        if (toReturn != null) {

            final Cursor cursor = context.getContentResolver().query(
                    SongProvider.CONTENT_URI,
                    new String[]{
                            SongHelper.COLUMN_PROCESSED,
                            SongHelper.COLUMN_SENDER_ID,
                            SongHelper.COLUMN_USER_NAME},
                    SongHelper.COLUMN_META_HASH + " = ?",
                    new String[]{toReturn.fileHash}, null);

            if (cursor == null)
                return Optional.of(toReturn);

            if (!cursor.moveToFirst()) {

                cursor.close();
                return Optional.absent();
            }

            toReturn.setProcessed(cursor.getLong(0));
            toReturn.setSenderId(cursor.getLong(1));
            toReturn.setSenderName(cursor.getString(2));
            return Optional.of(toReturn);
        }

        return Optional.absent();
    }

    public synchronized static void storeLastPlayed(@Nonnull Context context,
                                                    @Nonnull Song currentPlaying) {

        RandomAccessFile randomAccessFile = null;
        try {

            randomAccessFile = new RandomAccessFile(context.getCacheDir() + "/" + "last_played", "rwd");
            randomAccessFile.setLength(0);
            final byte[] bytes = currentPlaying.toByteArray();
            randomAccessFile.writeInt(bytes.length);
            randomAccessFile.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MiscUtils.closeQuietly(randomAccessFile);
        }
    }

    ////////////////////////////////////

    @Nonnull
    public synchronized static Map<String, Boolean> getPackageVisibilities(SharedPreferences sharedPreferences) {

        final String serializedString = sharedPreferences.getString("app_visibility", "");
        if (TextUtils.isEmpty(serializedString))
            return MiscUtils.getMap(0);

        final Map<String, Boolean> toReturn;
        try {
            toReturn = new Gson().fromJson(
                    serializedString,
                    new TypeToken<Map<String, Boolean>>() {
                    }.getType());
        } catch (Exception e) {
            e.printStackTrace();
            return MiscUtils.getMap(0);
        }

        return toReturn;
    }

//    public synchronized static void addPackageVisibility(SharedPreferences sharedPreferences,
//                                                         String packageName,
//                                                         boolean visibility) {
//
//        @Nullable
//        Map<String, Boolean> toSave = null;
//
//        final String serializedString = sharedPreferences.getString("app_visibility", "");
//        if (!TextUtils.isEmpty(serializedString))
//            try {
//                toSave = new Gson().fromJson(
//                        serializedString,
//                        new TypeToken<Map<String, Boolean>>() {
//                        }.getType());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        if (toSave == null)
//            toSave = MiscUtils.getMap(0);
//
//        toSave.put(packageName, visibility);
//
//        sharedPreferences.edit().putString("app_visibility",
//                new Gson().toJson(toSave, new TypeToken<Map<String, Boolean>>() {
//                }.getType())).apply();
//        Log.i("Ayush", "Saving new application visibility " + packageName + " " + visibility + " " + toSave.size());
//    }

    public synchronized static void overWritePackageVisibility(SharedPreferences sharedPreferences,
                                                               Map<String, Boolean> visibilities) {

        sharedPreferences.edit().putString("app_visibility",
                new Gson().toJson(visibilities, new TypeToken<Map<String, Boolean>>() {
                }.getType())).apply();
        Log.i("Ayush", "Saving new application visibility " + visibilities.size());
    }
}