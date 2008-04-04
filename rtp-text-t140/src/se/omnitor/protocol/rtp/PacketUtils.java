/* 
 * Copyright (C) 2004-2008  University of Wisconsin-Madison and Omnitor AB
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package se.omnitor.protocol.rtp;

/**
 * This class provides generic packet assembly and disassembly functions, which
 * are used in various parts of the project e.g. assembling and disassembling
 * RTP and RTCP packets etc.
 *
 * @author Unknown
 */
public class PacketUtils {

    /**
     *   Append two byte arrays.
     *   Appends packet B at the end of Packet A (Assuming Bytes as elements).
     *   Returns packet ( AB ).
     *
     *     @param     packetA The first packet.
     *     @param     packetB The second packet.
     *     @return    The desired packet which is A concatenated with B.
     */
    
    public static synchronized byte[] append (  byte[] packetA, 
						byte[] packetB ) {

        // Create a new array whose size is equal to sum of packets
        // being added
        byte[] packetAB = new byte [ packetA.length + packetB.length ];
	
        // First paste in packetA
        for ( int i=0; i < packetA.length; i++ ) {
            packetAB [i] = packetA [i];
	}
	
        // Now start pasting packetB
        for ( int i=0; i < packetB.length; i++ ) {
            packetAB [i+packetA.length] = packetB [i];
	}
	
        return packetAB;
    }
    
    /**
     * Convert signed int to long by taking 2's complement if necessary.
     *
     * @param intToConvert The signed integer which will be converted to Long.
     *
     * @return The unsigned long representation of the signed int.
     */
    public static synchronized long convertSignedIntToLong ( int intToConvert)
    {
        int in = intToConvert;
        // IP: Removed
        // Session.outprintln (String.valueOf(in));
	
        in = ( in << 1 ) >> 1;
	
        long lin = (long) in;
        lin = lin + 0x7FFFFFFF;
	
        return lin;
    }
    
    /**
     * Convert 64 bit long to n bytes.
     *
     * @param ldata   The long from which the n byte array will be constructed.
     * @param n       The desired number of bytes to convert the long to.
     *
     * @return The desired byte array which is populated with the long value.
     */
    public static synchronized byte[] longToBytes ( long ldata, int n )
    {
        byte[] buff = new byte [ n ];
	
        for ( int i=n-1; i>=0; i--) {
	    // Keep assigning the right most 8 bits to the
	    // byte arrays while shift 8 bits during each iteration
	    buff [ i ] = (byte) ldata;
            ldata = ldata >> 8;
        }
        return buff;
    }
    
    /**
     * Calculate number of octets required to fit the
     * given number of octets into 32 bit boundary.
     *
     * @param  lengthOfUnpaddedPacket The length of an unpadded packet
     *
     * @return The required number of octets which must be appended to this
     *         packet to make it fit into a 32 bit boundary.
     */
    public static synchronized int calculatePadLength 
	( int lengthOfUnpaddedPacket ) {

        // Determine the number of 8 bit words required to fit the packet in
	// 32 bit boundary
        int remain = 
	    (int) Math.IEEEremainder ( (double) lengthOfUnpaddedPacket , 
				       (double) 4 );
	
        int padLen = 0;
	
        // e.g. remainder -1, then we need to pad 1 extra
        // byte to the end to make it to the 32 bit boundary
        if ( remain < 0 )
            padLen = Math.abs ( remain );
	
        // e.g. remainder is +1 then we need to pad 3 extra bytes
        else if ( remain > 0 )
            padLen = 4-remain;
	
        return ( padLen );
	
    }
    
}
