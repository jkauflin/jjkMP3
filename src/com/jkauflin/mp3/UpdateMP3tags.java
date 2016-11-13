//----------------------------------------------------------------------------------------------------------
// 2011-09-27 JJK 	Commented out the M3U file writes because now I just use album organizations in WINAMP9
// 2011-09-30 JJK 	Modified to use the MyID3 library and correct run problems under Windows 7
// 2012-07-23 JJK 	Worked on setting the ID3v2 tags (if they exist)
// 2013-08-12 JJK 	Added convert for exported M3U files to change local root locations with one
//                	recognized by the HomeGroup (so that all users can use my M3U collections)
// 2016-09-30 JJK 	Changing to use new MP3 tag libraries (from GitHub), modifying tag to make sense
//                	in Google Play, and updating artwork if missing.
// 2016-10-08 JJK 	Not used the new libraries and not trying to update artwork in this (using mp3tag
//					and artwork downloaders.
//					Modified to add artist name to album if just Greatest Hits or Best Of, also change
//                  Misc to hits (to help the artwork downloader)
//----------------------------------------------------------------------------------------------------------
package com.jkauflin.mp3;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
//import java.sql.*;
import java.text.*;

//import com.mpatric.mp3agic.*;

import org.cmc.music.myid3.*;
import org.cmc.music.metadata.*;

//import org.cmc.music.myid3.id3v2.MyID3v2Frame;
//import org.cmc.music.myid3.examples.SampleUsage;
//import org.cmc.music.metadata.MusicMetadataSet;
/*
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.TagOptionSingleton;
*/

public class UpdateMP3tags {

    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    int jjkValue = 8888;
    
	boolean createAllCollFiles;
	boolean updateAllTags;
	boolean updateAllDbSongs;
	boolean makeAllFilesReadWrite;
    
	int			picColDirLevel;
    File 		rootDir;

    //------------------------------------------------------------------------------------------------------
    // Variables needed for the MusicCatalog update
    //------------------------------------------------------------------------------------------------------
	int			m_DirLevel;
	int			m_Pos;
	int			m_Pos2;
    int         temp_int;
	String		tempStr;
    String		FileType;
    boolean     artistFound;
    boolean     UpdTagsFlag;
	String		m_Category;
	String		m_CategoryOverride;
	int			m_Category_cnt;
    int         m_genre;
	String		m_Decade;
	String		m_Artist;
	int			m_Artist_cnt;
	String		m_Album;
	int			m_Album_cnt;
	int			m_Year;
	String		m_Title;
	String		m_Song;
	int			m_Song_cnt;
	int			m_V2tagErr_cnt;
	String		m_SongIndex;
	String		m_AlbumDisc;
	String		m_AlbumCol_ArtistName;
	int			m_ReadOnly_cnt;
    String 		MainDirAlbumDir;
    String 		ArtistColDir;
    String		debugStr;
    String		musicCatalogRootDir;
    int			fileYear;
    int			defaultYear;
    
    //MP3File 			temp_MP3File;
	MyID3 				myID3;
	MusicMetadataSet 	dataSet;
    
	//Mp3File mp3file;
	
	/*
	AudioFile audioFile = null;
	Tag tag = null;
	TagField tagField = null;
	*/
    
    java.util.Date 		SysDate;
    java.sql.Timestamp 	SysTimeStamp;

	private static final String SongNum[] = {"01","02","03","04","05","06","07","08","09","10",
                             				 "11","12","13","14","15","16","17","18","19","20",
                             				 "21","22","23","24","25","26","27","28","29","30",
                             				 "31","32","33","34","35","36","37","38","39","40",
                             				 "41","42","43","44","45","46","47","48","49","50",
                             				 "51","52","53","54","55","56","57","58","59","60",
                             				 "61","62","63","64","65","66","67","68","69","70",
                             				 "71","72","73","74","75","76","77","78","79","80",
                             				 "81","82","83","84","85","86","87","88","89","90",
                             				 "91","92","93","94","95","96","97","98","99"};

	public static void main(String[] args) {
		//final Logger logger = Logger.getLogger("org.jaudiotagger");
		//logger.setLevel(Level.WARNING);

		UpdateMP3tags updateMP3tags = new UpdateMP3tags();
		
		updateMP3tags.createAllCollFiles = false;
		updateMP3tags.updateAllDbSongs = false;
		// 2012-05-28 Updating all tags after moving some files to archive and renaming some

		updateMP3tags.makeAllFilesReadWrite = false;
		updateMP3tags.updateAllTags = false;

		//updateMP3tags.makeAllFilesReadWrite = true;
		//updateMP3tags.updateAllTags = true;

		try {
		    Calendar startTime = Calendar.getInstance();
		    long startTimeMS = startTime.getTimeInMillis();
			System.out.println("---------------------------------------------------------------------------------------------");
			System.out.println(dateTimeFormat.format(startTime.getTime())+" - In the UpdateMP3tags class");

			//updateMP3tags.updateMusicCatalog(System.getProperty("user.dir"));
			//updateMP3tags.updateMusicCatalog("M:\\");
			//updateMP3tags.updateMusicCatalog("D:\\jjkMusic");
			updateMP3tags.updateMusicCatalog("D:\\jjkMusicStaging");
			
		    Calendar endTime = Calendar.getInstance();
			System.out.println(dateTimeFormat.format(endTime.getTime())+" - In the UpdateMP3tags class");
			System.out.println("Total time was "+((endTime.getTimeInMillis()-startTime.getTimeInMillis())/1000)+" seconds");
		}
		catch (Exception e) {
			System.out.println("Error getting user.dir = "+e);
			e.printStackTrace();
		}
 	}

	
	    //------------------------------------------------------------------------------------------------------
	    // Method to update the PictureCollections table with data from the directory structure
	    //------------------------------------------------------------------------------------------------------
	    public void updateMusicCatalog(String musicCatalogRootDir) throws Exception 
	    {
	        try
	        {
	        	/* temp for DEBUG */
				//System.out.println("   CreateAllCollFiles = "+Boolean.parseBoolean(getCreateAllCollFiles()));
				//System.out.println("        UpdateAllTags = "+Boolean.parseBoolean(getUpdateAllTags()));
				//System.out.println("     UpdateAllDbSongs = "+Boolean.parseBoolean(getUpdateAllDbSongs()));
				//System.out.println("MakeAllFilesReadWrite = "+Boolean.parseBoolean(getMakeAllFilesReadWrite()));
				/* TEMP */

	      	    System.out.println("musicCatalogRootDir = "+musicCatalogRootDir);        	
	      	    
				if (musicCatalogRootDir.equals(""))
	            {
	            	System.out.println("Music catalog root dir is blank - cannot update");
	                return;
	            }

	            m_DirLevel = 0;
	            m_Pos = 0;
	            m_Pos2 = 0;
	            m_Category_cnt = 0;
	            m_Artist_cnt = 0;
	            m_Album_cnt = 0;
	            m_Song_cnt = 0;
	            m_V2tagErr_cnt = 0;
	            m_ReadOnly_cnt = 0;
	            m_CategoryOverride = "";
	            defaultYear = 1900;

	            try {
	                rootDir = new File(musicCatalogRootDir);
	            }
	            catch (Exception e) {
	                throw new Exception("Error creating File from rootDir = "+musicCatalogRootDir+", "+e);
	            }

	            if (!musicCatalogRootDir.endsWith("/"))
	            {
	            	musicCatalogRootDir = musicCatalogRootDir + "/";
	            }
	            MainDirAlbumDir = musicCatalogRootDir + "MyCollections\\Albums";
	            ArtistColDir = musicCatalogRootDir + "MyCollections\\Artists";
	           
	            SysDate = Calendar.getInstance().getTime();
	            SysTimeStamp = new java.sql.Timestamp(SysDate.getTime());

	            // Instantiate the MP3 library objects
	        	myID3 = new MyID3();
	            
	            // jaudiotagger options for track numbers
	            // ID3 saves track and disc numbers as text , and it is quite common for it to be zero padded to aid sorting , 
	            // i.e 01 instead of 1 or 03/10 instead of 3/10	, this option is disabled by default
	        	//TagOptionSingleton.getInstance().setPadNumbers(true);

	            // Start the recursive directory explore
	            System.out.println("Starting catalog update, rootDir = "+musicCatalogRootDir);
	            CatalogMP3s(rootDir);
	            
			    System.out.println(" ");
			    System.out.println("Total # of Categories = "+m_Category_cnt);
			    System.out.println("Total # of Artists    = "+m_Artist_cnt);
			    System.out.println("Total # of Albums     = "+m_Album_cnt);
			    System.out.println("Total # of Songs      = "+m_Song_cnt);
			    System.out.println("Total # of ReadOnly files = "+m_ReadOnly_cnt);
			    //System.out.println("Total # of V2 tag errors  = "+m_V2tagErr_cnt);

	            // 2013-11-29 JJK - Convert the playlist files for use by computers using the mapped drive of M:
	            //System.out.println("*** Converting playlist files ***");
			    // 2015-03-29 JJK - Now using computer as a DLNA media server, so using convert again to make sure M3U's point to the correct location
		    	//ConvertM3Us();
	            //System.out.println("ConvertM3Us successful");
		    	
	            System.out.println("Update successful");

	        }
	        catch (Exception e)
	        {
	            System.out.println("Problem in Music catelog update "+e);
	            System.out.println(debugStr);
				e.printStackTrace();
	            throw e;
	        }
	        catch (Error e)
	        {
	            System.out.println("Problem in Music catelog update "+e);
	            System.out.println(debugStr);
				e.printStackTrace();
	            throw e;
	        }
	        
	        return;

	    } // End of public List<String> updateMusicCatalog(String musicCatalogRootDir) 

	    
	    //------------------------------------------------------------------------------------------------------
	    // Internal recursive method to go through a directory list and update the Music catalog and database
	    //------------------------------------------------------------------------------------------------------
	    private void CatalogMP3s(File InFile)
			throws Exception
	    {
	        boolean fileIsReadOnly;
	        if (!InFile.isDirectory())
	        {
	            throw new Exception("ERROR: "+InFile+" is NOT a directory");
	        }

	        // Increment the directory level when we start processing a new directory
	        m_DirLevel++;

	        File FileArray[] = InFile.listFiles();
	        //Arrays.sort(FileArray, String.CASE_INSENSITIVE_ORDER);
	        SortFileArray(FileArray);
            int AlbumDiscNum = 0;
            int SongIndexNum = 0;
	        int newIndex = 0;
	        
	        for (int i=0; i < FileArray.length; i++)
	        {
	            debugStr = m_DirLevel+" >>>>> getName = *"+FileArray[i].getName()+"*";
	            //System.out.println(debugStr);

	            if (FileArray[i].isDirectory())
	            {
	    		    if (m_DirLevel == 1)
	    		    {
	    			    m_Category = FileArray[i].getName();

	    			    // Check for special categories
	    			    if (m_Category.equals("Album shortcuts") || m_Category.equals("MyCollections") || m_Category.equals("Playlists"))
	    			    {
	    				    continue;
	    			    }

	    			    m_Category_cnt++;
	    	            System.out.println(debugStr);
	    		    }
	    		    else if (m_DirLevel == 2)
	    		    {
	    			    m_Artist = FileArray[i].getName();

	    			    if (!m_Artist.equals("Misc"))
	    			    {
	        			    m_Artist_cnt++;
	        			    /*
	        			    Artist artist = new Artist();
	        			    artist.setArtist(m_Artist);
	        			    artist.setCategory(m_Category);
	                        if (m_CategoryOverride != null && !m_CategoryOverride.equals(""))
	                        {
	                			artist.setCategory(m_CategoryOverride);
	                        }
	                        artist.setComments("");
	                        artist.setLastChanged(SysTimeStamp);
	                        artist.setMembers("");
	                        artist.setRating(0);
	                        dbEntityManager.insertArtist(artist);
	                        */
	    			    }
	    		    }
	    		    else if (m_DirLevel == 3)
	    		    {
	    			    m_Album = FileArray[i].getName();

	                    // Check if there is an override category in the album name
	                    m_CategoryOverride = "";
	    		        m_Pos = m_Album.indexOf("[");
	    		        m_Pos2 = m_Album.indexOf("]");
	    		        if (m_Pos >= 0 && m_Pos2 >= 0)
	    		        {
	                        m_CategoryOverride = m_Album.substring(m_Pos+1,m_Pos2);
	                        m_Album = m_Album.substring(0,m_Pos-1);
	                    }

	    			    m_Album_cnt++;
	    			    m_Year	= defaultYear;
	    			    m_Title = m_Album;

	    			    // Check for special album names
	    			    if (m_Album.equals("Pictures"))
	    			    {
	    				    continue;
	    			    }

	    			    if (m_Album.startsWith("("))
	    			    {
	    				    m_Year  = Integer.parseInt(m_Album.substring(1,5));
	    				    m_Title = m_Album.substring(7);
	    			    }
	    			    
	    			    // If album title is too generic, add the artist name so it will be unique
	    	    	    if (m_Title.toLowerCase().equals("greatest hits") 
		    	    	    	|| m_Title.toLowerCase().equals("the greatest hits")
		    	    	    	|| m_Title.toLowerCase().equals("the greatest")
		    	    	    	|| m_Title.toLowerCase().equals("greatest")
		    	    	    	|| m_Title.toLowerCase().equals("essential")
		    	    	    	|| m_Title.toLowerCase().equals("the essential")
		    	    	    	|| m_Title.toLowerCase().equals("hits")
		    	    	    	|| m_Title.toLowerCase().equals("the hits")
		    	    	    	|| m_Title.toLowerCase().equals("best")
		    	    	    	|| m_Title.toLowerCase().equals("best of")
		    	    	    	|| m_Title.toLowerCase().equals("the best")
		    	    	    	|| m_Title.toLowerCase().equals("the best of")
		    	    	    	|| m_Title.toLowerCase().equals("the very best of")
		    	    	    	|| m_Title.toLowerCase().equals("anthology")
		    	    	    	|| m_Title.toLowerCase().equals("singles")
		    	    	    	|| m_Title.toLowerCase().equals("the singles")
		    	    	    	|| m_Title.toLowerCase().equals("ultimate")
		    	    	    	|| m_Title.toLowerCase().equals("ultimate collection")
		    	    	    	|| m_Title.toLowerCase().equals("the ultimate collection")
		    	    	    	|| m_Title.toLowerCase().equals("definitive collection")
		    	    	    	|| m_Title.toLowerCase().equals("the definitive collection")
		    	    	    	|| m_Title.toLowerCase().equals("the millennium collection")
	    	    	    	) {
	    	    	    	m_Title = m_Artist + " " + m_Title;
	    	    	    }
	    			    
	    		    }

	                // Recursively call the CatalogMP3s method with the current directory File
	                CatalogMP3s(FileArray[i]);

	                // When the recursive call returns for the Dir processing, go to the next file
	                continue;
	            }

	            // Process the file
	            tempStr = FileArray[i].getName();
	    	    FileType = tempStr.substring(tempStr.lastIndexOf(".")+1).toUpperCase();

	    	    /*	            
	            File getName() = 15 - The Mayor Of Simpleton.mp3
	            		Song      = The Mayor Of Simpleton
	            		Artist    = XTC
	            		Album     = (1997) Greatest Hits
	            		Category  = Alternative, Override = 
	            		Year      = 1997
	            		Title     = XTC Greatest Hits
	            		SongIndex = 15
	            		4 >>>>> getName = *AlbumArtSmall.jpg*
	            		4 >>>>> getName = *AlbumArt_{DA85D12C-B9EA-42D9-AC1B-AF6B21BE00D8}_Large.jpg*
	            		4 >>>>> getName = *AlbumArt_{DA85D12C-B9EA-42D9-AC1B-AF6B21BE00D8}_Small.jpg*
	            		4 >>>>> getName = *Folder.jpg*
	            		4 >>>>> getName = *Thumbs.db*
	            		4 >>>>> getName = *desktop.ini*
	            		3 >>>>> getName = *desktop.ini*
	    	     */	            
	            // Check if there is a good artwork image to use and add it to the MP3 (previous MP3?) - maybe not
	    	    
	    	    
	    	    if (!FileType.toUpperCase().equals("MP3"))
	    	    {
	    		    continue;
	    	    }

	            //System.out.println("$$$ File getName() = "+FileArray[i].getName());

	    	    m_Song = tempStr.substring(0,tempStr.lastIndexOf("."));
	    	    m_Song_cnt++;
	    	    m_SongIndex = "01";
	    	    m_AlbumDisc = "0";
	            artistFound = false;

	    	    if (m_Album.equals("Misc"))
	    	    {
	    		    m_Pos = m_Song.indexOf(" - ");
	    		    if (m_Pos >= 0)
	    		    {
	    			    m_Artist = m_Song.substring(0,m_Pos).trim();
	    			    m_Song	 = m_Song.substring(m_Pos+3).trim();
	                    artistFound = true;
	                    
	                    // 2016-10-08 Use "Artist hits" instead of "Misc" to make unique in Google Play and help artwork downloads
    	    	    	m_Title = m_Artist + " hits";
	    		    }
	    	    }
	    	    else if (m_Song.indexOf(" - ") >= 0)
	    	    {
	    		    m_Pos = m_Song.indexOf(" - ");
	    		    if (m_Pos >= 0)
	    		    {
	    			    if (m_Song.startsWith("Disc "))
	    			    {
	    				    m_AlbumDisc = m_Song.substring(5,6);
	    				    m_Song = m_Song.substring(9).trim();
	    			    }

	    		        m_Pos = m_Song.indexOf(" - ");
	    			    if (m_Pos >= 0)
	    			    {
	    				    m_SongIndex = m_Song.substring(0,m_Pos).trim();
	    				    m_Song = m_Song.substring(m_Pos+3).trim();
	    			    }
	    		    }

	    	        if (m_Category.equals("Various") || m_Category.equals("Soundtrack"))
	                {
	    		        m_Pos = m_Song.indexOf(" - ");
	    		        if (m_Pos >= 0)
	    		        {
	    			        m_Artist = m_Song.substring(m_Pos+3).trim();
	    			        m_Song   = m_Song.substring(0,m_Pos).trim();
	                        artistFound = true;
	    		        }

	                }
	    	    }
	    	    else if (m_Song.indexOf("-") >= 0)
	    	    {
	    	    	m_Pos = m_Song.indexOf("-");
	    	    	m_SongIndex = m_Song.substring(0,m_Pos).trim();
	    			m_Song = m_Song.substring(m_Pos+1).trim();
	    	    }
	    	    else
	    	    {
	    	    	System.out.println("Song format not recognized for "+FileArray[i].getName()+", m_Album = "+m_Album+", Artist = "+m_Artist);
	    	    	continue;
	    	    }

	            // Adjust the track number for multi-disc sets (so the order is correct for Nomad and other players)
	            AlbumDiscNum = 0;
	            SongIndexNum = 0;
	            try
	            {
	                AlbumDiscNum = Integer.parseInt(m_AlbumDisc);
	                SongIndexNum = Integer.parseInt(m_SongIndex);
	            }
	            catch (Exception e)
	            {
	                AlbumDiscNum = 0;
	                SongIndexNum = 0;
	            }
	            
	            
	            if (AlbumDiscNum > 1 && AlbumDiscNum < 5 && SongIndexNum < 30)
	            {
	                if (AlbumDiscNum == 4 && SongIndexNum > 9)
	                {
	                    // Out of numbers in the 00 - 99 range
	                }
	                else
	                {
	                    newIndex = SongIndexNum - 1 + ((AlbumDiscNum - 1) * 30);
	                    m_SongIndex = SongNum[newIndex];
	                    SongIndexNum = newIndex;
	                }
	                
	                /*
		            System.out.println("-----------------------------------------------------------------------");
		            System.out.println("File getName() = "+FileArray[i].getName());
		            System.out.println("Song      = "+m_Song);
		            System.out.println("Artist    = "+m_Artist);
		            System.out.println("Album     = "+m_Album);
		            System.out.println("Category  = "+m_Category+", Override = "+m_CategoryOverride);
		            System.out.println("Year      = "+m_Year);
		            System.out.println("Title     = "+m_Title);
		            System.out.println("SongIndex = "+m_SongIndex);
		            System.out.println("AlbumDisc = "+m_AlbumDisc);	                
	                */
	            }
	            
	            
	            if (artistFound)
	            {
	                m_Artist_cnt++;
	                /*
				    Artist artist = new Artist();
				    artist.setArtist(m_Artist);
				    artist.setCategory(m_Category);
	                if (m_CategoryOverride != null && !m_CategoryOverride.equals(""))
	                {
	        			artist.setCategory(m_CategoryOverride);
	                }
	                artist.setComments("");
	                artist.setLastChanged(SysTimeStamp);
	                artist.setMembers("");
	                artist.setRating(0);
	                dbEntityManager.insertArtist(artist);
	                */
	            }
	            else
	            {
	                if (m_Category.equals("Various") || m_Category.equals("Soundtrack"))
	                {
	                    m_Artist = "Misc";
	                }
	            }

	            //========================================================================================
	            //
	            // Make sure the MP3 file can be updated
	            //
	            //========================================================================================
	            fileIsReadOnly = true;
	            //dataSet = null;
	            if (FileArray[i].canWrite())
	            {
	                fileIsReadOnly = false;
	            }
	            else
	            {
	                //---------------------------------------------------------------------------------
	                // If the file is read-only, but the override says to update all Tags
	                // then set the file back to read-write first
	                //---------------------------------------------------------------------------------
	                if (updateAllTags)
	                {
	                	FileArray[i].setWritable(true);
	                    fileIsReadOnly = false;
	                }
	                
	                //---------------------------------------------------------------------------------
	                // If the year is found in the Album name, and it doesn't match the MP3 tag,
	                // then make sure it gets updated by setting the file to read/write
	                //---------------------------------------------------------------------------------
	                /*
	                if (m_Year != defaultYear) {
	            		dataSet = myID3.read(FileArray[i]);
	            		//System.out.println("after read to check year");
	            		System.out.println("dataSet.id3v1Clean.getYear() = "+dataSet.id3v1Clean.getYear());
	            		System.out.println("dataSet.id3v2Clean.getYear() = "+dataSet.id3v2Clean.getYear());
	            		
	            		
	            		//fileYear = (Integer) dataSet.merged.getYear();
	                    //if (m_Year != fileYear) {
		                //	FileArray[i].setWritable(true);
		                //    fileIsReadOnly = false;
	                    //}
	                }
	                */
	            }

	            //-------------------------------------------------------------------------------------
	            //-------------------------------------------------------------------------------------
	            if (fileIsReadOnly)
	            {
	                m_ReadOnly_cnt++;
	                
	                if (makeAllFilesReadWrite)
	                {
	                	FileArray[i].setWritable(true);
	                }
	            }
	            else
	            {
	                System.out.println("-----------------------------------------------------------------------");
	                System.out.println("File getName() = "+FileArray[i].getName());
	                System.out.println("Song      = "+m_Song);
	                System.out.println("Artist    = "+m_Artist);
	                System.out.println("Album     = "+m_Album);
	                System.out.println("Category  = "+m_Category+", Override = "+m_CategoryOverride);
	                System.out.println("Year      = "+m_Year);
	                System.out.println("Title     = "+m_Title);
	                System.out.println("SongIndex = "+m_SongIndex);
	                //System.out.println("AlbumDisc = "+m_AlbumDisc);

	            	try
	                {
	            		
	            		//mp3file = new Mp3File(FileArray[i]);
	            		/*
	            		System.out.println("Length of this mp3 is: " + mp3file.getLengthInSeconds() + " seconds");
	            		System.out.println("Bitrate: " + mp3file.getBitrate() + " kbps " + (mp3file.isVbr() ? "(VBR)" : "(CBR)"));
	            		System.out.println("Sample rate: " + mp3file.getSampleRate() + " Hz");
	            		System.out.println("Has ID3v1 tag?: " + (mp3file.hasId3v1Tag() ? "YES" : "NO"));
	            		System.out.println("Has ID3v2 tag?: " + (mp3file.hasId3v2Tag() ? "YES" : "NO"));
	            		System.out.println("Has custom tag?: " + (mp3file.hasCustomTag() ? "YES" : "NO"));

	            		if (mp3file.hasId3v1Tag()) {
	            			  ID3v1 id3v1Tag = mp3file.getId3v1Tag();
	            			  System.out.println("Track: " + id3v1Tag.getTrack());
	            			  System.out.println("Artist: " + id3v1Tag.getArtist());
	            			  System.out.println("Title: " + id3v1Tag.getTitle());
	            			  System.out.println("Album: " + id3v1Tag.getAlbum());
	            			  System.out.println("Year: " + id3v1Tag.getYear());
	            			  System.out.println("Genre: " + id3v1Tag.getGenre() + " (" + id3v1Tag.getGenreDescription() + ")");
	            			  System.out.println("Comment: " + id3v1Tag.getComment());
	            		}
	            		
	            		if (mp3file.hasId3v2Tag()) {
	            			  ID3v2 id3v2Tag = mp3file.getId3v2Tag();
	            			  System.out.println("Track: " + id3v2Tag.getTrack());
	            			  System.out.println("Artist: " + id3v2Tag.getArtist());
	            			  System.out.println("Title: " + id3v2Tag.getTitle());
	            			  System.out.println("Album: " + id3v2Tag.getAlbum());
	            			  System.out.println("Year: " + id3v2Tag.getYear());
	            			  System.out.println("Genre: " + id3v2Tag.getGenre() + " (" + id3v2Tag.getGenreDescription() + ")");
	            			  System.out.println("Comment: " + id3v2Tag.getComment());
	            			  System.out.println("Composer: " + id3v2Tag.getComposer());
	            			  System.out.println("Publisher: " + id3v2Tag.getPublisher());
	            			  System.out.println("Original artist: " + id3v2Tag.getOriginalArtist());
	            			  System.out.println("Album artist: " + id3v2Tag.getAlbumArtist());
	            			  System.out.println("Copyright: " + id3v2Tag.getCopyright());
	            			  System.out.println("URL: " + id3v2Tag.getUrl());
	            			  System.out.println("Encoder: " + id3v2Tag.getEncoder());
	            			  byte[] albumImageData = id3v2Tag.getAlbumImage();
	            			  if (albumImageData != null) {
	            			    System.out.println("Have album image data, length: " + albumImageData.length + " bytes");
	            			    System.out.println("Album image mime type: " + id3v2Tag.getAlbumImageMimeType());
	            			  }
	            		}
	            		
	            		ID3v2 id3v2Tag;
	            		if (mp3file.hasId3v2Tag()) {
	            		  id3v2Tag = mp3file.getId3v2Tag();
	            		} else {
	            		  // mp3 does not have an ID3v2 tag, let's create one..
	            		  id3v2Tag = new ID3v24Tag();
	            		  mp3file.setId3v2Tag(id3v2Tag);
	            		}
	          
	            		id3v2Tag.setTrack("5");
	            		id3v2Tag.setArtist("An Artist");
	            		id3v2Tag.setTitle("The Title");
	            		id3v2Tag.setAlbum("The Album");
	            		id3v2Tag.setYear("2001");
	            		id3v2Tag.setGenre(12);
	            		id3v2Tag.setComment("Some comment");
	            		id3v2Tag.setComposer("The Composer");
	            		id3v2Tag.setPublisher("A Publisher");
	            		id3v2Tag.setOriginalArtist("Another Artist");
	            		id3v2Tag.setAlbumArtist("An Artist");
	            		id3v2Tag.setCopyright("Copyright");
	            		id3v2Tag.setUrl("http://foobar");
	            		id3v2Tag.setEncoder("The Encoder");
	            		mp3file.save("c://Users//JohnDad//"+FileArray[i].getName());
	            		
	            		*/
	            		

	            		
	            		//audioFile = AudioFileIO.read(FileArray[i]);
	            		//tag = audioFile.getTag();

	            		/*
	            		tagField = null;
                        Iterator<TagField> iterator = tag.getFields();
                        while(iterator.hasNext())
                        {
                        	tagField = iterator.next();
                        	//tag.getFirst(FieldKey.ARTIST_SORT)
                        	System.out.println("tagField.getId() = "+tagField.getId()+", value = *"+tag.getFirst(tagField.getId()) +"*");
                            //if (!tagField.isBinary()) {
                            //    Log.debug(tagField.toString());
                            //}
                        }
	            	*/
	            		
	            		/*
	            		tag.setField(FieldKey.TITLE,m_Song);
	            		tag.setField(FieldKey.ARTIST,m_Artist);
	            		tag.setField(FieldKey.ALBUM_ARTIST,m_Artist);
	            		tag.setField(FieldKey.ALBUM,m_Title);

	                    if (m_Year != defaultYear)
	                    {
		            		tag.setField(FieldKey.YEAR,String.valueOf(m_Year));
	                    }
	            		
	                    String temp_Category = m_Category;
	                    if (!m_CategoryOverride.equals(""))
	                    {
	                        temp_Category = m_CategoryOverride;
	                    }

	            		tag.setField(FieldKey.GENRE,temp_Category);
	            		// If the genere name was not set correctly, just set it to a default value
	            		/*
	            		if (!dataSet.merged.getGenreName().equals(temp_Category)) {
		            		dataSet.merged.setGenreName("Other");
	            		}
						*/
	            		/*
		                if (SongIndexNum != 0) {
		            		tag.setField(FieldKey.TRACK,m_SongIndex);
		                }
		                */

		                //TDRC
		                
	            		//System.out.println("------------------------------------------");
	            		//System.out.println("FieldKey.YEAR = "+FieldKey.YEAR);
                		//tag.deleteField(FieldKey.YEAR);

                		/*
                		iterator = tag.getFields();
                        while(iterator.hasNext())
                        {
                        	tagField = iterator.next();
                        	//tag.getFirst(FieldKey.ARTIST_SORT)
                        	System.out.println("tagField.getId() = "+tagField.getId()+", value = *"+tag.getFirst(tagField.getId()) +"*");
                        	if (tagField.getId().equals("TDRC")) {
                        		tag.deleteField(FieldKey.YEAR);
                        		//tag.setField(tagField);
                        	}
                            //if (!tagField.isBinary()) {
                            //    Log.debug(tagField.toString());
                            //}
                        }
	            		*/

                        /*
	                        TagField coverArt = tag.getFirstField(TagFieldKey.COVER_ART);
	                        if (coverArt != null) {
	                                Log.debug("hasCovertArt.");
	                        }
	                        */	            		
	            			            		
	            		//audioFile.commit();

                        
                        
	            		/*
	            		MyID3 exposes two apis: a simplified interface, and a "raw" interface.
	            		The Simple Interface hides the differences between the ID3v1 and ID3v2.
	            		It maps values from both tags into a MusicMetadata object which has typed accessor methods for the following fields: 
	            			Album, Artist, Comment, Compilation, Composer, Composer2, DurationSeconds, Genre, Producer, ProducerArtist, SongTitle, Year.
	            		 
	            		import org.cmc.music.myid3.*;
	            		import org.cmc.music.common.MusicMetadata;

	            		File src = ?;
	            		MusicMetadataSet src_set = new MyID3().read(src); // read metadata

	            		if (src_set == null) // perhaps no metadata
	            			...
	            			
	            		Debug.debug("src_set", src_set); // dump all info.
	            		Debug.debug("src_set", src_set.getArtist()); 

	            		MusicMetadata metadata = src_set.getSimplified();
	            		String artist = metadata.getArtist();  
	            		String album = metadata.getAlbum();  
	            		String song_title = metadata.getSongTitle(); 
	            		Number track_number = metadata.getTrackNumber(); 

	            		metadata.setArtist("Bob Marley");

	            		File dst = ...
	            		new MyID3().write(src, dst, src_set, metadata);  // write updated metadata

	            		
	            		The Raw Interface exposes the differences between the ID3v1 and ID3v2, every frame, as well as the underlying bytes, etc.
	            		 
	            		import org.cmc.music.myid3.*;
	            		import org.cmc.music.common.MusicMetadata;

	            		File src = ?;
	            		MusicMetadataSet src_set = new MyID3().read(src); // read metadata
	            		String id3v1_artist = src_set.id3_v1_raw.values.getArtist();
	            		String id3v2_artist = src_set.id3_v2_raw.values.getArtist();

	            		byte id3v1_tag_bytes[] = src_set.id3_v1_raw.bytes; // tag bytes
	            		byte id3v2_tag_bytes[] = src_set.id3_v2_raw.bytes; // tag bytes

	            		Vector id3v2_frames = src_set.id3_v2_raw.frames; // 
	            		if (id3v2_frames.size() > 1)
	            		{
	            			MyID3v2Frame first_frame = (MyID3v2Frame) id3v2_frames
	            					.get(0);
	            			String frame_frame_id = first_frame.frame_id;
	            			byte frame_frame_bytes[] = first_frame.data_bytes;
	            		}
	            		*/

	            		
	            		//==================================================================================================================================
	            		// Update the MP3 tags
	                    if (dataSet == null) {
	                    	dataSet = myID3.read(FileArray[i]);
	                    }
	            		//==================================================================================================================================

	            		
	                    //myID3.removeTags(FileArray[i], FileArray[i]);
	                    	                    
	                    //dataSet.id3v2Clean.setAlbum("jjk album");

	            		//System.out.println("dataSet.id3v1Clean = "+dataSet.id3v1Clean);
	            		//System.out.println("dataSet.id3v2Clean = "+dataSet.id3v2Clean);

	            		//System.out.println("dataSet.id3v1Clean.getYear() = "+dataSet.id3v1Clean.getYear());
	            		//System.out.println("dataSet.id3v2Clean.getYear() = "+dataSet.id3v2Clean.getYear());
	                    
	            		/*
	                    System.out.println("dataSet.id3v2Raw.values.getYear() = *"+dataSet.id3v2Raw.values.getYear()+"*");
	                    System.out.println("dataSet.id3v1Raw.values.getYear() = *"+dataSet.id3v1Raw.values.getYear()+"*");

	            		byte id3v1_tag_bytes[] = dataSet.id3v1Raw.bytes;
	            		byte id3v2_tag_bytes[] = dataSet.id3v2Raw.bytes;

	            		System.out.print("id3v1_tag_bytes = *");	            		
	            		for (int j = 0; j < id3v1_tag_bytes.length; j++) {
	            			System.out.print((char)id3v1_tag_bytes[j]);
	            		}
	            		System.out.println("*");
	            		
	            		System.out.print("id3v2_tag_bytes = *");	            		
	            		for (int j = 0; j < id3v2_tag_bytes.length; j++) {
	            			//System.out.println((char)id3v2_tag_bytes[j]+" ("+id3v2_tag_bytes[j]+")");
	            			System.out.print((char)id3v2_tag_bytes[j]);
	            		}
	            		System.out.println("*");
						*/

//	            		TYER Year (replaced by TDRC in v2.4)  *** get rid of TDRC
/*	            		
id3v1_tag_bytes = *TAGCult of personality
id3v2_tag_bytes = *ID3
*/
	            		/*
	            		Vector id3v2_frames = dataSet.id3v2Raw.frames;
	            		if (id3v2_frames.size() > 1)
	            		{
	            			MyID3v2Frame first_frame = (MyID3v2Frame) id3v2_frames.get(0);
	            			String first_frame_id = first_frame.frameID;
	            			System.out.println("first_frame_id = "+first_frame_id);
	            			byte frame_frame_bytes[] = first_frame.dataBytes;
	            		}
	            		*/

	            		
/*	            		
	MusicMetadataSet src_set = new MyID3().read(srcFile);

		IMusicMetadata metadataToWrite;
		if (src_set != null)
		{
			// The source file DID have an ID3v1 or ID3v2 tag (or both).
			// We'll update those values.
			metadataToWrite = src_set.merged;
		} else
		{
			// The file did not have an ID3v1 or ID3v2 tag, so
			// we need to add new tag(s).
			metadataToWrite = MusicMetadata.createEmptyMetadata();
		}

		// here we set or update the artist field.
		metadataToWrite.setArtist(artist);

		new MyID3().write(srcFile, dstFile, src_set, metadataToWrite);
*/
	            		//myID3.setSkipId3v2();

	            		//==================================================================================================================================
	            		dataSet.merged.setSongTitle(m_Song);
	            		dataSet.merged.setArtist(m_Artist);
	            		dataSet.merged.setAlbum(m_Title);
	                    if (m_Year != defaultYear)
	                    {
	                    	dataSet.merged.setYear(m_Year);
	                    }
	            		
	                    String temp_Category = m_Category;
	                    if (!m_CategoryOverride.equals(""))
	                    {
	                        temp_Category = m_CategoryOverride;
	                    }

	            		dataSet.merged.setGenreName(temp_Category);
	            		// If the genere name was not set correctly, just set it to a default value
	            		if (!dataSet.merged.getGenreName().equals(temp_Category)) {
		            		dataSet.merged.setGenreName("Other");
	            		}

		                if (SongIndexNum != 0) {
		            		dataSet.merged.setTrackNumberNumeric(SongIndexNum);
		                }
		                
		                if (AlbumDiscNum != 0) {
		                	//dataSet.merged.setDiscNumber(AlbumDiscNum);
		                }
		                
		                /*
		                ImageData image;
		                dataSet.merged.addPicture(image);
		                */
		                
	            		myID3.update(FileArray[i], dataSet, dataSet.merged);
	                    //dataSet.id3v2Raw.bytes
	            		//myID3.update(FileArray[i], dataSet, dataSet.id3v1Clean);
	                    dataSet = null;
	            		//==================================================================================================================================
	                    
		                if (!makeAllFilesReadWrite) {
		            		if (!FileArray[i].setReadOnly())
		    				{
		                    	throw new Exception("Error setting mp3 to ReadOnly: "+FileArray[i].getAbsolutePath());
		    				}
		                }

	                }
	                catch(Exception e)
	                {
	                    throw new Exception("Error updating mp3 tags: " + FileArray[i].getAbsolutePath()+" "+e);
	                }

	            } // End of if (FileArray[i].canWrite())


	        } // End of for (int i=0; i < FileArray.length; i++)

	        // Decrement the directory level when we're done processing new directory
	        m_DirLevel--;

	    } // End of public void CatalogMP3s(File InFile)


	    //------------------------------------------------------------------------------------------------------
	    // 2013-08-12 JJK
	    // Internal method to process the M3U collections exported from a "local" machine and convert the
	    // files locations to use a network address, so that the collections can be used by all computers
	    // mapping the music to the M: drive
	    // 2015-03-29 JJK - Now using computer as a DLNA media server, so using convert again to make sure M3U's point to the correct location
	    //------------------------------------------------------------------------------------------------------
	    private void ConvertM3Us()
			throws Exception
	    {
	    	String playlistDirStr = "C:\\Users\\JohnDad\\SkyDrive\\Documents\\Music\\Playlist-Export";
	    	String playlistOutDirStr = "D:\\jjkMusic\\Playlists";
	    	
	    	File playlistDir = null;
	    	File playlistOutDir = null;
	    	
            try {
                playlistDir = new File(playlistDirStr);
            }
            catch (Exception e) {
                throw new Exception("Error creating File from "+playlistDirStr+", "+e);
            }

            try {
                playlistOutDir = new File(playlistOutDirStr);
            }
            catch (Exception e) {
                throw new Exception("Error creating File from "+playlistOutDirStr+", "+e);
            }
			
            /*
	    	String hostName = "hostName";      
	    	try {      
	    	    hostName = java.net.InetAddress.getLocalHost().getHostName();
	    	    //System.out.println("hostName = "+hostName);
	    	    // hostName = LNGDAYL-4149706
	    	} catch (Exception e) {      
	    	}     	    	
	    	
	    	String homeGroupHost = "\\\\" + hostName;
	    	String homeGroupLoc = "\\\\" + hostName + "\\" + rootDir.getName();
	    	//System.out.println("homeGroupLoc = "+homeGroupLoc);
			*/
            
            /*
	    	String oldStr1 = "\\\\JJK-PC\\jjkMusic";
	    	String oldStr2 = "\\jjkMusic";

	    	String newStr = "D:\\jjkMusic";
			*/
            
//	    	D:\jjkMusic\Easy Listening\Misc\Misc\Firefall - You are the woman.mp3
//	    	D:\jjkMusic\Rock\Paramore\(2013) Paramore\09 - Still Into You.mp3
//	    	M:\Alternative\Fountains Of Wayne\(1996) Fountains Of Wayne\03 - Joe Rey.mp3

	    	
	    	/*	    	
	    	#EXTM3U
	    	#EXTINF:160,Fountains Of Wayne - Joe Rey
	    	\\JJK-PC\jjkMusic\Alternative\Fountains Of Wayne\(1996) Fountains Of Wayne\03 - Joe Rey.mp3
	    	#EXTINF:169,Miller, Rhett - Caroline
	    	\\JJK-PC\jjkMusic\Folk-Rock\Miller, Rhett\(2009) Rhett Miller\03 - Caroline.mp3

#EXTM3U
#EXTINF:219,Taylor Swift - Shake It Off
\jjkMusic\Pop\Taylor Swift\(2014) 1989\06 - Shake It Off.mp3


	    	\\JJK-PC\jjkMusic\Folk-Rock\Miller, Rhett\(2009) Rhett Miller\03 - Caroline.mp3

	    	#EXTM3U
	    	#EXTINF:183,Bowie, David - Cracked Actor
	    	\jjkMusic\Classic Rock\Bowie, David\(1973) Ziggy Stardust (Soundtrack)\Disc 2 - 02 - Cracked Actor.mp3
	    	#EXTINF:216,Bowie, David - Changes
	    	\jjkMusic\Classic Rock\Bowie, David\(1973) Ziggy Stardust (Soundtrack)\Disc 1 - 09 - Changes.mp3
	    	
	    	\\JJK-PC\jjkMusic
	    	\jjkMusic
	    	
	    	M:
	    	
	    	*/	    	
	    	
	        File FileArray[] = playlistDir.listFiles();
	        //Arrays.sort(FileArray, String.CASE_INSENSITIVE_ORDER);
	        //SortFileArray(FileArray);
	        
	        File tempFile = null;
	        PrintWriter newCollFile = null;
			BufferedReader br = null;
			String inLine = "";
			String newFilename = "";
			String oldFilename = "";
	        for (int i=0; i < FileArray.length; i++)
	        {
	            debugStr = "$$$$$ getName = *"+FileArray[i].getName()+"*";
	            //System.out.println(debugStr);
	            
	            if (FileArray[i].isDirectory()) {
	            	continue;
	            }

	            oldFilename = FileArray[i].getName();
	    	    FileType = oldFilename.substring(oldFilename.lastIndexOf(".")+1).toUpperCase();
	    	    if (!FileType.toUpperCase().equals("M3U"))
	    	    {
	    		    continue;
	    	    }

	    	    // Skip files that are set to Read Only
	    	    /*
	            if (!FileArray[i].canWrite())
	            {
	            	continue;
	            }
	            */
	    	    
	            try
                {
		    	    newFilename = playlistOutDirStr + "/" + oldFilename;
                    tempFile = new File(newFilename);
                    // If the previous file exists, get rid of it
                    if (tempFile.exists()) {
                    	if (!tempFile.delete()) {
                            throw new Exception("Error deleting previous M3U file: " + newFilename);
                    	}
                    }
                    tempFile = null;
                    
                    // Create a new MyCollections M3U file to copy the Exports file into
                	newCollFile = new PrintWriter(new BufferedWriter(new FileWriter(new File(newFilename))));
        			br = new BufferedReader(new FileReader(FileArray[i]));

        			String outLine;
        			while ((inLine = br.readLine()) != null) {
        				//outLine = inLine.replace("M:",homeGroupLoc);
        				//outLine = outLine.replace("D:",homeGroupHost);
        				//outLine = inLine.replace(oldStr1,"M:");
        				
        				if (inLine.startsWith("\\jjkMusic")) {
        					outLine = "D:" + inLine;
        				} else {
        					// Replace JJK-PC with D:
            				outLine = inLine.replace("\\\\JJK-PC\\jjkMusic","D:\\jjkMusic");
        				}
        				
                    	newCollFile.println(outLine);
        			}

        			br.close();
                	newCollFile.close();
                }
                //catch (IOException e)
                catch (Exception e) {
                    throw new Exception("Error opening M3U file: " + e);
                }
	            
	        } // for (int i=0; i < FileArray.length; i++)
	    	
	    } // private void ConvertM3Us(File collectionsDirFile)

	    /*
	    private void renameFile(String oldName, String newName) throws Exception {
	    	File oldFile = new File(oldName);
	    	File newFile = new File(newName);

	    	// Delete the file with the destination name (i.e. the original M3U file)
	    	if (!newFile.delete()) {
                throw new Exception("Error deleting Exports M3U file: " + newFile.getName());
        	}

	    	// Rename the _NEW to the original M3U name
	    	boolean status = oldFile.renameTo(newFile) ;
 
	    	if (status)
	    		System.out.println("File renamed successfully!!");
	    	else
	    		System.out.println("File name myNewFile.txt already exists");
            
	    	// Set the rename file to Read Only
    		if (!newFile.setReadOnly()) {
            	throw new Exception("Error setting file to ReadOnly: "+newName);
			}
	    }
	    */
	    
	    // void BubbleSort(int a[])
	    private void SortFileArray(File FArray[])
	    {
	        boolean swapped;
	        int i,j;
	        File tempFile;

	        for (i=FArray.length; --i >=0;)
	        {
	            swapped = false;
	            for (j=0; j < i; j++)
	            {
	                if (FArray[j].toString().compareTo(FArray[j+1].toString()) > 0)
	                {
	                    tempFile = FArray[j+1];
	                    FArray[j+1] = FArray[j];
	                    FArray[j] = tempFile;
	                    swapped = true;
	                }
	            }

	            if (!swapped)
	            {
	                return;
	            }
	        }
	    } // End of void SortFileArray(File FArray[])
	
	
} //
