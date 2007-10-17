/*
 * $Log: SpringJmsConfigurator.java,v $
 * Revision 1.3.2.1  2007-10-17 14:19:08  europe\M00035F
 * Merge changes from HEAD
 *
 * Revision 1.4  2007/10/17 11:33:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add at least one consumer
 *
 * Revision 1.3  2007/10/16 09:52:35  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Change over JmsListener to a 'switch-class' to facilitate smoother switchover from older version to spring version
 *
 * Revision 1.2  2007/10/15 13:11:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * copy from EJB branch
 *
 */
package nl.nn.adapterframework.unmanaged;

import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IJmsConfigurator;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.PushingJmsListener;
import nl.nn.adapterframework.receivers.GenericReceiver;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.log4j.Logger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Configure a Spring JMS Container from a {@link nl.nn.adapterframework.jms.PushingJmsListener}.
 * 
 * This implementation expects to receive an instance of
 * org.springframework.jms.listener.DefaultMessageListenerContainer
 * from the Spring BeanFactory. If another type of MessageListenerContainer
 * is created by the BeanFactory, then another implementation of IJmsConfigurator
 * should be provided as well.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class SpringJmsConfigurator extends AbstractJmsConfigurator implements IJmsConfigurator, BeanFactoryAware, ExceptionListener {
	private static final Logger log = LogUtil.getLogger(SpringJmsConfigurator.class);
	
	public static final TransactionDefinition TXSUPPORTS = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS);
	public static final TransactionDefinition TXMANDATORY = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY);
    
	public static final String version="$RCSfile: SpringJmsConfigurator.java,v $ $Revision: 1.3.2.1 $ $Date: 2007-10-17 14:19:08 $";
    
	private String connectionFactoryName;
	private PlatformTransactionManager txManager;
	private BeanFactory beanFactory;
	private DefaultMessageListenerContainer jmsContainer;
	private String messageListenerClassName;
    
	protected DefaultMessageListenerContainer createMessageListenerContainer() throws ConfigurationException {
		try {
			Class klass = Class.forName(messageListenerClassName);
			return (DefaultMessageListenerContainer) klass.newInstance();
		} catch (Exception e) {
			throw new ConfigurationException("Error creating instance of MessageListenerContainer ["+messageListenerClassName+"]", e);
		}
	}
    
	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.configuration.IJmsConfigurator#configureReceiver(nl.nn.adapterframework.jms.PushingJmsListener)
	 */
	public void configureJmsReceiver(final PushingJmsListener jmsListener) throws ConfigurationException {
		super.configureJmsReceiver(jmsListener);
        
		// Create the Message Listener Container manually.
		// This is needed, because otherwise the Spring Factory will
		// call afterPropertiesSet() on the object which will validate
		// that all required properties are set before we get a chance
		// to insert our dynamic values from the config. file.
		this.jmsContainer = createMessageListenerContainer();
        
		if (jmsListener.isTransacted()) {
			this.jmsContainer.setTransactionManager(this.txManager);
		}
        
		// Initialize with a number of dynamic properties which come from the configuration file
		try {
			this.connectionFactoryName = jmsListener.getConnectionFactoryName();
			ConnectionFactory connectionFactory = createConnectionFactory(this.connectionFactoryName);
            
			this.jmsContainer.setConnectionFactory(connectionFactory);
			this.jmsContainer.setDestination(getDestination());
		} catch (JmsException e) {
			throw new ConfigurationException("Cannot look up destination", e);
		}
        
		this.jmsContainer.setExceptionListener(this);
		this.jmsContainer.setReceiveTimeout(jmsListener.getTimeOut());
        
		final GenericReceiver receiver = (GenericReceiver) jmsListener.getHandler();
		final Counter threadsProcessing = new Counter(0);
		if (receiver.getNumThreads() > 0) {
			this.jmsContainer.setConcurrentConsumers(receiver.getNumThreads());
		} else {
			this.jmsContainer.setConcurrentConsumers(1);
		}
		this.jmsContainer.setMessageListener(new SessionAwareMessageListener() {
			public void onMessage(Message message, Session session)
				throws JMSException {
                
				threadsProcessing.increase();
				Thread.currentThread().setName(receiver.getName()+"["+threadsProcessing.getValue()+"]");
                
				TransactionStatus txStatus = null;
				Map threadContext = new HashMap();
				try {
					if (jmsListener.isTransacted()) {
						txStatus = txManager.getTransaction(TXMANDATORY);
						if (txStatus.isNewTransaction()) {
							log.error("Current Transaction is NEW, but was retrieved is using propagation:MANDATORY");
						}
					}
					jmsListener.populateThreadContext(threadContext, session);
					receiver.processRawMessage(jmsListener, message, threadContext,
						jmsListener.getTimeOut());
				} catch (ListenerException e) {
					invalidateSessionTransaction(e, session, txStatus);
				} finally {
					threadsProcessing.decrease();
					jmsListener.destroyThreadContext(threadContext);
				}
			}

			private void invalidateSessionTransaction(Throwable t, Session session,
					TransactionStatus txStatus) throws JMSException {
				// TODO Proper way to handle this error
				if (!jmsListener.isTransacted()) {
					log.debug("Exception caught in onMessage; rolling back JMS Session", t);
					session.rollback();
				} else {
					if (txStatus == null) {
						log.error("Unable to rollback current global transaction since it is null! Original exception for which we want to rollback:", t);
						throw new javax.jms.IllegalStateException("Trying to roll back current global transaction for transacted listener ["
								+ jmsListener.getName()+"] but reference to transaction-status is null; original exception for which we want to rollback is instance of "
								+ t.getClass().getName()+"; exception message: "+t.getMessage());
					}
					log.debug("Exception caught in onMessage; rolling back global transaction", t);
					txStatus.setRollbackOnly();
					throw new JMSException("Forcing rollback because Receiver could not process the message");
				}
			}
		});
		// Use Spring BeanFactory to complete the auto-wiring of the JMS Listener Container,
		// and run the bean lifecycle methods.
		try {
			((AutowireCapableBeanFactory) this.beanFactory).configureBean(this.jmsContainer, "proto-jmsContainer");
		} catch (BeansException e) {
			throw new ConfigurationException("Out of luck wiring up and configuring Default JMS Message Listener Container for JMS Listener "
					+ (jmsListener.getName() != null?jmsListener.getName():jmsListener.getLogPrefix()), e);
		}
        
		// Finally, set bean name to something we can make sense of
		if (jmsListener.getName() != null) {
			this.jmsContainer.setBeanName(jmsListener.getName());
		} else {
			this.jmsContainer.setBeanName(jmsListener.getLogPrefix());
		}
        
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.configuration.IJmsConfigurator#openJmsReceiver()
	 */
	public void openJmsReceiver() throws ListenerException {
		log.debug("Starting Spring JMS Container");
		try {
			jmsContainer.start();
		} catch (Exception e) {
			throw new ListenerException("Cannot start Spring JMS Container", e);
		}
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.configuration.IJmsConfigurator#closeJmsReceiver()
	 */
	public void closeJmsReceiver() throws ListenerException {
		log.debug("Stopping Spring JMS Container");
		try {
			jmsContainer.stop();
		} catch (Exception e) {
			throw new ListenerException("Exception while trying to stop Spring JMS Container", e);
		}
	}

	public void onException(JMSException e) {
		log.error("JMS Exception occurred in Message Listener Container configured for " +
				(getJmsListener().isUseTopicFunctions()?"Topic":"Queue")
				+ "Connection Factory [" + connectionFactoryName
				+ "], destination [" + getDestinationName()
				+ "]:", e);
		IbisExceptionListener ibisExceptionListener = getJmsListener().getExceptionListener();
		if (ibisExceptionListener == null) {
			ibisExceptionListener = (IbisExceptionListener) getJmsListener().getHandler();
		}
		if (ibisExceptionListener!= null) {
			log.error("Reporting the error to the IBIS Exception Listener");
			getJmsListener().getExceptionListener().exceptionThrown(getJmsListener(), e);
		} else {
			log.error("Cannot report the error to an IBIS Exception Listener");
		}
	}


	public void setTxManager(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}


	public void setMessageListenerClassName(String messageListenerClassName) {
		this.messageListenerClassName = messageListenerClassName;
	}
	public String getMessageListenerClassName() {
		return messageListenerClassName;
	}

}
