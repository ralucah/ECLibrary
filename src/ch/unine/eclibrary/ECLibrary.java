package ch.unine.eclibrary;

import com.sun.jna.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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

        //int cauchy_256_encode(int k, int m, const unsigned char *data_ptrs[], void *recovery_blocks, int block_bytes);
        /**
         *
         * @param k data blocks
         * @param m recovery blocks
         * @param block_bytes number of bytes per block; multiple of 8
         * @return zero on success, another value indicates failure.
         */
        int cauchy_256_encode(int k, int m, Pointer[] data_ptrs, Pointer recovery_blocks, int block_bytes);

        //int cauchy_256_decode(int k, int m, Block *blocks, int block_bytes);
        /**
         * Recover original data
         * @param k num of original blocks
         * @param m num of recovery blocks
         * @param blocks blocks of data, original or recovery
         * @param blockBytes number of bytes per block; multiple of 8
         * @return 0 on success, otherwise failure
         */
        int cauchy_256_decode(int k, int m, Block[] blocks, int blockBytes);
    }

    public static class Block extends Structure {
        public static class ByReference extends Block implements Structure.ByReference {}

        public Pointer data; // unsigned char *data
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

        // k - num of original blocks
        final int k = 3;
        assert(k >= 0 && k < 256);

        // m - num of recovery blocks
        final int m = 2;
        assert(m >= 0 && m <= 256 - k);

        // original data as bytes
        byte[] originalData = "hello, world!".getBytes();
        //System.out.println(new String(originalData, StandardCharsets.UTF_8));

        // compute length of each block
        int originalLen = originalData.length;
        int newLen = originalLen;
        while (newLen % (8 * k) != 0) {
            newLen++;
        }
        byte[] paddedData = new byte[newLen];
        System.arraycopy(originalData, 0, paddedData, 0, originalLen);

        // 1. allocate memory for original data
        Pointer dataPtr = new Memory(newLen * Native.getNativeSize(Byte.TYPE));
        // 2. write padded data to that memory
        dataPtr.write(0, paddedData, 0, newLen);
        //System.out.println(Arrays.equals(paddedData, dataPtr.getByteArray(0, newLen)));
        //System.out.println(dataPtr.getByteArray(0, newLen).length);

        // 3. divide original data into k blocks
        Pointer[] dataPtrs = new Pointer[k];
        int blockSize = newLen / k;
        for (int i = 0; i < k; i++) {
            //System.out.println(i + " " + i * blockSize);
            dataPtrs[i] = dataPtr.getPointer(i * blockSize);
        }


        // reserve memory for the recovery blocks
        Pointer recoveryBlocks = new Memory (blockSize * m * Native.getNativeSize(Byte.TYPE));

        // encode!
        assert(Longhair.INSTANCE.cauchy_256_encode(k,m, dataPtrs, recoveryBlocks, blockSize) == 0);

        // encoded blocks
        Block.ByReference[] blocks = new Block.ByReference[k + m];
        for (int i = 0; i < k + m; i++) {
            blocks[i] = new Block.ByReference();
        }
        System.out.println("num encoded blocks: " + blocks.length);
        assert(blocks.length == k + m);

        Pointer result = new Memory(newLen * Native.getNativeSize(Byte.TYPE));;
        for(int i = 0; i < k; i++) {
            blocks[i].data = dataPtrs[i];
            blocks[i].row = (char)i;
        }
        for (int i = 0; i < m; ++i) {
            blocks[k + i].data = recoveryBlocks.getPointer(i * blockSize);
            blocks[k + i].row = (char)i;
        }

        assert(Longhair.INSTANCE.cauchy_256_decode(k, m, blocks, blockSize) == 0);

        /*for (int i = 0; i < k; ++i) {
            System.out.println((int)blocks[i].row);
            System.out.println(blocks[i].data.getByteArray(i * blockSize, blockSize));
        }*/
    }
}
