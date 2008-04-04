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
*    BYE Packet that needs to be handed to the Application 
*    when a BYE Packet is received.
*
* @author Unknown
*/
public class RTCPBYEPacket
{
    private long ssrc;
    private String reasonForLeaving;

    /**
     * Gets the SSRC of the source that sent a BYE Packet.
     *
     * @return The SSRC of the soruce that sent a BYE packet.
     */
    public long getSsrc() {
	return ssrc;
    }

    /**
     * Sets the SSRC
     *
     * @param ssrc The SSRC
     */
    public void setSsrc(long ssrc) {
	this.ssrc = ssrc;
    }
    
    /**
     * Gets the reason for leaving
     *
     * @return The reason for leaving
     */
    public String getReasonForLeaving() {
	return reasonForLeaving;
    }

    /**
     * Sets the reason for leaving
     *
     * @param reason The reason for leaving
     */
    public void setReasonForLeaving(String reason) {
	this.reasonForLeaving = reason;
    }
}
