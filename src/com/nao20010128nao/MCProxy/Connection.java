package com.nao20010128nao.MCProxy;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.SecureRandom;

/**
 * A class that handles Minecraft Query protocol requests
 *
 * @author Ryan McCann
 */
public class Connection {
	static SecureRandom sr = new SecureRandom();

	final static byte HANDSHAKE = 9;
	final static byte LOGIN = 1;

	String serverAddress = "localhost";
	int queryPort = 25565; // the default minecraft query port

	int localPort = 25566; // the local port we're connected to the server on

	private DatagramSocket socket = null; // prevent socket already bound
											// exception

	public Connection(String address, int port) {
		serverAddress = address;
		queryPort = port;
	}

	public byte[] sendUdpForReceive(byte[] input) throws IOException {
		sendUdp(input);
		return receive();
	}

	public void sendUdp(byte[] input) throws IOException {
		while (socket == null) {
			try {
				socket = new DatagramSocket(localPort); // create the socket
			} catch (BindException e) {
				++localPort; // increment if port is already in use
			}
		}
		// create a packet from the input data and send it on the socket
		InetAddress address = InetAddress.getByName(serverAddress); // create
																	// InetAddress
																	// object
																	// from the
																	// address
		DatagramPacket packet1 = new DatagramPacket(input, input.length,
				address, queryPort);
		socket.send(packet1);
	}

	public byte[] receive() throws IOException {
		while (socket == null) {
			try {
				socket = new DatagramSocket(localPort); // create the socket
			} catch (BindException e) {
				++localPort; // increment if port is already in use
			}
		}
		byte[] out = new byte[1024 * 100]; // TODO guess at max size
		DatagramPacket packet = new DatagramPacket(out, out.length);
		socket.receive(packet);
		byte[] recv = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), 0, recv, 0, recv.length);
		return recv;
	}

	@Override
	public void finalize() {
		socket.close();
	}
}
