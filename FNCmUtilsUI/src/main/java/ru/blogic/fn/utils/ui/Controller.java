package ru.blogic.fn.utils.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by pkupershteyn on 06.01.2017.
 */
public class Controller implements Initializable, RunnerService.Events {
    @FXML
    private TextArea outputTextArea;

    @FXML
    private TextArea inputTextArea;

    @FXML
    private ImageView progressImage;

    @FXML
    private Button runBtn;

    @FXML
    private Button stopBtn;

    @FXML
    private Button clrBtn;
/*
    @FXML
    private Pane fnCmUtilsRootPane;
*/

    private final static KeyCodeCombination INTERRUPT_KEY_COMBINATION = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);

    private final static KeyCodeCombination RUN_KEY_COMBINATION = new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN);


    private final static KeyCodeCombination CLEAR_CONSOLE_KEY_COMBINATION = new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN);

    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("controller init start");
        outputTextArea.textProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                outputTextArea.positionCaret(outputTextArea.getLength());
                outputTextArea.deselect();
            }
        });


    /*    fnCmUtilsRootPane.setOnKeyPressed(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent event) {
                System.out.println("KEY EVT: controlDown: "+event.isShortcutDown()+"; code: "+event.getCode());
                for(KeyCodeCombination comb: INTERRUPT_KEY_COMBINATIONS){
                    if(comb.match(event)){
                        //cancel
                        System.out.println("interrupting...");
                        RunnerService.cancelAll();
                        event.consume();
                    }
                }
            }
        });*/
        RunnerService.setEvents(this);
        System.out.println("controller init end");
    }

    public void initAfterStage() {
        setKeyCombination(runBtn, RUN_KEY_COMBINATION);
        setKeyCombination(stopBtn, INTERRUPT_KEY_COMBINATION);
        setKeyCombination(clrBtn, CLEAR_CONSOLE_KEY_COMBINATION);
    }

    public void onStopBtnPressed() {
        stop();
    }

    public void onRunBtnPressed() {
        run();
    }

    public void onClrBtnPressed() {
        outputTextArea.setText("");
        outputTextArea.positionCaret(0);
    }

    public void onBtn() {
        progressImage.setVisible(true);
        System.out.println("image set to visible");
    }


    private void run() {

        List<String> params = new ArrayList<String>();
        for (CharSequence sequence : inputTextArea.getParagraphs()) {
            //Collections.addAll(params, sequence.toString().split(" "));
            params.add(sequence.toString());
        }

        RunnerService.start(params, new PrintWriter(new TextAreaWriter(outputTextArea)), new PrintWriter(new TextAreaWriter(outputTextArea)));
    }

    private void stop() {
        RunnerService.cancelAll();
    }

    public void beforeRunnerStart() {
        runBtn.setDisable(true);
        stopBtn.setDisable(false);
        progressImage.setVisible(true);

    }

    public void afterRunnerStop() {
        progressImage.setVisible(false);
        runBtn.setDisable(false);
        stopBtn.setDisable(true);
    }

    private void setKeyCombination(final Button button, KeyCodeCombination keyCodeCombination) {
        button.setTooltip(new Tooltip(keyCodeCombination.getDisplayText()));
        button.getScene().getAccelerators().put(keyCodeCombination, new Runnable() {
            public void run() {
                button.fire();
            }
        });
    }
}