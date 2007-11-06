/*
 * $Log: GenericMDB.java,v $
 * Revision 1.4.2.6  2007-11-06 12:41:16  europe\M00035F
 * Add original raw message as parameter to method 'createThreadContext' of 'pushingJmsListener' in preparation of adding it to interface
 *
 * Revision 1.4.2.5  2007/11/06 09:39:13  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Merge refactoring/renaming from HEAD
 *
 * Revision 1.4.2.4  2007/10/29 10:37:25  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix method visibility error
 *
 * Revision 1.4.2.3  2007/10/29 10:29:13  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Refactor: pullup a number of methods to abstract base class so they can be shared with new IFSA Session EJBs
 *
 * Revision 1.4.2.2  2007/10/25 08:36:57  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add shutdown method for IBIS which shuts down the scheduler too, and which unregisters all EjbJmsConfigurators from the ListenerPortPoller.
 * Unregister JmsListener from ListenerPortPoller during ejbRemove method.
 * Both changes are to facilitate more proper shutdown of the IBIS adapters.
 *
 * Revision 1.4.2.1  2007/10/24 15:04:43  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Let runstate of receivers/listeners follow the state of WebSphere ListenerPorts if they are changed outside the control of IBIS.
 *
 * Revision 1.4  2007/10/16 09:52:35  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Change over JmsListener to a 'switch-class' to facilitate smoother switchover from older version to spring version
 *
 * Revision 1.3  2007/10/15 13:08:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * EJB updates
 *
 * Revision 1.1.2.6  2007/10/15 11:35:51  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix direct retrieving of Logger w/o using the LogUtil
 *
 * Revision 1.1.2.5  2007/10/12 11:53:42  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add variable to indicate to MDB if it's transactions are container-managed, or bean-managed
 *
 * Revision 1.1.2.4  2007/10/10 14:30:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.2  2007/10/10 09:48:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.ejb;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.PushingJmsListener;
import nl.nn.adapterframework.receivers.GenericReceiver;

/**
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class GenericMDB extends AbstractListenerConnectingEJB implements MessageDrivenBean, MessageListener {
    
    protected MessageDrivenContext ejbContext;
    protected PushingJmsListener listener;
    public void setMessageDrivenContext(MessageDrivenContext ejbContext) throws EJBException {
        log.info("Received EJB-MDB Context");
        this.ejbContext = ejbContext;
    }
    
    public void ejbCreate() {
        log.info("Creating MDB");
        this.listener = retrieveJmsListener();
        this.containerManagedTransactions = retrieveTransactionType();
    }
    
    public void ejbRemove() throws EJBException {
        log.info("Removing MDB");
        listenerPortPoller.unregisterEjbListenerPortConnector(
                (EjbListenerPortConnector)listener.getJmsConnector());
    }

    public void onMessage(Message message) {
        Map threadContext = new HashMap();
        try {
            // Code is not thread-safe but the same instance
            // should be looked up always so there's no point
            // in locking
            if (this.listener == null) {
                this.listener = retrieveJmsListener();
            }

            GenericReceiver receiver = (GenericReceiver) this.listener.getReceiver();
            this.listener.populateThreadContext(message,threadContext, null);
            receiver.processRawMessage(listener, message, threadContext);
        } catch (ListenerException ex) {
            log.error(ex, ex);
            rollbackTransaction();
        } finally {
            this.listener.destroyThreadContext(threadContext);
        }
    }

    protected PushingJmsListener retrieveJmsListener() {
        String adapterName = (String) getContextVariable("adapterName");
        String receiverName = (String) getContextVariable("receiverName");
        return (PushingJmsListener) retrieveListener(receiverName, adapterName);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.ejb.AbstractEJBBase#getEJBContext()
     */
    protected EJBContext getEJBContext() {
        return this.ejbContext;
    }
    
    
}
