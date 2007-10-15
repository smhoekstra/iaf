/*
 * $Log: EjbDelegatingIbisManager.java,v $
 * Revision 1.1.2.8  2007-10-15 09:51:57  europe\M00035F
 * Add back transaction-management to BrowseExecute action
 *
 * Revision 1.1.2.7  2007/10/15 09:20:16  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Update logging
 *
 * Revision 1.1.2.6  2007/10/12 14:29:31  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Several fixes and improvements to get EJB deployment mode running
 *
 * Revision 1.1.2.5  2007/10/12 09:45:42  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add 'XPathUtil' interface with multiple implementations (both direct XPath API using, and indirect Transform API using) and remove the code from the EjbDelegatingIbisManager
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

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XPathUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class EjbDelegatingIbisManager implements IbisManager, BeanFactoryAware {
    private final static Logger log = LogUtil.getLogger(EjbDelegatingIbisManager.class);
    
    private static final String FACTORY_BEAN_ID = "&ibisManagerEjb";
    private static final String JNDI_NAME_PREFIX = "ejb/ibis/IbisManager/";
    
    private final static String CONFIG_NAME_XPATH = "/child::*/@configurationName";
    
    private String configurationName;
    private IbisManager ibisManager;
    private BeanFactory beanFactory;
    private XPathUtil xPathUtil;
    private PlatformTransactionManager transactionManager;
    
    protected synchronized IbisManager getIbisManager() {
        if (this.ibisManager == null) {
            if (configurationName == null) {
                log.error("Cannot look up the IbisManager implementation when configuration-name not yet read from the configuration file");
                return null;
            }
            // Look it up via EJB, using JNDI Name based on configuration name
            LocalStatelessSessionProxyFactoryBean factoryBean = 
                    (LocalStatelessSessionProxyFactoryBean) beanFactory.getBean(FACTORY_BEAN_ID);
            String beanJndiName = JNDI_NAME_PREFIX + configurationName.replace(' ', '-');
            factoryBean.setJndiName(beanJndiName);
            this.ibisManager = (IbisManager) factoryBean.getObject();
            log.info("Looked up IbisManagerEjb at JNDI location '" + beanJndiName + "'");
        }
        return this.ibisManager;
    }
    
    public Configuration getConfiguration() {
        IbisManager mngr = getIbisManager();
        if (mngr == null) {
            log.error("Cannot look up the configuration when the IbisManager is not set");
            return null;
        } else {
            Configuration cfg = mngr.getConfiguration();
            if (cfg == null) {
                log.error("Retrieved null configuration object from real IbisManager");
            } else {
                log.info("Configuration retrieved from real IbisManager: configuration-name '"
                        + cfg.getConfigurationName() + "', nr of adapters: "
                        + cfg.getRegisteredAdapters().size());
            }
            return cfg;
        }
    }

    public void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy) {
        getIbisManager().handleAdapter(action, adapterName, receiverName, commandIssuedBy);
    }

    public void startIbis() {
        // Not implemented for this case, since the Ibis will be auto-started from EJB container
    }

    public void startAdapters() {
        getIbisManager().startAdapters();
    }

    public void stopAdapters() {
        getIbisManager().stopAdapters();
    }

    public void startAdapter(IAdapter adapter) {
        getIbisManager().startAdapter(adapter);
    }

    public void stopAdapter(IAdapter adapter) {
        getIbisManager().stopAdapter(adapter);
    }

    public void loadConfigurationFile(String configurationFile) {
        try {
            setConfigurationName(xPathUtil.parseXpathToString(CONFIG_NAME_XPATH, configurationFile));
            if (getConfigurationName() == null) {
                log.error("Can not start the Ibis WEB front-end because no configuration-name can be extracted from the configuration-file '"
                    + configurationFile + "'");
                throw new IllegalStateException("Configuration-name loaded from configuration-file '"
                    + configurationFile + "' is null; this means that the Ibis WEB front can not be started.");
            }
            log.info("Extracted configuration-name '" + getConfigurationName()
                    + "' from configuration-file '" + configurationFile + "'");
        } catch (Exception ex) {
            log.error("Error retrieving configuration-name from configuration file '" +
                    configurationFile + "'", ex);
        }
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public String getDeploymentModeString() {
        return IbisManager.DEPLOYMENT_MODE_EJB_STRING;
    }

    public int getDeploymentMode() {
        return IbisManager.DEPLOYMENT_MODE_EJB;
    }

    public XPathUtil getXPathUtil() {
        return xPathUtil;
    }

    public void setXPathUtil(XPathUtil xPathUtil) {
        this.xPathUtil = xPathUtil;
    }

    public PlatformTransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
