package com.crigas.buildschematic.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class VarIntUtil {
    
    public static int[] decodeVarIntArray(byte[] data) throws IOException {
        if (data.length == 0) {
            return new int[0];
        }
        
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        java.util.List<Integer> values = new java.util.ArrayList<>();
        
        while (stream.available() > 0) {
            values.add(readVarInt(stream));
        }
        
        return values.stream().mapToInt(Integer::intValue).toArray();
    }
    
    private static int readVarInt(ByteArrayInputStream stream) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;
        
        while (true) {
            if (stream.available() == 0) {
                throw new IOException("Unexpected end of stream while reading VarInt");
            }
            
            currentByte = (byte) stream.read();
            value |= (currentByte & 0x7F) << position;
            
            if ((currentByte & 0x80) == 0) break;
            
            position += 7;
            if (position >= 32) {
                throw new IOException("VarInt is too big");
            }
        }
        
        return value;
    }
}
