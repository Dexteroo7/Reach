package reach.project.utils;

import android.content.Context;
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

    public static void setDataOn(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("mobileDataOn", true).apply();
    }

    public static void setDataOff(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("mobileDataOn", false).apply();
    }

    public static void storeLastPlayed(Context context, String data) {
        context.getSharedPreferences("reach_process", Context.MODE_PRIVATE).edit().putString("last_played", data).apply();
    }

    public static boolean getMobileData(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("mobileDataOn", true);
    }

    public static boolean getFirstIntroSeen(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("firstIntroSeen", false);
    }

    public static boolean getReachQueueSeen(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("reachQueueSeen", false);
    }

    public static Optional<MusicData> getLastPlayed(Context context) {
        
        final String unParsed = context.getSharedPreferences("reach_process", Context.MODE_PRIVATE).getString("last_played", "");
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

    public static String getUserNumber(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("phoneNumber", "");
    }

    public static String getUserName(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("userName", "");
    }

    public static String getImageId(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("imageId", "");
    }

    public static boolean toggleShuffle(Context context) {

        final SharedPreferences sharedPreferences = context.getSharedPreferences("reach_process", Context.MODE_PRIVATE);
        final boolean currentValue = sharedPreferences.getBoolean("shuffle", false);
        sharedPreferences.edit().putBoolean("shuffle", !currentValue).apply();
        return !currentValue;
    }

    public static boolean getShuffle(Context context) {

        final SharedPreferences sharedPreferences = context.getSharedPreferences("reach_process", Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean("shuffle", false);
    }

    public static boolean toggleRepeat(Context context) {
        
        final SharedPreferences sharedPreferences = context.getSharedPreferences("reach_process", Context.MODE_PRIVATE);
        final boolean currentValue = sharedPreferences.getBoolean("repeat", false);
        sharedPreferences.edit().putBoolean("repeat", !currentValue).apply();
        return !currentValue;
    }

    public static boolean getRepeat(Context context) {
        return context.getSharedPreferences("reach_process", Context.MODE_PRIVATE).getBoolean("repeat", false);
    }
}
