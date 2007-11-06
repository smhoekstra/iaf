/*
 * ListenerPortPoller.java
 *  
 * $Log: ListenerPortPoller.java,v $
 * Revision 1.1.2.3  2007-11-06 09:39:13  europe\M00035F
 * Merge refactoring/renaming from HEAD
 *
 * Revision 1.1.2.2  2007/10/25 08:36:57  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add shutdown method for IBIS which shuts down the scheduler too, and which unregisters all EjbListenerPortConnectors from the ListenerPortPoller.
 * Unregister JmsListener from ListenerPortPoller during ejbRemove method.
 * Both changes are to facilitate more proper shutdown of the IBIS adapters.
 *
 * Revision 1.1.2.1  2007/10/24 15:04:44  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Let runstate of receivers/listeners follow the state of WebSphere ListenerPorts if they are changed outside the control of IBIS.
 *
 *
 * Created on 24-okt-2007, 13:33:28
 * 
 */

package nl.nn.adapterframework.ejb;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.GenericReceiver;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;
import org.apache.log4j.Logger;

/**
 * The ListenerPortPoller checks for all registered EjbListenerPortConnector
 * instances if the associated listener-port is still open while the
 * listener is also open, and opens / closes the listeners / receivers
 * accordingly.
 * 
 * Instances to be polled are kept are weak references so that registration
 * here does not prevent objects from being garbage-collected. When a weak
 * reference goes <code>null</code> it is automatically unregistered.
 * 
 * @author Tim van der Leeuw
 * @version Id
 */
public class ListenerPortPoller {
    private Logger log = LogUtil.getLogger(this);
    
    private List jmsConfiguratorList = new ArrayList();
    
    /**
     * Add an EjbListenerPortConnector instance to be polled.
     * 
     * Only add instances if they are not already registered.
     */
    public void registerEjbListenerPortConnector(EjbListenerPortConnector ejc) {
        if (!isRegistered(ejc)) {
            jmsConfiguratorList.add(new WeakReference(ejc));
        }
    }
    
    /**
     * Remove an EjbListenerPortConnector instance from the list to be polled.
     */
    public void unregisterEjbListenerPortConnector(EjbListenerPortConnector ejc) {
        for (Iterator iter = jmsConfiguratorList.iterator(); iter.hasNext();) {
            WeakReference wr = (WeakReference)iter.next();
            Object referent = wr.get();
            if (referent == null || referent == ejc) {
                iter.remove();
            }
        }
    }
    
    public boolean isRegistered(EjbListenerPortConnector ejc) {
        for (Iterator iter = jmsConfiguratorList.iterator(); iter.hasNext();) {
            WeakReference wr = (WeakReference)iter.next();
            Object referent = wr.get();
            if (referent == null) {
                iter.remove();
            } else if (referent == ejc) {
                return true;
            }
        }
        return false;
    }
    /**
     * Unregister all registered EjbListenerPortConnector instances in one go.
     */
    public void clear() {
        jmsConfiguratorList.clear();
    }
    /**
     * Poll all registered EjbListenerPortConnector instances to see if they
     * are in the same state as their associated listener-ports, and
     * toggle their state if not.
     */
    public void poll() {
        for (Iterator iter = jmsConfiguratorList.iterator(); iter.hasNext();) {
            WeakReference wr = (WeakReference)iter.next();
            EjbListenerPortConnector EjbListenerPortConnector = (EjbListenerPortConnector) wr.get();
            if (EjbListenerPortConnector == null) {
                iter.remove();
                continue;
            }
            try {
                if (EjbListenerPortConnector.isClosed() != EjbListenerPortConnector.isListenerPortClosed()) {
                    toggleConfiguratorState(EjbListenerPortConnector);
                }
            } catch (Exception ex) {
                log.error("Cannot change, or enquire on, state of Listener ["
                        + EjbListenerPortConnector.getJmsListener().getName() + "]", ex);
            }
        }

    }

    /**
     * Toggle the state of the EjbListenerPortConnector instance by starting/stopping
     * the receiver it is attached to (via the JmsListener).
     */
    public void toggleConfiguratorState(EjbListenerPortConnector EjbListenerPortConnector) throws ConfigurationException {
        GenericReceiver receiver = (GenericReceiver) EjbListenerPortConnector.getJmsListener().getReceiver();
        if (EjbListenerPortConnector.isListenerPortClosed()) {
            log.info("Stopping Receiver [" + receiver.getName() + "] because the WebSphere Listener-Port is in state 'stopped' but the JmsConnector in state 'open'");
            receiver.stopRunning();
        } else {
            if (receiver.getAdapter().getRunState().equals(RunStateEnum.STARTED)) {
                log.info("Starting Receiver [" + receiver.getName() + "] because the WebSphere Listener-Port is in state 'started' but the JmsConnector in state 'closed'");
                receiver.startRunning();
            } else {
                try {
                    log.warn("JmsConnector is closed, Adapter is not in state '" + RunStateEnum.STARTED + "', but WebSphere Jms Listener Port is in state 'started'. Stopping the listener port.");
                    EjbListenerPortConnector.stop();
                } catch (ListenerException ex) {
                    log.error(ex,ex);
                }
            }
        }
    }
}
