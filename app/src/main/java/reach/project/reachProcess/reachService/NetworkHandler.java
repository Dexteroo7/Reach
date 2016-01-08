package reach.project.reachProcess.reachService;

import android.app.Application;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.backend.entities.userApi.model.Friend;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.auxiliaryClasses.ReachTask;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.SongMetadata;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by Dexter on 18-05-2015.
 * This is an auto-close type parent thread.
 * Explicit close only if User requests, using kill (Exit)
 * <p>
 * Process all network operations on a single thread.
 * a) scan for new requests
 * b) process current network operations
 * c) kill sleeping operations and sync up with database
 */
class NetworkHandler extends ReachTask<NetworkHandler.NetworkHandlerInterface> {

    public NetworkHandler(NetworkHandlerInterface handlerInterface) {
        super(handlerInterface, Type.NETWORK);
    }

    public boolean submitMessage(String message) {
        return pendingNetworkRequests.offer(message);
    }

    public static final String ACTION_NETWORK_MESSAGE = "reach.project.reachProcess.reachService.NETWORK_MESSAGE";
    public static final String ACTION_NETWORK_LOST = "reach.project.reachProcess.reachService.ACTION_NETWORK_LOST";
    ////////////////////////////////// these need cleaning in sanitize()
    //prevent GC of sockets and channels
    private static final LongSparseArray<Channel> openChannels = new LongSparseArray<>(100); //needs closing
    //message holder
    private static final ConcurrentLinkedQueue<String> pendingNetworkRequests = new ConcurrentLinkedQueue<>();
    private static final ByteBuffer transferBuffer = ByteBuffer.allocateDirect(4096);
    private static final StringBuilder requestDefaults = new StringBuilder()
            .append("User-Agent: ").append(System.getProperty("http.agent")).append("\r\n") //user agent
            .append("Accept: */*\r\n") //accept type
            .append("Cache-Control: no-cache\r\n") //specify no cache
            .append("Referer: http://letsreach.co\r\n") //the Referer
            .append("Connection: Keep-Alive\r\n") //connection type
            .append("Host: s3.amazonaws.com\r\n"); //specify the host

    private File reachDirectory = null;
    private Selector networkManager = null; //needs closing
    private WifiManager.WifiLock wifiLock = null; //needs closing
    private ThreadPoolExecutor threadPool = null;
    private long lastOperation = 0;
    //////////////////////////////////

    @Override
    protected void sanitize() {

        Log.i("Downloader", "Downloader starting sanitize");
        kill.set(true);
        if (wifiLock != null && wifiLock.isHeld())
            wifiLock.release();
        wifiLock = null;
//        Log.i("Downloader", "Downloader wiFi lock released");
        reachDirectory = null;
        //////////////////////////////////
//        Log.i("Downloader", "Releasing network manager");
        if (networkManager != null)
            try {
                networkManager.close();
            } catch (IOException ignored) {
            }
//        Log.i("Downloader", "NetworkManager released");
        //////////////////////////////////
        for (int i = 0, size = openChannels.size(); i < size; i++)
            MiscUtils.closeQuietly(openChannels.valueAt(i));
        openChannels.clear();
//        Log.i("Downloader", "Downloader openChannels released");
        //////////////////////////////////
        if (threadPool != null)
            threadPool.shutdownNow();
        threadPool = null;
        //////////////////////////////////
        transferBuffer.clear();
        Log.i("Downloader", "Downloader cleaning up");
    }

    @Override
    protected void performTask() {

        Log.i("Downloader", "Network handler thread started");
        kill.set(false);
        //getDirectory
        if ((reachDirectory = LocalUtils.getDirectory()) == null &&
                (reachDirectory = LocalUtils.getDirectory()) == null) {
            LocalUtils.toast(
                    "File system error",
                    handlerInterface,
                    SharedPrefUtils.getServerId(handlerInterface.getSharedPreferences()));
            return;
        }
        //check space
        if (reachDirectory.getUsableSpace() < StaticData.MINIMUM_FREE_SPACE) {
            LocalUtils.toast(
                    "Insufficient space on sdCard",
                    handlerInterface,
                    SharedPrefUtils.getServerId(handlerInterface.getSharedPreferences()));
            return;
        }
        //get networkManager
        try {
            networkManager = Selector.open();
        } catch (IOException ignored) {
            LocalUtils.toast(
                    "Could not get networkManager",
                    handlerInterface,
                    SharedPrefUtils.getServerId(handlerInterface.getSharedPreferences()));
            return;
        }

        LocalUtils.sanitizeReachDirectory(handlerInterface, reachDirectory); //sanitizeReachDirectory
        (wifiLock = handlerInterface.getWifiManager().createWifiLock(WifiManager.WIFI_MODE_FULL, "network_lock")).acquire(); //lock wifi
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(5); //create thread pool
        //////////////////////////////////
        //////////////////////////////////
        long lastActive = System.currentTimeMillis(), currentTime;
        boolean kill = false, sleeping = false, taskAdded;
        Set<SelectionKey> keySet;
        Log.i("Downloader", "Initialization done");
        while (true) { //start

            final Optional<String> networkRequest = Optional.fromNullable(pendingNetworkRequests.poll());
            //Check for new request
            if (taskAdded = processTask(networkRequest))
                handlerInterface.updateNetworkDetails();
            //Now we decide whether to kill the service or not
            try {
                Thread.sleep(5L); //relaxation period
                sleeping = (networkManager.selectNow() == 0);
            } catch (IOException e) {
                e.printStackTrace();
                sleeping = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
                kill = true;
            } finally {

                currentTime = System.currentTimeMillis();
                if (sleeping && //no connections to service (sleeping)
                        !taskAdded && //no work that is getting added (no taskAdded)
                        threadPool.getQueue().isEmpty() && //no work that is in queue
                        threadPool.getActiveCount() == 0 &&  //no work that is in running
                        networkManager.keys().isEmpty() && //no active connections (network-manager has 0 keys)
                        currentTime - lastActive > 5000) { //finally a sleep allowance of 5 seconds
                    Log.i("Downloader", "Service can be killed now");
                    kill = true;
                } else {
                    //if we are still alive, kill dead keys and sync
                    runOperation(networkManager.keys(), handlerInterface);
                }
            }
            if (kill)
                break;

            if (sleeping)
                continue;
            lastActive = currentTime;
            //Ok so now we can play
            /**
             * NOTE: it is very important to remove all keys from ready set.
             * We must directly remove all the keys.
             * .selectedKeys() returns the keys that have changed.
             */
            keySet = networkManager.selectedKeys();
            for (SelectionKey selectionKey : keySet)
                handleOp(selectionKey);
            keySet.clear();
        }
        //////////////////////////////////end infinite loop
        //noinspection unchecked
//        runOperation(networkManager.keys(), handlerInterface); needed ?
        handlerInterface.removeNetwork();
        Log.i("Downloader", "ConnectionManager QUIT !");
        //automatic cleaning will be called here
    }

    /**
     * Message exchange scheme :
     * a) Send/Receive confirmation for pause, for both upload and download
     * b) Send/Receive confirmation for delete, for download
     * <p>
     * Uploader : send paused, get paused, get deleted/get bytes
     * Downloader : send paused, get paused, send deleted
     *
     * @return weather a work was added or not
     */
    private boolean processTask(Optional<String> networkRequest) {
        //////////////////////////////////service network requests
        boolean taskAdded = false;
        Connection connection;
        if (networkRequest.isPresent() && !TextUtils.isEmpty(networkRequest.get())) {
            try {
                connection = new Gson().fromJson(networkRequest.get(), Connection.class);
            } catch (IllegalStateException | JsonSyntaxException e) {
                e.printStackTrace();
                connection = null; //low probability
            }
            taskAdded = startOperation(connection);
            Log.i("Downloader", "Task Added " + taskAdded);
        }
        return taskAdded;
    }

    /**
     * Pause and Delete in disk are newer.
     * For a download operation update information from memory (for processed value).
     *
     * @return Returns if a work was added or not
     */
    private boolean startOperation(Connection connection) {

        Log.i("Downloader", "processing network request");
        if (connection == null || TextUtils.isEmpty(connection.getMessageType())) {
            Log.i("Downloader", "illegal network request");
            return false;
        }
        //load from disk
        final ReachDatabase reachDatabase = LocalUtils.getReachDatabase(
                handlerInterface,
                connection.getSongId(),
                connection.getSenderId(),
                connection.getReceiverId());

        switch (connection.getMessageType()) {

            case "REQ": {
                //new upload op
                if (reachDatabase == null)
                    return handleSend(connection);
                    //currently on-going, but higher priority received
                else if (reachDatabase.getLogicalClock() < connection.getLogicalClock()) {

                    LocalUtils.removeKeyWithId(networkManager.keys(), reachDatabase.getId());
                    LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId(), true);
                    Log.i("Downloader", "Servicing higher logical clock " + connection.getLogicalClock());
                    return handleSend(connection);
                } else
                    Log.i("Downloader", "Upload Op Already Present in database " + connection.toString());
                break;
            }

            case "RELAY": {

                if (reachDatabase == null) {
                    Log.i("Downloader", "download deleted");
                    break;
                }

                if (reachDatabase.getStatus() == ReachDatabase.WORKING ||
                        reachDatabase.getStatus() == ReachDatabase.RELAY ||
                        reachDatabase.getStatus() == ReachDatabase.FINISHED ||
                        reachDatabase.getStatus() == ReachDatabase.PAUSED_BY_USER) {
                    Log.i("Downloader", "Dropping RELAY " + reachDatabase.getStatus());
                    break;
                } else if (reachDatabase.getProcessed() != connection.getOffset()) {

                    //illegal connection acknowledgement, offset not correct, force increment
                    Log.i("Downloader", "INCREMENTING Logical Clock");
                    final Optional<Runnable> optional = LocalUtils.reset(reachDatabase, handlerInterface);
                    if (optional.isPresent())
                        threadPool.submit(optional.get());
                    else
                        LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId(), false);
                    break;
                } else {
                    //everything good
                    return handleReceive(reachDatabase, connection);
                }
            }

            default:
                Log.i("Downloader", "ILLEGAL CONNECT OP !");
        }
        return false;
    }

    /**
     * Handles the RELAY/ACK messages.
     * In-case of handleReceive, the ReachDatabase object HAS TO exist in the memory.
     * Also attempts for LAN connection.
     *
     * @param reachDatabase the transaction that will be handled (will already be loaded in memory by now)
     * @param connection    the RELAY/ACK message
     * @return weather successfully registered into selector or not
     */
    private boolean handleReceive(ReachDatabase reachDatabase, Connection connection) {

        Log.i("Downloader", "Trying to put RELAY OP " + reachDatabase.getId());
        final ContentValues values = new ContentValues();
        reachDatabase.setStatus(ReachDatabase.RELAY);
        values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.RELAY);
        //setUp file
        switch (LocalUtils.prepareDownloadFile(reachDatabase, reachDirectory)) {

            case 0: { //created new file

                reachDatabase.setPath(reachDirectory + "/" + reachDatabase.getId());
                reachDatabase.setProcessed(0); //reset
                values.put(ReachDatabaseHelper.COLUMN_PATH, reachDatabase.getPath());
                values.put(ReachDatabaseHelper.COLUMN_PROCESSED, 0);
                break; //proceed
            }
            case 2: { //error

                reachDatabase.setPath("");
                reachDatabase.setProcessed(0); //reset
                reachDatabase.setStatus(ReachDatabase.FILE_NOT_CREATED); //file not created
                values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.FILE_NOT_CREATED);
                values.put(ReachDatabaseHelper.COLUMN_PATH, "");
                values.put(ReachDatabaseHelper.COLUMN_PROCESSED, 0);

                final boolean updateSuccess = LocalUtils.updateReachDatabase(
                        values,
                        handlerInterface,
                        reachDatabase.getId());
                if (!updateSuccess) //remove entry if update failed
                    LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId(), false);
                Log.i("Downloader", "Error loading file");
                return false; //end
            }
        }

        Log.i("Downloader", "File creation completed");

        final boolean connectSuccess;
        if (reachDatabase.getSenderId() == StaticData.DEVIKA)
            //try cloud if Devika
            connectSuccess = LocalUtils.connectCloud(reachDatabase, networkManager, handlerInterface);
        else
            //else go with p2p
            connectSuccess = LocalUtils.connectReceiver(
                    reachDatabase,
                    networkManager,
                    connection.getUniqueIdReceiver(),
                    connection.getUniqueIdSender(),
                    handlerInterface);

        if (!connectSuccess) { //reset and fail

            final Optional<Runnable> optional = LocalUtils.reset(reachDatabase, handlerInterface);
            if (optional.isPresent())
                threadPool.submit(optional.get());
            else {
                LocalUtils.removeKeyWithId(networkManager.keys(), reachDatabase.getId());
                LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId(), false);
                LocalUtils.closeSocket(reachDatabase.getReference());
            }
            return false;
        }

        final boolean updateSuccess = LocalUtils.updateReachDatabase(
                values,
                handlerInterface,
                reachDatabase.getId());

        if (!updateSuccess) {
            LocalUtils.removeKeyWithId(networkManager.keys(), reachDatabase.getId());
            LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId(), false);
            LocalUtils.closeSocket(reachDatabase.getReference());
        }
        return updateSuccess;
    }

    /**
     * Handles the REQ messages.
     * In-case of handleSend, the ReachDatabase object DOES NOT EXIST ANYWHERE.
     *
     * @param connection the REQ message
     * @return weather successfully registered into selector or not
     */
    private boolean handleSend(Connection connection) {

        final ReachDatabase reachDatabase = new ReachDatabase();

        reachDatabase.setSongId(connection.getSongId()); // id
        reachDatabase.setReceiverId(connection.getReceiverId());
        reachDatabase.setSenderId(connection.getSenderId());
        reachDatabase.setOperationKind((short) 1);

        reachDatabase.setLength(connection.getLength());
        reachDatabase.setProcessed(connection.getOffset());
        reachDatabase.setLastActive(System.currentTimeMillis());
        reachDatabase.setAdded(System.currentTimeMillis());

        reachDatabase.setLogicalClock(connection.getLogicalClock());
        reachDatabase.setStatus(ReachDatabase.RELAY);

        final String[] splitter = handlerInterface.getContentResolver().insert(ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.contentValuesCreator(reachDatabase)).toString().split("/");
        if (splitter.length == 0)
            return false;
        reachDatabase.setId(Long.parseLong(splitter[splitter.length - 1].trim()));
        Log.i("Downloader", "Upload op inserted " + reachDatabase.getId());

        final Optional<Object[]> setUpFile = LocalUtils.prepareUploadFile(
                handlerInterface,
                connection.getSongId(),
                connection.getSenderId(),
                connection.getLength());

        if (!setUpFile.isPresent()) {

            connection.setMessageType("404");
            MiscUtils.sendGCM("CONNECT" + new Gson().toJson(connection, Connection.class),
                    connection.getReceiverId(), connection.getSenderId());
            Log.i("Downloader", "Requested song path invalid");
            LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId(), true);
            return false;
        }

        //////////////////////////////////
        final Pair<String, Short> nameAndStatus =
                LocalUtils.getNameAndStatus(connection.getReceiverId(), connection.getSenderId(), handlerInterface);
        if (nameAndStatus == null)
            return false;

        reachDatabase.setSenderName(nameAndStatus.first);
        reachDatabase.setStatus(nameAndStatus.second);

        final ContentValues values = new ContentValues();
        reachDatabase.setDisplayName((String) setUpFile.get()[0]);
        reachDatabase.setActualName((String) setUpFile.get()[1]);
        values.put(ReachDatabaseHelper.COLUMN_DISPLAY_NAME, (String) setUpFile.get()[0]);
        values.put(ReachDatabaseHelper.COLUMN_ACTUAL_NAME, (String) setUpFile.get()[1]);
        values.put(ReachDatabaseHelper.COLUMN_ALBUM_ART_DATA, (byte[]) setUpFile.get()[2]);
        values.put(ReachDatabaseHelper.COLUMN_SENDER_NAME, nameAndStatus.first);
        values.put(ReachDatabaseHelper.COLUMN_ONLINE_STATUS, nameAndStatus.second);

        final boolean tryRelay, tryDB, tryGCM;

        //establish relay
        tryRelay = LocalUtils.connectSender(
                reachDatabase,
                networkManager,
                connection.getUniqueIdReceiver(),
                connection.getUniqueIdSender(),
                handlerInterface);
        if (!tryRelay) { //remove and fail
            LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId(), false);
            return false;
        }

        //update the database
        tryDB = LocalUtils.updateReachDatabase(
                values,
                handlerInterface,
                reachDatabase.getId());
        if (!tryDB) {

            LocalUtils.removeKeyWithId(networkManager.keys(), reachDatabase.getId());
            LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId(), false);
            LocalUtils.closeSocket(reachDatabase.getReference());
            return false;
        }

        //send RELAY gcm
        connection.setMessageType("RELAY");
        final MyBoolean myBoolean = MiscUtils.sendGCM("CONNECT" + new Gson().toJson(connection, Connection.class),
                connection.getReceiverId(), connection.getSenderId());
        tryGCM = !(myBoolean == null || myBoolean.getOtherGCMExpired());
        if (!tryGCM) {
            Log.i("Downloader", "GCM FAILED NOT SENDING " + connection.toString());
            LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId(), true);
            return false;
        }

        //finally
        return true;
    }

    private void handleOp(SelectionKey selectionKey) {

        /**
         * First check if attachment is valid
         */
        final ReachDatabase database;
        if (selectionKey == null || (database = (ReachDatabase) selectionKey.attachment()) == null) {
            Log.i("Downloader", "Selection key null");
            LocalUtils.keyCleanUp(selectionKey);
            return;
        }

        /**
         * Safety check for paused operation
         */
        if (database.getStatus() == ReachDatabase.PAUSED_BY_USER) {

            Log.i("Ayush", "Pausing operation");
            LocalUtils.closeSocket(database.getReference());
            LocalUtils.keyCleanUp(selectionKey);
            if (database.getOperationKind() == 0) //close fileChannel if download
                MiscUtils.closeQuietly(openChannels.get(LocalUtils.getFileChannelIndex(database)));
            return; //nothing more to do
        }

        /**
         * Check if socket is valid
         */
        final SocketChannel socketChannel;
        if (!selectionKey.isValid() ||
                (socketChannel = (SocketChannel) selectionKey.channel()) == null ||
                !socketChannel.isOpen() ||
                !socketChannel.isConnected()) {

            Log.i("Downloader", "InvalidKey detected");
            LocalUtils.keyCleanUp(selectionKey);
            LocalUtils.closeSocket(database.getReference());
            if (database.getOperationKind() == 0) { //reset

                final Optional<Runnable> optional = LocalUtils.reset(database, handlerInterface);
                if (optional.isPresent())
                    threadPool.submit(optional.get());
                else {
                    LocalUtils.keyCleanUp(selectionKey);
                    LocalUtils.removeReachDatabase(handlerInterface, database.getId(), false);
                    LocalUtils.closeSocket(database.getReference());
                }
            } else //for upload just remove
                LocalUtils.removeReachDatabase(handlerInterface, database.getId(), true);
            return;
        }

        /**
         * Perform network operation
         */
        final long fileChannelIndex = LocalUtils.getFileChannelIndex(database);
        final FileChannel fileChannel = (FileChannel) openChannels.get(fileChannelIndex);
        final short DOWNLOAD_FAIL = -1, UPLOAD_FAIL = -2;
        long bytesChanged = 0;

        switch (selectionKey.interestOps()) {

            case SelectionKey.OP_READ: {

                if (fileChannel == null || !fileChannel.isOpen()) {
                    Log.i("Downloader", "DOWNLOAD FILE CHANNEL NOT FOUND");
                    database.setProcessed(DOWNLOAD_FAIL);
                } else
                    try {
                        bytesChanged = fileChannel.transferFrom(socketChannel, database.getProcessed(), 4096);
                    } catch (IOException e) {
                        database.setProcessed(DOWNLOAD_FAIL); //close this channel and resetWorkingMode
                        e.printStackTrace();
                    }
                break;
            }

            case SelectionKey.OP_WRITE: {

                if (fileChannel == null || !fileChannel.isOpen()) {
                    Log.i("Downloader", "UPLOAD FILE CHANNEL NOT FOUND");
                    database.setProcessed(UPLOAD_FAIL);
                } else
                    try {
                        /**JNI bug in java/android transferTo fails, refer the below link
                         * http://stackoverflow.com/questions/28819743 seems to be device specific
                         */
//                    bytesChanged = fileChannel.transferTo
//                            (bundle.getBytesProcessed(), bytesSize, channel);
//                        Log.i("Ayush", "Attempting upload");
                        if (fileChannel.read(transferBuffer, database.getProcessed()) > 0) {
                            transferBuffer.flip();
                            bytesChanged = socketChannel.write(transferBuffer);
                        }
                    } catch (IOException e) {
                        database.setProcessed(UPLOAD_FAIL); //close and delete
                        e.printStackTrace();
                    } finally {
                        transferBuffer.clear();
                    }
                break;
            }

            default: {
                //illegal operation
                LocalUtils.keyCleanUp(selectionKey);
                LocalUtils.closeSocket(database.getReference());
                LocalUtils.removeReachDatabase(handlerInterface, database.getId(), true);
                return; //illegal operation
            }
        }

        /**
         * Check for failure
         */
        if (database.getProcessed() == DOWNLOAD_FAIL) { //Has to be a failed download

            LocalUtils.keyCleanUp(selectionKey);
            LocalUtils.closeSocket(database.getReference());
            handlerInterface.downloadFail(database.getDisplayName(), "Failed during operation");
            final Optional<Runnable> optional = LocalUtils.reset(database, handlerInterface);
            if (optional.isPresent())
                threadPool.submit(optional.get());
            else
                LocalUtils.removeReachDatabase(handlerInterface, database.getId(), false);
            Log.i("Downloader", "DOWNLOAD ERROR " + database.getDisplayName());
            return;
        }
        if (database.getProcessed() == UPLOAD_FAIL) {
            //Has to be an upload
            LocalUtils.keyCleanUp(selectionKey);
            LocalUtils.closeSocket(database.getReference());
            LocalUtils.removeReachDatabase(handlerInterface, database.getId(), true);
            Log.i("Downloader", "UPLOAD ERROR " + database.getDisplayName());
            return;
        }

        /**
         * Operation successful
         */
        if (bytesChanged > 0) {
            database.setProcessed(database.getProcessed() + bytesChanged);
            database.setLastActive(System.currentTimeMillis());
        }

        if (database.getProcessed() < database.getLength())
            return; //partially completed

        /**
         * Transaction complete
         */
        switch (selectionKey.interestOps()) {

            /**
             * In case of download, mark as finished and close the socket.
             * We leave the key hanging so that sync can commit into the database.
             */
            case SelectionKey.OP_READ: {

                database.setProcessed(database.getLength());
                database.setStatus(ReachDatabase.FINISHED); //download finished
                Log.i("Downloader", "Download ended " + database.getProcessed() + " " + database.getLength());

                //clean up
                LocalUtils.closeSocket(database.getReference());
                LocalUtils.closeSocket(fileChannelIndex);
                LocalUtils.keyCleanUp(selectionKey);

                //sync into database
                final ContentValues contentValues = new ContentValues();
                contentValues.put(ReachDatabaseHelper.COLUMN_PROCESSED, database.getLength());
                contentValues.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.FINISHED);

                LocalUtils.forceUpdateReachDatabase(contentValues, handlerInterface, database.getId());
                //Sync upload success to server
                MiscUtils.autoRetryAsync(() -> StaticData.USER_API.updateCompletedOperations(
                        database.getReceiverId(),
                        database.getSenderId(),
                        database.getDisplayName(),
                        database.getLength()).execute(), Optional.absent());

                //usage tracking
                final Context context = handlerInterface.getContext();
                final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
                simpleParams.put(PostParams.USER_ID, database.getSenderId() + "");
                simpleParams.put(PostParams.DEVICE_ID, MiscUtils.getDeviceId(context));
                simpleParams.put(PostParams.OS, MiscUtils.getOsName());
                simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
                try {
                    simpleParams.put(PostParams.APP_VERSION,
                            context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                simpleParams.put(PostParams.SCREEN_NAME, "unknown");

                final Map<SongMetadata, String> complexParams = MiscUtils.getMap(5);
                complexParams.put(SongMetadata.SONG_ID, database.getSongId() + "");
                complexParams.put(SongMetadata.ARTIST, database.getArtistName());
                complexParams.put(SongMetadata.TITLE, database.getDisplayName());
                complexParams.put(SongMetadata.DURATION, database.getDuration() + "");
                complexParams.put(SongMetadata.SIZE, database.getLength() + "");

                //obviously network available !

                try {
                    UsageTracker.trackSong(simpleParams, complexParams, UsageTracker.DOWNLOAD_COMPLETE);
                } catch (JSONException ignored) {
                }

                return; //download complete
            }

            /**TODO test :
             * In-case of upload we do not close the socket right away.
             * We shut down the input/output and leave it hanging.
             * We do not close to allow system buffer to clear up
             */
            case SelectionKey.OP_WRITE: {

                Log.i("Downloader", "Upload ended " + database.getProcessed() + " " + database.getLength());
                LocalUtils.removeReachDatabase(handlerInterface, database.getId(), true);
                selectionKey.cancel();
            }
            //default case already handled
        }
    }

    /**
     * Kills the channels/keys that are sleeping for more than 60 secs
     * Also sync with sql db
     *
     * @param keys the set of keys to check
     */
    private void runOperation(Iterable<SelectionKey> keys,
                              NetworkHandlerInterface handlerInterface) {

        final long currentTime = System.currentTimeMillis();
        if (currentTime - lastOperation < StaticData.LUCKY_DELAY)
            return;

        final Cursor cursor = handlerInterface.getContentResolver().query(
                ReachDatabaseProvider.CONTENT_URI,
                new String[]{
                        ReachDatabaseHelper.COLUMN_ID,
                        ReachDatabaseHelper.COLUMN_PROCESSED,
                        ReachDatabaseHelper.COLUMN_STATUS},
                null, null, null);
        if (cursor == null)
            return;

        final LongSparseArray<ReachDatabase> tempDiskHolder = new LongSparseArray<>();
        final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        boolean needToUpdate = false;
        lastOperation = currentTime;

        while (cursor.moveToNext()) {

            final ReachDatabase diskReach = new ReachDatabase();
            diskReach.setProcessed(cursor.getLong(1));
            diskReach.setStatus(cursor.getShort(2));
            tempDiskHolder.append(cursor.getLong(0), diskReach); //append for later use
        }

        //first of all kill/reset dead Transactions
        for (SelectionKey selectionKey : keys) {

            final ReachDatabase memoryReach, diskReach;
            if (selectionKey == null || (memoryReach = (ReachDatabase) selectionKey.attachment()) == null)
                continue;//should not happen
            diskReach = tempDiskHolder.get(memoryReach.getId(), null);

            /**
             * First perform sync related operations
             */
            //deleted or paused operation
            if (diskReach == null || diskReach.getStatus() == ReachDatabase.PAUSED_BY_USER) {

                LocalUtils.closeSocket(memoryReach.getReference());
                LocalUtils.keyCleanUp(selectionKey);
                if (memoryReach.getOperationKind() == 0) //close fileChannel if download
                    MiscUtils.closeQuietly(openChannels.get(LocalUtils.getFileChannelIndex(memoryReach)));
                needToUpdate = true;

                if (diskReach != null) //mark paused
                    memoryReach.setStatus(ReachDatabase.PAUSED_BY_USER);
                continue; //nothing more to do
            } else if (memoryReach.getProcessed() != diskReach.getProcessed()) {

                //update progress
                final ContentValues contentValues = new ContentValues();
                contentValues.put(ReachDatabaseHelper.COLUMN_PROCESSED, memoryReach.getProcessed());
                operations.add(LocalUtils.getUpdateOperation(contentValues, memoryReach.getId()));
            } else if (memoryReach.getStatus() != diskReach.getStatus()) {

                //update status
                final ContentValues contentValues = new ContentValues();
                contentValues.put(ReachDatabaseHelper.COLUMN_STATUS, memoryReach.getStatus());
                operations.add(LocalUtils.getUpdateOperation(contentValues, memoryReach.getId()));
            }

            /**
             * Then perform keyKiller operations
             */
            if (currentTime - memoryReach.getLastActive() > 60 * 1001) {

                Log.i("Downloader", "Running keyKiller");
                try {
                    if (memoryReach.getOperationKind() == 0) {

                        //for download, attempt reset
                        final Optional<Runnable> optional = LocalUtils.reset(memoryReach, handlerInterface);
                        if (optional.isPresent())
                            threadPool.submit(optional.get());
                        else {
                            operations.add(ContentProviderOperation.newDelete(
                                    Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + memoryReach.getId())).build());
                            needToUpdate = true;
                        }
                        handlerInterface.downloadFail(memoryReach.getDisplayName(), "Failed in keyKiller");
                    } else {

                        //for upload
                        operations.add(ContentProviderOperation.newDelete(
                                Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + memoryReach.getId())).build());
                        needToUpdate = true;
                    }
                } finally {
                    LocalUtils.keyCleanUp(selectionKey);
                    LocalUtils.closeSocket(memoryReach.getReference());
                }
            }
        }

        try {
            if (operations.size() > 0)
                handlerInterface.getContentResolver().applyBatch(ReachDatabaseProvider.AUTHORITY, operations);
        } catch (RemoteException | OperationApplicationException ignored) {
        } finally {

            cursor.close();
            if (needToUpdate)
                handlerInterface.updateNetworkDetails();
            tempDiskHolder.clear();
            operations.clear();
        }
    }

    private enum LocalUtils {
        ;

        private static final InetSocketAddress vmAddress = new InetSocketAddress("104.199.154.0", 60001);

        /**
         * Clean up the directory, if file not found, marks as FILE_NOT_CREATED
         */
        public static void sanitizeReachDirectory(NetworkHandlerInterface handlerInterface, File reachDirectory) {

            //////////////////////////////////purge all upload operations, but retain paused operations
            handlerInterface.getContentResolver().delete(
                    ReachDatabaseProvider.CONTENT_URI,
                    ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                            ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                    new String[]{"1", ReachDatabase.PAUSED_BY_USER + ""});
            //////////////////////////////////refresh reachDatabase : handle directory invalidation
            final Cursor cursor = handlerInterface.getContentResolver().query
                    (ReachDatabaseProvider.CONTENT_URI,
                            ReachDatabaseHelper.projection,
                            ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?",
                            new String[]{"0"},
                            null);
            if (cursor == null)
                return; //this should never happen
            //first we mask all operations as ReachDatabase.FILE_NOT_CREATED
            final LongSparseArray<ReachDatabase> temp = new LongSparseArray<>();
            while (cursor.moveToNext()) {

                final ReachDatabase reachDatabase = ReachDatabaseHelper.cursorToProcess(cursor);
                if (reachDatabase.getProcessed() > 0) //mask only those which have associated file
                    reachDatabase.setStatus(ReachDatabase.FILE_NOT_CREATED);
                else //has not started, obviously no file !
                    reachDatabase.setStatus(ReachDatabase.NOT_WORKING);
                temp.append(reachDatabase.getId(), reachDatabase);
            }
            cursor.close();
            //now we scan the directory and unmask those files that were found
            for (File file : reachDirectory.listFiles()) {

                final ReachDatabase reachDatabase = temp.get(Long.parseLong(file.getName().replaceAll("[^0-9]", "")), null);
                if (reachDatabase == null)
                    continue;

                if (reachDatabase.getProcessed() == reachDatabase.getLength())
                    reachDatabase.setStatus(ReachDatabase.FINISHED);  //completed
                else
                    reachDatabase.setStatus(ReachDatabase.NOT_WORKING); //not working (file found, can continue download)
            }

            final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
            for (int i = 0; i < temp.size(); i++) {
                // get the object by the key.
                final ReachDatabase database = temp.get(temp.keyAt(i));
                if (database == null)
                    continue;

                final ContentValues values = new ContentValues();
                values.put(ReachDatabaseHelper.COLUMN_STATUS, database.getStatus());
                operations.add(LocalUtils.getUpdateOperation(values, database.getId()));
            }

            if (operations.size() > 0)
                try {
                    handlerInterface.getContentResolver().applyBatch(ReachDatabaseProvider.AUTHORITY, operations);
                } catch (RemoteException | OperationApplicationException e) {
                    e.printStackTrace();
                }

            temp.clear();
        }

        /**
         * Connects to the cloud and registers into selector
         *
         * @return weather successful or not
         */
        public static boolean connectCloud(ReachDatabase reachDatabase,
                                           Selector networkManager,
                                           NetworkHandlerInterface handlerInterface) {

            //create a random reference for socket
            //In NIO, the default behaviour is blocking.
            final long reference = UUID.randomUUID().getMostSignificantBits();
            final StringBuilder stringBuilder = new StringBuilder()
                    .append("GET /reach-again/music/").append(reachDatabase.getActualName()).append(" HTTP/1.1\r\n"); //specify the file
            if (reachDatabase.getProcessed() > 0)
                stringBuilder.append("Range: bytes=").append(reachDatabase.getProcessed()).append("-\r\n"); //specify the range if required

            stringBuilder.append(requestDefaults)
                    .append("\r\n"); //end with this

            final String request = stringBuilder.toString();
            Log.i("Downloader", request);
            final ByteBuffer header = ByteBuffer.wrap(request.getBytes());
            final SocketChannel socketChannel;

            try {

                //open socket and send request
                openChannels.append(reference, socketChannel = SocketChannel.open(new InetSocketAddress("s3.amazonaws.com", 80)));
                final Socket socket = socketChannel.socket();
                socket.setKeepAlive(true);
                socketChannel.write(header);

                final InputStream reader = socket.getInputStream();
                stringBuilder.setLength(0);
                //first fetch header
                while (true) {

                    final char read = (char) reader.read();
                    if (read < 1) {
                        //fail
                        MiscUtils.closeQuietly(socket, reader);
                        LocalUtils.closeSocket(reference);
                        return false;
                    }

                    stringBuilder.append(read);
                    if (read == '\n')
                        break; //status header fetched
                }

                //check status
                final String status = stringBuilder.toString();
                Log.i("Downloader", status);
                if (!(status.contains("200") || status.contains("206"))) {
                    //fail
                    socket.close();
                    reader.close();
                    LocalUtils.closeSocket(reference);
                    return false;
                }

                //now consume the header
                stringBuilder.setLength(0);
                while (true) {

                    final char read = (char) reader.read();
                    if (read < 1) {
                        MiscUtils.closeQuietly(socket, reader);
                        LocalUtils.closeSocket(reference);
                        return false;
                    }

                    stringBuilder.append(read);
                    final int currentCount = stringBuilder.length();
                    if (currentCount > 4 &&
                            stringBuilder.charAt(currentCount - 1) == '\n' &&
                            stringBuilder.charAt(currentCount - 2) == '\r' &&
                            stringBuilder.charAt(currentCount - 3) == '\n' &&
                            stringBuilder.charAt(currentCount - 4) == '\r') {
                        break; //response consumed
                    }
                }

                //print the header
                final String completeResponse = stringBuilder.toString();
                Log.i("Downloader", completeResponse);

                //register
                socketChannel.configureBlocking(false).register(
                        networkManager,
                        SelectionKey.OP_READ,
                        reachDatabase);

            } catch (IOException | AssertionError e) {

                //removal of selectionKey will be done in keyKiller
                e.printStackTrace();
                LocalUtils.toast("Firewall issue", handlerInterface, reachDatabase.getReceiverId());
                LocalUtils.closeSocket(reference);
                return false;
            }

            Log.i("Downloader", "Connected to cloud");
            //cloud established !
            reachDatabase.setReference(reference);
            reachDatabase.setLastActive(System.currentTimeMillis());
            return true;
        }

        /**
         * Connects to the relay and registers into selector
         *
         * @return weather successful or not
         */
        public static boolean connectReceiver(ReachDatabase reachDatabase,
                                              Selector networkManager,
                                              long uniqueReceiverId,
                                              long uniqueSenderId,
                                              NetworkHandlerInterface handlerInterface) {

            //create a random reference for socket
            final long reference = UUID.randomUUID().getMostSignificantBits();
            reachDatabase.setReference(reference);
            reachDatabase.setLastActive(System.currentTimeMillis());

            try {

                final SocketChannel socketChannel;
                openChannels.append(reference, socketChannel = SocketChannel.open(vmAddress));
                socketChannel.socket().setKeepAlive(true);
                socketChannel.write(ByteBuffer.wrap((
                        uniqueReceiverId + "\n" +
                                uniqueSenderId + "\n" +
                                "Receiver" + "\n").getBytes()));
                socketChannel.configureBlocking(false).register(
                        networkManager,
                        SelectionKey.OP_READ,
                        reachDatabase);

            } catch (IOException | AssertionError e) {

                //removal of selectionKey will be done in keyKiller
                e.printStackTrace();
                LocalUtils.toast("Firewall issue", handlerInterface, reachDatabase.getReceiverId());
                LocalUtils.closeSocket(reference);
                return false;
            }
            //relay established !
            return true;
        }

        /**
         * Connects to the relay and registers into selector
         *
         * @return weather successful or not
         */
        public static boolean connectSender(ReachDatabase reachDatabase,
                                            Selector networkManager,
                                            long uniqueReceiverId,
                                            long uniqueSenderId,
                                            NetworkHandlerInterface handlerInterface) {

            //create a random reference for socket
            final long reference = UUID.randomUUID().getMostSignificantBits();
            reachDatabase.setReference(reference);
            reachDatabase.setLastActive(System.currentTimeMillis());

            try {

                final SocketChannel socketChannel;
                openChannels.append(reference, socketChannel = SocketChannel.open(vmAddress));
                socketChannel.socket().setKeepAlive(true);
                socketChannel.write(ByteBuffer.wrap((
                        uniqueSenderId + "\n" +
                                uniqueReceiverId + "\n" +
                                "Sender" + "\n").getBytes()));
                socketChannel.configureBlocking(false).register(
                        networkManager,
                        SelectionKey.OP_WRITE,
                        reachDatabase);

            } catch (IOException | AssertionError e) {

                //TODO make a central GCM and relay connectivity check
                //removal of selectionKey will be done in keyKiller
                e.printStackTrace();
                LocalUtils.toast("Firewall issue", handlerInterface, reachDatabase.getSenderId());
                LocalUtils.closeSocket(reference);
                return false;
            }
            //relay established !
            return true;
        }


        /**
         * Prepares the fileChannel needed for upload
         *
         * @param handlerInterface the handlerInterface
         * @param songId           the id of the song requested
         * @param myId             my own id
         * @return display name and actual name of the song
         */
        public static Optional<Object[]> prepareUploadFile(NetworkHandlerInterface handlerInterface,
                                                           long songId,
                                                           long myId,
                                                           long length) {

            /**
             * We do not know which DB the request should look into. Problem ?
             * First look in ReachSong table, then in ReachDatabase table.
             * Probability of ID and SIZE, both being same. Low.
             */

            Cursor cursor = handlerInterface.getContentResolver().query(
                    MySongsProvider.CONTENT_URI,
                    new String[]{
                            MySongsHelper.COLUMN_ID, //0
                            MySongsHelper.COLUMN_DISPLAY_NAME, //1
                            MySongsHelper.COLUMN_ACTUAL_NAME, //2
                            MySongsHelper.COLUMN_PATH, //3
                            MySongsHelper.COLUMN_VISIBILITY, //4
                            MySongsHelper.COLUMN_ALBUM_ART_DATA}, //5

                    MySongsHelper.COLUMN_SONG_ID + " = ? and " +
                            MySongsHelper.COLUMN_SIZE + " = ?",
                    new String[]{songId + "", length + ""}, null);

            Log.i("Downloader", myId + " " + songId + " " + length);

            if (cursor == null || !cursor.moveToFirst()) {

                Log.i("Ayush", "Picking from reachDatabase table");

                if (cursor != null)
                    cursor.close();
                cursor = handlerInterface.getContentResolver().query(
                        ReachDatabaseProvider.CONTENT_URI,
                        new String[]{
                                ReachDatabaseHelper.COLUMN_UNIQUE_ID, //0
                                ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //1
                                ReachDatabaseHelper.COLUMN_ACTUAL_NAME, //2
                                ReachDatabaseHelper.COLUMN_PATH, //3
                                ReachDatabaseHelper.COLUMN_VISIBILITY, //4
                                ReachDatabaseHelper.COLUMN_ALBUM_ART_DATA}, //5

                        ReachDatabaseHelper.COLUMN_UNIQUE_ID + " = ? and " +
                                ReachDatabaseHelper.COLUMN_SIZE + " = ?",
                        new String[]{songId + "", length + ""}, null);
            }

            if (cursor == null || !cursor.moveToFirst()) {

                //TODO temporary // FIXME: 08/01/16
                Log.i("Ayush", "Hacking from reachDatabase table");

                if (cursor != null)
                    cursor.close();
                cursor = handlerInterface.getContentResolver().query(
                        ReachDatabaseProvider.CONTENT_URI,
                        new String[]{
                                ReachDatabaseHelper.COLUMN_UNIQUE_ID, //0
                                ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //1
                                ReachDatabaseHelper.COLUMN_ACTUAL_NAME, //2
                                ReachDatabaseHelper.COLUMN_PATH, //3
                                ReachDatabaseHelper.COLUMN_VISIBILITY, //4
                                ReachDatabaseHelper.COLUMN_ALBUM_ART_DATA}, //5

                        ReachDatabaseHelper.COLUMN_SONG_ID + " = ? and " +
                                ReachDatabaseHelper.COLUMN_SIZE + " = ?",
                        new String[]{songId + "", length + ""}, null);
            }

            if (cursor == null || !cursor.moveToFirst() || cursor.getShort(4) == 0) {

                //TODO tell invisible
                Log.i("Downloader", "Requested song path invalid");
                if (cursor == null)
                    Log.i("Downloader", "Song not found cursor null");
                else if (!cursor.moveToFirst())
                    Log.i("Downloader", "Cursor could not be moved");
                else
                    Log.i("Downloader", cursor.getShort(4) + " visibility");
                return Optional.absent();
            }

            final Object[] toSend = new Object[]{
                    cursor.getString(1),  //display name
                    cursor.getString(2), //actual name
                    cursor.getBlob(5)}; //albumArtData
            final String filePath = cursor.getString(3);
            cursor.close();
            if (TextUtils.isEmpty(filePath))
                return Optional.absent();
            //song found, continuing
            final long fileChannelIndex = getFileChannelIndex((String) toSend[1], (String) toSend[0], songId);
            final Channel channel = openChannels.get(fileChannelIndex, null);
            if (channel == null || !channel.isOpen()) { //not present or not open

                final RandomAccessFile file;
                try {
                    file = new RandomAccessFile(filePath, "r");
                    Log.i("Downloader", "Requested song file invalid");
                    if (file.length() == 0)
                        return Optional.absent();
                } catch (IOException e) {
                    e.printStackTrace();
                    return Optional.absent();
                }
                openChannels.put(fileChannelIndex, file.getChannel());
                Log.i("Downloader", "Index for " + toSend[0] + " = " + fileChannelIndex);
            }
            //else channel already loaded

            return Optional.of(toSend);
        }

        /**
         * Prepare file and channel for download
         *
         * @return 0 : created new file
         * 1 : re-used old file
         * 2 : error
         */
        public static byte prepareDownloadFile(ReachDatabase reachDatabase,
                                               File reachDirectory) {

            final byte result;
            final long fileChannelIndex = getFileChannelIndex(reachDatabase);
            final Channel channel = openChannels.get(fileChannelIndex, null);
            Log.i("Downloader", "Index for " + reachDatabase.getDisplayName() + " = " + fileChannelIndex);
            if (channel != null && channel.isOpen())
                return 1; //channel already open

            final String path = reachDatabase.getPath();
            final long length = reachDatabase.getLength();
            final RandomAccessFile randomAccessFile;
            final File file;
            /**
             * The file exists, isFile and length functions can not be relied upon always
             * hence we use an ugly hack to confirm that the file is indeed invalid
             * 1) we check is the reachDatabase has not been allotted a file
             * 2) if it was allotted a file, we get a reference to it
             * 3) next we check if the allotted file has been invalidated or not
             * 4) finally if we are 100% sure that a file is already present, we continue as is,
             *    else we reset reachDatabase back to 0
             */
            if (TextUtils.isEmpty(path) || path.equals("hello_world") || //file allotted ?
                    (file = new File(path)) == null || //get file
                    !(file.isFile() || file.isFile()) || //valid file ?
                    !(file.length() >= length || file.length() >= length)) { //length ok ?

                Log.i("Downloader", "Creating new File for " + reachDatabase.getDisplayName());
                try {
                    randomAccessFile = new RandomAccessFile(reachDirectory + "/" + reachDatabase.getId(), "rw");
                    randomAccessFile.setLength(reachDatabase.getLength()); //resize to current
                } catch (IOException e) {
                    e.printStackTrace();
                    return 2;
                }
                result = 0;
            } else {
                /**
                 * 1) We re-use the pre-existing file
                 * 2) We do not mess with the length and file pointer
                 */
                try {
                    randomAccessFile = new RandomAccessFile(file, "rw");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return 2;
                }
                result = 1;
            }
            openChannels.put(fileChannelIndex, randomAccessFile.getChannel()); //gets the fileChannel
            return result;
        }

        /**
         * Remove a selectionKey from the selector
         *
         * @param keys set of keys from which to remove
         * @param id   the id to remove
         */
        public static void removeKeyWithId(Iterable<SelectionKey> keys,
                                           long id) {

            for (SelectionKey selectionKey : keys) {
                if (selectionKey == null || selectionKey.attachment() == null)
                    continue;
                final ReachDatabase database = (ReachDatabase) selectionKey.attachment();
                if (database.getId() == id) {
                    keyCleanUp(selectionKey);
                    closeSocket(database.getReference());
                    break;
                }
            }
        }

        /**
         * Resets the transaction, reset download only. Updates memory cache and disk table both.
         * Update happens in MiscUtils.startDownloadOperation() method.
         *
         * @param reachDatabase    the transaction to reset
         * @param handlerInterface the handlerInterface
         */
        public static Optional<Runnable> reset(ReachDatabase reachDatabase,
                                               NetworkHandlerInterface handlerInterface) {

            reachDatabase.setLogicalClock((short) (reachDatabase.getLogicalClock() + 1));
            reachDatabase.setStatus(ReachDatabase.NOT_WORKING);

            final ContentValues values = new ContentValues();
            values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.NOT_WORKING);
            values.put(ReachDatabaseHelper.COLUMN_LOGICAL_CLOCK, reachDatabase.getLogicalClock());
            //pass and empty list and update
            //noinspection unchecked
            final boolean updateSuccess =
                    updateReachDatabase(values, handlerInterface, reachDatabase.getId());

            if (updateSuccess)
                //send REQ gcm
                return Optional.of((Runnable) MiscUtils.startDownloadOperation(
                        handlerInterface.getContext(),
                        reachDatabase,
                        reachDatabase.getReceiverId(), //myID
                        reachDatabase.getSenderId(),   //the uploaded
                        reachDatabase.getId()));

            return Optional.absent(); //update failed !
        }

        /**
         * Get name and status for user
         *
         * @param id id of the user
         * @return name and status
         */
        public static Pair<String, Short> getNameAndStatus(long id, long myId, NetworkHandlerInterface handlerInterface) {

            final Cursor receiverName = handlerInterface.getContentResolver().query(
                    Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + id),
                    new String[]{
                            ReachFriendsHelper.COLUMN_USER_NAME,
                            ReachFriendsHelper.COLUMN_STATUS},
                    ReachFriendsHelper.COLUMN_ID + " = ?",
                    new String[]{id + ""}, null);


            final String name;
            final short status;

            if (receiverName != null && receiverName.moveToFirst()) {

                name = receiverName.getString(0);
                status = receiverName.getShort(1);
                receiverName.close();
            } else {

                if (receiverName != null)
                    receiverName.close();

                //look up from net
                final Friend friend = MiscUtils.autoRetry(() -> StaticData.USER_API.getFriendFromId(myId, id).execute(), Optional.absent()).orNull();
                if (friend == null)
                    return null;
                handlerInterface.getContentResolver().insert(
                        ReachFriendsProvider.CONTENT_URI,
                        ReachFriendsHelper.contentValuesCreator(friend));

                name = friend.getUserName();
                status = friend.getStatus().shortValue();
            }

            return new Pair<>(name, status);
        }

        /**
         * Clean up given key
         *
         * @param selectionKey the key
         */
        public static void keyCleanUp(SelectionKey selectionKey) {

            if (selectionKey == null || !selectionKey.isValid())
                return;
            Log.i("Downloader", "Running cleanUp");
            MiscUtils.closeQuietly(selectionKey.channel());
            selectionKey.attach(null); //discard old attachment
            selectionKey.cancel();
        }

        /**
         * Create a contentProviderOperation, we do not update
         * if the operation is paused. Case of unpause can not happen
         * in this class.
         *
         * @param contentValues the values to use
         * @param id            the id of the entry
         * @return the contentProviderOperation
         */
        public static ContentProviderOperation getUpdateOperation(ContentValues contentValues, long id) {
            return ContentProviderOperation
                    .newUpdate(Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id))
                    .withValues(contentValues)
                    .withSelection(ReachDatabaseHelper.COLUMN_ID + " = ? and " +
                                    ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                            new String[]{id + "", "" + ReachDatabase.PAUSED_BY_USER})
                    .build();
        }

        /**
         * Updates the reachDatabase table,we do not update
         * if the operation is paused. Case of unpause can not happen
         * in this class.
         *
         * @param contentValues the values that need to be updated
         * @param id            the id of the row that needs to be updated (reachDatabase id)
         * @return weather update was successful or not
         */ //TODO
        public static boolean updateReachDatabase(ContentValues contentValues,
                                                  NetworkHandlerInterface handlerInterface,
                                                  long id) {

            return handlerInterface.getContentResolver().update(
                    Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id),
                    contentValues,
                    ReachDatabaseHelper.COLUMN_ID + " = ? and " +
                            ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                    new String[]{id + "", "" + ReachDatabase.PAUSED_BY_USER}) > 0;
        }

        /**
         * Force update the reachDatabase table
         *
         * @param contentValues the values that need to be updated
         * @param id            the id of the row that needs to be updated (reachDatabase id)
         * @return weather update was successful or not
         */
        public static boolean forceUpdateReachDatabase(ContentValues contentValues,
                                                       NetworkHandlerInterface handlerInterface,
                                                       long id) {

            return handlerInterface.getContentResolver().update(
                    Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id),
                    contentValues,
                    ReachDatabaseHelper.COLUMN_ID + " = ?",
                    new String[]{id + ""}) > 0;
        }


        /**
         * Removes the reachDatabase entry from disk and memory
         *
         * @param id of reachDatabase to remove
         */
        public static void removeReachDatabase(NetworkHandlerInterface handlerInterface,
                                               long id,
                                               boolean force) {

            if (force) {

                //if force don't check
                if (handlerInterface.getContentResolver().delete(
                        Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id),
                        ReachDatabaseHelper.COLUMN_ID + " = ?",
                        new String[]{id + ""}) > 0)
                    handlerInterface.updateNetworkDetails();
            } else {

                //else check
                if (handlerInterface.getContentResolver().delete(
                        Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id),
                        ReachDatabaseHelper.COLUMN_ID + " = ? and " +
                                ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                        new String[]{id + "", "" + ReachDatabase.PAUSED_BY_USER}) > 0)
                    handlerInterface.updateNetworkDetails();
            }
        }

        /**
         * @param songId     the songId on sender side
         * @param senderId   server-id of sender
         * @param receiverId server-id of receiver
         * @return the reachDatabase object corresponding to parameters
         */
        public static ReachDatabase getReachDatabase(NetworkHandlerInterface handlerInterface,
                                                     long songId,
                                                     long senderId,
                                                     long receiverId) {

            final Cursor cursor = handlerInterface.getContentResolver().query(
                    ReachDatabaseProvider.CONTENT_URI,
                    ReachDatabaseHelper.projection,
                    ReachDatabaseHelper.COLUMN_SONG_ID + " = ? and " +
                            ReachDatabaseHelper.COLUMN_SENDER_ID + " = ? and " +
                            ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ?",
                    new String[]{songId + "",
                            senderId + "",
                            receiverId + ""},
                    null);

            if (cursor == null)
                return null;
            if (!cursor.moveToFirst()) {
                cursor.close();
                return null;
            }

            final ReachDatabase reachDatabase = ReachDatabaseHelper.cursorToProcess(cursor);
            cursor.close();
            return reachDatabase;
        }

        /**
         * Right now only logs
         *
         * @param message that needs to be shown as toast
         */
        public static void toast(String message, NetworkHandlerInterface handlerInterface, long id) {

            Log.i("Downloader", message + "TOAST");

            ((ReachApplication) handlerInterface.getApplication()).track(
                    Optional.of(message),
                    Optional.of("ServerId " + id),
                    Optional.absent(),
                    1);
        }

        /**
         * Get the fileChannel index for given reachDatabase
         *
         * @param reachDatabase to pick names from
         * @return fileChannel index
         */
        public static long getFileChannelIndex(ReachDatabase reachDatabase) {
            return getFileChannelIndex(
                    reachDatabase.getActualName(),
                    reachDatabase.getDisplayName(),
                    reachDatabase.getSongId());
        }

        /**
         * Get the fileChannel index for given name
         *
         * @param actualName  of the song
         * @param displayName of the song
         * @return fileChannel index
         */
        public static long getFileChannelIndex(String actualName, String displayName, long songId) {
            return Arrays.hashCode(new Object[]{
                    TextUtils.isEmpty(actualName) ? "" : actualName,
                    TextUtils.isEmpty(displayName) ? "" : displayName,
                    songId});
        }

        /**
         * Close and remove socket
         *
         * @param reference of the socket
         */
        public static void closeSocket(long reference) {
            MiscUtils.closeQuietly(openChannels.get(reference, null));
            openChannels.remove(reference);
        }

        /**
         * Get the ReachDirectory where we download songs
         *
         * @return File : reachDirectory
         */
        public static File getDirectory() {

            final File file = new File(Environment.getExternalStorageDirectory(), ".Reach");
            if (file.exists() && file.isDirectory()) {
                Log.i("Downloader", "Using getExternalStorageDirectory");
                return file;
            } else if (file.mkdirs()) {
                Log.i("Downloader", "Creating and using getExternalStorageDirectory");
                return file;
            }
            return null;
        }
    }

    public interface NetworkHandlerInterface {

        WifiManager getWifiManager();

        ContentResolver getContentResolver();

        Context getContext();

        Application getApplication();

        SharedPreferences getSharedPreferences();

        void updateNetworkDetails();

//        Future<?> submitChild(ReachTask runnable);

        void removeNetwork();

        void downloadFail(String songName, String reason);
    }
}