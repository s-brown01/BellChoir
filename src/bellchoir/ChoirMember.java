package bellchoir;

public class ChoirMember implements Runnable {
    
    private final Note note1;
    private final Note note2;
    private final Thread thread;
    
    public ChoirMember(Note note1, Note note2, String name) {
        this.note1 = note1;
        this.note2 = note2;
        this.thread = new Thread(this, name);
    }
    
    public Note getNote1() {
        return note1;
    }
    
    public Note getNote2() {
        return note2;
    }
    
    public String getName() {
        return thread.getName();
    }
    
    public void start() {
        this.thread.start();
    }
    
    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        System.out.println("Starting thread " + thread.getName());
    }
}
