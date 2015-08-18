//package reach.project.reachProcess.reachService;
//
//import com.google.gson.Gson;
//import com.google.gson.JsonSyntaxException;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.net.SocketAddress;
//import java.nio.channels.ServerSocketChannel;
//import java.nio.channels.SocketChannel;
//
//import reach.project.core.StaticData;
//import reach.project.reachProcess.auxiliaryClasses.Connection;
//import reach.project.reachProcess.auxiliaryClasses.ReachTask;
//import reach.project.utils.MiscUtils;
///**
// * Created by Dexter on 16-05-2015.
// */
//public class LanHandler extends ReachTask<LanHandler.LanHandlerInterface> {
//
//    private ServerSocketChannel lanSupplier;
//    public LanHandler(LanHandlerInterface handlerInterface) {
//        super(handlerInterface);
//    }
//
//    public SocketAddress getLocalSocketAddress() {
//        return lanSupplier.socket().getLocalSocketAddress();
//    }
//
//    @Override
//    protected void sanitize() {
//
//        kill.set(true);
//        if(lanSupplier != null)
//            try {
//                lanSupplier.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//    }
//
//    @Override
//    protected void performTask() {
//
//        try {
//            lanSupplier = ServerSocketChannel.open();
//            lanSupplier.socket().bind(new InetSocketAddress(MiscUtils.getLocalIp().getHostAddress(), 0));
//            lanSupplier.socket().setReuseAddress(true);
//            lanSupplier.configureBlocking(true);
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//            if(lanSupplier != null)
//                try {
//                    lanSupplier.close();
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
//            return;
//        }
//        while (!kill.get() && lanSupplier.isOpen()) {
//            try {
//                AsyncTask.THREAD_POOL_EXECUTOR.execute(new HandleLAN(lanSupplier.accept()));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    /**
//     * Being part of AsyncTask.THREAD_POOL_EXECUTOR, this Runnable gets accounted for
//     * in out controller kill check automatically
//     */
//    private final class HandleLAN implements Runnable {
//
//        private final SocketChannel channel;
//        private final byte [] fail = "fail\n".getBytes();
//        private final byte [] success = "success\n".getBytes();
//
//        private HandleLAN(SocketChannel channel) {
//            this.channel = channel;
//        }
//
//        private void fail(Socket socket, OutputStream stream) {
//            try {
//                if(stream != null) {
//                    stream.write(fail);
//                    Thread.sleep(500L);
//                    stream.close();
//                }
//                socket.close();
//            } catch (IOException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        public void run() {
//
//            final Socket lanRequest = channel.socket();
//            final BufferedReader reader;
//            final OutputStream stream;
//            final String line;
//            try {
//                reader = new BufferedReader(new InputStreamReader(lanRequest.getInputStream()));
//                stream = lanRequest.getOutputStream();
//                line = reader.readLine();
//            } catch (IOException e) {
//                e.printStackTrace();
//                fail(lanRequest, null);
//                return;
//            }
//            final Connection connection;
//            try {
//                connection = new Gson().fromJson(line, Connection.class);
//            } catch (IllegalStateException | JsonSyntaxException e) {
//                e.printStackTrace();
//                fail(lanRequest, stream);
//                return;
//            }
//
//            if (connection.getSenderId() != handlerInterface.getMyId())
//                fail(lanRequest, stream);
//
//            try {
//                stream.write(success);
//            } catch (IOException e) {
//                e.printStackTrace();
//                fail(lanRequest, stream);
//                return;
//            }
//            handlerInterface.submitLanRequest(channel, connection);
//        }
//    }
//
//    public interface LanHandlerInterface {
//        boolean submitLanRequest(SocketChannel channel, Connection connection);
//        long getMyId();
//    }
//}
