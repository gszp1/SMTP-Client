import Exceptions.authenticationFailedException;
import Exceptions.emailAddressException;
import Exceptions.serverResponseException;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SMTPClient extends Application {

    private final int PORT = 587;

    public static final String HOST_GMAIL = "smtp.gmail.com";

    public static final String HOST_OUTLOOK = "smtp-mail.outlook.com";

    private UnsecureConnection unsecureConnection;

    private SecureConnection secureConnection;

    private String userEmail;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SMTP Client Login");

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(10);

        Label emailLabel = new Label("E-mail:");
        TextField emailField = new TextField();
        gridPane.add(emailLabel, 0, 0);
        gridPane.add(emailField, 1, 0);

        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        gridPane.add(passwordLabel, 0, 1);
        gridPane.add(passwordField, 1, 1);

        Label notificationLabel = new Label("");
        Button button = getButton(emailField, passwordField, notificationLabel);
        gridPane.add(button, 0, 2);
        gridPane.add(notificationLabel, 0, 3, 2, 1);

        HBox hBox = new HBox(gridPane);
        hBox.setAlignment(javafx.geometry.Pos.CENTER);

        VBox vBox = new VBox();
        vBox.getChildren().add(hBox);
        vBox.setAlignment(javafx.geometry.Pos.CENTER);

        // Create the scene
        Scene scene = new Scene(vBox, 300, 200);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Button getButton(TextField emailField, PasswordField passwordField, Label notificationLabel) {
        Button button = new Button("Login");
        button.setOnAction(event -> {
            userEmail = emailField.getText();
            if ((!validateEmail(emailField.getText())) || (passwordField.getText().isEmpty())) {
                setError("Invalid credentials", notificationLabel);
                emailField.clear();
                passwordField.clear();
                return;
            }
            Optional<String> hostOp = getSMTPHost(emailField.getText());
            if (hostOp.isPresent()) {
                SSLSocket securedSocket;
                try {
                    unsecureConnection = new UnsecureConnection(hostOp.get(), PORT);
                    securedSocket = unsecureConnection.establishConnection();
                } catch (IOException | NoSuchAlgorithmException | serverResponseException e) {
                    setError("Failed to connect with email server.", notificationLabel);
                    return;
                }
                try {
                    secureConnection = new SecureConnection(securedSocket, hostOp.get(), PORT);
                    secureConnection.establishConnection();
                    secureConnection.authentifyUser(emailField.getText(), passwordField.getText());
                    notificationLabel.setText("Login successful!");
                    notificationLabel.setStyle("-fx-text-fill: green;");
                    openEmailWindow((Stage) button.getScene().getWindow());
                } catch (authenticationFailedException e){
                    setError("Invalid credentials!", notificationLabel);
                    emailField.clear();
                    passwordField.clear();
                } catch (serverResponseException | IOException e) {
                    setError("Failed to login", notificationLabel);
                    emailField.clear();
                    passwordField.clear();
                }
            } else {
                setError("Unrecognized email domain!", notificationLabel);
                emailField.clear();
                passwordField.clear();
            }
        });
        return button;
    }

    private final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    private boolean validateEmail(String userEmail) {
        Matcher matcher = pattern.matcher(userEmail);
        return matcher.matches();
    }

    private Optional<String> getSMTPHost(String eMail) {
        String [] eMailParts = eMail.split("@");
        if (eMailParts.length != 2) {
            return Optional.empty();
        }
        switch (eMailParts[1]) {
            case "gmail.com":
                return Optional.of(HOST_GMAIL);
            case "outlook.com":
                return Optional.of(HOST_OUTLOOK);
        }
        return Optional.empty();
    }

    private void setError(String errorName, Label notificationLabel) {
        notificationLabel.setText(errorName);
        notificationLabel.setStyle("-fx-text-fill: red;");
    }

    private void openEmailWindow(Stage loginStage) {
        loginStage.close();

        Stage newStage = new Stage();
        newStage.setOnCloseRequest((WindowEvent we) -> {
            try {
                secureConnection.sendMessage(CommandType.QUIT, null);
                unsecureConnection.closeConnection();
                secureConnection.closeConnection();
            } catch (IOException e){
                System.out.println("Error occurred during server closing.");
            }
        });
        newStage.setTitle("Email client");
        GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(10);
        gridPane.setAlignment(Pos.CENTER);

        Label toLabel = new Label("To:");
        TextField recipientField = new TextField();
        gridPane.add(toLabel, 0, 0);
        gridPane.add(recipientField, 1, 0);

        Label subjectLabel = new Label("Subject:");
        TextField subjectField = new TextField();
        gridPane.add(subjectLabel, 0, 1);
        gridPane.add(subjectField, 1, 1);

        Label contentLabel = new Label("Content:");
        TextArea contentArea = new TextArea();
        contentArea.setPrefColumnCount(50);
        contentArea.setPrefHeight(400);
        gridPane.add(contentLabel, 0, 2);
        gridPane.add(contentArea, 1, 2);

        Button button = new Button("Send");
        button.setMinSize(150, 50);
        gridPane.add(button, 1, 3, 2, 1);

        Label notificationLabel = new Label("");
        notificationLabel.setFont(new Font(14));
        gridPane.add(notificationLabel, 0, 4);

        button.setOnAction(event -> {
            try {
                secureConnection.sendEmail(userEmail,
                        recipientField.getText(),
                        subjectField.getText(),
                        contentArea.getText()
                );
                notificationLabel.setText("Email sent successfully.");
                notificationLabel.setStyle("-fx-text-fill: green;");
            } catch (IOException | serverResponseException e) {
                setError("Failed to send email.", notificationLabel);
            } catch (emailAddressException e) {
                setError("Provided clients were not found.", notificationLabel);
            }
        });

        // Create a scene and set it on the new stage
        Scene newScene = new Scene(gridPane, 960, 540);
        newStage.setScene(newScene);


        // Show the new stage
        newStage.show();
    }
}