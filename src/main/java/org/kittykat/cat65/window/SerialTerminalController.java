package org.kittykat.cat65.window;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import org.kittykat.cat65.core.MainManager;
import org.kittykat.cat65.core.ConfigManager;
import org.kittykat.cat65.settings.NewLineVariant;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SerialTerminalController extends WindowController {
    @FXML
    private TextArea terminal;

    private final ConcurrentLinkedQueue<Character> inputQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Character> printQueue = new ConcurrentLinkedQueue<>();

    public SerialTerminalController() {
        super("Cat65 Serial Terminal", "serial-terminal");
        window.getStage().setMaximized(true);
    }

    @FXML
    private void onKeyTyped(KeyEvent keyEvent) {
        for (char chr : keyEvent.getCharacter().toCharArray()) {
            handleTypedChar(chr);
        }
        keyEvent.consume();
    }

    private void handleTypedChar(char c) {
        if (c == 0x0d) {
            bufferChar(switch (ConfigManager.terminalNewLineVariant.get()) {
                case CR, CRLF -> '\r';
                case LF       -> '\n';
            });
            if (ConfigManager.terminalNewLineVariant.get() == NewLineVariant.CRLF) {
                bufferChar('\n');
            }
        } else {
            bufferChar(c);
        }
    }
    private void bufferChar(char c) {
        inputQueue.add(c);
    }

    public boolean isBufferEmpty() {
        return inputQueue.isEmpty();
    }
    @SuppressWarnings("DataFlowIssue")
    public char pollChar() {
        return inputQueue.poll();
    }

    public void print(char chr) {
        printQueue.add(chr);
    }
    @Override
    public void updateWindow() {
        while (!printQueue.isEmpty()) {
            char c = printQueue.poll();

            if ((c == '\r') || (c == '\n')) {
                if (ConfigManager.terminalNewLineVariant.get() == NewLineVariant.CRLF) {
                    terminal.appendText(String.valueOf(c));
                } else {
                    terminal.appendText("\r\n");
                }
            } else if (c == '\f') {
                terminal.clear();
            } else if ((c == 0x08) || (c == 0x7f)) {
                int len = terminal.getLength();
                if (len > 0) {
                    terminal.deleteText(len - 1, len);
                }
            } else {
                terminal.appendText(String.valueOf(c));
            }
        }
    }

    @FXML
    public void clear() {
        terminal.clear();
    }

    @FXML
    private void openSerialConfig() {
        MainManager.mainWindowController.openWindow(new ConfigWindowController("serial"));
    }
}
