/*
 * $Log: CustomIfsaFFReceiverMDB.java,v $
 * Revision 1.1.2.2  2007-11-02 13:01:09  europe\M00035F
 * Add JavaDoc comment
 *
 * Revision 1.1.2.1  2007/11/02 11:47:05  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add custom versions of IFSA MDB Receiver beans, and subclass of IFSA ServiceLocatorEJB
 *
 *
 * $Id: CustomIfsaFFReceiverMDB.java,v 1.1.2.2 2007-11-02 13:01:09 europe\M00035F Exp $
 *
 */
package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.provider.FFReceiver;
import com.ing.ifsa.provider.Receiver;
import javax.jms.Message;
import org.apache.log4j.Logger;

/**
 * IfsaReceiverMDB for FireForget services.
 * 
 * @author Tim van der Leeuw
 * @version Id
 */
public class CustomIfsaFFReceiverMDB extends CustomIfsaReceiverMDBAbstractBase {
    private static final Logger log = Logger.getLogger(CustomIfsaFFReceiverMDB.class);

    public void onMessage(Message msg) {
        if (log.isInfoEnabled()) {
            log.info(">>> onMessage()");
        }
        if (!((FFReceiver) receiver).handleMessage(msg)) {
            log.warn("message was not handled succesfully, rollback transaction");
            getMessageDrivenContext().setRollbackOnly();
        }
        if (log.isInfoEnabled()) {
            log.info("<<< onMessage");
        }
    }

    protected Receiver createReceiver() {
        return new FFReceiver(serviceLocator);
    }

}
