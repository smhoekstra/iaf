/*
 * $Log: AbstractListenerConnectingEJB.java,v $
 * Revision 1.1.2.1  2007-10-29 10:29:13  europe\M00035F
 * Refactor: pullup a number of methods to abstract base class so they can be shared with new IFSA Session EJBs
 *
 * 
 */

package nl.nn.adapterframework.ejb;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.receivers.GenericReceiver;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.jndi.JndiLookupFailureException;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
abstract public class AbstractListenerConnectingEJB extends AbstractEJBBase {
    protected Logger log = LogUtil.getLogger(GenericMDB.class);

    protected boolean containerManagedTransactions;

    protected ListenerPortPoller listenerPortPoller;

    protected boolean retrieveTransactionType() {
        try {
            Boolean txType = (Boolean) getContextVariable("containerTransactions");
            if (txType == null) {
                log.warn("Value of variable 'containerTransactions' in Bean JNDI context is null, assuming bean-managed transactions");
                return false;
            } else {
                return txType.booleanValue();
            }
        } catch (JndiLookupFailureException e) {
            log.error("Cannot look up variable 'containerTransactions' in Bean JNDI context; assuming bean-managed transactions", e);
            return false;
        }
    }

    protected IListener retrieveListener(String receiverName, String adapterName) {
        IAdapter adapter = config.getRegisteredAdapter(adapterName);
        GenericReceiver receiver = (GenericReceiver) adapter.getReceiverByName(receiverName);
        return receiver.getListener();
    }

    protected void rollbackTransaction() throws IllegalStateException {
        if (containerManagedTransactions) {
            getEJBContext().setRollbackOnly();
        } else {
            try {
                getEJBContext().getUserTransaction().setRollbackOnly();
            } catch (Exception ex) {
                log.error("Cannot roll back user-transactions, must be using container-managed transactions without being properly configured for it?", ex);
                // Try the container-maanged way
                try {
                    getEJBContext().setRollbackOnly();
                } catch (IllegalStateException e) {
                    log.error("After failing to rolll back user-transaction, also failing to roll back container-transaction.", e);
                }
                throw new IllegalStateException("Cannot roll back user-transaction; must be using container-managed transactions? Error-message: ["
                        + ex.getMessage() + "]");
            }
        }
    }

    public ListenerPortPoller getListenerPortPoller() {
        return listenerPortPoller;
    }

    public void setListenerPortPoller(ListenerPortPoller listenerPortPoller) {
        this.listenerPortPoller = listenerPortPoller;
    }

}
