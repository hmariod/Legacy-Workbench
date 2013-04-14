/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package develsys.legacy.workbench;

import develsys.utils.HttpSimpleClient;
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

/**
 *
 * @author hmariod me
 */
public class NorthStormBenchView extends AnchorPane{
    private ComboBox cmbSelectComm;
    private ComboBox cmbSelectBaudRate;
    private Button btnToogleConnection;
    private TextArea taCommIntput;
    private Label lbCommPortStatus;
    private Label lbNorthStormControlsStatus;
    private Button btnSendSeeking;
    private Button reloadPortsListButton;
    private ImageView imgTX;
    private ImageView imgRX;
    private ImageView imgCTS;
    private Stage primaryStage;
    private WorkbenchApp app;
    private Tab tabTerminal;
    private Tab tabNorthStormControls;
    private TableView tableUnsetDevices;
    private ObservableList<UnsetDevice> deviceList = FXCollections.observableArrayList();;
    private enum CommPortStatus{
        CLOSED, OPEN, ERROR
    }
    private CommPortStatus commPortStatus;
    private enum ControlStatus{
        NOCOMM, SEEKING, DEVICEFOUND, DEVICEWAITPERSON, SETTINGPERSON, STOPPED
    }
    private ControlStatus controlStatus;
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
    private Timer tmCommTimeout;
    private int seekCounter;
    private byte[] rxBuffer = new byte[100];
    private int rxBytesReceived = 0;
    private UnsetDevice currentUnsetDevice;
    
    public NorthStormBenchView(){}
    
    public void load(){
        setBottomAnchor(this, 0.0);
        setTopAnchor(this, 0.0);
        setLeftAnchor(this, 0.0);
        setRightAnchor(this, 0.0);
        setPrefSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);

        //Top Menu bar, comm controls, HBox
        cmbSelectComm = new ComboBox();
        cmbSelectComm.setPrefWidth(192);
        cmbSelectComm.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        cmbSelectComm.setPromptText("Select");
        //-Reload buttom
        reloadPortsListButton = new Button();
        reloadPortsListButton.setGraphic(new ImageView(new Image(WorkbenchApp.class.getResourceAsStream("images/reload.png"))));
        reloadPortsListButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(serialPort == null){
                    getPorts();
                    System.out.println("reloadPortsListButton called.\n");
                }
            }
        });
        reloadPortsListButton.setMaxHeight(Double.MAX_VALUE);
        Tooltip reloadPortsListButtonTooltip = new Tooltip();
        reloadPortsListButtonTooltip.setText("Reload Port list\n");
        reloadPortsListButton.setTooltip(reloadPortsListButtonTooltip);
        //-Select Baudrate
        cmbSelectBaudRate = new ComboBox();
        cmbSelectBaudRate.setPrefWidth(85);
        cmbSelectBaudRate.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        cmbSelectBaudRate.getItems().addAll("9600","14400","19200","28800","38400","56000","57600","115200");
        cmbSelectBaudRate.setValue("9600");
        //-Open/Close comm
        btnToogleConnection = new Button("Open");
        btnToogleConnection.setPrefWidth(70.0);
        btnToogleConnection.setMinWidth(USE_PREF_SIZE);
        btnToogleConnection.disableProperty().set(true);
        //-Label CommPortStatus
        lbCommPortStatus = new Label("");
        
        //-Leds
        //--Green
        StackPane spLedTX = new StackPane();
        ImageView imgViewLedTXOff = new ImageView(new Image(WorkbenchApp.class.getResourceAsStream("images/ledGreenOff.png")));
        imgTX = new ImageView(new Image(WorkbenchApp.class.getResourceAsStream("images/ledGreenOn.png")));
        imgTX.setVisible(false);
        spLedTX.getChildren().addAll(imgViewLedTXOff, imgTX);
        //--Red
        StackPane spLedRX = new StackPane();
        ImageView imgViewLedRXOff = new ImageView(new Image(WorkbenchApp.class.getResourceAsStream("images/ledRedOff.png")));
        imgRX = new ImageView(new Image(WorkbenchApp.class.getResourceAsStream("images/ledRedOn.png")));
        imgRX.setVisible(false);
        spLedRX.getChildren().addAll(imgViewLedRXOff, imgRX);
        //--Blue
        StackPane spLedCTS = new StackPane();
        ImageView imgViewLedCTSOff = new ImageView(new Image(WorkbenchApp.class.getResourceAsStream("images/ledBlueOff.png")));
        imgCTS = new ImageView(new Image(WorkbenchApp.class.getResourceAsStream("images/ledBlueOn3.png")));
        imgCTS.setVisible(false);
        spLedCTS.getChildren().addAll(imgViewLedCTSOff, imgCTS);
        //_
        HBox boxLeds = new HBox();
        boxLeds.setId("boxLeds");
        boxLeds.setAlignment(Pos.BOTTOM_RIGHT);
        boxLeds.getChildren().addAll(spLedTX, spLedRX, spLedCTS);

        //Top Menu bar CommControlsWraper --
        HBox hbCommControlsWraper = new HBox();
        setTopAnchor(hbCommControlsWraper, 0.0);
        setLeftAnchor(hbCommControlsWraper, 0.0);
        setRightAnchor(hbCommControlsWraper, 0.0);
        hbCommControlsWraper.setSpacing(10.0);
        hbCommControlsWraper.setAlignment(Pos.CENTER_LEFT);
        hbCommControlsWraper.setPadding(new Insets(10,10,10,10));
        hbCommControlsWraper.setStyle("-fx-background-color:linear-gradient(white, silver);");
        hbCommControlsWraper.setHgrow(boxLeds, Priority.ALWAYS);
        hbCommControlsWraper.getChildren().addAll(cmbSelectComm, reloadPortsListButton, cmbSelectBaudRate, btnToogleConnection, lbCommPortStatus, boxLeds);
        
        
        //Tab NorthStomControls
        //-NorthStormControlsMenu
        //--NortStormControlMenuStatusLabel
        lbNorthStormControlsStatus = new Label("Status:");
        //lbNorthStormControlsStatus.setMinHeight(20.0);
        lbNorthStormControlsStatus.setAlignment(Pos.BOTTOM_LEFT);
        lbNorthStormControlsStatus.setId("lbNorthStormControlStatus");
        //--Btn Seeking
        btnSendSeeking = new Button("Seek");
        btnSendSeeking.setPrefWidth(70.0);
        btnSendSeeking.setMinWidth(USE_PREF_SIZE);
        
        HBox hbNorthStormControlsMenu = new HBox();
        hbNorthStormControlsMenu.setId("hbNorthStormControlsMenu");
        hbNorthStormControlsMenu.setSpacing(10.0);
        hbNorthStormControlsMenu.setAlignment(Pos.BOTTOM_LEFT);
        hbNorthStormControlsMenu.getChildren().addAll(lbNorthStormControlsStatus, btnSendSeeking);
        
        //Table unsetDevices
        tableUnsetDevices = new TableView<>();
        tableUnsetDevices.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableUnsetDevices,Priority.ALWAYS);   //Grow vertically until reach the bottom, fill the entire vertical empty space.
        //tableUnsetDevices.setPlaceholder(new Text(""));
        //-Column Owner-Numero
        TableColumn ONCol = new TableColumn<>("ID");
        ONCol.setCellValueFactory(new PropertyValueFactory("ownerNumero"));
        ONCol.setPrefWidth(100.0);
        ONCol.setMinWidth(100.0);
        ONCol.setMaxWidth(100.0);
        //-Column Boca, centered
        TableColumn bocaCol = new TableColumn("Boca");
        bocaCol.setPrefWidth(80.0);
        bocaCol.setMinWidth(80.0);
        bocaCol.setMaxWidth(80.0);
        bocaCol.setCellValueFactory(new PropertyValueFactory("boca"));
        bocaCol.setCellFactory(new Callback<TableColumn,TableCell>(){
            @Override
            public TableCell call(TableColumn p) {
                TableCell cell = new TableCell<UnsetDevice, String>(){
                    @Override
                    public void updateItem(String item, boolean empty){
                        super.updateItem(item, empty);
                        if(empty){
                            setText(null);
                            setGraphic(null);
                        }else{
                            setText(getItem() == null ? "" : getItem().toString());
                        }
                    }
                };
                cell.setAlignment(Pos.TOP_CENTER); 
                return cell;
            }
        });
        //-Colunm Clase
        TableColumn claseCol = new TableColumn("Clase");
        claseCol.setCellValueFactory(new PropertyValueFactory("claseNameFull"));
        //Add columns to table and bound the content with deviceList
        tableUnsetDevices.getColumns().addAll(ONCol, bocaCol, claseCol);
        tableUnsetDevices.setItems(deviceList);
        
        //TabNorthStormContenWraper VBox
        VBox vboxTabNorthStormContensWraper = new VBox();
        setTopAnchor(vboxTabNorthStormContensWraper, 0.0);
        setLeftAnchor(vboxTabNorthStormContensWraper, 0.0);
        setRightAnchor(vboxTabNorthStormContensWraper, 0.0);
        vboxTabNorthStormContensWraper.setSpacing(1.0);
        vboxTabNorthStormContensWraper.setAlignment(Pos.TOP_LEFT);
        vboxTabNorthStormContensWraper.setPadding(new Insets(10,10,10,10));
        vboxTabNorthStormContensWraper.setStyle("-fx-background-color:linear-gradient(white, #A0A0A0);");
        vboxTabNorthStormContensWraper.getChildren().addAll(hbNorthStormControlsMenu, tableUnsetDevices);
        
        //Create tab NortStormControls and set contentns
        tabNorthStormControls = new Tab("North Storm");
        tabNorthStormControls.setContent(vboxTabNorthStormContensWraper);
        
        //Create tab Terminal and set content
        taCommIntput = new TextArea();
        tabTerminal = new Tab("Terminal");
        tabTerminal.setContent(taCommIntput);
        
        //Create TabPanel and add TabNorthStormControls + TabTerminal
        TabPane tabPanel = new TabPane();
        tabPanel.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        setBottomAnchor(tabPanel, 0.0);
        setTopAnchor(tabPanel, 42.0);
        setLeftAnchor(tabPanel, 0.0);
        setRightAnchor(tabPanel, 0.0);
        tabPanel.getTabs().addAll(tabNorthStormControls, tabTerminal);
        
        //Add CommControlMenuWraper + TabPanel
        getChildren().addAll(hbCommControlsWraper, tabPanel);
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
        
        cmbSelectComm.valueProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue ov, Object t, Object t1) {
                try{
                    if(!cmbSelectComm.getSelectionModel().isEmpty()){
                        NorthStormBenchView.CommCmbItem commCmbItem = (NorthStormBenchView.CommCmbItem) ov.getValue();
                        currentPortIdentifier = commCmbItem.portIdentifier;
                        btnToogleConnection.disableProperty().set(false);
                        if("Open".equals(btnToogleConnection.getText())){
                            openComm(currentPortIdentifier);
                        }
                    }
                }catch(Exception er){ }
            }
        });
        
        btnToogleConnection.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if("Open".equals(btnToogleConnection.getText())){
                    openComm(currentPortIdentifier);
                }else{
                    closeComm();
                }
            }
        });
        
        btnSendSeeking.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                sendSeek(2);
                reloadUnsetDevicesTable();
            }
        });
        
        tableUnsetDevices.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent t) {
                if(t.getCode() == KeyCode.ENTER && t.isControlDown()){
                    int ix = tableUnsetDevices.getSelectionModel().getSelectedIndex();
                    if(ix > -1){
                        System.out.println(deviceList.get(ix).ownerNumero.getValue());
                        personalizeDevice(ix);
                    }
                }
            }
        });
        
        tableUnsetDevices.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent t) {
                if(t.getClickCount() > 1){
                    int ix = tableUnsetDevices.getSelectionModel().getSelectedIndex();
                    if(ix > -1){
                        System.out.println(deviceList.get(ix).ownerNumero.getValue());
                        personalizeDevice(ix);
                    }
    //                Otra forma
    //                ObservableList<TablePosition> cells = tableUnsetDevices.getSelectionModel().getSelectedCells();
    //                if(!cells.isEmpty()){
    //                    int ix = cells.get(0).getRow();
    //                    System.out.println(deviceList.get(ix).ownerNumero.getValue());
    //                }                   
                }
            }
        });
        setCommPortStatus(CommPortStatus.CLOSED);
        getPorts();
        reloadUnsetDevicesTable();
    }
    
    
    
    /*
     * Comm Port functions
     */
    private void getPorts(){
        String lastUsedPort = app.getAppProperty("lastUsedPort");
        boolean lastUsedPortFound = false;
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
            if(lastUsedPort.equals(portName)){
                lastUsedPortFound = true;
                cmbSelectComm.getSelectionModel().selectLast();
            }
        }
        if(lastUsedPortFound){
            //cmbSelectComm.setValue(lastUsedPort);
            btnToogleConnection.disableProperty().set(false);
        }
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
                System.out.println("Comm port unavailable: " + ex.toString());
                setCommPortStatus(CommPortStatus.ERROR);
                return false;
            }
            serialPort.addEventListener(new SerialPortEventListener() {
                @Override
                public void serialEvent(SerialPortEvent spe) {
                    switch(spe.getEventType()) {
                        case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                            imgTX.setVisible(false);
                            break;
                        case SerialPortEvent.DATA_AVAILABLE:
                            try{
                                imgRX.setVisible(true);
                                int available = input.available();
                                byte chunk[] = new byte[available];
                                int read = input.read(chunk, 0, available);
                                System.arraycopy(chunk, 0, rxBuffer, rxBytesReceived, read);
                                rxBytesReceived += read;
                                taCommIntput.appendText(bytesToHexString(chunk).toUpperCase());
                                taCommIntput.appendText("\n");
                                if(rxBuffer[rxBytesReceived -1] == (byte)0xFE){
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            imgRX.setVisible(false);
                                            procCommAnswer();
                                        }
                                    });
                                    //procCommAnswer();
                                }
                            }catch(Exception ex){
                                System.out.println("DATA_AVAILABLE Exception: " + ex.toString());
                            }
                            break;
                        case SerialPortEvent.CTS:
                            imgCTS.setVisible(serialPort.isCTS());
                            break;
                    }
                }
            });
            serialPort.notifyOnDataAvailable(true);
            serialPort.notifyOnOutputEmpty(true);
            serialPort.notifyOnCTS(true);
        } catch (TooManyListenersException ex) {
            System.out.println("Comm TooManyListenersException: " + ex.toString());
            Logger.getLogger(TerminalView.class.getName()).log(Level.SEVERE, null, ex);
            setCommPortStatus(CommPortStatus.ERROR);
            return false;
        } catch (PortInUseException | UnsupportedCommOperationException er) {
            System.out.println("Error open Comm: " + er.toString());
            setCommPortStatus(CommPortStatus.ERROR);
            return false;
        }
        System.out.println("Comm open");
        setCommPortStatus(CommPortStatus.OPEN);
        CommCmbItem selCommCmbItem = (CommCmbItem)cmbSelectComm.getSelectionModel().getSelectedItem();
        app.setAppProperty("lastUsedPort", selCommCmbItem.portName);
        imgCTS.setVisible(serialPort.isCTS());
        return true;
    };
    
    private void sendCmd(byte[] bytes, boolean addNullEnd){
        try{
            serialPort.setDTR(true);
            try{
                output.write(bytes);
            }catch(IOException ex){
                ControlStatus currentColntrolStatus = controlStatus;
                closeComm();
                if(openComm(currentPortIdentifier)){
                    controlStatus = currentColntrolStatus;
                    output.write(bytes);
                }
            }
            if(addNullEnd){
                output.write(0x00);
            }
            rxBytesReceived = 0;
            tmCommTimeout = new Timer();
            tmCommTimeout.schedule(new TskCommTimeout(), 300);
            imgTX.setVisible(true);
        } catch (Exception ex) {
            closeComm();
        }
    };
    
    private void closeComm(){
        if(serialPort != null){
            serialPort.close();
            serialPort.removeEventListener();
            serialPort = null;
        }
        setCommPortStatus(CommPortStatus.CLOSED);
    };
    /****** End Comm Port functions ********/
    
    /*
     * Unset Devices
     */
    private boolean reloadUnsetDevicesTable(){
        boolean result;
        result = getUnsetDevices();
        if(result && !tableUnsetDevices.disableProperty().get()){
            tableUnsetDevices.getSelectionModel().select(0);
        }
        return result;
    }
    
    private boolean getUnsetDevices(){
        boolean result;
        HttpSimpleClient sc = new HttpSimpleClient(app.getAppProperty("servicesHost"));
        String url = "/legacy/services/northstormWB/getunsetdevices.asp";
        result = sc.get(url);
        if(!result){
            taCommIntput.appendText("Não foi posível completar a operação\nServiço momentaneamente indisponível.\n");
            return false;
        }
        if(sc.resultCode != 200){
            taCommIntput.appendText("Não foi posível completar a operação\nResult code: " + sc.resultCode + ".\n\n");
            return false;
        }
        
        String answers[] = sc.body.split("\\r?\\n");
        if("Result:Empty".equals(answers[0])){
            taCommIntput.appendText("Não tem dispositivos disponíveis para gravação.\n");
            return false;
        }
        if("Result:Error".equals(answers[0])){
            taCommIntput.appendText("Não foi posível completar a operação. O serviço retornou com error/s -> " + answers[1] + ".\n");
            return false;
        }
        if("Result:OK".equals(answers[0])){
            String[] arrDevices = answers[1].split("#");
            deviceList.clear();
            for(int i = 0; i < arrDevices.length; i++){
                String[] strDev = arrDevices[i].split("\\|");
                deviceList.add(new UnsetDevice(strDev[0], strDev[1], strDev[2], strDev[3], strDev[4], strDev[5], strDev[6]));
            }
            return true;
        }
        taCommIntput.appendText("Não foi posível completar a operação. Código de resposta inesperado -> " + sc.body + ".\n");
        return false;
    }
    
    private boolean setUnsetDevice(String owner, String numero, String boca, String setValue){
        boolean result;
        HttpSimpleClient sc = new HttpSimpleClient(app.getAppProperty("servicesHost"));
        String url = "/legacy/services/northstormWB/setunsetdevice.asp";
        url += "?Owner=" + owner;
        url += "&Numero=" + numero;
        url += "&Boca=" + boca;
        url += "&setValue=" + setValue;
        result = sc.get(url);
        if(!result){
            taCommIntput.appendText("SetUnsetDevices, Não foi posível completar a operação\nServiço momentaneamente indisponível.\n");
            return false;
        }
        if(sc.resultCode != 200){
            taCommIntput.appendText("SetUnsetDevices, Não foi posível completar a operação\nResult code: " + sc.resultCode + ".\n\n");
            return false;
        }
        
        String answers[] = sc.body.split("\\r?\\n");
        if("Result:Error".equals(answers[0])){
            taCommIntput.appendText("Não foi posível completar a operação. SetUnsetDevices, O serviço retornou com error/s -> " + answers[1] + ".\n");
            return false;
        }
        if("Result:OK".equals(answers[0])){
            taCommIntput.appendText("setUnsetDevices OK.\n ");
            return true;
        }
        taCommIntput.appendText("Não foi posível completar a operação. SetUnsetDevices, Código de resposta inesperado -> " + sc.body + ".\n");
        return false;
    }
    
    public static class UnsetDevice{
        private StringProperty owner;
        private StringProperty numero;
        private StringProperty boca;
        private StringProperty clase;
        private StringProperty tMaquina;
        private StringProperty tDispositivo;
        private StringProperty claseName;
        private StringProperty claseNameFull;
        private StringProperty ownerNumero;
        
        private UnsetDevice(String owner, String numero, String boca, String clase, String tMaquina, String tDispositivo, String claseName){
            this.owner = new SimpleStringProperty(owner);
            this.numero = new SimpleStringProperty(numero);
            this.boca = new SimpleStringProperty(boca);
            this.clase = new SimpleStringProperty(clase);
            this.tMaquina = new SimpleStringProperty(tMaquina);
            this.tDispositivo = new SimpleStringProperty(tDispositivo);
            this.claseName = new SimpleStringProperty(claseName);
            this.ownerNumero = new SimpleStringProperty(owner + "-" + numero);
            this.claseNameFull = new SimpleStringProperty(clase + " - " + claseName);
        }
        public String getOwnerNumero(){return ownerNumero.get();}
        public String getClaseName(){return claseName.get();}
        public String getOwner(){return owner.get();}
        public String getNumero(){return numero.get();}
        public String getBoca(){return boca.get();}
        public String getClase(){return clase.get();}
        public String gettMaquina(){return tMaquina.get();}
        public String gettDispositivo(){return tDispositivo.get();}
        public String getClaseNameFull(){return claseNameFull.get();}
    };
    
    /*
     * Comm & Control status functions
     */
    private void setCommPortStatus(CommPortStatus status){
        commPortStatus = status;
        switch(status){
            case CLOSED:
                cmbSelectComm.setDisable(false);
                cmbSelectBaudRate.setDisable(false);
                tableUnsetDevices.setDisable(true);
                btnToogleConnection.setText("Open");
                lbCommPortStatus.setText("Comm closed.");
                reloadPortsListButton.setDisable(false);
                imgCTS.setVisible(false);
                setControlStatus(ControlStatus.NOCOMM);
                break;
            case OPEN:
                btnToogleConnection.setText("Close");
                cmbSelectComm.setDisable(true);
                cmbSelectBaudRate.setDisable(true);
                btnSendSeeking.setDisable(false);
                lbCommPortStatus.setText("Comm open.");
                reloadPortsListButton.setDisable(true);
                setControlStatus(ControlStatus.STOPPED);
                sendSeek(2);
                break;
            case ERROR:
                closeComm();
                break;
        }
    };
    
    private void setControlStatus(ControlStatus status){
        controlStatus = status;
        switch(status){
            case NOCOMM:
                lbNorthStormControlsStatus.setText("Status: Comm closed.");
                btnSendSeeking.setDisable(true);
                tableUnsetDevices.setDisable(true);
                break;
            case SEEKING:
                tableUnsetDevices.setDisable(true);
                lbNorthStormControlsStatus.setText("Status: Seeking");
                btnSendSeeking.setDisable(true);
                break;
            case STOPPED:
                tableUnsetDevices.setDisable(true);
                btnSendSeeking.setDisable(false);
                lbNorthStormControlsStatus.setText("Status: Stopped");
                break;
            case DEVICEFOUND:
                tableUnsetDevices.setDisable(true);
                btnSendSeeking.setDisable(false);
                break;
            case DEVICEWAITPERSON:
                if(!reloadUnsetDevicesTable()){
                    setControlStatus(ControlStatus.STOPPED);
                    lbNorthStormControlsStatus.setText("No data services.");
                    break;
                }
                lbNorthStormControlsStatus.setText("New device found.");
                tableUnsetDevices.setDisable(false);
                tableUnsetDevices.requestFocus();
                break;
            case SETTINGPERSON:
                tableUnsetDevices.setDisable(true);
                lbNorthStormControlsStatus.setText("Status: Persolalizing device...");
                btnSendSeeking.setDisable(true);
                break;
            
        }
    };
    
    class TskCommTimeout extends TimerTask{
        @Override
        public void run() {
            tmCommTimeout.cancel();
             System.out.println("Timeout\n");
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    procCommAnswer();
                }
            });
        };
    };
    
    private void procCommAnswer(){
        tmCommTimeout.cancel();
        imgRX.setVisible(false);
        switch(controlStatus){
            case SEEKING:
                if(rxBytesReceived == 0){
                    if(seekCounter > 0){
                        lbNorthStormControlsStatus.setText("Status: Seeking again");
                        seekCounter--;
                        sendSeek(seekCounter);
                    }else{
                        setControlStatus(ControlStatus.STOPPED);
                        lbNorthStormControlsStatus.setText("No device found");                        
                    }
                }else{
                    seekCounter = 0;
                    if(rxBuffer[4] == (byte)0xF1){
                        setControlStatus(ControlStatus.DEVICEFOUND);
                        byte[] bytes = new byte[16];
                        
                        System.arraycopy(rxBuffer, 13, bytes, 0, 3);    //Owner
                        byte[] owner = new byte[3];
                        System.arraycopy(rxBuffer, 13, owner, 0, 3);
                        bytes[3] = 0x2D;    //"-"
                        System.arraycopy(rxBuffer, 16, bytes, 4, 4);    //Numero
                        byte[] numero = new byte[4];
                        System.arraycopy(rxBuffer, 16, numero, 0, 4);
                        bytes[8] = 0x2D;    //"-"
                        bytes[9] = (byte)(rxBuffer[8]|0x30);            //Boca
                        byte[] boca = new byte[]{(byte)(rxBuffer[8]|0x30)};
                        bytes[10] = 0x28;   //"("
                        System.arraycopy(rxBuffer, 9, bytes, 11, 4);    //Clase
                        bytes[15] = 0x29;   //")"
                        String strMsg = "Personalized device found: " + new String(bytes);
                        lbNorthStormControlsStatus.setText(strMsg);
                        setUnsetDevice(new String(owner), new String(numero), new String(boca),"1");
                        reloadUnsetDevicesTable();
                    }else if(rxBuffer[4] == (byte)0xEE){
                        setControlStatus(ControlStatus.DEVICEWAITPERSON);
                    }
                    taCommIntput.appendText("\nrxBuffer:");
                    taCommIntput.appendText(bytesToHexString(rxBuffer).toUpperCase());
                    taCommIntput.appendText("\n");
                }
                break;
            case SETTINGPERSON:
                if(rxBytesReceived == 0){
                    setControlStatus(ControlStatus.DEVICEWAITPERSON);
                    lbNorthStormControlsStatus.setText("Status: Personalize fail: device no answer.");
                    btnSendSeeking.setDisable(false);
                }else if(rxBuffer[4] == (byte)0xF4){    //TXOK
                    lbNorthStormControlsStatus.setText("Status: Device personalized.");
                    if(!setUnsetDevice(currentUnsetDevice.getOwner(), currentUnsetDevice.getNumero(),currentUnsetDevice.getBoca(), "1")){
                        lbNorthStormControlsStatus.setText("Status: Device personalized, database fail, stopped to avoid duplications.");
                    }else{
                        reloadUnsetDevicesTable();
                        sendSeek(2);
                    }
                }else{
                    taCommIntput.appendText("\nrxPersonalize:");
                    taCommIntput.appendText(bytesToHexString(rxBuffer).toUpperCase());
                    taCommIntput.appendText("\n");
                    lbNorthStormControlsStatus.setText("Status: Personalize fail: device answer missmatch.");
                    btnSendSeeking.setDisable(false);
                }
            default:
                System.out.println("ProcCommAnswer reach controlStatus default:" + controlStatus.toString());
        }
    };
    
    private void personalizeDevice(int ix){
        try {
            currentUnsetDevice = deviceList.get(ix);
            String owner = currentUnsetDevice.getOwner();
            byte[] bytesCmd = new byte[26];
            bytesCmd[3] = (byte)0x80;               //Address
            bytesCmd[4] = (byte)0xEE;               //Cmd = PERSON
            bytesCmd[5] = (byte)0x80;               //Address for EEProm
            bytesCmd[6] = (byte)0xEE;               //Cmd = PERSON
            byte[] tmp = currentUnsetDevice.gettDispositivo().getBytes("UTF8");
            bytesCmd[7] = (byte)(tmp[0] & 0x0F);    //TDispositivo
            bytesCmd[8] = (byte)0x01;               //TMaquina
            bytesCmd[9] = (byte)0x0A;               //TPrecio
            tmp = currentUnsetDevice.getBoca().getBytes("UTF8");
            bytesCmd[10] = (byte)(tmp[0] & 0x0F);   //Boca
            tmp = currentUnsetDevice.getClase().getBytes("UTF8");
            System.arraycopy(tmp, 0, bytesCmd, 11, 4);  //Clase
            tmp = currentUnsetDevice.getOwner().getBytes("UTF8");
            System.arraycopy(tmp, 0, bytesCmd, 15, 3);  //Owner
            tmp = currentUnsetDevice.getNumero().getBytes("UTF8");
            System.arraycopy(tmp, 0, bytesCmd, 18, 4);  //Numero
            bytesCmd[22] = (byte)0x80;           //SFlags
            bytesCmd[23] = (byte)0x20;           //IOMASK
            bytesCmd[24] = (byte)0x20;           //IOTRUE
            bytesCmd = prepareSendMsg(bytesCmd);
            setControlStatus(ControlStatus.SETTINGPERSON);
            sendCmd(bytesCmd, false);
                        taCommIntput.appendText("\nconvert:");
                        taCommIntput.appendText(bytesToHexString(bytesCmd).toUpperCase());
                        taCommIntput.appendText("\n");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(NorthStormBenchView.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    private byte[] prepareSendMsg(byte[] msg){
        byte[] data = new byte[msg.length];
        int chkSum = 0;
        System.arraycopy(msg, 0, data, 0, msg.length);
        for(int i = 3; i < msg.length-1; i++){
            chkSum += msg[i];
        }
        data[0] = data[1] = (byte)0xFF;
        chkSum = chkSum & 0xFF;
        if(chkSum > 0xFD || chkSum == 0){
            chkSum = 1;
        }
        data[2] = (byte)chkSum;
        data[msg.length-1] = (byte)0xFF;
        return data;
    }
    
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
    
    private void sendSeek(int qty){
        byte[] msg = hexStringToByteArray("ffff7a80faff");
        seekCounter = qty;
        controlStatus = ControlStatus.SEEKING;        
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
