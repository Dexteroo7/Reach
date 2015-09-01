package reach.project.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.common.base.Optional;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.squareup.wire.Wire;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

import reach.project.utils.auxiliaryClasses.MusicList;
import reach.project.music.albums.Album;
import reach.project.music.artists.Artist;
import reach.project.utils.auxiliaryClasses.UploadProgress;

/**
 * Created by Dexter on 28-03-2015.
 */
public enum CloudStorageUtils {
    ;

    private static final String APPLICATION_NAME_PROPERTY = "Reach";
    private static final String ACCOUNT_ID_PROPERTY = "528178870551-a2qc6pb788d3djjmmult1lkloc65rgt4@developer.gserviceaccount.com";
    private static final String BUCKET_NAME_IMAGES = "able-door-616-images";
    private static final String BUCKET_NAME_MUSIC_DATA = "able-door-616-music-data";
//    private final String PROJECT_ID_PROPERTY = "able-door-616";

    public static void uploadFile(final File file, InputStream key, UploadProgress uploadProgress) {

        //get file name
        String fileName;
        try {
            fileName = Files.hash(file, Hashing.md5()).toString();
        } catch (IOException e) {
            e.printStackTrace();
            fileName = null;
        }
        if (TextUtils.isEmpty(fileName)) {
            uploadProgress.error();
            return;
        }

        //get storage object
        final Optional<Storage> optional = getStorage(key);
        if (!optional.isPresent()) {
            uploadProgress.error();
            return;
        }

        final Storage storage = optional.get();

        //check if file is present
        try {
            storage.objects().get(BUCKET_NAME_IMAGES, fileName).execute();
            Log.i("Ayush", "File found" + fileName);
            uploadProgress.success(fileName);
            return; //quit since found
        } catch (IOException e) {

            //file not present, hence the error, we upload
            e.printStackTrace();
        }

        //prepare upload
        Log.i("Ayush", "File not found, Uploading " + fileName);
        final Storage.Objects.Insert insert;
        final AbstractInputStreamContent content = new AbstractInputStreamContent("image/") {

            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(file); //can be retried
            }

            @Override
            public long getLength() throws IOException {
                return file.length();
            }

            @Override
            public boolean retrySupported() {
                return true;
            }
        };

        try {
            insert = storage.objects().insert(BUCKET_NAME_IMAGES, null, content);
        } catch (IOException e) {

            e.printStackTrace();
            Log.i("Ayush", "Error while creating request" + fileName);
            uploadProgress.error();
            return; //quit if error
        }

        insert.setPredefinedAcl("publicRead");
        insert.setName(fileName);
        insert.getMediaHttpUploader().setDirectUploadEnabled(true);
        insert.getMediaHttpUploader().setProgressListener(uploadProgress);

        //upload
        final String md5;
        try {
            md5 = insert.execute().getMd5Hash();
        } catch (IOException e) {

            e.printStackTrace();
            Log.i("Ayush", "Error while uploading" + fileName);
            uploadProgress.error();
            return; //quit if error
        }

        Log.i("Ayush", "Upload complete " + md5);
        uploadProgress.success(fileName);
    }

//    public static void deleteFile(String fileName) {
//
//        getStorage();
//        try {
//            storage.objects().delete(BUCKET_NAME, fileName).execute();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public static void deleteAllFiles() {
//
//        getStorage();
//        List<String> stringList = listBucket(BUCKET_NAME);
//        for (String a : stringList) deleteFile(a);
//    }

//    public void createBucket(String bucketName) {
//
//        getStorage();
//        Bucket bucket = new Bucket();
//        bucket.setName(bucketName);
//        try {
//            storage.buckets().insert(PROJECT_ID_PROPERTY, bucket).execute();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public void deleteBucket(String bucketName) {
//
//        getStorage();
//        try {
//            storage.buckets().delete(bucketName).execute();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public static List<String> listBucket(String bucketName) {
//
//        final Storage storage = getStorage();
//        List<String> list = new ArrayList<>();
//        List<StorageObject> objects = null;
//        try {
//            objects = storage.objects().list(bucketName).execute().getItems();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if (objects != null) {
//            for (StorageObject o : objects) {
//                list.add(o.getName());
//            }
//        }
//        return list;
//    }

//    public List<String> listBuckets() {
//
//        getStorage();
//        List<String> list = new ArrayList<>();
//        List<Bucket> buckets = null;
//        try {
//            buckets = storage.buckets().list(PROJECT_ID_PROPERTY).execute().getItems() ;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if(buckets != null) {
//            for(Bucket b : buckets) {
//                list.add(b.getName());
//            }
//        }
//        return list;
//    }

    /**
     * Fetches music data from server and also inserts into database
     *
     * @param hostId  id of person to fetch from
     * @param context context to use
     * @return false : do not fetch music visibility
     * true : fetch music visibility
     */
    public static boolean getMusicData(long hostId, Context context) {

        if (!MiscUtils.isOnline(context))
            return false; //not online no use

        final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        final String fileName = MiscUtils.getMusicStorageKey(hostId);
        final String currentHash = SharedPrefUtils.getMusicHash(preferences, fileName);
        final boolean toReturn = !TextUtils.isEmpty(currentHash); //if current hash present, return true !

        //get cloud key
        final InputStream stream;
        try {
            stream = context.getAssets().open("key.p12");
        } catch (IOException e) {
            e.printStackTrace();
            return toReturn; //fail but fetch visibility if music already present
        }

        //prepare storage object
        final Storage storage = getStorage(stream).orNull();
        MiscUtils.closeQuietly(stream);
        if (storage == null)
            return toReturn; //fail but fetch visibility if music already present

        //getMd5Hash of Music data on storage
        String serverHash = "";
        try {
            serverHash = storage.objects().get(BUCKET_NAME_MUSIC_DATA, fileName).execute().getMd5Hash().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (TextUtils.isEmpty(serverHash)) {

            //TODO remove from local ?
//            final ContentResolver resolver = context.getContentResolver();
//            if (resolver == null)
//                return false; //application died probably,
//            MiscUtils.deleteSongs(hostId, resolver);
//            MiscUtils.deletePlayLists(hostId, resolver);
//            SharedPrefUtils.removeMusicHash(preferences, fileName);
//            return false; //no music found !
            return toReturn;
        }

//        if (currentHash.equals(serverHash))
//            return toReturn; //same music found, but still verify for music visibility

        InputStream download = null;
        GZIPInputStream compressedData = null;

        final MusicList musicList;
        try {

            download = storage.objects().get(BUCKET_NAME_MUSIC_DATA, fileName).executeMediaAsInputStream();
            compressedData = new GZIPInputStream(download);
            musicList = new Wire(MusicList.class).parseFrom(compressedData, MusicList.class);
        } catch (IOException e) {
            e.printStackTrace();
            return toReturn; //fail but fetch visibility if music already present
        } finally {
            MiscUtils.closeQuietly(download, compressedData);
        }

        final ContentResolver resolver = context.getContentResolver();
        if (resolver == null)
            return false; //application died probably,

        //proceed if alive
        if (musicList.song == null || musicList.song.isEmpty()) {

            //All the music got deleted
            MiscUtils.deleteSongs(hostId, resolver);
            MiscUtils.deletePlayLists(hostId, resolver);
            //Get rid of the hash
            SharedPrefUtils.removeMusicHash(preferences, fileName);
            return false; //no songs found !
        }

        //first update the hash
        SharedPrefUtils.storeMusicHash(preferences, fileName, serverHash);

        final Pair<ArrayMap<String, Album>, ArrayMap<String, Artist>> pair =
                MiscUtils.getAlbumsAndArtists(musicList.song, hostId);
        MiscUtils.bulkInsertSongs(
                musicList.song,
                pair.first,
                pair.second,
                resolver,
                hostId);

        if (musicList.playlist == null || musicList.playlist.isEmpty())
            //All playLists got deleted
            MiscUtils.deletePlayLists(hostId, resolver);
        else
            MiscUtils.bulkInsertPlayLists(musicList.playlist, resolver, hostId);

        return true; //all good, check for visibility as well
    }

    /**
     * Uploads the Music data to google cloud storage
     *
     * @param musicData bytes of Music data
     * @param fileName  the name of file (@MiscUtils.getMusicStorageKey())
     * @param key       the cloud storage key as input stream
     */
    public static boolean uploadMusicData(byte[] musicData, final String fileName, InputStream key) {

        //prepare storage object
        final Storage storage = getStorage(key).orNull();
        MiscUtils.closeQuietly(key);
        if (storage == null)
            return true; //error, but sync with local sql

        //compute hash of current Music data
        final String currentHash = Base64.encodeToString(Hashing.md5().newHasher()
                .putBytes(musicData)
                .hash().asBytes(), Base64.DEFAULT).trim();
        Log.i("Ayush", "Current hash = " + currentHash);

        //getMd5Hash of Music data on storage
        String hash = "";
        try {
            hash = storage.objects().get(BUCKET_NAME_MUSIC_DATA, fileName).execute().getMd5Hash().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("Ayush", "Server hash = " + hash);

        //compare hash
        if (currentHash.equals(hash))
            return false; //hash was same

        //file not present OR old
        Log.i("Ayush", "File not found, Uploading " + fileName);
        final Storage.Objects.Insert insert;
        final AbstractInputStreamContent content = new AbstractInputStreamContent("application/octet-stream") {

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(musicData); //can be retried
            }

            @Override
            public long getLength() throws IOException {
                return musicData.length;
            }

            @Override
            public boolean retrySupported() {
                return true;
            }
        };

        try {
            insert = storage.objects().insert(BUCKET_NAME_MUSIC_DATA, null, content);
        } catch (IOException e) {

            e.printStackTrace();
            Log.i("Ayush", "Error while creating request" + fileName);
            return true; //error, but sync with local sql
        }

        insert.setPredefinedAcl("publicRead");
        insert.setName(fileName);
        insert.getMediaHttpUploader().setDirectUploadEnabled(true);
//        insert.getMediaHttpUploader().setProgressListener(uploadProgress);

        //upload
        final String md5;
        try {
            md5 = insert.execute().getMd5Hash();
        } catch (IOException e) {

            e.printStackTrace();
            Log.i("Ayush", "Error while uploading" + fileName);
//            uploadProgress.error();
            return true; //error, but sync with local sql
        }

        Log.i("Ayush", "Upload complete " + md5);
//        uploadProgress.success(fileName);
        return true; //success, sync with local
    }

    private static Optional<Storage> getStorage(InputStream stream) {

        final HttpTransport transport = new NetHttpTransport();
        final JsonFactory factory = new JacksonFactory();
        final HttpRequestInitializer initializer = request -> {
            request.setConnectTimeout(request.getConnectTimeout() * 2);
            request.setReadTimeout(request.getReadTimeout() * 2);
        };

        final PrivateKey key;

        try {
            key = SecurityUtils.loadPrivateKeyFromKeyStore(
                    SecurityUtils.getPkcs12KeyStore(),
                    stream,
                    "notasecret", "privatekey", "notasecret");
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            try {
                transport.shutdown();
            } catch (IOException ignored) {
            }
            MiscUtils.closeQuietly(stream);
            return Optional.absent();
        }

        final GoogleCredential googleCredential = new GoogleCredential.Builder()
                .setTransport(transport)
                .setJsonFactory(factory)
                .setServiceAccountId(ACCOUNT_ID_PROPERTY)
                .setServiceAccountPrivateKey(key)
                .setServiceAccountScopes(Collections.singletonList(StorageScopes.DEVSTORAGE_FULL_CONTROL))
                .setRequestInitializer(initializer).build();

        return Optional.of(new Storage.Builder(transport, factory, googleCredential)
                .setApplicationName(APPLICATION_NAME_PROPERTY)
                .build());
    }
}