import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public abstract class Connection {

    public static final String CRLF = "\r\n";

    protected final String host;

    protected final int port;

    protected BufferedWriter output;

    protected BufferedReader input;

    public Connection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void sendMessage(CommandType command, String arguments) throws IOException{
        String message = command.getArgument();
        if (arguments != null) {
            if (command != CommandType.NO_COMMAND) {
                message += " ";
            }
            message += arguments;
        }
        message += CRLF;
        output.write(message);
        output.flush();
        System.out.println("CL: " + message);
    }

    public String readMessage() throws IOException {
        StringBuilder message = new StringBuilder();
        boolean continueReading = true;
        while (continueReading) {
            String line = input.readLine();
            if (line.charAt(3) != '-') {
                continueReading = false;
            }
            message.append(line);
        }
        return message.toString();
    }

    protected String getResponseCode(String Message) {
        return Message.substring(0, 3);
    }

}
