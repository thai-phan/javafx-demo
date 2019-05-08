package main.java;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.*;
import javafx.util.StringConverter;
import main.java.Controllers.LoginController;
import main.java.Models.MasterFolderModel;
import main.java.Models.ModelObject.ControlBindingObj;
import main.java.Models.ModelObject.Resultinfo;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Properties;


public class Main extends Application {
    public static final int WIDTH = 500;
    public static final int HEIGHT = 500;
    public static final String CAMPAIGN_LIST_FXML = "/fxml/campaignList.fxml";
    public static final String COMMUNICATION_MANAGER_FXML = "/fxml/communicationManager.fxml";
    public static final String LOGIN_FXML = "/fxml/login.fxml";
    public static final String FOLDER_SELECTION = "/fxml/folderSelection.fxml";
    public static final String EXPLAIN_LIST = "/fxml/explainList.fxml";
    public static final String SCHEDULE_DATE = "/fxml/scheduleDate.fxml";
    public static final String LOADING = "/fxml/loading.fxml";
    public static final String PASSWORD = "/fxml/password.fxml";


    public static final int API_CODE_SUCCESS = 0;
    public static final int API_CODE_LOGOUT = 9999;
    public static final String SESSION_EXPIRE_HEADER = "Session Expired";
    public static final String SESSION_EXPIRE_CONTENT = "Please re-login";
    public static final String ERROR_HEADER = "Error";
    public static final String SUCCESS_HEADER = "Action Success";
    public static final String ALERT_HEADER = "Alert";

    public final static Logger logger = Logger.getLogger(Main.class);


    public static String TITLE = "";
    public static String ICON = "";
    public static String SERVER_URL = "";
    public static String linkId = "";
    public static String currentUsername;
    public static String currentPassword;
    public static Stage stage;
    public static Scene scene;
    public static Properties prop;
    public static Stage loadingStage;

    public Main() {
        prop=new Properties();
        try {
            FileInputStream log4jProperty =  new FileInputStream("log4j.properties");
            PropertyConfigurator.configure(log4jProperty);
            FileInputStream config= new FileInputStream("config.properties");
            prop.load(config);
            SERVER_URL = prop.getProperty("SERVER_URL");
            TITLE = prop.getProperty("TITLE");
            ICON = prop.getProperty("ICON");
            config.close();
        } catch (IOException e) {
            e.printStackTrace();
            createNotificationDialog(e.toString(), null, null);
        }
    }

    public void initialize() {
    }

    private void createLoadingWindow() throws IOException {
        Parent loadingRoot = FXMLLoader.load(getClass().getResource(LOADING));
        loadingStage = new Stage();
        loadingStage.setScene(new Scene(loadingRoot));
        loadingStage.initStyle(StageStyle.TRANSPARENT);
        loadingStage.initModality(Modality.APPLICATION_MODAL);
    }

    protected static void setTextFieldLength(final TextField tf, final int maxLength) {
        tf.textProperty().addListener((ov, oldValue, newValue) -> {
            if (tf.getText().length() > maxLength) {
                String s = tf.getText().substring(0, maxLength);
                tf.setText(s);
            }
        });
    }
    @Override
    public void stop() {
    }

    protected void notificationForAction(Resultinfo resultinfo, String url) throws IOException {
        if (resultinfo.getErrCd() == API_CODE_SUCCESS) {
            createNotificationDialog(SUCCESS_HEADER, null, url);
        } else if(resultinfo.getErrCd() == API_CODE_LOGOUT){
            logoutByExpireSession(url);
        } else {
            createNotificationDialog(ERROR_HEADER, resultinfo.getErrString(), url);
        }
    }

    protected void setSceneByView(String resourceName) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(resourceName));
        scene.setRoot(root);
    }

    public void lg(String a) {
        System.out.println(a);
    }

    public static Gson gson = new Gson();

    protected void logoutByExpireSession(String url) throws IOException {
        createNotificationDialog(Main.SESSION_EXPIRE_HEADER, Main.SESSION_EXPIRE_CONTENT, url);
        logout();
    }

    protected void logout() throws IOException {
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);
        stage.setX((primScreenBounds.getWidth() - WIDTH) / 2);
        stage.setY((primScreenBounds.getHeight() - HEIGHT) / 2);
        setSceneByView(LOGIN_FXML);
    }

    public void changeDateTimeToKrTypeAndDisableEditor(DatePicker datePicker) {
        String pattern = "yyyy/MM/dd";
        datePicker.setPromptText(pattern.toLowerCase());
        datePicker.setConverter(new StringConverter<LocalDate>() {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateTimeFormatter.format((date));
                } else {
                    return "";
                }
            }
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateTimeFormatter);
                } else {
                    return null;
                }
            }
        });
        datePicker.setEditable(false);
    }

    public void createNotificationDialog(String header, String content, String url) {
        if(header.equals(ERROR_HEADER)){
            String api = url != null ? url : "";
            logger.error("Error: " + content + "API: " + api);
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("/css/main.css").toExternalForm());
        dialogPane.getStyleClass().add("dialogFolder");
        alert.setTitle("Notification Dialog");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public TreeItem<ControlBindingObj> loadFolderTreeItem(ControlBindingObj masterFolder) throws IOException {
        String urlForSubFolder = SERVER_URL + "/cm/list/folder?link_id=" + linkId +"&parentid=" + masterFolder.getId();
        String responseForSubFolder = getResponseFromAPI(urlForSubFolder);
        MasterFolderModel subFolderObj = gson.fromJson(responseForSubFolder, MasterFolderModel.class);
        if (subFolderObj.getResultinfo().getErrCd() == API_CODE_SUCCESS && subFolderObj.getCmFolderList().getCmFolderData().size() > 0) {
            TreeItem<ControlBindingObj> item = new TreeItem<>(masterFolder);
            subFolderObj.getCmFolderList().getCmFolderData().
                    forEach(index -> item.getChildren().add(new TreeItem<>(
                            new ControlBindingObj(index.getDisplay_Name(), index.getFolder_Id()))));
            return item;
        } else if (subFolderObj.getResultinfo().getErrCd() == API_CODE_LOGOUT) {
            logout();
            return null;
        } else if (subFolderObj.getResultinfo().getErrCd() != API_CODE_SUCCESS && subFolderObj.getResultinfo().getErrCd() != API_CODE_LOGOUT){
            createNotificationDialog(ERROR_HEADER, subFolderObj.getResultinfo().getErrString(), urlForSubFolder);
            return null;
        } else {
            return null;
        }
    }

    protected String getResponseFromAPI(String url) {
        try {
            BufferedReader in;
            if (url.matches("^https:\\/\\/.+")) {
                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
                };

                // Install the all-trusting trust manager
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                // Create all-trusting host name verifier
                HostnameVerifier allHostsValid = new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };

                // Install the all-trusting host verifier
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

                URL obj = new URL(url);
                HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            } else {
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            }

            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
            createNotificationDialog(ERROR_HEADER, e.getMessage(), null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
            createNotificationDialog(ERROR_HEADER, e.getMessage(), null);
        }
        return null;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            createLoadingWindow();
            stage = primaryStage;
            stage.getIcons().add(new Image(ICON));
            stage.setTitle(TITLE);
            stage.setOnCloseRequest(event -> {
                Alert alert = createAlert("Confirm to close window");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK){
                    System.exit(0);
                } else {
                    event.consume();
                }
            });
            createLoginWindow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Alert createAlert(String header) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText(header);
        alert.setContentText(null);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("/css/main.css").toExternalForm());
        dialogPane.getStyleClass().add("dialogFolder");
        return alert;
    }

    private void createLoginWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(LOGIN_FXML));
        Region root = loader.load();
        LoginController loginController = loader.getController();
        scene = new Scene(root, WIDTH, HEIGHT);
        loginController.setKeyBinding(scene);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
