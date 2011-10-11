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
package se.omnitor.util;

/**
 * An ordinary FIFO buffer.
 *
 * @author Andreas Piirimets, Omnitor AB
 */
public class FifoBuffer {
    
    private byte[] dataWaiting;
    
    /**
     * Initializes.
     *
     */
    public FifoBuffer() {
	dataWaiting = new byte[0];
    }

    /**
     * Sets new data, if data exists it is appended to the existing data.
     * 
     * @param newData The data to set/append. 
     */
    public synchronized void setData(byte[] newData) {

	if (dataWaiting.length == 0) {
	    dataWaiting = newData;
	}
	else {
		byte[] temp = dataWaiting;
	    dataWaiting = new byte[temp.length + newData.length];
	    System.arraycopy(temp, 0, dataWaiting, 0, temp.length);
	    System.arraycopy(newData, 0, dataWaiting, temp.length, 
			     newData.length);
	}

	notify();

    }
    
    /**
     * Gets the data of this object, data is consumed it is retrieved.
     * This method blocks until data is available.
     * 
     * @throws InterruptedException If the wait was interrupted.
     * @return The data.
     */ 
    public synchronized byte[] getData() throws InterruptedException {
	byte[] temp;

	if (dataWaiting.length == 0) {
	    wait();
	}
	
	temp = dataWaiting;
	dataWaiting = new byte[0];
	
	return temp;
    }
    
    /**
     * Empty the buffer.
     *
     */    
    public synchronized void empty() {
	dataWaiting = new byte[0];
    }

}