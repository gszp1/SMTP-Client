import Exceptions.authenticationFailedException;
import Exceptions.emailAddressException;
import Exceptions.serverResponseException;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.Base64;

public class SecureConnection extends Connection{

    private final SSLSocket socket;

    public SecureConnection(SSLSocket socket, String host, int port) throws IOException {
        super(host, port);
        this.socket = socket;
        output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void establishConnection() throws IOException {
        socket.startHandshake();
        sendMessage(CommandType.EHLO, socket.getInetAddress().getHostName());
        System.out.println("S: " + readMessage());
    }

    public void authentifyUser(String email, String password) throws IOException,
            serverResponseException, authenticationFailedException {
        String message;
        sendMessage(CommandType.AUTH_LOGIN, null);
        System.out.println("S: " + (message = readMessage()));
        loginResponseHandler(getResponseCode(message));
        sendMessage(CommandType.NO_COMMAND, Base64.getEncoder().encodeToString(email.getBytes()));
        System.out.println("S: " + (message = readMessage()));
        loginResponseHandler(getResponseCode(message));
        sendMessage(CommandType.NO_COMMAND, Base64.getEncoder().encodeToString(password.getBytes()));
        System.out.println("S: " + (message = readMessage()));
        loginResponseHandler(getResponseCode(message));
    }

    public void sendEmail(String sender, String recipient, String subject, String content) throws IOException,
            emailAddressException, serverResponseException{
        String message;
        sendMessage(CommandType.MAIL_FROM, "<"+sender+">");
        System.out.println("S: " + (message = readMessage()));
        sendingResponseHandler(getResponseCode(message));
        sendMessage(CommandType.RCPT_TO, "<"+recipient+">");
        System.out.println("S: " + (message = readMessage()));
        sendingResponseHandler(getResponseCode(message));
        sendMessage(CommandType.DATA, null);
        System.out.println("S: " + (message = readMessage()));
        String responseCode = getResponseCode(message);
        if (responseCode.equals("550")) {
            throw new serverResponseException(responseCode);
        }
        sendMessage(CommandType.NO_COMMAND, "Subject: " + subject);
        sendMessage(CommandType.NO_COMMAND, content);
        sendMessage(CommandType.MAIL_END, null);
        System.out.println("S: " + (message = readMessage()));
        sendingResponseHandler(getResponseCode(message));
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

    private void loginResponseHandler(String responseCode) throws authenticationFailedException, serverResponseException {
        if (responseCode.equals("535")) {
            throw new authenticationFailedException();
        }
        if (responseCode.equals("530") || responseCode.equals("538")) {
            throw new serverResponseException(responseCode);
        }
    }

    private void sendingResponseHandler(String responseCode) throws emailAddressException, serverResponseException,
            IOException{
        if (responseCode.equals("550") || responseCode.equals("553") || responseCode.equals("501")) {
            sendMessage(CommandType.RSET, null);
            System.out.println("S: " + readMessage());
            throw new emailAddressException();
        }
        if (!(responseCode.equals("250") || responseCode.equals("251") || responseCode.equals("343"))) {
            sendMessage(CommandType.RSET, null);
            System.out.println("S: " + readMessage());
            throw new serverResponseException(responseCode);
        }
    }

}
