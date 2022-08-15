package svengo.dev.deserialize2;
import java.util.function.IntFunction;

public interface Conversions {
    char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    /**
     * @return hexidecimal-representation of the byte array, with NO preceding "0x"
     */
    static String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = -1, k = -1; ++j < bytes.length;) {
            int v = bytes[j] & 0xFF;
            hexChars[++k] = HEX_ARRAY[v >>> 4];
            hexChars[++k] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * @return hexidecimal-representation of the byte array, optional preceding "0x"
     */
    static byte[] toBytes(String hex) throws NumberFormatException {
        if (hex.startsWith("0x")) hex = hex.substring(2);
        if (hex.length() % 2 == 1) hex = '0' + hex;
        byte[] bytes = new byte[hex.length() / 2];
        for(int di = 0, hi = 0; di < bytes.length;) {
            bytes[di++] = (byte) Integer.parseInt(hex.substring(hi, hi += 2), 16);
        }
        return bytes;
    }

    /**
     * @param tArray array to buffer
     * @param newArray creates a new T[] of given length ({@code T[]::new} is expected)
     * @param newArrayArray creates a new T[][] of given length ({@code T[][]::new} is expected)
     * @param maxBufferSize maximum number of elements in a buffer
     * @return an array which represents the buffering of {@code tArray} into
     *  {@code tArray.length / maxBufferSize} sub-arrays of size {@code maxBufferSize} with an
     *  additional subarray containing the remainder elements if a remander exists. For example,
     *  <pre> {@code
     *      Byte[] bytes = {0, 1, 2, 3, 4, 5, 6, 7};
     *      Byte[][] buffered = Conversions.buffer(bytes, Byte[]::new, Byte[][]::new, 4);
     *      // buffered = {{0, 1, 2, 3}, {4, 5, 6, 7}}
     *  } </pre>
     *  <pre> {@code
     *      Byte[] bytes = {0, 1, 2, 3, 4, 5, 6, 7, 8};
     *      Byte[][] buffered = Conversions.buffer(bytes, Byte[]::new, Byte[][]::new, 4);
     *      // buffered = {{0, 1, 2, 3}, {4, 5, 6, 7}, {8}}
     *  } </pre>
     * @throws IllegalArgumentException if {@code maxBufferSize} is non-positive
     */
    static <T> T[][] buffer(T[] tArray, IntFunction<T[]> newArray, IntFunction<T[][]> newArrayArray, int maxBufferSize) {
        if (maxBufferSize < 1) throw new IllegalArgumentException("maxBufferSize must be positive");
        final int remainder = tArray.length % maxBufferSize;
        final int maxBuffers = tArray.length / maxBufferSize;
        final T[][] tArrayArray = newArrayArray.apply(maxBuffers + (remainder == 0 ? 0 : 1));
        int i = -1;
        int offset = 0;
        for (; ++i < maxBuffers; offset += maxBufferSize)
            System.arraycopy(tArray, offset, (tArrayArray[i] = newArray.apply(maxBufferSize)), 0, maxBufferSize);
        if (remainder != 0) System.arraycopy(tArray, offset, (tArrayArray[i] = newArray.apply(remainder)), 0, remainder);
        return tArrayArray;
    }
}
