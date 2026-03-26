package main.bellchoir;

public enum NoteLength {
    WHOLE(1),
    HALF(2),
    QUARTER(4),
    EIGHTH(8),
    SIXTEENTH(16),
    THIRTY_SECOND(32);
    
    public static NoteLength fromDivision(int division) {
        for (NoteLength nl : values()) {
            if (nl.division == division) {
                return nl;
            }
        }
        return null;
    }
    
    private final int division;
    private final int timeMs;
    
    NoteLength(int division) {
        this.division = division;
        timeMs = (Note.MEASURE_LENGTH_SEC * 1000) / division;
    }
    
    public int timeMs() {
        return timeMs;
    }
    
    public int division() {
        return division;
    }
}