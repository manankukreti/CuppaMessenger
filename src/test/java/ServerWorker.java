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
	Gson gson = new Gson();
	
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
			heartbeat.cancel();
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
				try {
					removeThisUser();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 10000);
	}

	private void resetHeartbeatTimer(){
		heartbeat.cancel();
		countdownHeartbeatTimer();
	}

	protected void removeThisUser() throws IOException {
		if(isAuth) server.removeUser(user);
		server.removeClient(workerId);
		client.close();
	}

	protected String getUsername(){
		return user.getUsername();
	}

	//send data to the client being handled by this worker
	protected void send(Message msg) throws IOException {
		DataOutputStream dout = new DataOutputStream(out);

		try{
			dout.writeUTF(gson.toJson(msg) + "\n");
			dout.flush();
		}
		catch(Exception ex){
			System.out.println(ex.getMessage());
		}

	}

	private void authenticateUser() throws IOException{
		DataInputStream input = new DataInputStream(in);

		Message msg;

		//send client welcome message
		send(new Message("server", user.getUsername(), "server_to_client", "welcome_message", "success"));


		String line;
		try {
			while ((line = input.readUTF()) != null) {
				msg = (Message) gson.fromJson(line, Message.class);


				if (msg.type.equals("MSG-ARRAY")) {
					String[] credentials = gson.fromJson(msg.message, String[].class);
					User this_user = server.authenticateCredentials(credentials[0], credentials[1]);

					if (this_user != null) {
						isAuth = true;
						user = this_user;

						send(new Message("server", user.getUsername(), "MSG-RESULT", "login_credentials", gson.toJson(this_user)));
						server.addUser(this_user);
						send(new Message("server", user.getUsername(), "MSG-RESULT", "all_users", server.getAllUsers()));
						send(new Message("server", user.getUsername(), "MSG-RESULT", "all_posts", server.getAllPosts()));
						server.releasePendingMessages(user.getUsername());
						break;
					} else {
						send(new Message("server", user.getUsername(), "MSG-RESULT", "login_credentials", "fail"));
					}
				}
				resetHeartbeatTimer();
			}
		}catch(IOException e){
			removeThisUser();
		}

		
		if(isAuth)
			listenForClientRequests();
	}

	//listen for the requests being made by the client and call methods to handle the request
	private void listenForClientRequests() throws IOException {

		DataInputStream input = new DataInputStream(in);
		String line;
		Message msg;

		try {
			while ((line = input.readUTF()) != null) {

				msg = (Message) gson.fromJson(line, Message.class);
				resetHeartbeatTimer();

				//if heartbeat skip
				if (msg.subject.equalsIgnoreCase("hearbeat")) {
					continue;
				}

				//send message to another user
				if (msg.subject.equals("user_to_user") && !msg.message.equalsIgnoreCase("online_users")) {
					server.sendToClient(msg.to, msg);
				}
				//send message to group
				else if (msg.type.equals("MSG-TEXT") && msg.subject.contains("user_to_group")) {
					server.sendToGroup(msg);
				}
				//send user list of online users
				else if (msg.message.equalsIgnoreCase("online_users")) {

					Message info = new Message("server", msg.from, "MSG-RESULT", "online_users", server.getOnlineUsers());
					send(info);
				}
				//send user list of all registered users
				else if (msg.message.equalsIgnoreCase("all_users")) {

					Message info = new Message("server", msg.from, "MSG-RESULT", "all_users", server.getAllUsers());
					send(info);
				}
				//change users status
				else if (msg.subject.equalsIgnoreCase("set_status")) {
					user.setStatus(msg.message);
					server.broadcastNotify(getUsername(), "user_status_change", msg.message);
				}
				//change users bio
				else if(msg.subject.equals("set_bio")){
					server.updateUserInformation(user.getUsername(), "bio", msg.message);
					user.setBio(msg.message);
					server.broadcastNotify(user.getUsername(), "user_bio_change", msg.message);
				}
				//change users avatar
				else if(msg.subject.equals("set_avatar")){
					server.updateUserInformation(user.getUsername(), "avatar", msg.message);
					user.setBio(msg.message);
					server.broadcastNotify(user.getUsername(), "user_avatar_change", msg.message);
				}
				else if(msg.type.equals("MSG-POST")){
					if(msg.subject.equals("new_post")){
						Post post = gson.fromJson(msg.message, Post.class);
						server.addNewPost(post);
						server.broadcastNotify(user.getUsername(), "user_new_post", msg.message);
					}
				}
				//disconnect from the server
				else if (msg.message.equalsIgnoreCase("quit")) {
					out.write("disconnecting... \n".getBytes());
					break;
				}

			}
		}
		catch(IOException e){
			removeThisUser();
		}

		removeThisUser();
	}
}
