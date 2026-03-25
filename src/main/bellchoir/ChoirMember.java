package main.bellchoir;

import javax.sound.sampled.SourceDataLine;
import java.util.logging.Logger;

public class ChoirMember implements Runnable {
    
    private static final Logger logger = BellChoirLogger.createLogger(ChoirMember.class.getName());

    private final Note note;
    private final Thread thread;
    private final SourceDataLine audio;
    private volatile boolean isRunning;
    private volatile boolean myTurn;
    private volatile boolean isPlaying;
    private volatile NoteLength next_length;
    
    public ChoirMember(Note note, SourceDataLine audio, String name) {
        this.note = note;
        this.audio = audio;
        this.isRunning = false;
        this.isPlaying = false;
        this.myTurn = false;
        this.next_length = null;
        
        this.thread = new Thread(this, name);
    }
    
    public Note getNote() {
        return note;
    }
    
    public String getName() {
        return thread.getName();
    }
    
    public void start() {
        // if not already playing, tell it to play
        // prevents IllegalStateException from running thread.start() multiple times
        if (!isRunning) {
            this.isRunning = true;
            this.thread.start();
        }
    }
    
    /**
     * Runs this operation.
     */
    @Override
    public void run() {
//        if (isRunning) {
        logger.info(getName() + " started");
//        System.out.println(thread.getName() + " started");
//        }
        while (isRunning) {
            synchronized (this) {
                while (!myTurn && isRunning) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        logger.warning(getName() + " was interrupted while running (run())");
                        System.err.println(thread.getName() + " was interrupted while running");
                    }
                }
                // after this point, myTurn = true
                // double check that we are still supposed to be playing,
                if (isRunning) {
                    logger.info(getName() + " attempting to play sound (run())");
                    makeSound();
                }
                
                // done making sound so give up the current turn for the next person
                logger.info(getName() + " passing turn to next member (run())");
                myTurn = false;
                next_length = null;
            }
        }
    }
    
    private void makeSound() {
        if (next_length == null) {
            logger.warning(getName() + " tried to play a Note for null length (makeSound())");
//            System.err.println(getName() + " tried to play a Note for null length");
            return;
        }
//        System.out.println("Playing Note: " + note + " for " + next_length.name());
        final int length_time = Note.SAMPLE_RATE * next_length.timeMs() / 1000;
        
        // prevent multiple threads from writing to audio at the same time
        synchronized (audio) {
            logger.info(getName() + " playing Note: " + note + " for " + next_length.name());
            audio.write(note.sample(), 0, length_time);
            audio.write(Note.REST.sample(), 0, 50);
        }
        // after writing to the audio, set is playing to false since it is finished
        synchronized (this) {
            isPlaying = false;
            notify();
        }
    }
    
    public void stop() {
        this.isRunning = false;
        synchronized (this) {
            notify();
        }
        logger.info(getName() + " stopped");
//        System.out.println(thread.getName() + " stopped");
    }
    
    public synchronized void playNoteAndWait(NoteLength length) {
        this.myTurn = true;
        this.isPlaying = true;
        this.next_length = length;
        
        notify();
        
        while (isPlaying) {
            try {
                wait();
            } catch (InterruptedException e) {
                logger.warning(getName() + " was interrupted while waiting to play next note (playNoteAndWait())");
//                System.err.println(thread.getName() + " was interrupted trying to play the next note");
//                System.exit(1);
            }
        }
        
    }
    
}
