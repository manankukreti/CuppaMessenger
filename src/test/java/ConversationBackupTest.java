import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConversationBackupTest {
    static List<Conversation> convos = new ArrayList<>();
    static Path backupFile = Path.of("backup.cuppa");

    public static void main(String[] args) throws IOException {

        loadConvo();
        Instant now = Instant.now();
        printConvo();
        Instant end = Instant.now();
        System.out.println("time taken: " + Duration.between(now, end));
    }

    public static void saveConvo() throws IOException {
        Gson gson = new Gson();
        Files.writeString(backupFile, gson.toJson(convos));
    }

    public static void loadConvo() throws IOException {
        Gson gson = new Gson();
        convos = Arrays.asList(gson.fromJson(Files.readString(backupFile), Conversation[].class));
    }

    public static void printConvo(){
        int i = 0;
        for(Conversation convo : convos){
            System.out.println("--- CONVO ---");
            for(Message msg : convo.getMessages()){
                System.out.println(msg.from + ": " + msg.message);
                i++;
            }
            System.out.println("");
        }
        System.out.print("total lines: " + i + "+");
        System.out.println("--- END OF CONVOS ---");
    }

    public static void  creatConvo(){
        int rndAmt = (int) (Math.random() * 20);
        for(int i = 0; i < 20; i++){
            addRandomConvo();
        }
    }

    public static void addRandomConvo(){

        Conversation convo = new Conversation(new String[]{"manan", "shivain"});
        int rndAmt = (int) (Math.random() * 500);
        for(int i = 0; i < 500; i++){
            Message msg = new Message("shivain", "manan", "MSG-TEXT", "user_to_user", "hello how are you this is a standard message");
            convo.addMessage(msg);
        }

        convos.add(convo);
    }
}
