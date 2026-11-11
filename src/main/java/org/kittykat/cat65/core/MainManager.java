package org.kittykat.cat65.core;

import javafx.stage.Stage;
import org.kittykat.cat65.core.cpu.Bus;
import org.kittykat.cat65.core.cpu.CPU;
import org.kittykat.cat65.core.expansion.ExpansionData;
import org.kittykat.cat65.core.expansion.ExpansionManager;
import org.kittykat.cat65.core.extraChips.ACIA;
import org.kittykat.cat65.core.extraChips.LCD;
import org.kittykat.cat65.core.extraChips.VIA;
import org.kittykat.cat65.settings.ExpansionPort;
import org.kittykat.cat65.window.MainWindowController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;

public abstract class MainManager {
    private static final int MAX_FILE_SIZE = 0x4_000;  // 256 KiB
    private static final int HEADER_SIZE = 16;
    private static final char[] ID_CHARS = {'c', '6', '5', 0x15};

    private static final Bus bus = new Bus() {
        @Override
        public int read(int address) {
            return MainManager.get(address, true);
        }
        @Override
        public void write(int address, int value) {
            MainManager.set(address, value, true);
        }

        @Override
        public boolean getIRQ() {
            return MainManager.getIRQ();
        }
        @Override
        public boolean getNMI() {
            return MainManager.getNMI();
        }
    };

    private static final int ROM_SIZE = 0x8000;
    private static final byte[] rom = new byte[ROM_SIZE];
    private static final byte[] ram = new byte[0x4000];

    private static final ACIA acia = new ACIA();
    private static final VIA  via  = new VIA();
    private static final LCD  lcd  = new LCD();
    private static final CPU  cpu  = new CPU(bus);

    public static MainWindowController mainWindowController;

    private static long timeSinceLastClock = System.nanoTime();

    public static volatile boolean running = true;
    public static volatile boolean paused  = true;
    public static volatile boolean step    = false;
    public static volatile boolean reset   = true;

    public static CPU getCpu() {
        return cpu;
    }
    public static LCD getLcd() {
        return lcd;
    }

    public static void stop() {
        running = false;
        System.exit(0);
    }

    @SuppressWarnings("NonAtomicOperationOnVolatileField")
    public static void startClockThread() {
        Thread clockThread = new Thread(() -> {
            long lastTime = System.nanoTime();
            long timeAccumulator = 0L;
            long currentTime;
            long sleepTime;
            int clockPeriod;

            while (running) {
                currentTime = System.nanoTime();
                timeSinceLastClock = currentTime - lastTime;
                lastTime = currentTime;

                timeAccumulator += timeSinceLastClock;

                while (timeAccumulator >= ConfigManager.clockPeriod) {
                    if (!paused || step) {
                        if (reset) {
                            reset();
                            reset = false;
                        } else {
                            clock();
                        }
                        DebugData.executedClocks++;
                    }
                    AudioManager.clockAudio();
                    timeAccumulator -= ConfigManager.clockPeriod;
                }

                sleepTime = ConfigManager.clockPeriod - timeAccumulator;
                if (sleepTime > 0L) {
                    LockSupport.parkNanos(sleepTime);
                }
            }
        }, "Cat65 Clock");
        clockThread.setDaemon(true);
        clockThread.start();
    }

    public static void startUi(Stage stage) {
        mainWindowController = new MainWindowController(stage);
    }

    public static void clock() {
        via.clock();
        acia.clock();
        ExpansionManager.clock();
        cpu.clock();
        if (cpu.isAtInstructionBoundary()) step = false;
    }
    public static void reset() {
        via.reset();
        acia.reset();
        ExpansionManager.reset();
        cpu.reset();
        step = false;
    }

    public static int getPA(int pinState) {
        // ToDo: get PortA
        return 0x00;
    }
    public static void setPA(int pinState) {
        // ToDo: set PortA
    }
    /**
     * bits 0-3: Data
     **/
    public static int getPB(int pinState) {
        return lcd.get(pinState);
    }
    /**
     * bits 0-3: Data<br>
     * bit   4:  RS<br>
     * bit   5:  R/W<br>
     * bit   6:  E
     **/
    public static void setPB(int pinState) {
        lcd.set(pinState);
    }

    public static boolean getIRQ() {
        return via.getIRQ() && acia.getIRQ() && ExpansionManager.getIRQ();
    }
    public static boolean getNMI() {
        return ExpansionManager.getNMI();
    }

    public static int get(int address, boolean internal) {
        address &= 0xffff;
        int select = address >> 12;
        return 0xff & (switch (select) {
            case 0x0,0x1,0x2,0x3 -> ram[address];
            case 0x4,0x7         -> ExpansionManager.get(address, internal);
            case 0x5             -> acia.read(address - 0x5000, internal);
            case 0x6             -> via.read(address - 0x6000, internal);
            default              -> rom[address - 0x8000];
        });
    }
    public static void set(int address, int value, boolean internal) {
        address &= 0xffff;
        value &= 0xff;
        int select = address >> 12;
        switch (select) {
            case 0x0,0x1,0x2,0x3 -> ram[address] = (byte) value;
            case 0x4,0x7         -> ExpansionManager.set(address, value, internal);
            case 0x5             -> acia.write(address - 0x5000, value, internal);
            case 0x6             -> via.write(address - 0x6000, value, internal);
            default -> {
                 if (!internal) rom[address - 0x8000] = (byte) value;
            }
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void loadFile(File file) {
        boolean p = paused;
        paused = true;

        Arrays.fill(rom, (byte) 0x00);
        ExpansionData.resetData();

        try {
            System.out.println();

            Path path = file.toPath();
            if (Files.size(path) > MAX_FILE_SIZE) {
                System.err.printf("[!] Selected file is larger than the maximum file size of %d bytes!\n", MAX_FILE_SIZE);
            }
            byte[] data = Files.readAllBytes(path);

            boolean c65 = true;
            for (int i = 0; i < 4; i++) {
                if (data[i] != ID_CHARS[i]) {
                    c65 = false;
                    break;
                }
            }
            if (c65) {
                loadC65(data);
            } else {
                loadRaw(data);
            }
        } catch (Exception e) {
            System.err.println("[!] Could not open file...");
            e.printStackTrace();
        } finally {
            reset = true;
            paused = p;
        }
    }
    private static void loadRaw(byte[] file) {
        System.out.println("[#] loading ROM");
        if (file.length < ROM_SIZE) {
            System.err.println("[!] Warning! this ROM has a file size below 32KiB!");
        } else if (file.length > ROM_SIZE) {
            System.err.printf("[!] Warning! This ROM has %d unused bytes left over\n", file.length - ROM_SIZE);
        }

        for (int i = 0; i < rom.length; i++) {
            rom[i] = (i < file.length) ? file[i] : 0x00;
        }
    }
    private static void loadC65(byte[] file) throws IOException {
        System.out.println("[#] loading .c65 file");
        if (file.length < HEADER_SIZE) {
            throw new IOException("A .c65 ROM needs to be at least %d bytes!".formatted(HEADER_SIZE));
        }
        // 0x7 to 0xf are reserved for future use

        System.out.println("[*] loading Expansion...");
        ExpansionManager.ex04Property.setValue(ExpansionPort.fromID(file[0x4]));
        ExpansionManager.ex07Property.setValue(ExpansionPort.fromID(file[0x5]));

        System.out.println("[*] loading ROM...");
        if (file.length < (ROM_SIZE + HEADER_SIZE)) {
            System.err.println("[!] Warning! this .c65 file has a ROM size below 32KiB!");
        }

        int index = HEADER_SIZE;
        for (int a = 0; a < rom.length; a++) {
            rom[a] = (index < file.length) ? file[index] : 0x00;
            index++;
        }
        index = ExpansionData.loadC65(file, index);

        if (index < file.length) {
            System.err.printf("[!] Warning! This file has %d unused bytes left over\n", file.length - index);
        }
        reset = true;
    }
}
