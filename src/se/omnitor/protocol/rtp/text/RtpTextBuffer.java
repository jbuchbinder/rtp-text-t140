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

/**
 * Contains information about one RTP text packet.
 *
 * @author Andreas Piirimets, Omnitor AB
 */
public class RtpTextBuffer {

    private byte[] data;
    private int length;
    private int offset;
    private long timeStamp;
    private long seqNo;
    private long ssrc;
    private byte markerByte;
    
    public void setSsrc(long ssrc) {
	this.ssrc = ssrc;
    }

    public void setData(byte[] data) {
	this.data = data;
    }

    public void setLength(int length) {
	this.length = length;
    }

    public void setOffset(int offset) {
	this.offset = offset;
    }

    public void setTimeStamp(long timeStamp) {
	this.timeStamp = timeStamp;
    }

    public void setSequenceNumber(long seqNo) {
	this.seqNo = seqNo;
    }

    public void setMarker(boolean marker) {
	if(marker) {
	    markerByte=0x1;
	    return;
	}
	markerByte=0;
    }

    public long getSsrc() {
	return ssrc;
    }

    public byte[] getData() {
	return data;
    }

    public int getLength() {
	return length;
    }

    public int getOffset() {
	return offset;
    }

    public long getTimeStamp() {
	return timeStamp;
    }

    public long getSequenceNumber() {
	return seqNo;
    }

    public boolean getMarker() {
	if(markerByte==1) {
	    return true;
	}
	return false;
    }
}