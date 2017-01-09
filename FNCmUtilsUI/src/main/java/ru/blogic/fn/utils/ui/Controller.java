package ru.blogic.fn.utils.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import ru.blogic.fn.runner.Runner;

import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by pkupershteyn on 06.01.2017.
 */
public class Controller implements Initializable {
    @FXML
    private TextArea outputTextArea;

    @FXML
    private TextArea inputTextArea;


    public void initialize(URL location, ResourceBundle resources) {
        outputTextArea.textProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                outputTextArea.positionCaret(outputTextArea.getLength());
                outputTextArea.deselect();
            }
        });
    }

    public void onRunBtnPressed() {
        Runner runner = new Runner();

        runner.setOut(new PrintWriter(new TextAreaWriter(outputTextArea)));
        runner.setErr(new PrintWriter(new TextAreaWriter(outputTextArea)));
        List<String> params = new ArrayList<String>();
        for (CharSequence sequence : inputTextArea.getParagraphs()) {
            Collections.addAll(params, sequence.toString().split(" "));
        }
        runner.run(params.toArray(new String[params.size()]));
    }

    public void onClrBtnPressed() {
        outputTextArea.setText("");
        outputTextArea.positionCaret(0);
    }


}