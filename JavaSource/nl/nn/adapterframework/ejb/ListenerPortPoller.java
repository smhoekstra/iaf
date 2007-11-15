/*
 * ListenerPortPoller.java
 *  
 * $Log: ListenerPortPoller.java,v $
 * Revision 1.1.2.5  2007-11-15 10:34:31  europe\M00035F
 * Method in references class was renamed
 *
 * Revision 1.1.2.4  2007/11/15 09:54:23  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Add more detailed logging
 * * Do not attempt to start/stop a receiver which is in state 'Starting'
 *
 * Revision 1.1.2.3  2007/11/06 09:39:13  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
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
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.GenericReceiver;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

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
public class ListenerPortPoller implements DisposableBean {
    private Logger log = LogUtil.getLogger(this);
    
    private List portConnectorList = new ArrayList();

    public ListenerPortPoller() {
        log.debug("Created new ListenerPortPoller: " + this.toString());
    }
    
    
    /**
     * Add an EjbListenerPortConnector instance to be polled.
     * 
     * Only add instances if they are not already registered.
     */
    public void registerEjbListenerPortConnector(EjbListenerPortConnector ejc) {
        if (!isRegistered(ejc)) {
            portConnectorList.add(new WeakReference(ejc));
        }
    }
    
    /**
     * Remove an EjbListenerPortConnector instance from the list to be polled.
     */
    public void unregisterEjbListenerPortConnector(EjbListenerPortConnector ejc) {
        for (Iterator iter = portConnectorList.iterator(); iter.hasNext();) {
            WeakReference wr = (WeakReference)iter.next();
            Object referent = wr.get();
            if (referent == null || referent == ejc) {
                iter.remove();
            }
        }
    }
    
    public boolean isRegistered(EjbListenerPortConnector ejc) {
        for (Iterator iter = portConnectorList.iterator(); iter.hasNext();) {
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
        portConnectorList.clear();
    }
    /**
     * Poll all registered EjbListenerPortConnector instances to see if they
     * are in the same state as their associated listener-ports, and
     * toggle their state if not.
     */
    public void poll() {
        if (log.isDebugEnabled()) {
            log.debug("Enter polling " + this.toString() + ", thread: " + Thread.currentThread().getName());
        }
        for (Iterator iter = portConnectorList.iterator(); iter.hasNext();) {
            WeakReference wr = (WeakReference)iter.next();
            EjbListenerPortConnector elpc = (EjbListenerPortConnector) wr.get();
            if (elpc == null) {
                iter.remove();
                continue;
            }
            // Check for each ListenerPort if it's state matches with the
            // state that IBIS thinks it should be in.
            IPortConnectedListener listener = elpc.getListener();
            try {
                if (elpc.isClosed() != elpc.isListenerPortClosed()) {
                    log.info("State of listener [" + listener.getName()
                            + "] does not match state of WebSphere ListenerPort ["
                            + elpc.getListenerPortName(listener)
                            + "] to which it is attached; will try to change state of Receiver ["
                            + listener.getReceiver().getName() +"]");
                    toggleConfiguratorState(elpc);
                }
            } catch (Exception ex) {
                log.error("Cannot change, or enquire on, state of Listener ["
                        + listener.getName() + "]", ex);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Exit polling " + this.toString() + ", thread: " + Thread.currentThread().getName());
        }
    }

    /**
     * Toggle the state of the EjbListenerPortConnector instance by starting/stopping
     * the receiver it is attached to (via the JmsListener).
     * This method changes the state of the Receiver to match the state of the
     * WebSphere ListenerPort.
     */
    public void toggleConfiguratorState(EjbListenerPortConnector elpc) throws ConfigurationException {
        GenericReceiver receiver = (GenericReceiver) elpc.getListener().getReceiver();
        if (elpc.isListenerPortClosed()) {
            if (receiver.isInRunState(RunStateEnum.STARTED)) {
                log.info("Stopping Receiver [" + receiver.getName() + "] because the WebSphere Listener-Port is in state 'stopped' but the JmsConnector in state 'open' and the receiver is in state 'started'");
                receiver.stopRunning();
            } else {
                log.info("ListenerPort [" + elpc.getListenerPortName(elpc.getListener())
                        + "] is in closed state, Listener is not in closed state, but Receiver is in state ["
                        + receiver.getRunState().getName() + "] so the state of Receiver will not be changed.");
                
            }
        } else {
            // Only start the receiver for adapters which are running.
            if (receiver.getAdapter().getRunState().equals(RunStateEnum.STARTED)) {
                if (!receiver.isInRunState(RunStateEnum.STARTING)) {
                    log.info("Starting Receiver [" + receiver.getName() + "] because the WebSphere Listener-Port is in state 'started' but the JmsConnector in state 'closed'");
                    receiver.startRunning();
                } else {
                    log.info("Receiver [" + receiver.getName() + "] still starting, so not changing anything now");
                }
            } else {
                try {
                    log.warn("JmsConnector is closed, Adapter is not in state '" + RunStateEnum.STARTED + "', but WebSphere Jms Listener Port is in state 'started'. Stopping the listener port.");
                    elpc.stop();
                } catch (ListenerException ex) {
                    log.error(ex,ex);
                }
            }
        }
    }

    public void destroy() throws Exception {
        clear();
    }
}
