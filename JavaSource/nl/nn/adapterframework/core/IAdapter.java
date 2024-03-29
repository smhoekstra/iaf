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
package nl.nn.adapterframework.core;

import java.util.Iterator;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.MessageKeeper;

/**
 * The Adapter is the central manager in the framework. It has knowledge of both
 * <code>IReceiver</code>s as well as the <code>PipeLine</code> and statistics.
 * The Adapter is the class that is responsible for configuring, initializing and
 * accessing/activating IReceivers, Pipelines, statistics etc.
 * 
 * @version $Id$
 **/
public interface IAdapter extends IManagable {

    /**
  	 * Instruct the adapter to configure itself. The adapter will call the
  	 * pipeline to configure itself, the pipeline will call the individual
  	 * pipes to configure themselves.
  	 * @see nl.nn.adapterframework.pipes.AbstractPipe#configure()
  	 * @see PipeLine#configure()
  	 */
  	public void configure() throws ConfigurationException;
  	
 	/**
 	 * The messagekeeper is used to keep the last x messages, relevant to
 	 * display in the web-functions.
 	 */ 
	public MessageKeeper getMessageKeeper();
	public IReceiver getReceiverByName(String receiverName);
	public Iterator getReceiverIterator();
	public PipeLineResult processMessage(String messageId, String message, IPipeLineSession pipeLineSession);
	public PipeLineResult processMessageWithExceptions(String messageId, String message, IPipeLineSession pipeLineSession) throws ListenerException;

  	public void registerPipeLine (PipeLine pipeline) throws ConfigurationException;
  	public void setName(String name);
  	public boolean isAutoStart();
	public String toString();

	public String formatErrorMessage(String errorMessage, Throwable t, String originalMessage, String messageID, INamedObject objectInError, long receivedTime);
		
	public void forEachStatisticsKeeperBody(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException ;

    /**
     * state to put in PipeLineResult when a PipeRunException occurs.
     */
	public String getErrorState();
}
