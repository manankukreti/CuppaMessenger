package main;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class TestApp {

	public static void main(String[] args) throws IOException {

		Gson gson = new Gson();
		Scanner scanner = new Scanner(System.in);

		Thread listenThread;
		Thread sendThread;

		try {
			Client client = new Client();	
			
			try {
				
				//Heartbeat
				Timer heartbeat = new Timer();
				heartbeat.schedule(new TimerTask() {

					@Override
					public void run() {
						try{
							client.pulse();
						}
						catch(IOException ex){
							ex.printStackTrace();
						}
					}
					
				}, 0, 5000);

				//THREAD SENDING INFO TO SERVER
				sendThread = new Thread() {
					public void run() {

						try {

							while (true) {
								if (client.isAuth()) {
									System.out.print("Home>");
									String input = scanner.nextLine();

									if (input.equals("send message")) {
										System.out.print("Enter friends username: ");
										String friend = scanner.next();
										while (true) {
											System.out.print("to " + friend + ">");
											String msg = scanner.nextLine();
											if (msg.equals("to home")) {
												break;
											} else {
												try {
													client.sendToUser(friend, msg);
												} catch (IOException e) {
													e.printStackTrace();
												}
											}
										}
									} else if (input.equals("send group message")) {

									} else if (input.equals("get online users")) {
											client.requestOnlineUsers();
									} else if (input.equals("get all users")) {
										client.requestAllUsers();
									}
									else if(input.equals("set status")){
										System.out.println("0 - online");
										System.out.println("1 - busy");
										System.out.println("2 - away");
										System.out.print("status> ");
										int status_code = scanner.nextInt();
										client.setStatus(status_code);
									}


								}

							}
						}
						catch(Exception ex){

						}
					}
				};

				//THREAD LISTENING TO SERVER
				listenThread = new Thread() {
					public void run() {
						DataInputStream din = new DataInputStream(client.getInputStream());
						String line;
						Message msg;

						try {
							while((line = din.readUTF()) != null) {
								msg = gson.fromJson(line, Message.class);

								if(msg.subject.equals("welcome_message")){
									System.out.println("Connected to the chat!");
									System.out.print("Enter username: ");
									client.getUser().setUsername(scanner.next());
									System.out.print("Enter password: ");
									String password = scanner.next();
									String[] credentials = {client.getUser().getUsername(), password};
									client.send(new Message(client.getUser().getUsername(), "server", "MSG-ARRAY", "login_username", gson.toJson(credentials)));
								}
								else if(msg.subject.equals("login_credentials")){

									if(msg.message.equals("fail")){
										System.out.println("Incorrect credentials :(");
										System.out.print("Enter username: ");
										client.getUser().setUsername(scanner.next());
										System.out.print("Enter password: ");
										String password = scanner.next();
										String[] credentials = {client.getUser().getUsername(), password};
										client.send(new Message(client.getUser().getUsername(), "server", "MSG-ARRAY", "login_username", gson.toJson(credentials)));
									}
									else{
										System.out.print("Login successful!");
										User this_user = gson.fromJson(msg.message, User.class);
										client.setUser(this_user);
										client.setAuth(true);
										sendThread.start();
									}
								}
								else if(msg.subject.equals("user_to_user") && msg.type.equals("MSG-TEXT")){
									System.out.println(msg.from + ": " + msg.message);
								}
								else if(msg.subject.equals("online_users") || msg.subject.equals("all_users")){
									User[] online_users = gson.fromJson(msg.message, User[].class);

									for(User user: online_users){
										System.out.println(user.toString() + " ");
									}
								}
								else if(msg.type.equalsIgnoreCase("MSG-NOTIFY")){
									if(msg.subject.equalsIgnoreCase("user_status_change")){
										System.out.println(msg.from + " changed status to " + msg.message);
									}
								}

								else{
									System.out.println(line);
								}

							} 
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				
				listenThread.start();
				
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}
			
			
			
			
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
	}
}
