package ch.unine.eclibrary;

import com.sun.jna.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ubuntu on 01.01.16.
 */
public class ECLibrary {

    public interface Longhair extends Library {
        Longhair INSTANCE = (Longhair) Native.loadLibrary("longhair", Longhair.class);

        /**
         * Verifies binary compatibility with the API on startup.
         * @return non-zero on success; zero on failure.
         */
        int _cauchy_256_init();

        /**
         *
         * @param k data blocks
         * @param m recovery blocks
         * @param block_bytes number of bytes per block; multiple of 8
         * @return zero on success, another value indicates failure.
         */
        //int cauchy_256_encode(int k, int m, const unsigned char *data_ptrs[], void *recovery_blocks, int block_bytes);
        int cauchy_256_encode(int k, int m, String[] data_ptrs, Pointer recovery_blocks, int block_bytes);
    }

    public static class Block extends Structure {
        public static class ByReference extends Block implements Structure.ByReference {}

        public String data; // unsigned char *data
        public char row; // unsigned char row

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] {"data", "row"});
        }
    }


    public static void main(String[] args) {
        // check compatibility with the API
        if (Longhair.INSTANCE._cauchy_256_init() == 0) {
            System.exit(1);
        }

        /* k - num of original blocks */
        final int k = 3;
        assert(k >= 0 && k < 256);

        /* m - num of recovery blocks */
        final int m = 2;
        assert(m >= 0 && m <= 256 - k);

        /* data ptrs */

        // original data as bytes
        byte[] originalData = "hello, world!".getBytes();

        // compute length of each block
        int originalLen = originalData.length;
        int newLen = originalLen;
        while (newLen % (8 * k) != 0) {
            newLen++;
        }
        byte[] paddedData = new byte[newLen];
        System.arraycopy(originalData, 0, paddedData, 0, originalLen);
        System.out.println("Original_len: " + originalLen + " New_len: " + newLen);
        System.out.println("originalData.len: " + originalData.length + " paddedData.len: " + paddedData.length);


        // allocate memory for data pointers array + each data pointer
        String[] dataPtrs = new String[k];
        int bytesPerChunk = newLen / k;
        assert (bytesPerChunk % 8 == 0);
        System.out.println("bytesPerChunk: " + bytesPerChunk);
        // split data intro chunks; add padding if necessary
        for (int i = 0; i < k; ++i) {
            int pos = i * bytesPerChunk;
            dataPtrs[i] = Arrays.copyOfRange(paddedData, pos, pos + bytesPerChunk).toString();
            System.out.println("i: " + i + " " + pos + " - " + pos + bytesPerChunk);
        }

        Pointer recoveryBlocks = new Memory (bytesPerChunk * m * Native.getNativeSize(Byte.TYPE));
       assert(Longhair.INSTANCE.cauchy_256_encode(k,m, dataPtrs, recoveryBlocks, bytesPerChunk) == 0);

        Block.ByReference[] blocks = new Block.ByReference[k + m];
        for (int i = 0; i < k + m; i++) {
            blocks[i] = new Block.ByReference();
        }
        System.out.println(blocks.length);
        for(int i = 0; i < k; ++i) {
            blocks[i].data = (String)dataPtrs[i];
            blocks[i].row = (char)i;
        }

        for (int i = 0; i < m; ++i) {
            blocks[k + i].data = (String)(recoveryBlocks.toString() + i * bytesPerChunk);
            blocks[k+ i].row = (char)i;
        }
    }
}
