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
 *    SDES Item that needs to be handed to the Application 
 *    when a SDES Packet is received.
 *
 * @author Unknown
 */
public class SDESItem {

    private int type;
    private String value;

    /**
     * Gets the type of SDES Item, for example CNAME, EMAIL etc.
     *
     * @return The SDES type
     */
    public int getType() {
	return type;
    }
    
    /**
     * Sets the SDES type
     *
     * @param type The SDES type
     */
    public void setType(int type) {
	this.type = type;
    }

    /**
     * Gets the value of the SDES item
     *
     * @return The value of the SDES item
     */
    public String getValue() {
	return value;
    }

    /**
     * Sets the SDES item value
     *
     * @param value The value of the SDES item
     */
    public void setValue(String value) {
	this.value = value;
    }

}
