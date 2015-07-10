package reach.project.reachProcess.reachService;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.project.core.StaticData;
import reach.project.database.ReachDatabase;
import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.database.sql.ReachSongHelper;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.auxiliaryClasses.DataBundle;
import reach.project.reachProcess.auxiliaryClasses.ReachTask;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;

/**
 * Created by Dexter on 18-05-2015.
 * This is an auto-close type parent thread.
 * Explicit close only if user requests, using kill (Exit)
 */
public class NetworkHandler extends ReachTask<NetworkHandler.NetworkHandlerInterface> {

    public NetworkHandler(NetworkHandlerInterface handlerInterface) {
        super(handlerInterface);
    }

    public boolean submitMessage(String message) {
        return pendingNetworkRequests.offer(message);
    }

    public static final String ACTION_NETWORK_MESSAGE = "reach.project.reachProcess.reachService.NETWORK_MESSAGE";
    public static final String ACTION_NETWORK_LOST = "reach.project.reachProcess.reachService.ACTION_NETWORK_LOST";
    ////////////////////////////////// these need cleaning in sanitize()
    //prevent GC of sockets and channels
    private final LongSparseArray<Channel> openChannels = new LongSparseArray<>(100); //needs closing

    private final LongSparseArray<ReachDatabase> reachDatabaseMemory = new LongSparseArray<>(100);
    private final LongSparseArray<ReachDatabase> tempDiskHolder = new LongSparseArray<>();

    //message holder
    private final ConcurrentLinkedQueue<String> pendingNetworkRequests = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Pair<SocketChannel, Connection>> pendingLanRequests = new ConcurrentLinkedQueue<>(); //needs closing

    private final ByteBuffer transferBuffer = ByteBuffer.allocateDirect(5000);
    private final InetSocketAddress vmAddress = new InetSocketAddress("104.199.154.0", 60001);

    private File reachDirectory = null;
    private Selector networkManager = null; //needs closing
    private WifiManager.WifiLock wifiLock = null; //needs closing
    //    private SocketAddress lanAddress = null; TODO
//    private final LanHandler lanHandler = new LanHandler(this); //needs closing
//    private Future<?> lanHandlerFuture = null;
    private long lastKeyKill = 0;
    private long lastSync = 0;
    //////////////////////////////////

    @Override
    protected void sanitize() {

        Log.i("Downloader", "Downloader starting sanitize");
        kill.set(true);
        if (wifiLock != null && wifiLock.isHeld())
            wifiLock.release();
        wifiLock = null;
        Log.i("Downloader", "Downloader wiFi lock released");
        reachDirectory = null;
//        lanAddress = null;
//        lanHandler.close(); TODO
//        if (lanHandlerFuture != null)
//            lanHandlerFuture.cancel(true);
//        lanHandlerFuture = null;
        lastKeyKill = lastSync = 0;
        //////////////////////////////////
        Log.i("Downloader", "Releasing network manager");
        if (networkManager != null) {
            try {
                networkManager.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.i("Downloader", "Downloader networkManager released");
        //////////////////////////////////
        for (int i = 0, size = openChannels.size(); i < size; i++) {
            final Channel channel = openChannels.valueAt(i);
            if (channel != null)
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        openChannels.clear();
        Log.i("Downloader", "Downloader openChannels released");
        //////////////////////////////////
        Pair<SocketChannel, Connection> temp;
        while ((temp = pendingLanRequests.poll()) != null && temp.first != null) {
            try {
                temp.first.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        pendingLanRequests.clear();
        Log.i("Downloader", "Downloader pendingLanRequests released");
        //////////////////////////////////
        reachDatabaseMemory.clear();
        tempDiskHolder.clear();
        transferBuffer.clear();
        Log.i("Downloader", "Downloader cleaning up");
    }

    @Override
    protected void performTask() {

        Log.i("Downloader", "Network handler thread started");
        kill.set(false);
//        lanAddress = lanHandler.getLocalSocketAddress();
//        Log.i("Downloader", "LAN address = " + lanAddress);
        if (!getReachDirectory())
            return;
        if (!setUpNetworkManager()) //cleans on failure
            return;
        sanitizeReachDirectory();
        (wifiLock = handlerInterface.getWifiManager().createWifiLock(WifiManager.WIFI_MODE_FULL, "network_lock")).acquire();
        //////////////////////////////////
        //////////////////////////////////
        long lastActive = System.currentTimeMillis(), currentTime;
        boolean kill = false, sleeping = false, taskAdded;
        Set<SelectionKey> keySet;
        Log.i("Downloader", "Initialization done");
        while (networkManager != null && networkManager.isOpen()) {
//            if (lanHandlerFuture == null || lanHandlerFuture.isCancelled() || lanHandlerFuture.isDone()) {
//                lanHandler.sanitize();
//                Log.i("Downloader", "Starting LAN handler");
//                lanHandlerFuture = handlerInterface.submitChild(lanHandler);
//            }
            final Optional<String> networkRequest = Optional.fromNullable(pendingNetworkRequests.poll());
//            final Optional<Pair<SocketChannel, Connection>> lanRequest = Optional.fromNullable(pendingLanRequests.poll());
//            if (!(taskAdded = processTask(networkRequest, lanRequest)) && lanRequest.isPresent())
//                try {
//                    lanRequest.get().first.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            if(taskAdded = processTask(networkRequest))
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

                if (sleeping &&
                        !taskAdded &&
                        StaticData.threadPool.getQueue().size() == 0 &&
                        StaticData.threadPool.getActiveCount() == 0 &&
                        networkManager.keys().size() == 0 &&
                        currentTime - lastActive > 5000) {
                    //no connections to service (sleeping)
                    //no task that is getting added (no taskAdded)
                    //no task that is un-finished (task-manager has 0 entries)
                    //no active connections (network-manager has 0 keys)
                    //finally a sleep allowance of 5 seconds
                    Log.i("Downloader", "Service can be killed now");
                    kill = true;
                } else {
                    //this syncs up the result of sanitizeReachDirectory() as well
                    sync(currentTime);
                    //kill all the dead keys
                    keyKiller(networkManager.keys());
                }
            }
            if (kill)
                break;
            if (sleeping) {
//                Log.i("Downloader", "Sleeping " + " " + taskAdded + " " +
//                        StaticData.threadPool.getQueue().size() + " " +
//                        StaticData.threadPool.getActiveCount() + " " +
//                        networkManager.keys().size() + " " +
//                        (currentTime - lastActive));
                continue;
            }
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
        sync(System.currentTimeMillis());
        handlerInterface.removeNetwork();
        Log.i("Downloader", "ConnectionManager QUIT !");
        //automatic cleaning will be called here
    }

    /**
     * Message exchange scheme :
     * a) Send/Receive confirmation for pause, for both upload and download
     * b) Send/Receive confirmation for delete, for download
     * <p/>
     * Uploader : send paused, get paused, get deleted/get bytes
     * Downloader : send paused, get paused, send deleted
     *
     * @return weather a task was added or not
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
//        if (!lanRequest.isPresent())
//            return taskAdded;
//        Log.i("Downloader", "LAN Request was not present");
//        //////////////////////////////////service local_network requests
//        final SocketChannel channel = lanRequest.get().first;
//        if (!channel.isConnected() || !channel.isOpen())
//            return taskAdded;
//        connection = lanRequest.get().second;
//        if (connection == null || TextUtils.isEmpty(connection.getMessageType()))
//            return taskAdded; //low probability
//        ReachDatabase reachDatabase = getReachDatabase(
//                connection.getSongId(),
//                connection.getSenderId(),
//                connection.getReceiverId());
//        //first we perform checks
//        try {
//            if (reachDatabase != null && reachDatabase.getStatus() == ReachDatabase.PAUSED_BY_USER) {
//                channel.write(ByteBuffer.wrap(StaticData.sendFail)); //reply fail
//                Log.i("Downloader", "LAN_CONNECT for Paused/deleted operation encountered " + reachDatabase.getDisplayName());
//                return taskAdded; //high probability, paused operation
//            }
//            channel.write(ByteBuffer.wrap(StaticData.sendSuccess)); //reply success
//            final BufferedReader reader = new BufferedReader(new InputStreamReader(channel.socket().getInputStream()));
//            String message = reader.readLine();
//            //catch message.a
//            if (TextUtils.isEmpty(message) || message.equals(StaticData.getFail))
//                return taskAdded; //other end says shit is paused !
//            //////////////////////////////////
//            if (connection.getMessageType().equals("REQ")) {
//                message = reader.readLine();
//                if (TextUtils.isEmpty(message) || message.equals(StaticData.getFail))
//                    return taskAdded; //other end says shit is deleted !
//                //else its the offset
//                return (processLocalSend(channel, connection, reachDatabase, message) || taskAdded); // 1) process upload
//            }
//            //for download update latest info from memory
//            if (reachDatabase == null || (reachDatabase = reachDatabaseMemory.get(reachDatabase.getId(), null)) == null) {
//                channel.write(ByteBuffer.wrap(StaticData.sendFail)); //reply fail
//                return taskAdded; ////high probability, deleted download op
//            }
//            return (processLocalReceive(channel, reachDatabase) || taskAdded); //2) process download
//        } catch (IOException e) {
//            e.printStackTrace();
//            return taskAdded;
//        }
    }

//    private boolean processLocalReceive(SocketChannel channel, ReachDatabase reachDatabase) {
//
//        final ContentValues values = new ContentValues();
//        //setUp file
//        switch (prepareDownloadFile(reachDatabase)) {
//            case 0: { //created new file
//                reachDatabase.setPath(reachDirectory + "/" + reachDatabase.getId());
//                reachDatabase.setProcessed(0); //reset
//                values.put(ReachDatabaseHelper.COLUMN_PATH, reachDatabase.getPath());
//                values.put(ReachDatabaseHelper.COLUMN_PROCESSED, 0);
//                break;
//            }
//            //case 1 : the path and offset does not change :)
//            case 2: { //error
//                reachDatabase.setStatus(ReachDatabase.FILE_NOT_CREATED); //file not created
//                values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.FILE_NOT_CREATED);
//                updateReachDatabase(values, reachDatabase.getId());
//                reachDatabaseMemory.put(reachDatabase.getId(), reachDatabase);
//                return false;
//            }
//        }
//        for (SelectionKey selectionKey : networkManager.keys()) {
//            if (selectionKey == null || selectionKey.attachment() == null)
//                continue;
//            final DataBundle dataBundle = (DataBundle) selectionKey.attachment();
//            if (dataBundle == null)
//                continue;
//            if (dataBundle.getId() == reachDatabase.getId()) {
//                //drain and close old channel
//                long drained = 0;
//                try {
//                    drained = drainBuffer((ReadableByteChannel) selectionKey.channel(),
//                            openChannels.get(getFileChannelIndex(reachDatabase)));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                reachDatabase.setProcessed(reachDatabase.getProcessed() + drained);
//                values.put(ReachDatabaseHelper.COLUMN_PROCESSED, reachDatabase.getProcessed());
//                MiscUtils.keyCleanUp(selectionKey);
//                closeSocket(dataBundle.getReference());
//                break;
//            }
//        }
//        /** now the file is ready also we have the channel
//         ** inform the sender about the offset, where to start from **/
//        try {
//            channel.write(ByteBuffer.wrap((reachDatabase.getProcessed() + "\n").getBytes()));
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false; //fail
//        }
//        reachDatabase.setStatus(ReachDatabase.RELAY);
//        values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.RELAY);
//        //register into selector
//        final long reference = UUID.randomUUID().getMostSignificantBits();
//        openChannels.append(reference, channel);
//        try {
//            channel.configureBlocking(false).register(networkManager,
//                    SelectionKey.OP_READ, new DataBundle(
//                            reachDatabase.getId(),
//                            reachDatabase.getSongId(),
//                            reachDatabase.getReceiverId(),
//                            reachDatabase.getSenderId(),
//                            reachDatabase.getLength(),
//                            reachDatabase.getProcessed(),
//                            reference));
//        } catch (IOException e) {
//            //removal of selectionKey will be done in keyKiller
//            e.printStackTrace();
//            closeSocket(reference);
//            reset(reachDatabase);
//            toast("Firewall issue");
//            return false;
//        }
//        reachDatabase.setLastActive(System.currentTimeMillis());
//        values.put(ReachDatabaseHelper.COLUMN_LAST_ACTIVE, reachDatabase.getLastActive());
//
//        reachDatabaseMemory.put(reachDatabase.getId(), reachDatabase);
//        return updateReachDatabase(values, reachDatabase.getId());
//    }

//    /**
//     * @param channel    the current connection
//     * @param byteBuffer the file where we were writing to
//     * @return the new offset
//     */
//    private long drainBuffer(ReadableByteChannel channel, MappedByteBuffer byteBuffer) throws IOException {
//        return channel.read(byteBuffer);
//    }

//    private boolean processLocalSend(SocketChannel channel, Connection connection, ReachDatabase reachDatabase, String offset) {
//
//        final ReachDatabase newDatabase = new ReachDatabase();
//        newDatabase.setSongId(connection.getSongId());
//        newDatabase.setReceiverId(connection.getReceiverId());
//        newDatabase.setSenderId(connection.getSenderId());
//        newDatabase.setOperationKind((short) 1);
//
//        newDatabase.setLength(connection.getLength());
//        newDatabase.setProcessed(Long.parseLong(offset)); //NEW OFFSET !!
//        newDatabase.setLastActive(System.currentTimeMillis());
//        newDatabase.setAdded(System.currentTimeMillis());
//
//        newDatabase.setLogicalClock(connection.getLogicalClock());
//        newDatabase.setStatus(ReachDatabase.RELAY);
//
//        final long id;
//        if (reachDatabase != null) {
//            //get rid of relay transaction
//            for (SelectionKey selectionKey : networkManager.keys()) {
//                if (selectionKey == null || selectionKey.attachment() == null)
//                    continue; //maybe cleanup
//                final DataBundle dataBundle = (DataBundle) selectionKey.attachment();
//                if (dataBundle.getId() == reachDatabase.getId()) {
//                    MiscUtils.keyCleanUp(selectionKey);
//                    closeSocket(dataBundle.getReference());
//                    break;
//                }
//            }
//            id = reachDatabase.getId(); //reuse existing id :)
//        } else {
//            final String[] splitter = handlerInterface.getContentResolver().insert(ReachDatabaseProvider.CONTENT_URI,
//                    ReachDatabaseHelper.contentValuesCreator(newDatabase)).toString().split("/");
//            if (splitter.length == 0)
//                return false;
//            id = Long.parseLong(splitter[splitter.length - 1].trim());
//        }
//        newDatabase.setId(id);
//        Log.i("Downloader", "Local Upload op inserted " + newDatabase.getId());
//
//        final Optional<String[]> setUpFile = prepareUploadFile(connection.getSongId(),
//                connection.getSenderId()
//        );
//        if (!setUpFile.isPresent()) {
//            connection.setMessageType("404");
//            MiscUtils.sendGCM("CONNECT" + new Gson().toJson(connection, Connection.class),
//                    connection.getReceiverId(), connection.getSenderId());
//            Log.i("Downloader", "File Channel could not be created");
//            removeReachDatabase(newDatabase.getId());
//            return false;
//        }
//        //////////////////////////////////
//        final ContentValues values = new ContentValues();
//        newDatabase.setDisplayName(setUpFile.get()[0]);
//        newDatabase.setDisplayName(setUpFile.get()[1]);
//        values.put(ReachDatabaseHelper.COLUMN_DISPLAY_NAME, setUpFile.get()[0]);
//        values.put(ReachDatabaseHelper.COLUMN_ACTUAL_NAME, setUpFile.get()[1]);
//
//        final long reference = UUID.randomUUID().getMostSignificantBits();
//        openChannels.append(reference, channel);
//        try {
//            channel.configureBlocking(false).register(networkManager,
//                    SelectionKey.OP_WRITE, new DataBundle(
//                            newDatabase.getId(),
//                            newDatabase.getSongId(),
//                            newDatabase.getReceiverId(),
//                            newDatabase.getSenderId(),
//                            newDatabase.getLength(),
//                            newDatabase.getProcessed(),
//                            reference));
//        } catch (IOException e) {
//            e.printStackTrace();
//            toast("Firewall issue");
//            closeSocket(reference);
//            removeReachDatabase(newDatabase.getId());
//            return false;
//        }
//        Log.i("Downloader", "REQ Registered in Selector " + newDatabase.toString());
//        newDatabase.setLastActive(System.currentTimeMillis());
//        values.put(ReachDatabaseHelper.COLUMN_LAST_ACTIVE, newDatabase.getLastActive());
//        reachDatabaseMemory.put(newDatabase.getId(), newDatabase);
//        return updateReachDatabase(values, newDatabase.getId());
//    }

    private void closeSocket(long reference) {

        final Channel socketChannel = openChannels.get(reference, null);
        if (socketChannel == null)
            return;
        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            openChannels.remove(reference);
        }
    }

    private boolean getReachDirectory() {

        //Re-acquire the reachDirectory
        if (reachDirectory == null || !reachDirectory.exists() || !reachDirectory.isDirectory()) {

            //another classic hack, sometimes a single call does not work
            if ((reachDirectory = MiscUtils.getReachDirectory()) == null &&
                (reachDirectory = MiscUtils.getReachDirectory()) == null) {
                toast("File system error");
                return false;
            }

            if (reachDirectory.getUsableSpace() < StaticData.MINIMUM_FREE_SPACE) {
                toast("Insufficient space on sdCard");
                return false;
            }
        }
        return true;
    }

    private void sanitizeReachDirectory() {

        //////////////////////////////////purge all upload operations, but retain paused operations
        handlerInterface.getContentResolver().delete(
                ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                        ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                new String[]{1 + "", ReachDatabase.PAUSED_BY_USER + ""});
        //////////////////////////////////refresh reachDatabase : handle directory invalidation
        final Cursor cursor = handlerInterface.getContentResolver().query
                (ReachDatabaseProvider.CONTENT_URI,
                        ReachDatabaseHelper.projection,
                        null,
                        null,
                        null);
        if (cursor == null) return; //this should never happen
        //first we mask all operations as ReachDatabase.FILE_NOT_CREATED
        while (cursor.moveToNext()) {

            final ReachDatabase reachDatabase = ReachDatabaseHelper.cursorToProcess(cursor);
            if (reachDatabase.getProcessed() > 0) //mask only those which have associated file
                reachDatabase.setStatus(ReachDatabase.FILE_NOT_CREATED);
            else
                reachDatabase.setStatus(ReachDatabase.NOT_WORKING);
            reachDatabaseMemory.put(reachDatabase.getId(), reachDatabase);
        }
        cursor.close();
        //now we scan the directory and unmask those files that were found
        for (File file : reachDirectory.listFiles()) {

            final ReachDatabase reachDatabase = reachDatabaseMemory.get(Long.parseLong(file.getName().replaceAll("[^0-9]", "")), null);
            if (reachDatabase == null) continue;
//                Log.i("Downloader", "DELETING FILE " + file.getName() + " " + file.delete());
            if (reachDatabase.getProcessed() == reachDatabase.getLength())
                reachDatabase.setStatus(ReachDatabase.FINISHED);  //completed
            else reachDatabase.setStatus(ReachDatabase.NOT_WORKING); //not working
        }
    }

    private boolean setUpNetworkManager() {

        try {
            networkManager = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
            if (networkManager != null)
                try {
                    networkManager.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            return false;
        }
        return true;
    }

    /**
     * Pause and Delete in disk are newer.
     * For a download operation update information from memory (for processed value).
     *
     * @return Returns if a task was added or not
     */
    private boolean startOperation(Connection connection) {

        Log.i("Downloader", "processing network request");
        if (connection == null || TextUtils.isEmpty(connection.getMessageType())) {
            Log.i("Downloader", "illegal network request");
            return false;
        }
        ReachDatabase reachDatabase = getReachDatabase(
                connection.getSongId(),
                connection.getSenderId(),
                connection.getReceiverId());
        //first we perform checks
        if (reachDatabase != null && reachDatabase.getStatus() == ReachDatabase.PAUSED_BY_USER) {
            //TODO we should also perform this check in GCMIntentService
            Log.i("Downloader", "CONNECT for Paused/deleted operation encountered " + reachDatabase.getDisplayName());
            return false;
        }

        switch (connection.getMessageType()) {

            case "REQ": {
                //new upload op
                if (reachDatabase == null)
                    return handleSend(connection);
                    //currently on-going, but higher priority received
                else if (reachDatabase.getLogicalClock() < connection.getLogicalClock()) {

                    for (SelectionKey selectionKey : networkManager.keys()) {
                        if (selectionKey == null || selectionKey.attachment() == null)
                            continue;
                        final DataBundle dataBundle = (DataBundle) selectionKey.attachment();
                        if (dataBundle.getId() == reachDatabase.getId()) {
                            MiscUtils.keyCleanUp(selectionKey);
                            closeSocket(dataBundle.getReference());
                            break;
                        }
                    }
                    Log.i("Downloader", "Servicing higher logical clock " + connection.getLogicalClock());
                    removeReachDatabase(reachDatabase.getId());
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
                final ReachDatabase temp = reachDatabaseMemory.get(reachDatabase.getId(), null);
                if(temp != null)
                    reachDatabase = temp;
                if (reachDatabase.getStatus() == ReachDatabase.WORKING ||
                        reachDatabase.getStatus() == ReachDatabase.RELAY ||
                        reachDatabase.getStatus() == ReachDatabase.FINISHED) {
                    Log.i("Downloader", "Dropping RELAY " + reachDatabase.getStatus());
                    break;
                } else if (reachDatabase.getProcessed() != connection.getOffset()) {

                    //illegal connection acknowledgement, offset not correct, force increment
                    Log.i("Downloader", "INCREMENTING Logical Clock");
                    reachDatabase.setLogicalClock((short) (reachDatabase.getLogicalClock() + 1));
                    StaticData.threadPool.submit(MiscUtils.startDownloadOperation(reachDatabase, handlerInterface.getContentResolver()));
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
        switch (prepareDownloadFile(reachDatabase)) {

            case 0: { //created new file
                reachDatabase.setPath(reachDirectory + "/" + reachDatabase.getId());
                reachDatabase.setProcessed(0); //reset
                values.put(ReachDatabaseHelper.COLUMN_PATH, reachDatabase.getPath());
                values.put(ReachDatabaseHelper.COLUMN_PROCESSED, 0);
                break;
            }
            case 2: { //error
                reachDatabase.setStatus(ReachDatabase.FILE_NOT_CREATED); //file not created
                values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.FILE_NOT_CREATED);
                updateReachDatabase(values, reachDatabase.getId());
                reachDatabaseMemory.put(reachDatabase.getId(), reachDatabase);
                Log.i("Downloader", "Error loading file");
                return false;
            }
        }
        //establish relay
        try {
            return connectRelay((short) 0, reachDatabase, connection.getUniqueIdReceiver(), connection.getUniqueIdSender(), values);
        } finally {
            //attempt a LAN connection (even if relay failed)
//            StaticData.threadPool.submit(new LanRequester(connection));
        }
    }

//    private class LanRequester implements Runnable {
//
//        private final Connection connection;
//
//        private LanRequester(Connection connection) {
//            this.connection = connection;
//        }
//
//        private void fail(Closeable... closeables) {
//            for (Closeable closeable : closeables)
//                try {
//                    closeable.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//        }
//
//        @Override
//        public void run() {
//
//            Log.i("Downloader", "Received senderIp = " + connection.getSenderIp());
//            final BufferedReader reader;
//            final SocketChannel channel;
//            final OutputStream stream;
//            try {
//                channel = SocketChannel.open();
//                channel.configureBlocking(true);
//                channel.connect(new InetSocketAddress(connection.getSenderIp(), 60001)); //TODO test and fix
//                channel.finishConnect();
//                stream = channel.socket().getOutputStream();
//                reader = new BufferedReader(new InputStreamReader(channel.socket().getInputStream()));
//            } catch (IOException e) {
//                e.printStackTrace();
//                return;
//            }
//
//            final String message = new Gson().toJson(connection, Connection.class);
//            try {
//                stream.write(message.getBytes());
//            } catch (IOException e) {
//                e.printStackTrace();
//                fail(channel, stream);
//                return;
//            }
//
//            final String reply;
//            try {
//                reply = reader.readLine();
//            } catch (IOException e) {
//                e.printStackTrace();
//                fail(channel, stream, reader);
//                return;
//            }
//
//            if (TextUtils.isEmpty(reply) || reply.equals("fail")) {
//                fail(channel, stream, reader);
//                return;
//            }
//            //if LAN was successful we add to LAN processing queue
//            submitLanRequest(channel, connection);
//        }
//    }

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
        reachDatabaseMemory.append(reachDatabase.getId(), reachDatabase);
        Log.i("Downloader", "Upload op inserted " + reachDatabase.getId());

        final Optional<String[]> setUpFile = prepareUploadFile(connection.getSongId(),
                connection.getSenderId()
        );
        if (!setUpFile.isPresent()) {
            connection.setMessageType("404");
            MiscUtils.sendGCM("CONNECT" + new Gson().toJson(connection, Connection.class),
                    connection.getReceiverId(), connection.getSenderId());
            Log.i("Downloader", "File Channel could not be created");
            removeReachDatabase(reachDatabase.getId());
            return false;
        }
        //////////////////////////////////
//        Log.i("Downloader", "Setting senderIp " + lanAddress.toString());
//        connection.setSenderIp(lanAddress.toString()); //TODO verify

        final Cursor receiverName = handlerInterface.getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + connection.getReceiverId()),
                new String[]{ReachFriendsHelper.COLUMN_USER_NAME,
                ReachFriendsHelper.COLUMN_NETWORK_TYPE,
                ReachFriendsHelper.COLUMN_STATUS},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{connection.getReceiverId()+""}, null);

        //TODO handle the situation when friend  not found
        if(receiverName == null) {
            removeReachDatabase(reachDatabase.getId());
            return false;
        }
        if(!receiverName.moveToFirst()) {
            receiverName.close();
            removeReachDatabase(reachDatabase.getId());
            return false;
        }
        reachDatabase.setSenderName(receiverName.getString(0));
        reachDatabase.setNetworkType(receiverName.getShort(1)+"");
        reachDatabase.setOnlineStatus(receiverName.getShort(2)+"");
        receiverName.close();

        final ContentValues values = new ContentValues();
        reachDatabase.setDisplayName(setUpFile.get()[0]);
        reachDatabase.setActualName(setUpFile.get()[1]);
        values.put(ReachDatabaseHelper.COLUMN_DISPLAY_NAME, setUpFile.get()[0]);
        values.put(ReachDatabaseHelper.COLUMN_ACTUAL_NAME, setUpFile.get()[1]);
        values.put(ReachDatabaseHelper.COLUMN_SENDER_NAME, reachDatabase.getSenderName());
        values.put(ReachDatabaseHelper.COLUMN_NETWORK_TYPE, reachDatabase.getNetworkType());
        values.put(ReachDatabaseHelper.COLUMN_ONLINE_STATUS, reachDatabase.getOnlineStatus());

        if(connectRelay((short) 1, reachDatabase, connection.getUniqueIdReceiver(), connection.getUniqueIdSender(), values)) {

            connection.setMessageType("RELAY");
            final MyBoolean myBoolean = MiscUtils.sendGCM("CONNECT" + new Gson().toJson(connection, Connection.class),
                    connection.getReceiverId(), connection.getSenderId());
            if (myBoolean != null && myBoolean.getOtherGCMExpired()) {
                Log.i("Downloader", "GCM FAILED NOT SENDING " + connection.toString());
                //gcm failed, no point in continuing
                removeReachDatabase(reachDatabase.getId());
                return false;
            }
        }
        return true;
    }

    /**
     * Connects to the relay and registers into selector
     *
     * @param type 0 : download, 1 : upload
     * @return weather successful or not
     */
    private boolean connectRelay(short type, ReachDatabase reachDatabase, long uniqueReceiverId,
                                 long uniqueSenderId, ContentValues values) {

        final long reference = UUID.randomUUID().getMostSignificantBits();
        try {
            //Update fileChannel
            final SocketChannel socketChannel;
            openChannels.append(reference, socketChannel = SocketChannel.open());
            socketChannel.configureBlocking(true);
//            socketChannel.socket().setSoLinger(true, 1);
//            socketChannel.socket().setKeepAlive(true);
//            socketChannel.socket().setTrafficClass(0x10);
//            socketChannel.socket().setPerformancePreferences(0, 1, 0);
            socketChannel.socket().connect(vmAddress, 5000);

            final PrintWriter printWriter = new PrintWriter(socketChannel.socket().getOutputStream());
            final int interestOp;
            if (type == 1) {
                printWriter.println(uniqueSenderId);
                printWriter.println(uniqueReceiverId);
                printWriter.println("Sender");
                interestOp = SelectionKey.OP_WRITE;
            } else {
                printWriter.println(uniqueReceiverId);
                printWriter.println(uniqueSenderId);
                printWriter.println("Receiver");
                interestOp = SelectionKey.OP_READ;
            }
            printWriter.flush(); //Blocking mode exception, cant put after set to non-blocking

            socketChannel.configureBlocking(false).register(networkManager,
                    interestOp, new DataBundle(
                            reachDatabase.getId(),
                            reachDatabase.getSongId(),
                            reachDatabase.getReceiverId(),
                            reachDatabase.getSenderId(),
                            reachDatabase.getLength(),
                            reachDatabase.getProcessed(),
                            reference));
        } catch (IOException | AssertionError e) {

            //TODO make a central GCM and relay connectivity check
            //removal of selectionKey will be done in keyKiller
            e.printStackTrace();
            toast("Firewall issue");
            closeSocket(reference);
            if (type == 1)
                removeReachDatabase(reachDatabase.getId());
            else
                reset(reachDatabase);
            return false;
        }

        reachDatabase.setLastActive(System.currentTimeMillis());
        values.put(ReachDatabaseHelper.COLUMN_LAST_ACTIVE, reachDatabase.getLastActive());
        reachDatabaseMemory.put(reachDatabase.getId(), reachDatabase);
        return updateReachDatabase(values, reachDatabase.getId());
    }

    /**
     * Prepares the mapped byte buffer for the song.
     * CARE the position of the buffer is SET, in this method itself.
     *
     * @return .absent if error, else the name and file
     */
    private Optional<String[]> prepareUploadFile(long songId, long senderId) {

        final Cursor cursor = handlerInterface.getContentResolver().query(
                ReachSongProvider.CONTENT_URI,
                new String[]{
                        ReachSongHelper.COLUMN_ID,
                        ReachSongHelper.COLUMN_DISPLAY_NAME,
                        ReachSongHelper.COLUMN_ACTUAL_NAME,
                        ReachSongHelper.COLUMN_PATH,
                        ReachSongHelper.COLUMN_VISIBILITY
                },
                ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                        ReachSongHelper.COLUMN_SONG_ID + " = ?",
                new String[]{senderId + "", songId + ""}, null);
        //handle visibility as well
        if (cursor == null || !cursor.moveToFirst() || cursor.getShort(4) == 0) {
            Log.i("Downloader", "Requested song path invalid");
            return Optional.absent();
        }
        final String[] names = new String[]{cursor.getString(1),  //display name
                cursor.getString(2)}; //actual name
        final File file = new File(cursor.getString(3));   //path
        cursor.close();

        if (!(file.isFile() || file.isFile()) || //if ANY of the two return true, we are good to proceed
                !(file.length() > 0 || file.length() > 0)) {
            Log.i("Downloader", "Requested song file invalid");
            return Optional.absent();
        }
        //song found, continuing

        final long fileChannelIndex = getFileChannelIndex(names[1], names[0]);
        if(openChannels.get(fileChannelIndex, null) == null || !openChannels.get(fileChannelIndex).isOpen())
            try {
                openChannels.put(fileChannelIndex, new RandomAccessFile(file, "r").getChannel());
                Log.i("Downloader", "Index for " + names[0] + " = " + fileChannelIndex);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.i("Downloader", "Requested song file not found");
                return Optional.absent();
            }

        return Optional.of(names);
    }

    /**
     * @return
     * 0 : created new file
     * 1 : re-used old file
     * 2 : error
     */
    private byte prepareDownloadFile(ReachDatabase reachDatabase) {

        final byte result;
        final long fileChannelIndex = getFileChannelIndex(reachDatabase);
        Log.i("Downloader", "Index for " + reachDatabase.getDisplayName() + " = " + fileChannelIndex);
        if(openChannels.get(fileChannelIndex, null) != null && openChannels.get(fileChannelIndex, null).isOpen())
            return 1;

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
        if (reachDatabase.getPath().equals("hello_world") ||
                (file = new File(reachDatabase.getPath())) == null ||
                !(file.isFile() || file.isFile()) || //if ANY of the two return true, we are good to proceed
                !(file.length() >= reachDatabase.getLength() || file.length() >= reachDatabase.getLength())) {

            Log.i("Downloader", "Creating new File for " + reachDatabase.getDisplayName());
            try {
                randomAccessFile = new RandomAccessFile(reachDirectory + "/" + reachDatabase.getId(), "rw");
                randomAccessFile.setLength(reachDatabase.getLength());
                randomAccessFile.seek(0); //should rewind
            } catch (IOException e) {
                e.printStackTrace();
                return 2;
            }
            result = 0;
        } else {
            /**
             * 1) We use the pre-existing file
             * 2) We do not mess with the length and file pointer to keep
             *    mediaPlayer happy
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

    private void sync(long currentTime) {

        if (currentTime - lastSync < 3500L)
            return;
        lastSync = currentTime;
        tempDiskHolder.clear();
        final Cursor cursor = handlerInterface.getContentResolver().query(
                ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.projection,
                null, null, null);
        if (cursor == null)
            return;
        boolean needToUpdate = false;
        final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        while (cursor.moveToNext()) {

            final ReachDatabase diskReach = ReachDatabaseHelper.cursorToProcess(cursor);
            tempDiskHolder.append(diskReach.getId(), diskReach);
            final ReachDatabase memoryReach = reachDatabaseMemory.get(diskReach.getId(), null);

            if (memoryReach == null) {

                if (diskReach.getStatus() != ReachDatabase.PAUSED_BY_USER) {
                    if (diskReach.getOperationKind() == 1) {
                        //illegal presence of upload operation
                        removeReachDatabase(diskReach.getId());
                    } else if (diskReach.getProcessed() < diskReach.getLength() &&
                            diskReach.getStatus() != ReachDatabase.NOT_WORKING) {

                        //we fix any messed up download entries
                        final ContentValues values = new ContentValues();
                        values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.NOT_WORKING);
                        operations.add(MiscUtils.getUpdateOperation(values, diskReach.getId()));
                    }
                }
                continue;
            }
            //Handle Pause and Un-pause
            if (diskReach.getStatus() != memoryReach.getStatus()) {

                needToUpdate = true;
                if (diskReach.getStatus() == ReachDatabase.PAUSED_BY_USER) {
                    Log.i("Downloader", "Pausing " + memoryReach.getDisplayName());
                    memoryReach.setStatus(ReachDatabase.PAUSED_BY_USER);
                } else if (memoryReach.getStatus() == ReachDatabase.PAUSED_BY_USER) {
                    Log.i("Downloader", "Un-Pausing actual " + memoryReach.getDisplayName());
                    memoryReach.setStatus(ReachDatabase.NOT_WORKING);
                }
            }
            //for everything else memory has a higher priority
            if (diskReach.getStatus() != memoryReach.getStatus() ||
                    diskReach.getProcessed() != memoryReach.getProcessed() ||
                    diskReach.getLastActive() != memoryReach.getLastActive() ||
                    diskReach.getLogicalClock() != memoryReach.getLogicalClock()) {
                //sync the disk, other than status memory is always newer
                /**
                 * Logical Clock, Status, Last Active and Processed value can change
                 */
                final ContentValues contentValues = new ContentValues();
                contentValues.put(ReachDatabaseHelper.COLUMN_LOGICAL_CLOCK, memoryReach.getLogicalClock());
                contentValues.put(ReachDatabaseHelper.COLUMN_STATUS, memoryReach.getStatus());
                contentValues.put(ReachDatabaseHelper.COLUMN_LAST_ACTIVE, memoryReach.getLastActive());
                contentValues.put(ReachDatabaseHelper.COLUMN_PROCESSED, memoryReach.getProcessed());
                operations.add(MiscUtils.getUpdateOperation(contentValues, memoryReach.getId()));
            }
        }
        try {
            if (operations.size() > 0)
                handlerInterface.getContentResolver().applyBatch(ReachDatabaseProvider.AUTHORITY, operations);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        } finally {
            operations.clear();
            cursor.close();
            if(needToUpdate)
                handlerInterface.updateNetworkDetails();
        }

        /**
         * remove operations that have been deleted
         */
        for (int i = 0; i < reachDatabaseMemory.size(); i++) {

            final ReachDatabase memoryReach = reachDatabaseMemory.valueAt(i);
            if (memoryReach == null)
                continue;
            if (tempDiskHolder.get(memoryReach.getId(), null) == null) {

//                Log.i("Downloader", new File(memoryReach.getPath()).delete() + " Disk Reach not found, deleting file");
                Log.i("Downloader", "Disk Reach not found, DELETING FILE");
                if (memoryReach.getOperationKind() == 0)
                    closeSocket(getFileChannelIndex(memoryReach));
                //sockets get cleaned in handleOp because reachDatabase will be lost
                removeReachDatabase(memoryReach.getId());
            }
        }
        tempDiskHolder.clear();
    }

    private void handleOp(SelectionKey selectionKey) {

        final DataBundle bundle;
        if (selectionKey == null || (bundle = (DataBundle) selectionKey.attachment()) == null) {
            Log.i("Downloader", "Selection key null");
            MiscUtils.keyCleanUp(selectionKey);
            return;
        }

        final ReachDatabase reachDatabase = reachDatabaseMemory.get(bundle.getId(), null);
        if (reachDatabase == null) {
            Log.i("Downloader", "ReachDatabase key null");
            MiscUtils.keyCleanUp(selectionKey);
            closeSocket(bundle.getReference());
            return;
        }
        final long fileChannelIndex = getFileChannelIndex(reachDatabase);

        if (reachDatabase.getStatus() == ReachDatabase.PAUSED_BY_USER) {
            Log.i("Downloader", "Status checker, PAUSED ReachDatabase found");
            MiscUtils.keyCleanUp(selectionKey);
            closeSocket(bundle.getReference());
            if (reachDatabase.getOperationKind() == 0)
                closeSocket(fileChannelIndex); //TODO close only if download, refine for upload
            //in-case of pause we don't remove from disk
            reachDatabaseMemory.remove(reachDatabase.getId());
            return;
        }

        final SocketChannel socketChannel;
        if (!selectionKey.isValid() ||
                (socketChannel = (SocketChannel) selectionKey.channel()) == null ||
                !socketChannel.isOpen() ||
                !socketChannel.isConnected()) {
            Log.i("Downloader", "InvalidKey detected");
            MiscUtils.keyCleanUp(selectionKey);
            closeSocket(bundle.getReference());
            if (reachDatabase.getOperationKind() == 0)
                reachDatabase.setStatus(ReachDatabase.NOT_WORKING);
            else //for upload just remove
                removeReachDatabase(reachDatabase.getId());
            return;
        }
        //Finally handleOp if everything fine
        final FileChannel fileChannel = (FileChannel) openChannels.get(fileChannelIndex);
        long bytesChanged = 0;
        final short DOWNLOAD_FAIL = -1, UPLOAD_FAIL = -2;

        switch (selectionKey.interestOps()) {

            case SelectionKey.OP_READ: {

                if (fileChannel == null || !fileChannel.isOpen()) {
                    Log.i("Downloader", "DOWNLOAD FILE CHANNEL NOT FOUND");
                    bundle.setBytesProcessed(DOWNLOAD_FAIL);
                }
                else {
                    try {
                        bytesChanged = fileChannel.transferFrom(socketChannel, bundle.getBytesProcessed(), 5000);
                    } catch (IOException e) {
                        bundle.setBytesProcessed(DOWNLOAD_FAIL); //close this channel and resetWorkingMode
                        e.printStackTrace();
                    }
                }
                break;
            }

            case SelectionKey.OP_WRITE: {

                if (fileChannel == null || !fileChannel.isOpen()) {
                    Log.i("Downloader", "UPLOAD FILE CHANNEL NOT FOUND");
                    bundle.setBytesProcessed(UPLOAD_FAIL);
                }
                else {
                    try {
                        /**JNI bug in java/android transferTo fails, refer the below link
                         * http://stackoverflow.com/questions/28819743 seems to be device specific
                         */
//                    bytesChanged = fileChannel.transferTo
//                            (bundle.getBytesProcessed(), bytesSize, channel);
                        if(fileChannel.read(transferBuffer, bundle.getBytesProcessed()) > 0) {
                            transferBuffer.flip();
                            while (transferBuffer.hasRemaining())
                                bytesChanged += socketChannel.write(transferBuffer);
                        }
                    } catch (IOException e) {
                        bundle.setBytesProcessed(UPLOAD_FAIL); //close and delete
                        e.printStackTrace();
                    } finally {
                        transferBuffer.clear();
                    }
                }
                break;
            }

            default: {
                //illegal operation
                MiscUtils.keyCleanUp(selectionKey);
                closeSocket(bundle.getReference());
                removeReachDatabase(reachDatabase.getId());
                return;
            }
        }

        if (bundle.getBytesProcessed() == DOWNLOAD_FAIL) { //Has to be a failed download
            Log.i("Downloader", "NETWORK ERROR " + reachDatabase.getDisplayName());
            reachDatabase.setStatus(ReachDatabase.NOT_WORKING);
            reachDatabase.setLogicalClock((short) (reachDatabase.getLogicalClock() + 1));
            StaticData.threadPool.submit(MiscUtils.startDownloadOperation(reachDatabase, handlerInterface.getContentResolver()));
            handlerInterface.downloadFail(reachDatabase.getDisplayName());
            MiscUtils.keyCleanUp(selectionKey);
            closeSocket(bundle.getReference());
            return;
        }
        if (bundle.getBytesProcessed() == UPLOAD_FAIL) {
            //Has to be an upload
            Log.i("Downloader", "Upload ended " + bundle.getBytesProcessed() + " " + bundle.getLength());
            removeReachDatabase(reachDatabase.getId());
            MiscUtils.keyCleanUp(selectionKey);
            closeSocket(bundle.getReference());
            return;
        }
        if (bytesChanged > 0) {
//            Log.i("Downloader", "Bytes changed " + bytesChanged);
            bundle.setBytesProcessed(bundle.getBytesProcessed() + bytesChanged);
            final long currentTime = System.currentTimeMillis();
            bundle.setLastActive(currentTime);
            reachDatabase.setLastActive(currentTime);
            reachDatabase.setProcessed(bundle.getBytesProcessed());
        }

        if (bundle.getBytesProcessed() < bundle.getLength())
            return; //partially completed

        //operation compete
        switch (selectionKey.interestOps()) {

            case SelectionKey.OP_READ: {

                Log.i("Downloader", "Download ended " + bundle.getBytesProcessed() + " " + bundle.getLength());
                //Sync upload success to server
                reachDatabase.setStatus(ReachDatabase.FINISHED); //download finished
                MiscUtils.autoRetryAsync(new DoWork<Void>() {
                    @Override
                    protected Void doWork() throws IOException {

                        return StaticData.userEndpoint.updateCompletedOperations(
                                reachDatabase.getReceiverId(),
                                reachDatabase.getSenderId(),
                                reachDatabase.getDisplayName(),
                                reachDatabase.getLength()).execute();
                    }
                }, Optional.<Predicate<Void>>absent());

                MiscUtils.keyCleanUp(selectionKey);
                closeSocket(bundle.getReference());
                closeSocket(fileChannelIndex);
                return; //download complete
            }

            case SelectionKey.OP_WRITE: {

                Log.i("Downloader", "Upload ended " + bundle.getBytesProcessed() + " " + bundle.getLength());
                removeReachDatabase(reachDatabase.getId());
                /**TODO test :
                 * In-case of upload we do not close the socket right away.
                 * We shut down the input/output and leave it hanging.
                 * We do not close to allow system buffer to clear up
                 */
                selectionKey.cancel();
                openChannels.remove(fileChannelIndex);
            }
            //default case already handled
        }
    }

    /**
     * Kills the channels/keys that are sleeping for more than 60 secs
     *
     * @param keys the set of keys to check
     */
    private void keyKiller(Set<SelectionKey> keys) {

        final long currentTime = System.currentTimeMillis();
        if (currentTime - lastKeyKill < 3500L)
            return;
        lastKeyKill = currentTime;
        for (SelectionKey selectionKey : keys) {

            if (selectionKey == null || selectionKey.attachment() == null) {
                MiscUtils.keyCleanUp(selectionKey);
                continue;
            }

            final DataBundle bundle = (DataBundle) selectionKey.attachment();
            if (currentTime - bundle.getLastActive() <= 60 * 1001) continue;
            Log.i("Downloader", "Running keyKiller");
            try {
                final ReachDatabase reachDatabase = reachDatabaseMemory.get(bundle.getId());
                if (reachDatabase == null) continue;

                if (reachDatabase.getOperationKind() == 0) {
                    reset(reachDatabase);
                    handlerInterface.downloadFail(reachDatabase.getDisplayName());
                } else
                    removeReachDatabase(reachDatabase.getId());
            } finally {
                MiscUtils.keyCleanUp(selectionKey);
                closeSocket(bundle.getReference());
            }
        }
    }

    /**
     * Resets the transaction, reset download only. Updates memory cache and disk table both.
     * Update happens in MiscUtils.startDownloadOperation() method.
     *
     * @param reachDatabase the transaction that needs to be rest
     */
    private void reset(ReachDatabase reachDatabase) {

        final ContentValues values = new ContentValues();
        values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.NOT_WORKING);
        reachDatabase.setStatus(ReachDatabase.NOT_WORKING);
        values.put(ReachDatabaseHelper.COLUMN_LOGICAL_CLOCK, (short) (reachDatabase.getLogicalClock() + 1));
        reachDatabase.setLogicalClock((short) (reachDatabase.getLogicalClock() + 1));

        reachDatabaseMemory.put(reachDatabase.getId(), reachDatabase);
        Log.i("Downloader", "INCREMENTING Logical Clock " + updateReachDatabase(values, reachDatabase.getId()));
        StaticData.threadPool.submit(MiscUtils.startDownloadOperation(reachDatabase, handlerInterface.getContentResolver()));
    }

    /**
     * Removes the reachDatabase entry from disk and memory
     *
     * @param id of reachDatabase to remove
     */
    private void removeReachDatabase(long id) {

        handlerInterface.getContentResolver().delete(
                Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id),
                ReachDatabaseHelper.COLUMN_ID + " = ?",
                new String[]{id + ""});
        reachDatabaseMemory.remove(id);
        handlerInterface.updateNetworkDetails();
    }

    /**
     * Updates the reachDatabase table and if update fails, completely
     * removes the transaction
     *
     * @param contentValues the values that need to be updated
     * @param id            the id of the row that needs to be updated (reachDatabase id)
     * @return weather update was successful or not
     */
    private boolean updateReachDatabase(ContentValues contentValues, long id) {

        if (handlerInterface.getContentResolver().update(
                Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id),
                contentValues,
                ReachDatabaseHelper.COLUMN_ID + " = ? and " + ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                new String[]{id+"", ""+ReachDatabase.PAUSED_BY_USER}) > 0) return true;

        for (SelectionKey selectionKey : networkManager.keys()) {

            if (selectionKey == null || selectionKey.attachment() == null) {
                MiscUtils.keyCleanUp(selectionKey);
                continue;
            }
            final DataBundle dataBundle = (DataBundle) selectionKey.attachment();
            if (dataBundle.getId() == id) {
                MiscUtils.keyCleanUp(selectionKey);
                closeSocket(dataBundle.getReference());
                break;
            }
        }
//        closeSocket(); TODO
        removeReachDatabase(id);
        return false;
    }

    /**
     * @param songId     the songId on sender side
     * @param senderId   server-id of sender
     * @param receiverId server-id of receiver
     * @return the reachDatabase object corresponding to parameters
     */
    public ReachDatabase getReachDatabase(long songId, long senderId, long receiverId) {

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

//    @Override
//    public boolean submitLanRequest(SocketChannel channel, Connection connection) {
//        return pendingLanRequests.insertMessage(new Pair<>(channel, connection));
//    }
//
//    @Override
//    public long getMyId() {
//        return SharedPrefUtils.getServerId(handlerInterface.getSharedPreferences());
//    }

    /**
     * @param message that needs to be shown as toast
     */
    private void toast(final String message) {

        Log.i("Downloader", message);
        //TODO, this will be done using a messenger OR no toast, something else
//        if (ReachQueueActivity.reference == null)
//            return;
//        final ReachQueueActivity activity = ReachQueueActivity.reference.get();
//        if (activity == null)
//            return;
//        activity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
//            }
//        });
    }

    private long getFileChannelIndex(ReachDatabase reachDatabase) {

        if(TextUtils.isEmpty(reachDatabase.getActualName()))
            return MiscUtils.stringToLongHashCode(reachDatabase.getDisplayName());
        return MiscUtils.stringToLongHashCode(reachDatabase.getDisplayName() + reachDatabase.getActualName());
    }

    private long getFileChannelIndex(String actualName, String displayName) {
        if(TextUtils.isEmpty(actualName))
            return MiscUtils.stringToLongHashCode(displayName);
        return MiscUtils.stringToLongHashCode(displayName + actualName);
    }

    public interface NetworkHandlerInterface {

        WifiManager getWifiManager();

        ContentResolver getContentResolver();

        SharedPreferences getSharedPreferences();

        void updateNetworkDetails();

//        Future<?> submitChild(ReachTask runnable);

        void removeNetwork();

        void downloadFail(String songName);
    }
}