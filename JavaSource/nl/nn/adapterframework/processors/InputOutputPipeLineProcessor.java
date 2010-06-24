/*
 * $Log: InputOutputPipeLineProcessor.java,v $
 * Revision 1.1.2.1  2010-06-24 15:27:10  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.log4j.Logger;

/**
 * @version Id
 */
public class InputOutputPipeLineProcessor implements PipeLineProcessor {
	private Logger log = LogUtil.getLogger(this);
	private PipeLineProcessor pipeLineProcessor;

	public void setPipeLineProcessor(PipeLineProcessor pipeLineProcessor) {
		this.pipeLineProcessor = pipeLineProcessor;
	}
	
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId,
			String message, PipeLineSession pipeLineSession
			) throws PipeRunException {
		if (pipeLineSession==null) {
			pipeLineSession= new PipeLineSession();
		}
		// reset the PipeLineSession and store the message and its id in the session
		if (messageId==null) {
				messageId=Misc.createSimpleUUID();
				log.error("null value for messageId, setting to ["+messageId+"]");
	
		}
		if (message == null) {
			throw new PipeRunException(null, "Pipeline of adapter ["+ pipeLine.getOwner().getName()+"] received null message");
		}
		// store message and messageId in the pipeLineSession
		pipeLineSession.set(message, messageId);
		return pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession);
	}

}
