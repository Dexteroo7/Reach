package reach.project.utils;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Set;

import reach.backend.entities.userApi.model.ReachUser;
import reach.project.reachProcess.auxiliaryClasses.MusicData;

/**
 * Created by Dexter on 28-03-2015.
 */
public enum SharedPrefUtils {
    ;

    //TODO centralize all keys,

    public static void storeReachUser(SharedPreferences sharedPreferences, ReachUser reachUserDatabase) {

        sharedPreferences.edit().putString("phoneNumber", reachUserDatabase.getPhoneNumber())
                .putString("userName", reachUserDatabase.getUserName())
                .putString("deviceId", reachUserDatabase.getDeviceId())
                .putString("imageId", reachUserDatabase.getImageId())
                .putString("chatToken", reachUserDatabase.getChatToken())
                .putLong("serverId", reachUserDatabase.getId())
                .apply();
    }

    public static void storeAuthKey(SharedPreferences sharedPreferences, String key) {
        sharedPreferences.edit().putString("otpAuthKey", key).apply();
    }

    public static String getAuthKey(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("otpAuthKey", "");
    }


    public static String getPhoneNumber(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("phoneNumber", "");
    }

    public static String getPromoCode(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("promoCode", "");
    }

    public static void storePromoCode(SharedPreferences sharedPreferences, String promoCode) {
        sharedPreferences.edit().putString("promoCode", promoCode).apply();
    }

    public static String getChatToken(SharedPreferences sharedPreferences) {

        final String chatToken = sharedPreferences.getString("chatToken", "");
        Log.i("Ayush", "Found chat token " + chatToken);
        return chatToken;
    }

    public static void storeChatToken(SharedPreferences sharedPreferences, String chatToken) {

        Log.i("Ayush", "Storing chat token " + chatToken);
        sharedPreferences.edit().putString("chatToken", chatToken).apply();
    }

    public static String getChatUUID(SharedPreferences sharedPreferences) {

        final String chatUUID = sharedPreferences.getString("chatUUID", "");
        Log.i("Ayush", "Found chatUUID " + chatUUID);
        return chatUUID;
    }

    public static void storeChatUUID(SharedPreferences sharedPreferences, String chatUUID) {

        Log.i("Ayush", "Storing chatUUID " + chatUUID);
        sharedPreferences.edit().putString("chatUUID", chatUUID).apply();
    }

    /**
     *
     * @param sharedPreferences for accessing the prefs
     * @return 0 : start numberVerification, 1 : start account creation, 2 : OK normal
     */
    public static short isUserAbsent(SharedPreferences sharedPreferences) {

        if(TextUtils.isEmpty(sharedPreferences.getString("phoneNumber", "")))
            return 0;

        if(TextUtils.isEmpty(sharedPreferences.getString("userName", "")) ||
                sharedPreferences.getLong("serverId", 0) == 0)
            return 1;

        return 2;
    }

    public static String getMusicHash(SharedPreferences sharedPreferences, String fileName) {
        return sharedPreferences.getString(fileName, "");
    }

    public static void storeMusicHash(SharedPreferences sharedPreferences, String fileName, String hash) {
        sharedPreferences.edit().putString(fileName, hash).apply();
    }

    public static void removeMusicHash(SharedPreferences sharedPreferences, String fileName) {
        sharedPreferences.edit().remove(fileName).apply();
    }

    public static void storePhoneNumber(SharedPreferences sharedPreferences, String number) {
        sharedPreferences.edit().putString("phoneNumber", number).apply();
    }

    public static void storeGenres(SharedPreferences editor, Set<String> genres) {
        editor.edit().putStringSet("genres", genres).apply();
    }

    public static void storeUserName(SharedPreferences sharedPreferences, String userName) {
        sharedPreferences.edit().putString("userName", userName).apply();
    }

    public static void storeImageId(SharedPreferences sharedPreferences, String imageId) {
        sharedPreferences.edit().putString("imageId", imageId).apply();
    }

    public static void setReachQueueSeen(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("reachQueueSeen", true).apply();
    }

    public static void setFirstIntroSeen(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("firstIntroSeen", true).apply();
    }

    public static void setSecondIntroSeen(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("secondIntroSeen", true).apply();
    }

    public static void setDataOn(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("mobileDataOn", true).apply();
    }

    public static void setDataOff(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("mobileDataOn", false).apply();
    }

    public static void storeLastPlayed(SharedPreferences sharedPreferences, String data) {
        sharedPreferences.edit().putString("last_played", data).apply();
    }

    public static boolean getMobileData (SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("mobileDataOn", true);
    }

    public static boolean getFirstIntroSeen(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("firstIntroSeen", false);
    }

    public static boolean getSecondIntroSeen(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("secondIntroSeen", false);
    }

    public static boolean getReachQueueSeen(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("reachQueueSeen", false);
    }

    public static Optional<MusicData> getLastPlayed(SharedPreferences sharedPreferences) {
        final String unParsed = sharedPreferences.getString("last_played", "");
        if (TextUtils.isEmpty(unParsed))
            return Optional.absent();
        final MusicData toReturn;
        try {
            toReturn = new Gson().fromJson(unParsed, MusicData.class);
        } catch (IllegalStateException | JsonSyntaxException e) {
            e.printStackTrace();
            return Optional.absent();
        }
        return Optional.of(toReturn);
    }

    public static long getServerId(SharedPreferences sharedPreferences) {
        return sharedPreferences.getLong("serverId", 0);
    }

//    public static String getPhoneNumber (SharedPreferences sharedPreferences) {
//        return sharedPreferences.getString("phoneNumber", "");
//    }

    public static String getUserNumber(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("phoneNumber", "");
    }

    public static String getUserName(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("userName", "");
    }

    public static String getImageId(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("imageId", "");
    }

    public static int getSongCodeForUser(long userId, SharedPreferences sharedPreferences) {
        return sharedPreferences.getInt("song_hash" + userId, 0);
    }

    public static int getPlayListCodeForUser(long userId, SharedPreferences sharedPreferences) {
        return sharedPreferences.getInt("play_list_hash" + userId, 0);
    }

    public static void storeSongCodeForUser(long userId, int songId, SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putInt("song_hash" + userId, songId).apply();
    }

    public static void storePlayListCodeForUser(long userId, int playListId, SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putInt("play_list_hash" + userId, playListId).apply();
    }

    public static boolean toggleShuffle(SharedPreferences sharedPreferences) {

        final boolean currentValue = sharedPreferences.getBoolean("shuffle", false);
        sharedPreferences.edit().putBoolean("shuffle", !currentValue).apply();
        return !currentValue;
    }

    public static boolean getShuffle(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("shuffle", false);
    }

    public static boolean toggleRepeat(SharedPreferences sharedPreferences) {
        final boolean currentValue = sharedPreferences.getBoolean("repeat", false);
        sharedPreferences.edit().putBoolean("repeat", !currentValue).apply();
        return !currentValue;
    }

    public static boolean getRepeat(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("repeat", false);
    }
}
