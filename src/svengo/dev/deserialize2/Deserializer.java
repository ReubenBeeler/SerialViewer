package svengo.dev.deserialize2;

import static java.io.ObjectStreamConstants.SC_BLOCK_DATA;
import static java.io.ObjectStreamConstants.SC_EXTERNALIZABLE;
import static java.io.ObjectStreamConstants.SC_SERIALIZABLE;
import static java.io.ObjectStreamConstants.SC_WRITE_METHOD;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamConstants;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import svengo.dev.deserialize2.Deserialized.Content;
import svengo.dev.deserialize2.Deserialized.Content.BlockData;
import svengo.dev.deserialize2.Deserialized.Content.Object;
import svengo.dev.deserialize2.Deserialized.Content.Object.ClassDesc;
import svengo.dev.deserialize2.Deserialized.Content.Object.Exception;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewArray;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewClass;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewClassDesc;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewClassDesc.ClassAnnotation;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewClassDesc.ClassDescInfo;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewClassDesc.ClassDescInfo.FieldDesc;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewClassDesc.ClassDescInfo.FieldDesc.ObjectDesc;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewClassDesc.ClassDescInfo.FieldDesc.PrimitiveDesc;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewClassDesc.ProxyClassDescInfo;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewEnum;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewObject;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewObject.ClassDatum;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewObject.ClassDatum.Field;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewObject.ExternalContents;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewObject.ObjectAnnotation;
import svengo.dev.deserialize2.Deserialized.Content.Object.NewString;
import svengo.dev.deserialize2.Deserialized.Content.Object.PrevObject;

public class Deserializer {
	public static void main(String[] args) {
		ObjectInputStream ois;
		System.out.println(new Gson().toJson(new byte[] {0, 1, 2, (byte) 0xFF}));
	}
	public static class Boof {}
	private static final List<Character> PRIM_TYPECODES = List.of('B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z');
	private static final List<Character> OBJ_TYPECODES = List.of('[', 'L');

	private final ByteBuffer buf;
	public Deserializer(ByteBuffer buf) {
		this.buf = buf;
	}
	public class DeserializationException extends java.lang.Exception {
		private static final long serialVersionUID = -2759610509448663423L;
		
		public Deserialized preException;
		private DeserializationException(String expectedType, java.lang.Object received) {
			super("position: " + buf.position() + ", expected: " + expectedType + ", received: " + (received instanceof Number ? "0x" + Long.toHexString(((Number) received).longValue()) : received));
		}
		private DeserializationException(String message) {
			super(message);
		}
		private DeserializationException(Throwable cause) {
			super(cause);
		}
	}
	private Map<Integer, java.lang.Object> handleMap = new HashMap<>();
	private int nextHandle = ObjectStreamConstants.baseWireHandle;
	private String readModifiedUTF8() throws DeserializationException {
		short length = buf.getShort();
		if (length > buf.remaining()) throw new DeserializationException("length longer than rest of buffer", length);
		byte[] bytes = new byte[2 + length];
		bytes[0] = (byte) (length >> 8);
		bytes[1] = (byte) length;
		buf.get(bytes, 2, length);
		try {
			return new DataInputStream(new ByteArrayInputStream(bytes)).readUTF();
		} catch (IOException ioe) {throw new DeserializationException(ioe);}
	}
	private String readModifiedLongUTF8() throws DeserializationException {
		long length = buf.getLong();
		if (length < 0 || Integer.MAX_VALUE < length) throw new DeserializationException("utf8 of length <= Integer.MAX_VALUE", length);
		if (length > buf.array().length - buf.position()) throw new DeserializationException("length longer than rest of buffer", length);
		byte[] bytes = new byte[(int) length];
		try {
			return new DataInputStream(new ByteArrayInputStream(bytes)).readUTF();
		} catch (IOException ioe) {throw new DeserializationException(ioe);}
	}
	
	private static interface ConsumerThrows<T, U extends Throwable> {
		void accept(T t) throws U;
	}
	private static interface SupplierThrows<T, U extends Throwable> {
		T get() throws U;
	}
	
	public Deserialized deserialize() throws DeserializationException {
		Deserialized d = new Deserialized();
		d.magic = buf.getShort();
		if (d.magic != ObjectStreamConstants.STREAM_MAGIC) throw new DeserializationException("magic", d.magic);
		d.version = buf.getShort();
		if (d.version != ObjectStreamConstants.STREAM_VERSION) throw new DeserializationException("version", d.version);
		List<Content> contents = new ArrayList<>();
		DeserializationException exception = null;
		try {
			while (buf.hasRemaining()) {
				contents.add(content(buf.get()));
			}
		} catch (DeserializationException de) {
			(exception = de).preException = d;
		}
		d.contents = contents.toArray(Content[]::new);
		if (exception != null) throw exception;
		return d;
	}
	private Content content(byte tc) throws DeserializationException {
		Content content = new Content();
		switch (tc) {
			case ObjectStreamConstants.TC_OBJECT:
				content.object = new Object();
				content.object.newObject = newObject();
				break;
			case ObjectStreamConstants.TC_CLASS:
				content.object = new Object();
				content.object.newClass = newClass();
				break;
			case ObjectStreamConstants.TC_ARRAY:
				content.object = new Object();
				content.object.newArray = newArray();
				break;
			case ObjectStreamConstants.TC_STRING:
				content.object = new Object();
				content.object.newString = newString();
				break;
			case ObjectStreamConstants.TC_LONGSTRING:
				content.object = new Object();
				content.object.newString = newLongString();
				break;
			case ObjectStreamConstants.TC_ENUM:
				content.object = new Object();
				content.object.newEnum = newEnum();
				break;
			case ObjectStreamConstants.TC_CLASSDESC:
				content.object = new Object();
				content.object.newClassDesc = newClassDesc();
				break;
			case ObjectStreamConstants.TC_PROXYCLASSDESC:
				content.object = new Object();
				content.object.newClassDesc = newProxyClassDesc();
				break;
			case ObjectStreamConstants.TC_REFERENCE:
				content.object = new Object();
				content.object.prevObject = prevObject();
				break;
			case ObjectStreamConstants.TC_NULL:
				content.object = new Object();
				content.object.nullReference = tc;
				break;
			case ObjectStreamConstants.TC_EXCEPTION:
				content.object = new Object();
				content.object.exception = exception();
				break;
			case ObjectStreamConstants.TC_RESET:
				content.object = new Object();
				content.object.TC_RESET = tc;
				break;
			case ObjectStreamConstants.TC_BLOCKDATA:
				content.blockData = blockDataShort();
				break;
			case ObjectStreamConstants.TC_BLOCKDATALONG:
				content.blockData = blockDataLong();
				break;
			default: throw new DeserializationException("content tc type", tc);
		}
		return content;
	}
	private NewObject newObject() throws DeserializationException {
		NewObject newObject = new NewObject();
		newObject.classDesc = classDesc();
		newObject.handle = nextHandle++;
		handleMap.put(newObject.handle, newObject);
		newObject.classData = classData(asNonProxy(newObject.classDesc));
		return newObject;
	}

	private NewClassDesc asNonProxy(ClassDesc classDesc) throws DeserializationException {
		if (classDesc.nullReference != null) throw new DeserializationException("no info on nullReference classDesc", classDesc);
		if (classDesc.newClassDesc != null) {
			if (classDesc.newClassDesc.classDescInfo != null) return classDesc.newClassDesc;
			// TODO this is a proxyClassDesc, just throw exception for now
			throw new DeserializationException("proxy classes are currently unsupported");
		}
		java.lang.Object o = handleMap.get(classDesc.prevObject.handle);
		if (o instanceof ClassDesc) return asNonProxy((ClassDesc) o);
		throw new DeserializationException("prevObject handle needs to point to ClassDesc", classDesc.prevObject.handle);
	}
	private ClassDatum[] classData(NewClassDesc nonProxy) throws DeserializationException {
		ClassDatum classDatum = new ClassDatum();
		final byte flags = nonProxy.classDescInfo.classDescFlags;
		final boolean customWriteMethod = (SC_WRITE_METHOD & flags) != 0;
		final boolean serializable = (SC_SERIALIZABLE & flags) != 0;
		final boolean externalizable = (SC_EXTERNALIZABLE & flags) != 0;
		final boolean blockData = (SC_BLOCK_DATA & flags) != 0;
		if (serializable && externalizable) throw new DeserializationException("cannot have both serializable and externalizable flags");
		if (serializable) {
			if (customWriteMethod) {
				// wrclass objectAnnotation
				classDatum.wrclass = fields(nonProxy);
				classDatum.objectAnnotation = objectAnnotation();
			} else {
				// nowrclass
				classDatum.nowrclass = fields(nonProxy);
			}
		} else if (externalizable) {
			if (blockData) {
				// objectAnnotation
				classDatum.objectAnnotation = objectAnnotation();
			} else {
				// externalContents
				classDatum.externalContents = externalContents();
			}
		}
		return null;
	}
	private Field[] fields(NewClassDesc nonProxy) throws DeserializationException {
		Field[] fields = new Field[nonProxy.classDescInfo.fieldDescs.length];
		for (int i = 0; i < fields.length; ++i) {
			FieldDesc fieldDesc = nonProxy.classDescInfo.fieldDescs[i];
			Field field = (fields[i] = new Field());
			if (fieldDesc.primitiveDesc != null) {
				field.name = fieldDesc.primitiveDesc.fieldName;
				switch (fieldDesc.primitiveDesc.typecode) {
					case 'B': field.value = buf.get(); break;
					case 'S': field.value = buf.getShort(); break;
					case 'I': field.value = buf.getInt(); break;
					case 'J': field.value = buf.getLong(); break;
					case 'F': field.value = buf.getFloat(); break;
					case 'D': field.value = buf.getDouble(); break;
					case 'C': field.value = buf.getChar(); break;
					case 'Z': 
						byte b = buf.get();
						if (b == 0x00) field.value = false;
						else if (b == 0x01) field.value = true;
						throw new DeserializationException("boolean as byte", b);
				}
			} else {
				field.name = fieldDesc.objectDesc.fieldName;
				String expectedClassName = fieldDesc.objectDesc.className.value;
				switch (fieldDesc.objectDesc.typecode) {
					case '[':
						field.value = newArray();
						String arrayClassName = asNonProxy(((NewArray) field.value).classDesc).className;
						if (!arrayClassName.equals(expectedClassName)) {
							// TODO format is slightly different
							throw new DeserializationException(arrayClassName + " == " + expectedClassName + " is false!");
						}
						break;
					case 'L':
						// TODO check if is String?
				}
			}
		}
		return fields;
	}
	private ObjectAnnotation objectAnnotation() throws DeserializationException {
		ObjectAnnotation objectAnnotation = new ObjectAnnotation();
		List<Content> contents = new ArrayList<>();
		byte tc = buf.get();
		while (tc != ObjectStreamConstants.TC_ENDBLOCKDATA) {
			contents.add(content(tc));
		}
		if (contents.size() > 0) objectAnnotation.contents = contents.toArray(Content[]::new);
		return objectAnnotation;
	}
	private ExternalContents externalContents() throws DeserializationException {
		ExternalContents externalContents = new ExternalContents();
		// TODO implement. There's no way this is safe (indefinite loop -> new Content or external content?)
		return externalContents;
	}
	private NewClass newClass() throws DeserializationException {
		NewClass newClass = new NewClass();
		newClass.classDesc = classDesc();
		newClass.handle = nextHandle++;
		handleMap.put(newClass.handle, newClass);
		return newClass;
	}
	private NewArray newArray() throws DeserializationException {
		NewArray newArray = new NewArray();
		newArray.classDesc = classDesc();
		newArray.handle = nextHandle++;
		handleMap.put(newArray.handle, newArray);
		newArray.size = buf.getInt();
		if (newArray.size < 0) throw new DeserializationException("(non-negative) newArray size", newArray.size);
		SupplierThrows<?, DeserializationException> getter = null;
		if (newArray.classDesc.newClassDesc != null) {
			if (newArray.classDesc.newClassDesc.className != null) {
				String signature = newArray.classDesc.newClassDesc.className.substring(1);
				char typecode = signature.charAt(0);
				if (PRIM_TYPECODES.contains(typecode)) {
					if (signature.length() > 1) throw new DeserializationException("type signature", signature);
					switch (typecode) {
						case 'B': getter = buf::get; break;
						case 'S': getter = buf::getShort; break;
						case 'I': getter = buf::getInt; break;
						case 'J': getter = buf::getLong; break;
						case 'F': getter = buf::getFloat; break;
						case 'D': getter = buf::getDouble; break;
						case 'C': getter = buf::getChar; break;
						case 'Z': getter = () -> {
								byte b = buf.get();
								if (b == 0x00) return false;
								if (b == 0x01) return true;
								throw new DeserializationException("boolean as byte", b);
							}; break;
						default: throw new RuntimeException("Unsupported");
					}
				} else if (typecode == '[') {
					getter = this::newArray; // TODO make sure each value has right type (warning?)
				} else if (typecode == 'L') {
					if (signature.equals("Ljava/lang/String;")) getter = this::newString;
					else getter = this::newObject; // TODO make sure object follows `signature`
				} else throw new DeserializationException("primitive/object typecode", typecode);
			} else { // proxyClassDesc
				// TODO
			}
		}
		// TODO getter will not be null, this is just debug
		newArray.values = new Object[newArray.size];
		if (getter != null) {
			for (int i = 0; i < newArray.values.length; ++i) {
				newArray.values[i] = getter.get();
			}
		}
		return newArray;
	}
	private NewString newString() throws DeserializationException {
		NewString newString = new NewString();
		// newString.TC_STRING = ObjectStreamConstants.TC_STRING;
		newString.handle = nextHandle++;
		handleMap.put(newString.handle, newString);
		newString.value = readModifiedUTF8();
		return newString;
	}
	private NewString newLongString() throws DeserializationException {
		NewString newString = new NewString();
		// newString.TC_LONGSTRING = ObjectStreamConstants.TC_LONGSTRING;
		newString.handle = nextHandle++;
		handleMap.put(newString.handle, newString);
		newString.value = readModifiedLongUTF8();
		return newString;
	}
	private NewEnum newEnum() throws DeserializationException {
		NewEnum newEnum = new NewEnum();
		newEnum.classDesc = classDesc();
		newEnum.handle = nextHandle++;
		handleMap.put(newEnum.handle, newEnum);
		newEnum.enumConstantName = stringObject();
		return newEnum();
	}
	private NewClassDesc newClassDesc() throws DeserializationException {
		NewClassDesc newClassDesc = new NewClassDesc();
		// newClassDesc.TC_CLASSDESC = ObjectStreamConstants.TC_CLASSDESC;
		newClassDesc.className = readModifiedUTF8();
		newClassDesc.serialVersionUID = buf.getLong();
		newClassDesc.handle = nextHandle++;
		handleMap.put(newClassDesc.handle, newClassDesc);
		newClassDesc.classDescInfo = classDescInfo();
		return newClassDesc;
	}
	private NewClassDesc newProxyClassDesc() throws DeserializationException {
		NewClassDesc newClassDesc = new NewClassDesc();
		// newClassDesc.TC_PROXYCLASSDESC = ObjectStreamConstants.TC_CLASSDESC;
		newClassDesc.handle = nextHandle++;
		handleMap.put(newClassDesc.handle, newClassDesc);
		newClassDesc.proxyClassDescInfo = proxyClassDescInfo();
		return newClassDesc;
	}
	private ProxyClassDescInfo proxyClassDescInfo() throws DeserializationException {
		ProxyClassDescInfo proxyClassDescInfo = new ProxyClassDescInfo();
		proxyClassDescInfo.count = buf.getInt();
		if (proxyClassDescInfo.count < 0) throw new DeserializationException("(non-negative) proxyClassDescInfo count of proxyInterfaceNames", proxyClassDescInfo.count);
		proxyClassDescInfo.proxyInterfaceNames = new String[proxyClassDescInfo.count];
		for (int i = 0; i < proxyClassDescInfo.proxyInterfaceNames.length; ++i) {
			proxyClassDescInfo.proxyInterfaceNames[i] = readModifiedUTF8();
		}
		proxyClassDescInfo.classAnnotation = classAnnotation();
		proxyClassDescInfo.superClassDesc = classDesc();
		return proxyClassDescInfo;
	}
	private PrevObject prevObject() throws DeserializationException {
		PrevObject prevObject = new PrevObject();
		prevObject.handle = buf.getInt();
		java.lang.Object o = handleMap.get(prevObject.handle);
		if (o == null) throw new DeserializationException("prevObject handle", prevObject.handle);
		return prevObject;
	}
	private Exception exception() throws DeserializationException {
		Exception exception = new Exception();
		
		return exception;
	}
	private ClassDesc classDesc() throws DeserializationException {
		ClassDesc classDesc = new ClassDesc();
		byte tc = buf.get();
		switch (tc) {
			case ObjectStreamConstants.TC_CLASSDESC:
				classDesc.newClassDesc = newClassDesc();
				break;
			case ObjectStreamConstants.TC_PROXYCLASSDESC:
				classDesc.newClassDesc = newProxyClassDesc();
				break;
			case ObjectStreamConstants.TC_NULL:
				classDesc.nullReference = tc;
				break;
			case ObjectStreamConstants.TC_REFERENCE:
				// TODO classDesc.prevObject
				break;
		}
		return classDesc;
	}
	private ClassDescInfo classDescInfo() throws DeserializationException {
		ClassDescInfo classDescInfo = new ClassDescInfo();
		classDescInfo.classDescFlags = buf.get();
		if ((classDescInfo.classDescFlags & 0xE0) != 0) throw new DeserializationException("classDescFlags", classDescInfo.classDescFlags);
		classDescInfo.fieldDescs = fieldDescs();
		classDescInfo.classAnnotation = classAnnotation();
		classDescInfo.superClassDesc = classDesc();
		return classDescInfo;
	}
	private FieldDesc[] fieldDescs() throws DeserializationException {
		short count = buf.getShort();
		if (count < 0) throw new DeserializationException("(non-negative) fields count", count);
		FieldDesc[] fields = new FieldDesc[count];
		for (int i = 0; i < fields.length; ++i) {
			fields[i] = fieldDesc();
		}
		return fields;
	}
	private FieldDesc fieldDesc() throws DeserializationException {
		FieldDesc fieldDesc = new FieldDesc();
		char typecode = (char) buf.get();
		if (PRIM_TYPECODES.contains(typecode)) {
			fieldDesc.primitiveDesc = primitiveDesc(typecode);
		} else if (OBJ_TYPECODES.contains(typecode)) {
			fieldDesc.objectDesc = objectDesc(typecode);
		} else throw new DeserializationException("primitive/object typecode", typecode);
		return fieldDesc;
	}
	private PrimitiveDesc primitiveDesc(char typecode) throws DeserializationException {
		PrimitiveDesc primitiveDesc = new PrimitiveDesc();
		primitiveDesc.typecode = typecode;
		primitiveDesc.fieldName = readModifiedUTF8();
		return primitiveDesc;
	}
	private ObjectDesc objectDesc(char typecode) throws DeserializationException {
		ObjectDesc objectDesc = new ObjectDesc();
		objectDesc.typecode = typecode;
		objectDesc.fieldName = readModifiedUTF8();
		objectDesc.className = stringObject();
		return objectDesc;
	}
	private NewString stringObject() throws DeserializationException {
		byte tc = buf.get();
		switch (tc) {
			case ObjectStreamConstants.TC_STRING: return newString();
			case ObjectStreamConstants.TC_LONGSTRING: return newLongString();
			default: throw new DeserializationException("TC_STRING | TC_LONGSTRING", tc);
		}
	}
	private ClassAnnotation classAnnotation() throws DeserializationException {
		ClassAnnotation classAnnotation = new ClassAnnotation();
		List<Content> contents = new ArrayList<>();
		for (byte tc; (tc = buf.get()) != ObjectStreamConstants.TC_ENDBLOCKDATA;) {
			contents.add(content(tc));
		}
		if (contents.size() > 0) classAnnotation.contents = contents.toArray(Content[]::new);
		return classAnnotation;
	}
	private BlockData blockDataShort() throws DeserializationException {
		BlockData blockData = new BlockData();
		blockData.size = 0xFF & buf.get();
		byte[] data = new byte[blockData.size];
		buf.get(data);
		blockData.hex = Conversions.toHex(data);
		return blockData;
	}
	private BlockData blockDataLong() throws DeserializationException {
		BlockData blockData = new BlockData();
		blockData.size = buf.getInt();
		if (blockData.size < 0) throw new DeserializationException("(non-negative) blockData size", blockData.size);
		byte[] data = new byte[blockData.size];
		buf.get(data);
		blockData.hex = Conversions.toHex(data);
		return blockData;
	}
	private void reset() {
		nextHandle = ObjectStreamConstants.baseWireHandle;
		handleMap.clear();
		// TODO clear contents?
	}
}
