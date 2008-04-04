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
*    This class encapsulates all the necessary parameters of a
*    Sender Report that needs to be handed to the Application 
*    when a Sender Report is received
*
* @author Unknown
*/
public class SenderInfo {

    private long ntpTimeStampMostSignificant;
    private long ntpTimeStampLeastSignificant;
    private long rtpTimeStamp;
    private long senderPacketCount;
    private long senderOctetCount;

    /**
     * Gets the 32 most significant bits of the NTP time stamp. <br>
     * <br>
     * Indicates the wallclock time when this report was sent so that
     * it may be used in combination with timestamps returned in
     * reception reports from other receivers to measure round-trip
     * propagation to those receivers.
     *
     * @return The most significant 32 bits of the NTP time stamp.
     */
    public long getNtpTimeStampMostSignificant() {
	return ntpTimeStampMostSignificant;
    }

    /**
     * Sets the 32 most significant bits of the NTP time stamp
     *
     * @param timeStamp The 32 most significant bits of the NTP time stamp
     */
    public void setNtpTimeStampMostSignificant(long timeStamp) {
	this.ntpTimeStampMostSignificant = timeStamp;
    }
    
    /**
     * Gets the least significant 32 bits of the NTP Time stamp.
     *
     * @return The least significant 32 bits of the NTP time stamp.
     */
    public long getNtpTimeStampLeastSignificant() {
	return ntpTimeStampLeastSignificant;
    }

    /**
     * Sets the least significant 32 bits of the NTP time stamp
     *
     * @param timeStamp The least significant 32 bits if the NTP time stamp
     */ 
    public void setNtpTimeStampLeastSignificant(long timeStamp) {
	this.ntpTimeStampLeastSignificant = timeStamp;
    }
    
    /**
     * Gets the RTP time stamp. <br>
     * <br>
     * Corresponds to the same time as the NTP timestamp (above), but
     * in the same units and with the same random offset as the RTP
     * timestamps in data packets. This correspondence may be used for
     * intra- and inter-media synchronization for sources whose NTP
     * timestamps are synchronized, and may be used by media-
     * independent receivers to estimate the nominal RTP clock
     * frequency.
     *
     * @return The RTP time stamp
     */
    public long getRtpTimeStamp() {
	return rtpTimeStamp;
    }

    /**
     * Sets the RTP time stamp
     *
     * @param timeStamp The RTP time stamp
     */
    public void setRtpTimeStamp(long timeStamp) {
	this.rtpTimeStamp = timeStamp;
    }

    /**
     * Sets the RTP time stamp
     *
     * @param timeStamp The RTP time stamp
     */
    public void setTimeStamp(long timeStamp) {
	this.rtpTimeStamp = timeStamp;
    }
    
    /**
     * Gets the sender packet count. <br>
     * <br>
     * The total number of RTP data packets transmitted by the sender
     * since starting transmission up until the time this SR packet was
     * generated.  The count is reset if the sender changes its SSRC
     * identifier.
     *
     * @return The sender packet count
     */
    public long getSenderPacketCount() {
	return senderPacketCount;
    }

    /**
     * Sets the sender packet count
     *
     * @param count The sender packet count
     */
    public void setSenderPacketCount(long count) {
	this.senderPacketCount = count;
    }
    
    /**
     * Gets the sender octet count. <br>
     * <br>
     * The total number of payload octets (i.e., not including header
     * or padding) transmitted in RTP data packets by the sender since
     * starting transmission up until the time this SR packet was
     * generated. The count is reset if the sender changes its SSRC
     * identifier. This field can be used to estimate the average
     * payload data rate.
     *
     * @return The sender octet count  
     */
    public long getSenderOctetCount() {
	return senderOctetCount;
    }

    /**
     * Sets the sender octet count
     *
     * @param count The sender octet count
     */
    public void setSenderOctetCount(long count) {
	this.senderOctetCount = count;
    }
           
}
