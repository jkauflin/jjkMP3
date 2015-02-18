# jjkMP3
Java application to update MP3 tags according to directory structure.  
Using org.cmc.music.myid3 code for working with MP3 file tags.
This java application does a recursive read through a directory tree of MP3 files organizaed by genere, artist, album, and 
updates the MP3 tags to match the information from the directory folders.  It's hard-coded to look for a specific structure
and organization for now.  After it updates the MP3 it sets it to read-only, and then only updates non read-only files
it finds.

