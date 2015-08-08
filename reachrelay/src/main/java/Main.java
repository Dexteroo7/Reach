import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Created by dexter on 08/08/15.
 */
public class Main {

    public static void main (String [] args) throws IOException{

        final String hostIp = args[0];
        final String hostPort = args[1];

        System.out.println("If you are uploader enter 1, else enter 0");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        final String type = reader.readLine();

        switch (type) {

            case "1" :
                handleSend(reader, hostIp, hostPort);
                break;
            case "2":
                handleReceive();
                break;
        }
    }

    private static void handleSend(BufferedReader reader, String hostIp, String hostPort) throws IOException{

        System.out.println("Enter path");
        final String path = reader.readLine();

        final File file = new File(path);
        if (!file.exists() && !file.isFile()) {

            System.out.print("Invalid file");
            return;
        }

        System.out.println("Transfer " + file.getName() + " size " + (file.length() / 1024) / 1024 + "mb ?");
        reader.readLine();

        final SocketChannel channel = SocketChannel.open(new InetSocketAddress(hostIp, Integer.parseInt(hostPort)));

        final FileChannel fileChannel = new FileOutputStream(file).getChannel()

    }

    private static void handleReceive() {

    }
}
