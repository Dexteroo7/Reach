import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

public final class Main {

    private static final ConcurrentHashMap<String, SocketChannel> currentChannels =
            new ConcurrentHashMap<>(5000, 0.75f, 100);
    private static final ExecutorService threadPool = Executors.newCachedThreadPool(new CustomThreadFactoryBuilder()
            .setPriority(Thread.MAX_PRIORITY)
            .setNamePrefix("relay_thread")
            .setDaemon(false).build());
    private static ServerSocketChannel controller;

    /**
     * Closes closeables and ignores exception
     *
     * @param closeables that need to be closed
     */
    private static void closeAndIgnore(Closeable... closeables) {

        for (Closeable closeable : closeables)
            if (closeable != null)
                try {
                    closeable.close();
                } catch (IOException ignored) {
                }
    }

    /**
     * Cleans up any ongoing transfers by closing channels
     * Also gets rid of controller.
     * Called only when controller fucked up
     */
    private static void sanitize() {

        for (Closeable channel : currentChannels.values())
            closeAndIgnore(channel);

        currentChannels.clear();
        closeAndIgnore(controller);
        controller = null;
        System.gc();
    }

    public static void main(String[] args) {

        while (true) {

            //Check if controller is healthy
            if (controller == null || !controller.isOpen()) {

                System.out.println("Controller lost " + System.currentTimeMillis());
                System.out.println("*********----------********");

                sanitize();
                //relaxationPeriod
                try {
                    //Wait till gc finishes completely
                    Thread.sleep(10 * 1000L);
                } catch (InterruptedException e) {

                    //SUPER BAD !!
                    System.out.println("Complete failure" + System.currentTimeMillis());
                    System.out.println("*********----------********");
                    threadPool.shutdownNow();
                    System.exit(0);
                    return;
                }

                try {
                    controller = ServerSocketChannel.open();
                    controller.configureBlocking(true);
                    controller.bind(new InetSocketAddress(60001), 500);
                    controller.socket().setReuseAddress(true);
                } catch (IOException e) {

                    //BAD !
                    System.out.println("Partial failure " + System.currentTimeMillis());
                    System.out.println("*********----------********");
                    closeAndIgnore(controller);
                    controller = null;
                    continue;
                }
            }

            try {
                threadPool.submit(new ConnectionHandler(controller.accept()));
            } catch (IOException e) {

                System.out.println("Could not accept " + System.currentTimeMillis());
                System.out.println("*********----------********");
                closeAndIgnore(controller);
                controller = null; //RESET
            }
        }
    }

    /**
     * Performs the actual relay
     */
    private static final class ConnectionHandler implements Runnable {

        private final SocketChannel socket;
        private final Socket temp;

        private ConnectionHandler(SocketChannel socket) {
            this.socket = socket;
            this.temp = socket.socket();
        }

        /**
         * @param wantsToConnectTo the id of the other party we are interested in
         * @return true : other party arrived/already there
         * false : timed out, kill this thread
         */
        private boolean poll(String wantsToConnectTo) {

            //we poll check every 2 seconds, for 60 seconds
            final int fixedWait = 2 * 1000;
            final int maximumWait = 60 * 1000;

            int waitTime = 0;
            while (!currentChannels.containsKey(wantsToConnectTo)) {

                try {
                    Thread.sleep(fixedWait);
                    waitTime += fixedWait;
                } catch (InterruptedException ignored) {
                    closeAndIgnore(socket, temp);
                    return false;
                }

                if (waitTime >= maximumWait) {
                    closeAndIgnore(socket, temp);
                    return false;
                }
            }

            return true; //found !
        }

        /**
         * Copies all bytes from the readable channel to the writable channel.
         * Does not close or flush either channel.
         *  @param from the readable channel to read from
         * @param to   the writable channel to write to
         * @param buf intermediate buffer
         */
        private void copy(ReadableByteChannel from,
                          WritableByteChannel to,
                          ByteBuffer buf) {

            System.out.println("Connected");

            boolean fail = false;

            while (!fail) {

                try {
                    fail = (from.read(buf) == -1);
                } catch (IOException ignored) {
                    fail = true;
                }

                buf.flip();
                while (buf.hasRemaining())
                    try {
                        to.write(buf);
                    } catch (IOException ignored) {
                        fail = true;
                    }
                buf.clear();
            }

            //sleep before closing !
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException ignored) {
                //ignored as we anyway kill this thread soon
                return;
            }

            closeAndIgnore(from, to);
        }

        @Override
        public final void run() {

            final String id, wantsToConnectTo, socketType;

            try {

                socket.configureBlocking(true);
                temp.setSoLinger(true, 1);
                temp.setKeepAlive(true);
                temp.setTrafficClass(0x10);
                temp.setPerformancePreferences(0, 1, 0);

                final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(temp.getInputStream()));
                id = bufferedReader.readLine();
                wantsToConnectTo = bufferedReader.readLine();
                socketType = bufferedReader.readLine();

            } catch (IOException ignored) {
                closeAndIgnore(socket, temp);
                return;
            }

            System.out.println(socketType + " | self " + id + " | other " + wantsToConnectTo);
            currentChannels.put(id, socket);

            final ByteBuffer buf;
            if (socketType.equals("Receiver")) //create buffer before busy waiting
                buf = ByteBuffer.allocateDirect(4096);
            else
                buf = null;

            //busy wait till partner arrives
            if (poll(wantsToConnectTo)) {

                //noinspection StatementWithEmptyBody
                if (socketType.equals("Sender")) {

//                    try {
//                        Thread.sleep(2000L);
//                    } catch (InterruptedException ignored) {
//                        return;
//                    }
                    /**
                     * the socket needs to be kept alive till
                     * the receiver comes and takes it
                     */
                } else {

                    //get hold of the receiver
                    final ReadableByteChannel sender = currentChannels.get(wantsToConnectTo);
                    if (sender == null || !sender.isOpen()) {

                        //sanity check
                        closeAndIgnore(currentChannels.remove(id),
                                currentChannels.remove(wantsToConnectTo),
                                socket,
                                temp,
                                sender);
                        System.out.println("Found sender, but closed");
                        System.out.println("*********----------********");
                    } else {

                        //first copy
                        copy(sender, socket, buf); //Use this thread only to perform copy
                        //then evict
                        closeAndIgnore(currentChannels.remove(id),
                                currentChannels.remove(wantsToConnectTo),
                                socket,
                                temp,
                                sender);
                    }
                }

            } else {
                //partner did not arrive, close
                closeAndIgnore(currentChannels.remove(id),
                        currentChannels.remove(wantsToConnectTo),
                        socket,
                        temp);
            }

            final Cleaner cleaner = buf != null ? ((DirectBuffer) buf).cleaner() : null;
            if (cleaner != null)
                cleaner.clean();
        }
    }

    /**
     * Thread factory to create threads of high priority
     */
    public static final class CustomThreadFactoryBuilder {

        private String namePrefix = null;
        private boolean daemon = false;
        private int priority = Thread.NORM_PRIORITY;

        private static ThreadFactory build(CustomThreadFactoryBuilder builder) {

            final String namePrefix = builder.namePrefix;
            final Boolean daemon = builder.daemon;
            final Integer priority = builder.priority;
            final AtomicLong count = new AtomicLong(0);

            //noinspection NullableProblems
            return new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable);
                    if (namePrefix != null) {
                        thread.setName(namePrefix + "-" + count.getAndIncrement());
                    }
                    thread.setDaemon(daemon);
                    thread.setPriority(priority);
                    return thread;
                }
            };
        }

        public CustomThreadFactoryBuilder setNamePrefix(String namePrefix) {
            if (namePrefix == null) {
                throw new NullPointerException();
            }
            this.namePrefix = namePrefix;
            return this;
        }

        public CustomThreadFactoryBuilder setDaemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public CustomThreadFactoryBuilder setPriority(int priority) {

            if (priority > Thread.MAX_PRIORITY) {
                throw new IllegalArgumentException(String.format(
                        "Thread priority (%s) must be <= %s", priority,
                        Thread.MAX_PRIORITY));
            }

            this.priority = priority;
            return this;
        }

        public ThreadFactory build() {
            return build(this);
        }
    }
}
