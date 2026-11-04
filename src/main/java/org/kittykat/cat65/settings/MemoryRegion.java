package org.kittykat.cat65.settings;

import javafx.util.StringConverter;
import org.kittykat.cat65.core.expansion.ExpansionData;

import java.util.function.Function;

import static org.kittykat.cat65.core.MainManager.get;

public enum MemoryRegion {
    CPU_Memory     (0x1000, a -> get(a, false)),
    PRG_ROM        (0x800,  a -> get(a + 0x8000, false)),
    System_RAM     (0x400,  a -> get(a,          false)),
    Expansion_4    (0x100,  a -> get(a + 0x4000, false)),
    Expansion_7    (0x100,  a -> get(a + 0x7000, false)),
    Exp_Tileset_ROM(0x200, ExpansionData::readTilesetByte);

    public final int lineCount;
    private final Function<Integer, Integer> getter;

    MemoryRegion(int lineCount, Function<Integer, Integer> getter) {
        this.lineCount = lineCount;
        this.getter = getter;
    }

    public int read(int address) {
        return getter.apply(address);
    }

    public String getName() {
        return stringConverter.toString(this);
    }

    public static final StringConverter<MemoryRegion> stringConverter = new StringConverter<>() {
        @Override
        public String toString(MemoryRegion object) {
            return object.toString().replaceAll("_", " ");
        }
        @Override
        public MemoryRegion fromString(String string) {
            return MemoryRegion.valueOf(string.strip().replaceAll(" ", "_"));
        }
    };
}
