/*
 * The MIT License
 * 
 * Copyright (c) 2011 Institut TELECOM, Madhumita DHAR, 
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.oslccm;

import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import jenkins.plugins.oslccm.CMConsumer.DescriptorImpl;

import hudson.model.Action;
import hudson.model.AbstractBuild;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.client.methods.HttpPost;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class OslccmBuildAction implements Action {

	private static final Logger LOGGER = Logger.getLogger(OslccmBuildAction.class.getName());
	private AbstractBuild<?, ?> build;
	private String url;
	private String width;
	private String height;
	private String Ourl;
	private String oauthUrl;
	//private String name;
	private OAuthConsumer consumer;
	private String buildUrl;
	
	public OslccmBuildAction(AbstractBuild<?, ?> build, String delegUrl, String oauthUrl, int width, int height, OAuthConsumer consumer, String absoluteBuildURL) {
		this.build = build;
		Ourl = delegUrl;
		this.oauthUrl = oauthUrl;
		this.width = width + "";
		this.height = height + "";
		LOGGER.info("New buid action added with url: " + url + ", width:" + width + ", height:" + height);
		this.consumer = consumer;
		this.buildUrl = absoluteBuildURL;
	}

	public AbstractBuild<?, ?> getBuild() {
		return build;
	}
	
	public String getUrl()	{
		Date now = new Date();
		LOGGER.info("Old url: " + Ourl);
        url = Ourl;
        try {
        	if(url.indexOf("?")>0) {
        		url = url + "&build_url=" + this.buildUrl + "&build_number=" + this.getBuild().number;
        	}else {
        		url = url + "?build_url=" + this.buildUrl + "&build_number=" + this.getBuild().number;
        	}
			if(this.oauthUrl==null) {
				url = consumer.sign(url);
			}else {
				String tempUrl = consumer.sign(this.oauthUrl);
				int index = tempUrl.indexOf("?");
				String oauthParams = consumer.sign(this.oauthUrl).substring(index+1);
				url = url + "&" + oauthParams;
			}
			
			LOGGER.info("Signed url: " + url);
			return url;
		} catch (OAuthMessageSignerException e1) {
			LOGGER.log(Level.SEVERE, "The url could not be signed!", e1);
		} catch (OAuthExpectationFailedException e1) {
			LOGGER.log(Level.SEVERE, "The url could not be signed!", e1);
		} catch (OAuthCommunicationException e1) {
			LOGGER.log(Level.SEVERE, "The url could not be signed!", e1);
		}
        
		return url;
		
	}
	
	public String getWidth()	{
		return width;
	}
	
	public String getHeight()	{
		return height;
	}

	public void doDynamic(StaplerRequest req, StaplerResponse res)
			throws IOException {
		Date now = new Date();
		res.sendRedirect2("DelegatedBugReport?time="+now.toString());
		return;
		
	}
	
	public void doBlahBlah(StaplerRequest req, StaplerResponse res)
	throws IOException {
		LOGGER.info("Doing some blah blah");
		
	}

	public String getIconFileName() {
		//return "document.gif";
		return null;
	}

	public String getDisplayName() {
		
		return "Delegated Bug Report Creation";
	}

	public String getUrlName() {
		return "OSLC-CM";
	}
	
	
}