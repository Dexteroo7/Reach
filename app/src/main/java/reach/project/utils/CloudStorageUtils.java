package reach.project.utils;

import android.util.Log;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.StorageObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;

import reach.project.core.StaticData;

/**
 * Created by Dexter on 28-03-2015.
 */
public enum CloudStorageUtils {;

    private static Storage storage = null;
    public static final String APPLICATION_NAME_PROPERTY = "Reach";
    public static final String ACCOUNT_ID_PROPERTY = "528178870551-a2qc6pb788d3djjmmult1lkloc65rgt4@developer.gserviceaccount.com";
    public static final String BUCKET_NAME = "able-door-616.appspot.com";
//    private final String PROJECT_ID_PROPERTY = "able-door-616";

    public static String uploadFile(final byte [] data, boolean returnNow) {

        if(data == null || data.length == 0) return "";

        final String fileName;

        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            //TODO reduce memory usage
            final byte [] digested = md.digest(data);
            final StringBuilder stringBuilder = new StringBuilder(digested.length);
            for (final byte aDigested : digested) {
                stringBuilder.append(Integer.toString((aDigested & 0xff) + 0x100, 16).substring(1));
            }
            fileName = stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }

        if(fileName.equals("")) return fileName;
        else {

            if (returnNow) {

                StaticData.threadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        verify(fileName, data, 0);
                    }
                });
                return fileName;
            } else {

                verify(fileName, data, 0);
                return fileName;
            }
        }
    }

    private static short genericUpload(final String fileName, final byte [] data, int retry) {
        try {
            getStorage();
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            final String contentType = URLConnection.guessContentTypeFromStream
                    (byteArrayInputStream);
            final InputStreamContent content = new InputStreamContent(contentType, byteArrayInputStream);

            final Storage.Objects.Insert insert = storage.objects().insert(BUCKET_NAME, null, content);
            insert.setPredefinedAcl("publicRead");
            insert.setName(fileName);

            try {
                Log.i("Ayush", "uploading photo size " + data.length);
                insert.execute();
            } catch (Exception e) {

                e.printStackTrace();
                byteArrayInputStream.close();
            } finally {
                byteArrayInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return verify(fileName, data, retry);
    }

    private static short verify(String fileName, byte[] data, int retry) {

        if(retry > 4) return 0;
        Log.i("Ayush", "Verifying upload of " + fileName + " " + retry++);
        getStorage();
        try {
            storage.objects().get(BUCKET_NAME, fileName).executeMediaAsInputStream();
        } catch (SSLPeerUnverifiedException e) {

            e.printStackTrace();
            return verify(fileName, data, retry);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Ayush", "Retrying upload of " + fileName);
            return genericUpload(fileName, data, retry);
        }
        return 1;
    }

//    public byte [] downloadFile(String fileName) {
//
//        try {
//            getStorage();
//            final ByteArrayOutputStream download = new ByteArrayOutputStream();
//            BufferedInputStream bufferedInputStream = null;
//            try {
//                Log.i("Ayush", "Attempting to download " + fileName);
//                Storage.Objects.Get get = storage.objects().get(StaticData.BUCKET_NAME, fileName);
//
//                bufferedInputStream = new BufferedInputStream(get.executeMediaAsInputStream());
//                byte [] bytes = new byte[5000];
//                int read;
//                int total = 0;
//                read = bufferedInputStream.read(bytes);
//                while(read != -1) {
//
//                    total += read;
//                    byte [] toCopy = new byte[read];
//                    System.arraycopy(bytes, 0, toCopy, 0, read);
//                    download.write(toCopy);
//                    read = bufferedInputStream.read(bytes);
//                }
//            } catch (GoogleJsonResponseException |
//                     UnknownHostException |
//                     SocketTimeoutException e) {
//
//                e.printStackTrace();
//                Log.i("Ayush", "Network error man");
//                download.close();
//                if (bufferedInputStream != null) {
//                    bufferedInputStream.close();
//                }
//                return null;
//            } catch(Exception e) {
//                e.printStackTrace();
//                Log.i("Ayush", "Retrying download");
//                download.close();
//                if (bufferedInputStream != null) {
//                    bufferedInputStream.close();
//                }
//                try {
//                    Thread.sleep(5000L);
//                } catch (InterruptedException e1) {
//                    e1.printStackTrace();
//                    return downloadFile(fileName);
//                }
//                return downloadFile(fileName);
//            } finally {
//                download.close();
//                if (bufferedInputStream != null) {
//                    bufferedInputStream.close();
//                }
//            }
//            Log.i("Ayush", "downloaded " + fileName + " " + download.size());
//            return download.toByteArray();
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.i("Ayush", "Retrying download");
//            return downloadFile(fileName);
//        }
//    }

    public static void deleteFile(String fileName) {

        getStorage();
        try {
            storage.objects().delete(BUCKET_NAME, fileName).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteAllFiles() {

        getStorage();
        List<String> stringList = listBucket(BUCKET_NAME);
        for(String a : stringList) deleteFile(a);
    }

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

    public static List<String> listBucket(String bucketName) {

        getStorage();
        List<String> list = new ArrayList<>();
        List<StorageObject> objects = null;
        try {
            objects = storage.objects().list(bucketName).execute().getItems();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(objects != null) {
            for(StorageObject o : objects) {
                list.add(o.getName());
            }
        }
        return list;
    }

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

    private static Storage getStorage() {

        if (storage == null) {

            HttpTransport httpTransport = new ApacheHttpTransport();
            JsonFactory jsonFactory = new JacksonFactory();

            try {

                storage = new Storage.Builder(
                        httpTransport,
                        jsonFactory,
                        new GoogleCredential.Builder()
                                .setTransport(httpTransport)
                                .setJsonFactory(jsonFactory)
                                .setServiceAccountId(ACCOUNT_ID_PROPERTY)
                                .setServiceAccountPrivateKeyFromP12File(StaticData.keyFile)
                                .setServiceAccountScopes(Collections.singletonList(StorageScopes.DEVSTORAGE_FULL_CONTROL))
                                .setRequestInitializer(new HttpRequestInitializer() {
                                    @Override
                                    public void initialize(HttpRequest request) throws IOException {
                                        request.setReadTimeout(request.getReadTimeout() * 2);
                                        request.setConnectTimeout(request.getConnectTimeout()*2);
                                        request.setNumberOfRetries(request.getNumberOfRetries()*2);
                                    }
                                }).build())
                        .setApplicationName(APPLICATION_NAME_PROPERTY)
                        .build();
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }
        }
        return storage;
    }
}