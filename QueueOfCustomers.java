import java.util.LinkedList;
import java.util.Queue;

public class QueueOfCustomers {
    private Queue<Customer> recipients;
    
    public QueueOfCustomers() {
        recipients = new LinkedList<>();
    }
    
    public boolean enqueueRecipient(Customer recipient) {
        return recipients.offer(recipient);
    }
    
    public boolean dequeueRecipient() {
        return recipients.poll() != null;
    }
    
    public boolean evaluateRecipient(Customer recipient) {
        return recipients.contains(recipient);
    }
    
    public Queue<Customer> getQueueContents() {
        return new LinkedList<>(recipients);
    }
}