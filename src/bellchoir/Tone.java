package bellchoir;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Tone {
    
    // Mary had a little lamb
    private static final List<BellNote> MARY_LITTLE_LAMB = new ArrayList<BellNote>() {{
        add(new BellNote(Note.A5, NoteLength.QUARTER));
        add(new BellNote(Note.G4, NoteLength.QUARTER));
        add(new BellNote(Note.F4, NoteLength.QUARTER));
        add(new BellNote(Note.G4, NoteLength.QUARTER));
        
        add(new BellNote(Note.A5, NoteLength.QUARTER));
        add(new BellNote(Note.A5, NoteLength.QUARTER));
        add(new BellNote(Note.A5, NoteLength.HALF));
        
        add(new BellNote(Note.G4, NoteLength.QUARTER));
        add(new BellNote(Note.G4, NoteLength.QUARTER));
        add(new BellNote(Note.G4, NoteLength.HALF));
        
        add(new BellNote(Note.A5, NoteLength.QUARTER));
        add(new BellNote(Note.A5, NoteLength.QUARTER));
        add(new BellNote(Note.A5, NoteLength.HALF));
        
        add(new BellNote(Note.A5, NoteLength.QUARTER));
        add(new BellNote(Note.G4, NoteLength.QUARTER));
        add(new BellNote(Note.F4, NoteLength.QUARTER));
        add(new BellNote(Note.G4, NoteLength.QUARTER));
        
        add(new BellNote(Note.A5, NoteLength.QUARTER));
        add(new BellNote(Note.A5, NoteLength.QUARTER));
        add(new BellNote(Note.A5, NoteLength.QUARTER));
        add(new BellNote(Note.A5, NoteLength.QUARTER));
        
        add(new BellNote(Note.G4, NoteLength.QUARTER));
        add(new BellNote(Note.G4, NoteLength.QUARTER));
        add(new BellNote(Note.A5, NoteLength.QUARTER));
        add(new BellNote(Note.G4, NoteLength.QUARTER));
        
        add(new BellNote(Note.F4, NoteLength.WHOLE));
    }};
    
    public static void main(String[] args) throws Exception {
        final AudioFormat af =
                new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
        Tone t = new Tone(af);
        List<BellNote> newSong = t.readSong();
        t.playSong(newSong);

//        t.playSong(MARY_LITTLE_LAMB);
    
    }
    
    private final AudioFormat af;
    
    Tone(AudioFormat af) {
        this.af = af;
    }
    
    void playSong(List<BellNote> song) throws LineUnavailableException {
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            line.open();
            line.start();
            
            for (BellNote bn : song) {
                playNote(line, bn);
            }
            line.drain();
        }
    }
    
    private List<BellNote> readSong() {
        final String filename = "res/songs/MaryLamb.txt";
        final List<BellNote> song = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line =reader.readLine()) != null) {
                String[] note_length = line.split(" ");
                
                BellNote note = parseBellNote(note_length);
                
                if (note != null) {
                    song.add(note);
                }
            }
            
        } catch (FileNotFoundException e) {
            System.err.println("No song found with filename: " + filename);
        } catch (IOException e) {
            System.err.println("IO Exception has occurred with reading song: " + filename);
        }
        
        return song;
        
    }
    
    private BellNote parseBellNote(String[] note_length) {
        // should always be 2 objects in the array: Note and Length
        // more or less than 2 and try the next line
        if (note_length.length != 2) {
            return null;
        }
        
        // try to get a BellNote out of the given string array
        try {
            // try to get the note to play, should be in the Note enum
            Note tempNote = Note.valueOf(note_length[0]);
            // get the length of the note to play, should be an int
            int lengthNum = Integer.parseInt(note_length[1]);
            // using the new int, get the intended note's play length
            NoteLength tempLength = NoteLength.fromDivision(lengthNum);
            
            // if the NoteLength is not valid, fromDivision returns null, so make sure it is not null
            if (tempLength == null) {
                return null;
            }
            
            return new BellNote(tempNote, tempLength);
        } catch (IllegalArgumentException e) {
            // this is for Note.Value of and
            return null;
        }
        
    }
    
    private void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        line.write(bn.note.sample(), 0, length);
        line.write(Note.REST.sample(), 0, 50);
    }
}
