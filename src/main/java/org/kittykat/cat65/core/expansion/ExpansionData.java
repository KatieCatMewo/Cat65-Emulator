package org.kittykat.cat65.core.expansion;

import org.kittykat.cat65.EmuHelper;

public abstract class ExpansionData {
    public static final int MAX_TILESETS = 8;
    public static final int TILESET_SIZE = 0x2000;
    public static final byte[] tilesetROM = new byte[TILESET_SIZE * MAX_TILESETS];

    public static final int COLORMAP_COUNT = 8;
    public static final int COLORMAP_SIZE  = 0x100;
    public static final int COLOR_ROM_SIZE = COLORMAP_SIZE * COLORMAP_COUNT;
    public static final byte[] colorR = new byte[COLOR_ROM_SIZE];
    public static final byte[] colorG = new byte[COLOR_ROM_SIZE];
    public static final byte[] colorB = new byte[COLOR_ROM_SIZE];

    public static void init() {
        loadColormap("red",   colorR);
        loadColormap("green", colorG);
        loadColormap("blue",  colorB);
    }
    @SuppressWarnings("CallToPrintStackTrace")
    private static void loadColormap(String file, byte[] array) {
        try {
            byte[] data = EmuHelper.readResourceFile("colormap/%s.rom".formatted(file));
            System.arraycopy(data, 0, array, 0, Math.min(data.length, array.length));
        } catch (Exception e) {
            System.err.printf("[!] Could not load %s colormap ROM!\n", file);
            e.printStackTrace();
        }
    }

    public static int loadC65(byte[] file, int index) {
        if (index < file.length) {
            System.out.println("[*] loading tilesets...");

            int max = file.length - index;
            int tilesetCount = file[0x6] & 0xff;

            if (tilesetCount > MAX_TILESETS) {
                System.err.printf("[!] The maximum tileset count allowed is %d, got %d\n", MAX_TILESETS, tilesetCount);
                tilesetCount = MAX_TILESETS;
            }
            int tilesetSize = tilesetCount * TILESET_SIZE;
            if (tilesetSize > max) {
                System.err.printf("[!] Cannot load %d bytes when only %d are available!\n", tilesetSize, max);
                tilesetSize = max;
            }

            for (int t = 0; t < tilesetSize; t++) {
                tilesetROM[t] = file[index++];
            }
        }
        return index;
    }

    public static int getColor(int address) {
        return 0xff_000000 | (readRed(address) << 16) | (readGreen(address) << 8) | readBlue(address);
    }
    public static int readRed(int address) {
        return colorR[address] & 0xff;
    }
    public static int readGreen(int address) {
        return colorG[address] & 0xff;
    }
    public static int readBlue(int address) {
        return colorB[address] & 0xff;
    }
}
