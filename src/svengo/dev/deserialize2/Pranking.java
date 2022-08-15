package svengo.dev.deserialize2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Pranking implements Serializable {
	private static final long serialVersionUID = 0x1122334455667788L;
	/*
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject(); // This is fucking required!
		out.write('l');
	}
	*/
	private String s;
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		final Pranking instance = new Pranking();
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				 ObjectOutputStream oos = new ObjectOutputStream(baos)
		) {
			oos.writeObject(instance);
			byte[] bytes = baos.toByteArray();
			System.out.println(Conversions.toHex(bytes));
			try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
					 ObjectInputStream ois = new ObjectInputStream(bais)
			) {
				ois.readObject();
			}
		}
	}
}
