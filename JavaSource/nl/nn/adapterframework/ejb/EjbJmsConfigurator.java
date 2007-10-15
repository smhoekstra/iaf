/*
 * $Log: EjbJmsConfigurator.java,v $
 * Revision 1.1.2.5  2007-10-15 08:36:31  europe\M00035F
 * Fix lookup of JMX MBean for ListenerPort
 *
 * Revision 1.1.2.4  2007/10/12 14:29:31  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Several fixes and improvements to get EJB deployment mode running
 *
 * Revision 1.1.2.3  2007/10/10 14:30:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.2  2007/10/10 09:48:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.ejb;

import com.ibm.websphere.management.AdminService;
import com.ibm.websphere.management.AdminServiceFactory;
import java.util.Hashtable;
import java.util.Set;
import javax.jms.Destination;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IJmsConfigurator;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.receivers.GenericReceiver;

/**
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class EjbJmsConfigurator implements IJmsConfigurator {
    private final static String LISTENER_PORTNAME_SUFFIX = "ListenerPort";
    
    private JmsListener jmsListener;
    private ObjectName listenerPortMBean;
    private AdminService adminService;
    private Destination destination;
    private Configuration configuration;
    
    public Destination getDestination() {
        return destination;
    }

    public void configureJmsReceiver(JmsListener jmsListener) throws ConfigurationException {
        try {
            this.jmsListener = jmsListener;
            this.listenerPortMBean = lookupListenerPortMBean(jmsListener);
            String destinationName = (String) getAdminService().getAttribute(listenerPortMBean, "jmsDestJNDIName");
            Context ctx = new InitialContext();
            this.destination = (Destination) ctx.lookup(destinationName);
        } catch (ConfigurationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConfigurationException(ex);
        }
    }

    public void openJmsReceiver() throws ListenerException {
        try {
            getAdminService().invoke(listenerPortMBean, "start", null, null);
        } catch (Exception ex) {
            throw new ListenerException(ex);
        }
    }

    public void closeJmsReceiver() throws ListenerException {
        try {
            getAdminService().invoke(listenerPortMBean, "stop", null, null);
        } catch (Exception ex) {
            throw new ListenerException(ex);
        }
    }
    
    /**
     * Lookup the MBean for the listener-port in WebSphere that the JMS Listener
     * binds to.
     */
    protected ObjectName lookupListenerPortMBean(JmsListener jmsListener)  throws ConfigurationException {
        try {
            // Get the admin service
            AdminService as = getAdminService();
            String listenerPortName = getListenerPortName(jmsListener);
            
            // Create ObjectName instance to search for
//            Hashtable queryProperties = new Hashtable();
//            queryProperties.put("name",listenerPortName);
//            queryProperties.put("type", "ListenerPort");
//            ObjectName queryName = new ObjectName("WebSphere", queryProperties);
            
            ObjectName queryName = new ObjectName("WebSphere:type=ListenerPort,name="
                    + listenerPortName + ",*");
            // Query AdminService for the name
            Set names = as.queryNames(queryName, null);
            
            // Assume that only 1 is returned and return it
            if (names.size() == 0) {
                throw new ConfigurationException("Can not find WebSphere ListenerPort by name of '"
                        + listenerPortName + "', JmsListener can not be configured");
            } else if (names.size() > 1) {
                throw new ConfigurationException("Multiple WebSphere ListenerPorts found by name of '"
                        + listenerPortName + "': " + names + ", JmsListener can not be configured");
            } else {
                return (ObjectName) names.iterator().next();
            }
        } catch (MalformedObjectNameException ex) {
            throw new ConfigurationException(ex);
        }
    }
    
    /**
     * Get the WebSphere admin-service for accessing MBeans.
     */
    protected synchronized AdminService getAdminService() {
        if (this.adminService == null) {
            this.adminService = AdminServiceFactory.getAdminService();
        }
        return this.adminService;
    }
    
    /**
     * Get the name of the ListenerPort to look up as WebSphere MBean.
     * 
     * Construct the name of the WebSphere listenerport according to the
     * following logic:
     * <ol>
     * <li>If the property 'listenerPort' is set in the configuration, then use that</li>
     * <li>Otherwise, concatenate the configuration-name with the receiver-name, replaces all spaces with minus-signs, and append 'ListenerPort'
     * </ol>
     * 
     */
    protected String getListenerPortName(JmsListener jmsListener) {
        String name = jmsListener.getListenerPort();
        
        if (name == null) {
            GenericReceiver receiver;
            receiver = (GenericReceiver)jmsListener.getHandler();
            name = configuration.getConfigurationName()
                    + '-' + receiver.getName() + LISTENER_PORTNAME_SUFFIX;
            name = name.replace(' ', '-');
        }
        
        return name;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}
