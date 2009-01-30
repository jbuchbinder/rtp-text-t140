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
 * This class holds information about a redundant generation.
 *
 */
public class RedGen {
	
	private int generation;
	private long time;
	private int length;
	private String data;
	/**
	 * Initates the class and sets the initial values.
	 * 
	 * @param generation
	 * @param time
	 * @param length
	 * @param data
	 */
	public RedGen(int generation, long time, int length, String data) {
		this.generation = generation;
		this.time = time;
		this.length = length;
		this.data = data;
	}
	
	/**
	 * Returns the generation number.
	 * 
	 * @return
	 */
	public int getGeneration() {
		return generation;
	}
	
	/**
	 * Returns the time value.
	 * 
	 * @return
	 */
	public long getTime() {
		return time;
	}
	
	/**
	 * Returns the length.
	 * 
	 * @return
	 */
	public int getLength() {
		return length;
	}
	
	/**
	 * Returns the data.
	 * 
	 * @return
	 */
	public String getData() {
		return data;
	}
}