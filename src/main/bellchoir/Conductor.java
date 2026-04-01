package main.bellchoir;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.*;
import java.util.logging.Logger;

/**
 * Represents the conductor of a bell choir, responsible for coordinating the playback of a song.
 * <p></p>
 * The conductor takes an ordered {@link List} of {@link BellNote} objects and uses an {@link AudioFormat} to produce
 * sound. Each unique note in the song is assigned to a {@link ChoirMember}, which is responsible for playing that
 * note.
 * <p></p>
 * This class runs on its own thread and manages the timing and sequencing of notes, ensuring that each note is played
 * in the correct order by signaling the appropriate choir member.
 */
public class Conductor implements Runnable {
    
    /**
     * A {@link Logger} created using {@link BellChoirLogger}, configured specifically for the Bell Choir project
     */
    private static final Logger logger = BellChoirLogger.createLogger(Conductor.class.getName());
    
    /**
     * A basic functionality test for the Conductor
     *
     * @param args <b>UNUSED</b>
     */
    public static void main(String[] args) {
        List<BellNote> testSong = new ArrayList<>();
        testSong.add(new BellNote(Note.A4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.A4S, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.B4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.C4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.C4S, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.D4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.D4S, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.E4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.REST, NoteLength.HALF));
        testSong.add(new BellNote(Note.E4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.D4S, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.D4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.C4S, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.C4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.B4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.A4S, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.A4, NoteLength.WHOLE));
        
        AudioFormat af = new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
        
        Conductor conductor = new Conductor(testSong, af, "TestConductor");
        
        conductor.startPlaying();
        
        try {
            conductor.thread.join(); // wait for song to finish
        } catch (InterruptedException e) {
            System.err.println("Conductor interrupted while waiting to finish");
        }
        
        System.out.println("Test finished");
    }
    
    /**
     * A {@link List} of {@link BellNote} that represent the song to play, in correct order of the song
     */
    private final List<BellNote> song;
    /**
     * A {@link Map} of {@link Note} to {@link ChoirMember} representing the ChoirMembers required to play the song
     */
    private final Map<Note, ChoirMember> choir;
    /**
     * The {@link Thread} responsible for running this Conductor's playback logic
     */
    private final Thread thread;
    /**
     * The {@link AudioFormat} that this program will use to play audio on
     */
    private final AudioFormat af;
    
    /**
     * Constructor for a Conductor. The created Conductor will be named Conductor. Will not play a song until
     * {@link #startPlaying()} is used.
     *
     * @param song The {@link List} of {@link BellNote} that represent the desired song to be played
     * @param af   The {@link AudioFormat} for this Conductor to play audio on
     */
    public Conductor(List<BellNote> song, AudioFormat af) {
        this(song, af, "Conductor");
    }
    
    /**
     * Constructor for a Conductor. The created Conductor will be named Conductor. Will not play a song until
     * {@link #startPlaying()} is used.
     *
     * @param song          The {@link List} of {@link BellNote} that represent the desired song to be played
     * @param af            The {@link AudioFormat} for this Conductor to play audio on
     * @param ConductorName The name of the Conductor. If name is empty or null, the name defaults to "Conductor"
     */
    public Conductor(List<BellNote> song, AudioFormat af, String ConductorName) {
        this.song = song;
        this.af = af;
        this.choir = new HashMap<>();
        // if the name is null, fix it
        // if the name is empty or full of spaces, fix it
        // Conductor will always have a name
        if (ConductorName == null || ConductorName.isBlank()) {
            ConductorName = "Conductor";
        }
        this.thread = new Thread(this, ConductorName);
    }
    
    /**
     * This creates the metaphorical choir.
     * <p></p>
     * Identifies all unique {@link Note} values present in the given {@link #song} and creates a {@link ChoirMember}
     * for each Note in the song. Each ChoirMember is responsible for only 1 Note.
     * <p></p>
     * All created ChoirMembers share the same {@link SourceDataLine}, so all audio output is written to the same line
     *
     * @param line The {@link SourceDataLine} that all ChoirMembers will use to output audio
     */
    private void createChoir(SourceDataLine line) {
        logger.info("Attempting to create a choir");
        final List<Note> allUniqueNotes = new LinkedList<>();
        // set of all the unique notes that will be played in the song
        for (BellNote bn : song) {
            if (!allUniqueNotes.contains(bn.note)) {
                allUniqueNotes.add(bn.note);
            }
        }
        // for each unique note, make a ChoirMember who will play it
        int i = 0;
        for (Note note : allUniqueNotes) {
            // i starts at 0 before loop, so incrementing here means the member numbering starts at 1
            i++;
            logger.info("Creating a new Choir Member for " + note + ", Member # " + i);
            final ChoirMember newMember = new ChoirMember(note, line, "Member #" + i);
            choir.put(note, newMember);
        }
        logger.info("Choir created with " + choir.size() + " members for a song with " + allUniqueNotes.size() + " unique notes");
    }
    
    /**
     * Tells the Conductor to start its thread and start running.
     * <p></p>
     * This method is non-blocking and will immediately return after starting the Conductor's internal thread. To wait
     * for the playback to finish, use {@link #waitUntilFinished()}
     */
    public void startPlaying() {
        logger.info("Starting to play song, setting isRunning to true and starting thread");
        thread.start();
    }
    
    /**
     * Blocks the calling thread until the Conductor has finished playing the song.
     * <p></p>
     * If this method is called from the Conductor's own thread, it will not block to avoid deadlock.
     */
    public void waitUntilFinished() {
        // make sure that the thread that calls this isn't the running thread of this
        // otherwise it would wait for itself to finish, thus never finishing
        if (Thread.currentThread() != this.thread) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                logger.warning(Thread.currentThread().getName() + " was interrupted while joining " + this.thread.getName());
            }
        } else {
            logger.warning("waitUntilFinished() called from the Conductor thread itself; returning immediately.");
        }
    }
    
    /**
     * Stops the playback of the song.
     * <p></p>
     * Signals the conductor to stop running and instructs all {@link ChoirMember} in the {@link #choir} to stop. Does
     * not block or wait for the thread to terminate.
     */
    public void stop() {
        logger.info("Stopping");
        logger.info("Stopping the choir");
        stopChoir();
    }
    
    /**
     * Executes the playback logic for the song.
     * <p></p>
     * This method initializes the audio line, creates and starts the choir, and iterates through the {@link #song},
     * signaling the appropriate {@link ChoirMember} to play each note in sequence. It is blocked each time it signals a
     * ChoirMember until they are done playing.
     * <p></p>
     * After all notes are played, the choir is stopped and the audio line is drained. If a critical error occurs, the
     * program will terminate.
     */
    @Override
    public void run() {
        logger.info("Starting thread");
        
        // create the choir once told to run
        // it is a bit of a small delay till the song is played when thread is started, but worth it
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            // open the line and start it
            line.open();
            line.start();
            
            // create the choir
            // give the same SourceDataLine to all choir members
            createChoir(line);
            
            // start the choir
            startChoir();
            
            // play the song
            for (BellNote bn : song) {
                // for each not in the song get the Member that should be playing
                ChoirMember cm = choir.get(bn.note);
                // if the member is null, the Choir wasn't created correctly
                if (cm == null) {
                    logger.severe("An error occurred while trying to play the song: found a null member. Terminating program.");
//                    System.err.println("An error occurred while trying to play the song, terminating program");
                    System.exit(1);
                }
                
                // tell the member to play for a certain amount of time
                // this waits for the cm to say they are finished before moving on
                cm.playNoteAndWait(bn.length);
                
            }
            
            // finished song so shut down
            stopChoir();
            line.drain();
            
        } catch (LineUnavailableException e) {
            // something happened while getting SourceDataLine from the AudioFormat
            // this is very bad so program should shut down
            logger.severe("An error occurred while trying to play the song: resource was restricted. Terminating program.");
//            System.err.println("An error occurred while trying to play the song, terminating program");
            System.exit(1);
        }
        
        logger.info("Done playing song, stopping now");
        this.stop();
    }
    
    /**
     * Starts all {@link ChoirMember} threads in the choir.
     * <p></p>
     * Each ChoirMember will begin running and wait until it is signaled to play its assigned note.
     */
    public void startChoir() {
        logger.info("Starting choir");
        
        // tell every choir member to start
        for (Note note : choir.keySet()) {
            // since keys are guaranteed to be unique AND each Choir Member only plays one note this tells each CM to start only 1 time
            choir.get(note).start();
        }
    }
    
    /**
     * Stops all {@link ChoirMember} threads in the choir.
     * <p></p>
     * Each ChoirMember is signaled to stop running and terminate its thread.
     */
    public void stopChoir() {
        logger.info("Stopping choir");
        final Set<ChoirMember> allMembers = new HashSet<>(choir.values());
        
        for (ChoirMember cm : allMembers) {
            cm.stop();
        }
    }
}
