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
 * Sender Report that needs to be handed to the Application 
 * when a Sender Report is received. <br>
 * <br>
 * Note: this class derives from RTCPPacket
 *
 * @author Unknown
 */
public class RTCPSenderReportPacket extends RTCPPacket {

    private SenderInfo senderInfo;

    /**
     * Gets the Sender Info Block Contained in this Sender Report
     *
     * @return The sender info block
     */
    public SenderInfo getSenderInfo() {
	return senderInfo;
    }

    /**
     * Sets the sender info block
     *
     * @param senderInfo The sender info
     */
    public void setSenderInfo(SenderInfo senderInfo) {
	this.senderInfo = senderInfo;
    }
}
