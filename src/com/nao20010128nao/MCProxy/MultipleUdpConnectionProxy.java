package com.nao20010128nao.MCProxy;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MultipleUdpConnectionProxy implements Runnable, Closeable {
	static Thread MAIN_THREAD = Thread.currentThread();

	Map<String, Connection> connections = new HashMap<>(10);
	Map<Connection, String> connectionsR = new HashMap<>(10);

	String server_ip = "127.0.0.1";
	int server_port = 19133;
	int proxy_port = 19132;
	DatagramSocket ds;

	Set<Thread> servants = new HashSet<>(10);

	public MultipleUdpConnectionProxy(String ip, int port, int proxPort) {
		server_ip = ip;
		server_port = port;
		proxy_port = proxPort;
	}

	@Override
	public void run() {
		System.out.println(server_ip);
		System.out.println(server_port);
		System.out.println(proxy_port);
		try {
			server_ip = InetAddress.getByName(server_ip).getHostAddress();

			ds = new DatagramSocket(proxy_port);

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
					dump(received);

					String clientInfo = sockAddress.toString();
					String ip = clientInfo
							.substring(1, clientInfo.indexOf(":"));
					int port = Integer.valueOf(clientInfo.substring(clientInfo
							.indexOf(":") + 1));

					System.out.println(ip + ":" + port);

					if (connections.containsKey(ip + ":" + port)) {
						connections.get(ip + ":" + port).sendUdp(received);
					} else {
						System.out.println("New connection for: " + ip + ":"
								+ port);
						final Connection ph = new Connection(server_ip,
								server_port);
						ph.sendUdp(received);
						connections.put(ip + ":" + port, ph);
						connectionsR.put(ph, ip + ":" + port);
						new Thread() {
							@Override
							public void run() {
								Connection local = ph;
								String master = connectionsR.get(local);
								String ip = master.substring(0,
										master.indexOf(":"));
								int port = Integer.valueOf(master
										.substring(master.indexOf(":") + 1));

								System.out.println("conn:" + ip + ":" + port);

								while (!isInterrupted()) {
									try {
										byte[] data = local.receive();
										System.out.println(server_ip + ":"
												+ server_port + ".");
										dump(data);
										DatagramPacket dp = new DatagramPacket(
												data, data.length,
												InetAddress.getByName(ip), port);
										ds.send(dp);
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}
						}.start();
					}
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

	void dump(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length * 3);
		for (int i = 0; i < b.length; i++) {
			sb.append(Character.forDigit(b[i] >> 4 & 0xF, 16));
			sb.append(Character.forDigit(b[i] & 0xF, 16));
			sb.append(' ');
		}
		System.out.println(sb.toString());
		System.out.println(new String(b, StandardCharsets.UTF_8));
	}

	@Override
	public void close() throws IOException {
		// TODO: Implement this method
		for (Thread t : servants) {
			t.interrupt();
		}
		ds.close();
		for (Connection ph : connections.values()) {
			ph.finalize();
		}
	}
}
