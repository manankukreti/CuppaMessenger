package main;

import java.util.LinkedList;
import java.util.Queue;

public class PendingMessages {
    String type;
    String recipient;
    String remainingRecipients;
    private final Queue<Message> messages;

    PendingMessages(String type, String recipient){
        this.type = type;
        this.recipient = recipient;
        this.messages = new LinkedList<Message>();
    }

    public void addMessage(Message msg){
        messages.add(msg);
    }

    public Message removeMessage(){
        if(messages.size() != 0)
            return messages.remove();

        return null;
    }

    public int getSize(){
        return messages.size();
    }

    public void displayPending(){
        for(Message msg: messages){
            System.out.println(msg.message);
        }
    }

    public void removeRecipient(String user){

    }
}
