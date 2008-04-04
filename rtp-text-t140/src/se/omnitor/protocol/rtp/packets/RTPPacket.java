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
package se.omnitor.protocol.rtp.packets;

/**
 * This class encapsulates all the necessary parameters of a
 * RTP Packet that needs to be handed to the Application
 * when a RTP Packet is received.
 *
 * @author Unknown
 */
public class RTPPacket {

    private long csrcCount;
    private long sequenceNumber;
    private long timeStamp;
    private long ssrc;
    private byte[] data;
    private byte markerByte;

    /**
     * Gets the CSRC count. <br>
     * <br>
     * The CSRC count contains the number of CSRC identifiers that
     * follow the fixed header.
     *
     * @return The CSRC count
     */
    public long getCsrcCount() {
	return csrcCount;
    }

    /**
     * Sets the CSRC count.
     *
     * @param csrcCount The CSRC count to set
     */
    public void setCsrcCount(long csrcCount) {
	this.csrcCount = csrcCount;
    }

    /**
     * Gets the sequence number. <br>
     * <br>
     * The sequence number increments by one for each RTP data packet
     * sent, and may be used by the receiver to detect packet loss and
     * to restore packet sequence. The initial value of the sequence
     * number is random (unpredictable) to make known-plaintext attacks
     * on encryption more difficult, even if the source itself does not
     * encrypt, because the packets may flow through a translator that
     * does.
     *
     * @return The sequence number
     */
    public long getSequenceNumber() {
	return sequenceNumber;
    }

    /**
     * Sets the sequence number
     *
     * @param seqNo The sequencenumber
     */
    public void setSequenceNumber(long seqNo) {
	this.sequenceNumber = seqNo;
    }

    /**
     * Gets the time stamp. <br>
     * <br>
     * The timestamp reflects the sampling instant of the first octet
     * in the RTP data packet.
     *
     * @return The time stamp
     */
    public long getTimeStamp() {
	return timeStamp;
    }

    /**
     * Sets the time stamp
     *
     * @param timeStamp The time stamp to set
     */
    public void setTimeStamp(long timeStamp) {
	this.timeStamp = timeStamp;
    }

    /**
     * Gets the SSRC. <br>
     * <br>
     * The SSRC field identifies the synchronization source. This
     * identifier is chosen randomly, with the intent that no two
     * synchronization sources within the same RTP session will have
     * the same SSRC identifier.
     *
     * @return The SSRC field
     */
    public long getSsrc() {
	return ssrc;
    }

    /**
     * Sets the SSRC.
     *
     * @param ssrc The SSRC to set
     */
    public void setSsrc(long ssrc) {
	this.ssrc = ssrc;
    }

    /**
     * Gets the payload, that is the actual payload contained in an RTP Packet.
     *
     * @return The payload data
     */
    public byte[] getPayloadData() {
	return data;
    }

    /**
     * Sets the payload data
     *
     * @param payloadData The payload data to set
     */
    public void setPayloadData(byte[] payloadData) {
	this.data = payloadData;
    }

    /**
     * Sets the marker bit for this packet.
     *
     * @param marker true = marker bit set
     */
    public void setMarker(boolean marker) {
	if(marker) {
	    markerByte=0x1;
	    return;
	}
	markerByte=0;
    }

    /**
     * Gets the marker bit for this packet.
     *
     * @return 1 if marker is set, 0 otherwise.
     */
    public byte getMarker() {
	return markerByte;
    }

}



