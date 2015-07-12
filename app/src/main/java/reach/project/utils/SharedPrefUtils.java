package reach.project.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import reach.backend.entities.userApi.model.ReachUser;
import reach.project.reachProcess.auxiliaryClasses.MusicData;

/**
 * Created by Dexter on 28-03-2015.
 */
public enum SharedPrefUtils {
    ;

    //TODO centralize all keys,

    public static String getDeviceId (Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static void storeReachUser (SharedPreferences.Editor editor, ReachUser reachUserDatabase) {

        editor.putString("phoneNumber", reachUserDatabase.getPhoneNumber())
                .putString("userName", reachUserDatabase.getUserName())
                .putString("deviceId", reachUserDatabase.getDeviceId())

                .putString("imageId", reachUserDatabase.getImageId())
                .putString("statusSong", reachUserDatabase.getStatusSong())
                .putString("genres", reachUserDatabase.getGenres())

                .putLong("megaBytesReceived", reachUserDatabase.getMegaBytesReceived())
                .putLong("megaBytesSent", reachUserDatabase.getMegaBytesSent())
                .putLong("serverId", reachUserDatabase.getId())
                .apply();
    }

    public static boolean isUserAbsent (SharedPreferences sharedPreferences) {

        return TextUtils.isEmpty(sharedPreferences.getString("userName", "")) ||
               TextUtils.isEmpty(sharedPreferences.getString("phoneNumber", "")) ||
               sharedPreferences.getLong("serverId", 0) == 0;
    }

    public static void storePhoneNumber (SharedPreferences sharedPreferences, String number) {
        sharedPreferences.edit().putString("phoneNumber", number).apply();
    }

    public static void storeInviteCode (SharedPreferences.Editor editor, String code) {
        editor.putString("inviteCode", code);
        editor.apply();
    }

    public static void storeGenres (SharedPreferences.Editor editor, String genres) {
        editor.putString("genres", genres);
        editor.apply();
    }

    public static void storeUserName (SharedPreferences editor, String userName) {
        editor.edit().putString("userName", userName).apply();
    }

    public static void storeImageId (SharedPreferences editor, String imageId) {
        editor.edit().putString("imageId", imageId).apply();
    }

    public static void setReachQueueSeen (SharedPreferences.Editor editor) {
        editor.putBoolean("reachQueueSeen", true);
        editor.apply();
    }

    public static void setFirstIntroSeen (SharedPreferences.Editor editor) {
        editor.putBoolean("firstIntroSeen", true);
        editor.apply();
    }

    public static void setSecondIntroSeen (SharedPreferences.Editor editor) {
        editor.putBoolean("secondIntroSeen", true);
        editor.apply();
    }

    public static void setDataOn (SharedPreferences.Editor editor) {
        editor.putBoolean("mobileDataOn", true);
        editor.apply();
    }

    public static void setDataOff (SharedPreferences.Editor editor) {
        editor.putBoolean("mobileDataOn", false);
        editor.apply();
    }

    public static void storeLastPlayed (SharedPreferences.Editor editor, String data) {
        editor.putString("last_played", data).apply();
    }

    public static boolean getMobileData (SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("mobileDataOn", false);
    }

    public static boolean getFirstIntroSeen (SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("firstIntroSeen", false);
    }

    public static boolean getSecondIntroSeen (SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("secondIntroSeen", false);
    }

    public static boolean getReachQueueSeen (SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("reachQueueSeen", false);
    }

    public static Optional<MusicData> getLastPlayed (SharedPreferences sharedPreferences) {
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

    public static long getServerId (SharedPreferences sharedPreferences) {
        return sharedPreferences.getLong("serverId", 0);
    }

//    public static String getPhoneNumber (SharedPreferences sharedPreferences) {
//        return sharedPreferences.getString("phoneNumber", "");
//    }

    public static String getUserNumber (SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("phoneNumber", "");
    }

    public static String getUserName (SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("userName", "");
    }

    public static String getImageId (SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("imageId", "");
    }

    public static String getInviteCode (SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("inviteCode", "");
    }

    public static int getSongCodeForUser (long userId, SharedPreferences sharedPreferences) {
        return sharedPreferences.getInt("song_hash" + userId, 0);
    }

    public static int getPlayListCodeForUser (long userId, SharedPreferences sharedPreferences) {
        return sharedPreferences.getInt("play_list_hash" + userId, 0);
    }

    public static void storeSongCodeForUser (long userId, int songId, SharedPreferences.Editor editor) {
        editor.putInt("song_hash" + userId, songId).apply();
    }

    public static void storePlayListCodeForUser (long userId, int playListId, SharedPreferences.Editor editor) {
        editor.putInt("play_list_hash" + userId, playListId).apply();
    }

    public static boolean toggleShuffle (SharedPreferences sharedPreferences) {

        final boolean currentValue = sharedPreferences.getBoolean("shuffle", false);
        sharedPreferences.edit().putBoolean("shuffle", !currentValue).apply();
        return !currentValue;
    }

    public static boolean getShuffle (SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("shuffle", false);
    }

    public static boolean toggleRepeat (SharedPreferences sharedPreferences) {
        final boolean currentValue = sharedPreferences.getBoolean("repeat", false);
        sharedPreferences.edit().putBoolean("repeat", !currentValue).apply();
        return !currentValue;
    }

    public static boolean getRepeat (SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("repeat", false);
    }
}
