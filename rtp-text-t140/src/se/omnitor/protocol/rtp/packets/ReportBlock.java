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
 * Reception Report that needs to be handed to the Application 
 * when a Reception Report is received.
 *
 * @author Unknown
 */
public class ReportBlock {

    private double fractionLost ;
    private long cumulativeNumberOfPacketsLost;
    private long extendedHighestSequenceNumberReceived;
    private double interarrivalJitter;
    private double lastSr;
    private long delayLastSr;

    /**
     * Gets the fraction lost value. <br>
     * <br>
     * The fraction of RTP data packets from source SSRC_n lost since
     * the previous SR or RR packet was sent, expressed as a fixed
     * point number with the binary point at the left edge of the
     * field. (That is equivalent to taking the integer part after
     * multiplying the loss fraction by 256.) This fraction is defined
     * to be the number of packets lost divided by the number of
     * packets expected. 
     *
     * @return The fraction lost value
     */
    public double getFractionLost() {
	return fractionLost;
    }

    /**
     * Sets the fraction lost value.
     *
     * @param fractionLost The fraction lost value
     */
    public void setFractionLost(double fractionLost) {
	this.fractionLost = fractionLost;
    }
    
    /**
     * Gets the cumulative number of packets lost. <br>
     * <br>
     * The total number of RTP data packets from source SSRC_n that
     * have been lost since the beginning of reception. This number is
     * defined to be the number of packets expected less the number of
     * packets actually received, where the number of packets received
     * includes any which are late or duplicates. Thus packets that
     * arrive late are not counted as lost, and the loss may be
     * negative if there are duplicates.  The number of packets
     * expected is defined to be the extended last sequence number
     * received less the initial sequence number received.
     *
     * @return The cumulative number of packets lost
     */
    public long getCumulativeNumberOfPacketsLost() {
	return cumulativeNumberOfPacketsLost;
    }

    /**
     * Sets the cumulative number of packets lost.
     *
     * @param nbr The cumulative number of packets lost
     */
    public void setCumulativeNumberOfPacketsLost(long nbr) {
	cumulativeNumberOfPacketsLost = nbr;
    }
    
    /**
     * Gets the extended highest sequence number received. <br>
     * <br>
     * The low 16 bits contain the highest sequence number received in
     * an RTP data packet from source SSRC_n, and the most significant
     * 16 bits extend that sequence number with the corresponding count
     * of sequence number cycles
     *
     * @return The extended highest sequence number received.
     */
    public long getExtendedHighestSequenceNumberReceived() {
	return extendedHighestSequenceNumberReceived;
    }

    /**
     * Sets the extended highest sequence number received.
     *
     * @param seqNo The extended highest sequence number received
     */
    public void setExtendedHighestSequenceNumberReceived(long seqNo) {
	this.extendedHighestSequenceNumberReceived = seqNo;
    }

    /**
     * Gets the interarrival jitter value. <br>
     * <br>
     * An estimate of the statistical variance of the RTP data packet
     * interarrival time, measured in timestamp units and expressed as
     * an unsigned integer. The interarrival jitter J is defined to be
     * the mean deviation (smoothed absolute value) of the difference D
     * in packet spacing at the receiver compared to the sender for a
     * pair of packets. 
     *
     * @return The interarrival jitter value
     */
    public double getInterarrivalJitter() {
	return interarrivalJitter;
    }

    /**
     * Sets the interarrival jitter value.
     *
     * @param jitter The interarrival jitter value
     */
    public void setInterarrivalJitter(double jitter) {
	this.interarrivalJitter = jitter;
    }
    
    /**
     * Gets the middle 32 bits out of 64 in the NTP timestamp 
     * received as part of the most recent RTCP sender
     * report (SR) packet from source SSRC_n.  If no SR has been
     * received yet, the field is set to zero.
     *
     * @return A part of NTP timestamp from the most recent RTCP sender report
     */
    public double getLastSr() {
	return lastSr;
    }
    
    /**
     * Sets a part of NTP timestamp from the most recent RTCP sender report. 
     * <br><br>
     * See getLastSr() for more information.
     *
     * @param lastSr A part of last SR
     */
    public void setLastSr(double lastSr) {
	this.lastSr = lastSr;
    }

    /**
     * Gets the delay, expressed in units of 1/65536 seconds, between
     * receiving the last SR packet from source SSRC_n and sending this
     * reception report block.  If no SR packet has been received yet
     * from SSRC_n, the DLSR field is set to zero.
     *
     * @return The delay
     */
    public long getDelayLastSr() {
	return delayLastSr;
    }

    /**
     * Sets the delay between receiving the last SR packet and sending this
     * reception report block. See getDelayLastSr() for more information.
     *
     * @param delay The delay
     */
    public void setDelayLastSr(long delay) {
	this.delayLastSr = delay;
    }

}
