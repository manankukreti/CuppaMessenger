

import ch.qos.logback.classic.LoggerContext;
import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;

import java.util.*;

public class Server {

	private final ServerSocket socket;
	private boolean listenForClients;
	private final Gson gson = new Gson();

	//Server Lists
	private final List<ServerWorker> clientList = new ArrayList<>();
	private final List<User> userList = new ArrayList<>();
	private final HashMap<String, PendingMessages> pendingMessages =  new HashMap<>();

	//Database Collections
	private static MongoCollection<Document> userCollection;
	private static MongoCollection<Document> postCollection;


	public Server() throws IOException {
		listenForClients = true;
		socket = new ServerSocket(5000);

		MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
		MongoDatabase database = mongoClient.getDatabase("chatapp");
		userCollection = database.getCollection("users");
		postCollection = database.getCollection("posts");

		//TURN OFF MONGODB LOGS
		((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.mongodb.driver").setLevel(Level.ERROR);
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
			broadcastNotify(user.getUsername(), "user_status_change", "online");
		}
	}

	protected void removeUser(User user) throws IOException {
		userList.remove(user);
		broadcastNotify(user.getUsername(), "user_status_change", "offline");
	}

	protected List<User> getOnlineUsers(){

		return userList;
	}

	protected List<User> getAllUsers(){
		List<User> users = new ArrayList<>();
		for (Document userDoc : userCollection.find()) {
			String username = userDoc.get("username", String.class);
			User userToAdd = new User(username, userDoc.get("fullName", String.class), userDoc.get("jobTitle", String.class), userDoc.get("bio", String.class), userDoc.get("avatar", String.class));

			userToAdd.setStatus(getUserStatus(username));
			users.add(userToAdd);
		}

		return users;
	}

	protected User getUser(String username){
		Document userDoc = userCollection.find(new Document("username", username)).first();

		if(userDoc == null)
			return null;

		return new User(userDoc.get("username", String.class), userDoc.get("fullName", String.class), userDoc.get("jobTitle", String.class), userDoc.get("bio", String.class), userDoc.get("avatar", String.class));
	}

	public String getUserStatus(String username){
		for(User user : userList){
			if(user.getUsername().equals(username))
				return user.getStatus();
		}

		return "offline";
	}

	protected void sendToClient(String username, Message msg) throws IOException {
		String to = msg.to;

		ServerWorker recipientWorker = getServerWorker(username);

		if(recipientWorker != null){
			recipientWorker.send(msg);
		}
		else{
			addToPendingMessages(to, msg);
		}
	}

	protected void sendToGroup(Message msg) throws IOException{
		String[] recipients = gson.fromJson(msg.to, String[].class);

		for(String recipient: recipients){
			ServerWorker sv = getServerWorker(recipient);
			if(sv == null){
				System.out.println(recipient + " has pending");
				addToPendingMessages(recipient, msg);
			}
			else{
				sv.send(msg);
			}
		}
	}

	protected void updateUserInformation(String username, String field, String newValue){
		userCollection.updateOne(Filters.eq("username", username), Updates.set(field, newValue));
	}

	protected void broadcastNotify(String from, String subject, String message) throws IOException{
		for(User user : userList){
			Message notify = new Message(from, user.getUsername(), "MSG-NOTIFY", subject, message);
			Objects.requireNonNull(getServerWorker(user.getUsername())).send(notify);
		}
	}

	private void addToPendingMessages(String to, Message msg){
		if(pendingMessages.containsKey(to)){
			pendingMessages.get(to).addMessage(msg);
		}
		else{
			PendingMessages messages = new PendingMessages(to);
			messages.addMessage(msg);
			messages.displayPending();
			pendingMessages.put(to, messages);
		}
	}

	protected void releasePendingMessages(String recipient) throws IOException {

		if(pendingMessages.containsKey(recipient)) {

			PendingMessages pm = pendingMessages.get(recipient);
			System.out.println(recipient + " has pending messages " + pm.getSize());
			while(pm.getSize() != 0){
				sendToClient(recipient, pm.removeMessage());
			}
			pendingMessages.remove(recipient);
		}
	}

	protected void removeClient(String workerId) {
		clientList.removeIf(worker -> worker.getWorkerId().equals(workerId));
	}

	private boolean addNewAccount(String fullName, String username, String password, String jobTitle, String bio, String avatar){

		//return false if the username already exists

		if(userCollection.find(new Document("username", username)).first() != null)
			return false;

		Document new_user = new Document("_id", new ObjectId());
		new_user.append("username", username).append("fullName", fullName).append("password", hashPassword(password)).append("jobTitle", jobTitle).append("bio", bio).append("avatar", avatar);
		userCollection.insertOne(new_user);

		return true;
	}

	public boolean removeAccount(String username){
		if(userCollection.find(new Document("username", username)).first() == null)
			return false;

		userCollection.deleteOne(new Document("username", username));
		return true;
	}

	public boolean changePassword(String username, String oldPass, String newPass){
		System.out.println(oldPass +" " + newPass);
		Document user = userCollection.find(new Document("username", username)).first();
		if(user == null)
			return false;

		String hashedPass = user.get("password", String.class);
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

		if(bc.matches(oldPass, hashedPass)){
			String newHashPass = hashPassword(newPass);
			updateUserInformation(username, "password", newHashPass);
			return true;
		}
		else{
			System.out.println("password does not match");
		}

		return false;
	}

	private String hashPassword(String pw){
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		return bc.encode(pw);
	}

	protected User authenticateCredentials(String username, String password){
		BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
		Document usernameDocument = userCollection.find(new Document("username", username)).first();

		if(usernameDocument == null)
			return null;

		String storedpw = usernameDocument.get("password", String.class);

		if(bc.matches(password, storedpw)){
			return new User(username, usernameDocument.get("fullName", String.class), usernameDocument.get("jobTitle", String.class), usernameDocument.get("bio", String.class), usernameDocument.get("avatar", String.class));
		}

		return null;
	}

	protected void addNewPost(Post post){
		Document new_post = new Document("_id", new ObjectId());
		new_post.append("title", post.getTitle()).append("author", post.getAuthor().getUsername()).append("date", gson.toJson(post.getDate())).append("body", post.getBody());
		postCollection.insertOne(new_post);
	}

	protected String getAllPosts(){
		List<Post> posts = new ArrayList<>();
		for (Document postDoc : postCollection.find()) {
			Post postToAdd = new Post(postDoc.get("title", String.class), getUser(postDoc.get("author", String.class)), gson.fromJson(postDoc.get("date", String.class), Date.class), postDoc.get("body", String.class));

			posts.add(postToAdd);
		}

		return gson.toJson(posts);
	}

	public void closeServer() throws IOException {
		socket.close();
	}

	public static void main(String[] args) throws IOException {

		Thread serverManager = new Thread(new Runnable() {

			final Server server = new Server();
			boolean isServerRunning = false;
			final Scanner scanner = new Scanner(System.in);
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
						System.out.print("Enter full name: ");
						String fullName = scanner.nextLine();
						System.out.print("Enter username: ");
						String username = scanner.next();
						System.out.print("Enter password: ");
						String password = scanner.next();
						//consume line character
						scanner.nextLine();
						System.out.print("Enter job title: ");
						String job = scanner.nextLine();
						System.out.print("Enter bio: ");
						String bio = scanner.nextLine();
						System.out.print("Enter avatar: ");
						String avatar = scanner.next();
						scanner.nextLine();
						server.addNewAccount(fullName, username, password, job, bio, avatar);

					}
					else if(cmd.equalsIgnoreCase("remove account")){
						System.out.print("Enter username: ");
						String username = scanner.next();
						scanner.nextLine();
						server.removeAccount(username);

					}

				}
			}
		});

		serverManager.start();


	}

}

