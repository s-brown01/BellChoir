package bellchoir;

import javax.sound.sampled.SourceDataLine;

public class ChoirMember implements Runnable {
    
    private final Note note;
    private final Thread thread;
    private final SourceDataLine audio;
    private volatile boolean isPlaying;
    private final boolean myTurn;
    
    public ChoirMember(Note note, SourceDataLine audio, String name) {
        this.note = note;
        this.audio = audio;
        this.isPlaying = false;
        myTurn = false;
        
        this.thread = new Thread(this, name);
    }
    
    public Note getNote() {
        return note;
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
        synchronized (this) {
            while (isPlaying) {
                if (myTurn) {
                
                }
            }
        }
    }
    
    public void stop() {
        this.isPlaying = false;
        synchronized (this) {
            notify();
        }
        System.out.println(thread.getName() + " stopped");
    }
    
    private void playNote(Note note, NoteLength length) {
        final int ms = Math.min(length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length_time = Note.SAMPLE_RATE * ms / 1000;
        audio.write(note.sample(), 0, length_time);
        audio.write(Note.REST.sample(), 0, 50);
    }
    
}
