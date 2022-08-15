package svengo.dev.deserialize2;

import java.io.ObjectStreamConstants;

public class Deserialized {
	Short magic;
	Short version;
	Content[] contents;
	
	public static class Content {
		Object object;
		BlockData blockData;
		
		public static class Object {
			NewObject newObject;
			NewClass newClass;
			NewArray newArray;
			NewString newString;
			NewEnum newEnum;
			NewClassDesc newClassDesc;
			PrevObject prevObject;
			Byte nullReference;
			Exception exception;
			Byte TC_RESET;
			
			public static class NewObject {
				// final byte TC_OBJECT = ObjectStreamConstants.TC_OBJECT;
				Integer handle; // after ClassDesc but whatever
				ClassDesc classDesc;
				ClassDatum[] classData;

				public static class ClassDatum {
					Field[] nowrclass;
					Field[] wrclass;
					ObjectAnnotation objectAnnotation;
					ExternalContents externalContents;
					
					public static class Field {
						String name;
						java.lang.Object value;
					}
				}
				public static class ExternalContents {	// externalContent written by
					ExternalContent externalContent;	// writeExternal in PROTOCOL_VERSION_1.
					ExternalContents externalContents; // TODO ExternalContent externalContent;
					
					public static class ExternalContent {// Only parseable by readExternal
						byte[] bytes; // primitive data
						Object object;
					}
				}
				public static class ObjectAnnotation {
					Content[] contents;
					// final byte endBlockData = ObjectStreamConstants.TC_ENDBLOCKDATA;
				}
			}
			public static class NewClass {
				// final byte TC_CLASS = ObjectStreamConstants.TC_CLASS;
				Integer handle; // after ClassDesc but whatever
				ClassDesc classDesc;
			}
			public static class NewArray {
				// final byte TC_ARRAY = ObjectStreamConstants.TC_ARRAY;
				Integer handle; // after ClassDesc but whatever
				ClassDesc classDesc;
				Integer size;
				java.lang.Object[] values; // TODO java.lang.Object[] ?
			}
			public static class NewString {
				// option 1
				// Byte TC_STRING;
				// option 2
				// Byte TC_LONGSTRING
				Integer handle;
				String value;
			}
			public static class NewEnum {
				// final byte TC_ENUM = ObjectStreamConstants.TC_ENUM;
				Integer handle; // after ClassDesc but whatever
				ClassDesc classDesc;
				NewString enumConstantName;
			}
			public static class NewClassDesc {
				// required
				Integer handle;
				// option 1
				// Byte TC_CLASSDESC;
				String className;
				Long serialVersionUID;
				ClassDescInfo classDescInfo;
				// option 2
				// Byte TC_PROXYCLASSDESC;
				ProxyClassDescInfo proxyClassDescInfo;
				
				public static class ClassDescInfo {
					Byte classDescFlags;
					FieldDesc[] fieldDescs;
					ClassAnnotation classAnnotation;
					ClassDesc superClassDesc;

					public static class FieldDesc {
						PrimitiveDesc primitiveDesc;
						ObjectDesc objectDesc;
						
						public static class PrimitiveDesc {
							Character typecode;
							String fieldName;
						}
						public static class ObjectDesc {
							Character typecode;
							String fieldName;
							NewString className;
						}
					}
				}
				public static class ProxyClassDescInfo {
					Integer count;
					String[] proxyInterfaceNames;
					ClassAnnotation classAnnotation;
					ClassDesc superClassDesc;
				}
				// placed here for visiblity
				public static class ClassAnnotation {
					Content[] contents; // if present, they are written by ObjectOutputStream#annotateClass()
					// final byte endBlockData = ObjectStreamConstants.TC_ENDBLOCKDATA;
				}
			}
			public static class PrevObject {
				// final byte TC_REFERENCE = ObjectStreamConstants.TC_REFERENCE;
				Integer handle;
			}
			public static class Exception {
				// final byte TC_EXCEPTION = ObjectStreamConstants.TC_EXCEPTION;
				
			}
			// placed here for visiblity
			public static class ClassDesc {
				NewClassDesc newClassDesc;
				Byte nullReference;
				PrevObject prevObject; // must be a ClassDesc
			}
		}
		public static class BlockData {
			Integer size;
			String hex; // byte[] data;
			/*
			BlockDataShort blockDataShort;
			BlockDataLong blockDataLong;
			
			public static class BlockDataShort {
				// final byte TC_BLOCKDATA = ObjectStreamConstants.TC_BLOCKDATA;
				Integer size; // unsigned byte but we don't have unsigned bytes!
				byte[] data;
			}
			public static class BlockDataLong {
				// final byte TC_BLOCKDATALONG = ObjectStreamConstants.TC_BLOCKDATALONG;
				Integer size;
				byte[] data;
			}
			*/
		}
	}
}