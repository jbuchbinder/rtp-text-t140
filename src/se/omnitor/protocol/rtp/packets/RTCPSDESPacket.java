/* 
 * Copyright (C) 2004  University of Wisconsin-Madison and Omnitor AB
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
 *    RTCP SDES Packet that needs to be handed to the Application 
 *    when a SDES Packet is received.
 *
 * @author Unknown
 */
public class RTCPSDESPacket {

    private SDESItem sdesItem;

    /**
     * Gets the SDES Item contained in the RTCP SDES Packet
     *
     * @return the SDES item
     */
    public SDESItem getSdesItem() {
	return sdesItem;
    }

    /**
     * Sets the SDES item
     *
     * @param sdesItem The SDES item
     */
    public void setSdesItem(SDESItem sdesItem) {
	this.sdesItem = sdesItem;
    }
    
}
