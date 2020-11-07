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

	public static void main(String args[]) {

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


						while(true) {
							System.out.println(client.isAuth);
							if(client.isAuth){
								System.out.print("Home>");
								String input = scanner.nextLine();

								if(input.equals("send message")){
									System.out.print("Enter friends username: ");
									String friend = scanner.next();
									while(true){
										System.out.print("to " + friend + ">");
										String msg = scanner.nextLine();
										if(msg.equals("to home")){
											break;
										}
										else{
											try {
												client.sendToUser(friend, msg);
											} catch (IOException e) {
												e.printStackTrace();
											}
										}
									}
								}
								else if(input.equals("send group message")){
									
								}
								else if(input.equals("get online users")){
									try {
										client.send(new Message(client.user.username, "server", "MSG-REQ", "general info", "online_users"));
									} catch (IOException e) {
										e.printStackTrace();
									}
								}

							}

						}
					}
				};

				//THREAD LISTENING TO SERVER
				listenThread = new Thread() {
					public void run() {
						DataInputStream din = new DataInputStream(client.in);
						String line;
						Message msg;

						try {
							while((line = din.readUTF()) != null) {
								msg = gson.fromJson(line, Message.class);

								if(msg.subject.equals("welcome_message")){
									System.out.println("Connected to the chat!");
									System.out.print("Enter username: ");
									client.user.username = scanner.next();;
									System.out.print("Enter password: ");
									String password = scanner.next();
									String[] credentials = {client.user.username, password};
									client.send(new Message(client.user.username, "server", "MSG-ARRAY", "login_username", gson.toJson(credentials)));
								}
								else if(msg.subject.equals("login_credentials")){

									if(msg.message.equals("fail")){
										System.out.println("Incorrect credentials :(");
										System.out.print("Enter username: ");
										client.user.username = scanner.next();;
										System.out.print("Enter password: ");
										String password = scanner.next();
										String[] credentials = {client.user.username, password};
										client.send(new Message(client.user.username, "server", "MSG-ARRAY", "login_username", gson.toJson(credentials)));
									}
									else if(msg.message.equals("success")){
										System.out.print("Login successful!");
										client.isAuth = true;
										sendThread.start();
									}
								}
								else if(msg.subject.equals("user_to_user") && msg.type.equals("MSG-TEXT")){
									System.out.println(msg.from + ": " + msg.message);
								}
								else if(msg.subject.equals("online_users")){
									User[] online_users = gson.fromJson(msg.message, User[].class);

									for(User user: online_users){
										System.out.println(user.username + " ");
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
