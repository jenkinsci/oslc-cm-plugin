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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import org.json.JSONStringer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.Mailer;
import hudson.util.FormValidation;

import org.kohsuke.stapler.QueryParameter;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;


public class CMConsumer extends Notifier {
	
	private static final Logger LOGGER = Logger.getLogger(CMConsumer.class.getName());
	
	private String token;
	private String tokenSecret;
	private boolean manual;
	private boolean automatic;
	private String url;
	private String delegUrl;
	private boolean eachBuildFailure;
	private boolean firstBuildFailure;
	private int width;
	private int height;
	private List<String> bugprops;
	private final int HEIGHT = 600;
	private final int WIDTH = 800;
	private boolean defaultProps;
	private OAuthConsumer consumer;
	
	public CMConsumer(String token, String tokenSecret, boolean manual, boolean automatic, String url, String delegUrl, String width, String height, boolean eachBuildFailure, boolean firstBuildFailure, List<String> newprops)	{
		this.token = token;
		this.tokenSecret = tokenSecret;
		this.manual = manual;
		this.automatic = automatic;
		this.url = url;
		this.delegUrl = delegUrl;		
		this.width = isInteger(width, WIDTH);
		this.height = isInteger(height, HEIGHT);
		this.eachBuildFailure = eachBuildFailure;
		this.firstBuildFailure = firstBuildFailure;
		if(newprops==null)	{
			LOGGER.info("Newprops = NULL");
			this.defaultProps = true;
			this.bugprops = new ArrayList<String>();
			
		}else	{
			this.defaultProps = false;
			this.bugprops = newprops;
		}
	}
	
	@DataBoundConstructor
	public CMConsumer(String token, String tokenSecret)	{
		this.token = token;
		this.tokenSecret = tokenSecret;
	}
	
	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		return new OslccmProjectAction(project);
	}
	
	public boolean getEachBuildFailure()	{
		return eachBuildFailure;
	}
	
	public boolean getFirstBuildFailure()	{
		return firstBuildFailure;
	}
	
	public String getToken()	{
		return token;
	}
	
	public String getTokenSecret()	{
		return tokenSecret;
	}
	
	public boolean getManual()	{
		return manual;
	}
	
	public boolean getAutomatic()	{
		return automatic;
	}
	
	public String getUrl()	{
		return url;
	}
	
	public String getDelegUrl()	{
		return delegUrl;
	}
	
	public int getWidth()	{
		return width;
	}
	
	public int getHeight()	{
		return height;
	}
	
	/**
     * to display pre-defined bug properties for automatic bug creation
     */
    public List<String> getBugprops() {
    	if(this.defaultProps)	return null;
    	else return Collections.unmodifiableList(bugprops);
    }
    
    public void setBugprops(List<String> newprops) {
        this.bugprops = newprops;
    }
	
	private static int isInteger(String num, int n){
		try{
			return Integer.parseInt(num);
		}catch(Exception e){
			return n;
		}
	}
	
	private static String createTinyUrl(String url) throws IOException {
		org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();
		GetMethod gm = new GetMethod("http://tinyurl.com/api-create.php?url="
				+ url.replace(" ", "%20"));

		int status = client.executeMethod(gm);
		if (status == HttpStatus.SC_OK) {
			return gm.getResponseBodyAsString();
		} else {
			throw new IOException("Error in tinyurl: " + status);
		}

	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}
	
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
		LOGGER.info("Consumer Key: " + ((DescriptorImpl) getDescriptor()).getConsumerKey());
		LOGGER.info("Consumer Secret: " + ((DescriptorImpl) getDescriptor()).getConsumerSecret());
		LOGGER.info("Token: " + token);
		LOGGER.info("Token Secret: " + tokenSecret);
		LOGGER.info("Manual: " + manual);
		LOGGER.info("Automatic: " + automatic);
		LOGGER.info("URL: " + url);
		LOGGER.info("Delegated URL: " + delegUrl);
		LOGGER.info("Delegated URL width: " + width);
		LOGGER.info("Delegated URL height: " + height);
		LOGGER.info("On every failure: " + eachBuildFailure);
		LOGGER.info("On first failure: " + firstBuildFailure);
		
		String uiUrl = this.getDelegUrl();
		if(manual)	{
			consumer = new CommonsHttpOAuthConsumer(((DescriptorImpl) getDescriptor()).getConsumerKey(), ((DescriptorImpl) getDescriptor()).getConsumerSecret());
	        consumer.setTokenWithSecret(getToken(), getTokenSecret());
	        String absoluteBuildURL = ((DescriptorImpl) getDescriptor()).getUrl() + build.getUrl();
			
			OslccmBuildAction bAction = new OslccmBuildAction(build, uiUrl, this.width, this.height, consumer, absoluteBuildURL);
			build.addAction(bAction);
			LOGGER.info("Adding delegated create action");
		}
		
		if (shouldSendBugReport(build)) {
			try {
				String report = createBugReport(build);
				sendReport(report);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Unable to send bug report.", e);
			}
		}
		return true;
	}

	private String createBugReport(AbstractBuild<?, ?> build) {
		String projectName = build.getProject().getName();
		String result = build.getResult().toString();
		String tinyUrl = "";
		String absoluteBuildURL = ((DescriptorImpl) getDescriptor()).getUrl() + build.getUrl();
		try {
			tinyUrl = createTinyUrl(absoluteBuildURL);
		} catch (Exception e) {
			tinyUrl = "?";
		}
		return String.format("%s:%s $%d (%s)", projectName, result, build.number, tinyUrl);
	}
	
	public void sendReport(String message) throws Exception {
		LOGGER.info("Attempting to send bug report: " + message);
		
		JSONStringer js = new JSONStringer();
        if(this.defaultProps)	{
	    	js.object().key("dcterms:title").value("Hudson Build Failure").
	    	    key("dcterms:description").value(message).
	    	    key("oslc_cm:status").value("Open").
	    	    key("helios_bt:priority").value("3").
	    	    key("helios_bt:assigned_to").value("Nobody").endObject();
        }
        else	{
        	Iterator iter = bugprops.iterator();
        	String temp;
        	String[] prop;
        	js.object();
    		while(iter.hasNext())	{
    			temp = (String)iter.next();
    			prop = temp.split("::");
    			js.key(prop[0]).value(prop[1]);
    		}
    		js.endObject();
        }
        
        String jsonbug = js.toString();
        LOGGER.info("Report: " + jsonbug);
    	
        HttpPost request = new HttpPost((getUrl()));
        request.setHeader("Accept", "application/json");
        StringEntity body = new StringEntity(jsonbug);
        request.setEntity(body);        

        consumer.sign(request);
        LOGGER.info(request.getFirstHeader("Authorization").getValue());
        LOGGER.info(consumer.sign(getUrl()));

        LOGGER.info("Sending bug report to Fusionforge...");
        
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(request);

        /*System.out.println("Response: " + response.getStatusLine().getStatusCode() + " "
                + response.getStatusLine().getReasonPhrase());*/
        LOGGER.info("Response: " + response.getStatusLine().getStatusCode() + " "
                + response.getStatusLine().getReasonPhrase());
        
        //System.out.println("Response: " + response.getEntity().getContent().toString());
        //response.getEntity().writeTo(LOGGER);
        LOGGER.info(EntityUtils.toString(response.getEntity()));

		
	}

	/**
	 * Determine if this build failure is the first failure in a series of
	 * build failures
	 *
	 * @param build the Build object
	 * @return true if this build is the first
	 */
	protected boolean isFirstFailure(AbstractBuild<?, ?> build) {
		if (build.getResult() == Result.FAILURE || build.getResult() == Result.UNSTABLE) {
			AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();
			if (previousBuild != null && previousBuild.getResult() == Result.SUCCESS) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Determine if a bug report should be created and sent
	 *
	 * @param build the Build object
	 * @return true if we should report this build failure
	 */
	protected boolean shouldSendBugReport(AbstractBuild<?, ?> build) {
		if(this.getAutomatic())	{
			LOGGER.info("inside getautomatic");
			if (this.getEachBuildFailure())	{
				LOGGER.info("inside getEachBuildFailure");
				return true;
			}else if(this.getFirstBuildFailure())	{
				LOGGER.info("inside getFirstBuildFailure");
				if(this.isFirstFailure(build))	{
					LOGGER.info("inside isFirstFailure");
					return true;
				}else	{
					return false;
				}
			}else	{
				return false;
			}
		}else	{
			return false;
		}
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		
		public String hudsonUrl;
		public String consumerKey;
		public String consumerSecret;
		
		public DescriptorImpl() {
			super(CMConsumer.class);
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			hudsonUrl = Mailer.descriptor().getUrl();
			req.bindParameters(this);
			save();
			return super.configure(req, formData);
		}

		@Override
		public String getDisplayName() {
			return "OSLC Consumer";
		}
		
		public String getConsumerKey()	{
			return consumerKey;
		}
		
		public String getConsumerSecret()	{
			return consumerSecret;
		}

		public String getUrl() {
			return hudsonUrl;
		}
		
		public String VerifyJsonProperties(String property)	{
			property = property.trim();
			if(property.length()==0)	{
				return "Nothing specified";
			}
			else if(!property.matches("[^\"\']+"))	{
				return "Quotes are not allowed";
			}
			else if((property.indexOf("::")<0)){
				return "The '::' operator must be present";
			}
			else if((property.indexOf("::")==0)){
				return "The property cant be empty";
			}
			else if(property.indexOf("::")!=property.lastIndexOf("::"))	{
				return "The '::' operator must be present only once";
			}
			else if((property.indexOf("::")>(property.length()-3))){
				return "The value cant be empty";
			}
			else	{				
				return null;				
			}
		}
		
		public FormValidation doCheckProp(@QueryParameter String prop) {
			String result = VerifyJsonProperties(prop);
			if(result==null)	{
				return FormValidation.ok();
			}else	{
				return FormValidation.error(result);
			}
		}
		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public CMConsumer newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			if (hudsonUrl == null) {
				// if Hudson URL is not configured yet, infer some default
				hudsonUrl = Functions.inferHudsonURL(req);
				save();
			}
			JSONObject auto = formData.getJSONObject("automatic");
			LOGGER.info(auto.toString());
			List<String> newProps = null;
			
			if((req.getParameter("automatic")!=null)&&(auto.has("bugprops")))	{
				Object properties = auto.get("bugprops");
				newProps = new ArrayList<String>();
				//LOGGER.info(properties.toString());
				if(properties instanceof JSONObject)	{
					String property = (String)((JSONObject) properties).get("prop");
					String result = this.VerifyJsonProperties(property);
					if(result==null)	{
						newProps.add(property);
					}
				}else if(properties instanceof JSONArray)	{
					Iterator<JSONObject> i = ((JSONArray) properties).iterator();					
					while(i.hasNext())	{
						String property = i.next().getString("prop");
						String result = this.VerifyJsonProperties(property);
						if(result==null)	{
							newProps.add(property);
							LOGGER.info(property);
						}
					}
				}
			}
			if((newProps!=null)&&(newProps.isEmpty()))	{
				newProps = null;
			}
			
			//return super.newInstance(req, formData);
			LOGGER.info("new Instance");
			return new CMConsumer(	
					req.getParameter("token"),
					req.getParameter("tokenSecret"),
					req.getParameter("manual")!=null,
					req.getParameter("automatic")!=null,
					req.getParameter("url"),
					req.getParameter("delegUrl"),
					req.getParameter("width"),
					req.getParameter("height"),
					req.getParameter("eachBuildFailure")!=null,
					req.getParameter("firstBuildFailure")!=null,
					newProps);
		}	
		
	}
	
	
}