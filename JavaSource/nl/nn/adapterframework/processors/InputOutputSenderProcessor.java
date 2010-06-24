/*
 * $Log: InputOutputSenderProcessor.java,v $
 * Revision 1.1.2.1  2010-06-24 15:27:11  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.SenderWrapperBase;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @version Id
 */
public class InputOutputSenderProcessor implements SenderProcessor {
	private Logger log = LogUtil.getLogger(this);
	private SenderProcessor senderProcessor;

	public void setSenderProcessor(SenderProcessor senderProcessor) {
		this.senderProcessor = senderProcessor;
	}

	public String sendMessage(ISender sender, String correlationID,
			Object message, PipeLineSession pipeLineSession,
			boolean namespaceAware) throws SenderException, TimeOutException {
		if (message!=null && !(message instanceof String)) {
			throw new SenderException("String expected, got a [" + message.getClass().getName() + "]");
		}
		if (sender instanceof SenderWrapperBase) {
			SenderWrapperBase senderWrapperBase = (SenderWrapperBase)sender;
			String senderInput=(String)message;
			if (StringUtils.isNotEmpty(senderWrapperBase.getGetInputFromSessionKey())) {
				senderInput=(String)pipeLineSession.get(senderWrapperBase.getGetInputFromSessionKey());
				if (log.isDebugEnabled()) log.debug(senderWrapperBase.getLogPrefix()+"set contents of session variable ["+senderWrapperBase.getGetInputFromSessionKey()+"] as input ["+senderInput+"]");
			} else {
				if (StringUtils.isNotEmpty(senderWrapperBase.getGetInputFromFixedValue())) {
					senderInput=senderWrapperBase.getGetInputFromFixedValue();
					if (log.isDebugEnabled()) log.debug(senderWrapperBase.getLogPrefix()+"set input to fixed value ["+senderInput+"]");
				}
			}
			String result = senderProcessor.sendMessage(senderWrapperBase, correlationID, senderInput, pipeLineSession, namespaceAware);
			if (StringUtils.isNotEmpty(senderWrapperBase.getStoreResultInSessionKey())) {
				if (log.isDebugEnabled()) log.debug(senderWrapperBase.getLogPrefix()+"storing results in session variable ["+senderWrapperBase.getStoreResultInSessionKey()+"]");
				pipeLineSession.put(senderWrapperBase.getStoreResultInSessionKey(),result);
			}
			return senderWrapperBase.isPreserveInput()?(String)message:result;
		} else {
			return senderProcessor.sendMessage(sender, correlationID, message, pipeLineSession, namespaceAware);
		}
	}


}
