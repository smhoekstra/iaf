/*
 * $Log: Configuration.java,v $
 * Revision 1.28.2.1  2007-11-15 12:18:37  europe\M00035F
 * Formatting fix
 *
 * Revision 1.28  2007/10/16 08:40:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed ifsa facade version display
 *
 * Revision 1.27  2007/10/09 15:07:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * copy changes from Ibis-EJB:
 * added formerly static classe appConstants to config 
 * delegate work to IbisManager
 *
 * Revision 1.26  2007/10/08 13:29:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.25  2007/07/24 08:04:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reversed shutdown sequence
 *
 * Revision 1.24  2007/07/17 15:07:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added list of adapters, to access them in order
 *
 * Revision 1.23  2007/06/26 09:35:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add instance name to log at startup
 *
 * Revision 1.22  2007/05/02 11:22:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute 'active'
 *
 * Revision 1.21  2007/02/26 16:55:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * start scheduler when a job is found in the configuration
 *
 * Revision 1.20  2007/02/21 15:57:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * throw exception if scheduled job not OK
 *
 * Revision 1.19  2007/02/12 13:38:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.18  2005/12/28 08:59:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * replaced application-name by instance-name
 *
 * Revision 1.17  2005/12/28 08:35:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced StatisticsKeeper-iteration
 *
 * Revision 1.16  2005/11/01 08:53:35  John Dekker <john.dekker@ibissource.org>
 * Moved quartz scheduling knowledge to the SchedulerHelper class
 *
 * Revision 1.15  2005/05/31 09:11:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * detailed version info for XML parsers and transformers
 *
 * Revision 1.14  2004/08/23 07:41:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed Pushers to Listeners
 *
 * Revision 1.13  2004/08/09 08:43:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed pushing receiverbase
 *
 * Revision 1.12  2004/07/06 07:06:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added PushingReceiver and Sap-extensions
 *
 * Revision 1.11  2004/06/30 10:01:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified error handling
 *
 * Revision 1.10  2004/06/16 12:34:46  Johan Verrips <johan.verrips@ibissource.org>
 * Added AutoStart functionality on Adapter
 *
 * Revision 1.9  2004/04/23 14:45:36  Johan Verrips <johan.verrips@ibissource.org>
 * added JMX support
 *
 * Revision 1.8  2004/03/30 07:30:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.7  2004/03/26 09:56:43  Johan Verrips <johan.verrips@ibissource.org>
 * Updated javadoc
 *
 */
package nl.nn.adapterframework.configuration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StatisticsKeeperIterationHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

/**
 * The Configuration is placeholder of all configuration objects. Besides that, it provides
 * functions for starting and stopping adapters as a facade.
 * 
 * @version Id
 * @author Johan Verrips
 * @see    nl.nn.adapterframework.configuration.ConfigurationException
 * @see    nl.nn.adapterframework.core.IAdapter
 */
public class Configuration {
	public static final String version="$RCSfile: Configuration.java,v $ $Revision: 1.28.2.1 $ $Date: 2007-11-15 12:18:37 $";
    protected Logger log=LogUtil.getLogger(this); 
     
    private Map adapterTable = new Hashtable();
    private List adapters = new ArrayList();
    private List scheduledJobs = new ArrayList();
    
    private URL configurationURL;
    private URL digesterRulesURL;
    private String configurationName = "";
    private boolean enableJMX=false;
    
    private AppConstants appConstants;
    
    /**
     *Set JMX extensions as enabled or not. Default is that JMX extensions are NOT enabled.
     * @param enable
     * @since 4.1.1
     */
    public void setEnableJMX(boolean enable){
    	enableJMX=enable;
    }

	/**
	 * Are JMX extensions enabled?
     * @since 4.1.1
	 * @return boolean
	 */    
    public boolean isEnableJMX(){
    	return enableJMX;
    }

	public void forEachStatisticsKeeper(StatisticsKeeperIterationHandler hski) {
		Object root=hski.start();
		Object groupData=hski.openGroup(root,appConstants.getString("instance.name",""),"instance");
		for (int i=0; i<adapters.size(); i++) {
			IAdapter adapter = getRegisteredAdapter(i);
			adapter.forEachStatisticsKeeperBody(hski,groupData);
		}
		hski.closeGroup(groupData);
		hski.end(root);
	}

    /**
     *	initializes the log and the AppConstants
     * @see nl.nn.adapterframework.util.AppConstants
     */
    public Configuration() {
    }
    public Configuration(URL digesterRulesURL, URL configurationURL) {
        this();
        this.configurationURL = configurationURL;
        this.digesterRulesURL = digesterRulesURL;

    }
    protected void init() {
        log.info(VersionInfo());
    }
    public String getConfigurationName() {
        return configurationName;
    }
    public URL getConfigurationURL() {
        return configurationURL;
    }
    public String getDigesterRulesFileName() {
        return digesterRulesURL.getFile();

    }
    /**
     * get a registered adapter by its name
     * @param name  the adapter to retrieve
     * @return IAdapter
     */
    public IAdapter getRegisteredAdapter(String name) {
        return (IAdapter) adapterTable.get(name);
    }
	public IAdapter getRegisteredAdapter(int index) {
		return (IAdapter) adapters.get(index);
	}

	public List getRegisteredAdapters() {
		return adapters;
	}

    //Returns a sorted list of registered adapter names as an <code>Iterator</code>
    public Iterator getRegisteredAdapterNames() {
        SortedSet sortedKeys = new TreeSet(adapterTable.keySet());
        return sortedKeys.iterator();
    }
//    /**
//     * Utility function
//     */
//
//    public void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy) {
//        if (action.equalsIgnoreCase("STOPADAPTER")) {
//        	if (adapterName.equals("**ALL**")) {
//	            log.info("Stopping all adapters on request of [" + commandIssuedBy+"]");
//	            stopAdapters();
//        	}
//        	else {
//				log.info("Stopping adapter [" + adapterName + "], on request of [" + commandIssuedBy+"]");
//				getRegisteredAdapter(adapterName).stopRunning();
//        	}
//        }
//        else if (action.equalsIgnoreCase("STARTADAPTER")) {
//        	if (adapterName.equals("**ALL**")) {
//				log.info("Starting all adapters on request of [" + commandIssuedBy+"]");
//        		startAdapters();
//        	}
//        	else {
//				try {
//					log.info("Starting adapter [" + adapterName + "] on request of [" + commandIssuedBy+"]");
//					getRegisteredAdapter(adapterName).startRunning();
//				} catch (Exception e) {
//					log.error("error in execution of command [" + action + "] for adapter [" + adapterName + "]",	e);
//					//errors.add("", new ActionError("errors.generic", e.toString()));
//				}
//        	}
//        }
//        else if (action.equalsIgnoreCase("STOPRECEIVER")) {
//            IAdapter adapter = (IAdapter) this.getRegisteredAdapter(adapterName);
//            IReceiver receiver = adapter.getReceiverByName(receiverName);
//            receiver.stopRunning();
//            log.info("receiver [" + receiverName + "] stopped by webcontrol on request of " + commandIssuedBy);
//        }
//        else if (action.equalsIgnoreCase("STARTRECEIVER")) {
//            IAdapter adapter = (IAdapter) this.getRegisteredAdapter(adapterName);
//            IReceiver receiver = adapter.getReceiverByName(receiverName);
//            receiver.startRunning();
//            log.info("receiver [" + receiverName + "] started by " + commandIssuedBy);
//        }
//		else if (action.equalsIgnoreCase("SENDMESSAGE")) {
//			try {
//				// send job
//				IbisLocalSender localSender = new IbisLocalSender();
//				localSender.setJavaListener(receiverName);
//				localSender.setIsolated(false);
//				localSender.setName("AdapterJob");
//				localSender.configure();
//			
//				localSender.open();
//				try {
//					localSender.sendMessage(null, "");
//				}
//				finally {
//					localSender.close();
//				}
//			}
//			catch(Exception e) {
//				log.error("Error while sending message (as part of scheduled job execution)", e);
//			}
////			ServiceDispatcher.getInstance().dispatchRequest(receiverName, "");
//		}
//    }
    /**
     * returns wether an adapter is known at the configuration.
     * @param name the Adaptername
     * @return true if the adapter is known at the configuration
     */
    public boolean isRegisteredAdapter(String name){
        return getRegisteredAdapter(name)==null;
    }
    /**
     * @param adapterName the adapter
     * @param receiverName the receiver
     * @return true if the receiver is known at the adapter
     */
    public boolean isRegisteredReceiver(String adapterName, String receiverName){
        IAdapter adapter=getRegisteredAdapter(adapterName);
        if (null==adapter) {
        	return false;
		}
        return adapter.getReceiverByName(receiverName) != null;
    }
    public void listObjects() {
		for (int i=0; i<adapters.size(); i++) {
			IAdapter adapter = getRegisteredAdapter(i);

			log.info(i+") "+ adapter.getName()+ ": "	+ adapter.toString());
        }
    }
    
    /**
     * Register an adapter with the configuration.  If JMX is {@link #setEnableJMX(boolean) enabled},
     * the adapter will be visible and managable as an MBEAN. 
     * @param adapter
     * @throws ConfigurationException
     */
    public void registerAdapter(IAdapter adapter) throws ConfigurationException {
    	if (adapter instanceof Adapter && !((Adapter)adapter).isActive()) {
    		log.debug("adapter [" + adapter.getName() + "] is not active, therefore not included in configuration");
    		return;
    	} 
        if (null != adapterTable.get(adapter.getName())) {
            throw new ConfigurationException("Adapter [" + adapter.getName() + "] already registered.");
        }
        adapterTable.put(adapter.getName(), adapter);
		adapters.add(adapter);
		if (isEnableJMX()) {
			log.debug("Registering adapter [" + adapter.getName() + "] to the JMX server");
	        JmxMbeanHelper.hookupAdapter( (nl.nn.adapterframework.core.Adapter) adapter);
	        log.info ("[" + adapter.getName() + "] registered to the JMX server");
		}
        adapter.configure();

    }
    /**
     * Register an {@link AdapterJob job} for scheduling at the configuration.
     * The configuration will create an {@link AdapterJob AdapterJob} instance and a JobDetail with the
     * information from the parameters, after checking the
     * parameters of the job. (basically, it checks wether the adapter and the
     * receiver are registered.
     * <p>See the <a href="http://quartz.sourceforge.net">Quartz scheduler</a> documentation</p>
     * @param jobdef a JobDef object
     * @see nl.nn.adapterframework.scheduler.JobDef for a description of Cron triggers
     * @since 4.0
     */
    public void registerScheduledJob(JobDef jobdef) throws ConfigurationException {
        if (this.getRegisteredAdapter(jobdef.getAdapterName()) == null) {
        	String msg="Jobdef [" + jobdef.getName() + "] got error: adapter [" + jobdef.getAdapterName() + "] not registered.";
            log.error(msg);
            throw new ConfigurationException(msg);
        }
        if (StringUtils.isNotEmpty(jobdef.getReceiverName())){
            if (! isRegisteredReceiver(jobdef.getAdapterName(), jobdef.getReceiverName())) {
				String msg="Jobdef [" + jobdef.getName() + "] got error: adapter [" + jobdef.getAdapterName() + "] receiver ["+jobdef.getReceiverName()+"] not registered.";
                log.error(msg);
				throw new ConfigurationException(msg);
            }
        }
        scheduledJobs.add(jobdef);
    }
    
    public void setConfigurationName(String name) {
        configurationName = name;
        log.debug("configuration name set to [" + name + "]");
    }
    
//    public void startAdapters() {
//		for (int i=0; i<adapters.size(); i++) {
//			IAdapter adapter = getRegisteredAdapter(i);
//
//			if (adapter.isAutoStart()) {
//				log.info("Starting adapter [" + adapter.getName()+"]");
//				adapter.startRunning();
//			}
//		}
//    }
//    
//    public void stopAdapters() {
//		for (int i=adapters.size()-1; i>=0; i--) {
//			IAdapter adapter = getRegisteredAdapter(i);
//
//			log.info("Stopping adapter [" + adapter.getName() + "]");
//			adapter.stopRunning();
//		}
//        forEachStatisticsKeeper(new StatisticsKeeperLogger());
//    }
    
    public String getInstanceInfo() {
		String instanceInfo=appConstants.getProperty("application.name")+" "+
							appConstants.getProperty("application.version")+" "+
							appConstants.getProperty("instance.name")+" "+
							appConstants.getProperty("instance.version")+" ";
		String buildId=	appConstants.getProperty("instance.build_id");
		if (StringUtils.isNotEmpty(buildId)) {
			instanceInfo+=" build "+buildId;						
		}
		return instanceInfo;
    }
    
    public String VersionInfo() {
    	StringBuffer sb=new StringBuffer();
    	sb.append(getInstanceInfo()+SystemUtils.LINE_SEPARATOR);
    	sb.append(version+SystemUtils.LINE_SEPARATOR);
    	sb.append(ConfigurationDigester.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.IReceiver.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.IPullingListener.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.Adapter.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.IPipe.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.PipeLine.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.receivers.ServiceDispatcher.version+SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.receivers.ReceiverBase.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.util.AppConstants.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.util.Variant.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.pipes.AbstractPipe.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.pipes.MessageSendingPipe.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.pipes.XmlValidator.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.pipes.XmlSwitch.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter.version+SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.http.HttpSender.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.http.WebServiceSender.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.http.IbisWebServiceSender.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.http.WebServiceListener.version +SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.ConfigurationServlet.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.IniDynaActionForm.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.action.ActionBase.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.action.ShowConfiguration.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.action.ShowConfigurationStatus.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.scheduler.SchedulerAdapter.version +SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.extensions.coolgen.CoolGenWrapperPipe.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.ifsa.IfsaRequesterSender.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.ifsa.IfsaProviderListener.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.rekenbox.RekenBoxCaller.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.rekenbox.Adios2XmlPipe.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.sap.SapFunctionFacade.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.sap.SapListener.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.sap.SapSender.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.sap.SapFunctionHandler.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.sap.SapSystem.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.util.XmlUtils.getVersionInfo());
    	return sb.toString();
    	
    }
	/**
	 * @param url
	 */
	public void setConfigurationURL(URL url) {
		configurationURL = url;
	}

	/**
	 * @param url
	 */
	public void setDigesterRulesURL(URL url) {
		digesterRulesURL = url;
	}

	/**
	 * @return
	 */
	public List getScheduledJobs() {
		return scheduledJobs;
	}

    /**
     * @return
     */
    public AppConstants getAppConstants() {
        return appConstants;
    }

    /**
     * @param constants
     */
    public void setAppConstants(AppConstants constants) {
        appConstants = constants;
    }

}
