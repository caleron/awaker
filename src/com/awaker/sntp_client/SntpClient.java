package com.awaker.sntp_client;

import com.awaker.config.Config;
import com.awaker.config.ConfigKey;
import com.awaker.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.ZonedDateTime;


/**
 * http://support.ntp.org/bin/view/Support/JavaSntpClient
 * <p>
 * NtpClient - an NTP client for Java.  This program connects to an NTP server
 * and prints the response to the console.
 * <p>
 * The local clock offset calculation is implemented according to the SNTP
 * algorithm specified in RFC 2030.
 * <p>
 * Note that on windows platforms, the curent time-of-day timestamp is limited
 * to an resolution of 10ms and adversely affects the accuracy of the results.
 * <p>
 * <p>
 * This code is copyright (c) Adam Buckley 2004
 * <p>
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.  A HTML version of the GNU General Public License can be
 * seen at http://www.gnu.org/licenses/gpl.html
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * @author Adam Buckley
 */

public class SntpClient {
    public static ZonedDateTime getTime() {
        try {
            String serverName = Config.getString(ConfigKey.TIME_SERVER);

            // Send request
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(20000);
            InetAddress address = InetAddress.getByName(serverName);
            byte[] buf = new NtpMessage().toByteArray();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 123);

            // Set the transmit timestamp *just* before sending the packet
            NtpMessage.encodeTimestamp(packet.getData(), 40, (System.currentTimeMillis() / 1000.0) + 2208988800.0);

            socket.send(packet);

            // Get response
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            // Process response
            NtpMessage msg = new NtpMessage(packet.getData());

            ZonedDateTime currentZonedDateTime = msg.getCurrentZonedDateTime();
            Log.message("Received current Time: " + currentZonedDateTime.toString());

            socket.close();

            return currentZonedDateTime;
        } catch (IOException e) {
            Log.message("Couldn't fetch Time, unknown host or timeout");
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }
}
