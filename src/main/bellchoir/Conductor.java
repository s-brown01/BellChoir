package main.bellchoir;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.*;
import java.util.logging.Logger;

public class Conductor implements Runnable {
    
    private static final Logger logger = BellChoirLogger.createLogger(Conductor.class.getName());
    
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
            e.printStackTrace();
        }
        
        System.out.println("Test finished");
    }
    
    private final List<BellNote> song;
    private final Map<Note, ChoirMember> choir;
    private final Thread thread;
    private final AudioFormat af;
    private volatile boolean isRunning = true;
    
    public Conductor(List<BellNote> song, AudioFormat af) {
        this(song, af, "Conductor");
    }
    
    /**
     * Creates a Conductor with the specified ConductorName. If no ConductorName is provided, the default is
     * "Conductor"
     *
     * @param ConductorName the name for this Conductor
     */
    public Conductor(List<BellNote> song, AudioFormat af, String ConductorName) {
        this.song = song;
        this.af = af;
        this.choir = new HashMap<>();
        this.thread = new Thread(this, ConductorName);
        
    }
    
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
    
    public void startPlaying() {
        logger.info("Starting to play song, setting isRunning to true and starting thread");
        isRunning = true;
        thread.start();
    }
    
    public void stop() {
        logger.info("Stopping");
        isRunning = false;
        logger.info("Stopping the choir");
        stopChoir();
    }
    
    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        logger.info("Starting thread");
//        System.out.println("Conductor starting");
        
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
    
    public void startChoir() {
        logger.info("Starting choir");

        // tell every choir member to start
        for (Note note : choir.keySet()) {
            // since keys are guaranteed to be unique AND each Choir Member only plays one note this tells each CM to start only 1 time
            choir.get(note).start();
        }
    }
    
    public void stopChoir() {
        logger.info("Stopping choir");
//        System.out.println("Conductor stopping choir");
        final Set<ChoirMember> allMembers = new HashSet<>(choir.values());
        
        for (ChoirMember cm : allMembers) {
            cm.stop();
        }
    }
    
    
}
