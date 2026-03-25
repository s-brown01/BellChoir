package bellchoir;

public class ChoirMember implements Runnable {
    
    private final Note note1;
    private final Note note2;
    private final Thread thread;
    private volatile boolean isPlaying;
    
    public ChoirMember(Note note1, Note note2, String name) {
        this.note1 = note1;
        this.note2 = note2;
        this.isPlaying = false;
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
        this.isPlaying = true;
        this.thread.start();
    }
    
    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        if (isPlaying) {
            System.out.println(thread.getName() + " started");
        }
    }
    
    public void stop() {
        this.isPlaying = false;
        synchronized (this) {
            notify();
        }
        System.out.println(thread.getName() + " stopped");
    }
    
    private void playNote(){
        // play a note?
    }
    
}
