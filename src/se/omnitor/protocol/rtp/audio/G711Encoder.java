package se.omnitor.protocol.rtp.audio;

/**
 * Implements G.711 U-law compressor.
 *
 * @author Oleg Kulikov
 */
public class G711Encoder {

    private final static byte[] muLawCompressTable = new byte[]{
        0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
        5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
        5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7
    };
    private final static int cBias = 0x84;
    private final static int cClip = 32635;
    private static short seg_end[] = new short[]{0xFF, 0x1FF, 0x3FF, 0x7FF,
        0xFFF, 0x1FFF, 0x3FFF, 0x7FFF
    };
    private byte[] temp = new byte[8192];

    public static int encode(byte[] src, int offset, int len, byte[] res) {
        int j = offset;
        int count = len / 2;
        short sample = 0;
        for (int i = 0; i < count; i++) {
            sample = (short) (((src[j++] & 0xff) | (src[j++]) << 8));
            res[i] = linear2ulaw(sample);
        }
        return count;
    }

    /*
     * linear2ulaw() - Convert a linear PCM value to u-law
     *
     * In order to simplify the encoding process, the original linear magnitude
     * is biased by adding 33 which shifts the encoding range from (0 - 8158) to
     * (33 - 8191). The result can be seen in the following encoding table:
     *
     *  Biased Linear Input Code        Compressed Code
     *  ------------------------        ---------------
     *  00000001wxyza                   000wxyz
     *  0000001wxyzab                   001wxyz
     *  000001wxyzabc                   010wxyz
     *  00001wxyzabcd                   011wxyz
     *  0001wxyzabcde                   100wxyz
     *  001wxyzabcdef                   101wxyz
     *  01wxyzabcdefg                   110wxyz
     *  1wxyzabcdefgh                   111wxyz
     *
     * Each biased linear code has a leading 1 which identifies the segment
     * number. The value of the segment number is equal to 7 minus the number
     * of leading 0's. The quantization interval is directly available as the
     * four bits wxyz.  * The trailing bits (a - h) are ignored.
     *
     * Ordinarily the complement of the resulting code word is used for
     * transmission, and so the code word is complemented before it is returned.
     *
     * For further information see John C. Bellamy's Digital Telephony, 1982,
     * John Wiley & Sons, pps 98-111 and 472-476.
     */
    private static byte linear2ulaw(short pcm_val) {
        int mask;
        int seg;
        byte uval;

        /* Get the sign and the magnitude of the value. */
        if (pcm_val < 0) {
            pcm_val = (short) (cBias - pcm_val);
            mask = 0x7F;
        } else {
            pcm_val += cBias;
            mask = 0xFF;
        }

        /* Convert the scaled magnitude to segment number. */
        seg = search(pcm_val, seg_end, 8);

        /*
         * Combine the sign, segment, quantization bits;
         * and complement the code word.
         */
        if (seg >= 8) /* out of range, return maximum value. */ {
            return (byte)(0x7F ^ mask);
        } else {
            uval = (byte)((seg << 4) | ((pcm_val >> (seg + 3)) & 0xF));
            return  (byte)(uval ^ mask);
        }

    }

    private static int search(int val, short[] table, int size) {
        int i;

        for (i = 0; i < size; i++) {
            if (val <= table[i]) {
                return (i);
            }
        }
        return (size);
    }

}
