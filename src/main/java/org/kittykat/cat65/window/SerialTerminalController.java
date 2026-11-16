package org.kittykat.cat65.window;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import org.kittykat.cat65.core.MainManager;
import org.kittykat.cat65.core.ConfigManager;
import org.kittykat.cat65.settings.NewLineVariant;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SerialTerminalController extends WindowController {
    private static final int MAX_PROCESS_COUNT = 512;
    private static final int MAX_TEXT_LENGTH = 16_384;
    private static final StringBuilder sb = new StringBuilder();

    @FXML
    private TextArea terminal;

    private final ConcurrentLinkedQueue<Character> inputQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Character> printQueue = new ConcurrentLinkedQueue<>();

    public SerialTerminalController() {
        super("Cat65 Serial Terminal", "serial-terminal");
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
        boolean scollToBottom = false;
        for (int i = 0; i < MAX_PROCESS_COUNT; i++) {
            if (printQueue.isEmpty()) break;
            char c = printQueue.poll();
            scollToBottom = true;

            if ((c == '\r') || (c == '\n')) {
                if (ConfigManager.terminalNewLineVariant.get() == NewLineVariant.CRLF) {
                    sb.append(c);
                } else {
                    sb.append("\r\n");
                }
            } else if (c == '\f') {
                clear();
            } else if ((c == 0x08) || (c == 0x7f)) {
                int len = terminal.getLength();
                if (len > 0) {
                    appendStringBuffer();
                    terminal.deleteText(len - 1, len);
                }
            } else {
                sb.append(c);
            }
        }
        appendStringBuffer();
        checkTextOverflow();
        if (scollToBottom) {
            terminal.positionCaret(terminal.getLength());
        }
    }
    private void appendStringBuffer() {
        if (!sb.isEmpty()) {
            terminal.appendText(sb.toString());
            sb.setLength(0);
        }
    }
    private void checkTextOverflow() {
        int removeCount = terminal.getLength() - MAX_TEXT_LENGTH;
        if (removeCount > 0) {
            terminal.deleteText(0, removeCount);
        }
    }

    @FXML
    public void clear() {
        sb.setLength(0);
        terminal.clear();
    }

    @FXML
    private void openSerialConfig() {
        MainManager.mainWindowController.openWindow(new ConfigWindowController("serial"));
    }
}
