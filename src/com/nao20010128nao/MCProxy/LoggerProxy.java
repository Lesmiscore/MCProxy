package com.nao20010128nao.MCProxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.Queue;

public class LoggerProxy implements Runnable {
	// Inserts packet(s) for server->client (PM->MCPE)
	public static final int INSERT_PACKET_STC = 0;
	// Inserts packet(s) for client->server (MCPE->PM)
	public static final int INSERT_PACKET_CTS = 1;

	Queue<byte[]> stcPackets, ctsPackets;
	PacketInserter pi = new PacketInserter();

	String serverIp = "127.0.0.1";
	int serverPort = 19133;
	int proxyPort = 19132;
	DatagramSocket ds;
	String bindIp = "";
	int bindPort = -1;

	public LoggerProxy(String ip, int port, int proxPort) {
		serverIp = ip;
		serverPort = port;
		proxyPort = proxPort;
		stcPackets = new LinkedList<>();
		ctsPackets = new LinkedList<>();
	}

	@Override
	public void run() {

		System.out.println(serverIp);
		System.out.println(serverPort);
		System.out.println(proxyPort);

		try {
			serverIp = InetAddress.getByName(serverIp).getHostAddress();

			ds = new DatagramSocket(proxyPort);

			byte buffer[] = new byte[4 * 1024 * 1024];
			while (!Thread.currentThread().isInterrupted()) {
				try {
					DatagramPacket dp = new DatagramPacket(buffer,
							buffer.length);
					ds.receive(dp);
					SocketAddress sockAddress = dp.getSocketAddress();

					byte received[] = new byte[dp.getLength()];
					byte tmp[] = dp.getData();
					for (int i = 0; i < received.length; i++)
						received[i] = tmp[i];

					String clientInfo = sockAddress.toString();
					String ip = clientInfo
							.substring(1, clientInfo.indexOf(":"));
					int port = Integer.valueOf(clientInfo.substring(clientInfo
							.indexOf(":") + 1));

					System.out.println(ip + ":" + port);

					if (bindPort == -1 & !ip.equals(serverIp)) {
						bindIp = ip;
						bindPort = port;
						System.out.println("bind: " + ip + ":" + port);
					}

					byte[] buf = received;
					dump(buf);
					DatagramPacket senddp;
					if (ip.equals(serverIp)) {
						System.out.println("S->C");
						senddp = new DatagramPacket(buf, buf.length,
								InetAddress.getByName(bindIp), bindPort);
					} else {
						System.out.println("C->S");
						senddp = new DatagramPacket(buf, buf.length,
								InetAddress.getByName(serverIp), serverPort);
					}
					ds.send(senddp);
				} catch (Exception exx) {
					exx.printStackTrace();
					System.out.println("Error!!");
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Error!");
		}
	}

	public void insertPacket(byte[] pack, int dest) {
		Queue<byte[]> toPut;
		switch (dest) {
		case INSERT_PACKET_STC:
			toPut = stcPackets;
			break;
		case INSERT_PACKET_CTS:
			toPut = ctsPackets;
			break;
		default:
			throw new IllegalArgumentException("Invalid dest: " + dest);
		}
		toPut.add(pack);
		if (!pi.isAlive()) {
			(pi = new PacketInserter()).start();
		}
	}

	void dump(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length * 3);
		StringBuilder sb2 = new StringBuilder(b.length);
		for (int i = 0; i < b.length; i++) {
			sb.append(Character.forDigit(b[i] >> 4 & 0xF, 16));
			sb.append(Character.forDigit(b[i] & 0xF, 16));
			sb.append(' ');
			sb2.append((char) b[i]);
		}
		System.out.println(sb.toString());
		System.out.println(sb2.toString());
	}

	class PacketInserter extends Thread {
		@Override
		public void run() {
			// TODO: Implement this method
			try {
				while (!isInterrupted()) {
					try {
						if (ds == null) {
							continue;// Connection is not ready
						}
						if (bindPort == -1) {
							continue;// The game is not ready
						}
						if (stcPackets.size() != 0) {
							// STC
							byte[] pack = stcPackets.poll();
							if (pack == null) {
								continue;
							}
							DatagramPacket dp = new DatagramPacket(pack,
									pack.length,
									InetAddress.getByName("localhost"),
									bindPort);
							ds.send(dp);
						}
						if (ctsPackets.size() != 0) {
							// CTS
							byte[] pack = ctsPackets.poll();
							if (pack == null) {
								continue;
							}
							DatagramPacket dp = new DatagramPacket(pack,
									pack.length,
									InetAddress.getByName(serverIp), serverPort);
							ds.send(dp);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (Throwable e) {

			}
		}
	}

	public static interface PacketHandler {
		public void onSTCPacket(byte[] a);

		public void onCTSPacket(byte[] b);
	}
}
