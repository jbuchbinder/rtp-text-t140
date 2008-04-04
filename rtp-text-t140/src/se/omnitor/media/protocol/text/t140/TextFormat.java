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
package se.omnitor.media.protocol.text.t140;

import javax.media.format.AudioFormat;

/**
 * Defines text formats similary to AudioFormat and VideoFormat.
 * Had to extend AudioFormat instead of Format. If we extend Format, JMF 
 * refused to create a player.
 *
 * @author Erik Zetterstrom, Omnitor AB
 */
public class TextFormat extends AudioFormat {
    
    /**
     * The enocding string must be one of those defined in the 
     * latest version of java.nio.charset.Charset
     * 
     * @param encoding Sets the encoding of this format, normally "UTF8".
     */
    public TextFormat(String encoding) {
	super(encoding,
	      AudioFormat.NOT_SPECIFIED,
	      AudioFormat.NOT_SPECIFIED,
	      AudioFormat.NOT_SPECIFIED,
	      AudioFormat.NOT_SPECIFIED,
	      AudioFormat.NOT_SPECIFIED,
	      AudioFormat.NOT_SPECIFIED,
	      AudioFormat.NOT_SPECIFIED,
	      null);

	this.encoding=encoding;
    }

    /**
     * Gets the encoding of the text.
     *
     * @return A string deifining the encoding of the text.
     */
    public String getEncoding() {
	return encoding;
    }

}
