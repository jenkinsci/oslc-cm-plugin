
// FIXME Missing copyright notice

package com.coclico.oauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

public class OAuth {

    public static void main(String[] args) throws Exception {
    	Properties prop = new java.util.Properties();
    	
    	try {
            String PropsFile = "com/coclico/oauth/FusionForge.properties";
    		
    		InputStream in = FusionForgeOAuth.class.getClassLoader().getResourceAsStream(PropsFile);
    		if (in == null) {
    			//File not found!
    			System.out.println("PROPERTIES FILE NOT FOUND!!");
    		}
    		
    		prop.load(in);

    	} catch (IOException ex) {
    		ex.printStackTrace();
        }
    	
    	String consumer_key = prop.getProperty("CONSUMER_KEY");
		String consumer_secret = prop.getProperty("CONSUMER_SECRET");

		// The server endpoints
		String request_token_url = prop.getProperty("REQUEST_TOKEN_URL");
		String access_token_url = prop.getProperty("ACCESS_TOKEN_URL");
		String authorisation_url = prop.getProperty("AUTHORISATION_URL");
		
        OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumer_key, consumer_secret);
    	
    	System.out.println("Enter project name: ");
    	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String project = br.readLine();
        
        OAuthProvider provider = new CommonsHttpOAuthProvider(
                request_token_url,
                access_token_url,
                authorisation_url + "?type=group&name=" + project + "&callback=false");
    	
        System.out.println("Fetching request token...");

        // we do not support callbacks, thus pass OOB
        String authUrl = provider.retrieveRequestToken(consumer, "http://www.example.com");

        System.out.println("Request token: " + consumer.getToken());
        System.out.println("Token secret: " + consumer.getTokenSecret());

        System.out.println("Now visit:\n" + authUrl + "\n... and grant this app authorization");
        System.out.println("Enter the verification code and hit ENTER when you're done");

        br = new BufferedReader(new InputStreamReader(System.in));
        String code = br.readLine();

        System.out.println("Fetching access token from FusionForge...");

        provider.retrieveAccessToken(consumer, code);

        System.out.println("Access token: " + consumer.getToken());
        System.out.println("Token secret: " + consumer.getTokenSecret());

    }
}
