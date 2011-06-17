/**
 * This file is (c) Copyright 2011 by Madhumita DHAR, Institut TELECOM
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * This program has been developed in the frame of the COCLICO
 * project with financial support of its funders.
 *
 */

package jenkins.plugins.oslccm;

import java.io.IOException;
import java.util.Date;
import java.util.Random;
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
	//private String name;
	private OAuthConsumer consumer;
	private String buildUrl;
	
	public OslccmBuildAction(AbstractBuild<?, ?> build, String delegUrl, int width, int height, OAuthConsumer consumer, String absoluteBuildURL) {
		this.build = build;
		Ourl = delegUrl;
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
        	url = url + "?build_url=" + this.buildUrl + "&build_number=" + this.getBuild().number;
			url = consumer.sign(url);
        	LOGGER.info("Signed url: " + url);
			return url;
		} catch (OAuthMessageSignerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (OAuthExpectationFailedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (OAuthCommunicationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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