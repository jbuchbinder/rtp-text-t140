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
 * Interface which is implemented by classes which are interested in
 * receiving RTCP Packets.
 * Implementation for handleRTPEvent methods must be provided by such classes.
 * The method <b> addRTCP_actionListener() </b> in Session is responsible for
 * registerring the listener. ( Only one RTCP listener class per session. )
 * RTCP packets will be posted only after the class implementing this
 * interface registers itself with the Session. <br>
 * <br>
 * Note: <br>
 * It is assumed that ONE and only one class in an application is implementing
 * this interface is registered with the Session. <br>
 * <br>
 * RTCP LoopBack: <br>
 * RTCP packets originated from this session are not posted.  Self RTCP
 * packets are filtered out and RTCP packets originated by other sources 
 * cause the RTCP events. <br>
 * <br>
 * Future releases might add multiple listener registeration. <br>
 *
 * @author Unknown
 *
 * @see RTCPReceiverReportPacket
 * @see RTCPSenderReportPacket
 * @see RTCPSDESPacket
*/

public interface RTCP_actionListener {
    /**
     * Implementation of this method makes use of the received RTCP Receiver
     * Report Packet.
     *
     * @param rrPkt The received RTCP Receiver report packet.
     */
    public void handleRTCPEvent ( RTCPReceiverReportPacket rrPkt);

    /**
     * Implementation of this method makes use of the received
     * RTCPReceiverReportPacket.
     *
     * @param srPkt The received RTCP Sender report packet.
     */
    public void handleRTCPEvent ( RTCPSenderReportPacket srPkt);

    /**
     * Implementation of this method makes use of the received
     * RTCPReceiverReportPacket.
     *
     * @param sdespkt The received RTCP SDES racket packet.
     */
    public void handleRTCPEvent ( RTCPSDESPacket sdespkt);

    /**
     * Implementation of this method makes use of the received
     * RTCPReceiverReportPacket.
     *
     * @param  byepkt The received BYE packet.
     */
    public void handleRTCPEvent ( RTCPBYEPacket byepkt);
}
