package reach.project.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    public static void storeCampaignValues(SharedPreferences sharedPreferences, String campaignValues) {
        sharedPreferences.edit().putString("campaignValues", campaignValues).apply();
    }

    public static void storeCampaignTerms(SharedPreferences sharedPreferences, String campaignTerms) {
        sharedPreferences.edit().putString("campaignTerms", campaignTerms).apply();
    }

    public static void storeCampaignEmail(SharedPreferences sharedPreferences, String campaignEmail) {
        sharedPreferences.edit().putString("campaignEmail", campaignEmail).apply();
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

    public static boolean getMobileData(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("mobileDataOn", true);
    }

    public static boolean getFirstIntroSeen(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("firstIntroSeen", false);
    }

    public static boolean getReachQueueSeen(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("reachQueueSeen", false);
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

    public static String getCampaignValues(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("campaignValues", "");
    }

    public static String getCampaignTerms(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("campaignTerms", "");
    }

    public static String getCampaignEmail(SharedPreferences sharedPreferences) {
        return sharedPreferences.getString("campaignEmail", "");
    }

    //////////////////

    public static Optional<MusicData> getLastPlayed(Context context) {

        RandomAccessFile randomAccessFile = null;
        final byte[] stuff;

        try {
            randomAccessFile = new RandomAccessFile(context.getCacheDir() + "/" + "last_played", "r");
            final int size = randomAccessFile.readInt();
            stuff = new byte[size];
            randomAccessFile.readFully(stuff, 0, size); //read fully
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.absent();
        } finally {
            MiscUtils.closeQuietly(randomAccessFile);
        }

        return deserialize(stuff);
    }

    public synchronized static void storeLastPlayed(Context context, MusicData musicData) {

        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(context.getCacheDir() + "/" + "last_played", "rwd");
            randomAccessFile.setLength(0);
            final byte[] bytes = serialize(musicData);
            randomAccessFile.writeInt(bytes.length);
            randomAccessFile.write(bytes, 0, bytes.length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MiscUtils.closeQuietly(randomAccessFile);
        }
    }

    private static byte[] serialize(MusicData obj) throws IOException {

        ByteArrayOutputStream out = null;
        ObjectOutputStream os = null;
        try {

            out = new ByteArrayOutputStream();
            os = new ObjectOutputStream(out);
            os.writeObject(obj);
            return out.toByteArray();
        } finally {
            MiscUtils.closeQuietly(out, os);
        }
    }

    private static Optional<MusicData> deserialize(byte[] data) {


        ByteArrayInputStream in = null;
        ObjectInputStream is = null;

        try {
            in = new ByteArrayInputStream(data);
            is = new ObjectInputStream(in);
            return Optional.fromNullable((MusicData) is.readObject());
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return Optional.absent();
        } finally {
            MiscUtils.closeQuietly(in, is);
        }
    }

    //////////////////

    public synchronized static boolean toggleShuffle(Context context) {

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

    public static boolean getShuffle(Context context) {

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

    //////////////////

    public synchronized static boolean toggleRepeat(Context context) {

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

    public static boolean getRepeat(Context context) {

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

    //////////////////

    @Nonnull
    public static Map<String, Boolean> getPackageVisibilities(SharedPreferences sharedPreferences) {

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

    public synchronized static void addPackageVisibility(SharedPreferences sharedPreferences,
                                                         String packageName,
                                                         boolean visibility) {

        @Nullable
        Map<String, Boolean> toSave = null;

        final String serializedString = sharedPreferences.getString("app_visibility", "");
        if (!TextUtils.isEmpty(serializedString))
            try {
                toSave = new Gson().fromJson(
                        serializedString,
                        new TypeToken<Map<String, Boolean>>() {
                        }.getType());
            } catch (Exception e) {
                e.printStackTrace();
            }

        if (toSave == null)
            toSave = MiscUtils.getMap(0);

        toSave.put(packageName, visibility);

        sharedPreferences.edit().putString("app_visibility",
                new Gson().toJson(toSave, new TypeToken<Map<String, Boolean>>() {
                }.getType())).apply();
        Log.i("Ayush", "Saving new application visibility " + packageName + " " + visibility + " " + toSave.size());
    }

    public synchronized static void overWritePackageVisibility(SharedPreferences sharedPreferences, Map<String, Boolean> visibilities) {

        sharedPreferences.edit().putString("app_visibility",
                new Gson().toJson(visibilities, new TypeToken<Map<String, Boolean>>() {
                }.getType())).apply();
        Log.i("Ayush", "Saving new application visibility " + visibilities.size());
    }
}
