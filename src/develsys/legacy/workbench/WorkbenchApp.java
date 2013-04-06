
package develsys.legacy.workbench;

import develsys.utils.LoginEvent;
import develsys.utils.SysUser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 *
 * @author hmariod
 */
public class WorkbenchApp extends Application {
    public Stage primaryStage;
    public AnchorPane root;
    public WorkbenchAppMenu appMenu;
    public WorkbenchAppView appView;
    public SysUser sysUser = new SysUser();
    public Properties appProperties = new Properties();
    private File appPropertiesFile = new File("defaultProperties");
    private EventHandler loginEventHandler = new EventHandler <LoginEvent>() { 
        @Override
        public void handle(LoginEvent event) {
            if(event.getEventType() == LoginEvent.USER_LOG_ON){
                appMenu.menuUser.setVisible(false);
                appMenu.menuUser.setText(sysUser.userName);
                appMenu.menuItemUserLogoff.setDisable(false);
                appMenu.menuUser.setVisible(true);
                sysUser.status = SysUser.statusEnum.LOGGED;
            }else{
                sysUser.status = SysUser.statusEnum.AWAY;
                appView.invalidateUser();
            }
        }
    };
    @Override
    public void start(Stage primaryStage) {
        //App Default Properties
        if(getAppProperties()){
            if("".equals(getAppProperty("servicesHost"))){
                setAppProperty("servicesHost", "direct.playcenter.com.br");
            }
        }
        //User Interfase
        this.primaryStage = primaryStage;
        appMenu = new WorkbenchAppMenu();
        appView = new WorkbenchAppView();
        appView.load(this);
        appMenu.init(this);
        appView.init(this);
        
        root = new AnchorPane();
        root.getChildren().addAll(appView, appMenu);

        Scene scene = new Scene(root,800,600);

        primaryStage.setTitle("Legacy Workbench");
        primaryStage.setScene(scene);
        primaryStage.show();
        Init();
    }

    private void Init(){
        root.addEventHandler(LoginEvent.USER_LOG_ON, loginEventHandler);
        root.addEventHandler(LoginEvent.USER_LOG_OFF, loginEventHandler);
    }
    
    //------------- App Properties -----------------------------------
    private boolean getAppProperties(){
        try{
            appPropertiesFile.createNewFile(); //Crea el archivo si no existe
            try (FileInputStream propInputStream = new FileInputStream(appPropertiesFile)) {
                appProperties.load(propInputStream);
            }
        }catch(IOException ex){
            Logger.getLogger(WorkbenchApp.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }
 
    public String getAppProperty(String prop){
        return getAppProperty(prop,"");
    }
    
    public String getAppProperty(String prop, String _default){
        String str = appProperties.getProperty(prop, _default);
        return str;
    }
    
    public void setAppProperty(String key, String value){
        FileOutputStream propOutputStream = null;
        try {
            appProperties.setProperty(key, value);
            propOutputStream = new FileOutputStream(appPropertiesFile);
            appProperties.store(propOutputStream, "");
        } catch (IOException ex) {
            Logger.getLogger(WorkbenchApp.class.getName()).log(Level.SEVERE, null, ex);
        }finally {
            try {
                propOutputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(WorkbenchApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    //----------------------------------------------------
    public static void main(String[] args) {
        launch(args);
    }
}
