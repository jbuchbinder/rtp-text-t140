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
 * A FirstInFirstOut buffer that holds integer values.
 *
 * @author Ingemar Persson, Omnitor AB
 */

public class FIFOBuffer {
    private int firstInPointer;
    private int usedSize;
    private int totalSize;
    private String[] circularBuffer;
    
    /**
     * Initializes the buffer, sets it empty.
     *
     * @param size The maximum size of the buffer
     */
    public FIFOBuffer(int size) {
        firstInPointer = 0;
        usedSize = 0;
        totalSize = size;
        circularBuffer = new String[totalSize];
    }

    /**
     * Put an String into the buffer. The method will wait untill the buffer
     * got space if the buffer is full.
     *
     * @param value to buffer
     *
     * @throws java.lang.InterruptedException
     */

    public synchronized void put(String value) throws InterruptedException {
        int index;

        // Wait if buffer is full
        while (usedSize >= totalSize)
        {
            // Wait for lock.
            wait();
        }

        index = (firstInPointer + usedSize) % totalSize;
        circularBuffer[index] = value;
        usedSize++;

        // Release the lock.
        notify();
    }

    /**
     * Get first in String from buffer. If no data is available the method
     * will wait untill data is put with the put method.
     *
     * @return first in String.
     *
     * @throws java.lang.InterruptedException
     */

    public synchronized String get() throws InterruptedException
    {
        String value;
	    int index;

        // Wait for data
        while (usedSize <= 0)
        {
            // Wait for a notify from the put method
            wait();
        }

        index = firstInPointer;
        firstInPointer = (firstInPointer + 1) % totalSize;
        usedSize--;
        value = circularBuffer[index];

        // Release the lock
        notify();

        return value;
    }

    /**
     *  Clears the buffer contents.
     *
     */
    public synchronized void clear()
    {
	firstInPointer = 0;
        usedSize = 0;
	
	// Release any pending locks
	notify();
    }
}
