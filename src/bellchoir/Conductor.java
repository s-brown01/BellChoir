package bellchoir;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.*;

public class Conductor implements Runnable {
    
    public static void main(String[] args) {
        List<BellNote> testSong = new ArrayList<>();
        testSong.add(new BellNote(Note.A4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.C4, NoteLength.SIXTEENTH));
        testSong.add(new BellNote(Note.REST, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.E4, NoteLength.THIRTY_SECOND));
        testSong.add(new BellNote(Note.A4, NoteLength.EIGHTH));
        testSong.add(new BellNote(Note.REST, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.E4, NoteLength.HALF));
        testSong.add(new BellNote(Note.A4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.REST, NoteLength.WHOLE));
        testSong.add(new BellNote(Note.E4, NoteLength.EIGHTH));
        testSong.add(new BellNote(Note.A4, NoteLength.WHOLE));
        testSong.add(new BellNote(Note.REST, NoteLength.HALF));
        
        AudioFormat af = new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
        
        Conductor conductor = new Conductor(testSong, af, "TestConductor");
        
        conductor.start();
        
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
        final List<Note> allNotes = new LinkedList<>();
        // set all the unique notes that will be played in the song
        for (BellNote bn : song) {
            if (!allNotes.contains(bn.note)) {
                allNotes.add(bn.note);
            }
        }
        // for each unique note, make a ChoirMember who will play it
        int i = 0;
        for (Note note : allNotes) {
            // i starts at 0 before loop, so incrementing here means the member numbering starts at 1
            i++;
            final ChoirMember newMember = new ChoirMember(note, line, "Member #" + (i));
            choir.put(note, newMember);
        }
    }
    
    public void start() {
        isRunning = true;
        thread.start();
    }
    
    public void stop() {
        isRunning = false;
        stopChoir();
        
        try {
            thread.join();
        } catch (InterruptedException e) {
            System.err.println("Conductor interrupted while stopping");
        }
    }
    
    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        System.out.println("Conductor starting");
        startChoir();
        // give the same SourceDataLine to all choir members
        // create the choir once told to run
        // it is a bit of a small delay till the song is played when thread is started, but worth it
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            // open the line and start it
            line.open();
            line.start();
            
            // create the choir
            createChoir(line);
            
            // play the song
            for (BellNote bn : song) {
                ChoirMember cm = choir.get(bn.note);
                System.out.println("BN Note: " + bn.note.toString() + " played by member: " + cm.getName());
                try {
                    Thread.sleep(bn.length.timeMs());
                } catch (InterruptedException e) {
                    System.err.println("Conductor interrupted while sleeping");
                    System.exit(1);
                }
                
//                ChoirMember cm = choir.get(bn.note);
//                if (cm != null) {
//                    System.out.println("Conductor cueing " + cm.getName() + " for " + bn.note);
//                    cm.play(bn);
//                }
//
//                // wait for note duration
//                Thread.sleep(bn.length.timeMs());
            }
            
            // finished song so shut down
            stopChoir();
            line.drain();
            
        } catch (LineUnavailableException e) {
            System.err.println("An error occurred while trying to play the song, terminating program");
            System.exit(1);
        }
        

        System.out.println("Conductor ending");
//        stopChoir();
    }
    
    public void startChoir() {
        System.out.println("Conductor starting choir");
        final Set<ChoirMember> allMembers = new HashSet<>(choir.values());
        
        for (ChoirMember cm : allMembers) {
            System.out.println("Conductor telling " + cm.getName() + " to start");
            cm.start();
        }
    }
    
    public void stopChoir() {
        System.out.println("Conductor stopping choir");
        final Set<ChoirMember> allMembers = new HashSet<>(choir.values());
        
        for (ChoirMember cm : allMembers) {
            System.out.println("Conductor telling " + cm.getName() + " to stop");
            cm.stop();
        }
    }
    
    
}
