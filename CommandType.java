public enum CommandType {

    EHLO("EHLO"),
    HELO("HELO"),
    STARTTLS("STARTTLS"),
    AUTH_LOGIN("AUTH LOGIN"),
    MAIL_FROM("MAIL FROM:"),
    RCPT_TO("RCPT TO:"),
    DATA("DATA"),
    QUIT("QUIT"),
    NO_COMMAND(""),
    MAIL_END("."),
    RSET("RSET");


    private final String command;

    CommandType(String command) {
        this.command = command;
    }

    public String getArgument() {
        return command;
    }

}
