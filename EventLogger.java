import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventLogger {
    private static EventLogger instance;
    private static final String EVENT_LOG_FILE = "depot_events.log";
    private static final DateTimeFormatter timeFormatter = 
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    
    private EventLogger() {

    }
    
    public static EventLogger getInstance() {
        if (instance == null) {
            instance = new EventLogger();
        }
        return instance;
    }
    
    public void logEvent(String event) {
        try (FileWriter fileStream = new FileWriter(EVENT_LOG_FILE, true);
             BufferedWriter buffStream = new BufferedWriter(fileStream);
             PrintWriter outputStream = new PrintWriter(buffStream)) {
            
            String timeStamp = LocalDateTime.now().format(timeFormatter);
            outputStream.println(timeStamp + " | " + event);
            
        } catch (IOException ex) {
            System.err.println("Failed to write event to log: " + ex.getMessage());
        }
    }
    
    public String getEventHistory() {
        StringBuilder eventLog = new StringBuilder();
        try (BufferedReader inputStream = new BufferedReader(new FileReader(EVENT_LOG_FILE))) {
            String eventEntry;
            while ((eventEntry = inputStream.readLine()) != null) {
                eventLog.append(eventEntry).append("\n");
            }
        } catch (IOException ex) {
            System.err.println("Failed to retrieve event log: " + ex.getMessage());
        }
        return eventLog.toString();
    }
}