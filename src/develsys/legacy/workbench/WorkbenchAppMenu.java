/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package develsys.legacy.workbench;

import develsys.utils.LoginEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 *
 * @author hmariod
 */
public class WorkbenchAppMenu extends HBox{
    private Stage primaryStage;
    public WorkbenchApp app;
    public MenuItem menuItemUserLogoff;
    public Menu menuUser;
    

    public WorkbenchAppMenu(){
        MenuBar menuBar = new MenuBar();
        menuBar.useSystemMenuBarProperty().set(true);
        
        //-- Menu Help --
        Menu menuHelp = new Menu("Help");
        //-- Menu Help -- Items
        MenuItem menuItemAbout = new MenuItem("About");
        // Add items
        menuHelp.getItems().addAll(menuItemAbout);
        
        //-- Menu User --
        menuUser = new Menu("User");
        menuItemUserLogoff = new MenuItem("Logoff");
        menuItemUserLogoff.setDisable(true);
        menuUser.getItems().addAll(menuItemUserLogoff);
        
        // Add menus to menubar
        menuBar.getMenus().addAll(menuHelp, menuUser);
        
        // Add menuBar to AnchotPane
        this.getChildren().add(menuBar);
    }
    
    public void init(WorkbenchApp app){
        //final WorkbenchAppMenu root = app.appMenu;
        this.app = app;
        primaryStage = app.primaryStage;
        menuItemUserLogoff.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                fireEvent(new LoginEvent(LoginEvent.USER_LOG_OFF));
                menuUser.setVisible(false);
                menuItemUserLogoff.setDisable(true);
                menuUser.setVisible(true);
            }
        });
    }
    
    
    
    
    
}
