package main.bellchoir;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioPlayer {
    
    private static final String SONG_DIRECTORY = "res/songs/";
    
    /**
     * The main method for player. To run, use `ant run` at the project level.
     *
     * @param args - Only accepts args of length 1. Should be the name of the file to be played, without the path. File
     *             is expected to be in res/songs/
     */
    public static void main(String[] args) {
        // should be exactly one argument: the song's filename
        if (args.length != 1) {
            System.err.println("Unexpected amount of arguments. Expected 1 but received " + args.length);
            System.exit(1);
        }
        // the song should always be in this directory
        final String filename = SONG_DIRECTORY + args[0];
        
        final AudioFormat af =
                new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
        AudioPlayer t = new AudioPlayer(af);
        
        // try to read and play the song, easier to run 1 function from a user standpoint
        t.readAndPlaySong(filename);
        
    }
    
    private final AudioFormat af;
    
    AudioPlayer(AudioFormat af) {
        this.af = af;
    }
    
    private void readAndPlaySong(String filename) {
        // try to read the song that is to be played
        List<BellNote> newSong = this.readSong(filename);
        // if the readSong(filename) returns null if the file couldn't be processed for
        if (newSong == null) {
            System.err.println("Invalid song with filename: " + filename);
            // exit with 1 to show an invalid exit (0 is all good, 1 is expected but not good)
            System.exit(1);
        }
        // try to play the song
        try {
            this.playSong(newSong);
        } catch (LineUnavailableException e) {
            // if something unexpected occurs tell the user that it was a problem on the programs end
            System.err.println("An error occurred while trying to play " + filename);
            System.exit(1);
        }
    }
    
    /**
     * Play the song in the parameter
     *
     * @param song a List of BellNotes that are to be played by the players in the band
     * @throws LineUnavailableException - thrown when resources are restricted
     */
    void playSong(List<BellNote> song) throws LineUnavailableException {
        final Conductor c = new Conductor(song, af, "Conductor");
        c.start();
    }
    
    /**
     * Read a given file and create a List of BellNotes. This will return null if the song is not valid
     * (FileNotFoundException or IOException Occurred, or contains a null BellNote). Notes in the file should be
     * separated by line with a single space between the note-length pair (e.g. A5 16). The length
     *
     * @param filename the file to read the song from
     * @return A List containing a valid song to play, will be null if the song is invalid
     */
    private List<BellNote> readSong(String filename) {
        final List<BellNote> song = new ArrayList<>();
        boolean validSong = true;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int line_num = 0;
            while ((line = reader.readLine()) != null) {
                String[] note_length = line.split(" ");
                
                BellNote note = parseBellNote(note_length, line_num);
                
                if (note != null) {
                    song.add(note);
                } else if (validSong) {
                    validSong = false;
                }
                line_num++;
            }
            
        } catch (FileNotFoundException e) {
            System.err.println("No song found with filename: " + filename);
            validSong = false;
        } catch (IOException e) {
            System.err.println("An error occurred while trying to read the song with filename: " + filename);
            validSong = false;
        }
        
        if (!validSong) {
            System.err.println(filename + " has bad lines and cannot be played ");
            return null;
        }
        
        return song;
    }
    
    /**
     * @param note_length the note-length pair to parse a BellNote from, in format [Note, Length]
     * @param line_number the line number associated with the specific note_length pair
     * @return A BellNote parsed from the note_length param. Will be null if invalid
     */
    private BellNote parseBellNote(String[] note_length, int line_number) {
        // should always be 2 objects in the array: Note and Length
        // more or less than 2 and try the next line
        if (note_length.length != 2) {
            System.err.println("An unexpected amount of items per line, expecting 2 got " + note_length.length + " \n\tLine " + line_number + ": " + Arrays.toString(note_length));
            return null;
        }
        
        final Note tempNote = parseNote(note_length, line_number);
        final NoteLength tempLength = parseNoteLength(note_length, line_number);
        
        if (tempNote == null || tempLength == null) {
            return null;
        }
        
        return new BellNote(tempNote, tempLength);
    }
    
    private Note parseNote(String[] note_length, int line_number) {
        final String note = note_length[0];
        // try to get a BellNote out of the given string
        try {
            // try to get the note to play, should be in the Note enum
            return Note.valueOf(note);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid note: " + note + " \n\tLine " + line_number + ": " + Arrays.toString(note_length));
            return null;
        }
    }
    
    private NoteLength parseNoteLength(String[] note_length, int line_number) {
        final int lengthNum;
        try {
            // get the length of the note to play, should be an int
            lengthNum = Integer.parseInt(note_length[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid note length: " + note_length[1] + " \n\tLine " + line_number + ": " + Arrays.toString(note_length));
            return null;
        }
        
        try {
            // using the new int, get the intended note's play length
            // if the NoteLength is not valid, fromDivision returns null, so this will return null if bad
            return NoteLength.fromDivision(lengthNum);
        } catch (IllegalArgumentException e) {
            System.err.println("Length " + lengthNum + " is not a valid note length" + " \n\tLine " + line_number + ": " + Arrays.toString(note_length));
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
