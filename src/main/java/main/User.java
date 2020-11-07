package main;
import java.io.Serializable;

public class User implements Serializable {
    String username;
    String jobTitle;
    String bio;
    String bio;
    String status;

    public User(){
        username = "guest";
        jobTitle = "visitor";
        bio = "The visitor has not been identified. Will be treated as a guest.";
        status = "online";
    }
    public User(String username, String job_title, String about) {
        this.username = username;
        this.jobTitle = job_title;
        this.bio = about;
        this.status = "online";
    }

    public void setStatus(int i){
        switch(i){
            case 0:
                status = "offline";
                break;
            case 1:
                status = "online";
                break;
            case 2:
                status = "away";
                break;
            case 3:
                status = "busy";
                break;
        }
    }

}
