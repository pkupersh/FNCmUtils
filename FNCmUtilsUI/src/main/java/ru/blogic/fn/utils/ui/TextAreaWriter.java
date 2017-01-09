package ru.blogic.fn.utils.ui;

import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.Writer;

/**
 * Created by pkupershteyn on 06.01.2017.
 */
public class TextAreaWriter extends Writer {
    private final TextArea textArea;
    public TextAreaWriter(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        textArea.appendText(new String(cbuf, off, len));
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
}
