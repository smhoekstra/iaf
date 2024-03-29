/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.extensions.fxf;

import java.io.File;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jms.JMSFacade;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.ProcessUtil;

import org.apache.commons.lang.StringUtils;

/**
 * Sender for transferring files using the FxF protocol version 1 and 2. For FxF
 * protocol version 3 see the {@link FxfWrapperPipe FxfWrapperPipe}. Assumes
 * sender input is local filename.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.fxf.FxfListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setScript(String) script}</td><td>full pathname to the FXF script to be executed to transfer the file</td><td>/usr/local/bin/FXF_init</td></tr>
 * <tr><td>{@link #setTransfername(String) transfername}</td><td>FXF transfername</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>(FXF 2 only) name of the JMS queue used to send processedPutFile messages</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFxf2Compatibility(boolean) fxf2Compatibility}</td><td>when set <code>true</code>, attributes only required for FXF 2.0 will be mandatory for FXF 1.3 too</td><td>true</td></tr>
 * <tr><td>{@link #setProcessedDirectory(String) processedDirectory}</td><td>Directory where files are stored after being processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNumberOfBackups(int) numberOfBackups}</td><td>number of copies held of a file with the same name. Backup files have a dot and a number suffixed to their name. If set to 0, no backups will be kept.</td><td>5</td></tr>
 * <tr><td>{@link #setOverwrite(boolean) overwrite}</td><td>when set <code>true</code>, the destination file will be deleted if it already exists</td><td>false</td></tr>
 * <tr><td>{@link #setDelete(boolean) delete}</td><td>when set <code>true</code>, the file processed will deleted after being processed, and not stored</td><td>true</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>(since 2.0) the timeout in seconds, which is the maximum time the application allows for the transfer of the file.</td><td>120</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>(since 2.0)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setForceMQCompliancy(String) forceMQCompliancy}</td><td>If the MQ destination is not a JMS receiver, format errors occur.
	 To prevent this, settting <code>forceMQCompliancy</code> to MQ will inform
	 MQ that the replyto queue is not JMS compliant. Setting <code>forceMQCompliancy</code>
	 to "JMS" will cause that on mq the destination is identified as jms-compliant.</td><td>MQ</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>remoteFilename</td><td><i>String</i></td><td>remote filename, used as 4th parameter of fxf command. When not specified, the remote filename is determined by FXF from its configuration</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version $Id$
 */
public class FxfSender extends JMSFacade implements ISenderWithParameters {

	public static final String REMOTE_FILENAME_PARAM="remoteFilename";

	private String name;
	private String script="/usr/local/bin/FXF_init";
	private String transfername;
	private String processedDirectory;
	private int numberOfBackups = 0;
	private boolean overwrite = false;
	private boolean delete = true;
	private int timeout=120;
	
	private boolean atLeastVersion2=false;
	private boolean fxf2Compatibility=true;
	
	private Parameter remoteFilenameParam=null;
	
	protected ParameterList paramList = null;

	public FxfSender() {
		super();
		setForceMQCompliancy("MQ");
	}

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getScript())) {
			throw new ConfigurationException("FxfSender ["+getName()+"] attribute script must be specified");
		}
//		File f = new File(getScript());
//		if (!f.exists()) {
//			throw new ConfigurationException("FxfSender ["+getName()+"] script ["+getScript()+"] does not exist");
//		}
		if (StringUtils.isEmpty(getTransfername())) {
			throw new ConfigurationException("FxfSender ["+getName()+"] attribute transfername must be specified");
		}
		if (paramList!=null && paramList.size()>0) {
			remoteFilenameParam=(Parameter)paramList.get(0);
			if (!REMOTE_FILENAME_PARAM.equalsIgnoreCase(remoteFilenameParam.getName())) {
				ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
				String msg = getLogPrefix()+"name of parameter for remote filename ["+remoteFilenameParam.getName()+"] is not equal to ["+REMOTE_FILENAME_PARAM+"], as expected. Using it anyway";
				configWarnings.add(log, msg);
			}
		}
		if (paramList!=null) {
			paramList.configure();
		}
		atLeastVersion2=FxfUtil.isAtLeastVersion2(getScript());
		if (atLeastVersion2) {
			setDestinationType("QUEUE");
			super.configure();
		} else {
			if (isFxf2Compatibility() && StringUtils.isEmpty(getDestinationName())) {
				throw new ConfigurationException("please specify destinationName for Fxf 2 Compatibility, or set fxf2Compatibility to false");
			}
			if (isFxf2Compatibility() && StringUtils.isEmpty(getQueueConnectionFactoryName())) {
				throw new ConfigurationException("please specify queueConnectionFactoryName (or applicable jmsRealm) for Fxf 2 Compatibility, or set fxf2Compatibility to false");
			}
		}
 	} 

	public void open() throws SenderException {
		if (atLeastVersion2) {
			try {
				openFacade();
			}
			catch (Exception e) {
				throw new SenderException(e);
			}
		}
	}

	public void close() throws SenderException {
		if (atLeastVersion2) {
			try {
				closeFacade();
			}
			catch (Throwable e) {
				throw new SenderException(getLogPrefix() + "got error occured stopping sender", e);
			}
		}
	}

	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	public boolean isSynchronous() {
		return false;
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sendMessage(correlationID, message, null);
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		String action="put";
		String transfername=getTransfername();
		String filename=message;
		
		String command = getScript()+" "+action+" "+transfername +" "+filename;
		if (atLeastVersion2) {
			int timeout = getTimeout();
			command = command + " " +timeout;
		}
		String remoteFilename = null;
		if (remoteFilenameParam!=null && prc!=null) {
			try {
				remoteFilename=(String)prc.getValues(paramList).getParameterValue(0).getValue();
				command += " "+remoteFilename;
			} catch (ParameterException e) {
				throw new SenderException("Could not resolve remote filename", e);
			}
		}
		log.debug(getLogPrefix()+"sending local file ["+message+"] by executing command ["+command+"]");
		String transporthandle=ProcessUtil.executeCommand(command,getTimeout()*2);
		log.debug(getLogPrefix()+"output of command ["+transporthandle+"]");
		if (transporthandle!=null) {
			transporthandle=transporthandle.trim();
		}
		
		// delete file or move it to processed directory
		if (isDelete() || StringUtils.isNotEmpty(getProcessedDirectory())) {
			File f=new File(message);
			try {
				log.debug(getLogPrefix()+"moving or deleteing file ["+message+"]");
				FileUtils.moveFileAfterProcessing(f, getProcessedDirectory(), isDelete(), isOverwrite(), getNumberOfBackups()); 
			} catch (Exception e) {
				throw new SenderException("Could not move file ["+message+"]",e);
			}
		}
		if (atLeastVersion2) {
			String commitMsg=FxfUtil.makeProcessedPutFileMessage(transporthandle);

			Session s = null;
			MessageProducer mp = null;

			try {
				s = createSession();
				mp = getMessageProducer(s, getDestination());

				// create message
				Message msg = createTextMessage(s, correlationID, commitMsg);
				mp.setDeliveryMode(DeliveryMode.PERSISTENT);

				// send message	
				send(mp, msg);
				if (log.isDebugEnabled()) {
					log.debug(getLogPrefix()+ "sent message [" + message + "] to [" + getDestinationName() + "] " + 
							"msgID [" + msg.getJMSMessageID() + "]" );;
				} else {
					if (log.isInfoEnabled()) {
						log.info(getLogPrefix()+ "sent message to [" + getDestinationName() + "] " + 
								"msgID [" + msg.getJMSMessageID() + "]" );;
					}
				}
			} catch (Throwable e) {
				throw new SenderException(e);
			} finally {
				if (mp != null) { 
					try { 
						mp.close(); 
					} catch (JMSException e) { 
						log.warn(getLogPrefix()+ "got exception closing message producer",e); 
					}
				}
				closeSession(s);
			}
			
		}
		return transporthandle;
	}




	public void setScript(String string) {
		script = string;
	}
	public String getScript() {
		return script;
	}

	public void setTransfername(String string) {
		transfername = string;
	}
	public String getTransfername() {
		return transfername;
	}

	public void setProcessedDirectory(String processedDirectory) {
		this.processedDirectory = processedDirectory;
	}
	public String getProcessedDirectory() {
		return processedDirectory;
	}

	public void setNumberOfBackups(int i) {
		numberOfBackups = i;
	}
	public int getNumberOfBackups() {
		return numberOfBackups;
	}

	public void setOverwrite(boolean b) {
		overwrite = b;
	}
	public boolean isOverwrite() {
		return overwrite;
	}

	public void setDelete(boolean b) {
		delete = b;
	}
	public boolean isDelete() {
		return delete;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name=name;
	}


	public void setTimeout(int i) {
		timeout = i;
	}
	public int getTimeout() {
		return timeout;
	}

	public void setFxf2Compatibility(boolean b) {
		fxf2Compatibility = b;
	}
	public boolean isFxf2Compatibility() {
		return fxf2Compatibility;
	}

	public String getPhysicalDestinationName() {
		String result="transfername ["+getTransfername()+"]";
		if (atLeastVersion2) {
			result +=" "+ super.getPhysicalDestinationName();
		}
		return result;
	}

}
