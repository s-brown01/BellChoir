package bellchoir;

import java.util.*;

public class Conductor implements Runnable {
    
    private final List<BellNote> song;
    private final Map<Note, ChoirMember> choir;
    private final Thread thread;
    private volatile boolean isRunning = true;
    
    public static void main(String[] args) {
        // Build a fake song
        List<BellNote> testSong = new ArrayList<>();
        testSong.add(new BellNote(Note.A4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.C4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.E4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.A4, NoteLength.QUARTER));
        testSong.add(new BellNote(Note.REST, NoteLength.QUARTER));
        
        // Create a conductor
        Conductor conductor = new Conductor(testSong, "TestConductor");
        
        conductor.start();
        int i = 0;
        while (i < 10) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.err.println("Interrupted Conductor while testing");
                break;
            }
            System.out.print(".");
            i++;
        }
        System.out.println();
        // Stop all members
        conductor.stop();
        
        System.out.println("Test finished");
    }
    
    /**
     * Creates a Conductor with the name "Conductor".
     */
    public Conductor(List<BellNote> song) {
        this(song, "Conductor");
    }
    
    /**
     * Creates a Conductor with the specified ConductorName.
     * If no ConductorName is provided, the default is "Conductor"
     *
     * @param ConductorName the name for this Conductor
     */
    public Conductor(List<BellNote> song, String ConductorName) {
        this.song = song;
        this.choir = new HashMap<>();
        this.thread = new Thread(this, ConductorName);
        
        this.createChoir();
//        this.thread.start();
    }
    
    private void createChoir() {
        final List<Note> allNotes = new LinkedList<>();
        for (BellNote bn : song) {
            if (!allNotes.contains(bn.note)) {
                allNotes.add(bn.note);
            }
        }

        for (int i = 0; i < allNotes.size(); i += 2) {
            final Note note1 = allNotes.get(i);
            final Note note2;
            if (i + 1 < allNotes.size()) {
                 note2 = allNotes.get(i + 1);
            } else {
                note2 = null;
            }
            
            final ChoirMember newMember = new ChoirMember(note1, note2, "Member #" + (i + 1));
            choir.put(note1, newMember);
            if (note2 != null) {
                choir.put(note2, newMember);
            }
        }
    }
    
    public void start() {
        isRunning = true;
        thread.start();
//        startChoir();
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
        
        while (isRunning) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.err.println("Conductor interrupted");
            }
        }
        System.out.println("Conductor stopping");
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
