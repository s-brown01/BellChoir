# Bell Choir
## Overview
This multithreaded Java project simulates a real Bell Choir. 
This is ran through the main controller: `AudioPlayer`. 
The song is played by a central unit, called a `Conductor`, which creates the necessary `ChoirMembers`.
Each `ChoirMember` is assigned only $1$ note to play.

The program reads a song from a file, parses it into a sequence of notes, and plays it using synchronized thread communication.

## Features
* Multithreaded Audio Playback using `SourceDataLine`
* Central Control via `AudioPlayer` Thread
* Thread-safe communication through `wait()` and `notify()`
* File-based song input

## How to Run

## Structure
* AudioPlayer
  * Entry point of the program
    * Uses main method: accepts only $1$ Argument (song's filename)
  * Reads song files and initializes playback
  * Creates the `Conductor`
* Conductor
  * Controls the flow of the song
  * Assigns turns to `ChoirMember` threads
  * Ensures notes are played in sequence
* ChoirMember
  * Represents a musician responsible for a single note 
  * Runs in its own thread
  * Waits until signaled by the `Conductor` to play
* BellNote
  * Combines a Note and a NoteLength
* Note / NoteLength
  * Represents the pitch and duration of notes

## Challenges Faced

