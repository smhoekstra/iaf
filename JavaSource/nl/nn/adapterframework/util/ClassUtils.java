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
package nl.nn.adapterframework.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.log4j.Logger;

/**
 * A collection of class management utility methods.
 * @version $Id$
 * @author Johan Verrips
 *
 */
public class ClassUtils {
	public static final String version = "$RCSfile: ClassUtils.java,v $ $Revision: 1.23 $ $Date: 2012-04-12 09:01:26 $";
	private static Logger log = LogUtil.getLogger(ClassUtils.class);

	private static final boolean trace=false;

    /**
     * Return the context classloader.
     * BL: if this is command line operation, the classloading issues
     *     are more sane.  During servlet execution, we explicitly set
     *     the ClassLoader.
     *
     * @return The context classloader.
     */
    public static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
	/**
	* Retrieves the constructor of a class, based on the parameters
	*
	**/
	public static Constructor getConstructorOnType(Class clas, Class[] parameterTypes) {
		Constructor theConstructor = null;
		try {
			theConstructor = clas.getDeclaredConstructor(parameterTypes);
		} catch (java.lang.NoSuchMethodException e) {
			log.error("cannot create constructor for Class [" + clas.getName() + "]", e);
			for (int i = 0; i < parameterTypes.length; i++)
				log.error("Parameter " + i + " type " + parameterTypes[i].getName());
		}
		return theConstructor;
	}
    /**
     * Return a resource URL.
     * BL: if this is command line operation, the classloading issues
     *     are more sane.  During servlet execution, we explicitly set
     *     the ClassLoader.
     *
     * @return The context classloader.
     * @deprecated Use getResourceURL().openStream instead.
     */
    public static InputStream getResourceAsStream(Class klass, String resource) throws  IOException {
	    InputStream stream=null;
	    URL url=getResourceURL(klass, resource);


	     stream=url.openStream();
        return stream;

    }

	static public URL getResourceURL(String resource) {
		return getResourceURL(null,resource);
	}

  /**
     * Get a resource-URL, first from Class then from ClassLoader
     * if not found with class.
	 *
     * @deprecated Use getResourceURL(Object, resource) instead.
     *
     */
    static public URL getResourceURL(Class klass, String resource)
    {
    	resource = Misc.replace(resource,"%20"," ");
        URL url = null;

		if (klass == null) {
			klass = ClassUtils.class;
		}

        // first try to get the resource as a resource
        url = klass.getResource(resource);
		if (url == null) {
			url = klass.getClassLoader().getResource(resource);
		}
		// then try to get it as a URL
		if (url == null) {
			try {
				url = new URL(Misc.replace(resource," ","%20"));
            } catch(MalformedURLException e) {
				log.debug("Could not find resource as URL [" + resource + "]: "+e.getMessage());
			}
        }
		// then try to get it in java:comp/env
		if (url == null && resource!=null && !resource.startsWith("java:comp/env/")) {
			log.debug("cannot find URL for resource ["+resource+"], now trying [java:comp/env/"+resource+"] (e.g. for TomCat)");
			String altResource = "java:comp/env/"+resource;
			url = klass.getResource(altResource); // to make things work under tomcat
			if (url == null) {
				url = klass.getClassLoader().getResource(altResource);
			}
		}

        if (url==null)
          log.warn("cannot find URL for resource ["+resource+"]");
        else {

			// Spaces must be escaped to %20. But ClassLoader.getResource(String)
			// has a bug in Java 1.3 and 1.4 and doesn't do this escaping.
			// See also:
			//
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4778185
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4785848
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4273532
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4496398
			//
			// Escaping spaces to %20 if spaces are found.
			String urlString = url.toString();
			if (urlString.indexOf(' ')>=0 && !urlString.startsWith("jar:")) {
				urlString=Misc.replace(urlString," ","%20");
				try {
					URL escapedURL = new URL(urlString);
					log.debug("resolved resource-string ["+resource+"] to URL ["+escapedURL.toString()+"]");
					return escapedURL;
				} catch(MalformedURLException e) {
					log.warn("Could not find URL from space-escaped url ["+urlString+"], will use unescaped original version ["+url.toString()+"] ");
				}
			}
        }
        return url;
    }
  /**
     * Get a resource-URL, first from Class then from ClassLoader
     * if not found with class.
     *
     */
    static public URL getResourceURL(Object obj, String resource)
    {
        return getResourceURL(obj.getClass(), resource);
    }
    /**
     * Tests if a class implements a given interface
     *
     * @return true if class implements given interface.
     */
    public static boolean implementsInterface(Class class1, Class iface) {
        return iface.isAssignableFrom (class1);
    }
    /**
     * Tests if a class implements a given interface
     *
     * @return true if class implements given interface.
     */
    public static boolean implementsInterface(String className, String iface) throws Exception {
        Class class1 = ClassUtils.loadClass (className);
        Class class2 = ClassUtils.loadClass (iface);
        return ClassUtils.implementsInterface(class1, class2);
    }
    /**
     * Determine the last modification date for this
     * class file or its enclosing library
     *
     * @param aClass A class whose last modification date is queried
     * @return The time the given class was last modified
     * @exception IOException IOError
     * @exception IllegalArgumentException The class was not loaded from a file
     * or directory
     */
    public static long lastModified(Class aClass)
        throws IOException, IllegalArgumentException  {
        URL url = aClass.getProtectionDomain().getCodeSource().getLocation();

        if (!url.getProtocol().equals("file")) {
            throw new IllegalArgumentException("Class was not loaded from a file url");
        }

        File directory = new File(url.getFile());
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Class was not loaded from a directory");
        }

        String className = aClass.getName();
        String basename = className.substring(className.lastIndexOf(".") + 1);

        File file = new File(directory, basename + ".class");

        return file.lastModified();
    }
    /**
     * Load a class given its name.
     * BL: We wan't to use a known ClassLoader--hopefully the heirarchy
     *     is set correctly.
     *
     * @param className A class name
     * @return The class pointed to by <code>className</code>
     * @exception ClassNotFoundException If a loading error occurs
     */
    public static Class loadClass(String className) throws ClassNotFoundException {
        return ClassUtils.getClassLoader().loadClass(className);
    }
    /**
     * Create a new instance given a class name. The constructor of the class
     * does NOT have parameters.
     *
     * @param className A class name
     * @return A new instance
     * @exception Exception If an instantiation error occurs
     */
    public static Object newInstance(String className) throws Exception {
        return ClassUtils.loadClass(className).newInstance();
    }
  /**
   * creates a new instance of an object, based on the classname as string, the classes
   * and the actual parameters.
   */
  public static Object newInstance(String className, Class[] parameterClasses, Object[] parameterObjects) {
    // get a class object
    Class clas = null;
    try {
      clas=ClassUtils.loadClass(className);
    } catch (java.lang.ClassNotFoundException C) {System.err.println(C);}

     Constructor con;
     con= ClassUtils.getConstructorOnType(clas, parameterClasses);
     Object theObject=null;
     try {
        theObject=con.newInstance(parameterObjects);
      } catch(java.lang.InstantiationException E) {System.err.println(E);}
        catch(java.lang.IllegalAccessException A) {System.err.println(A);}
        catch(java.lang.reflect.InvocationTargetException T) {System.err.println(T);}
     return theObject;

  }

	/**
	 * Creates a new instance from a class, while it looks for a constructor
	 * that matches the parameters, and initializes the object (by calling the constructor)
	 * Notice: this does not work when the instantiated object uses an interface class
	 * as a parameter, as the class names are, in that case, not the same..
	 *
	 * @param className a class Name
	 * @param parameterObjects the parameters for the constructor
	 * @return A new Instance
	 *
	 **/
	public static Object newInstance(String className, Object[] parameterObjects) {
	    Class parameterClasses[] = new Class[parameterObjects.length];
	    for (int i = 0; i < parameterObjects.length; i++)
	        parameterClasses[i] = parameterObjects[i].getClass();
	    return newInstance(className, parameterClasses, parameterObjects);
	}

    /**
     * Gets the absolute pathname of the class file
     * containing the specified class name, as prescribed
     * by the current classpath.
     *
     * @param aClass A class
     */
     public static String which(Class aClass) {
        String path = null;
        try {
            path = aClass.getProtectionDomain().getCodeSource().getLocation().toString();
        } catch (Throwable t){
        }
        return path;
    }

 	/**
 	 * returns the classname of the object, without the pacakge name.
 	 */
	public static String nameOf(Object o) {
		if (o==null) {
			return "<null>";
		}
		String name=o.getClass().getName();
		int pos=name.lastIndexOf('.');
		if (pos<0) {
			return name;
		} else {
			return name.substring(pos+1);
		}
	}

	public static void invokeSetter(Object o, String name, Object value) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		invokeSetter(o,name,value,value.getClass());
	}
	public static void invokeSetter(Object o, String name, Object value, Class clazz) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Class argsTypes[] = { clazz };
		Method setterMtd = o.getClass().getMethod(name, argsTypes );
		Object args[] = { value };
		setterMtd.invoke(o,args);
	}
	public static Object invokeGetterSafe(Object o, String name, boolean forceAccess) {
		try {
			return invokeGetter(o,name,forceAccess);
		} catch (Exception e) {
			return nameOf(o)+"."+name+"() "+nameOf(e)+": "+e.getMessage();
		}
	}
	public static Object invokeGetter(Object o, String name, boolean forceAccess) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Method getterMtd = o.getClass().getMethod(name, null );
		if (forceAccess) {
			getterMtd.setAccessible(true);
		}
		return getterMtd.invoke(o,null);
	}
	public static Object invokeGetter(Object o, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return invokeGetter(o,name,false);
	}

	public static String invokeStringGetterSafe(Object o, String name) {
		try {
			return invokeStringGetter(o,name);
		} catch (Exception e) {
			return nameOf(o)+"."+name+"() "+nameOf(e)+": "+e.getMessage();
		}
	}
	public static String invokeStringGetter(Object o, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return (String)invokeGetter(o,name);
	}

	public static Object getFieldValueSafe(Object o, String name) {
		try {
			return getFieldValue(o,name);
		} catch (Exception e) {
			return nameOf(o)+"."+name+" "+nameOf(e)+": "+e.getMessage();
		}
	}
	public static Object getFieldValue(Object o, Class c, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return c.getField(name).get(o);
	}
	public static Object getFieldValue(Object o, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return getFieldValue(o, o.getClass(), name);
	}

	public static Object getDeclaredFieldValue(Object o, Class c, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		Field f = c.getDeclaredField(name);
		try {
			f.setAccessible(true);
			return f.get(o);
		} catch (Exception e) {
			log.error(e);
			return e.getMessage();
		}
	}
	public static Object getDeclaredFieldValue(Object o, String name) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return getDeclaredFieldValue(o, o.getClass(), name);
	}

	private static void appendFieldsAndMethods(StringBuffer result, Object o, String type, Class c) {
		Field fields[] = c.getDeclaredFields();
		Method methods[] = c.getDeclaredMethods();
		result.append(type+ " "+c.getName()+" #fields ["+fields.length+"] #methods ["+methods.length+"]");
		if (fields.length>0 || methods.length>0) {
			result.append(" {\n");
			for (int i=0; i<fields.length; i++) {
				Field f=fields[i];
				Object value;
				try {
					f.setAccessible(true);
					value=f.get(o);
				} catch (Exception e) {
					value="Could not get value: "+ClassUtils.nameOf(e)+": "+e.getMessage();
				}
				result.append("  field["+i+"] "+f.getName()+"("+f.getType().getName()+"): ["+value+"]\n");
			}
			for (int i=0; i<methods.length; i++) {
				Method m=methods[i];
				result.append("  method["+i+"] "+m.getName());
//				Object value;
//				try {
//					m.setAccessible(true);
//					value=m.invoke(o,null);
//				} catch (Exception e) {
//					value="Could not get value: "+ClassUtils.nameOf(e)+": "+e.getMessage();
//				}
//				result +=": ["+value+"]\n";
				result.append("\n");
			}
			result.append("}");
		}
		result.append("\n");
	}

	public static String debugObject(Object o) {
		if (o==null) {
			return null;
		}
		StringBuffer result=new StringBuffer(nameOf(o)+"\n");
		Class c=o.getClass();
		Class interfaces[] = c.getInterfaces();
		for (int i=0;i<interfaces.length; i++) {
			appendFieldsAndMethods(result,o,"Interface",interfaces[i]);
		}
		while (c!=Object.class) {
			appendFieldsAndMethods(result,o,"Class",c);
			c=c.getSuperclass();
		}
		result.append("toString=["+o.toString()+"]\n");
		result.append("reflectionToString=["+reflectionToString(o,null)+"]\n");
		return result.toString();
	}

	public static String reflectionToString(final Object o, final String fieldnameEnd) {
		String result=(new ReflectionToStringBuilder(o) {
				protected boolean accept(Field f) {
					if (super.accept(f)) {
						if (trace) log.debug(nameOf(o)+" field ["+f.getName()+"]");
						return fieldnameEnd==null || f.getName().endsWith(fieldnameEnd);
					}
					return false;
				}
			}).toString();
		return result;
	}

}
