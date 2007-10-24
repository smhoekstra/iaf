/*
 * $Log: EjbJmsConfigurator.java,v $
 * Revision 1.4.2.2  2007-10-24 15:04:44  europe\M00035F
 * Let runstate of receivers/listeners follow the state of WebSphere ListenerPorts if they are changed outside the control of IBIS.
 *
 * Revision 1.4.2.1  2007/10/23 09:46:11  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add TODO item
 *
 * Revision 1.4  2007/10/16 09:52:35  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Change over JmsListener to a 'switch-class' to facilitate smoother switchover from older version to spring version
 *
 * Revision 1.3  2007/10/15 13:08:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * EJB updates
 *
 * Revision 1.1.2.5  2007/10/15 08:36:31  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
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
import java.util.Set;
import javax.jms.Destination;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.naming.Context;
import javax.naming.InitialContext;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IJmsConfigurator;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.PushingJmsListener;
import nl.nn.adapterframework.receivers.GenericReceiver;

/**
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class EjbJmsConfigurator implements IJmsConfigurator {
    private final static String LISTENER_PORTNAME_SUFFIX = "ListenerPort";
    
    private PushingJmsListener jmsListener;
    private ObjectName listenerPortMBean;
    private AdminService adminService;
    private Destination destination;
    private Configuration configuration;
    private boolean closed;
    private ListenerPortPoller listenerPortPoller;
    
    public Destination getDestination() {
        return destination;
    }

    public void configureJmsReceiver(PushingJmsListener jmsListener) throws ConfigurationException {
        try {
            this.jmsListener = jmsListener;
            this.listenerPortMBean = lookupListenerPortMBean(jmsListener);
            // TODO: Verification that the ListenerPort is configured same as JmsListener
            String destinationName = (String) getAdminService().getAttribute(listenerPortMBean, "jmsDestJNDIName");
            Context ctx = new InitialContext();
            this.destination = (Destination) ctx.lookup(destinationName);
            
            closed = isListenerPortClosed();
            
            listenerPortPoller.registerEjbJmsConfigurator(this);
        } catch (ConfigurationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConfigurationException(ex);
        }
    }

    public void openJmsReceiver() throws ListenerException {
        try {
            getAdminService().invoke(listenerPortMBean, "start", null, null);
            // Register again, to be sure, b/c a registration can have been
            // removed by some other controlling code.
            listenerPortPoller.registerEjbJmsConfigurator(this);
            closed = false;
        } catch (Exception ex) {
            throw new ListenerException(ex);
        }
    }

    public void closeJmsReceiver() throws ListenerException {
        try {
            getAdminService().invoke(listenerPortMBean, "stop", null, null);
            closed = true;
        } catch (Exception ex) {
            throw new ListenerException(ex);
        }
    }

    public boolean isListenerPortClosed() throws ConfigurationException {
        try {
            return !((Boolean)getAdminService().getAttribute(listenerPortMBean, "started")).booleanValue();
        } catch (Exception ex) {
            throw new ConfigurationException("Failure enquiring on state of Listener Port MBean '"
                    + listenerPortMBean + "'", ex);
        }
    }
    
    /**
     * Lookup the MBean for the listener-port in WebSphere that the JMS Listener
     * binds to.
     */
    protected ObjectName lookupListenerPortMBean(PushingJmsListener jmsListener)  throws ConfigurationException {
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
                        + listenerPortName + "', PushingJmsListener can not be configured");
            } else if (names.size() > 1) {
                throw new ConfigurationException("Multiple WebSphere ListenerPorts found by name of '"
                        + listenerPortName + "': " + names + ", PushingJmsListener can not be configured");
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
    protected String getListenerPortName(PushingJmsListener jmsListener) {
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

    public boolean isClosed() {
        return closed;
    }

    public PushingJmsListener getJmsListener() {
        return jmsListener;
    }

    public ListenerPortPoller getListenerPortPoller() {
        return listenerPortPoller;
    }

    public void setListenerPortPoller(ListenerPortPoller listenerPortPoller) {
        this.listenerPortPoller = listenerPortPoller;
    }

}
