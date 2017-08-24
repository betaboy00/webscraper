package com.redlight.web.webscraper;

import com.redlight.web.webscraper.ShutdownHook;

/**
 * WebScraper
 *
 */
public class WebScraper
{
	public static void main( String[] args )
    {
        System.out.println( "Starting Page Tracking....." );
        
        try {
        	
        	Thread workerThread = new Thread(new BMHAScraperThread());
        	
        	ShutdownHook hook = ShutdownHook.getInstance();
        	hook.addThread(workerThread);
        	
        	Runtime.getRuntime().addShutdownHook(hook);

        	workerThread.start();
        } catch (Exception e) {
        	System.out.println(e);
        }
        
    }
	
}
