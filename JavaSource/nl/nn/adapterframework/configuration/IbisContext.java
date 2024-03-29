/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.JdkVersion;

/**
 * Main entry point for creating and starting Ibis instances from
 * the configuration file.
 * 
 * This class can not be created from the Spring context, because it
 * is the place where the Spring context is created.
 * 
 * 
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version $Id$
 */
public class IbisContext {
    private final static Logger log = LogUtil.getLogger(IbisContext.class);

    public static final String DFLT_AUTOSTART = "TRUE";
	//public static final String DFLT_SPRING_CONTEXT = "/springContext.xml";
	public static final String APPLICATION_SERVER_TYPE = "application.server.type";
    
    private ApplicationContext applicationContext;
	private static String springContextFileName = null;
    private IbisManager ibisManager;
    
	/**
	 * Initialize Ibis with all default parameters.
	 * 
	 * @return
	 */
	public boolean initConfig() {
	    return initConfig(getSpringContextFileName(), IbisManager.DFLT_CONFIGURATION, IbisContext.DFLT_AUTOSTART);
	}
    
    /**
     * Initalize Ibis with the given parameters, substituting default
     * values when <code>null</code> is passed in.
     * 
     * This method creates the Spring context, and loads the configuration
     * file. After executing this method, the BeanFactory, IbisManager and Configuration
     * properties are available and the Ibis instance can be started and
     * stopped.
     * 
     * @param springContext
     * @param configurationFile
     * @param autoStart
     * @return
     */
    public boolean initConfig(String springContext, String configurationFile, String autoStart) {
		initContext(springContext);        
        ibisManager.loadConfigurationFile(configurationFile);
        
        if ("TRUE".equalsIgnoreCase(autoStart)) {
            log.info("* IBIS Startup: Starting adapters");
            ibisManager.startIbis();
        }
        log.info("* IBIS Startup: Startup complete");
        return true;
    }

	public void initContext(String springContext) {
		log.info("* IBIS Startup: Running on JDK version [" + System.getProperty("java.version")
				+ "], Spring indicates JDK Major version: 1." + (JdkVersion.getMajorJavaVersion()+3));
		// This should be made conditional, somehow
//		startJmxServer();
		
		applicationContext = createApplicationContext(springContext);
		ibisManager = (IbisManager) applicationContext.getBean("ibisManager");
		AbstractSpringPoweredDigesterFactory.setIbisContext(this);
	}

	/**
	 * Create Spring Bean factory. Parameter 'springContext' can be null.
	 * 
	 * Create the Spring Bean Factory using the supplied <code>springContext</code>,
	 * if not <code>null</code>.
	 * 
	 * @param springContext Spring Context to create. If <code>null</code>,
	 * use the default spring context.
	 * The spring context is loaded as a spring ClassPathResource from
	 * the class path.
	 * 
	 * @return The Spring XML Bean Factory.
	 * @throws BeansException If the Factory can not be created.
	 * 
	 */
	static public ApplicationContext createApplicationContext(String springContext) throws BeansException {
		// Reading in Spring Context
		if (springContext == null) {
		    springContext = getSpringContextFileName();
		}
		log.info("* IBIS Startup: Creating Spring ApplicationContext from file [" + springContext + "]");
//		Resource rs = new ClassPathResource(springContext);
//		XmlBeanFactory bf = new XmlBeanFactory(rs);
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(springContext);
		return applicationContext;
	}
	
	public void destroyConfig() {
		((ConfigurableApplicationContext)applicationContext).close();
	}

//	public Object getAutoWiredObject(Class clazz) throws ConfigurationException {
//		return getAutoWiredObject(clazz, null);
//	}
//	
//	public Object getAutoWiredObject(Class clazz, String prototypeName) throws ConfigurationException {
//		
//		String beanName;
//		
//		prototypeName="proto-"+prototypeName;
//		// No explicit classname given; get bean from Spring Factory
//		if (clazz == null) {
//			beanName = prototypeName;
//		} else {
//			// Get all beans matching the classname given
//			String[] matchingBeans = getBeanFactory().getBeanNamesForType(clazz);
//			if (matchingBeans.length == 1) {
//				// Only 1 bean of this type, so create it
//				beanName = matchingBeans[0];
//			} else if (matchingBeans.length > 1) {
//				// multiple beans; find if there's one with the
//				// same name as from 'getBeanName'.
//				beanName = prototypeName;
//			} else {
//				// No beans matching the type.
//				// Create instance, and if the instance implements
//				// Spring's BeanFactoryAware interface, use it to
//				// set BeanFactory attribute on this Bean.
//				try {
//					return createBeanAndAutoWire(clazz, prototypeName);
//				} catch (Exception e) {
//					throw new ConfigurationException(e);
//				}
//			}
//		}
//        
//		// Only accept prototype-beans!
//		if (!getBeanFactory().isPrototype(beanName)) {
//			throw new ConfigurationException("Beans created from the BeanFactory must be prototype-beans, bean ["
//				+ beanName + "] of class [" + clazz.getName() + "] is not.");
//		}
//		if (log.isDebugEnabled()) {
//			log.debug("Creating bean with actual bean-name [" + beanName + "], bean-class [" + (clazz != null ? clazz.getName() : "null") + "] from Spring Bean Factory.");
//		}
//		return getBeanFactory().getBean(beanName, clazz);
//	}

//	protected Object createBeanAndAutoWire(Class beanClass, String prototype) throws InstantiationException, IllegalAccessException {
//		if (log.isDebugEnabled()) {
//			log.debug("Bean class [" + beanClass.getName() + "] not found in Spring Bean Factory, instantiating directly and using Spring Factory for auto-wiring support.");
//		}
//		Object o = beanClass.newInstance();
//		if (getBeanFactory() instanceof AutowireCapableBeanFactory) {
//			((AutowireCapableBeanFactory)getBeanFactory()).autowireBeanProperties(o,AutowireCapableBeanFactory.AUTOWIRE_BY_NAME,false);
//			o = ((AutowireCapableBeanFactory)getBeanFactory()).initializeBean(o, prototype);
//		} else if (o instanceof BeanFactoryAware) {
//			((BeanFactoryAware)o).setBeanFactory(getBeanFactory());
//		}
//		return o;
//	}

//	private void startJmxServer() {
//		//Start MBean server
//        
//        // It seems that no reference to the server is required anymore,
//        // anywhere later? So no reference is returned from
//        // this method.
//        log.info("* IBIS Startup: Attempting to start MBean server");
//		MBeanServer server=MBeanServerFactory.createMBeanServer();
//		try {
//		  ObjectInstance html = server.createMBean("com.sun.jdmk.comm.HtmlAdaptorServer", null);
//		    
//		  server.invoke(html.getObjectName(), "start", new Object[0], new String[0]);
//        } catch (ReflectionException e ) {
//            log.error("Requested JMX Server MBean can not be created; JMX not available.");
//            return;
//        } catch (Exception e) {
//		    log.error("Error with jmx:",e);
//            return;
//		}
//		log.info("MBean server up and running. Monitor your application by pointing your browser to http://localhost:8082");
//	}

//	public void setBeanFactory(ListableBeanFactory factory) {
//		beanFactory = factory;
//	}


	private static String getSpringContextFileName() {
		if (springContextFileName==null) {
			springContextFileName = "/springContext" + AppConstants.getInstance().getString(APPLICATION_SERVER_TYPE, "") + ".xml";
		}
		return springContextFileName;
	}

	public IbisManager getIbisManager() {
		return ibisManager;
	}

	public static void main(String[] args) {
		IbisContext im=new IbisContext();
		im.initConfig(getSpringContextFileName(), IbisManager.DFLT_CONFIGURATION, IbisContext.DFLT_AUTOSTART);
	}
	
	public Object getBean(String beanName) {
		return applicationContext.getBean(beanName);
	}
	
	public Object getBean(String beanName, Class beanClass) {
		return applicationContext.getBean(beanName, beanClass);
	}

	public Object createBean(Class beanClass, int autowireMode, boolean dependencyCheck) {
		return applicationContext.getAutowireCapableBeanFactory().createBean(beanClass, autowireMode, false);
	}
	
	public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck) {
		applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(existingBean, autowireMode, dependencyCheck);
	}
	
	public void initializeBean(Object existingBean, String beanName) {
		applicationContext.getAutowireCapableBeanFactory().initializeBean(existingBean, beanName);
	}

	public String[] getBeanNamesForType(Class beanClass) {
		return applicationContext.getBeanNamesForType(beanClass);
	}

	public boolean isPrototype(String beanName) {
		return applicationContext.isPrototype(beanName);
	}

}
