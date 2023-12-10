import Exceptions.serverResponseException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class UnsecureConnection extends Connection{

    private final Socket socket;

    public UnsecureConnection(String host, int port) throws IOException {
        super(host, port);
        socket = new Socket(host, port);
        output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public SSLSocket establishConnection() throws IOException, NoSuchAlgorithmException, serverResponseException {
        String message = readMessage();
        System.out.println("S: " + message);
        if (getResponseCode(message).equals("554")) {
            sendMessage(CommandType.QUIT, null);
            throw new serverResponseException("554");
        }
        sendMessage(CommandType.EHLO , socket.getLocalAddress().getHostName());
        System.out.println("S: " + (message = readMessage()));
        if (getResponseCode(message).equals("502")) {
            sendMessage(CommandType.HELO , socket.getLocalAddress().getHostName());
            System.out.println("S: " + (message = readMessage()));
        }
        if (!getResponseCode(message).equals("250")) {
            throw new serverResponseException(getResponseCode(message));
        }
        sendMessage(CommandType.STARTTLS, null);
        System.out.println("S: " + (message = readMessage()));
        if (getResponseCode(message).equals("454")) {
            throw new serverResponseException(getResponseCode(message));
        }
        return (SSLSocket) SSLContext
                .getDefault()
                .getSocketFactory()
                .createSocket(socket, host, port, true);
    }

    public void closeConnection() throws IOException {
        if (output != null) {
            output.close();
        }
        if (input != null) {
            input.close();
        }
        if (socket != null) {
            socket.close();
        }
    }

}
