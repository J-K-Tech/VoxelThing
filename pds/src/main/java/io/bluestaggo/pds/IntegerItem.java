package io.bluestaggo.pds;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class IntegerItem extends StructureItem{
    
    public Integer value;
    public IntegerItem() {
    }

    public IntegerItem(int value) {
        this((Integer) value);
    }

    public IntegerItem(Integer value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntegerItem item = (IntegerItem) o;
        return value == item.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String getString() {
        return String.valueOf(value);
    }

    @Override
    public byte getByte() {
        return value.byteValue();
    }

    @Override
    public short getShort() {
        return value.shortValue();
    }

    @Override
    public int getInt() {
        return value;
    }

    @Override
    public long getLong() {
        return value;
    }

    @Override
    public float getFloat() {
        return value;
    }

    @Override
    public double getDouble() {
        return value;
    }

    @Override
    protected void read(DataInputStream stream) throws IOException {
        value = stream.readInt();
    }

    @Override
    protected void write(DataOutputStream stream) throws IOException {
        stream.writeShort(value);
    }
}
