package mai.cn.film;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Category;

import weblech.spider.Spider;
import weblech.spider.SpiderConfig;
import weblech.ui.TextSpider;

public class FilmCrawler {
	
	private static Category _logClass = Category.getInstance(TextSpider.class);
	
	//static String path="D:\\weblech\\sample";
	static String path="D:\\weblech\\manual";
	static String propsFile="config\\FilmSpider.properties";
	
	public static void main(String[] args) {
		
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
	
		Properties props = null;
        try
        {
            FileInputStream propsIn = new FileInputStream(propsFile);
            props = new Properties();
            props.load(propsIn);
            propsIn.close();
        }
        catch(FileNotFoundException fnfe)
        {
            _logClass.error("File not found: " + args[0], fnfe);
            System.exit(1);
        }
        catch(IOException ioe)
        {
            _logClass.error("IO Exception caught reading config file: " + ioe.getMessage(), ioe);
            System.exit(1);
        }
        
        _logClass.debug("Configuring Spider from properties");
        SpiderConfig config = new SpiderConfig(props);
        _logClass.debug(config);
        Spider spider = new Spider(config, listOfFiles);
        
       // spider.readCheckpoint();
        
        _logClass.info("Starting Spider...");
        spider.start();

        System.out.println("\nHit any key to stop Spider\n");
        try
        {
            while(spider.isRunning())
            {
                if(System.in.available() != 0)
                {
                    System.out.println("\nStopping Spider...\n");
                    spider.stop();
                    break;
                }
                pause(weblech.spider.Constants.SPIDER_STOP_PAUSE);
                
            }
        }
        catch(IOException ioe)
        {
            _logClass.error("Unexpected exception caught: " + ioe.getMessage(), ioe);
            System.exit(1);
        }
         	
	}

	 private static void pause(long howLong){
        try
        {
            Thread.sleep(howLong);
        }
        catch(InterruptedException ignored)
        {
        }
    }

}
