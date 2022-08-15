import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import svengo.dev.deserialize2.Conversions;

public class CustomSerializationTesting implements Serializable {
	public long l = 0xCCCCCCCC;
	private static final byte[] BYTES = {0x00, 0x00, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00};
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.write(BYTES);
	}
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		System.out.println("lol");
		/*
		byte[] bytes = new byte[BYTES.length];
		in.read(bytes);
		*/
		System.out.println("hex: " + Conversions.toHex(in.readNBytes(6)));
		/*
		for (int i = 0; i < bytes.length; ++i) {
			if (bytes[i] != BYTES[i]) System.out.println("rip");
		}
		*/
	}
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// final CustomSerializationTesting instance = new CustomSerializationTesting();
		final SO[] instance = new SO[1]; instance[0] = new SO();
		byte[] bytes;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				 ObjectOutputStream oos = new ObjectOutputStream(baos)
		) {
			oos.writeObject(instance);
			bytes = baos.toByteArray();
		}
		System.out.println(Conversions.toHex(bytes));
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				 ObjectInputStream ois = new ObjectInputStream(bais)
		) {
			ois.readObject();
		}
	}
}
class SO implements Serializable {
	private static final long serialVersionUID = 0x0102030405060708L;}
