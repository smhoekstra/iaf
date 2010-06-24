/*
 * $Log: CorePipeLineProcessor.java,v $
 * Revision 1.1.2.1  2010-06-24 15:27:10  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 *
 */
package nl.nn.adapterframework.processors;

import java.io.IOException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineExitHandler;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.log4j.Logger;

/**
 * @version Id
 */
public class CorePipeLineProcessor implements PipeLineProcessor {
	private Logger log = LogUtil.getLogger(this);
	private PipeProcessor pipeProcessor;

	public void setPipeProcessor(PipeProcessor pipeProcessor) {
		this.pipeProcessor = pipeProcessor;
	}
	
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId,
			String message, PipeLineSession pipeLineSession
			) throws PipeRunException {
		// Object is the object that is passed to and returned from Pipes
		Object object = (Object) message;
		PipeRunResult pipeRunResult;
		// the PipeLineResult 
		PipeLineResult pipeLineResult=new PipeLineResult();   
		
		// ready indicates wether the pipeline processing is complete
		boolean ready=false;

		// get the first pipe to run
		IPipe pipeToRun = pipeLine.getPipe(pipeLine.getFirstPipe());

		IPipe inputValidator = pipeLine.getInputValidator();
		if (inputValidator!=null) {
			PipeRunResult validationResult = inputValidator.doPipe(message,pipeLineSession);
			if (validationResult!=null && !validationResult.getPipeForward().getName().equals("success")) {
				PipeForward validationForward=validationResult.getPipeForward();
				if (validationForward.getPath()==null) {
					throw new PipeRunException(pipeToRun,"forward ["+validationForward.getName()+"] of inputValidator has emtpy forward path");
				}	
				log.warn("setting first pipe to ["+validationForward.getPath()+"] due to validation fault");
				pipeToRun = pipeLine.getPipe(validationForward.getPath());
				if (pipeToRun==null) {
					throw new PipeRunException(pipeToRun,"forward ["+validationForward.getName()+"], path ["+validationForward.getPath()+"] does not correspond to a pipe");
				}
			}
		}

		if (pipeLine.isStoreOriginalMessageWithoutNamespaces()) {
			if (XmlUtils.isWellFormed(message)) {
				String removeNamespaces_xslt = XmlUtils.makeRemoveNamespacesXslt(true,true);
				try{
					String xsltResult = null;
					Transformer transformer = XmlUtils.createTransformer(removeNamespaces_xslt);
					xsltResult = XmlUtils.transformXml(transformer, message);
					pipeLineSession.put("originalMessageWithoutNamespaces", xsltResult);
				} catch (IOException e) {
					throw new PipeRunException(pipeToRun,"cannot retrieve removeNamespaces", e);
				} catch (TransformerConfigurationException te) {
					throw new PipeRunException(pipeToRun,"got error creating transformer from removeNamespaces", te);
				} catch (TransformerException te) {
					throw new PipeRunException(pipeToRun,"got error transforming removeNamespaces", te);
				} catch (DomBuilderException te) {
					throw new PipeRunException(pipeToRun,"caught DomBuilderException", te);
				}
			} else {
				log.warn("original message is not well-formed");
				pipeLineSession.put("originalMessageWithoutNamespaces", message);
			}
		}
	
		boolean outputValidated=false;
		try {    
			while (!ready){
				
				pipeRunResult = pipeProcessor.processPipe(pipeLine, pipeToRun, messageId, object, pipeLineSession);
				object=pipeRunResult.getResult();
				
				PipeForward pipeForward=pipeRunResult.getPipeForward();
	
	                
				if (pipeForward==null){
					throw new PipeRunException(pipeToRun, "Pipeline of ["+pipeLine.getOwner().getName()+"] received result from pipe ["+pipeToRun.getName()+"] without a pipeForward");
				}
				// get the next pipe to run
				String nextPath=pipeForward.getPath();
				if ((null==nextPath) || (nextPath.length()==0)){
					throw new PipeRunException(pipeToRun, "Pipeline of ["+pipeLine.getOwner().getName()+"] got an path that equals null or has a zero-length value from pipe ["+pipeToRun.getName()+"]. Check the configuration, probably forwards are not defined for this pipe.");
				}
	
				PipeLineExit plExit=(PipeLineExit)pipeLine.getPipeLineExits().get(nextPath);
				if (null!=plExit){
					IPipe outputValidator = pipeLine.getOutputValidator();
					if (outputValidator !=null && !outputValidated) {
						outputValidated=true;
						log.debug("validating PipeLineResult");
						PipeRunResult validationResult = outputValidator.doPipe(object,pipeLineSession);
						if (validationResult!=null && !validationResult.getPipeForward().getName().equals("success")) {
							PipeForward validationForward=validationResult.getPipeForward();
							if (validationForward.getPath()==null) {
								throw new PipeRunException(pipeToRun,"forward ["+validationForward.getName()+"] of outputValidator has emtpy forward path");
							}	
							log.warn("setting next pipe to ["+validationForward.getPath()+"] due to validation fault");
							pipeToRun = pipeLine.getPipe(validationForward.getPath());
							if (pipeToRun==null) {
								throw new PipeRunException(pipeToRun,"forward ["+validationForward.getName()+"], path ["+validationForward.getPath()+"] does not correspond to a pipe");
							}
						} else {
							log.debug("validation succeeded");
							ready=true;
						}
					} else {
						ready=true;
					}
					if (ready) {
						String state=plExit.getState();
						pipeLineResult.setState(state);
						if (object!=null) {
							pipeLineResult.setResult(object.toString());
						} else { 
							pipeLineResult.setResult(null);
						}
						ready=true;
						if (log.isDebugEnabled()){  // for performance reasons
							log.debug("Pipeline of adapter ["+ pipeLine.getOwner().getName()+ "] finished processing messageId ["+messageId+"] result: ["+ object+ "] with exit-state ["+state+"]");
						}
					}
				} else {
					pipeToRun=pipeLine.getPipe(pipeForward.getPath());
					if (pipeToRun==null) {
						throw new PipeRunException(null, "Pipeline of adapter ["+ pipeLine.getOwner().getName()+"] got an erroneous definition. Pipe to execute ["+pipeForward.getPath()+ "] is not defined.");
					}
				}
			}
		} finally {
			for (int i=0; i<pipeLine.getExitHandlers().size(); i++) {
				IPipeLineExitHandler exitHandler = (IPipeLineExitHandler)pipeLine.getExitHandlers().get(i);
				try {
					if (log.isDebugEnabled()) log.debug("processing ExitHandler ["+exitHandler.getName()+"]");
					exitHandler.atEndOfPipeLine(messageId,pipeLineResult,pipeLineSession);
				} catch (Throwable t) {
					log.warn("Caught Exception processing ExitHandler ["+exitHandler.getName()+"]",t);
				}
			}
		}
		return pipeLineResult;
	}

}
