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

public class Player {
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Unexpected amount of arguments. Expected 1 but received " + args.length);
            System.exit(1);
        }
        
        final String filename = "res/songs/" + args[0];
        
        final AudioFormat af =
                new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
        Player t = new Player(af);
        
        List<BellNote> newSong = t.readSong(filename);
        
        if (newSong == null) {
            System.err.println("Invalid song with filename: " + filename);
            System.exit(1);
        }
        
        try {
            t.playSong(newSong);
        } catch (LineUnavailableException e) {
            System.err.println("An error occurred while trying to play " + filename + ":\n\t" + e);
        }
    }

    private final AudioFormat af;

    Player(AudioFormat af) {
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

    private List<BellNote> readSong(String filename) {
        final List<BellNote> song = new ArrayList<>();
        boolean validSong = true;

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line =reader.readLine()) != null) {
                String[] note_length = line.split(" ");

                BellNote note = parseBellNote(note_length);
                
                if (note != null) {
                    song.add(note);
                } else if (validSong) {
                    validSong = false;
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("No song found with filename: " + filename);
        } catch (IOException e) {
            System.err.println("An error occurred while trying to read the song with filename: " + filename);
        }
        
        if (!validSong) {
            System.err.println(filename + " is not a valid song");
            return null;
        }

        return song;
        
    }

    private BellNote parseBellNote(String[] note_length) {
        // should always be 2 objects in the array: Note and Length
        // more or less than 2 and try the next line
        if (note_length.length != 2) {
            System.err.println("An unexpected amount of items per line, got " + note_length.length + " but expected 2");
            return null;
        }
        
        Note tempNote;
        int lengthNum;
        NoteLength tempLength;

        // try to get a BellNote out of the given string array
        try {
            // try to get the note to play, should be in the Note enum
            tempNote = Note.valueOf(note_length[0]);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid note: " + note_length[0]);
            return null;
        }
        
        try {
            // get the length of the note to play, should be an int
            lengthNum = Integer.parseInt(note_length[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid note length: " + note_length[1]);
            return null;
        }
        
        try{
            // using the new int, get the intended note's play length
            tempLength = NoteLength.fromDivision(lengthNum);
            
            // if the NoteLength is not valid, fromDivision returns null, so make sure it is not null
            if (tempLength == null) {
                return null;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Length " + lengthNum + " is not a valid note length");
            return null;
        }
        
        return new BellNote(tempNote, tempLength);
    }

    private void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        line.write(bn.note.sample(), 0, length);
        line.write(Note.REST.sample(), 0, 50);
    }
}
