package reach.project.reachProcess.reachService;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.RemoteException;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.project.core.StaticData;
import reach.project.utils.auxiliaryClasses.ReachDatabase;
import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.database.sql.ReachSongHelper;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.auxiliaryClasses.ReachTask;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.utils.MiscUtils;

/**
 * Created by Dexter on 18-05-2015.
 * This is an auto-close type parent thread.
 * Explicit close only if user requests, using kill (Exit)
 * <p/>
 * Process all network operations on a single thread.
 * a) scan for new requests
 * b) process current network operations
 * c) kill sleeping operations and sync up with database
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
    private static final LongSparseArray<Channel> openChannels = new LongSparseArray<>(100); //needs closing
    //message holder
    private final ConcurrentLinkedQueue<String> pendingNetworkRequests = new ConcurrentLinkedQueue<>();
    //    private final ConcurrentLinkedQueue<Pair<SocketChannel, Connection>> pendingLanRequests = new ConcurrentLinkedQueue<>(); //needs closing
    private final ByteBuffer transferBuffer = ByteBuffer.allocateDirect(4096);

    private File reachDirectory = null;
    private Selector networkManager = null; //needs closing
    private WifiManager.WifiLock wifiLock = null; //needs closing
    private ThreadPoolExecutor threadPool;
    private long lastOperation = 0;

    //    private SocketAddress lanAddress = null; TODO
//    private final LanHandler lanHandler = new LanHandler(this); //needs closing
//    private Future<?> lanHandlerFuture = null;
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
        //////////////////////////////////
        Log.i("Downloader", "Releasing network manager");
        MiscUtils.closeAndIgnore(networkManager);
        Log.i("Downloader", "NetworkManager released");
        //////////////////////////////////
        for (int i = 0, size = openChannels.size(); i < size; i++)
            MiscUtils.closeAndIgnore(openChannels.valueAt(i));
        openChannels.clear();
        Log.i("Downloader", "Downloader openChannels released");
        //////////////////////////////////
        if (threadPool != null)
            threadPool.shutdownNow();
        threadPool = null;
//        Pair<SocketChannel, Connection> temp;
//        while ((temp = pendingLanRequests.poll()) != null && temp.first != null) {
//            try {
//                temp.first.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        pendingLanRequests.clear();
//        Log.i("Downloader", "Downloader pendingLanRequests released");
        //////////////////////////////////
        transferBuffer.clear();
        Log.i("Downloader", "Downloader cleaning up");
    }

    @Override
    protected void performTask() {

        Log.i("Downloader", "Network handler thread started");
        kill.set(false);
//        lanAddress = lanHandler.getLocalSocketAddress();
//        Log.i("Downloader", "LAN address = " + lanAddress);

        //getDirectory
        if ((reachDirectory = LocalUtils.getDirectory()) == null &&
                (reachDirectory = LocalUtils.getDirectory()) == null) {
            LocalUtils.toast("File system error");
            return;
        }
        //check space
        if (reachDirectory.getUsableSpace() < StaticData.MINIMUM_FREE_SPACE) {
            LocalUtils.toast("Insufficient space on sdCard");
            return;
        }
        //get networkManager
        try {
            networkManager = Selector.open();
        } catch (IOException ignored) {
            LocalUtils.toast("Could not get networkManager");
            return;
        }

        LocalUtils.sanitizeReachDirectory(handlerInterface, reachDirectory); //sanitizeReachDirectory
        (wifiLock = handlerInterface.getWifiManager().createWifiLock(WifiManager.WIFI_MODE_FULL, "network_lock")).acquire(); //lock wifi
        threadPool = (ThreadPoolExecutor) Executors.unconfigurableExecutorService(Executors.newCachedThreadPool()); //create thread pool
        //////////////////////////////////
        //////////////////////////////////
        long lastActive = System.currentTimeMillis(), currentTime;
        boolean kill = false, sleeping = false, taskAdded;
        Set<SelectionKey> keySet;
        Log.i("Downloader", "Initialization done");
        while (true) { //start
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
                        !taskAdded && //no task that is getting added (no taskAdded)
                        threadPool.getQueue().isEmpty() && //no task that is in queue
                        threadPool.getActiveCount() == 0 &&  //no task that is in running
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
        runOperation(Collections.EMPTY_LIST, handlerInterface);
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
        //load from disk
        ReachDatabase reachDatabase = LocalUtils.getReachDatabase(
                handlerInterface,
                connection.getSongId(),
                connection.getSenderId(),
                connection.getReceiverId());
        //already done in intentService
//        if (reachDatabase != null && reachDatabase.getStatus() == ReachDatabase.PAUSED_BY_USER) {
//            Log.i("Downloader", "CONNECT for Paused/deleted operation encountered " + reachDatabase.getDisplayName());
//            return false;
//        }

        switch (connection.getMessageType()) {

            case "REQ": {
                //new upload op
                if (reachDatabase == null)
                    return handleSend(connection);
                    //currently on-going, but higher priority received
                else if (reachDatabase.getLogicalClock() < connection.getLogicalClock()) {

                    LocalUtils.removeKeyWithId(networkManager.keys(), reachDatabase.getId());
                    LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId());
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
                        reachDatabase.getStatus() == ReachDatabase.FINISHED) {
                    Log.i("Downloader", "Dropping RELAY " + reachDatabase.getStatus());
                    break;
                } else if (reachDatabase.getProcessed() != connection.getOffset()) {

                    //illegal connection acknowledgement, offset not correct, force increment
                    Log.i("Downloader", "INCREMENTING Logical Clock");
                    final Optional<Runnable> optional = LocalUtils.reset(reachDatabase, handlerInterface);
                    if (optional.isPresent())
                        threadPool.submit(optional.get());
                    else
                        LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId());
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
                break;
            }
            case 2: { //error
                reachDatabase.setStatus(ReachDatabase.FILE_NOT_CREATED); //file not created
                values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.FILE_NOT_CREATED);

                final boolean updateSuccess = LocalUtils.updateReachDatabase(
                        values,
                        handlerInterface,
                        reachDatabase.getId());
                if (!updateSuccess) //remove entry if update failed
                    LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId());
                Log.i("Downloader", "Error loading file");
                return false;
            }
        }

        final boolean connectSuccess = LocalUtils.connectReceiver(
                reachDatabase,
                networkManager,
                connection.getUniqueIdReceiver(),
                connection.getUniqueIdSender());

        if (!connectSuccess) { //reset and fail

            final Optional<Runnable> optional = LocalUtils.reset(reachDatabase, handlerInterface);
            if (optional.isPresent())
                threadPool.submit(optional.get());
            else {
                LocalUtils.removeKeyWithId(networkManager.keys(), reachDatabase.getId());
                LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId());
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
            LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId());
            LocalUtils.closeSocket(reachDatabase.getReference());
        }
        return updateSuccess;
        //establish relay
//        try {
//        } finally {
//            //attempt a LAN connection (even if relay failed)
//            StaticData.threadPool.submit(new LanRequester(connection));
//        }
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
        Log.i("Downloader", "Upload op inserted " + reachDatabase.getId());

        final Optional<String[]> setUpFile = LocalUtils.prepareUploadFile(
                handlerInterface,
                connection.getSongId(),
                connection.getSenderId());

        if (!setUpFile.isPresent()) {
            connection.setMessageType("404");
            MiscUtils.sendGCM("CONNECT" + new Gson().toJson(connection, Connection.class),
                    connection.getReceiverId(), connection.getSenderId());
            Log.i("Downloader", "File Channel could not be created");
            LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId());
            return false;
        }
        //////////////////////////////////
//        Log.i("Downloader", "Setting senderIp " + lanAddress.toString());
//        connection.setSenderIp(lanAddress.toString()); //TODO verify
        final Cursor receiverName = handlerInterface.getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + connection.getReceiverId()),
                new String[]{
                        ReachFriendsHelper.COLUMN_USER_NAME,
                        ReachFriendsHelper.COLUMN_STATUS},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{connection.getReceiverId() + ""}, null);

        //TODO handle the situation when friend  not found
        if (receiverName == null) {
            LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId());
            return false;
        }
        if (!receiverName.moveToFirst()) {
            receiverName.close();
            LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId());
            return false;
        }

        reachDatabase.setSenderName(receiverName.getString(0));
        reachDatabase.setOnlineStatus(receiverName.getShort(1) + "");
        receiverName.close();

        final ContentValues values = new ContentValues();
        reachDatabase.setDisplayName(setUpFile.get()[0]);
        reachDatabase.setActualName(setUpFile.get()[1]);
        values.put(ReachDatabaseHelper.COLUMN_DISPLAY_NAME, setUpFile.get()[0]);
        values.put(ReachDatabaseHelper.COLUMN_ACTUAL_NAME, setUpFile.get()[1]);
        values.put(ReachDatabaseHelper.COLUMN_SENDER_NAME, reachDatabase.getSenderName());
        values.put(ReachDatabaseHelper.COLUMN_ONLINE_STATUS, reachDatabase.getOnlineStatus());

        final boolean tryRelay, tryDB, tryGCM;

        //establish relay
        tryRelay = LocalUtils.connectSender(
                reachDatabase,
                networkManager,
                connection.getUniqueIdReceiver(),
                connection.getUniqueIdSender());
        if (!tryRelay) { //remove and fail
            LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId());
            return false;
        }

        //update the database
        tryDB = LocalUtils.updateReachDatabase(
                values,
                handlerInterface,
                reachDatabase.getId());
        if (!tryDB) {

            LocalUtils.removeKeyWithId(networkManager.keys(), reachDatabase.getId());
            LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId());
            LocalUtils.closeSocket(reachDatabase.getReference());
            return false;
        }

        //send RELAY gcm
        connection.setMessageType("RELAY");
        final MyBoolean myBoolean = MiscUtils.sendGCM("CONNECT" + new Gson().toJson(connection, Connection.class),
                connection.getReceiverId(), connection.getSenderId());
        tryGCM = myBoolean != null && myBoolean.getOtherGCMExpired();
        if (!tryGCM) {
            Log.i("Downloader", "GCM FAILED NOT SENDING " + connection.toString());
            LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId());
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
         * Check if socket is valid
         */
        final SocketChannel socketChannel;
        if (!selectionKey.isValid() ||
                (socketChannel = (SocketChannel) selectionKey.channel()) == null ||
                !socketChannel.isOpen() ||
                !socketChannel.isConnected()) {

            Log.i("Downloader", "InvalidKey detected");
            LocalUtils.keyCleanUp(selectionKey);
            if (database.getOperationKind() == 0) //reset
                LocalUtils.reset(database, handlerInterface);
            else //for upload just remove
                LocalUtils.removeReachDatabase(handlerInterface, database.getId());
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
                LocalUtils.removeReachDatabase(handlerInterface, database.getId());
                return; //illegal operation
            }
        }

        /**
         * Check for failure
         */
        if (database.getProcessed() == DOWNLOAD_FAIL) { //Has to be a failed download

            LocalUtils.keyCleanUp(selectionKey);
            LocalUtils.closeSocket(database.getReference());
            handlerInterface.downloadFail(database.getDisplayName());
            final Optional<Runnable> optional = LocalUtils.reset(database, handlerInterface);
            if (optional.isPresent())
                threadPool.submit(optional.get());
            else
                LocalUtils.removeReachDatabase(handlerInterface, database.getId());
            Log.i("Downloader", "DOWNLOAD ERROR " + database.getDisplayName());
            return;
        }
        if (database.getProcessed() == UPLOAD_FAIL) {
            //Has to be an upload
            LocalUtils.keyCleanUp(selectionKey);
            LocalUtils.closeSocket(database.getReference());
            LocalUtils.removeReachDatabase(handlerInterface, database.getId());
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

                Log.i("Downloader", "Download ended " + database.getProcessed() + " " + database.getLength());
                //Sync upload success to server
                database.setStatus(ReachDatabase.FINISHED); //download finished
                LocalUtils.closeSocket(database.getReference());
                LocalUtils.closeSocket(fileChannelIndex);

                MiscUtils.autoRetryAsync(new DoWork<Void>() {
                    @Override
                    protected Void doWork() throws IOException {

                        return StaticData.userEndpoint.updateCompletedOperations(
                                database.getReceiverId(),
                                database.getSenderId(),
                                database.getDisplayName(),
                                database.getLength()).execute();
                    }
                }, Optional.<Predicate<Void>>absent());

                return; //download complete
            }

            /**TODO test :
             * In-case of upload we do not close the socket right away.
             * We shut down the input/output and leave it hanging.
             * We do not close to allow system buffer to clear up
             */
            case SelectionKey.OP_WRITE: {

                Log.i("Downloader", "Upload ended " + database.getProcessed() + " " + database.getLength());
                LocalUtils.removeReachDatabase(handlerInterface, database.getId());
                selectionKey.cancel();
            }
            //default case already handled
        }
    }

    /**
     * Kills the channels/keys that are sleeping for more than 60 secs
     *
     * @param keys the set of keys to check
     */
    private void runOperation(Iterable<SelectionKey> keys,
                              NetworkHandlerInterface handlerInterface) {

        final long currentTime = System.currentTimeMillis();
        if (currentTime - lastOperation < 4000L)
            return;

        final Cursor cursor = handlerInterface.getContentResolver().query(
                ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.projection,
                null, null, null);
        if (cursor == null)
            return;

        final LongSparseArray<ReachDatabase> tempDiskHolder = new LongSparseArray<>();
        final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        boolean needToUpdate = false;
        lastOperation = currentTime;

        while (cursor.moveToNext()) {
            final ReachDatabase diskReach = ReachDatabaseHelper.cursorToProcess(cursor);
            tempDiskHolder.append(diskReach.getId(), diskReach); //append for later use
        }

        //first of all kill/reset dead transactions
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
                    MiscUtils.closeAndIgnore(openChannels.get(LocalUtils.getFileChannelIndex(memoryReach)));
                needToUpdate = true;
                continue; //nothing more to do
            } else if (diskReach.getProcessed() != memoryReach.getProcessed()) {

                //update progress
                final ContentValues contentValues = new ContentValues();
                contentValues.put(ReachDatabaseHelper.COLUMN_PROCESSED, memoryReach.getProcessed());
                operations.add(LocalUtils.getUpdateOperation(contentValues, memoryReach.getId()));
            } else if (memoryReach.getStatus() == ReachDatabase.FINISHED) {

                //mark finished
                final ContentValues contentValues = new ContentValues();
                contentValues.put(ReachDatabaseHelper.COLUMN_PROCESSED, memoryReach.getLength());
                contentValues.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.FINISHED);
                operations.add(LocalUtils.getUpdateOperation(contentValues, memoryReach.getId()));
            }

            /**
             * Then perform keyKiller operations
             */
            if (currentTime - memoryReach.getLastActive() <= 60 * 1001)
                continue;//has not timed out yet

            Log.i("Downloader", "Running keyKiller");
            try {

                if (memoryReach.getOperationKind() == 0) {
                    final ReachDatabase reachDatabase = LocalUtils.getReachDatabase(handlerInterface, memoryReach.getId());
                    if (reachDatabase == null)
                        continue;

                    final Optional<Runnable> optional = LocalUtils.reset(reachDatabase, handlerInterface);
                    if (optional.isPresent())
                        threadPool.submit(optional.get());
                    else {
                        LocalUtils.removeKeyWithId(keys, reachDatabase.getId());
                        LocalUtils.removeReachDatabase(handlerInterface, reachDatabase.getId());
                        LocalUtils.closeSocket(reachDatabase.getReference());
                    }
                    handlerInterface.downloadFail(reachDatabase.getDisplayName());
                } else
                    LocalUtils.removeReachDatabase(handlerInterface, memoryReach.getId());
            } finally {
                LocalUtils.keyCleanUp(selectionKey);
                LocalUtils.closeSocket(memoryReach.getReference());
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
        private static final byte[] filler = new byte[16384];

        /**
         * Clean up the directory, if file not found, marks as FILE_NOT_CREATED
         */
        public static void sanitizeReachDirectory(NetworkHandlerInterface handlerInterface, File reachDirectory) {

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
            temp.clear();
        }

        /**
         * Connects to the relay and registers into selector
         *
         * @return weather successful or not
         */
        public static boolean connectReceiver(ReachDatabase reachDatabase,
                                              Selector networkManager,
                                              long uniqueReceiverId,
                                              long uniqueSenderId) {

            //create a random reference for socket
            final long reference = UUID.randomUUID().getMostSignificantBits();
            reachDatabase.setReference(reference);

            try {
                //Update fileChannel
                final SocketChannel socketChannel;
                openChannels.append(reference, socketChannel = SocketChannel.open());
                socketChannel.configureBlocking(true);
                socketChannel.socket().setSoLinger(true, 1);
                socketChannel.socket().setKeepAlive(true);
                socketChannel.socket().connect(vmAddress, 5000);
                socketChannel.write(ByteBuffer.wrap((String.valueOf(
                        uniqueReceiverId) + "\n" +
                        uniqueSenderId + "\n" +
                        "Receiver" + "\n").getBytes()));
                socketChannel.configureBlocking(false).register(
                        networkManager,
                        SelectionKey.OP_READ,
                        reachDatabase);
//                final PrintWriter printWriter = new PrintWriter(socketChannel.socket().getOutputStream());
//                printWriter.println(uniqueReceiverId);
//                printWriter.println(uniqueSenderId);
//                printWriter.println("Receiver");
//                printWriter.flush(); //Blocking mode exception, cant put after set to non-blocking

            } catch (IOException | AssertionError e) {

                //removal of selectionKey will be done in keyKiller
                e.printStackTrace();
                LocalUtils.toast("Firewall issue");
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
                                            long uniqueSenderId) {

            //create a random reference for socket
            final long reference = UUID.randomUUID().getMostSignificantBits();
            reachDatabase.setReference(reference);

            try {
                //Update fileChannel
                final SocketChannel socketChannel;
                openChannels.append(reference, socketChannel = SocketChannel.open());
                socketChannel.configureBlocking(true);
                socketChannel.socket().setSoLinger(true, 1);
                socketChannel.socket().setKeepAlive(true);
                socketChannel.socket().connect(vmAddress, 5000);
                socketChannel.write(ByteBuffer.wrap((String.valueOf(
                        uniqueSenderId) + "\n" +
                        uniqueReceiverId + "\n" +
                        "Sender" + "\n").getBytes()));
                socketChannel.configureBlocking(false).register(networkManager, SelectionKey.OP_READ, reachDatabase);

//                final PrintWriter printWriter = new PrintWriter(socketChannel.socket().getOutputStream());
//                printWriter.println(uniqueSenderId);
//                printWriter.println(uniqueReceiverId);
//                printWriter.println("Sender");
//                printWriter.flush(); //Blocking mode exception, cant put after set to non-blocking

            } catch (IOException | AssertionError e) {

                //TODO make a central GCM and relay connectivity check
                //removal of selectionKey will be done in keyKiller
                e.printStackTrace();
                LocalUtils.toast("Firewall issue");
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
         * @param songId           the id of the song reuested
         * @param myId             my own id
         * @return display name and actual name of the song
         */
        public static Optional<String[]> prepareUploadFile(NetworkHandlerInterface handlerInterface,
                                                           long songId,
                                                           long myId) {

            final Cursor cursor = handlerInterface.getContentResolver().query(
                    ReachSongProvider.CONTENT_URI,
                    new String[]{
                            ReachSongHelper.COLUMN_ID,
                            ReachSongHelper.COLUMN_DISPLAY_NAME,
                            ReachSongHelper.COLUMN_ACTUAL_NAME,
                            ReachSongHelper.COLUMN_PATH,
                            ReachSongHelper.COLUMN_VISIBILITY},
                    ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                            ReachSongHelper.COLUMN_SONG_ID + " = ?",
                    new String[]{myId + "", songId + ""}, null);
            //handle visibility as well
            if (cursor == null || !cursor.moveToFirst() || cursor.getShort(4) == 0) {
                Log.i("Downloader", "Requested song path invalid");
                return Optional.absent();
            }
            final String[] names = new String[]{cursor.getString(1),  //display name
                    cursor.getString(2)}; //actual name
            final String filePath = cursor.getString(3);
            cursor.close();
            if (TextUtils.isEmpty(filePath))
                return Optional.absent();
            //song found, continuing
            final long fileChannelIndex = getFileChannelIndex(names[1], names[0]);
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
                Log.i("Downloader", "Index for " + names[0] + " = " + fileChannelIndex);
            }
            //else channel already loaded

            return Optional.of(names);
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
                    randomAccessFile.setLength(reachDatabase.getLength());
                    randomAccessFile.seek(0); //should rewind
                    //fill the file with 0's
                    int written = 0;
                    while (written < length) {
                        randomAccessFile.write(filler);
                        Log.i("Downloader", "File pointer " + randomAccessFile.getFilePointer());
                        written += 4096; //buffer written
                    }

                    randomAccessFile.seek(0); //should rewind
                    Log.i("Ayush", "Filled " + (length - randomAccessFile.length()) + " | " + randomAccessFile.getFilePointer());

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
                        MiscUtils.generateRequest(reachDatabase),
                        reachDatabase.getReceiverId(), //myID
                        reachDatabase.getSenderId(),   //the uploaded
                        reachDatabase.getId()));

            return Optional.absent(); //update failed !
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
            MiscUtils.closeAndIgnore(selectionKey.channel());
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
         */
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
         * Removes the reachDatabase entry from disk and memory
         *
         * @param id of reachDatabase to remove
         */
        public static void removeReachDatabase(NetworkHandlerInterface handlerInterface,
                                               long id) {

            handlerInterface.getContentResolver().delete(
                    Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id),
                    ReachDatabaseHelper.COLUMN_ID + " = ?",
                    new String[]{id + ""});
            handlerInterface.updateNetworkDetails();
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
         * @param databaseId the databaseId
         * @return the reachDatabase object corresponding to parameters
         */
        public static ReachDatabase getReachDatabase(NetworkHandlerInterface handlerInterface,
                                                     long databaseId) {

            final Cursor cursor = handlerInterface.getContentResolver().query(
                    ReachDatabaseProvider.CONTENT_URI,
                    ReachDatabaseHelper.projection,
                    ReachDatabaseHelper.COLUMN_ID + " = ?",
                    new String[]{databaseId + ""},
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
         * Right now only logs
         *
         * @param message that needs to be shown as toast
         */
        public static void toast(final String message) {

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

        /**
         * Get the fileChannel index for given reachDatabase
         *
         * @param reachDatabase to pick names from
         * @return fileChannel index
         */
        public static long getFileChannelIndex(ReachDatabase reachDatabase) {
            return getFileChannelIndex(reachDatabase.getActualName(), reachDatabase.getDisplayName());
        }

        /**
         * Get the fileChannel index for given name
         *
         * @param actualName  of the song
         * @param displayName of the song
         * @return fileChannel index
         */
        public static long getFileChannelIndex(String actualName, String displayName) {
            return Arrays.hashCode(new Object[]{
                    TextUtils.isEmpty(actualName) ? "" : actualName,
                    TextUtils.isEmpty(displayName) ? "" : displayName});
        }

        /**
         * Close and remove socket
         *
         * @param reference of the socket
         */
        public static void closeSocket(long reference) {
            MiscUtils.closeAndIgnore(openChannels.get(reference, null));
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

        SharedPreferences getSharedPreferences();

        void updateNetworkDetails();

//        Future<?> submitChild(ReachTask runnable);

        void removeNetwork();

        void downloadFail(String songName);
    }
}