package reach.project.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.squareup.wire.Wire;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import reach.project.apps.App;
import reach.project.apps.AppList;
import reach.project.core.StaticData;
import reach.project.music.MusicList;
import reach.project.music.Song;
import reach.project.utils.ancillaryClasses.UploadProgress;
import reach.project.utils.ancillaryClasses.UseContext;

/**
 * Created by Dexter on 28-03-2015.
 */
public enum CloudStorageUtils {
    ;

    public static byte APP = 0;
    public static byte MUSIC = 1;

    private static final String APPLICATION_NAME_PROPERTY = "Reach";
    private static final String ACCOUNT_ID_PROPERTY = "528178870551-a2qc6pb788d3djjmmult1lkloc65rgt4@developer.gserviceaccount.com";
    private static final String BUCKET_NAME_IMAGES = "able-door-616-images";
    private static final String BUCKET_NAME_MUSIC_DATA = "able-door-616-music-data";
    private static final String BUCKET_NAME_APP_DATA = "able-door-616-app-data";
//    private final String PROJECT_ID_PROPERTY = "able-door-616";

    public static void uploadImage(@NonNull File file,
                                   @NonNull InputStream key,
                                   @NonNull UploadProgress uploadProgress) {

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
     * Uploads the Music data to google cloud storage
     *
     * @param metadata bytes of meta data (un-compressed)
     * @param fileName the name of file
     * @param key      the cloud storage key as input stream
     */
    public static boolean uploadMetaData(
            @NonNull byte[] metadata,
            @NonNull String fileName,
            @NonNull InputStream key,
            byte type) {

        final byte[] musicData;
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(metadata.length);
        GZIPOutputStream gzipOutputStream = null;

        try {
            gzipOutputStream = new GZIPOutputStream(outputStream);
            gzipOutputStream.write(metadata);
            gzipOutputStream.close();
            musicData = outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return true; //error, but sync with local sql
        } finally {
            MiscUtils.closeQuietly(outputStream, gzipOutputStream);
        }

        Log.i("Ayush", "Compression ratio " + (musicData.length * 100) / metadata.length);
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

        //getMd5Hash of meta data on storage
        final String bucketToUse;
        if (type == APP)
            bucketToUse = BUCKET_NAME_APP_DATA;
        else if (type == MUSIC)
            bucketToUse = BUCKET_NAME_MUSIC_DATA;
        else
            throw new IllegalArgumentException("Un-expected type");

        String hash = "";
        try {
            hash = storage.objects().get(bucketToUse, fileName).execute().getMd5Hash().trim();
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
            insert = storage.objects().insert(bucketToUse, null, content);
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

    public static boolean isNewMusicAvailable(long userId, InputStream key, String localHash) {

        final String fileName = MiscUtils.getMusicStorageKey(userId);
        final Optional<Storage> optional = getStorage(key);
        if (!optional.isPresent())
            return false;

        final Storage storage = optional.get();
        final Storage.Objects.Get get;
        try {
            get = storage.objects().get(BUCKET_NAME_MUSIC_DATA, fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        get.getRequestHeaders().setCacheControl("no-cache");
        get.getMediaHttpDownloader().setDirectDownloadEnabled(true);

        final String serverHash;
        try {
            serverHash = get.execute().getMd5Hash();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Log.i("Ayush", "New songs for " + userId + " ? " + !localHash.equals(serverHash));
        return !localHash.equals(serverHash);
    }

    public static List<Song> fetchSongs(long userId, WeakReference<Context> reference) {

        final String fileName = MiscUtils.getMusicStorageKey(userId);
        Log.i("Ayush", "Fetching songs for " + fileName);

        //get storage object
        final InputStream key = MiscUtils.useContextFromContext(reference, context -> {
            try {
                return context.getAssets().open("key.p12");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).orNull();
        final Optional<Storage> optional = getStorage(key);
        if (!optional.isPresent()) {
            return Collections.emptyList();
        }

        final Storage storage = optional.get();
        final String serverHash;
        final MusicList musicList;

        try {

            final Storage.Objects.Get get = storage.objects().get(BUCKET_NAME_MUSIC_DATA, fileName);
            get.getRequestHeaders().setCacheControl("no-cache");
            get.getMediaHttpDownloader().setDirectDownloadEnabled(true);

            serverHash = get.execute().getMd5Hash();
            if (TextUtils.isEmpty(serverHash))
                throw new IOException("Response md5 was null");

            final BufferedInputStream bufferedInputStream = new BufferedInputStream(get.executeMediaAsInputStream());
            final GZIPInputStream compressedData = new GZIPInputStream(bufferedInputStream);
            musicList = new Wire(MusicList.class).parseFrom(compressedData, MusicList.class);
            MiscUtils.closeQuietly(bufferedInputStream, compressedData);

        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList(); //fail
        }

        //store the hash
        final List<Song> songList = MiscUtils.useContextFromContext(reference, (UseContext<List<Song>, Context>) context -> {

            final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
            if (musicList.song == null || musicList.song.isEmpty()) {
                //All the music got delete, get rid of the hash
                SharedPrefUtils.removeMusicHash(preferences, fileName);
                return Collections.emptyList(); //no songs found !
            }

            //first update the hash
            SharedPrefUtils.storeCloudStorageFileHash(preferences, fileName, serverHash);
            //song must not be null and must be visible
            return Lists.newArrayList(Iterables.filter(musicList.song, song -> song != null && song.visibility));

        }).or(Collections.emptyList());

        //if dead/empty return empty
        if (songList == null || songList.isEmpty())
            return Collections.emptyList();

        //sort by name and return
        Collections.sort(songList, (lhs, rhs) -> {

            final String d1 = TextUtils.isEmpty(lhs.displayName) ? "" : lhs.displayName;
            final String d2 = TextUtils.isEmpty(rhs.displayName) ? "" : rhs.displayName;
            return d1.compareToIgnoreCase(d2);
        });

        return songList;
    }

    public static boolean isNewAppAvailable(long userId, InputStream key, String localHash) {

        final String fileName = MiscUtils.getAppStorageKey(userId);
        final Optional<Storage> optional = getStorage(key);
        if (!optional.isPresent())
            return false;

        final Storage storage = optional.get();
        final Storage.Objects.Get get;
        try {
            get = storage.objects().get(BUCKET_NAME_APP_DATA, fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        get.getRequestHeaders().setCacheControl("no-cache");
        get.getMediaHttpDownloader().setDirectDownloadEnabled(true);

        final String serverHash;
        try {
            serverHash = get.execute().getMd5Hash();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Log.i("Ayush", "New apps for " + userId + " ? " + !localHash.equals(serverHash));
        return !localHash.equals(serverHash);
    }

    public static List<App> fetchApps(long userId, WeakReference<Context> reference) {

        final String fileName = MiscUtils.getAppStorageKey(userId);
        Log.i("Ayush", "Fetching apps for " + fileName);

        //get storage object
        final InputStream key = MiscUtils.useContextFromContext(reference, context -> {
            try {
                return context.getAssets().open("key.p12");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).orNull();
        //get storage object
        final Optional<Storage> optional = getStorage(key);
        if (!optional.isPresent()) {
            return Collections.emptyList();
        }

        final Storage storage = optional.get();

        final String serverHash;
        final AppList appList;

        try {

            final Storage.Objects.Get get = storage.objects().get(BUCKET_NAME_APP_DATA, fileName);
            get.getRequestHeaders().setCacheControl("no-cache");
            get.getMediaHttpDownloader().setDirectDownloadEnabled(true);

            serverHash = get.execute().getMd5Hash();
            if (TextUtils.isEmpty(serverHash))
                throw new IOException("Response md5 was null");

            final BufferedInputStream bufferedInputStream = new BufferedInputStream(get.executeMediaAsInputStream());
            final GZIPInputStream compressedData = new GZIPInputStream(bufferedInputStream);
            appList = new Wire(AppList.class).parseFrom(compressedData, AppList.class);
            MiscUtils.closeQuietly(bufferedInputStream, compressedData);

        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList(); //fail
        }

        //store the hash
        final List<App> apps = MiscUtils.useContextFromContext(reference, (UseContext<List<App>, Context>) context -> {

            final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
            if (appList.app == null || appList.app.isEmpty()) {
                //All the music got delete, get rid of the hash
                SharedPrefUtils.removeMusicHash(preferences, fileName);
                return Collections.emptyList(); //no songs found !
            }

            //first update the hash
            SharedPrefUtils.storeCloudStorageFileHash(preferences, fileName, serverHash);
            //app must not be null and must be visible
            return Lists.newArrayList(Iterables.filter(appList.app, input -> input != null && input.visible));

        }).or(Collections.emptyList());

        //if dead return empty
        if (apps == null || apps.isEmpty())
            return Collections.emptyList();

        //sort by name and return
        Collections.sort(apps, StaticData.byName);
        return apps;
    }

    @Nullable
    private static Storage storage = null;

    private static Optional<Storage> getStorage(@Nullable InputStream stream) {

        if (stream == null)
            return Optional.absent();

        if (storage != null) {
            MiscUtils.closeQuietly(stream);
            return Optional.of(storage);
        }

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

        return Optional.of(storage = new Storage.Builder(transport, factory, googleCredential)
                .setApplicationName(APPLICATION_NAME_PROPERTY)
                .build());
    }
}