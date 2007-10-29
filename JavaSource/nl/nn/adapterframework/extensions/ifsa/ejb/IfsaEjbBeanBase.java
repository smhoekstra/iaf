/*
 * $Log: IfsaEjbBeanBase.java,v $
 * Revision 1.1.2.1  2007-10-29 12:25:35  europe\M00035F
 * Create EJb Beans required to connect to IFSA J2EE implementation as an IFSA Provider application
 *
 * 
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.api.ServiceRequest;
import com.ing.ifsa.exceptions.ServiceException;
import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.ejb.AbstractListenerConnectingEJB;
import nl.nn.adapterframework.receivers.GenericReceiver;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
abstract public class IfsaEjbBeanBase extends AbstractListenerConnectingEJB implements SessionBean {
    protected SessionContext ejbContext;
    protected IfsaProviderListener listener;
    
    public void ejbCreate() throws CreateException {
        // TODO: More can be pulled up when there's a proper interface
        // implemented by both PushingJmsListener and IfsaProviderListener
        this.listener = retrieveJmsListener();
        this.containerManagedTransactions = retrieveTransactionType();
        
    }
    
    public void ejbRemove() throws EJBException, RemoteException {
        // TODO: ListenerPort unregistration
    }

    protected IfsaProviderListener retrieveJmsListener() {
        String adapterName = (String) getContextVariable("adapterName");
        String receiverName = (String) getContextVariable("receiverName");
        return (IfsaProviderListener) retrieveListener(receiverName, adapterName);
    }

    protected String processRequest(ServiceRequest request) throws ServiceException {
        try {
            GenericReceiver receiver = (GenericReceiver) listener.getHandler();
            String message = listener.getStringFromRawMessage(request, null);
            String id = listener.getIdFromRawMessage(request, null);
            String cid = id;
            String replyText = receiver.processRequest(listener, cid, message);
            return replyText;
        } catch (ListenerException ex) {
            listener.getExceptionListener().exceptionThrown(listener, ex);
            throw new ServiceException(ex);
        }
    }

    public void setSessionContext(SessionContext context) throws EJBException, RemoteException {
        this.ejbContext = context;
    }

    protected EJBContext getEJBContext() {
        return this.ejbContext;
    }

    public void ejbActivate() throws EJBException, RemoteException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void ejbPassivate() throws EJBException, RemoteException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
