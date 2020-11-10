package main;

import com.google.gson.Gson;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Client {
	private User user;
	private final Socket socket;
	private final InputStream in;
	private final OutputStream out;
	private boolean isAuth;
	
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
		send(new Message(user.getUsername(), to, "MSG-TEXT", "user_to_user", msg));
	}

	public void sendToGroup(String[] to, String msg) throws IOException {
		send(new Message(Arrays.toString(to), "", "MSG-TEXT", "user_to_group", msg));
	}

	public void requestAllUsers() throws IOException {
		send(new Message(user.getUsername(), "server", "MSG-REQ", "general", "all_users"));
	}

	public void requestOnlineUsers() throws IOException {
		send(new Message(user.getUsername(), "server", "MSG-REQ", "general", "online_users"));
	}

	public void pulse() throws IOException {
		Message heartbeat = new Message(user.getUsername(), "server", "client_status", "heartbeat", "alive");
		send(heartbeat);
	}

	public InputStream getInputStream(){
		return in;
	}

	public void setUser(User user){
		this.user = user;
		System.out.println(this.user.toString());
	}

	public User getUser(){
		return user;
	}

	public void setAuth(boolean auth){
		isAuth = auth;
	}

	public boolean isAuth(){
		return isAuth;
	}

}
