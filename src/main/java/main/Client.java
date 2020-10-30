package main;

import com.google.gson.Gson;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
	public User user;
	private final Socket socket;
	public InputStream in;
	public OutputStream out;
	public boolean isAuth;
	
	public Client() throws IOException {
		user = new User();
		socket = new Socket("localhost", 5000);
		in = socket.getInputStream();
		out = socket.getOutputStream();
	}
	
	public Client(String ip, int port) throws UnknownHostException, IOException {
		socket = new Socket(ip, port);
		in = socket.getInputStream();
		out = socket.getOutputStream();
	}

	
	public void send(Message msg) throws IOException {
		DataOutputStream dout = new DataOutputStream(out);
		Gson gson = new Gson();
		dout.writeUTF( gson.toJson(msg) + "\n");
		dout.flush();
	}

	public void sendToUser(String to, String msg) throws IOException {
		send(new Message(user.username, to, "MSG-TEXT", "user_to_user", msg));
	}

	public void sendToGroup(String[] to, String msg) throws IOException {
		send(new Message(user.username, "", "MSG-TEXT", "user_to_group", msg));
	}


	public void pulse() throws IOException {
		Message heartbeat = new Message(user.username, "server", "client_status", "heartbeat", "alive");
		send(heartbeat);
	}


}
