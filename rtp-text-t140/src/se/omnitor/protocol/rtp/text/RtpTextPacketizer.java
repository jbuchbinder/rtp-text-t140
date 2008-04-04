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
package se.omnitor.protocol.rtp.text;

import java.util.Vector;

/**
 * Constructs an RTP-Text packet. <br>
 * <br>
 * According to "RFC 4103 - RTP Payload for text conversation"
 * transmitted text must be in UTF-8 form. <br>
 *
 * @author Erik Zetterstrom, Omnitor AB
 * @author Andreas Piirimets, Omnitor AB
 */
public class RtpTextPacketizer {

    //Redundancy header bits for packetizer

    /**
     * Set red bit mask.
     */
    public static final int RTP_RED_SET_BIT = 0x1 << 07;

    /**
     * Clear red bit.
     */
    public static final int RTP_RED_CLEAR_BIT = 0x0 << 07;

    /**
     * Set the red F bit.
     */
    public static final int RTP_RED_F_BIT = 0x1 << 07;

    //Redundancy header masks for packetizer

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

    private int t140Pt;
    private int redGen;

    //Previous generations that are to be transmitted redundantely.
    private Vector<RTPTextRedData> redundantBuffer   = null;

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
     * @param t140pt The payload type to use.
     * @param redGen The number of redundant generations.
     */
    public RtpTextPacketizer(int t140Pt, int redPt, int redGen) {

	this.t140Pt = t140Pt;
	this.redGen = redGen;
	redundantBuffer = new Vector<RTPTextRedData>(0, 1);

    }


    /**
     * Encodes an RTP packet according to RFC 4103.
     *
     * @param inBuffer The data to be packetized.
     * @param outBuffer The packet
     *
     * @return a statuscode
     */
    public synchronized void encode(RtpTextBuffer inBuffer,
				    RtpTextBuffer outBuffer) {

	int i=0; //Packet index

	theTimeStamp = java.lang.System.currentTimeMillis();//today.getTime();
        byte[] inData = inBuffer.getData();
	int inDataLength = inBuffer.getLength();
        byte[] outData = null;
	byte[] tempOutData = null;
	int outDataSize = 0;

	//Allocate memory for redundant headers
	outData = new byte[redGen * TextConstants.REDUNDANT_HEADER_SIZE];

	if (redundantBuffer == null) {
	    redundantBuffer = new Vector<RTPTextRedData>(redGen, 0);
	}

	//Redundant data will be sent.
	if (redGen > 0) {
	    int gen = 0;

	    int timestampOffset = 0;
	    int dataLength = 0;

	    //Compensate for insufficient redundant data.

	    int border = redGen - redundantBuffer.size();
	    for (int g=0;g<border;g++) {
		//Timestamp 14 bits long
		timestampOffset=0;
		dataLength=0;

		//Add redundant header to packet
		outData[i++] = (byte)( RTP_RED_SET_BIT |
				       t140Pt);
		outData[i++] = (byte)(( RTP_PACK_TIMEOFFSET_UPPER_MASK &
					timestampOffset) >>> 6);
		outData[i++] = (byte)((( RTP_PACK_TIMEOFFSET_LOWER_MASK &
					 timestampOffset) << 2) |
				      (( RTP_PACK_BLOCKLEN_UPPER_MASK &
					 dataLength) >>> 8));
		outData[i++] = (byte)(( RTP_PACK_BLOCKLEN_LOWER_MASK &
					dataLength));
	    }

	    //Add headers for all redundant data, latest data LAST.
	    for (gen=0;gen<redGen;gen++) {

		//Check that enough redundant generations have been stored.
		if (gen<redundantBuffer.size()) {
		    RTPTextRedData dataObj =(RTPTextRedData)
			(redundantBuffer.elementAt(gen));

		    //Build the extra header info
		    //Timestamp 14 bits long
		    timestampOffset = (int)((theTimeStamp -
					     dataObj.getTimeStamp())
					    & 0x3FFF);
		    if (dataObj.getData() == null) {
			dataLength = 0;
		    }
		    else {
			dataLength = dataObj.getData().length;
		    }


		    //Add redundant header to packet
		    outData[i++] = (byte)( RTP_RED_SET_BIT |
					   t140Pt);
		    outData[i++] = (byte)((RTP_PACK_TIMEOFFSET_UPPER_MASK &
					   timestampOffset) >>> 6);
		    outData[i++] = (byte)(((RTP_PACK_TIMEOFFSET_LOWER_MASK &
					    timestampOffset) << 2) |
					  ((RTP_PACK_BLOCKLEN_UPPER_MASK &
					    dataLength) >>> 8));
		    outData[i++] = (byte)((RTP_PACK_BLOCKLEN_LOWER_MASK &
					   dataLength));
		}

	    }

	    //Allocate memory for primary header
	    tempOutData = outData;
	    outDataSize = outData.length;
	    outData = new byte[outDataSize+TextConstants.PRIMARY_HEADER_SIZE];
	    System.arraycopy(tempOutData, 0, outData, 0, outDataSize);

	    //Add final header
	    outData[i++] = (byte)( RTP_RED_CLEAR_BIT | t140Pt);

	    //Add redundant data, latest data LAST.
	    for (gen=0;gen < redGen;gen++) {

		if (gen < redundantBuffer.size()) {
		    RTPTextRedData dataObjData=(RTPTextRedData)
			(redundantBuffer.elementAt(gen));

		    //Add redundant data to packet
		    byte[] dataArr = dataObjData.getData();

		    if (dataArr != null) {

			//Allocate memory for redundant data
			tempOutData=outData;
			outDataSize=outData.length;
			outData = new byte[outDataSize+dataArr.length];
			System.arraycopy(tempOutData,0,outData,0,outDataSize);

			System.arraycopy(dataArr, 0, outData, i,
					 dataArr.length);
			i += dataArr.length;

			dataArr = null;
		    }

		    dataObjData = null;
		}

	    }

	    //Remove first redundant element in vector.
	    if (redundantBuffer.size() >= redGen) {
		redundantBuffer.removeElementAt(0);
	    }

	    //Add a new redundant element to vector.
	    redundantBuffer.addElement(new RTPTextRedData(theTimeStamp,
							  inData,
							  0,
							  inDataLength));
	}

	if (inDataLength > 0) {

	    //Allocate memory for primary data
	    tempOutData=outData;
	    outDataSize=outData.length;
	    outData = new byte[outDataSize+inDataLength];
	    System.arraycopy(tempOutData,0,outData,0,outDataSize);

	    //Add primary data to packet.
	    System.arraycopy(inData, 0, outData, i, inData.length);
	    i += inData.length;
	}

	outBuffer.setData(outData);
	outBuffer.setLength(outData.length);
	outBuffer.setOffset(0);
	outBuffer.setTimeStamp(theTimeStamp);
	outBuffer.setSequenceNumber(sequenceNumber);
	sequenceNumber++;

	return;
    }


    /**
     * Carries redundant data.
     *
     * @author Erik Zetterstrom, Omnitor AB
     */
    public class RTPTextRedData {
	private long   myTimestamp = 0;
	private int    mySeqNum    = 0;
	private byte[] myDataArr   = null;

	/**
	 * Constructor
	 *
	 * @param timestamp The timestamp of the data.
	 * @param dataArr The data byte array.
	 * @param dataOffset The offset in bytes for the data.
	 * @param dataLength The number of data bytes in the array.
	 */
	public RTPTextRedData(long timestamp,
			      byte[] dataArr,
			      int dataOffset,
			      int dataLength) {

	    myTimestamp = timestamp;

	    if (dataArr == null) {
		myDataArr = null;
	    }
	    else {
		myDataArr = new byte[dataLength];
		System.arraycopy(dataArr, dataOffset, myDataArr, 0, dataLength);
	    }
	}


	/**
	 * Constructor
	 *
	 * @param timestamp  The timestamp of the data.
	 * @param dataArr    The data byte array.
	 * @param dataOffset The offset in bytes for the data.
	 * @param dataLength The number of data bytes in the array.
	 * @param theSeqNum  The sequnece number.
	 */
	public RTPTextRedData(long timestamp,
			      byte[] dataArr,
			      int dataOffset,
			      int dataLength,
			      int theSeqNum) {
	    myDataArr = new byte[dataLength];
	    myTimestamp = timestamp;
	    mySeqNum = theSeqNum;

	    System.arraycopy(dataArr, dataOffset, myDataArr, 0, dataLength);
	}

	/**
	 *  Gets the timestamp
	 *
	 * @return the timestamp.
	 */
	public long getTimeStamp() {
	    return myTimestamp;
	}

	/**
	 *  Gets the sequence number
	 *
	 * @return the sequence number.
	 */
	public int getSeqNum() {
	    return mySeqNum;
	}

	/**
	 *  Gets the data.
	 *
	 * @return the data byte array.
	 */
	public byte[] getData()    {
	    return myDataArr;
	}
    }
    
    public int dropOneRtpTextSeqNo() {
    	sequenceNumber++;
    	return (int)(sequenceNumber-1);
    }


}