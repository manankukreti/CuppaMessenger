package main;

import java.util.LinkedList;
import java.util.Queue;

public class PendingMessages {
    String recipient;
    private final Queue<Message> messages = new LinkedList<>();

    PendingMessages(String recipient){
        this.recipient = recipient;
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

}
