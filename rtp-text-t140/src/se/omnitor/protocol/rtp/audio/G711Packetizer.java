/*
 * RTP text/t140 Library
 *
 * Copyright (C) 2004-2008 Board of Regents of the University of Wisconsin System
 * (Univ. of Wisconsin-Madison, Trace R&D Center)
 * Copyright (C) 2004-2008 Omnitor AB
 *
 * This software was developed with support from the National Institute on
 * Disability and Rehabilitation Research, US Dept of Education under Grant
 * # H133E990006 and H133E040014
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Please send a copy of any improved versions of the library to:
 * Gunnar Hellstrom, Omnitor AB, Renathvagen 2, SE 121 37 Johanneshov, SWEDEN
 * Gregg Vanderheiden, Trace Center, U of Wisconsin, Madison, Wi 53706
 *
 */
package se.omnitor.protocol.rtp.audio;

/**
 * Constructs an RTP-Text packet. <br>
 * <br>
 * According to "RFC 4103 - RTP Payload for text conversation"
 * transmitted text must be in UTF-8 form. <br>
 *
 * @author Erik Zetterstrom, Omnitor AB
 * @author Andreas Piirimets, Omnitor AB
 */
public class G711Packetizer {

	/**
	 * Time offset upper mask.
	 */
	public static final int RTP_PACK_TIMEOFFSET_UPPER_MASK = 0xff << 6;

	/**
	 * Time offset lower mask.
	 */
	public static final int RTP_PACK_TIMEOFFSET_LOWER_MASK = 0x3f << 0;

	/**
	 * Blocklength upper mask.
	 */
	public static final int RTP_PACK_BLOCKLEN_UPPER_MASK   = 0x3  << 8;

	/**
	 * BLock length lower mask.
	 */
	public static final int RTP_PACK_BLOCKLEN_LOWER_MASK   = 0xff << 0;

	private int pt;

	private long theTimeStamp        = 0;
	private long sequenceNumber      = 1;   //BEGIN AT WHICH NUMBER?

	/*
    //Formats
    private Format inputFormat  = null;
    private Format outputFormat = null;
    private TextFormat[] supportedInputFormats  = null;
    private TextFormat[] supportedOutputFormats = null;

    private AudioFormat[] siaf=null;
    private AudioFormat[] soaf=null;

    //Data not transmitted yet.
    private int historyLength    = 0;
    private byte[] history       = null;

    //Packet header
    private byte[] packetHeader     = null;

    //Dynamic payload types
    private int payload=0;

    private int bufferTimer          = 0;    //milliseconds

	 */


	/**
	 * Iniializes the packetizer.
	 *
	 * @param pt The payload type to use.
	 */
	public G711Packetizer(int pt) {

		this.pt = pt;

	}


	/**
	 * Encodes an RTP packet according to RFC 4103.
	 *
	 * @param inBuffer The data to be packetized.
	 * @param outBuffer The packet
	 *
	 * @return a statuscode
	 */
	public synchronized void encode(RtpAudioBuffer inBuffer, RtpAudioBuffer outBuffer) {

		int i=0; //Packet index

		theTimeStamp = java.lang.System.currentTimeMillis();//today.getTime();
		byte[] inData = inBuffer.getData();
		byte[] outData = null;

		if (inData.length > 0) {
			
			//Allocate memory for primary data
			int outLen = inData.length/2;
			outData = new byte[outLen];

			G711Encoder.encode(inData, 0, outData.length, outData);
			
/*			for (int cnt=0; cnt<outLen; cnt++) {
				outData[cnt] = (byte)G711Encoder.linear2ulaw(inData[cnt*2] & inData[cnt*2+1] << 8);
			}
	*/		
			//Add primary data to packet.
			//System.arraycopy(inData, 0, outData, i, inData.length);
			//i += inData.length;
		}
		else {
			outData = new byte[0];
		}			

		outBuffer.setData(outData);
		outBuffer.setLength(outData.length);
		outBuffer.setOffset(0);
		outBuffer.setTimeStamp(theTimeStamp);
		outBuffer.setSequenceNumber(sequenceNumber);
		sequenceNumber++;

		return;
	}


}