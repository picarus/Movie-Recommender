/*
 * This is the MIT license, see also http://www.opensource.org/licenses/mit-license.html
 *
 * Copyright (c) 2001 Brian Pitcher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

// $Header: /cvsroot/weblech/weblech/src/weblech/spider/Spider.java,v 1.8 2002/06/09 11:34:38 weblech Exp $

package weblech.spider;

import weblech.util.Logger;

import java.util.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;


public class Spider extends Logger implements Runnable, Constants
{
    /** Config for the spider */
    private SpiderConfig config;
    /**
     * Download queue.
     * Thread safety: To access the queue, first synchronize on it.
     */
    private DownloadQueue queue;
    /**
     * Set of URLs downloaded or scheduled, so we don't download a
     * URL more than once.
     * Thread safety: To access the set, first synchronize on it.
     */
    private Set<URL> urlsDownloadedOrScheduled;
    /**
     * Set of URLs currently being downloaded by Spider threads.
     * Thread safety: To access the set, first synchronize on it.
     */
    private Set<URLToDownload> urlsDownloading;
    /**
     * Number of downloads currently taking place.
     * Thread safety: To modify this value, first synchronize on
     *                the download queue.
     */
    private int downloadsInProgress;
    /** Whether the spider should quit */
    private boolean quit;
    /** Count of running Spider threads. */
    private int running;
    /** Time we last checkpointed. */
    private long lastCheckpoint;

    private HTMLParser htmlParser; 
    
    
    static String readFile(String path) throws  IOException {
		return readFile(path,StandardCharsets.ISO_8859_1);
	}
	
	static String readFile(String path, Charset encoding) throws IOException 
	{
	  byte[] encoded = Files.readAllBytes(Paths.get(path));
	  return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}
    
	/**************************************************************************/
    public Spider(SpiderConfig config, File[] listOfFiles){
    	initSpider(config);
    	initURLFromFiles(listOfFiles);
    	//queue.queueURLs(URLToDownload.createList(config.getStartLocations()));
    }
    
    
    public Spider(SpiderConfig config) {
        initSpider(config);        
        //queue.queueURLs(URLToDownload.createList(config.getStartLocations()));
    }

	protected void initSpider(SpiderConfig config) {
		this.config = config;
        htmlParser = new HTMLParser(config);
        queue = new DownloadQueue(config);
        
        urlsDownloadedOrScheduled = new HashSet<URL>();
        urlsDownloading = new HashSet<URLToDownload>();        
        downloadsInProgress = 0;
        lastCheckpoint = 0;
	}

    
    public void initURLFromFiles(File[] listOfFiles){
    	 String textContent;
    	 List<URL> urlList;
         for (int i = 0; i < listOfFiles.length; i++) {
 			if (listOfFiles[i].isFile()) {
 				try {
 					textContent=readFile(listOfFiles[i].getAbsolutePath());
 					urlList=htmlParser.parseLinksInDocument(listOfFiles[i].toURI().toURL(), textContent);
 					urlList=filterURLsWithTranslation(urlList);

 					queue.queueURLs(urlList,0);
 				} catch(IOException e){
 					System.err.println(e.getMessage());
 				}				
 		    }
 		}	
    }
    
    
    public void start()
    {
        quit = false;
        running = 0;

        for(int i = 0; i < config.getSpiderThreads(); i++)
        {
            _logClass.info("Starting Spider thread");
            Thread t = new Thread(this, "Spider-Thread-" + (i + 1));
            t.start();
            running++;
        }
    }

    public void stop()
    {
        quit = true;
    }

    public boolean isRunning()
    {
        return running == 0;
    }

    private void checkpointIfNeeded()
    {
        if(config.getCheckpointInterval() == 0)
        {
            return;
        }

        if(System.currentTimeMillis() - lastCheckpoint > config.getCheckpointInterval())
        {
            synchronized(queue)
            {
                if(System.currentTimeMillis() - lastCheckpoint > config.getCheckpointInterval())
                {
                    writeCheckpoint();
                    lastCheckpoint = System.currentTimeMillis();
                }
            }
        }
    }

    private void writeCheckpoint()
    {
        _logClass.debug("writeCheckpoint()");
        try
        {
            FileOutputStream fos = new FileOutputStream("spider.checkpoint", false);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(queue);
            oos.writeObject(urlsDownloading);
            oos.close();
        }
        catch(IOException ioe)
        {
            _logClass.warn("IO Exception attempting checkpoint: " + ioe.getMessage(), ioe);
        }
    }

    @SuppressWarnings("unchecked")
	public void readCheckpoint()
    {
    	ObjectInputStream ois;
        try
        {
            FileInputStream fis = new FileInputStream("spider.checkpoint");
            ois = new ObjectInputStream(fis);
            queue = (DownloadQueue) ois.readObject();
            urlsDownloading = (Set<URLToDownload>) ois.readObject();
            queue.queueURLs(urlsDownloading);
            urlsDownloading.clear(); 
            ois.close();
        }
        catch(Exception e)
        {
            _logClass.error("Caught exception reading checkpoint: " + e.getMessage(), e);
        }
        
    }

    public void run() //executed as part of Thread.run
    {
        
        URLGetter urlGetter = new URLGetter(config);

        while((queueSize() > 0 || downloadsInProgress > 0) && quit == false)
        {
            checkpointIfNeeded();
            if(queueSize() == 0 && downloadsInProgress > 0)
            {
                // Wait for a download to finish before seeing if this thread should stop
                try
                {
                    Thread.sleep(QUEUE_CHECK_INTERVAL);
                }
                catch(InterruptedException ignored)
                {
                }
                // Have another go at the loop
                continue;
            }
            else if(queueSize() == 0)
            {
                break;
            }
            URLToDownload nextURL;
            synchronized(queue)
            {
                nextURL = queue.getNextInQueue();
                downloadsInProgress++;
            }
            synchronized(urlsDownloading)
            {
                urlsDownloading.add(nextURL);
            }
            int newDepth = nextURL.getDepth() + 1;
            int maxDepth = config.getMaxDepth();
            synchronized(urlsDownloading)
            {
                urlsDownloading.remove(nextURL);
            }
	        List<URL> newURLs = downloadURL(nextURL, urlGetter, htmlParser);

            newURLs = filterURLs(newURLs);

            ArrayList<URLToDownload> u2dsToQueue = new ArrayList<URLToDownload>();
            for(Iterator<URL> i = newURLs.iterator(); i.hasNext(); )
            {
                URL u = i.next();
                // Download if not yet downloaded, and the new depth is less than the maximum
                synchronized(urlsDownloadedOrScheduled)
                {
                    if(!urlsDownloadedOrScheduled.contains(u)
                    && (maxDepth == 0 || newDepth <= maxDepth))
                    {
                        u2dsToQueue.add(new URLToDownload(u, nextURL.getURL(), newDepth));
                        urlsDownloadedOrScheduled.add(u);
                    }
                }
            }
            synchronized(queue)
            {
                queue.queueURLs(u2dsToQueue);
                downloadsInProgress--;
            }
        }
        _logClass.info("Spider thread stopping");
        running--;
    }

    /**
     * Get the size of the download queue in a thread-safe manner.
     */
    private int queueSize()
    {
        synchronized(queue)
        {
            return queue.size();
        }
    }
    
    /**
     * Get a URL, and return new URLs that are referenced from it.
     *
     * @return A List of URL objects.
     */
    private List<URL> downloadURL(URLToDownload url, URLGetter urlGetter, HTMLParser htmlParser)
    {
        _logClass.debug("downloadURL(" + url + ")");

        // Bail out early if image and already on disk
        URLObject obj = new URLObject(url.getURL(), config);
        if(obj.existsOnDisk())
        {
            if(config.refreshHTMLs() && (obj.isHTML() || obj.isXML()))
            {
                _logClass.info("Q: [" + queue + "] " + url);
                obj = urlGetter.getURL(url);
            }
            else if(config.refreshImages() && obj.isImage())
            {
                _logClass.info("Q: [" + queue + "] " + url);
                obj = urlGetter.getURL(url);
            }
        }
        else
        {
            _logClass.info("Q: [" + queue + "] " + url);
            int n_attempts=0;
            int maxAttempts=config.getMaxAttempts();
            do {            	
            	n_attempts++;
            	obj = urlGetter.getURL(url);
            } while (obj==null && n_attempts<maxAttempts);
            if (obj==null)
            	_logClass.error("Failed to Download after attempts("+maxAttempts+"):"+url);
        }

        if(obj == null)
        {        	
            return new ArrayList<>();
        }

        if(!obj.existsOnDisk())
        {
            obj.writeToFile();
        }

        if(obj.isHTML() || obj.isXML()) {
            return htmlParser.parseLinksInDocument(url.getURL(), obj.getStringContent());
        }
        else if(obj.isImage()) {
            return new ArrayList<>();
        }
        else
        {
            _logClass.warn("Unsupported content type received: " + obj.getContentType());
            _logClass.info("URL was " + url);
            return new ArrayList<>();
        }
    }

    private List<URL> filterURLs(List<URL> URLs)
    {
        
        ArrayList<URL> retVal = new ArrayList<URL>();

        synchronized(urlsDownloadedOrScheduled)
        {
            for(Iterator<URL> i = URLs.iterator(); i.hasNext(); )
            {
                URL u =  i.next();
                if( filter(u)) {
                    retVal.add(u);
                }
            }
        }
        return retVal;
    }
    
    private List<URL> filterURLsWithTranslation(List<URL> URLs)
    {
        
        ArrayList<URL> retVal = new ArrayList<URL>();

        synchronized(urlsDownloadedOrScheduled)
        {
            for(Iterator<URL> i = URLs.iterator(); i.hasNext(); )
            {
                URL u =  i.next();
                if( filter(u)) {
                    retVal.add(u);
                }
                String urlStr=u.toExternalForm();
                if (urlStr.contains("/userrec.php?user_id")){
                	urlStr=urlStr.replace("userrec.php", "userratings.php");
                	URL u2;
					try {
						u2 = URLFactory.createURL(u, urlStr);
						if( filter(u2)) {
	                        retVal.add(u2);
	                    }
					} catch (MalformedURLException e) {
					}
                }
            }
        }
        return retVal;
    }

	protected boolean filter(URL u) {
		return ! urlsDownloadedOrScheduled.contains(u) && config.isCrawlable(u) && !config.isNoFollow(u);
	}

}
