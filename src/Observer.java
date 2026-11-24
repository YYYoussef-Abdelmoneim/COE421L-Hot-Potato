// Observer pattern: any class that wants to get messages from a board implements this.
public interface Observer {
    void update(String message);
}
