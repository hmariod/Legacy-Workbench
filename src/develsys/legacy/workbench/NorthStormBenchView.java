/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package develsys.legacy.workbench;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author hmariod me
 */
public class NorthStormBenchView extends AnchorPane{
    private ComboBox cmbSelectComm;
    private ComboBox cmbSelectBaudRate;
    private Button btnToogleConnection;
    private TextArea taCommIntput;
    private ImageView imgRefresh;
    private Label lbConsolePrompt;
    private Button btnSendSeeking;
    private Stage primaryStage;
    private WorkbenchApp app;
    private TabPane tabPane;
    private Tab tabTerminal;
    private Tab tabControls;
        
    CommPortIdentifier currentPortIdentifier = null;
    public SerialPort serialPort;
    /** Buffered input stream from the port */
    private InputStream input;
    /** The output stream to the port */
    private OutputStream output;
    /** Milliseconds to block while waiting for port open */
    private static final int TIME_OUT = 2000;
    /** Default bits per second for COM port. */
    private static final int DATA_RATE = 9600;
    
    public NorthStormBenchView(){}
    
    public void load(){
        setBottomAnchor(this, 0.0);
        setTopAnchor(this, 0.0);
        setLeftAnchor(this, 0.0);
        setRightAnchor(this, 0.0);
        setPrefSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);

        //-- CommControlsWraper --
        HBox hbCommControlsWraper = new HBox();
        setTopAnchor(hbCommControlsWraper, 0.0);
        setLeftAnchor(hbCommControlsWraper, 0.0);
        setRightAnchor(hbCommControlsWraper, 0.0);
        hbCommControlsWraper.setSpacing(10.0);
        hbCommControlsWraper.setAlignment(Pos.CENTER_LEFT);
        hbCommControlsWraper.setPadding(new Insets(10,10,10,10));
        hbCommControlsWraper.setStyle("-fx-background-color:linear-gradient(white, silver);");
        
        cmbSelectComm = new ComboBox();
        cmbSelectComm.setPrefWidth(192);
        cmbSelectComm.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        cmbSelectComm.setPromptText("Select");
        
        imgRefresh = new ImageView(new Image(WorkbenchApp.class.getResourceAsStream("images/reload.png")));
        
        cmbSelectBaudRate = new ComboBox();
        cmbSelectBaudRate.setPrefWidth(85);
        cmbSelectBaudRate.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        cmbSelectBaudRate.getItems().addAll("9600","14400","19200","28800","38400","56000","57600","115200");
        cmbSelectBaudRate.setValue("9600");
        
        btnToogleConnection = new Button("Open");
        btnToogleConnection.setPrefWidth(70.0);
        btnToogleConnection.setMinWidth(USE_PREF_SIZE);
        btnToogleConnection.disableProperty().set(true);
        
        lbConsolePrompt = new Label("");
        
        hbCommControlsWraper.getChildren().addAll(cmbSelectComm, imgRefresh, cmbSelectBaudRate, btnToogleConnection, lbConsolePrompt);
        
        //-- TabPane --
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        setBottomAnchor(tabPane, 0.0);
        setTopAnchor(tabPane, 42.0);
        setLeftAnchor(tabPane, 0.0);
        setRightAnchor(tabPane, 0.0);
        //tabPane.setStyle("-fx-control-inner-background:  rgb(250, 250, 250);");
        
        //tab Controls
        HBox hbControlsWraper = new HBox();
        setTopAnchor(hbControlsWraper, 0.0);
        setLeftAnchor(hbControlsWraper, 0.0);
        setRightAnchor(hbControlsWraper, 0.0);
        hbControlsWraper.setSpacing(10.0);
        hbControlsWraper.setAlignment(Pos.TOP_LEFT);
        hbControlsWraper.setPadding(new Insets(10,10,10,10));
        hbControlsWraper.setStyle("-fx-background-color:linear-gradient(white, silver);");
        
        HBox hbButtonsWraper = new HBox();
        setTopAnchor(hbButtonsWraper, 0.0);
        setLeftAnchor(hbButtonsWraper, 0.0);
        setRightAnchor(hbButtonsWraper, 0.0);
        hbButtonsWraper.setSpacing(10.0);
        hbButtonsWraper.setAlignment(Pos.TOP_LEFT);
        //hbButtonsWraper.setPadding(new Insets(10,10,10,10));
        hbButtonsWraper.setStyle("-fx-background-color:linear-gradient(white, silver);");
        
        btnSendSeeking = new Button("Seek");
        btnSendSeeking.setPrefWidth(70.0);
        btnSendSeeking.setMinWidth(USE_PREF_SIZE);
        btnSendSeeking.disableProperty().set(true);
        
        hbButtonsWraper.getChildren().addAll(btnSendSeeking);
        
        hbControlsWraper.getChildren().addAll(hbButtonsWraper);
        
        tabControls = new Tab("North Storm");
        tabControls.setContent(hbControlsWraper);
        
        //tab Terminal
        tabTerminal = new Tab("Terminal");
        taCommIntput = new TextArea();
        //taCommIntput.setStyle("-fx-border-color:silver;");
        tabTerminal.setContent(taCommIntput);
        
        
        tabPane.getTabs().addAll(tabControls, tabTerminal);
        
        getChildren().addAll(hbCommControlsWraper, tabPane);
    }
    
    public void init(WorkbenchApp app){
        this.app = app;
        primaryStage = app.primaryStage;
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e){
                if(serialPort != null){
                    serialPort.close();
                }
            }
        });
        
        taCommIntput.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent t) {
                if(t.getCode().equals(KeyCode.ENTER)){
                    if(serialPort != null){
                        CharSequence paragraphs[] = taCommIntput.getParagraphs().toArray(new CharSequence[0]);
                        String strCmd = paragraphs[paragraphs.length-1].toString();
                        try {
                            sendCmd(strCmd.getBytes("UTF8"), true);
                        } catch (UnsupportedEncodingException ex) {
                            Logger.getLogger(TerminalView.class.getName()).log(Level.SEVERE, null, ex);
                        }                        
                    }
                }
            }
        });
        
        imgRefresh.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                if(serialPort == null){
                    getPorts();
                }
            }
        });
        
        imgRefresh.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                if(serialPort == null){
                    imgRefresh.setEffect(new DropShadow(15, Color.BLACK));
                }
            }
        });
        
        imgRefresh.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                imgRefresh.setEffect(null);
            }
        });
        
        cmbSelectComm.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue ov, Object t, Object t1) {
                try{
                    if(!cmbSelectComm.getSelectionModel().isEmpty()){
                        NorthStormBenchView.CommCmbItem commCmbItem = (NorthStormBenchView.CommCmbItem) ov.getValue();
                        currentPortIdentifier = commCmbItem.portIdentifier;
                        btnToogleConnection.disableProperty().set(false); 
                    }
                }catch(Exception er){ }
            }
        });
        
        btnToogleConnection.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if("Open".equals(btnToogleConnection.getText())){
                    if(openComm(currentPortIdentifier)){
                        btnToogleConnection.setText("Close");
                        cmbSelectComm.setDisable(true);
                        cmbSelectComm.setOpacity(1.0);
                        cmbSelectBaudRate.setDisable(true);
                        cmbSelectBaudRate.setOpacity(1.0);
                        btnSendSeeking.setDisable(false);
                        taCommIntput.requestFocus();
                    }
                }else{
                    closeComm();
                    getPorts();
                }
            }
        });
        
        btnSendSeeking.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                sendSeek();
            }
        });
        
        getPorts();
    }
    
    
    
    /*
     * Comm Port functions
     */
    private void getPorts(){
        cmbSelectComm.getItems().clear();
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
        String portName;
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            portName = currPortId.getName();
            if(portName.indexOf("tty")>-1){
                String[] arrTmp = currPortId.getName().split("/");
                if(arrTmp.length > 2){
                    portName = arrTmp[2].substring(4);
                }
            }else if(portName.indexOf("Com") < 0){
                continue;
            }
            System.out.println(currPortId.getName());
            cmbSelectComm.getItems().add((new NorthStormBenchView.CommCmbItem(portName,currPortId)));
        }
        lbConsolePrompt.setText("Comm closed.");
    };
    
    private boolean openComm(CommPortIdentifier commIdentifier){
        try {
            serialPort = (SerialPort) commIdentifier.open(this.getClass().getName(),TIME_OUT);
            serialPort.setSerialPortParams(Integer.parseInt(cmbSelectBaudRate.getValue().toString()),
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
            try {
                // open streams
                input = serialPort.getInputStream();
                output = serialPort.getOutputStream();
            } catch (IOException ex) {
                lbConsolePrompt.setText("Comm port unavailable.");
                System.out.println("Comm port unavailable: " + ex.toString());
                return false;
            }
            serialPort.addEventListener(new SerialPortEventListener() {
                @Override
                public void serialEvent(SerialPortEvent spe) {
                    if(spe.getEventType() == SerialPortEvent.DATA_AVAILABLE){
                        try{
                            int available = input.available();
                            byte chunk[] = new byte[available];
                            int read = input.read(chunk, 0, available);
                            taCommIntput.appendText(bytesToHexString(chunk).toUpperCase());
                            taCommIntput.appendText("\n");
                        }catch(Exception ex){
                            System.out.println("DATA_AVAILABLE Exception: " + ex.toString());
                        }
                    }
                }
            });
            serialPort.notifyOnDataAvailable(true);
        } catch (TooManyListenersException ex) {
            System.out.println("Comm TooManyListenersException: " + ex.toString());
            Logger.getLogger(TerminalView.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (PortInUseException | UnsupportedCommOperationException er) {
            lbConsolePrompt.setText("Comm port unavailable. " + er.toString());
            System.out.println("Error open Comm: " + er.toString());
            return false;
        }
        System.out.println("Comm open");
        lbConsolePrompt.setText("Comm open.");
        return true;
    };
    
    private void sendCmd(byte[] bytes, boolean addNullEnd){
        try{
            output.write(bytes);
            if(addNullEnd){
                output.write(0x00);
            }
        } catch (IOException ex) {
            Logger.getLogger(TerminalView.class.getName()).log(Level.SEVERE, null, ex);
        }
    };
    
    private void closeComm(){
        if(serialPort != null){
            serialPort.close();
            serialPort.removeEventListener();
            serialPort = null;
        }
        cmbSelectComm.setDisable(false);
        cmbSelectBaudRate.setDisable(false);
        btnSendSeeking.setDisable(true);
        btnToogleConnection.setText("Open");
    };
    
    
    /*
     * Hex Convertions
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    private String bytesToHexString(byte[] bytes) {  
        StringBuilder sb = new StringBuilder(bytes.length * 2);  
        Formatter formatter = new Formatter(sb);  
        for (byte b : bytes) {  
            formatter.format("%02x ", b);  
        }  
        return sb.toString();  
    };
    
    private void sendSeek(){
        byte[] msg = hexStringToByteArray("ffff7a80faff");
        sendCmd(msg, false);
    }
    
    
    /*
     * Class CommCmbItem
     */
    private class CommCmbItem{
        String portName;
        CommPortIdentifier portIdentifier;
        CommCmbItem(String portName, CommPortIdentifier port){
            this.portName = portName;
            this.portIdentifier = port;
        };
        @Override
        public String toString() { return portName; };
        public CommPortIdentifier getPort(){ return portIdentifier;};
    };
    
}
