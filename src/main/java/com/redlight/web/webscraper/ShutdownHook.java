package com.redlight.web.webscraper;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * JVM shutdown hook that maintains a list of threads to be shutdown prior to JVM shutdown
 * @author ray.lian
 *
 */
public class ShutdownHook extends Thread{
	
	private static ShutdownHook _INSTANCE = new ShutdownHook();
	// list of threads to be shutdown 
	private ConcurrentLinkedQueue<Thread> threadList = new ConcurrentLinkedQueue<Thread>();
	
	private static final long MAX_WAIT_MS = 1000;
	
	private ShutdownHook() {
	}
	
	/**
	 * Get single instance 
	 * @return ShutdownHook
	 */
	public static ShutdownHook getInstance() {
		return _INSTANCE;
	}
	
	/**
	 * Add thread to the list
	 * @param t thread to add
	 */
	public void addThread(Thread t) {
		threadList.add(t);
		System.out.println("Thread List: " + threadList);
	}
	
	/**
	 * Clear the thread list
	 */
	public void clearThreads() {
		threadList.clear();
	}
	
	public void run() {
		System.out.println("Run Thread List: " + threadList);
		for (Thread t : threadList) {
			System.out.println("Shutting down " + t.getName());
			t.interrupt();
			try  {
				t.join(MAX_WAIT_MS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
