package com.itextpdf.core.pdf.filters;

import com.itextpdf.basics.PdfException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A class for performing LZW decoding.
 */
public class LZWDecoder {

    byte stringTable[][];
    byte data[] = null;
    OutputStream uncompData;
    int tableIndex, bitsToGet = 9;
    int bytePointer, bitPointer;
    int nextData = 0;
    int nextBits = 0;

    int andTable[] = {
            511,
            1023,
            2047,
            4095
    };

    public LZWDecoder() {
    }

    /**
     * Method to decode LZW compressed data.
     *
     * @param data The compressed data.
     * @param uncompData Array to return the uncompressed data in.
     */
    public void decode(byte data[], OutputStream uncompData) throws PdfException {

        if(data[0] == (byte)0x00 && data[1] == (byte)0x01) {
            throw new PdfException(PdfException.LzwFlavourNotSupported);
        }

        initializeStringTable();

        this.data = data;
        this.uncompData = uncompData;

        // Initialize pointers
        bytePointer = 0;
        bitPointer = 0;

        nextData = 0;
        nextBits = 0;

        int code, oldCode = 0;
        byte string[];

        while ((code = getNextCode()) != 257) {

            if (code == 256) {

                initializeStringTable();
                code = getNextCode();

                if (code == 257) {
                    break;
                }

                writeString(stringTable[code]);
                oldCode = code;

            } else {

                if (code < tableIndex) {

                    string = stringTable[code];

                    writeString(string);
                    addStringToTable(stringTable[oldCode], string[0]);
                    oldCode = code;

                } else {

                    string = stringTable[oldCode];
                    string = composeString(string, string[0]);
                    writeString(string);
                    addStringToTable(string);
                    oldCode = code;
                }
            }
        }
    }


    /**
     * Initialize the string table.
     */
    public void initializeStringTable() {

        stringTable = new byte[8192][];

        for (int i=0; i<256; i++) {
            stringTable[i] = new byte[1];
            stringTable[i][0] = (byte)i;
        }

        tableIndex = 258;
        bitsToGet = 9;
    }

    /**
     * Write out the string just uncompressed.
     */
    public void writeString(byte string[]) throws PdfException {
        try {
            uncompData.write(string);
        }
        catch (IOException e) {
            throw new PdfException(PdfException.LzwDecoderException, e);
        }
    }

    /**
     * Add a new string to the string table.
     */
    public void addStringToTable(byte oldString[], byte newString) {
        int length = oldString.length;
        byte string[] = new byte[length + 1];
        System.arraycopy(oldString, 0, string, 0, length);
        string[length] = newString;

        // Add this new String to the table
        stringTable[tableIndex++] = string;

        if (tableIndex == 511) {
            bitsToGet = 10;
        } else if (tableIndex == 1023) {
            bitsToGet = 11;
        } else if (tableIndex == 2047) {
            bitsToGet = 12;
        }
    }

    /**
     * Add a new string to the string table.
     */
    public void addStringToTable(byte string[]) {

        // Add this new String to the table
        stringTable[tableIndex++] = string;

        if (tableIndex == 511) {
            bitsToGet = 10;
        } else if (tableIndex == 1023) {
            bitsToGet = 11;
        } else if (tableIndex == 2047) {
            bitsToGet = 12;
        }
    }

    /**
     * Append <code>newString</code> to the end of <code>oldString</code>.
     */
    public byte[] composeString(byte oldString[], byte newString) {
        int length = oldString.length;
        byte string[] = new byte[length + 1];
        System.arraycopy(oldString, 0, string, 0, length);
        string[length] = newString;

        return string;
    }

    // Returns the next 9, 10, 11 or 12 bits
    public int getNextCode() {
        // Attempt to get the next code. The exception is caught to make
        // this robust to cases wherein the EndOfInformation code has been
        // omitted from a strip. Examples of such cases have been observed
        // in practice.
        try {
            nextData = (nextData << 8) | (data[bytePointer++] & 0xff);
            nextBits += 8;

            if (nextBits < bitsToGet) {
                nextData = (nextData << 8) | (data[bytePointer++] & 0xff);
                nextBits += 8;
            }

            int code =
                    (nextData >> (nextBits - bitsToGet)) & andTable[bitsToGet-9];
            nextBits -= bitsToGet;

            return code;
        } catch(ArrayIndexOutOfBoundsException e) {
            // Strip not terminated as expected: return EndOfInformation code.
            return 257;
        }
    }
}