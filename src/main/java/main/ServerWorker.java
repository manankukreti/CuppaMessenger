package main;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class ServerWorker extends Thread {

	private final String workerId;
	private final Server server;
	private final Socket client;
	private User user;
	private Timer heartbeat;
	private final InputStream in;
	private final OutputStream out;
	private boolean isAuth;
	
	protected ServerWorker(String workerId, Server server, Socket client) throws IOException {
		this.workerId = workerId;
		this.server = server;
		this.client = client;
		user = new User();
		in = client.getInputStream();
		out = client.getOutputStream();
		isAuth = false;
	}

	protected String getWorkerId(){
		return workerId;
	}

	public void run() {
		try {
			countdownHeartbeatTimer();
			authenticateUser();
			System.out.println("Stopping client thread.");
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private void countdownHeartbeatTimer(){
		heartbeat = new Timer();
		heartbeat.schedule(new TimerTask(){

			@Override
			public void run() {
				System.out.println("Client " + user.username + " has gone offline.");
				removeThisUser();
			}
		}, 10000);
	}

	private void resetHeartbeatTimer(){
		heartbeat.cancel();
		countdownHeartbeatTimer();
	}

	protected void removeThisUser(){
		if(isAuth) server.removeUser(user);
		server.removeClient(workerId);
	}

	protected String getUsername(){
		return user.username;
	}

	protected void send(Message msg) throws IOException {
		DataOutputStream dout = new DataOutputStream(out);
		Gson gson = new Gson();

		dout.writeUTF(gson.toJson(msg) + "\n");
		dout.flush();
	}

	private void authenticateUser() throws IOException{
		DataInputStream input = new DataInputStream(in);
		DataOutputStream dout = new DataOutputStream(out);
		Gson gson = new Gson();
		Message msg;

		//send client welcome message
		send(new Message("server", user.username, "server_to_client", "welcome_message", "success"));


		String line;
		while((line = input.readUTF()) != null) {
			msg = (Message) gson.fromJson(line, Message.class);


			if(msg.type.equals("MSG-ARRAY") ){
				String[] credentials = gson.fromJson(msg.message, String[].class);
				User this_user = server.authenticateCredentials(credentials[0], credentials[1]);

				if(this_user != null){
					isAuth = true;
					user = this_user;
					send(new Message("server", user.username, "MSG-RESULT", "login_credentials", "success"));
					server.addUser(this_user);
					System.out.println("success login");
					break;
				}
				else{
					send(new Message("server", user.username, "MSG-RESULT", "login_credentials", "fail"));
				}
			}
			resetHeartbeatTimer();
		}

		
		if(isAuth)
			listenForClientRequests();
	}
	
	private void listenForClientRequests() throws IOException {
		System.out.println(user.username + " logged in to the server");
		DataInputStream input = new DataInputStream(in);
		String line;
		Gson gson = new Gson();
		Message msg;

		while((line = input.readUTF()) != null) {

			msg = (Message) gson.fromJson(line, Message.class);
			resetHeartbeatTimer();

			//if heartbeat
			if(msg.subject.equalsIgnoreCase("hearbeat")){
				continue;
			}

			if(msg.subject.equals("user_to_user") && !msg.message.equalsIgnoreCase("online_users")){
				server.sendToClient(msg);
			}
			else if(msg.type.equals("user_to_group")){
				server.sendToGroup(msg);
			}
			else if(msg.message.equalsIgnoreCase("online_users")){

				Message info = new Message("server", msg.from, "MSG-RESULT", "online_users", server.getOnlineUsers() );

				send(info);
			}
			else if(msg.message.equalsIgnoreCase("quit")) {
				out.write("disconnecting... \n".getBytes());
				break;
			}

		}

		removeThisUser();
		client.close();
	}
}
