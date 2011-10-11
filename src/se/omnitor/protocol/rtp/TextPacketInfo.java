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

import java.util.ArrayList;
import java.util.List;

/**
 * This class holds information about a text packet.
 * 
 */
public class TextPacketInfo {
	
	private int sequenceNumber;
	private int payloadType;
	private byte[] rawData;
	private List<RedGen> redGenList;
	private static final int INITIAL_REDLIST_CAPACITY = 10;
	
	/**
	 * Initiates the class and sets the initial values.
	 * 
	 * @param sequenceNumber
	 * @param payloadType
	 * @param rawData
	 */
	public TextPacketInfo(int sequenceNumber, int payloadType, byte[] rawData) {
		this.sequenceNumber = sequenceNumber;
		this.payloadType = payloadType;
		this.rawData = rawData;
		redGenList = new ArrayList<RedGen>(INITIAL_REDLIST_CAPACITY);
	}
	
	/**
	 * Adds a redundant generation to the list.
	 * 
	 * @param redGen
	 */
	public void addRedundantGeneration(RedGen redGen) {
		redGenList.add(redGen);
	}

	/**
	 * Returns the sequence number.
	 * 
	 * @return
	 */
	public int getSequenceNumber() {
		return sequenceNumber;
	}
	
	/**
	 * Returns the payload type.
	 * 
	 * @return
	 */
	public int getPayloadType() {
		return payloadType;
	}
	
	/**
	 * Returns redundant generations list for this packet
	 * 
	 * @return
	 */
	public List<RedGen> getRedundantGenerations() {
		return redGenList;
	}
	
	/**
	 * Returns raw packet data
	 * 
	 * @return
	 */
	public byte[] getRawData() {
		return rawData;
	}
	
	/**
	 * Returns primary generation for this packet or null
	 * 
	 * @return
	 */	
	public RedGen getPrimaryRedGen() {
		return findRedGen(0);
	}
	/**
	 * Returns generation with specified number for this packet or null
	 * 
	 * @return
	 */		
	public RedGen findRedGen(int generationNumber) {
		for (RedGen redGen : redGenList) {
			if(redGen.getGeneration() == generationNumber)
				return redGen;
		}
		return null;
	}
}