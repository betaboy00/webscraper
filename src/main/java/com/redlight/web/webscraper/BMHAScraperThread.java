package com.redlight.web.webscraper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class BMHAScraperThread implements Runnable {

	private static volatile boolean isRunning = true;
	private static volatile boolean isInitiate = true;
	
	private static final long HOUR = 60 * 60 * 1000;
    private static final long MIN = 60 * 1000;
    private static final long HALF_HOUR = 30 * 60 * 1000;
    private static final long FIFTEEN_MIN = 15 * 60 * 1000;
    private static final long INTERVAL = HALF_HOUR;
    
    private static final String URL = "http://www.burnabyminor.com/default.aspx?p=reptryouts";
	private static final String CONTENT_ELEMENT_SELECTOR = "div.divcontent";
	private static final String REP_TEAM_DECLARATION_ELEMENT_SELECTOR = "span:contains(Rep Team Declaration)+span";
	private static final String REP_TRYOUT_PROCESS_ELEMENT_SELECTOR = "span:contains(Rep Tryout Process)+br+span";
	private static final String REP_FEES_ELEMENT_SELECTOR_DESC = "span:matches(^Rep.+Fees)+br+span";
	private static final String REP_FEES_ELEMENT_SELECTOR_FEES_LINK = "span:matches(^Rep.+Fees)+br+span+br+span";
	private static final String REP_TRYOUT_SCHEDULE_ELEMENT_SELECTOR = "span:contains(Rep Tryout Schedule)+span";
	
	private static final String API_KEY = "api:key-5af3d1752b95a3c6676005974eefc37e";
	private static final String DOMAIN = "sandbox881d3606a556474e97884edf8a6bda50.mailgun.org";
	private static final String MAIL_GATEWAY = "https://api.mailgun.net/v3/"+ DOMAIN +"/messages";
	
	private static final String HTML = "html";
	private static final String TEXT = "text";
	private static final String TO = "to";
	private static final String FROM = "from";
	private static final String SUBJECT = "subject";
	private static final String CONTENT_TYPE = HTML;
	private static final String EMPTY_STRING = "";
	
	private static final String DESTINATION = "betaboy00@hotmail.com";
	private static final String SENDER = "BMHA TryOut <betaboy00@" + DOMAIN + ">";
	private static final String SUBJECT_STRING = "Rep Tryouts Page Change Detected !!!";
	
	private static final String START_HIGHLIGHT = "<span style=\"background-color: #FFFF00\">";
	private static final String END_HIGHLIGHT = "</span>";
	
	
	private static final HashMap<String, String> base_contents = new HashMap<String, String>();
	private static final HashMap<String, String> selector_strings = new HashMap<String, String>();
	
	static {
		base_contents.put(REP_TEAM_DECLARATION_ELEMENT_SELECTOR, EMPTY_STRING);
		base_contents.put(REP_TRYOUT_PROCESS_ELEMENT_SELECTOR, EMPTY_STRING);
		base_contents.put(REP_FEES_ELEMENT_SELECTOR_DESC, EMPTY_STRING);
		base_contents.put(REP_FEES_ELEMENT_SELECTOR_FEES_LINK, EMPTY_STRING);
		base_contents.put(REP_TRYOUT_SCHEDULE_ELEMENT_SELECTOR, EMPTY_STRING);
		
		selector_strings.put(REP_TEAM_DECLARATION_ELEMENT_SELECTOR, "Rep Team Declaration");
		selector_strings.put(REP_TRYOUT_PROCESS_ELEMENT_SELECTOR, "Rep Tryout Process");
		selector_strings.put(REP_FEES_ELEMENT_SELECTOR_DESC, "Rep Fees Description");
		selector_strings.put(REP_FEES_ELEMENT_SELECTOR_FEES_LINK, "Rep Fees Link");
		selector_strings.put(REP_TRYOUT_SCHEDULE_ELEMENT_SELECTOR, "Rep Tryout Schedule");
	}
	
	public void run() {
		while (isRunning) {
			System.out.println("Running " + this.getClass().getSimpleName() + "....");
			try{
				if (isInitiate) {
					getInitialSnapshot();
					isInitiate = false;
				}
				
				// check base on frequency
				Thread.sleep(INTERVAL);
				checkForChanges();
				
			} catch (InterruptedException e) {
				// interrupted, do a final check for changes
				System.out.println("Thread " + this.getClass().getSimpleName() + " interrupted.");
				isRunning = false;
				checkForChanges();
			} catch (IOException ioe) {
				System.out.println(ioe.getMessage());
			}
		}
	}

	/**
	 * Highlight the changes of the content
	 * 
	 * @param htmlContent
	 *            The content that contain the changes
	 * @param changedStr
	 *            The changes to be highlighted
	 * @return the content that contain the highlighted changes
	 */
	private String highlightChanges(String htmlContent, String changedStr) {
		String result = null;
		result = StringUtils.replace(htmlContent, changedStr, START_HIGHLIGHT + changedStr + END_HIGHLIGHT);
		return result;
	}

	/**
	 * Check for changes
	 */
	private void checkForChanges() {
		// check for content changes
		String content = checkAndHighlightChanges();
		if (content != null && !content.isEmpty()) {
			// changes found, send email
			sendEmail(content);
		}
	}

	/**
	 * Check for changes in the content. If changes are detected, highlight the
	 * changes in the content.
	 * 
	 * @return The entire content containing the highlighted changes, or null if no
	 *         changes detected.
	 */
	private String checkAndHighlightChanges() {
		String content = null;
		boolean isChanged = false;

		System.out.println("Checking content on: " + Calendar.getInstance().getTime() + " .... ");

		try {
			Document doc = Jsoup.connect(URL).get();

			// The main content that will be manipulated (changes highlighted) and returned
			content = doc.select(CONTENT_ELEMENT_SELECTOR).first().toString();

			for (String selector : base_contents.keySet()) {

				System.out.print(String.format("***** Checking %-25s ....", selector_strings.get(selector)));

				Elements elems = doc.select(selector);
				if (!elems.isEmpty()) {
					String current_content = elems.first().toString();
					String difference = StringUtils.difference(base_contents.get(selector), current_content);

					if (difference != null && !difference.isEmpty()) {
						System.out.println("Changes Found !!!!");
						System.out.println(difference);
						System.out.println();
						// update the base_content
						base_contents.put(selector, current_content);
						// highlight the changes in the content document
						content = highlightChanges(content, difference);
						isChanged = true;
					} else {
						System.out.println("No Changes");
						System.out.println();
					}
				}

			}
		} catch (IOException e) {
			System.out.println("Can't connect to URL: " + e);
		}

		return (isChanged ? content : null);
	}

	/**
	 * Send email containing the changed content and highlighted changes.
	 * 
	 * @param content
	 *            content containing the highlighted changes
	 */
	private void sendEmail(String content) {

		try {
			final NameValuePair[] data = { new BasicNameValuePair(FROM, SENDER),
					new BasicNameValuePair(TO, DESTINATION), new BasicNameValuePair(SUBJECT, SUBJECT_STRING),
					new BasicNameValuePair(CONTENT_TYPE, content) };

			HttpClient httpClient = HttpClients.createMinimal();
			HttpPost httpPost = new HttpPost(MAIL_GATEWAY);

			httpPost.setEntity(new UrlEncodedFormEntity(Arrays.asList(data)));
			String encoding = Base64.getEncoder().encodeToString((API_KEY).getBytes());

			httpPost.addHeader("Authorization", "Basic " + encoding);
			HttpResponse httpResponse = httpClient.execute(httpPost);

			String responseString = EntityUtils.toString(httpResponse.getEntity());
			System.out.println(responseString);
		} catch (Exception e) {
			System.out.println("Not able to send email: " + e.getMessage());
		}
	}


	/**
	 * Get the initial snap shot of the content.
	 * 
	 * @throws IOException
	 *             if not able to connect to the URL
	 */
	private void getInitialSnapshot() throws IOException {

		try {

			System.out.println("Getting initial snapshot on: " + Calendar.getInstance().getTime() + " ...... ");
			Document doc = Jsoup.connect(URL).get();

			for (String s : base_contents.keySet()) {
				Elements elems = doc.select(s);
				if (!elems.isEmpty()) {
					base_contents.put(s, elems.first().toString());
				}
			}

			System.out.println(base_contents);

		} catch (IOException e) {
			System.out.println("Can't connect to URL: " + e);
			throw e;
		}

	}

}
