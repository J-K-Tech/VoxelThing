package io.bluestaggo.pds;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class StructureItem {
	private static final List<Class<? extends StructureItem>> REGISTERED_TYPES = List.of(
			ByteItem.class,
			ShortItem.class,
			IntegerItem.class,
			IntItem.class,
			LongItem.class,
			FloatItem.class,
			DoubleItem.class,
			ByteArrayItem.class,
			ShortArrayItem.class,
			IntArrayItem.class,
			LongArrayItem.class,
			StringItem.class,
			ListItem.class,
			CompoundItem.class
	);

	protected static String readString(DataInputStream stream) throws IOException {
		int length = stream.readUnsignedShort();
		byte[] bytes = stream.readNBytes(length);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	protected static void writeString(String string, DataOutputStream stream) throws IOException {
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		stream.writeShort(bytes.length);
		stream.write(bytes);
	}

	@Override
	public String toString() {
		return getString();
	}

	private String getUnsupportedMessage(String type) {
		return "\"" + getClass().getSimpleName() + "\" does not contain \"" + type + "\"";
	}

	public String getString() {
		throw new UnsupportedOperationException(getUnsupportedMessage("String"));
	}

	public byte getByte() {
		throw new UnsupportedOperationException(getUnsupportedMessage("byte"));
	}

	public short getShort() {
		throw new UnsupportedOperationException(getUnsupportedMessage("short"));
	}

	public int getInt() {
		throw new UnsupportedOperationException(getUnsupportedMessage("int"));
	}

	public Integer getInteger(){
		throw new UnsupportedOperationException(getUnsupportedMessage("Integer"));
	}

	public int getUnsignedByte() {
		return getByte() & 0xFF;
	}

	public int getUnsignedShort() {
		return getShort() & 0xFFFF;
	}

	public long getLong() {
		throw new UnsupportedOperationException(getUnsupportedMessage("long"));
	}

	public long getUnsignedInt() {
		return getInt() & 0xFFFFFFFFL;
	}

	public boolean getBoolean() {
		return getByte() != 0;
	}

	public float getFloat() {
		throw new UnsupportedOperationException(getUnsupportedMessage("float"));
	}

	public double getDouble() {
		throw new UnsupportedOperationException(getUnsupportedMessage("double"));
	}

	public byte[] getByteArray() {
		throw new UnsupportedOperationException(getUnsupportedMessage("byte[]"));
	}

	public short[] getShortArray() {
		throw new UnsupportedOperationException(getUnsupportedMessage("short[]"));
	}

	public int[] getIntArray() {
		throw new UnsupportedOperationException(getUnsupportedMessage("int[]"));
	}

	public long[] getLongArray() {
		throw new UnsupportedOperationException(getUnsupportedMessage("long[]"));
	}

	public List<StructureItem> getList() {
		throw new UnsupportedOperationException(getUnsupportedMessage("List"));
	}

	public Map<String, StructureItem> getMap() {
		throw new UnsupportedOperationException(getUnsupportedMessage("Map"));
	}

	public int getType() {
		return REGISTERED_TYPES.indexOf(getClass()) + 1;
	}

	protected abstract void read(DataInputStream stream) throws IOException;

	protected abstract void write(DataOutputStream stream) throws IOException;

	public static StructureItem readItem(DataInputStream stream) throws IOException {
		int type = stream.readUnsignedByte();

		if (type == 0 || type > REGISTERED_TYPES.size()) {
			return null;
		}

		try {
			StructureItem item = REGISTERED_TYPES.get(type - 1).getDeclaredConstructor().newInstance();
			item.read(stream);
			return item;
		} catch (NoSuchMethodException
				| InstantiationException
				| IllegalAccessException
				| IllegalArgumentException
				| java.lang.reflect.InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void writeItem(DataOutputStream stream) throws IOException {
		stream.write(getType());
		write(stream);
	}

	public static StructureItem readItemFromPath(Path path, boolean compressed) throws IOException {
		try (InputStream istream = Files.newInputStream(path)) {
			try (InputStream cistream = compressed ? new GZIPInputStream(istream) : istream) {
				var distream = new DataInputStream(cistream);
				return readItem(distream);
			}
		}
	}

	public void writeItemToPath(Path path, boolean compressed) throws IOException {
		try (OutputStream ostream = Files.newOutputStream(path)) {
			try (OutputStream costream = compressed ? new GZIPOutputStream(ostream) : ostream) {
				var dostream = new DataOutputStream(costream);
				writeItem(dostream);
			}
		}
	}
}
