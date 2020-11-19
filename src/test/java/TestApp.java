import com.google.gson.Gson;

import javax.sound.sampled.*;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class TestApp {

	public static void main(String[] args) throws IOException {

		Gson gson = new Gson();
		Scanner scanner = new Scanner(System.in);

		Thread listenThread;
		Thread sendThread;

		try {
			Client client = Client.getInstance();
			
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
										scanner.nextLine();
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
										System.out.println("Enter usernames.");
										ArrayList<String> users = new ArrayList<String>();
										while(true){
											System.out.print("Add user> ");
											String user = scanner.next();
											if(user.equals("done"))
												break;

											users.add(user);
										}
										String[] rec = new String[users.size()];
										for(int i = 0; i < users.size(); i++){
											rec[i] = users.get(i);
										}
										scanner.nextLine();
										System.out.println("users added: " + users.toString());
										System.out.print("Enter group name> ");
										String grpName = scanner.nextLine();
										System.out.println("group name set to: " + grpName);

										while(true){
											System.out.print("to" + grpName + "> ");
											String message = scanner.nextLine();
											if(message.equals("done")){
												break;
											}

											client.sendToGroup(rec, message, grpName);
										}

									} else if (input.equals("get online users")) {
											client.requestOnlineUsers();
									} else if (input.equals("get all users")) {
										client.requestAllUsers();
									}
									else if (input.equals("get all posts")) {
										client.requestAllPosts();
									}
									else if(input.equals("set status")){
										System.out.println("0 - online");
										System.out.println("1 - busy");
										System.out.println("2 - away");
										System.out.print("status> ");
										int status_code = scanner.nextInt();
										scanner.nextLine();
										client.setStatus(status_code);
									}
									else if(input.equals("set bio")){
										System.out.print("Enter bio: ");
										String bio = scanner.nextLine();
										client.setBio(bio);
									}
									else if(input.equals("set avatar")){
										System.out.print("Enter bio: ");
										String avatar = scanner.next();
										scanner.nextLine();
										client.setBio(avatar);
									}
									else if(input.equals("new post")){
										System.out.print("Enter Title: ");
										String title = scanner.next();
										scanner.nextLine();
										System.out.print("Enter Body: ");
										String body = scanner.nextLine();
										Post post = new Post(title, client.getUser().getUsername(),new Date(), body);
										client.submitPost(post);
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
										AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(this.getClass().getResource("notification.wav"));
										Clip clip = AudioSystem.getClip();
										clip.open(audioInputStream);
										clip.start();
										System.out.println(msg.from + ": " + msg.message);
								}
								else if(msg.type.equals("MSG-TEXT") && msg.subject.contains("user_to_group")){
									AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(this.getClass().getResource("notification.wav"));
									Clip clip = AudioSystem.getClip();
									clip.open(audioInputStream);
									clip.start();
									String grpName = msg.subject.replace("user_to_group:", "");

									System.out.println(grpName+ ": " + msg.message);
								}
								else if(msg.type.equals("MSG-RESULT")){
									if(msg.subject.equals("online_users") || msg.subject.equals("all_users")){
										User[] online_users = gson.fromJson(msg.message, User[].class);

										for(User user: online_users){
											System.out.println(user.toString() + " ");
										}
									}
									else if(msg.subject.equals("all_posts")){
										Post[] posts = gson.fromJson(msg.message, Post[].class);
										System.out.println("---ALL POSTS---");
										for(Post post: posts){
											System.out.println(post.toString() + " ");
										}
									}
								}
								else if(msg.type.equalsIgnoreCase("MSG-NOTIFY")){
									if(msg.subject.equalsIgnoreCase("user_status_change")){
										System.out.println(msg.from + " changed status to " + msg.message);
									}
									else if(msg.subject.equalsIgnoreCase("user_bio_change")){
										System.out.println(msg.from + " changed bio to " + msg.message);
									}
									else if(msg.subject.equalsIgnoreCase("user_avatar_change")){
										System.out.println(msg.from + " changed avatar to " + msg.message);
									}
								}

								else{
									System.out.println("Unknown message: " + line);
								}

							} 
						} catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
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
