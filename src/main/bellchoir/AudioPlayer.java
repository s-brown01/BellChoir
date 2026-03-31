package main.bellchoir;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Entry point for the Bell Choir application.
 * <p></p>
 * This class is responsible for reading a song file, parsing it into a {@link List} of {@link BellNote}, and delegating
 * playback to a {@link Conductor}.
 * <p></p>
 * It validates input, handles file I/O, and ensures that the song is played to completion before terminating.
 * <p></p>
 * Terminates with status 0 if a successful read and play of song. Otherwise, terminates with status 1.
 */
public class AudioPlayer {
    /**
     * A {@link Logger} created using {@link BellChoirLogger}, configured specifically for the Bell Choir project
     */
    private static final Logger logger = BellChoirLogger.createLogger(AudioPlayer.class.getName());
    /**
     * The directory where all songs to play should be
     */
    private static final String SONG_DIRECTORY = "res/songs/";
    
    /**
     * The main method for the AudioPlayer
     *
     * @param args Command-line arguments. Accepts exactly one argument: the filename of the song located in res/songs/
     */
    public static void main(String[] args) {
        // should be exactly one argument: the song's filename
        if (args.length != 1) {
            logger.severe("Unexpected amount of arguments given. Needs 1, but received " + args.length + ". Terminating program.");
            System.exit(1);
        }
        // the song should always be in this directory
        final String filename = SONG_DIRECTORY + args[0];
        
        final AudioFormat af =
                new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
        AudioPlayer mainPlayer = new AudioPlayer(af);
        
        // try to read and play the song, easier to run 1 function from a user standpoint
        mainPlayer.readAndPlaySong(filename);
        logger.info("readAndPlaySong with filename " + filename + " finished successfully. Terminating program with code 0 (success)");
        System.exit(0);
        
    }
    
    /**
     * The {@link AudioFormat} used for playback.
     * <p></p>
     * Used in {@link Conductor} and parsed into {@link SourceDataLine} for {@link ChoirMember}.
     */
    private final AudioFormat af;
    
    /**
     * Creates an AudioPlayer with the specified {@link AudioFormat}.
     *
     * @param af The audio format used for playback
     */
    AudioPlayer(AudioFormat af) {
        this.af = af;
    }
    
    /**
     * Reads a song from a file and plays it.
     * <p></p>
     * This method attempts to parse the file into a {@link List} of {@link BellNote}. If the file is invalid or cannot
     * be read, the program will terminate with status code 1.
     *
     * @param filename The path to the file containing the song
     */
    private void readAndPlaySong(String filename) {
        // try to read the song that is to be played
        logger.info("Attempting to read song with filename: " + filename);
        List<BellNote> newSong = this.readSong(filename);
        // if the readSong(filename) returns null if the file couldn't be processed for
        if (newSong == null) {
            logger.severe("Reading the song resulted in a null value. Terminating program\n\tFilename was " + filename);
            // exit with 1 to show an invalid exit (0 is all good, 1 is expected but not good)
            System.exit(1);
        }
        
        // try to play the song
        logger.info("New song was created and not null, attempting to play the new song");
        this.playSong(newSong);
        
    }
    
    /**
     * Plays the given song by creating and coordinating a {@link Conductor}.
     * <p></p>
     * This method blocks until the song has finished playing using the Conductor.
     *
     * @param song the {@link List} of {@link BellNote} to be played
     */
    private void playSong(List<BellNote> song) {
        logger.info("Creating a Conductor");
        final Conductor c = new Conductor(song, af, "Conductor");
        logger.info("Telling Conductor to start");
        // play the song
        c.startPlaying();
        logger.info("Waiting for the Conductor to finish");
        c.waitUntilFinished();
        logger.info("Song has finished playing");
    }
    
    /**
     * Reads the given file and represents it as a {@link List} of {@link BellNote}.
     * <p></p>
     * Notes in the file should be separated by line with a single space between the note-length pair (e.g. A5 16).
     * <p></p>
     * Returns null if song is not valid:
     * <ul>
     *     <li>The file cannot be found</li>
     *     <li>An I/O error occurs</li>
     *     <li>Any line contains an invalid note or length</li>
     * </ul>
     *
     * @param filename the file to read the song from
     * @return A valid song will be represented as a {@link List} of {@link BellNote}. Invalid songs return null
     */
    private List<BellNote> readSong(String filename) {
        final List<BellNote> song = new ArrayList<>();
        boolean validSong = true;
        int badLines = 0;
        
        logger.info("Attempting to open file with filename: " + filename);
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int line_num = 0;
            while ((line = reader.readLine()) != null) {
                String[] note_length = line.split(" ");
                
                BellNote note = parseBellNote(note_length, line_num);
                
                if (note != null) {
                    song.add(note);
                } else {
                    // setting it to false every time only decreases performance a little bit, no other downsides
                    validSong = false;
                    badLines++;
                }
                line_num++;
            }
            
        } catch (FileNotFoundException e) {
            logger.severe("No file found with the filename: " + filename);
            validSong = false;
        } catch (IOException e) {
            logger.severe("An error occurred while trying to read the song with filename: " + filename);
            validSong = false;
        }
        
        if (!validSong) {
            logger.severe(filename + " has " + badLines + " bad lines and cannot be played");
            return null;
        }
        logger.info("Successfully read the song from filename: " + filename);
        return song;
    }
    
    /**
     * Parses a single line of input into a {@link BellNote}.
     * <p></p>
     * The input must be in the format [Note, Length]. Returns null if either the note or length is invalid.
     *
     * @param note_length the note-length pair to parse
     * @param line_number the line number associated with the input
     * @return A valid {@link BellNote}, or null if invalid
     */
    private BellNote parseBellNote(String[] note_length, int line_number) {
        // should always be 2 objects in the array: Note and Length
        // more or less than 2 and try the next line
        if (note_length.length != 2) {
            logger.severe("Unexpected amount of arguments. Expected 2 but received " + note_length.length + " \n\tLine " + line_number + ": " + Arrays.toString(note_length));
            return null;
        }
        
        // a boolean to represent if the note is valid or not
        boolean validNote = true;
        
        // use helper methods to part the Note and NoteLength
        final Note tempNote = parseNote(note_length, line_number);
        final NoteLength tempLength = parseNoteLength(note_length, line_number);
        // if the returned length is null, the note is invalid
        if (tempNote == null) {
            logger.warning("Line " + line_number + ": note is invalid and resulted in a null value");
            validNote = false;
        }
        if (tempLength == null) {
            logger.warning("Line " + line_number + ": length is invalid and resulted in a null value");
            validNote = false;
        }
        // if it is NOT a valid note, return null. Specific reason will be in logs under the Warnings above
        if (!validNote) {
            return null;
        }
        
        logger.info("Returning a new note for " + tempNote + " with " + tempLength + " length from line " + line_number);
        return new BellNote(tempNote, tempLength);
    }
    
    /**
     * Parses a {@link Note} from the given {@link String} Array. Expects the note to be the first value (index = 0).
     *
     * @param note_length the split input containing the note
     * @param line_number the line number for error reporting
     * @return The parsed {@link Note}, or null if invalid
     */
    private Note parseNote(String[] note_length, int line_number) {
        final String note = note_length[0];
        logger.info("Attempting to parse note: " + note);
        // try to get a BellNote out of the given string
        try {
            // try to get the note to play, should be in the Note enum
            return Note.valueOf(note);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid note: " + note + " at line " + line_number + " (" + Arrays.toString(note_length) + ")");
            return null;
        }
    }
    
    /**
     * Parses a {@link NoteLength} from the given input. Expects the length to be the second value (index = 1).
     * <p></p>
     * Converts the string to an integer and maps it to a NoteLength.
     *
     * @param note_length the split input containing the note length
     * @param line_number the line number for error reporting
     * @return The parsed {@link NoteLength}, or null if invalid
     */
    private NoteLength parseNoteLength(String[] note_length, int line_number) {
        final int lengthNum;
        logger.info("Attempting to parse " + note_length[1] + " into an Integer");
        try {
            // get the length of the note to play, should be an int
            lengthNum = Integer.parseInt(note_length[1]);
        } catch (NumberFormatException e) {
            logger.warning("Invalid note length: " + note_length[1] + " at line " + line_number + " (" + Arrays.toString(note_length) + ")");
            return null;
        }
        
        logger.info("Attempting to parse Integer " + lengthNum + " into a NoteLength");
        try {
            // using the new int, get the intended note's play length
            // if the NoteLength is not valid, fromDivision returns null, so this will return null if bad
            return NoteLength.fromDivision(lengthNum);
        } catch (IllegalArgumentException e) {
            logger.warning("Length " + lengthNum + " is not a NoteLength at line " + line_number + " (" + Arrays.toString(note_length) + ")");
            return null;
        }
    }
}
