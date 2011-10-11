/*
 * Copyright (c) 2004-2008 University of Wisconsin-Madison and Omnitor AB
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package se.omnitor.protocol.rtp.t140redundancy;

import java.util.Vector;
import java.util.LinkedList;
import java.util.Iterator;

/**
 *  handles inserting and filtering of highlevel text redundancy data
 *
 *  @author Staffan Hellström
 *  @autohr Erik Zetterström
 */
public class RedundancyFilter {

    //Add redundancy variables
    private LinkedList<byte[]> fifoBuffer = new LinkedList<byte[]>();

    private int primarySeqNumber=0;
    private int redundancyLevels=3;



    //T140Filter variables
    private boolean redFlagOutgoing = false;

    private final byte[] sosBytes={(byte)0xC2,(byte)0x98,(byte)'R'};
    private final byte[] stBytes={(byte)0xC2,(byte)0x9C};

    // "packet" counter
    private int lastReceivedSeqNr=-1;


    /**
     * Creates the redundany filter with default settings.
     */
    public RedundancyFilter() {
	// preallocate for sender
	//fifoBuffer.add(new byte[0]);
    }

    /**
     * Creates the reundancy filter.
     *
     * @param redFlagOutgoing True if using RFC 4103 redundancy
     *                        is supported in this endpoint.
     * @param redundancyLevels Number of redundantGenerations to use.
     */
    public RedundancyFilter(boolean redFlagOutgoing,
			    int redundancyLevels) {
        this.redFlagOutgoing=redFlagOutgoing;
	this.redundancyLevels=redundancyLevels;
    }



    /**
     * Converts a char in byte form to a number.
     *
     * @param c The byte to convert.
     * @return The converted number.
     */
    private int charToNumber(byte c) {
	char ch=(char)c;
	String string=""+ch;
	return Integer.parseInt(string);
    }


    /**
     * Converts an integer to bytes.
     *
     * @param n The number to convert.
     * @return The converted bytes.
     */
    private byte[] numberToBytes(int n) {
	String str=String.valueOf(n);
	return str.getBytes();
    }


    /**
     * Converts a number of a specified length to bytes.
     * @param n The number to convert.
     * @param length The length of the numberto convert.
     *
     * @return The converted bytes.
     */
    private byte[] numberToBytes(int n, int length) {
	String str=String.valueOf(n);
	if (str.length()<length) {
	    for (int i=0; i<length-str.length(); i++)
		str="0"+str;
	}
	return str.getBytes();
    }


    /**
     * Converts a char in byte form to a number.
     *
     * @param c The char to convert.
     * @return The converted number.
     */
    /*
    private int charToNumber(byte[] c) {
	int result=0;
	int j=c.length-1;
	for (int i=0; i<c.length; i++) {
	    int power=(int)Math.pow(10,j);
	    int mult=charToNumber(c[i]);
	    result+=power*mult;
	    j--;
	}
	return result;
    }
*/

    /**
     * Converts two chars to a number.
     *
     * @param a The first byte of the char.
     * @param b The second number of the char.
     *
     * @return The cinverted number.
     */
    private int charToNumber(byte a, byte b) {
	return 10*charToNumber(a)+charToNumber(b);
    }


    /**
     * Finds a specified bytes in an array of other bytes.
     *
     * @param bytes The bytes to search in.
     * @param startIdx Index to start searching from.
     * @param endIdx Index to search to.
     * @param toFind THe byte to search for.
     * @return Returns the first index of the byte searched for,
     *         -1 if not found or bad input.
     */
    int findByte(byte[] bytes, int startIdx, int endIdx, byte toFind) {
	if (startIdx==-1) startIdx=0;
	if (endIdx==-1) endIdx=bytes.length;
	if (endIdx<startIdx) return -1;

	for (int i=startIdx; i<endIdx; i++) {
	    if (bytes[i] == toFind)
		return i;
	}

	return -1;
    }


    /**
     * Finds a specfied set of bytes in an array of bytes.
     *
     * @param bytes The bytes to search in.
     * @param startIdx The Index to start searching from.
     * @param endIdx The index to search to.
     * @param toFind The bytes to search for.
     * @param Returns The first index of the bytes searched for,
     *                -1 if not found or bad input.
     */
    int findBytes(byte[] bytes, int startIdx, int endIdx, byte[] toFind) {
	if (startIdx==-1) startIdx=0;
	if (endIdx==-1) endIdx=bytes.length;
	if (endIdx<startIdx) return -1;

	for (int i=startIdx; i<endIdx; i++) {
	    int matches=0;
	    for (int j=0; j<toFind.length && i+j<endIdx; j++) {
		if (bytes[i+j]==toFind[j]) matches++;
	    }
	    if (matches==toFind.length) return i;
	}
	return -1;
    }


    /**
     * Finds a byte in an array.
     *
     * @param bytes The bytes to search in.
     * @param toFind The bytes to search for.
     * @return The index of the byte or -1.
     */
    int findByte(byte[] bytes, byte toFind) {
	return findByte(bytes,-1,-1,toFind);
    }


    /**
     * Finds a byte in an array.
     *
     * @param bytes The bytes to search in.
     * @param startIdx The index to start searching from.
     * @param toFind The byte to search for.
     * @return The index of the byte or -1.
     * /
    int findByte(byte[] bytes, int startIdx, byte toFind) {
	return findByte(bytes,startIdx,-1, toFind);
    }


    /**
     * Finds a set of bytes in an array.
     *
     * @param bytes The bytes to search in.
     * @param startIdx The index to search from.
     * @param toFind The bytes to find.
     * @returnxsx The index of the bytes or -1.
     */
    int findByte(byte[] bytes, int startIdx, byte[] toFind) {
	return findBytes(bytes,startIdx,-1, toFind);
    }


    /**
     *  Seek for SOSR-ST blocks and extracts any lost data using the
     * 	redundancy data found in the blocks
     * 	NOTE: all packet loss characters are removed - new ones will
     * 	be inserted as need occurs.
     *
     * @param inputString The data to search.
     * @return The parsed data.
     */
    public byte[] filterInput(byte[] inputString) {

	Vector<Byte> output = new Vector<Byte>();

	boolean done=false;
	int stIndex=-1;
	boolean firstLoop=true;

	while (!done) {
	    int sosIndex=findByte(inputString,stIndex,sosBytes);

	    stIndex=findByte(inputString,sosIndex,stBytes);

	    if (firstLoop && sosIndex != -1) {

		// add everything before the redundancy block

		for (int i=0; i<sosIndex; i++) {
		    output.add(new Byte(inputString[i]));
		}

		firstLoop=false;
	    }

	    if (sosIndex != -1 && stIndex != -1 && stIndex>sosIndex) {
				// Inside a redundancy block

		int startIdx=sosIndex+sosBytes.length;
		int rfcSupport=charToNumber(inputString[startIdx]);
		int redLevel = charToNumber(inputString[startIdx+1]);

		int priSeqNr = charToNumber(inputString[startIdx+2],
					    inputString[startIdx+3]);

		int nbrPacketsLost = priSeqNr - lastReceivedSeqNr - 1;

		if ( nbrPacketsLost < 0 )
		    nbrPacketsLost += 100;

		//EZ: Added check to see if sending endpoint supports
		//    RFC 4103 redundancy.
		//    If RFC 4103 is supported T.140 redundancy
		//    should be ignored.
		if ( nbrPacketsLost > 0 && (rfcSupport != 1)) {

		    // packet loss
		    if (nbrPacketsLost <= redLevel) {
			// Manageable loss, recover using redundancy
			int recoverStart = redLevel - nbrPacketsLost;
			int rIdx=startIdx+4;
			int pos=0;

			// skip undamaged data
			for (int i=0; i<recoverStart; i++) {
			    int length = charToNumber(inputString[rIdx+pos],
						      inputString[rIdx+pos+1]);
			    pos += length+2;
			}

			// recover lost data
			for (int i=0; i<nbrPacketsLost; i++) {
			    int length = charToNumber(inputString[rIdx+pos],
						      inputString[rIdx+pos+1]);
			    pos += 2;
			    for (int j=0; j<length; j++) {
				output.add(new Byte(inputString[rIdx+pos+j]));
			    }
			    pos+=length;
			}


		    }
		    else {
			//EZ: Recover what we can. Replace each non-recoverable
			//    lost packet with a loss char.

			int rIdx = startIdx+4;
			int pos = 0;

			//EZ: Add a loss char for each unrecoverable packet.
			for (int i=0;i<(nbrPacketsLost-redLevel);i++) {
			    output.add(new Byte((byte)0xef));
			    output.add(new Byte((byte)0xbf));
			    output.add(new Byte((byte)0xbd));
			}

			//EZ: Extract and add all available redundant data.
			for (int i=0;i<redLevel;i++) {
			    int length = charToNumber(inputString[rIdx+pos],
						      inputString[rIdx+pos+1]);
			    pos += 2;
			    for (int j=0;j<length;j++) {
				output.add(new Byte(inputString[rIdx+pos+j]));
			    }
			    pos+=length;

			}

		    }

		}

		lastReceivedSeqNr=priSeqNr;
	   }

	    // Check for more redundancy blocks in the input
	    int nextSOS=findByte(inputString,sosIndex+1,sosBytes);

	    if (stIndex<nextSOS) {

		// between redundancy blocks, add everything apart
		// from packet loss chars to the output
		int st=(stIndex==-1)?0:stIndex+stBytes.length;
		for (int i=st; i<nextSOS; i++) {
		    if (inputString[i]==(byte)0xef &&
			inputString[i+1]==(byte)0xbf &&
			inputString[i+2]==(byte)0xbd) {
			i+=2;
			continue;
		    }
		    output.add(new Byte(inputString[i]));
		}
	    }


	    if (nextSOS==-1) {
		int startCopyPos=0;
		if (stIndex!= -1) {
		    startCopyPos=stIndex+stBytes.length;
		}

		// end of redundancy block, add everything afterwards
		// except packet loss chars
		done=true;
		for (int i=startCopyPos; i<inputString.length; i++) {
		    // EZ 041114: Removed. Should not filter away
		    //            any incoming data.
		    /*if (inputString[i]==0xef &&
		      inputString[i+1]==0xbf &&
		      inputString[i+2]==0xbd)
		    {
		        i+=2;
		        continue;
			}*/
		    output.add(new Byte(inputString[i]));
		}

	    }

	}

	// convert to byte array

	Object[] objarray=output.toArray();
	byte[] result=new byte[objarray.length];

	for (int cnt=0; cnt<objarray.length; cnt++) {
	    result[cnt] = ((Byte)objarray[cnt]).byteValue();
	}

	return result;
    }


    /**
     * Adds T.140 redundancy to T.140 text.
     *
     * @param The T.140 in byte format.
     * @return The T.140 text plus redundancy.
     */
    public byte[] addRedundancy(byte[] input) {

	byte[] rlevelsBytes=numberToBytes(redundancyLevels);
	byte[] priseqBytes=numberToBytes(primarySeqNumber,2);

	// the header
	byte[] header=new byte[4+rlevelsBytes.length+priseqBytes.length];
	header[0]=sosBytes[0];
	header[1]=sosBytes[1];
	header[2]=sosBytes[2];

	//EZ 041114: Added rfc4103 redundancy support indicator.
	if (redFlagOutgoing)
	    header[3]=(byte)('1');
	else
	    header[3]=(byte)('0');

	for (int i=0; i<rlevelsBytes.length; i++)
	    header[4+i]=rlevelsBytes[i];

	for (int i=0; i<priseqBytes.length; i++)
	    header[4+rlevelsBytes.length+i]=priseqBytes[i];

	// the body
	int nbytes=0;
	if (fifoBuffer.size()<redundancyLevels)
	    for (int i=0;i<redundancyLevels-fifoBuffer.size(); i++)
		nbytes+=2;

	for (Iterator itor=fifoBuffer.iterator(); itor.hasNext(); ) {
	    byte[] elmt=(byte[])itor.next();
	    //EZ: Added check if last element was null
	    //    Empty generation used to send old ones.
	    byte[] lengthBytes = null;
	    if (elmt!=null) {
		lengthBytes=this.numberToBytes(elmt.length,2);
		nbytes+=elmt.length+lengthBytes.length;
	    }
	    else {
		lengthBytes=this.numberToBytes(0,2);
		nbytes+=lengthBytes.length;
	    }
	}

	byte[] body=new byte[nbytes];
	int pos=0;

	// zero out nonused redundancy levels
	if (fifoBuffer.size()<redundancyLevels) {
	    for (int i=0;i<redundancyLevels-fifoBuffer.size(); i++){
		body[pos]=(byte)'0';
		body[pos+1]=(byte)'0';
		pos+=2;
	    }
	}

	for (Iterator itor=fifoBuffer.iterator(); itor.hasNext(); ) {
	    byte[] elmt=(byte[])itor.next();

	    // length of the data
	    byte[] lengthBytes = null;
	    if (elmt==null) {
		lengthBytes=this.numberToBytes(0,2);
	    }
	    else {
		lengthBytes=this.numberToBytes(elmt.length,2);
	    }
	    //EZ:  Added check if last element was null
	    //     Empty generation used to send old ones
	    for (int i=0; i<lengthBytes.length; i++)
		body[pos+i]=lengthBytes[i];
	    pos+=lengthBytes.length;

	    // the data
	    if (elmt!=null) {
		for (int j=0; j<elmt.length; j++)
		    body[pos+j]=elmt[j];

		pos+=elmt.length;
	    }
	}

	// convert to byte array
	//EZ: Added check if called with no data. For
	//    transmitting old redundant data.
	byte[] result = null;
	if (input == null)
	    result = new byte[header.length +
			     body.length +
			     stBytes.length];// + 1];
	else
	    result=new byte[header.length +
			   body.length +
			   stBytes.length +
			   input.length];//+1];

	System.arraycopy(header,0,result,0,header.length);
	System.arraycopy(body,0,result,header.length,body.length);
	System.arraycopy(stBytes,0,result,header.length+
			                  body.length,
			                  stBytes.length);
	//EZ: If function was called with no new data.
	//    For transmitting old redundant data.
	if (input != null)
	    System.arraycopy(input,0,result,header.length+
			                    body.length+
   			                    stBytes.length,
			                    input.length);

	// update FIFO buffer
	if (fifoBuffer.size() >= redundancyLevels)
	    fifoBuffer.removeFirst();

	fifoBuffer.addLast(input);

	primarySeqNumber++;
	if (primarySeqNumber==100)
	    primarySeqNumber=0;

	return result;
    }

}
