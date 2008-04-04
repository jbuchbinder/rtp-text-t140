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
 * An extention to Thread that also holds the state that the thread is in.
 * @author  Ingemar Persson
 * @version 0.1, 2003-jul-28 (Original version is marked as 0.1 and it is 
 * then incremented by 0.1 for every change made)
 */

public class StateThread extends Thread {

    /**
     * Defines that the thread is stopped.
     */
    public static final int STOP                = 0;

    /**
     * Defines that the thread is running
     */
    public static final int RUN                 = 1;

    /**
     * Defines that the thread is waiting.
     */
    public static final int WAIT                = 2;

    private static final int NUMBER_OF_STATES   = 3;

    private int state = RUN;

    /**
     * Initialize the thread.
     *
     */
    public StateThread()
    {
        super();
    }

    /**
     * Initializes the thread.
     *
     * @param target The object whose run() method is called.
     */
    public StateThread(Runnable target)
    {
        super(target);
    }

    /**
     * Initializes the thread.
     *
     * @param target The object whose run() method is called.
     * @param threadName The name of the new thread.
     */
    public StateThread(Runnable target, String threadName)
    {
        super(target, threadName);
    }

    /**
     * Sets new state of this thread. If state is changed from WAIT to RUN the
     * notify method will be called
     *
     * @param state defines new state for this thread
     */
    public synchronized void setState(int state)
    {
        // Make sure this is a legal state before altering state
        if ( (state < NUMBER_OF_STATES) && (state >= 0) )
        {
            int oldState = this.state;

            this.state = state;
            if (state == RUN && oldState != RUN)
            {
                notify();
            }
        }
    }

    /**
     * Returns the current state of this thread. If the state of this thread 
     * is WAIT this method will not return until
     * the state has changed to another state.
     *
     * @return state of this thread.
     */
    public synchronized int checkState()
    {
        while (state == WAIT)
        {
            try
            {
                wait();
            } catch (InterruptedException e){
		// Ignore interruptions
	    }
        }
        return state;
    }
}
