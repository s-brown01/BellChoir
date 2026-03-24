package bellchoir;

import java.util.*;

public class Conductor implements Runnable {
    
    private final List<BellNote> song;
    private final Map<Note, ChoirMember> choir;
    private final Thread thread;
    
    
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
        this.thread.start();
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
    
    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        System.out.println("Conductor started");
        final Set<ChoirMember> uniqueMembers = new HashSet<>(choir.values());
        
        for (ChoirMember cm : uniqueMembers) {
            System.out.println("Conductor telling " + cm.getName() + " to start");
            cm.start();
        }
        
    }
}
