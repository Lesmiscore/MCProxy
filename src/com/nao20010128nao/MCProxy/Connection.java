package com.nao20010128nao.MCProxy;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Connection {
	String serverAddress = "localhost";
	int serverPort = 25565;

	int bindPort = 25566;

	private DatagramSocket socket = null;

	public Connection(String address, int port) {
		serverAddress = address;
		serverPort = port;
	}

	public byte[] sendUdpForReceive(byte[] input) throws IOException {
		sendUdp(input);
		return receive();
	}

	public void sendUdp(byte[] input) throws IOException {
		while (socket == null) {
			try {
				socket = new DatagramSocket(bindPort);
			} catch (BindException e) {
				++bindPort;
			}
		}
		InetAddress address = InetAddress.getByName(serverAddress);
		DatagramPacket packet1 = new DatagramPacket(input, input.length,
				address, serverPort);
		socket.send(packet1);
	}

	public byte[] receive() throws IOException {
		while (socket == null) {
			try {
				socket = new DatagramSocket(bindPort);
			} catch (BindException e) {
				++bindPort;
			}
		}
		byte[] out = new byte[1024 * 100];
		DatagramPacket packet = new DatagramPacket(out, out.length);
		socket.receive(packet);
		byte[] recv = new byte[packet.getLength()];
		byte tmp[] = packet.getData();
		for (int i = 0; i < recv.length; i++)
			recv[i] = tmp[i];
		return recv;
	}

	@Override
	public void finalize() {
		socket.close();
	}
}
