package svengo.dev.deserialize2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import com.google.gson.GsonBuilder;

import svengo.dev.deserialize2.Deserializer.DeserializationException;

public class Test implements Serializable {
	private static final long serialVersionUID = 0x1122334455667788L;
	private String s;
	public static void main(String[] args) throws IOException {
		final Test instance = new Test();
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				 ObjectOutputStream oos = new ObjectOutputStream(baos)
		) {
			oos.writeObject(instance);
			byte[] bytes = baos.toByteArray();
			System.out.println(Conversions.toHex(bytes));
			Deserialized deserialized;
			try {
				deserialized = new Deserializer(ByteBuffer.wrap(bytes)).deserialize();
			} catch (DeserializationException de) {
				de.printStackTrace();
				deserialized = de.preException;
			}
			System.out.println(new GsonBuilder()
					/*.registerTypeAdapter(ClassDescInfo.class, new TypeAdapter<ClassDescInfo>() {
				@Override
				public ClassDescInfo read(JsonReader reader) throws IOException {
					// TODO Auto-generated method stub
					return null;
				}
				@Override
				public void write(JsonWriter writer, ClassDescInfo cdi) throws IOException {
					List<String> classDescFlags = new ArrayList<>(5);
					if ((cdi.classDescFlags & 0x10) != 0) classDescFlags.add("SC_ENUM");
					if ((cdi.classDescFlags & 0x08) != 0) classDescFlags.add("SC_BLOCK_DATA");
					if ((cdi.classDescFlags & 0x04) != 0) classDescFlags.add("SC_EXTERNALIZABLE");
					if ((cdi.classDescFlags & 0x02) != 0) classDescFlags.add("SC_SERIALIZABLE");
					if ((cdi.classDescFlags & 0x01) != 0) classDescFlags.add("SC_WRITE_METHOD");
					writer.value(String.join(" | ", classDescFlags));
				}
			})*/.setPrettyPrinting().create().toJson(deserialized));
		}
	}
}
class SO implements Serializable {
	private static final long serialVersionUID = 0x10101010FFFFFFEEL;}
