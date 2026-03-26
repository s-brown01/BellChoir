package main.bellchoir;

import javax.sound.sampled.SourceDataLine;
import java.util.logging.Logger;

/**
 * Represents a member of the bell choir responsible for playing a single {@link Note}.
 * <p></p>
 * Each ChoirMember runs on its own thread and waits until it is signaled by the Conductor to play its assigned note.
 * When signaled, it plays the note for a specified {@link NoteLength} using the shared {@link SourceDataLine}.
 * <p></p>
 * This class uses thread coordination (wait/notify) to ensure that only one ChoirMember plays at a time and that notes
 * are played in the correct sequence.
 */
public class ChoirMember implements Runnable {
    
    /**
     * A {@link Logger} created using {@link BellChoirLogger}, configured specifically for the Bell Choir project
     */
    private static final Logger logger = BellChoirLogger.createLogger(ChoirMember.class.getName());
    
    /**
     * The {@link Note} that ChoirMember is responsible for
     */
    private final Note note;
    /**
     * The {@link Thread} that runs this ChoirMember's execution loop
     */
    private final Thread thread;
    /**
     * The {@link SourceDataLine} that is used for ChoirMembers' audio output. Should be the same for all ChoirMembers
     * but is not enforced so audio can be separate per instance if needed or desired.
     */
    private final SourceDataLine audio;
    /**
     * Indicates whether this ChoirMember should be running. Volatile to ensure each thread sees the most current value
     */
    private volatile boolean isRunning;
    /**
     * Indicates whether it is this ChoirMember's turn to play a note. Volatile to ensure each thread sees the most
     * current value
     */
    private volatile boolean myTurn;
    /**
     * Indicates whether this ChoirMember is currently playing a note. Volatile to ensure each thread sees the most
     * current value
     */
    private volatile boolean isPlaying;
    /**
     * The {@link NoteLength} for the next note to be played. Should be set to null if there is no Note to play.
     */
    private volatile NoteLength next_length;
    
    /**
     * Creates a ChoirMember responsible for playing a specific {@link Note}.
     *
     * @param note  The {@link Note} that this ChoirMember is responsible for
     * @param audio The {@link SourceDataLine} that this ChoirMember will produce audio on
     * @param name  The name of this ChoirMember and its thread
     */
    public ChoirMember(Note note, SourceDataLine audio, String name) {
        this.note = note;
        this.audio = audio;
        this.isRunning = false;
        this.isPlaying = false;
        this.myTurn = false;
        this.next_length = null;
        
        this.thread = new Thread(this, name);
    }
    
    /**
     * Getter for this ChoirMember's name. Stored in the {@link Thread}
     *
     * @return a String representing the ChoirMember's name
     */
    public String getName() {
        return thread.getName();
    }
    
    /**
     * Starts this ChoirMember's thread. Does not start audio
     * <p></p>
     * This method will only start the thread if it is not already running, preventing multiple calls to
     * {@link Thread#start()}.
     */
    public void start() {
        // if not already playing, tell it to play
        // prevents IllegalStateException from running thread.start() multiple times
        if (!isRunning) {
            this.isRunning = true;
            this.thread.start();
        }
    }
    
    /**
     * Executes the main loop for this ChoirMember.
     * <p></p>
     * The thread waits until it is signaled that it is its turn to play a note. When signaled, it plays the assigned
     * note and then returns to a waiting state.
     * <p></p>
     * This method continues running until {@link #stop()} is called.
     */
    @Override
    public void run() {
        logger.info(getName() + " started");
        while (isRunning) {
            synchronized (this) {
                while (!myTurn && isRunning) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        logger.warning(getName() + " was interrupted while running (run())");
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
    
    /**
     * Plays the assigned {@link Note} for the specified {@link #next_length}. If {@link #next_length} is null, the
     * function immediately returns.
     * <p></p>
     * This method writes audio data to the shared {@link SourceDataLine}. Access to the audio line is synchronized to
     * prevent multiple threads from writing simultaneously.
     */
    private void makeSound() {
        if (next_length == null) {
            logger.warning(getName() + " tried to play a Note for null length (makeSound())");
            return;
        }
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
            // tell any and all threads waiting on this thread that it is finished playing
            notifyAll();
        }
    }
    
    /**
     * Stops this ChoirMember's execution.
     * <p></p>
     * This method signals the thread to stop running and wakes it if it is waiting.
     */
    public void stop() {
        this.isRunning = false;
        synchronized (this) {
            notify();
        }
        logger.info(getName() + " stopped");
    }
    
    /**
     * Signals this ChoirMember to play a note and waits until playback is complete.
     * <p></p>
     * This method sets the note length, notifies all threads so that the correct one (where {@link #myTurn} is true)
     * can proceed, and blocks until the note has finished playing.
     *
     * @param length The {@link NoteLength} specifying how long to play the note
     */
    public synchronized void playNoteAndWait(NoteLength length) {
        // since this program blocks, unsure a class can't call itself so it doesn't get deadlocked
        if (Thread.currentThread() == this.thread) {
            logger.warning(this.getName() + " tried to call playNoteAndWait() on itself. Disallowed to prevent deadlocks");
            return;
        }
        this.myTurn = true;
        this.isPlaying = true;
        this.next_length = length;
        
        // notify all waiting threads that state has changed
        // multiple threads may be waiting on this monitor, so notifyAll() ensures
        // the thread with myTurn == true wakes up and proceeds while others continue waiting
        notifyAll();
        
        // wait until the ChoirMember signals that it has finished playing
        // this creates a synchronized handshake: Conductor signals start and waits for the ChoirMember signals completion
        while (isPlaying) {
            try {
                wait();
            } catch (InterruptedException e) {
                logger.warning(getName() + " was interrupted while waiting to play next note (playNoteAndWait())");
            }
        }
    }
    
}
