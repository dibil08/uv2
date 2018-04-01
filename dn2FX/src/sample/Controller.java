package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Controller {
    public TabPane methodTab;

    public Pane encodeLoadPane;
    public javafx.scene.control.TextField encodeImgUrl;
    public Button encodeLoadButton;
    public Button encodeLoadUrl;
    public Pane encodeImagePane;
    public ImageView encodeImgPreview;
    public Text encodeImageText;
    public Pane encodeSavePane;
    public javafx.scene.control.TextField encodeSavePath;
    public Button encodeExecuteButton;
    public javafx.scene.control.Label encodeMessageLabel;
    public Pane encodeMessagePane;
    public javafx.scene.control.TextArea encodeMessageBox;
    public ProgressBar progressBar;
    public ProgressBar progressBarAttachments;
    public javafx.scene.control.Label progressBarAttachmentsLabel;
    public javafx.scene.control.Label encodeAvailableSizeAttachments;
    public javafx.scene.control.Label encodeAvailableSize;
    public Button encodeAtachmentsExecuteButton;

    public javafx.scene.control.Label progressBarLabel;
    public ComboBox encodeAttachmentList;

    public Pane decodeLoadPane;
    public Button decodeLoadButton;
    public Pane decodeImagePane;
    public ImageView decodeImgPreview;
    public javafx.scene.control.Label decodeMessageLabel;
    public javafx.scene.control.TextArea decodedMessage;
    public Pane decodeMessagePane;
    public javafx.scene.control.Label decodeImageLabel;
    public ComboBox decodedAttachmentsBox;
    public javafx.scene.control.Label decodedAttachmentsBoxLabel;

    public String imagePath;
    public String savePath;
    public int method;
    public static SteganographicImage steganographicImage;
    public Button saveAttachmentButton;

    @FXML
    void initialize() {
        method=methodTab.getSelectionModel().getSelectedIndex();
        try {
            encodeAttachmentList.getItems().removeAll(encodeAttachmentList.getItems());
            decodedAttachmentsBox.setVisible(false);
            decodedAttachmentsBoxLabel.setVisible(false);
            saveAttachmentButton.setVisible(false);
            encodeAvailableSizeAttachments.setText("0/0 B");
            progressBarAttachments.setVisible(false);
            decodedAttachmentsBox.setVisible(false);
            decodedAttachmentsBoxLabel.setVisible(false);
            decodedMessage.setVisible(false);
            decodeMessageLabel.setVisible(false);
            decodeImageLabel.setVisible(false);
            encodeImageText.setVisible(false);
            encodeImgPreview.setVisible(false);
            decodeImgPreview.setVisible(false);
            progressBarLabel.setVisible(false);
            encodeImgUrl.setText("");
            encodeSavePath.setText("");
            encodeMessageBox.setText("");
            encodeImageText.setText("");
            encodeAvailableSize.setText("0/0");
            progressBarLabel.setText("");
            progressBar.setProgress(0);
            decodedMessage.setText("");
            decodeImageLabel.setText("");
            encodeImageText.setText("");
            progressBar.setVisible(false);
            encodeAttachmentList.getItems().removeAll();
        }catch (Exception e){

        }
        imagePath=null;
        savePath=null;
    }
    void reset(){
        }
    @FXML
    void setFields(){

    }
    @FXML
    void changeMethod(){
       initialize();
    }
    @FXML
    void loadUrl(){
        File file = new File("img.img");
            if (file.exists()){
                file.delete();
            }
            try(InputStream in = new URL(encodeImgUrl.getText()).openStream()){

            Files.copy(in, Paths.get("img.img"));
            encodeImageText.setVisible(false);
            imagePath="img.img";
            in.close();
            loadImage();
        }catch (Exception e){
            e.printStackTrace();
            encodeImageText.setVisible(true);
            encodeImageText.setText("I'm sorry, the URL you provided is invalid");
            return;
        }

    }
    @FXML
    void executeEncode(){
        encodeImageText.setVisible(false);
        if(imagePath==null){
            encodeImageText.setVisible(true);
            encodeImageText.setText("Please load an image first.");
            return;
        }

        try {
            steganographicImage.setMessage(encodeMessageBox.getText());
        }catch (Exception e) {
            e.printStackTrace();
            encodeImageText.setVisible(true);
            encodeImageText.setText("Your message to encode is to large");
            return;
        }
        try{
            steganographicImage.saveToFile(encodeSavePath.getText());
            System.out.println("SAVED TO : "+encodeSavePath.getText());
        }catch (Exception e){
            e.printStackTrace();
            encodeImageText.setVisible(true);
            encodeImageText.setText("Your save file name is not valid");
            return;
        }
        encodeImageText.setVisible(true);
        try{File f = new File(encodeSavePath.getText());
            encodeImageText.setText("Congratulations! You succesfully encoded your message to "+f.getName()+".png");
            steganographicImage.clear();}
        catch (Exception e){

        }

    }

    @FXML
    void refreshImageStats(){
        if(imagePath!=null) {
            try {
                progressBarLabel.setVisible(true);
                encodeAvailableSize.setVisible(true);
                progressBarLabel.setVisible(true);
                progressBar.setVisible(true);
                steganographicImage.setMessage(encodeMessageBox.getText());
                Integer available=steganographicImage.getTotalStorage();
                Integer used=steganographicImage.getUsedStorage();
                encodeAvailableSize.setText(((Integer)(available-used)).toString()+"/"+available.toString());
                Double progress = (used*1.0/available);
                progressBar.setProgress(progress);
                progressBarLabel.setText("Used "+( (Double)(progress*100.0)).intValue()+"%");
            }catch (Exception e){
                encodeAvailableSize.setText("None");
                progressBarLabel.setText("To large to encode");
                progressBar.setProgress(1);
            }
        }
    }
    void loadImage(){
        javafx.scene.image.Image img=null;
        try {
            FileInputStream inputstream = new FileInputStream(imagePath);
            img = new javafx.scene.image.Image(inputstream);
            encodeImageText.setVisible(false);
            decodeImageLabel.setVisible(false);
            inputstream.close();
        }catch (Exception e){
            e.printStackTrace();
            if(method==0) {
                encodeImageText.setVisible(true);
                encodeImageText.setText("I'm sorry, we couldn't open the image");
            }else{
                decodeImageLabel.setVisible(true);
                decodeImageLabel.setText("I'm sorry, we couldn't open the image");
            }
            return;
        }
        try {
            steganographicImage = SteganographicImage.loadFromFile(imagePath);
        }catch (Exception e){
            e.printStackTrace();

            if(method==0) {
                encodeImageText.setVisible(true);
                encodeImageText.setText("Couldn't load file");
            }else{
                decodeImageLabel.setVisible(true);
                decodeImageLabel.setText("Couldn't load file");
            }
            return;
        }
        if(method==0) {
            encodeImgPreview.setImage(img);
            encodeImgPreview.fitHeightProperty();
            encodeImgPreview.fitWidthProperty();
            encodeImgPreview.setVisible(true);
        }else{

            decodeImgPreview.setVisible(true);
            decodeImgPreview.setImage(img);
            decodeImgPreview.fitHeightProperty();
            decodeImgPreview.fitWidthProperty();
        }
    }

    @FXML
    void loadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(
                // new FileChooser.ExtensionFilter("Image Files",   "*.png", "*.jpg","*"))
        );
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            imagePath = selectedFile.getAbsolutePath();
        }
        loadImage();
        if (method == 1) {
            decode();
        }
    }
    @FXML
    void saveFileSelect(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(
                // new FileChooser.ExtensionFilter("Image Files",   "*.png", "*.jpg","*"))
        );
        File selectedFile = fileChooser.showSaveDialog(new Stage());
        System.out.println(selectedFile.getAbsolutePath());
        if (selectedFile != null) {
            encodeSavePath.setText(selectedFile.getAbsolutePath());
        }
        if (method == 1) {
        }
    }


    @FXML
    void addAttachments(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(
                // new FileChooser.ExtensionFilter("Image Files",   "*.png", "*.jpg","*"))
        );
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(new Stage());
        if (selectedFiles != null) {
            encodeAttachmentList.setVisible(true);
            selectedFiles.forEach(file -> {
                encodeAttachmentList.getItems().add(file.getAbsolutePath());
            });
        } refreshAttachments();
    }


    @FXML
    void removeAttachment(){
        try {
            encodeAttachmentList.getItems().remove(encodeAttachmentList.getValue());
            encodeAttachmentList.setPromptText("list of attachments:");
        }catch (Exception e){
            e.printStackTrace();
        }
        refreshAttachments();
    }
    void refreshAttachments(){
        if(imagePath!=null) {
            try {
                steganographicImage=SteganographicImage.loadFromFile(imagePath);
                progressBarAttachmentsLabel.setVisible(true);
                encodeAvailableSizeAttachments.setVisible(true);
                progressBarAttachments.setVisible(true);
                if(!encodeAttachmentList.getItems().isEmpty()){
                    steganographicImage.clear();
                    encodeAttachmentList.getItems().forEach(o -> {
                        try {
                            steganographicImage.addAttachment((String)o);
                            System.out.println("ADDED ATTACHMENT IN REFRESH");
                            System.out.println(steganographicImage.getUsedStorage());
                        }catch (Exception e){
                            e.printStackTrace();
                            encodeAvailableSizeAttachments.setText("None");
                            progressBarAttachmentsLabel.setText("To large to encode");
                            progressBarAttachments.setProgress(1);
                        }
                    });
                }
                Integer available=steganographicImage.getTotalStorage();
                Integer used=steganographicImage.getUsedStorage();
                encodeAvailableSizeAttachments.setText(((Integer)(available-used)).toString()+"/"+available.toString()+" B");
                Double progress = (used*1.0/available);
                progressBarAttachments.setProgress(progress);
                progressBarAttachmentsLabel.setText("Used "+( (Double)(progress*100.0)).intValue()+"%");
            }catch (Exception e){
                encodeAvailableSizeAttachments.setText("None");
                progressBarAttachmentsLabel.setText("To large to encode");
                progressBarAttachments.setProgress(1);
            }
        }
    }
    @FXML
    void executeEncodeAtachments(){
        encodeImageText.setVisible(false);
        if(imagePath==null){
            encodeImageText.setVisible(true);
            encodeImageText.setText("Please load an image first.");
            return;
        }
        try {
            if(!encodeAttachmentList.getItems().isEmpty()){
                encodeAttachmentList.getItems().forEach(o -> {
                    try{steganographicImage.addAttachment((String)o);}
                    catch (Exception e){
                        e.printStackTrace();
                        encodeImageText.setVisible(true);
                        encodeImageText.setText("Couldn't add attachment "+(String)o);
                        return;
                    }
                });
            }
        }catch (Exception e) {
            e.printStackTrace();
            encodeImageText.setVisible(true);
            encodeImageText.setText("Your attachments to encode are to large");
            return;
        }
        try{
            steganographicImage.saveToFile(encodeSavePath.getText());
            System.out.println("SAVED TO : "+encodeSavePath.getText());
        }catch (Exception e){
            e.printStackTrace();
            encodeImageText.setVisible(true);
            encodeImageText.setText("Your save file name is not valid");
            return;
        }
        encodeImageText.setVisible(true);
        try{File f = new File(encodeSavePath.getText());
            encodeImageText.setText("Congratulations! You succesfully encoded your attachments to "+f.getName()+".png");
            steganographicImage.clear();}
        catch (Exception e){

        }

    }
    @FXML
    void saveAttachment(){
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory =
                directoryChooser.showDialog(new Stage());
                String[] list = steganographicImage.listAttachments();
                for (int i = 0; i < list.length; i++) {
                    try {
                        System.out.println(selectedDirectory.getAbsolutePath() + "" + list[i]);
                        steganographicImage.saveAttachment(list[i], selectedDirectory.getAbsolutePath() + "\\" + list[i]);
                    } catch (Exception e) {
                        e.printStackTrace();
                        decodeImageLabel.setVisible(true);
                        decodeImageLabel.setText("Couldn't save file " + list[i]);
                        return;
                    }
                }
    }

    void decode(){
        try {
            decodedMessage.setText(steganographicImage.getMessage());
            if(!steganographicImage.getMessage().equals("")){
                decodeMessageLabel.setVisible(true);
                decodedMessage.setVisible(true);
                decodeMessageLabel.setText("Encoded message:");
            }
            String[] attachments=steganographicImage.listAttachments();
            if(!(attachments.length==0)){
                decodedAttachmentsBox.setVisible(true);
                decodedAttachmentsBoxLabel.setVisible(true);
                decodeMessageLabel.setText("Encoded attachments:");
                for(int i=0;i<attachments.length;i++){
                    decodedAttachmentsBox.getItems().add(attachments[i]);
                }
            }
            if(attachments.length==0 && steganographicImage.getMessage().equals("")){
                decodeImageLabel.setVisible(true);
                decodeImageLabel.setText("This image doesen't have an encoded message.");
            }
        }catch (Exception e){
            e.printStackTrace();
            decodeImageLabel.setVisible(true);
            decodeImageLabel.setText("Couldn't load file");
            return;
        }
    }



}
