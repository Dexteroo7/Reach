package com.relay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by dexter on 15/07/15.
 */
public class Main2 {

    public static void main(String[] args) throws IOException {


        final ServerSocket serverSocket = new ServerSocket(80);
        System.out.println("Waiting");
        final Socket socket = serverSocket.accept();
        System.out.println("Reading");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String test;
        while ((test = reader.readLine()) != null)
            System.out.println(test);
    }
}
