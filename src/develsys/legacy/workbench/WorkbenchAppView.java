/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package develsys.legacy.workbench;

import develsys.utils.LoginEvent;
import develsys.utils.SysUser;
import javafx.event.EventHandler;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 *
 * @author hmariod
 */
public class WorkbenchAppView extends AnchorPane{
    
    private Stage primaryStage;
    private WorkbenchApp app;
    private LoginView loginView;
    private TerminalView terminalView;
    private NorthStormBenchView northStormBenchView;
    private boolean NorthStormBench = false;
    private EventHandler loginEventHandler = new EventHandler <LoginEvent>() { 
        @Override
        public void handle(LoginEvent event) {
            if(event.getEventType() == LoginEvent.USER_LOG_ON){
                primaryStage.setTitle("Legacy Workbench - " + loginView.sysUser.userName);
                showCurrentView();
            }else{
                invalidateUser();
            }
        }
    };
    public WorkbenchAppView(){}
    
    public void load(WorkbenchApp app){
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);
        AnchorPane.setBottomAnchor(this, 0.0);
        AnchorPane.setTopAnchor(this, 0.0);
        AnchorPane.setLeftAnchor(this, 0.0);
        AnchorPane.setRightAnchor(this, 0.0);
        
        //Load Login
        loginView = new LoginView();
        loginView.load();
        loginView.setVisible(false);
        
        if("yes".equals(app.getAppProperty("NorthStormBench"))){
            NorthStormBench = true;
            //Load NorthStormBench
            northStormBenchView = new NorthStormBenchView();
            northStormBenchView.load();
            northStormBenchView.setVisible(true);
        }else{
            //Load Terminal
            terminalView = new TerminalView();
            terminalView.load();
            terminalView.setVisible(false);
        }
        

        
        //Add childrens
        getChildren().add(loginView);
        if(NorthStormBench){
            getChildren().add(northStormBenchView);
        }else{
            getChildren().add(terminalView);
        }
        

    }
    
    public void init(WorkbenchApp app){
        this.app = app;
        primaryStage = app.primaryStage;
        addEventHandler(LoginEvent.USER_LOG_ON, loginEventHandler);
        //addEventHandler(LoginEvent.USER_LOG_OFF, loginEventHandler);
        loginView.init(app);
        if(NorthStormBench){
            northStormBenchView.init(app);
        }else{
            terminalView.init(app);
        }
    }
    
    private void showCurrentView(){
        loginView.setVisible(false);
        if(NorthStormBench){
            northStormBenchView.setVisible(true);
        }else{
            terminalView.setVisible(true);
        }
    }

    public void invalidateUser(){
        primaryStage.setTitle("Legacy Workbench");
        app.appMenu.menuUser.setText("User");
        app.sysUser.status = SysUser.statusEnum.AWAY;
        loginView.setVisible(true);
        if(NorthStormBench){
            northStormBenchView.setVisible(false);
        }else{
            terminalView.setVisible(false);
        }
    }
    
}
