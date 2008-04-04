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
 * RTCP Packet that needs to be handed to the Application 
 * when a RTCP Packet is received.This class represents the least common
 * denominator in an RTCP Packet. It is equivalent to an abstract base class
 * from which RTCPSenderReportPacket and RTCPReceiverReportPacket derive
 * from.
 *
 * @author Unknown
 */
public class RTCPPacket {
    
    private long senderSsrc;
    private ReportBlock reportBlock;
    private boolean containsReportBlock;

    /**
     * Gets the synchronization source identifier for the originator of this
     * RTCP packet. 
     *
     * @return The sender SSRC
     */
    public long getSenderSsrc() {
	return senderSsrc;
    }

    /**
     * Sets the sender SSRC
     *
     * @param senderSsrc The sender's SSRC
     */
    public void setSenderSsrc(long senderSsrc) {
	this.senderSsrc = senderSsrc;
    }
    
    /**
     * Gets the reception Report Block contained in this packet (if any)
     *
     * @return The reception report block
     */
    public ReportBlock getReportBlock() {
	return reportBlock;
    }

    /**
     * Sets the reception report block
     *
     * @param block The reception report block
     */
    public void setReportBlock(ReportBlock block) {
	this.reportBlock = block;
    }
    
    /**
     * Indicates whether this RTCP Packet contains a Reception
     * Report Block.
     *
     * @return True if this RTCP packet contains a reception report block
     */
    public boolean isContainingReportBlock() {
	return containsReportBlock;
    }

    /**
     * Sets whether this RTCP packets contains a reception report block.
     *
     * @param isReportBlock Whether this RTCP packet contains a reception
     * report block
     */
    public void doesContainReportBlock(boolean isReportBlock) {
	this.containsReportBlock = isReportBlock;
    }

}
