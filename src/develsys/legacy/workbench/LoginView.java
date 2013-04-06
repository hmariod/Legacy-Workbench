/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package develsys.legacy.workbench;


import develsys.utils.HttpSimpleClient;
import develsys.utils.LoginEvent;
import develsys.utils.SysUser;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Reflection;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 *
 * @author hmariod
 */
public class LoginView extends AnchorPane {
    private Stage primaryStage;
    private WorkbenchApp app;
    private TextField txtUserName;
    private PasswordField txtPwd;
    private Label lbUserMessage;
    public SysUser sysUser;
    private HBox hbp;
    
    public LoginView(){

    }
    public SysUser load(){
        //Root
        VBox.setVgrow(this, Priority.ALWAYS);
        //setPadding(new Insets(0,0,0,0));
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);
        Rectangle recRootFill = new Rectangle(30.0, 25.0);
        recRootFill.setFill(new LinearGradient(0,0,0,1, true, CycleMethod.NO_CYCLE,
        new Stop[]{
        new Stop(0,Color.web("#FEFEFE")),
        new Stop(0.5, Color.web("#CCCCCC")),
        new Stop(1,Color.web("#777777")),})
        );
        recRootFill.widthProperty().bind(this.widthProperty());
        recRootFill.heightProperty().bind(this.heightProperty());
        getChildren().addAll(recRootFill);
        setBottomAnchor(this, 0.0);
        setTopAnchor(this, 0.0);
        setLeftAnchor(this, 0.0);
        setRightAnchor(this, 0.0);
        hbp = new HBox();
        //hbp.setPadding(new Insets(90,10,10,10));
        AnchorPane.setRightAnchor(hbp, 0.0);
        AnchorPane.setLeftAnchor(hbp, 0.0);
        AnchorPane.setTopAnchor(hbp, 90.0);
        hbp.setAlignment(Pos.CENTER);
        
        //Prompt text and DropShadow
        Text prompt = new Text("Login");
        prompt.setFont(Font.font("Courier New", FontWeight.BOLD, 28));
        prompt.setFill(Color.web("#B51E4E"));
        DropShadow dropShadow = new DropShadow(5.0,Color.DARKGRAY);
        dropShadow.setOffsetX(5);
        dropShadow.setOffsetY(5);
        prompt.setEffect(dropShadow);
        
        //Adding HBox for GridPane for login box
        HBox hbg = new HBox();
        AnchorPane.setRightAnchor(hbg, 0.0);
        AnchorPane.setLeftAnchor(hbg, 0.0);
        AnchorPane.setTopAnchor(hbg, 145.0);
        hbg.setAlignment(Pos.CENTER);
        
        //Adding GridPane for login box
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(20,30,10,30));
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setStyle("-fx-background-color:linear-gradient(lightgray, gray);-fx-background-radius: 5;");

        //Implementing Nodes for GridPane
        Label lblUserName = new Label("Username");
        txtUserName = new TextField();
        Label lblPassword = new Label("Password");
        txtPwd = new PasswordField();
        lbUserMessage = new Label();
        //lbUserMessage.setMinHeight(30);
        lbUserMessage.setStyle("-fx-text-fill:#B51E4E;-fx-text-alignment:center;");

        //Adding Nodes to GridPane layout
        gridPane.add(lblUserName, 0, 0);
        gridPane.add(txtUserName, 1, 0);
        gridPane.add(lblPassword, 0, 1);
        gridPane.add(txtPwd, 1, 1);
        gridPane.add(lbUserMessage, 0, 2);
        GridPane.setColumnSpan(lbUserMessage, 2);

        //Reflection for gridPane
        Reflection r = new Reflection();
        r.setFraction(0.7f);
        gridPane.setEffect(r);
        
        //Add GridPane to HBox
        hbg.getChildren().add(gridPane);
        
        gridPane.setAlignment(Pos.CENTER);
        
        //Adding prompt to HBox
        hbp.getChildren().add(prompt);
        
        //Adding the HBox
        getChildren().addAll(hbp, hbg);
        
        //Create the user
        return sysUser;
    }
    
    public void init(WorkbenchApp app){
        this.app = app;
        primaryStage = app.primaryStage;
        sysUser = app.sysUser;
        txtUserName.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                if(txtUserName.getText().length() > 3){
                    txtPwd.requestFocus();
                }
            }
        });
        
        txtPwd.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                if(txtPwd.getText().length() > 3){
                    autenticate();
                }
            }
        });
    }
    
    public boolean autenticate(){
        boolean result;
        HttpSimpleClient sc = new HttpSimpleClient(app.getAppProperty("servicesHost"));
        String url = "/legacy/Services/Pura/Login.asp?userName=" + txtUserName.getText() + "&pwd=" + txtPwd.getText();
        result = sc.get(url);
        if(!result){
            lbUserMessage.setText("Não foi posível completar a operação\nServiço momentaneamente indisponível.\n\n");
            return false;
        }
        if(sc.resultCode != 200){
            lbUserMessage.setText("Não foi posível completar a operação\nServiço momentaneamente indisponível.\nResult Code: " + sc.resultCode + "\n\n");
            return false;
        }
        String answers[] = sc.body.split("\\r?\\n");
        if("result:E".equals(answers[0])){
            lbUserMessage.setText("Não foi posível completar a operação\nServiço momentaneamente indisponível.\nResult Code:E0\n\n");
            return false;
        }
        if("result:NO".equals(answers[0])){
            lbUserMessage.setText("Não foi posível completar a operação\n" + answers[1].split(":")[1] + "\n\n");
            return false;
        }
        if("result:OK".equals(answers[0])){
            sysUser.userName = txtUserName.getText();
            fireEvent(new LoginEvent(LoginEvent.USER_LOG_ON));
            txtPwd.setText("");
            txtUserName.setText("");
            lbUserMessage.setText("");
            return true;
        } else{
            lbUserMessage.setText("Não foi posível completar a operação\nServiço momentaneamente indisponível.\nResult Code:No msg.\n\n");
            return false;
        }
    }

}
