/*
 * $Log: RequestReplyExecutor.java,v $
 * Revision 1.1.2.1  2010-06-24 15:27:12  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 *
 */
package nl.nn.adapterframework.core;

/**
 * Runnable object for calling a request reply service. When a
 * <code>Throwable</code> has been thrown during execution is should be returned
 * by getThrowable() otherwise the reply should be returned by getReply().
 *    
 * @author Jaco de Groot
 * @version Id
 */
public abstract class RequestReplyExecutor implements Runnable {
	protected String correlationID;
	protected Object request;
	protected Object reply;
	protected Throwable throwable;

	public void setCorrelationID(String correlationID) {
		this.correlationID = correlationID;
	}
	
	public String getCorrelationID() {
		return correlationID;
	}

	public void setRequest(Object request)  {
		this.request = request;
	}
		
	public Object getRequest() {
		return request;
	}

	public void setReply(Object reply)  {
		this.reply = reply;
	}
		
	public Object getReply() {
		return reply;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public Throwable getThrowable() {
		return throwable;
	}

}
