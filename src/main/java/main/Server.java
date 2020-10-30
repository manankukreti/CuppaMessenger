package main;

import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.*;

public class Server {
	private final ServerSocket socket;
	private boolean listenForClients;
	private final List<ServerWorker> clientList = new ArrayList<>();
	private final List<User> userList = new ArrayList<>();
	private static MongoCollection<Document> userCollection;
	private final Gson gson = new Gson();
	private final HashMap<String, PendingMessages> pendingMessages =  new HashMap<>();



	public Server() throws IOException {
		listenForClients = true;
		socket = new ServerSocket(5000);
		MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
		MongoDatabase database = mongoClient.getDatabase("chatapp");
		userCollection = database.getCollection("users");
		System.out.println("Server started on port " + socket.getLocalPort());
	}

	public Server(int port) throws IOException {
		socket = new ServerSocket(port);
	}

	private ServerWorker getServerWorker(String username){
		for(ServerWorker sw : clientList){
			if(sw.getUsername().equals(username))
				return sw;
		}

		return null;
	}

	public void setListenForClients(boolean listen) {
		this.listenForClients = listen;
	}

	public void listenForClients() {
		try {
			while(listenForClients) {
				Socket clientSocket = socket.accept();

				final ServerWorker SERVER_WORKER = new ServerWorker(UUID.randomUUID().toString(),this, clientSocket);
				SERVER_WORKER.start();

				clientList.add(SERVER_WORKER);
				System.out.println("Client added. Total clients: " + clientList.size());
				System.out.println(clientList.toString());
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	protected void addUser(User user) throws IOException {
		if(!userList.contains(user)) {
			userList.add(user);
			releasePendingMessages(user.username);
		}

	}

	protected void removeUser(User user){
		userList.remove(user);
	}

	protected String getOnlineUsers(){
		String onlineUsers = gson.toJson(userList);
		System.out.println(onlineUsers);

		return onlineUsers;
	}

	protected void sendToClient(Message msg) throws IOException {
		String to = msg.to;

		ServerWorker recipientWorker = getServerWorker(to);

		if(recipientWorker != null){
			recipientWorker.send(msg);
		}
		else{
			if(pendingMessages.containsKey(to)){
				pendingMessages.get(to).addMessage(msg);
			}
			else{
				PendingMessages messages = new PendingMessages("regular", to);
				messages.addMessage(msg);
				messages.displayPending();
				pendingMessages.put(to, messages);
			}

		}
	}

	protected void releasePendingMessages(String recipient) throws IOException {
		if(pendingMessages.containsKey(recipient)) {
			PendingMessages pm = pendingMessages.get(recipient);
			while(pm.getSize() != 0){
				sendToClient(pm.removeMessage());

			}
			pendingMessages.remove(recipient);
		}
	}

	protected void removeClient(String workerId) {
		clientList.removeIf(sw -> sw.getWorkerId().equals(workerId));
	}

	public boolean addNewAccount(String username, String password, String jobTitle){

		if(userCollection.find(new Document("username", username)).first() != null)
			return false;

		Document new_user = new Document("_id", new ObjectId());
		new_user.append("username", username).append("password", hashPassword(password)).append("jobTitle", jobTitle);
		userCollection.insertOne(new_user);

		return true;
	}

	protected String hashPassword(String pw){
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		return bc.encode(pw);
	}

	protected User authenticateCredentials(String username, String password) throws IOException {
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		Document usernameDocument = userCollection.find(new Document("username", username)).first();

		if(usernameDocument == null)
			return null;

		String storedpw = (String) usernameDocument.values().toArray()[2];

		if(bc.matches(password, storedpw)){
			return new User(username, (String) usernameDocument.values().toArray()[3], "");
		}

		return null;
	}

	public void closeServer() throws IOException {
		socket.close();
	}

	public static void main(String[] args) throws IOException {

		Thread serverManager = new Thread(new Runnable() {

			Server server = new Server();
			boolean isServerRunning = false;
			Scanner scanner = new Scanner(System.in);
			String cmd;

			@Override
			public void run() {
				while(true){
					System.out.print("Enter command: ");
					cmd = scanner.nextLine();

					if(cmd.equalsIgnoreCase("start server")){
						if(!isServerRunning) {
							isServerRunning = true;
							Thread serverThread = new Thread(new Runnable() {
								@Override
								public void run() {
									server.listenForClients();
								}
							});
							serverThread.start();
						}
						else{
							System.out.println("Server still running.");
						}
					}
					else if(cmd.equalsIgnoreCase("create account")){
						System.out.print("Enter username: ");
						String username = scanner.next();
						System.out.print("Enter password: ");
						String password = scanner.next();
						System.out.print("Enter job title: ");
						String job = scanner.next();

						server.addNewAccount(username, password, job);

					}

				}
			}
		});

		serverManager.start();


	}

}

