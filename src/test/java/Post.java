import java.util.Date;

public class Post {
    private String title;
    private String author;
    private Date date;
    private String body;

    public Post(String title, String author, Date date, String body) {
        this.title = title;
        this.author = author;
        this.date = date;
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

   @Override
    public String toString(){
        return "{Title:" + title + ", Author:"+author+", Date: " + date.toString() +", Body: \""+body+"\"}";
    }
}
