package main;
import java.io.Serializable;

public class User implements Serializable {
    String username;
    String job_title;
    String about;
    String status;

    public User(){
        username = "guest";
        job_title = "visitor";
        about = "The visitor has not been identified. Will be treated as a guest.";
        status = "online";
    }
    public User(String username, String job_title, String about) {
        this.username = username;
        this.job_title = job_title;
        this.about = about;
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
