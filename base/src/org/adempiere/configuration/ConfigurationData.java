/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
package org.adempiere.configuration;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.db.CConnection;
import org.compiere.db.Database;
import org.compiere.model.MEMailConfig;
import org.compiere.util.CLogger;
import org.compiere.util.EMail;
import org.compiere.util.Ini;
import org.compiere.util.Login;
import org.compiere.util.SecureEngine;

/**
 * ConfigurationData. Used to manage the configuration data contained in the
 * AdempiereEnv.properties file and, indirectly through the Ini class, the
 * adempiere.properties file. <br>
 * <br>
 * The ConfigurationData class can be used to access the configuration data.
 * After instantiation, call the {@link #load() load} method which will load the
 * data from the current AdempiereEnv.properties file. If that file can't be
 * found or doesn't exist, the load function will try to load the properties
 * from the AdempiereEnvTemplate.properties file. If that also can't be found, a
 * hard-coded set of defaults will be used. <br>
 * <br>
 * If the data is changed, the configuration data should be tested before being
 * saved by calling the {@link #test() test} method. <br>
 * <br>
 * Public assess to the properties is through the
 * {@link #setProperty(String, String) setProperty} and
 * {@link #getProperty(String) getProperty} methods. These methods will call the
 * individual set/get method for the property or will access the property
 * directly. The "get" functions will return a default value where these are
 * required and the property value is null or an empty string. "Set" functions
 * will validate the new property value and will throw errors if there are any
 * problems. <br>
 * <br>
 * To set defaults and/or test the property, use<br>
 * <br>
 * <code>
 *    setProperty(property, getPropertyFunction(property));
 *  </code> <br>
 * <br>
 * Changing the value of a property will fire a Property Change Event. Listeners
 * can register for these events using the
 * {@link #addPropertyChangeListener(PropertyChangeListener)
 * addPropertyChangeListner} method. <br>
 * <br>
 * For the Java Type, Apps Server Type and Database Type properties, changes in
 * the value of these properties will set the associated/dependent properties to
 * default values for that type. Where dependent properties are not relevant in
 * a particular case, the {@link #isUsedList} Hashtable can be consulted to
 * determine if a UI field should be enabled or not. <br>
 * <br>
 * Test results on the value of a property are also signaled through the
 * Property Change event using a property name "TEST_ERROR". For these events,
 * the new value consists of an object that implements the {@link ErrorData}
 * interface. This interface identifies the Test property which starts with
 * "TEST_", the test result (pass/fail), whether the property is critical and an
 * error message. UI interfaces to the configuration data can use this to signal
 * test results to the user.
 * 
 * @author Jorg Janke
 * @version $Id: ConfigurationData.java,v 1.4 2006/07/30 00:57:42 jjanke Exp $
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com <li>FR [
 *         402 ] Mail setup is hardcoded
 * @see https://github.com/adempiere/adempiere/issues/402 <li>FR [ 391 ] Add
 *      connection support to MariaDB
 * @see https://github.com/adempiere/adempiere/issues/464
 * @author mckayERP, michael.mckay@mckayerp.com <li>Rewrite to follow MVC design
 *         and allow for other UI interfaces
 */
public class ConfigurationData implements PropertyChangeListener {

	/*
	 * Database and Java configurations to support various vendors are
	 * hard-coded in lists of available types. Adding an additional vendor can
	 * be done by creating the configuration class implementing
	 * org.compiere.install.Config and adding a reference to the class to the
	 * appropriate static array below.
	 * 
	 * Adding properties can be done by:
	 * 
	 * * Creating a constant property name
	 * 
	 * * Adding the property to the set of {@link #KNOWN_PROPERTIES
	 * KNOWN_PROPERTIES}
	 * 
	 * * If necessary, creating set/get functions to manage the defaults and
	 * tests of the property value.
	 * 
	 * * Adding the set/get functions to the function maps.
	 * 
	 * Dependencies between properties should be managed through the
	 * propertyChangeEvent listener and not within the "set" functions.
	 */

	/**
	 * An interface class passed through Property Change Events to provide
	 * information about property test results. Use in Property Change Event
	 * listeners as follows: <br>
	 * 
	 * <pre>
	 * public void propertyChange(PropertyChangeEvent pce) {
	 * 
	 * 	String propertyName = pce.getPropertyName();
	 * 	if (ConfigurationData.TEST_ERROR.equals(propertyName))
	 * 	{
	 * 		if (pce.getNewValue() instanceof ConfigurationData.ErrorData)
	 * 		{
	 * 			// The config data is sending a test result
	 * 			ConfigurationData.ErrorData errorData = (ConfigurationData.ErrorData) pce.getNewValue();
	 * 		
	 * 			String propName = errorData.propertyName;
	 * 			String errorMsg = errorData.errorMessage;
	 * 			boolean pass = errorData.pass;
	 * 			boolean critical = errorData.critical;
	 * 		
	 * 			// Do something brilliant with this data ...
	 * 		}
	 * 	}
	 * </pre>
	 * 
	 * @author mckayERP
	 *
	 */
	public class ErrorData {

		/**
		 * A property defined in the {@link ConfigurationData} class. For
		 * generic test results, the propertyName may start with "TEST_"
		 */
		public String propertyName = "";

		/** The test result. true for no error. false if there was an error. */
		public boolean pass = false;

		/**
		 * A flag to indicate if the property is critical or not. Critical
		 * properties will need to pass the tests before the properties can be
		 * saved. Non critical errors can be ignored.
		 */
		public boolean critical = false;

		/** The error message or a null/empty string */
		public String errorMessage = "";

		/**
		 * Construct the ErrorData class with default values
		 */
		public ErrorData() {

			propertyName = "";
			pass = false;
			critical = false;
			errorMessage = "";
		}
		
		public String toString() {
			return "ErrorData: Property = " + propertyName 
					+ ", pass = " + pass 
					+ ", critical = " + critical 
					+ ", error message = " + errorMessage + "";
		}
	}

	/**
	 * Include Property change support to record listners and fire events.
	 */
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	/**
	 * Add a property change listener to the configuration data properties.
	 * Changes to the properties will cause a property change event.
	 * 
	 * @param listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {

		this.pcs.addPropertyChangeListener(listener);
	}

	/**
	 * Remove a listner from the list of listeners.
	 * 
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {

		this.pcs.removePropertyChangeListener(listener);
	}

	/** Static Logger */
	static CLogger log = CLogger.getCLogger(ConfigurationData.class);

	/**
	 * Deprecated Constructor that communicated directly with a UI. The class
	 * has been modified to pass such communication through property change
	 * events. Use ConfigurationData() instead and register the UI as a
	 * PropertyChangeListener using
	 * {@link #addPropertyChangeListener(PropertyChangeListener)}.
	 * 
	 * @param UI
	 *            panel - ignored.
	 */
	@Deprecated
	public ConfigurationData(Object panel) {

		this();
	}

	/**
	 * Constructor. Clears the properties but does not load them. Register
	 * Property Change Listeners and then call {@link #load()} to load the
	 * properties.
	 */
	public ConfigurationData() {

		super();

		// Initialize the setter/getter lists.
		initPropertyFunctionMaps();

		// Register 'this' as a the property change listener to manage
		// dependencies
		this.addPropertyChangeListener(this);

		// Initialize the property container. Does not add properties or set any
		// values.
		p_properties.clear();

		// Initialize all fields as "used". The configuration instances that
		// implement
		// the Config interface will update the values of these as required.
		setAllFieldsAsUsed();

	}

	/**
	 * Get the specified property value. The properties should be initialized by
	 * calling {@link #load} before making this call.
	 * 
	 * @param property
	 *            - the name of the property which must be a known and not null.
	 * @return a String with the property value.
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public String getProperty(String property) throws IllegalArgumentException {

		if (property == null || property.isEmpty())
			throw new IllegalArgumentException(
					"The property argument can not be null or empty.");

		if (!KNOWN_PROPERTIES.contains(property))
			throw new IllegalArgumentException("The property '" + property
					+ "' is not a known property name.");

		Method method = getPropertyFunction.get(property);

		if (method == null)
		{
			log.warning("No get function map for property '"
					+ property
					+ "'. Using direct access instead. Default values will not be set.");
			return p_properties.getProperty(property);
		}

		String returnValue = null;
		@SuppressWarnings("rawtypes")
		Class[] params = method.getParameterTypes();
		if (params.length == 0)
		{
			try
			{
				returnValue = (String) method.invoke(this);
			}
			catch (IllegalAccessException | InvocationTargetException e)
			{
				log.warning(e.getMessage());
				e.printStackTrace();

				// Fallback - no defaults/test
				returnValue = p_properties.getProperty(property);
			}
		}
		else if (params.length == 1 && params[0] == String.class)
		{
			try
			{
				returnValue = (String) method.invoke(this, property);
			}
			catch (IllegalAccessException | InvocationTargetException e)
			{
				log.warning(e.getMessage());
				e.printStackTrace();

				// Fallback - no defaults/test
				returnValue = p_properties.getProperty(property);
			}
		}
		else
		{

			log.warning("The get function for property '" + property
					+ "' takes " + params.length
					+ " parameters.  None or one were expected.");

			// Fallback - no defaults/test
			returnValue = p_properties.getProperty(property);
		}

		return returnValue;

	}

	/**
	 * Set the given property to value.
	 * 
	 * @param property
	 *            The property name as specified in
	 *            {@link org.adempiere.configuration.ConfigurationData}
	 * @param value
	 *            A string value.
	 * @throws IllegalArgumentException
	 *             if the property is null, empty or not known.
	 */
	public String setProperty(String property, String value)
			throws IllegalArgumentException
	{

		if (property == null || property.isEmpty())
			throw new IllegalArgumentException(
					"The property argument can not be null or empty.");

		if (!KNOWN_PROPERTIES.contains(property))
			throw new IllegalArgumentException("The property '" + property
					+ "' is not a known property name.");

		Method method = setPropertyFunction.get(property);

		if (method == null)
		{
			log.fine("No set function map for property '"
					+ property
					+ "' taking String as argument. Using direct access instead. The property value will not be tested.");
			// fall back - no checking/testing
			updateProperty(property, value);
			return value;
		}

		@SuppressWarnings("rawtypes")
		Class[] params = method.getParameterTypes();
		if (params[0].equals(String.class))
		{
			try
			{
				method.invoke(this, value);
			}
			catch (IllegalAccessException | InvocationTargetException e)
			{
				log.severe(e.getMessage());
				e.printStackTrace();
				// fall back - no checking/testing
				updateProperty(property, value);
			}
		}
		else
		{
			log.severe("The set function for property '" + property
					+ "' takes " + params[0].getSimpleName()
					+ " as its argument. A String class is expected.");
			// fall back - no checking/testing
			updateProperty(property, value);
		}

		return value;
	}

	/**
	 * Set all properties as used.
	 */
	private void setAllFieldsAsUsed() {

		// Initialize/set list of properties as used that may be readonly/unused
		// Server Related fields - set by the server config classes.
		for (String property : KNOWN_PROPERTIES)
			isUsedList.put(property, true);

		// Add a few special cases
		isUsedList.put(DATABASE_DISCOVERED, true);
		isUsedList.put(DISCOVERED_DATABASE_LIST, true);
		isUsedList.put(TEST_ERROR, true);

	}

	/**
	 * Environment Properties with the keys() function overridden to sort
	 * alphabetically when saving the properties to file
	 */
	@SuppressWarnings("serial")
	protected Properties p_properties = new Properties() {

		@Override
		public synchronized Enumeration<Object> keys() {

			return Collections.enumeration(new TreeSet<Object>(super.keySet()));
		}
	};

	/** ADempiere Home */
	private File m_adempiereHome;

	/** Properties File name */
	public static final String ADEMPIERE_ENV_FILE = "AdempiereEnv.properties";

	/** Properties Template File name */
	public static final String ADEMPIERE_ENV_TEMPLATE_FILE = "AdempiereEnvTemplate.properties";

	/**
	 * Property ADEMPIERE_DATE_VERSION used to determine if the source code and
	 * database versions match.
	 */
	public static final String ADEMPIERE_DATE_VERSION = "ADEMPIERE_DATE_VERSION";

	/**
	 * Property ADEMPIERE_CONNECTION in the Adempiere.properties file to
	 * identify the database connection.
	 */
	public static final String ADEMPIERE_CONNECTION = "Connection";

	/**
	 * Property ADEMPIERE_HOME used to specify the value of the environment
	 * variable of the same name.
	 */
	public static final String ADEMPIERE_HOME = "ADEMPIERE_HOME";

	/** Property JAVA_HOME used to specify the directory containing the JDK. */
	public static final String JAVA_HOME = "JAVA_HOME";

	/**
	 * Property ADEMPIERE_JAVA_TYPE used to identify the JAVA vendor. Acceptable
	 * values can be found in the {@link #JAVATYPE} String array.
	 */
	public static final String ADEMPIERE_JAVA_TYPE = "ADEMPIERE_JAVA_TYPE";

	/**
	 * Property ADEMPIERE_JAVA_OPTIONS contains the java options that should be
	 * used by the JVM when it is initialized.
	 */
	public static final String ADEMPIERE_JAVA_OPTIONS = "ADEMPIERE_JAVA_OPTIONS";

	/**
	 * Property ADEMPIERE_APPS_TYPE used to identify the application server
	 * type. Acceptable values can be found in the {@link #APPSTYPE} String
	 * array.
	 */
	public static final String ADEMPIERE_APPS_TYPE = "ADEMPIERE_APPS_TYPE";

	/**
	 * Property ADEMPIERE_APPS_SERVER defines the application server address
	 * which is the primary access for the web interface, system admin functions
	 * and the client web start. Once the server is running, the application
	 * server access will be found at <br>
	 * <br>
	 * {@literal http://<ADEMPIERE_APPS_SERVER>:<ADEMPIERE_WEB_PORT>/admin} <br>
	 * <br>
	 * "localhost" or the loopback address (127.0.0.1) are not allowed. The
	 * default value will be the host name of the first active Internet
	 * interface found.
	 */
	public static final String ADEMPIERE_APPS_SERVER = "ADEMPIERE_APPS_SERVER";

	/**
	 * Property ADEMPIERE_APPS_DEPLOY - the web server directory into which the
	 * application server ear/war/jars will be deployed. This is a dependent
	 * property of {@link #ADEMPIERE_APPS_TYPE}.
	 */
	public static final String ADEMPIERE_APPS_DEPLOY = "ADEMPIERE_APPS_DEPLOY";

	/**
	 * Property ADEMPIERE_JNP_PORT - the Java Naming Protocol port. This is a
	 * dependent property of {@link #ADEMPIERE_APPS_TYPE}.
	 */
	public static final String ADEMPIERE_JNP_PORT = "ADEMPIERE_JNP_PORT";

	/**
	 * Property ADEMPIERE_WEB_PORT defines the application server port. Once the
	 * server is running, the application server access will be found at <br>
	 * <br>
	 * {@literal http://<ADEMPIERE_APPS_SERVER>:<ADEMPIERE_WEB_PORT>/admin}
	 */
	public static final String ADEMPIERE_WEB_PORT = "ADEMPIERE_WEB_PORT";

	/**
	 * Property ADEMPIERE_SSL_PORT defines the application server port for
	 * Secure Socket Layer communication. Once the server is running, the
	 * application server access will be found at <br>
	 * <br>
	 * {@literal https://<ADEMPIERE_APPS_SERVER>:<ADEMPIERE_SSL_PORT>/admin}
	 */
	public static final String ADEMPIERE_SSL_PORT = "ADEMPIERE_SSL_PORT";

	/** Property ADEMPIERE_WEB_ALIAS */
	public static final String ADEMPIERE_WEB_ALIAS = "ADEMPIERE_WEB_ALIAS";

	/**
	 * Property ADEMPIERE_KEYSTORE contains the file path/name of the keystore
	 * used to hold the security certificates that are used to secure the
	 * application jar files.
	 */
	public static final String ADEMPIERE_KEYSTORE = "ADEMPIERE_KEYSTORE";

	/** Property ADEMPIERE_KEYSTOREPASS - the keystore password */
	public static final String ADEMPIERE_KEYSTOREPASS = "ADEMPIERE_KEYSTOREPASS";

	/** The Keystore code alias */
	public static final String ADEMPIERE_KEYSTORECODEALIAS = "ADEMPIERE_KEYSTORECODEALIAS";
	/** The Keystore web alias */
	public static final String ADEMPIERE_KEYSTOREWEBALIAS = "ADEMPIERE_KEYSTOREWEBALIAS";

	/**
	 * Property ADEMPIERE_CERT_CN is the default certificate common name used in
	 * the Keystore.
	 */
	public static String ADEMPIERE_CERT_CN = "ADEMPIERE_CERT_CN";
	/**
	 * Property ADEMPIERE_CERT_ORG is the default certificate organization name
	 * used in the Keystore.
	 */
	public static String ADEMPIERE_CERT_ORG = "ADEMPIERE_CERT_ORG";
	/**
	 * Property ADEMPIERE_CERT_ORG_UNIT is the default certificate
	 * organizational unit name used in the Keystore.
	 */
	public static String ADEMPIERE_CERT_ORG_UNIT = "ADEMPIERE_CERT_ORG_UNIT";
	/**
	 * Property ADEMPIERE_CERT_LOCATION is the default certificate
	 * organizational location used in the Keystore.
	 */
	public static String ADEMPIERE_CERT_LOCATION = "ADEMPIERE_CERT_LOCATION";
	/**
	 * Property ADEMPIERE_CERT_STATE is the default certificate organizational
	 * province/state used in the Keystore.
	 */
	public static String ADEMPIERE_CERT_STATE = "ADEMPIERE_CERT_STATE";
	/**
	 * Property ADEMPIERE_CERT_COUNTRY is the default certificate country used
	 * in the Keystore.
	 */
	public static String ADEMPIERE_CERT_COUNTRY = "ADEMPIERE_CERT_COUNTRY";

	/** Database Type */
	public static final String ADEMPIERE_DB_TYPE = "ADEMPIERE_DB_TYPE";
	/** Database Path */
	public static final String ADEMPIERE_DB_PATH = "ADEMPIERE_DB_PATH";
	/** Database server name */
	public static final String ADEMPIERE_DB_SERVER = "ADEMPIERE_DB_SERVER";
	/** Database port */
	public static final String ADEMPIERE_DB_PORT = "ADEMPIERE_DB_PORT";
	/** Database name */
	public static final String ADEMPIERE_DB_NAME = "ADEMPIERE_DB_NAME";
	/** Database URL */
	public static final String ADEMPIERE_DB_URL = "ADEMPIERE_DB_URL";

	/** Database user name */
	public static final String ADEMPIERE_DB_USER = "ADEMPIERE_DB_USER";
	/** Database password */
	public static final String ADEMPIERE_DB_PASSWORD = "ADEMPIERE_DB_PASSWORD";

	/** Database admin password - use {@link #ADEMPIERE_DB_SYSTEM_PASS} instead */
	@Deprecated
	public static final String ADEMPIERE_DB_SYSTEM = "ADEMPIERE_DB_SYSTEM";

	/** Database system/admin password */
	public static final String ADEMPIERE_DB_SYSTEM_PASS = "ADEMPIERE_DB_SYSTEM_PASS";

	/** Mail server name */
	public static final String ADEMPIERE_MAIL_SERVER = "ADEMPIERE_MAIL_SERVER";
	/** Mail server user used to access the mail server */
	public static final String ADEMPIERE_MAIL_USER = "ADEMPIERE_MAIL_USER";
	/** Password of the ADEMPIERE_MAIL_USER */
	public static final String ADEMPIERE_MAIL_PASSWORD = "ADEMPIERE_MAIL_PASSWORD";
	/**
	 * The email address of the ADempiere Administrator. This address will be
	 * sent a copy of the properties when the properties are tested.
	 */
	public static final String ADEMPIERE_ADMIN_EMAIL = "ADEMPIERE_ADMIN_EMAIL";
	/** 				*/
	public static final String ADEMPIERE_MAIL_UPDATED = "ADEMPIERE_MAIL_UPDATED";
	// FR [ 402 ]
	/** The port for outgoing email */
	public static final String ADEMPIERE_MAIL_PORT = "ADEMPIERE_MAIL_PORT";
	/** The mail protocol to use */
	public static final String ADEMPIERE_MAIL_PT = "ADEMPIERE_MAIL_PROTOCOL";
	/** The mail encryption type to use */
	public static final String ADEMPIERE_MAIL_ET = "ADEMPIERE_MAIL_ENCRYPTION_TYPE";
	/** The mail authentication mechanism */
	public static final String ADEMPIERE_MAIL_AM = "ADEMPIERE_MAIL_AUTHENTICATION_MECHANISM";
	/** The FTP server name */
	public static final String ADEMPIERE_FTP_SERVER = "ADEMPIERE_FTP_SERVER";
	/** The FTP server user name */
	public static final String ADEMPIERE_FTP_USER = "ADEMPIERE_FTP_USER";
	/** The FTP password */
	public static final String ADEMPIERE_FTP_PASSWORD = "ADEMPIERE_FTP_PASSWORD";
	/** The FTP prefix */
	public static final String ADEMPIERE_FTP_PREFIX = "ADEMPIERE_FTP_PREFIX";

	/** The ADempiere webstores */
	public static final String ADEMPIERE_WEBSTORES = "ADEMPIERE_WEBSTORES";

	/**
	 * The database version from {@link org.compiere.Adempiere#DB_VERSION}
	 */
	public static final String ADEMPIERE_DB_VERSION = "ADEMPIERE_DB_VERSION";
	/**
	 * The main version string from {@link org.compiere.Adempiere#MAIN_VERSION}
	 */
	public static final String ADEMPIERE_MAIN_VERSION = "ADEMPIERE_MAIN_VERSION";

	// Properties for the test results - not saved
	public static final String TEST_JAVA_HOME = "TEST_JAVA_HOME";
	public static final String TEST_ADEMPIERE_HOME = "TEST_ADEMPIERE_HOME";
	public static final String TEST_KEYSTORE_PASS = "TEST_KEYSTORE_PASS";
	public static final String TEST_APPS_SERVER = "TEST_APPS_SERVER";
	public static final String TEST_DEPLOYMENT_DIR = "TEST_DEPLOYMENT_DIR";
	public static final String TEST_JNP_PORT = "TEST_JNP_PORT";
	public static final String TEST_WEB_PORT = "TEST_WEB_PORT";
	public static final String TEST_SSL_PORT = "TEST_SSL_PORT";
	public static final String TEST_DB_SERVER = "TEST_DB_SERVER";
	public static final String TEST_DB_PORT = "TEST_DB_PORT";
	public static final String TEST_DB_NAME = "TEST_DB_NAME";
	public static final String TEST_DB_ADMIN_PASS = "TEST_DB_ADMIN_PASS";
	public static final String TEST_DB_PASS = "TEST_DB_PASS";
	public static final String TEST_DB_USER = "TEST_DB_USER";
	public static final String TEST_DB_SQL = "TEST_DB_SQL";
	public static final String TEST_EMAIL_PORT = "TEST_EMAIL_PORT";
	public static final String TEST_EMAIL_PASS = "TEST_EMAIL_PASS";
	public static final String TEST_ERROR = "TEST_ERROR";

	/**************************************************************************
	 * Java Settings
	 *************************************************************************/

	/** SUN VM (default) */
	private static String JAVATYPE_SUN = "sun";
	/** Apple VM */
	private static String JAVATYPE_MAC = "mac";
	/** IBM VM */
	private static String JAVATYPE_IBM = "<ibm>";
	/** Open JDK */
	private static String JAVATYPE_OPENJDK = "OpenJDK";
	/** Java VM Types */
	public static String[] JAVATYPE = new String[] { JAVATYPE_SUN,
			JAVATYPE_OPENJDK, JAVATYPE_MAC, JAVATYPE_IBM };

	/** An array of classes to configure various java virtual machines */
	private Config[] m_javaConfig = new Config[] { new ConfigVMSun(this),
			new ConfigVMOpenJDK(this), new ConfigVMMac(this), null };

	/**************************************************************************
	 * Application Server Settings
	 *************************************************************************/

	/** JBoss (default) */
	public static final String APPSTYPE_JBOSS = "JBoss";
	/** GlassFish */
	public static final String APPSTYPE_GLASSFISH = "GlassFish";
	/** Tomcat */
	public static final String APPSTYPE_TOMCAT = "Tomcat";

	/** Application Server Type */
	public static final String[] APPSTYPE = new String[] { APPSTYPE_TOMCAT,
			APPSTYPE_JBOSS, APPSTYPE_GLASSFISH };

	/** The list of available/known application server configuration classes */
	private Config[] m_appsConfig = new Config[] { new ConfigTomcat(this),
			new ConfigJBoss(this), new ConfigGlassfish(this) };

	/**************************************************************************
	 * Mail Settings
	 *************************************************************************/

	// FR [ 402 ]
	/** None = N */
	private static final String ENCRYPTIONTYPE_None = "None";
	/** SSL = S */
	private static final String ENCRYPTIONTYPE_SSL = "SSL";
	/** TLS = T */
	private static final String ENCRYPTIONTYPE_TLS = "TLS";
	/** Encryption Type */
	public static String[] ENCRYPTIONTYPE = new String[] { ENCRYPTIONTYPE_None,
			ENCRYPTIONTYPE_SSL, ENCRYPTIONTYPE_TLS };

	/** Login = L */
	private static final String AUTHMECHANISM_LOGIN = "Login";
	/** Plain = P */
	private static final String AUTHMECHANISM_PLAIN = "Plain";
	/** Digest-MD5 = D */
	private static final String AUTHMECHANISM_DIGEST_MD5 = "Digest-MD5";
	/** NTLM = N */
	private static final String AUTHMECHANISM_NTLM = "NTLM";
	/** NTLM = N */
	private static final String AUTHMECHANISM_OAUTH = "OAUTH2";
	/** Authentication Mechanism */
	public static String[] AUTHMECHANISMS = new String[] { AUTHMECHANISM_LOGIN,
			AUTHMECHANISM_PLAIN, AUTHMECHANISM_DIGEST_MD5, AUTHMECHANISM_NTLM,
			AUTHMECHANISM_OAUTH };

	/** SMTP = S */
	private static final String PROTOCOL_SMTP = "SMTP";
	/** POP3 = P */
	// private static final String PROTOCOL_POP3 = "POP3";
	/** IMAP = I */
	private static final String PROTOCOL_IMAP = "IMAP";

	/** Authentication Mechanism */
	public static String[] EMAIL_PROTOCOL = new String[] { PROTOCOL_SMTP,
			PROTOCOL_IMAP };

	/**************************************************************************
	 * Database Settings
	 *************************************************************************/
	// end e-evolution vpj-cd 02/07/2005 PostgreSQL
	/** Database Types */
	public static final String[] DBTYPE = new String[] { Database.DB_ORACLE,
			Database.DB_ORACLE + "XE", Database.DB_POSTGRESQL,
			Database.DB_MYSQL, Database.DB_MARIADB };
	// end e-evolution vpj-cd 02/07/2005 PostgreSQL

	/**
	 * Database Configs - an array of classes that configure individual database
	 * types.
	 */
	public Config[] m_databaseConfig = new Config[] {
			new ConfigOracle(this, true), new ConfigOracle(this, false),
			new ConfigPostgreSQL(this), new ConfigMySQL(this),
			new ConfigMariaDB(this) };

	/**
	 * A string identifying the list of discovered databases. This list is not
	 * saved in the property file.
	 */
	public static final String DISCOVERED_DATABASE_LIST = "DISCOVERED_DATABASE_LIST";
	private String[] m_discoveredDatabases;

	/**
	 * The currently selected item in the {@link #DISCOVERED_DATABASE_LIST}
	 * array. Not a property but a dependent item used to set the Database Name
	 */
	public static final String DATABASE_DISCOVERED = "DATABASE_DISCOVERED";
	private String m_discoveredDatabase;

	/** A flag to indicate if properties are allowed to be an empty string. */
	private boolean ALLOW_EMPTY = true;

	private boolean DONT_ALLOW_EMPTY = false;

	/**************************************************************************
	 * Default values
	 *************************************************************************/
	private static final String DEFAULT_JAVA_TYPE = JAVATYPE_SUN;
	private static final String DEFAULT_JAVA_OPTIONS = "-Xms64M -Xmx512M";
	private static final String DEFAULT_JAVA_HOME = "";

	private static final String DEFAULT_ADEMPIERE_HOME = "";
	private static final String DEFAULT_APPS_TYPE = APPSTYPE_TOMCAT;
	private static final String DEFAULT_APPS_DEPLOY = "";
	private static final String DEFAULT_APPS_SERVER = "";
	private static final String DEFAULT_WEB_PORT = "80";
	private static final String DEFAULT_JNP_PORT = "1099";
	private static final String DEFAULT_SSL_PORT = "443";
	private static final String DEFAULT_WEB_ALIAS = "adempiere";

	private static final String DEFAULT_DB_NAME = "adempiere";
	private static final String DEFAULT_DB_PASSWORD = "adempiere";
	private static final String DEFAULT_DB_PATH = "";
	private static final String DEFAULT_DB_PORT = "5432";
	private static final String DEFAULT_DB_TYPE = Database.DB_POSTGRESQL;
	private static final String DEFAULT_DB_SERVER = "localhost";
	private static final String DEFAULT_DB_SYSTEM_PASS = "postgres";
	private static final String DEFAULT_DB_USER = "adempiere";

	private static final String DEFAULT_ADMIN_EMAIL = "admin@"; // "info@"
																// +
	// server name
	private static final String DEFAULT_MAIL_ET = ENCRYPTIONTYPE_SSL;
	private static final String DEFAULT_MAIL_AM = AUTHMECHANISM_LOGIN;
	private static final String DEFAULT_MAIL_USER = "info (a) gmail.com";
	private static final String DEFAULT_MAIL_PASSWORD = "";
	/**
	 * Default email ports for the Mail Protocol Type - set to work with Google
	 * SSL as an example
	 */
	private static final String[] DEFAULT_MAIL_PORTS = new String[] { "465",
			"143" };
	private static final String DEFAULT_MAIL_PORT = DEFAULT_MAIL_PORTS[0];
	private static final String DEFAULT_MAIL_PT = PROTOCOL_SMTP;
	private static final String DEFAULT_MAIL_SERVER = "smtp.gmail.com";
	private static final String DEFAULT_MAIL_UPDATED = "";

	private static final String DEFAULT_FTP_SERVER = "localhost";
	private static final String DEFAULT_FTP_USER = "anonymous";
	private static final String DEFAULT_FTP_PASSWORD = "user@host.com";
	private static final String DEFAULT_FTP_PREFIX = "my";

	/** The default Keystore Password */
	private static final String DEFAULT_KEYSTORE = "keystore"; // ADEMPIERE_HOME/keystore
	private static final String DEFAULT_KEYSTOREPASS = "myPassword";
	private static final String DEFAULT_KEYSTORECODEALIAS = "adempiere";
	private static final String DEFAULT_KEYSTOREWEBALIAS = "adempiere";
	private static final String DEFAULT_CERT_CN = "localhost";
	private static final String DEFAULT_CERT_ORG = "ADempiere Foundation";
	private static final String DEFAULT_CERT_ORG_UNIT = "ADempiereUser";
	private static final String DEFAULT_CERT_LOCATION = "myTown";
	private static final String DEFAULT_CERT_STATE = "CA";
	private static final String DEFAULT_CERT_COUNTRY = "US";

	private static final String DEFAULT_MAIN_VERSION = "";
	private static final String DEFAULT_DATE_VERSION = "";
	private static final String DEFAULT_DB_VERSION = "";

	/**
	 * A set of known properties. Use to verify if a property is valid as
	 * follows:<br>
	 * <br>
	 * <code>
	 *    boolean ok = KNOWN_PROPERTIES.contains(propertyToTest);
	 * </code><br>
	 * <br>
	 * Only the properties in this list are saved in {@link #p_properties}.
	 */
	private static final Set<String> KNOWN_PROPERTIES = new HashSet<String>(
			Arrays.asList(new String[] { ADEMPIERE_HOME, JAVA_HOME,
					ADEMPIERE_CONNECTION, ADEMPIERE_DB_VERSION,
					ADEMPIERE_DATE_VERSION, ADEMPIERE_MAIN_VERSION,
					ADEMPIERE_JAVA_TYPE, ADEMPIERE_JAVA_OPTIONS,
					ADEMPIERE_APPS_TYPE, ADEMPIERE_APPS_SERVER,
					ADEMPIERE_APPS_DEPLOY, ADEMPIERE_JNP_PORT,
					ADEMPIERE_WEB_PORT, ADEMPIERE_SSL_PORT,
					ADEMPIERE_WEB_ALIAS, ADEMPIERE_KEYSTORE,
					ADEMPIERE_KEYSTOREPASS, ADEMPIERE_KEYSTORECODEALIAS,
					ADEMPIERE_KEYSTOREWEBALIAS, ADEMPIERE_CERT_CN,
					ADEMPIERE_CERT_ORG, ADEMPIERE_CERT_ORG_UNIT,
					ADEMPIERE_CERT_LOCATION, ADEMPIERE_CERT_STATE,
					ADEMPIERE_CERT_COUNTRY, ADEMPIERE_DB_TYPE,
					ADEMPIERE_DB_PATH, ADEMPIERE_DB_SERVER, ADEMPIERE_DB_PORT,
					ADEMPIERE_DB_NAME, ADEMPIERE_DB_URL, ADEMPIERE_DB_USER,
					ADEMPIERE_DB_PASSWORD, ADEMPIERE_DB_SYSTEM_PASS,
					ADEMPIERE_MAIL_SERVER, ADEMPIERE_MAIL_USER,
					ADEMPIERE_MAIL_PASSWORD, ADEMPIERE_ADMIN_EMAIL,
					ADEMPIERE_MAIL_UPDATED, ADEMPIERE_MAIL_PORT,
					ADEMPIERE_MAIL_PT, ADEMPIERE_MAIL_ET, ADEMPIERE_MAIL_AM,
					ADEMPIERE_FTP_SERVER, ADEMPIERE_FTP_USER,
					ADEMPIERE_FTP_PASSWORD, ADEMPIERE_FTP_PREFIX,
					ADEMPIERE_WEBSTORES, ADEMPIERE_DB_SYSTEM,
					ADEMPIERE_JAVA_TYPE }));

	/** A holder for the localhost InetAddress */
	private InetAddress localhost = null;;

	/**
	 * HashMap that identifies the "set" method to use when the
	 * {@link #setProperty(String, String)} method is called.
	 */
	private HashMap<String, Method> setPropertyFunction = null;
	
	/**
	 * HashMap that identifies the "get" method to use when the
	 * {@link #getProperty(String, String)} method is called.
	 */
	private HashMap<String, Method> getPropertyFunction = null;
	
	/**
	 * HashMap that identifies the default value to use when the
	 * {@link #getProperty(String, String)} method is called.
	 */
	private HashMap<String, String> propertyDefault = null;

	/**
	 * A Hashtable of property names and a boolean value that indicates if the
	 * property is relevant/used in a particular configuration. Where the
	 * property is not used, the associated field in a GUI, for example, could
	 * be disabled.
	 */
	private Hashtable<String, Boolean> isUsedList = new Hashtable<String, Boolean>();

	/**
	 * A flag indicating the properties are being loaded. Don't respond to
	 * property change events during the load process
	 */
	private boolean isLoading;

	/**
	 * Initialize the setPropertyFunction and getPropertyFunction HashMaps that
	 * link property names to setter/getter functions.
	 */
	private void initPropertyFunctionMaps() {

		try
		{
			// Set functions - only include functions that can't be replaced by
			// updateProperty(property,value) instead.
			setPropertyFunction = new HashMap<String, Method>();
			setPropertyFunction.put(ADEMPIERE_HOME, getClass()
					.getDeclaredMethod("setAdempiereHome", String.class));
			setPropertyFunction.put(ADEMPIERE_APPS_DEPLOY, getClass()
					.getDeclaredMethod("setAppsServerDeployDir", String.class));
			setPropertyFunction.put(ADEMPIERE_APPS_SERVER, getClass()
					.getDeclaredMethod("setAppsServer", String.class));
			setPropertyFunction.put(ADEMPIERE_APPS_TYPE, getClass()
					.getDeclaredMethod("setAppsServerType", String.class));
			setPropertyFunction.put(ADEMPIERE_JAVA_TYPE, getClass()
					.getDeclaredMethod("setJavaType", String.class));
			setPropertyFunction.put(ADEMPIERE_DB_TYPE, getClass()
					.getDeclaredMethod("setDatabaseType", String.class));
			setPropertyFunction.put(ADEMPIERE_MAIL_SERVER, getClass()
					.getDeclaredMethod("setMailServer", String.class));
			setPropertyFunction.put(ADEMPIERE_MAIL_PORT, getClass()
					.getDeclaredMethod("setMailPort", String.class));
			setPropertyFunction.put(ADEMPIERE_MAIL_PT, getClass()
					.getDeclaredMethod("setMailProtocol", String.class));
			setPropertyFunction.put(ADEMPIERE_MAIL_ET, getClass()
					.getDeclaredMethod("setEncryptionType", String.class));
			setPropertyFunction.put(ADEMPIERE_MAIL_AM, getClass()
					.getDeclaredMethod("setAuthMechanism", String.class));
			setPropertyFunction.put(DATABASE_DISCOVERED, getClass()
					.getDeclaredMethod("setDatabaseDiscovered", String.class));

			setPropertyFunction.put(JAVA_HOME,
					getClass().getDeclaredMethod("setJavaHome", String.class));

			// Get functions
			getPropertyFunction = new HashMap<String, Method>();
			getPropertyFunction.put(
					ADEMPIERE_DATE_VERSION,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_MAIN_VERSION,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_DB_VERSION,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));
			getPropertyFunction.put(ADEMPIERE_HOME, getClass()
					.getDeclaredMethod("getAdempiereHome"));
			getPropertyFunction.put(ADEMPIERE_ADMIN_EMAIL, getClass()
					.getDeclaredMethod("getAdminEmail"));
			getPropertyFunction.put(ADEMPIERE_APPS_DEPLOY, getClass()
					.getDeclaredMethod("getAppsServerDeployDir"));
			getPropertyFunction.put(ADEMPIERE_APPS_SERVER, getClass()
					.getDeclaredMethod("getAppsServer"));
			getPropertyFunction.put(
					ADEMPIERE_APPS_TYPE,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_FTP_SERVER,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_FTP_USER,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));
			getPropertyFunction.put(ADEMPIERE_KEYSTORE, getClass()
					.getDeclaredMethod("getKeyStore"));
			getPropertyFunction.put(
					ADEMPIERE_KEYSTORECODEALIAS,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_KEYSTOREPASS,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_KEYSTOREWEBALIAS,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_JAVA_TYPE,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_JNP_PORT,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_SSL_PORT,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_WEB_PORT,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_DB_SERVER,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_DB_TYPE,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_DB_NAME,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_DB_PATH,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_DB_PORT,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_DB_SYSTEM_PASS,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_DB_SYSTEM,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_DB_USER,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_DB_PASSWORD,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_MAIL_SERVER,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_MAIL_PORT,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_MAIL_PT,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(ADEMPIERE_ADMIN_EMAIL, getClass()
					.getDeclaredMethod("getAdminEmail"));
			getPropertyFunction.put(
					ADEMPIERE_MAIL_ET,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_MAIL_AM,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_MAIL_UPDATED,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_MAIL_USER,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_MAIL_PASSWORD,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_WEB_ALIAS,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));
			getPropertyFunction.put(DATABASE_DISCOVERED, getClass()
					.getDeclaredMethod("getDatabaseDiscovered"));

			getPropertyFunction.put(JAVA_HOME,
					getClass().getDeclaredMethod("getJavaHome"));
			getPropertyFunction.put(
					ADEMPIERE_JAVA_OPTIONS,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));

			getPropertyFunction.put(
					ADEMPIERE_CERT_CN,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_CERT_ORG,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_CERT_ORG_UNIT,
					getClass().getDeclaredMethod("getValueOrDefaultAllowEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_CERT_LOCATION,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_CERT_STATE,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));
			getPropertyFunction.put(
					ADEMPIERE_CERT_COUNTRY,
					getClass().getDeclaredMethod("getValueOrDefaultNotEmpty",
							String.class));

			// Default values
			propertyDefault = new HashMap<String, String>();
			propertyDefault.put(ADEMPIERE_DATE_VERSION, DEFAULT_DATE_VERSION);
			propertyDefault.put(ADEMPIERE_MAIN_VERSION, DEFAULT_MAIN_VERSION);
			propertyDefault.put(ADEMPIERE_DB_VERSION, DEFAULT_DB_VERSION);
			propertyDefault.put(ADEMPIERE_HOME, DEFAULT_ADEMPIERE_HOME);
			propertyDefault.put(ADEMPIERE_ADMIN_EMAIL, DEFAULT_ADMIN_EMAIL);
			propertyDefault.put(ADEMPIERE_APPS_DEPLOY, DEFAULT_APPS_DEPLOY);
			propertyDefault.put(ADEMPIERE_APPS_SERVER, DEFAULT_APPS_SERVER);
			propertyDefault.put(ADEMPIERE_APPS_TYPE, DEFAULT_APPS_TYPE);
			propertyDefault.put(ADEMPIERE_FTP_PASSWORD, DEFAULT_FTP_PASSWORD);
			propertyDefault.put(ADEMPIERE_FTP_PREFIX, DEFAULT_FTP_PREFIX);
			propertyDefault.put(ADEMPIERE_FTP_SERVER, DEFAULT_FTP_SERVER);
			propertyDefault.put(ADEMPIERE_FTP_USER, DEFAULT_FTP_USER);
			propertyDefault.put(ADEMPIERE_KEYSTORE, DEFAULT_KEYSTORE);
			propertyDefault.put(ADEMPIERE_KEYSTORECODEALIAS,
					DEFAULT_KEYSTORECODEALIAS);
			propertyDefault.put(ADEMPIERE_KEYSTOREPASS, DEFAULT_KEYSTOREPASS);
			propertyDefault.put(ADEMPIERE_KEYSTOREWEBALIAS,
					DEFAULT_KEYSTOREWEBALIAS);
			propertyDefault.put(ADEMPIERE_JAVA_TYPE, DEFAULT_JAVA_TYPE);
			propertyDefault.put(ADEMPIERE_JNP_PORT, DEFAULT_JNP_PORT);
			propertyDefault.put(ADEMPIERE_SSL_PORT, DEFAULT_SSL_PORT);
			propertyDefault.put(ADEMPIERE_WEB_PORT, DEFAULT_WEB_PORT);
			propertyDefault.put(ADEMPIERE_DB_SERVER, DEFAULT_DB_SERVER);
			propertyDefault.put(ADEMPIERE_DB_TYPE, DEFAULT_DB_TYPE);
			propertyDefault.put(ADEMPIERE_DB_NAME, DEFAULT_DB_NAME);
			propertyDefault.put(ADEMPIERE_DB_PATH, DEFAULT_DB_PATH);
			propertyDefault.put(ADEMPIERE_DB_PORT, DEFAULT_DB_PORT);
			propertyDefault.put(ADEMPIERE_DB_SYSTEM_PASS,
					DEFAULT_DB_SYSTEM_PASS);
			propertyDefault.put(ADEMPIERE_DB_SYSTEM, DEFAULT_DB_SYSTEM_PASS);
			propertyDefault.put(ADEMPIERE_DB_USER, DEFAULT_DB_USER);
			propertyDefault.put(ADEMPIERE_DB_PASSWORD, DEFAULT_DB_PASSWORD);
			propertyDefault.put(ADEMPIERE_MAIL_SERVER, DEFAULT_MAIL_SERVER);
			propertyDefault.put(ADEMPIERE_MAIL_PORT, DEFAULT_MAIL_PORT);
			propertyDefault.put(ADEMPIERE_MAIL_PT, DEFAULT_MAIL_PT);
			propertyDefault.put(ADEMPIERE_ADMIN_EMAIL, DEFAULT_ADMIN_EMAIL);
			propertyDefault.put(ADEMPIERE_MAIL_ET, DEFAULT_MAIL_ET);
			propertyDefault.put(ADEMPIERE_MAIL_AM, DEFAULT_MAIL_AM);
			propertyDefault.put(ADEMPIERE_MAIL_UPDATED, DEFAULT_MAIL_UPDATED);
			propertyDefault.put(ADEMPIERE_MAIL_USER, DEFAULT_MAIL_USER);
			propertyDefault.put(ADEMPIERE_MAIL_PASSWORD, DEFAULT_MAIL_PASSWORD);
			propertyDefault.put(ADEMPIERE_WEB_ALIAS, DEFAULT_WEB_ALIAS);

			propertyDefault.put(JAVA_HOME, DEFAULT_JAVA_HOME);
			propertyDefault.put(ADEMPIERE_JAVA_OPTIONS, DEFAULT_JAVA_OPTIONS);

			propertyDefault.put(ADEMPIERE_CERT_CN, DEFAULT_CERT_CN);
			propertyDefault.put(ADEMPIERE_CERT_ORG, DEFAULT_CERT_ORG);
			propertyDefault.put(ADEMPIERE_CERT_ORG_UNIT, DEFAULT_CERT_ORG_UNIT);
			propertyDefault.put(ADEMPIERE_CERT_LOCATION, DEFAULT_CERT_LOCATION);
			propertyDefault.put(ADEMPIERE_CERT_STATE, DEFAULT_CERT_STATE);
			propertyDefault.put(ADEMPIERE_CERT_COUNTRY, DEFAULT_CERT_COUNTRY);

		}
		catch (NoSuchMethodException | SecurityException e)
		{
			log.severe(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Update the property with the value. Returns true if the value was
	 * changed.
	 * 
	 * @param property
	 *            The property to set. The property has to be a valid known
	 *            property name.
	 * @param value
	 *            The value to set.
	 * @return True if the property value was updated. False if the current
	 *         value and the provided value are equal
	 * @throws IllegalArgumentException
	 *             If the property name is not known.
	 */
	private boolean updateProperty(String property, String value)
			throws IllegalArgumentException
	{

		if (value == null)
			value = "";

		// Test that the property is known/allowed.
		if (!KNOWN_PROPERTIES.contains(property))
		{
			throw new IllegalArgumentException("Property " + property
					+ " is not a recognized property."
					+ " See ConfigurationData.java for a full list.");
		}

		String currentValue = (String) p_properties.get(property);

		if (currentValue == null || !currentValue.equals(value))
		{
			log.fine("Property '" + property
					+ "' value changed from current value '" + currentValue
					+ "' to '" + value + "'.");
			p_properties.put(property, value);
			this.pcs.firePropertyChange(property, currentValue, value);

			return true;
		}
		log.fine("Property '" + property
				+ "' value not changed from current value '" + currentValue
				+ "'.");

		return false;
	}

	/**
	 * Get the value of the property. Return the default value if the property
	 * is null (hasn't been set) or the value is empty and the allowEmpty
	 * argument is false.
	 * 
	 * @param property
	 * @param defaultValue
	 * @param allowEmpty
	 * @return
	 */
	private String getValueOrDefault(String property, String defaultValue,
			boolean allowEmpty)
	{

		String value = (String) p_properties.get(property);

		if (value == null || (value.isEmpty() && !allowEmpty))
			value = defaultValue;

		return value;
	}

	/**
	 * Get the value of the property. Return the default value if the property
	 * is null (hasn't been set). Empty strings are valid property values.
	 * 
	 * @param property
	 * @return the property value or default
	 */
	@SuppressWarnings("unused")
	private String getValueOrDefaultAllowEmpty(String property) {

		String defaultValue = propertyDefault.get(property);

		if (defaultValue == null)
			defaultValue = "";

		return getValueOrDefault(property, defaultValue, ALLOW_EMPTY);

	}

	/**
	 * Get the value of the property. Return the default value if the property
	 * is null (hasn't been set) or is an empty strings.
	 * 
	 * @param property
	 * @return the property value or default
	 */
	@SuppressWarnings("unused")
	private String getValueOrDefaultNotEmpty(String property) {

		String defaultValue = propertyDefault.get(property);

		if (defaultValue == null)
			defaultValue = "(default not specified)";

		return getValueOrDefault(property, defaultValue, DONT_ALLOW_EMPTY);

	}

	/**
	 * Set and test the property by calling setProperty(getProperty()) to ensure
	 * a valid (default) value is set.
	 */
	private String setTestProperty(String property) {

		return setProperty(property, getProperty(property));
	}

	/**
	 * Load Configuration Data from the AdempipiereEnv.properties file, if it
	 * exists. If the file does not exist, try the
	 * AdempiereEnvTemplate.properties file. Finally, fall back to hard coded
	 * defaults.
	 * 
	 * @return true if the properties were successfully loaded
	 */
	public boolean load() {

		isLoading = true;

		// Load C:\Adempiere\AdempiereEnv.properties
		boolean envLoaded = false;

		Properties loaded = new Properties();

		try
		{
			localhost = InetAddress.getLocalHost();
		}
		catch (UnknownHostException e1)
		{
		}

		// Load the environment file with the properties. The call to
		// getAdempiereHome()
		// will set defaults if the property for ADEMPIERE_HOME has not been
		// set.
		String fileName = getAdempiereHome() + File.separator
				+ ADEMPIERE_ENV_FILE;
		File env = new File(fileName);

		if (!env.exists())
		{
			fileName = getAdempiereHome() + File.separator
					+ ADEMPIERE_ENV_TEMPLATE_FILE;
			env = new File(fileName);
		}

		if (env.exists())
		{
			try
			{
				FileInputStream fis = new FileInputStream(env);
				loaded.load(fis);
				fis.close();
			}
			catch (Exception e)
			{
				log.warning(e.toString());
			}

			log.info(env.toString());

			if (loaded.size() > 5)
				envLoaded = true;

			// Check that the loaded properties are correct. Ignore any
			// that are unrecognized.

			Enumeration<?> propertyNames = loaded.propertyNames();

			while (propertyNames.hasMoreElements())
			{
				String propertyName = (String) propertyNames.nextElement();
				if (!KNOWN_PROPERTIES.contains(propertyName))
				{
					log.warning(fileName + " contains unknown property '"
							+ propertyName + "'. Ignored");
				}
				else
				{
					String value = (String) loaded.get(propertyName);
					log.info("Loading property " + propertyName
							+ " with value '" + value + "'.");
					try
					{
						setProperty(propertyName, value);
					}
					catch (IllegalArgumentException e)
					{
						log.severe("Unable to load property " + propertyName
								+ " with value '" + value + "'.\n"
								+ e.getMessage());
					}
				}
			}

			// Checks of validity of properties
			for (String property : KNOWN_PROPERTIES)
			{
				// Check for missing defaults and test the values
				setProperty(property, getProperty(property));
			}

			// Non-property values

			// Update the list of Discovered Database according to the Database
			// Type
			setDatabaseDiscoveredList();

			// Try to select from the Discovered List, preserving the name.
			setDatabaseDiscovered(getDatabaseDiscovered());

			// Overwrite web alias with the local host
			// TODO - why?
			// if (localhost != null)
			// setWebAlias(localhost.getCanonicalHostName());

		}

		// No environment file found - defaults
		// envLoaded = false;
		if (!envLoaded)
		{
			log.info("Defaults");

			// set defaults, check validity and add to the properties
			for (String property : KNOWN_PROPERTIES)
			{
				// Check for missing defaults and test the values
				setProperty(property, getProperty(property));
			}

			// Initialize/check the configurations
			// Java
			initJava();
			// AppsServer
			initAppsServer();
			// Database Server
			initDatabase();

		} // !envLoaded

		isLoading = false;

		return true;

	} // load

	/**
	 * Init Java parameters after a change in the java type
	 */
	private void initJava() {

		int index = setJavaType(getProperty(ADEMPIERE_JAVA_TYPE));

		if (index < 0 || index >= JAVATYPE.length)
		{
			log.warning("JavaType Index invalid: " + index);
		}
		else if (m_javaConfig[index] == null)
		{
			log.warning("JavaType Config missing: " + JAVATYPE[index]);
		}
		else
			m_javaConfig[index].init();
	}

	/**
	 * Test Java
	 *
	 * @return error message or null of OK
	 */
	private boolean testJava() {

		String error = "";

		int index = 0;

		try
		{
			index = getJavaTypeIndex(setTestProperty(ADEMPIERE_JAVA_TYPE));
		}
		catch (IllegalArgumentException e)
		{
			error = e.getMessage();
			log.severe(error);
			setTestError(e.getMessage());
		}

		// Common tests
		// Java Home
		File javaHome = new File(getJavaHome());
		if (!javaHome.exists())
		{
			error = "Not found: Java Home";
		}

		// Java Version
		else if (!Login.isJavaOK(false)) // Java Version - based on the Login
		{
			error = "Java Version " + System.getProperty("java.version")
					+ Login.JAVA_VERSION_ERROR;
		}

		// Configuration defined
		else if (m_javaConfig[index] == null)
		{
			error = "JavaType Config class missing: " + index;
		}

		// Configuration specific tests
		else
		{
			error = m_javaConfig[index].test();
		}

		if (error == null)
			error = "";

		setTestError(TEST_JAVA_HOME, error.isEmpty(), false, error);

		// Set Java Home
		if (error.isEmpty())
		{
			System.setProperty(ConfigurationData.JAVA_HOME,
					javaHome.getAbsolutePath());
			return true;
		}

		return false;

	} // testJava

	/**
	 * Init Apps Server
	 */
	public void initAppsServer() {

		// initialize and verify the server name
		try
		{
			setAppsServer(getAppsServer());
		}
		catch (IllegalArgumentException | UnknownHostException e)
		{
			log.severe("Can't set apps server name: " + e.getMessage());
		}

		// Set server type defaults.

		int index = getAppsServerTypeIndex(setTestProperty(ADEMPIERE_APPS_TYPE));

		if (index < 0 || index >= APPSTYPE.length)
		{
			log.warning("AppsServerType Index invalid: " + index);
		}
		else if (m_appsConfig[index] == null)
		{
			log.warning("AppsServerType Config missing: " + APPSTYPE[index]);
		}
		else
			m_appsConfig[index].init();
	}

	/**
	 * Init Database
	 * 
	 * @param selected
	 *            DB
	 */
	private void initDatabase() {

		log.finest("");
		int index = getDatabaseTypeIndex(getProperty(ADEMPIERE_DB_TYPE));

		if (index < 0 || index >= DBTYPE.length)
		{
			log.warning("DatabaseType Index invalid: " + index);
		}
		else if (m_databaseConfig[index] == null)
		{
			log.warning("DatabaseType Config missing: " + DBTYPE[index]);
		}
		else
		{
			// Set specific defaults for the selecte database type.
			m_databaseConfig[index].init();
			// Set generic defaults for the other database fields.
			setTestProperty(ADEMPIERE_DB_SERVER);
			setTestProperty(ADEMPIERE_DB_SYSTEM_PASS);
			setTestProperty(ADEMPIERE_DB_USER);
			setTestProperty(ADEMPIERE_DB_PASSWORD);
		}

	}

	/**
	 * Test Database
	 *
	 * @return error message or null of OK
	 */
	private boolean testDatabase() {

		String error = null;

		int index = getDatabaseTypeIndex(getProperty(ADEMPIERE_DB_TYPE));
		if (index < 0 || index >= DBTYPE.length)
			error = "DatabaseType Index invalid: " + index;
		else if (m_databaseConfig[index] == null)
			error = "DatabaseType Config class missing: " + index;

		if (error == null)
			error = m_databaseConfig[index].test();

		if (error != null)
		{
			return false;
		}

		return true;
	} // testDatabase

	/**************************************************************************
	 * Test the settings.  Errors can be received through the Property Change Events
	 * which will include new values that implement the {@link ErrorData} interface.
	 *
	 * @return true if test ok
	 */
	public boolean test() {

		try
		{

			boolean pass = testJava() && testAdempiere() && testAppsServer()
					&& testDatabase();

			if (pass)
			{
				// Optional - doen't prevent operations
				testMail();
			}

			return pass;

		}
		catch (Exception e)
		{
			// setTestError(e.getMessage());
			log.severe(e.getMessage());
			e.printStackTrace();
			return false;

		}

	} // test

	/**
	 * Test Adempiere and the KeyStore. Throws errors if there are any problems.
	 *
	 * @return null if OK
	 * @throws IllegalArgumentException
	 * @throws FileNotFoundException
	 * @throws AdempiereException
	 *             if the KeyStore can't be verified.
	 */
	private boolean testAdempiere() throws FileNotFoundException,
			IllegalArgumentException
	{

		// Adempiere Home
		setAdempiereHome(getAdempiereHome()); // Throws exception if there is a
		// problem.
		setTestError(TEST_ADEMPIERE_HOME, true, true, "");
		log.fine("OK: AdempiereHome = " + getAdempiereHome());

		String error = null;
		String fileName = getKeyStore();
		String pw = getProperty(ADEMPIERE_KEYSTOREPASS);

		// Load the keystore data.  If the keystore doesn't exist, this
		// data will be used to create it.  If it does exist, this data 
		// will be ignored.
		KeyStoreMgt ks = new KeyStoreMgt(fileName, pw.toCharArray());
		ks.setCommonName(getProperty(ADEMPIERE_CERT_CN));
		ks.setOrganization(getProperty(ADEMPIERE_CERT_ORG));
		ks.setOrganizationUnit(getProperty(ADEMPIERE_CERT_ORG_UNIT));
		ks.setLocation(getProperty(ADEMPIERE_CERT_LOCATION));
		ks.setState(getProperty(ADEMPIERE_CERT_STATE));
		ks.setCountry(getProperty(ADEMPIERE_CERT_COUNTRY));

		// Test the keystore and create it if it doesn't exist.
		error = ks.verify();

		setTestError(TEST_KEYSTORE_PASS, (error == null), true, error);

		if (error != null)
		{
			return false;
		}

		log.fine("OK: KeyStore = " + fileName);
		return true;
	} // testAdempiere

	/**
	 * Test the application server.
	 * @return true is successful, false if there was a failure.
	 * @throws IllegalArgumentException
	 * @throws UnknownHostException
	 */
	private boolean testAppsServer() throws IllegalArgumentException,
			UnknownHostException
	{

		// Test and throw error
		setAppsServer(getAppsServer());

		// Pass the test
		setTestError(TEST_APPS_SERVER, true, false, "");

		String error = m_appsConfig[getAppsServerTypeIndex(getProperty(ADEMPIERE_APPS_TYPE))]
				.test();
		
		if (error != null)
			return false;

		return true;
	}

	/**************************************************************************
	 * Test (optional) Mail
	 *
	 * @return error message or null, if OK
	 * @throws UnknownHostException
	 * @throws IllegalArgumentException
	 * @throws NumberFormatException
	 *             - if the mail port strings are not parsable as integers.
	 * @throws AddressException
	 */
	private boolean testMail() throws IllegalArgumentException,
			UnknownHostException, NumberFormatException, AddressException
	{

		// Mail Server
		String server = "";

		// if (server == null || server.isEmpty())
		// return true; // Always pass

		InetAddress mailServer = null;
		Boolean pass = true;

		// Throws error
		server = setTestProperty(ADEMPIERE_MAIL_SERVER); // Tests and sets the
		// server
		mailServer = InetAddress.getByName(server);

		// FR [ 402 ]
		// Mail Port
		String mailPort = getProperty(ADEMPIERE_MAIL_PORT);
		int port = 25;
		if (mailPort != null)
		{
			port = Integer.parseInt(mailPort);
		}

		// Mail Protocol
		String mailProtocol = getProperty(ADEMPIERE_MAIL_PT);
		// Mail Encryption Type
		String mailEncryptionType = getProperty(ADEMPIERE_MAIL_ET);
		// Mail Authentication Mechanism
		String mailAuthMechanism = getProperty(ADEMPIERE_MAIL_AM);
		// Mail User
		String mailUser = getProperty(ADEMPIERE_MAIL_USER);
		String mailPassword = getProperty(ADEMPIERE_MAIL_PASSWORD);
		// log.config("Mail User = " + mailUser + "/" + mailPassword);

		// Mail Address
		String adminEMailString = getProperty(ADEMPIERE_ADMIN_EMAIL);
		;
		InternetAddress adminEMail = null;

		// Throws error
		adminEMail = new InternetAddress(adminEMailString);
		//
		pass = testMailServer(mailServer, adminEMail, mailUser, mailPassword,
				port, mailProtocol, mailEncryptionType, mailAuthMechanism);

		if (pass)
		{
			// No need to set properties after the test
			// The following property seems to indicate that 
			// no changes were made - but it also indicates that 
			// the test was successful.
			setProperty(ADEMPIERE_MAIL_UPDATED, "No");
		}
		else
		{
			// No need to reset properties after a failed test
			// Doing so means the user will have to reenter the values
			// before trying the test again.
			setProperty(ADEMPIERE_MAIL_UPDATED, "");
		}

		return pass;
	} // testMail

	/**
	 * Test Mail
	 * 
	 * @param mailServer
	 *            mail server
	 * @param adminEMail
	 *            email of admin
	 * @param mailUser
	 *            user ID
	 * @param mailPassword
	 *            password
	 * @param port
	 * @param protocol
	 * @param encryptionType
	 * @param authMechanism
	 * @return true of OK
	 */
	private boolean testMailServer(InetAddress mailServer,
			InternetAddress adminEMail, String mailUser, String mailPassword,
			int port, String protocol, String encryptionType,
			String authMechanism)
	{

		boolean serverOK = false;

		// Change Protocol
		if (protocol == null || protocol.isEmpty())
		{
			throw new IllegalArgumentException("EMail Protocol can not be null");
		}
		else
		{
			if (protocol.length() > 1)
			{
				protocol = protocol.substring(0, 1);
			}
		}
		// Change Encryption Type
		if (encryptionType == null)
		{
			encryptionType = MEMailConfig.ENCRYPTIONTYPE_None;
		}
		else
		{
			if (encryptionType.length() > 1)
			{
				encryptionType = encryptionType.substring(0, 1);
			}
		}
		// Change Authentication Mechanism
		if (authMechanism == null)
		{
			authMechanism = MEMailConfig.AUTHMECHANISM_Login;
		}
		else
		{
			if (authMechanism.length() > 1)
			{
				authMechanism = authMechanism.substring(0, 1);
			}
		}

		// Removed smtp/imap tests as they were identical.  There is only 
		// the single test below to test the server/port.
		String error = "Server not available or not active.";
		boolean pass = false;
		if (Config.testPort(mailServer, port, true))
		{
			log.fine("OK: Server contacted");
			serverOK = true;
			pass = true;
		}
		else
		{
			log.warning(error);
			pass = false;
			serverOK = false;
		}

		setTestError(TEST_EMAIL_PORT, pass, false, error);

		//
		if (!serverOK)
		{
			return false;
		}
		//
		error = "Could NOT send Email to " + adminEMail;
		pass = false;
		try
		{
			// FR [ 402 ]
			// Add support to send mail without context
			EMail email = new EMail(mailServer.getHostName(), port, protocol,
					encryptionType, authMechanism, adminEMail.toString(),
					adminEMail.toString(), "Adempiere Server Setup Test",
					"Test: " + getProperties(), false);
			email.createAuthenticator(mailUser, mailPassword);
			if (EMail.SENT_OK.equals(email.send()))
			{
				pass = true;
				log.info("OK: Send Test Email to " + adminEMail);
			}
			else
			{
				pass = false;
				log.warning(error);
			}
		}
		catch (Exception ex)
		{
			log.severe(ex.getLocalizedMessage());
			error += " " + ex.getLocalizedMessage();
		}

		setTestError(TEST_EMAIL_PASS, pass, false, error);
		//
		if (!pass)
			return false;
		//

		return true;
	} // testMailServer

	/**************************************************************************
	 * Save Settings
	 *
	 * @return true if saved
	 * @throws IOException
	 */
	public boolean save() throws IOException {

		// Create Connection

		// For Oracle and Oracle XE
		String ccType = Database.DB_ORACLE;
		// For others
		if (!getProperty(ConfigurationData.ADEMPIERE_DB_TYPE).startsWith(
				Database.DB_ORACLE))
		{
			ccType = getProperty(ConfigurationData.ADEMPIERE_DB_TYPE);
		}
		CConnection cc = null;
		try
		{
			cc = CConnection.get(ccType,
					getProperty(ConfigurationData.ADEMPIERE_DB_SERVER),
					getPropertyAsInt(ConfigurationData.ADEMPIERE_DB_PORT),
					getProperty(ConfigurationData.ADEMPIERE_DB_NAME),
					getProperty(ConfigurationData.ADEMPIERE_DB_USER),
					getProperty(ConfigurationData.ADEMPIERE_DB_PASSWORD));
			cc.setAppsHost(getAppsServer());
			cc.setAppsPort(getPropertyAsInt(ConfigurationData.ADEMPIERE_JNP_PORT));
			// cc.setConnectionProfile(CConnection.PROFILE_LAN); // Deprecated -
			// not used
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "connection", e);
			return false;
		}

		p_properties.setProperty(Ini.P_CONNECTION,
				SecureEngine.encrypt(cc.toStringLong()));

		log.finest(p_properties.toString());

		// Before we save, load Ini - shows license dialog and creates the
		// property file.
		Ini.setClient(false);
		String fileName = m_adempiereHome.getAbsolutePath() + File.separator
				+ Ini.ADEMPIERE_PROPERTY_FILE;
		Ini.loadProperties(fileName);

		// Save Environment
		fileName = m_adempiereHome.getAbsolutePath() + File.separator
				+ ADEMPIERE_ENV_FILE;
		try
		{
			FileOutputStream fos = new FileOutputStream(new File(fileName));
			p_properties.store(fos, ADEMPIERE_ENV_FILE);
			fos.flush();
			fos.close();
		}
		catch (IOException e)
		{
			log.severe("Cannot save Properties to " + fileName + " - "
					+ e.toString());
			throw e;
		}

		log.info(fileName);
		return saveIni();
	} // save

	/**
	 * Synchronize and save Connection Info in Ini
	 * 
	 * @return true
	 */
	private boolean saveIni() {

		Ini.setAdempiereHome(m_adempiereHome.getAbsolutePath()); // Sets the
		// system
		// env
		// variable.
		// Create Connection
		String url = null;
		try
		{
			// For Oracle and Oracle XE
			String ccType = Database.DB_ORACLE;
			// For others  - changed the equals to startsWith to match both Oracle and OracleXE
			// There is no value for OracleXE in the Database class
			if (!getProperty(ConfigurationData.ADEMPIERE_DB_TYPE).startsWith(
					Database.DB_ORACLE))
			{
				ccType = getProperty(ConfigurationData.ADEMPIERE_DB_TYPE);
			}
			//
			CConnection cc = CConnection.get(ccType,
					getProperty(ConfigurationData.ADEMPIERE_DB_SERVER),
					getPropertyAsInt(ConfigurationData.ADEMPIERE_DB_PORT),
					getProperty(ConfigurationData.ADEMPIERE_DB_NAME),
					getProperty(ConfigurationData.ADEMPIERE_DB_USER),
					getProperty(ConfigurationData.ADEMPIERE_DB_PASSWORD));

			cc.setAppsHost(getProperty(ConfigurationData.ADEMPIERE_APPS_SERVER));
			cc.setAppsPort(getPropertyAsInt(ConfigurationData.ADEMPIERE_JNP_PORT));
			// cc.setConnectionProfile(CConnection.PROFILE_LAN); // Deprecated -
			// no longer used.
			url = cc.toStringLong();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "connection", e);
			return false;
		}
		if (url == null)
		{
			log.warning("No Connection");
			return false;
		}
		Ini.setProperty(Ini.P_CONNECTION, url);
		Ini.saveProperties(false);
		return true;
	} // saveIni

	/**
	 * Get Properties
	 *
	 * @return properties
	 */
	private Properties getProperties() {

		return p_properties;
	} // getProperties

	/**
	 * Get Adempiere Home
	 *
	 * @return adempiere home
	 */
	private String getAdempiereHome() {

		String adempiereHome = (String) p_properties.get(ADEMPIERE_HOME);
		if (adempiereHome == null || adempiereHome.isEmpty())
		{
			adempiereHome = System.getProperty(ADEMPIERE_HOME);
			if (adempiereHome == null || adempiereHome.isEmpty())
				adempiereHome = System.getProperty("user.dir");

			try
			{
				setAdempiereHome(adempiereHome);
			}
			catch (FileNotFoundException | IllegalArgumentException e)
			{

				log.severe(e.getMessage());
			}
		}
		return adempiereHome;
	} // getAdempiereHome

	/**
	 * Set Adempiere Home (ADEMPIERE_HOME property). Does not set the system
	 * environment variable ADEMPIERE_HOME.
	 *
	 * @param adempiereHome
	 *            - typically "c:\adempiere"
	 */
	private void setAdempiereHome(String adempiereHome)
			throws FileNotFoundException, IllegalArgumentException
	{

		String error = "";
		if (adempiereHome == null)
		{
			error = "ADEMPIERE_HOME can not be null.";
			setTestError(TEST_ADEMPIERE_HOME, false, true, error);
			throw new IllegalArgumentException(error);
		}

		if (adempiereHome.isEmpty())
		{
			error = "ADEMPIERE_HOME can not be empty string.";
			setTestError(TEST_ADEMPIERE_HOME, false, true, error);
			throw new IllegalArgumentException(error);
		}
		m_adempiereHome = new File(adempiereHome);
		if (!m_adempiereHome.exists() && !m_adempiereHome.isDirectory())
		{
			error = "ADEMPIERE_HOME not found or not a directory: "
					+ adempiereHome;
			setTestError(TEST_ADEMPIERE_HOME, false, true, error);
			throw new FileNotFoundException(error);
		}

		updateProperty(ADEMPIERE_HOME, m_adempiereHome.getAbsolutePath());

		setTestError(TEST_ADEMPIERE_HOME, true, true, "");

	} // setAdempiereHome

	/**
	 * Get Key Store (ADEMPIERE_KEYSTORE property). Defaults to the settings in
	 * {@link org.adempiere.configuration.KeyStoreMgt#getKeystoreFileName(String)}
	 * based on the location of ADEMPIERE_HOME
	 *
	 * @return Key Store filename
	 */
	private String getKeyStore() {

		String value = (String) p_properties.getProperty(ADEMPIERE_KEYSTORE);

		if (value == null || value.isEmpty())
		{
			value = KeyStoreMgt.getKeystoreFileName(getAdempiereHome());

		}

		return value;

	} // getKeyStorePass

	/**
	 * Set Java Type
	 *
	 * @param javaType
	 *            The javaType to set.
	 */
	public int setJavaType(String javaType) throws IllegalArgumentException {

		int index = -1;
		for (int i = 0; i < JAVATYPE.length; i++)
		{
			if (JAVATYPE[i].equals(javaType))
			{
				index = i;
				break;
			}
		}
		if (index == -1)
		{
			throw new IllegalArgumentException("Invalid JavaType=" + javaType);
		}

		updateProperty(ADEMPIERE_JAVA_TYPE, JAVATYPE[index]);

		return index;
	} // setJavaType

	/**
	 * @return Returns the javaHome.
	 */
	public String getJavaHome() {

		String javaHome = (String) p_properties.get(JAVA_HOME);

		if (javaHome == null || javaHome.isEmpty())
		{
			javaHome = System.getProperty(JAVA_HOME); // Not java.home which is
			// the JRE directory
		}

		if (javaHome == null || javaHome.isEmpty())
		{
			// Java Home, e.g. D:\j2sdk1.4.1\jre
			javaHome = System.getProperty("java.home"); // Returns the JRE
			// directory
			log.fine(javaHome);
			if (javaHome.endsWith("jre"))
				javaHome = javaHome.substring(0, javaHome.length() - 4);
		}

		return javaHome;
	}

	/**
	 * Set the JAVA_HOME property.
	 * 
	 * @param javaHome
	 *            The javaHome to set.
	 * @return The value set.
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings("unused")
	private String setJavaHome(String javaHome) throws FileNotFoundException {

		String error = "";

		// If loading and the value is not provided, try to find it.
		if (isLoading && (javaHome == null || javaHome.isEmpty()))
			javaHome = getJavaHome();

		if (javaHome == null)
		{
			error = "JAVA_HOME can not be null.";
			setTestError(TEST_JAVA_HOME, false, true, error);
			throw new IllegalArgumentException(error);
		}

		if (javaHome.isEmpty())
		{
			error = "JAVA_HOME can not be empty string.";
			setTestError(TEST_JAVA_HOME, false, true, error);
			throw new IllegalArgumentException(error);
		}

		File file = new File(javaHome);
		if (!file.exists())
		{
			error = "JAVA_HOME not found: " + javaHome;
			setTestError(TEST_JAVA_HOME, false, true, error);
			throw new FileNotFoundException(error);
		}

		updateProperty(JAVA_HOME, file.getAbsolutePath());

		setTestError(TEST_JAVA_HOME, true, true, "");

		return javaHome;
	}

	/**
	 * Get the index of the given Application Server from the APPSTYPE array.
	 * Throws an IllegalArgumentException if the appsType argument does not
	 * match one of the entries in the APPSTYPE array. The test is case
	 * insensitive.
	 * 
	 * @param appsType
	 *            - one of the entries in the APPSTYPE array.
	 * @return The index.
	 */
	private int getJavaTypeIndex(String javaType) {

		int index = -1;
		for (int i = 0; i < JAVATYPE.length; i++)
		{
			if (JAVATYPE[i].equalsIgnoreCase(javaType))
			{
				index = i;
				break;
			}
		}
		if (index == -1)
		{
			throw new IllegalArgumentException("Invalid Java type: " + javaType);
		}

		return index;
	}

	/**
	 * Set the ADEMPIERE_APPS_TYPE property. No tests performed.
	 *
	 * @param appsType
	 *            The appsType to set.
	 */
	@SuppressWarnings("unused")
	private String setAppsServerType(String appsType) {

		int index = getAppsServerTypeIndex(appsType);
		updateProperty(ADEMPIERE_APPS_TYPE, APPSTYPE[index]);

		return APPSTYPE[index];
	} // setAppsServerType

	/**
	 * Get the index of the given Application Server from the APPSTYPE array.
	 * Throws an IllegalArgumentException if the appsType argument does not
	 * match one of the entries in the APPSTYPE array. The test is case
	 * insensitive.
	 * 
	 * @param appsType
	 *            - one of the entries in the APPSTYPE array.
	 * @return The index.
	 */
	private int getAppsServerTypeIndex(String appsType) {

		int index = -1;
		for (int i = 0; i < APPSTYPE.length; i++)
		{
			if (APPSTYPE[i].equalsIgnoreCase(appsType)) // Ignore case to
			// catch/change older
			// versions which used
			// lower case names.
			{
				index = i;
				break;
			}
		}
		if (index == -1)
		{
			throw new IllegalArgumentException(
					"Invalid application server type: " + appsType);
		}

		return index;
	}

	/**
	 * Get {@link #ADEMPIERE_APPS_SERVER} (Application Server Name) property. If
	 * the property has not been set or is an empty string, the
	 * {@link java.net.NetworkInterface#getNetworkInterfaces()
	 * getNetworkInterfaces} method is used to search for active, non-loopback
	 * interfaces. The canonical host name of the first
	 * {@link java.net.InetAddress} of the first found interface is used.
	 * 
	 * @return Returns the ADEMPIERE_APPS_SERVER property.
	 */
	private String getAppsServer() {

		String server = (String) p_properties.get(ADEMPIERE_APPS_SERVER);

		if (server == null || server.isEmpty())
		{
			// Set the host name defaults
			server = "unknown";
			try
			{
				// localhost = InetAddress.getLocalHost();
				// server = localhost.getHostName();

				// Find the first active IP address that isn't the loopback
				// address
				Enumeration<NetworkInterface> interfaces = NetworkInterface
						.getNetworkInterfaces();
				while (interfaces.hasMoreElements())
				{
					NetworkInterface iface = interfaces.nextElement();
					// filters out 127.0.0.1 and inactive interfaces
					if (iface.isLoopback() || !iface.isUp())
						continue;

					Enumeration<InetAddress> addresses = iface
							.getInetAddresses();
					while (addresses.hasMoreElements())
					{
						InetAddress addr = addresses.nextElement();
						server = addr.getCanonicalHostName();
						log.fine(iface.getDisplayName() + " " + server);
						break;
					}
				}

			}
			catch (Exception e)
			{
				log.severe("Unable to determine server host name.");
			}
		}

		return server;
	}

	/**
	 * Set the application server name (ADEMPIERE_APPS_SERVER property) . The
	 * server name can't refer to the loopback address "localhost" or
	 * "127.0.0.1". The server, if identified as an IP address, will be
	 * converted to its name.
	 * 
	 * @param server
	 *            The application server to set.
	 * @throws IllegalArgumentException
	 *             if the server argument is null or refers to the loopback
	 *             address.
	 * @throws UnknownHostException
	 *             if the server name/IP address can not be found.
	 */
	private void setAppsServer(String server) throws IllegalArgumentException,
			UnknownHostException
	{

		if (server == null)
			throw new IllegalArgumentException("Server argument is null.");

		InetAddress appsServer = null;

		appsServer = InetAddress.getByName(server); // Throws
		// UnknownHostException

		if (appsServer.getHostName().toLowerCase().indexOf("localhost") != -1
				|| appsServer.getHostName().indexOf("127.0.0.1") != -1)
		{
			if (isLoading)
			{
				// Ignore the error - try to identify the correct server
				// Don't test
				updateProperty(ADEMPIERE_APPS_SERVER, getAppsServer());
				return;
			}
			String error = "Server argument can't refer to localhost or address 127.0.0.1: "
					+ server;
			setTestError(TEST_APPS_SERVER, false, true, error);
			throw new IllegalArgumentException(error);
		}

		updateProperty(ADEMPIERE_APPS_SERVER, appsServer.getHostName());

		setTestError(ConfigurationData.TEST_APPS_SERVER, true, true, "");

	}

	/**
	 * Get the Application Server Deployment Directory (ADEMPIERE_APPS_DEPLOY
	 * property)
	 * 
	 * @return Returns the ADEMPIERE_APPS_DEPLOY property value.
	 */
	private String getAppsServerDeployDir() {

		String deployDir = (String) p_properties.get(ADEMPIERE_APPS_DEPLOY);

		if ((deployDir == null || deployDir.isEmpty())
				&& this.isUsed(ADEMPIERE_APPS_DEPLOY))
		{
			// Get the default
			deployDir = m_appsConfig[getAppsServerTypeIndex(getProperty(ADEMPIERE_APPS_TYPE))]
					.getDeployDir();
		}

		return deployDir;
	}

	/**
	 * Set the Application Server Deployment Directory (ADEMPIERE_APPS_DEPLOY
	 * property). Test that the directory exists.
	 * 
	 * @param appsServerDeployDir
	 *            The Application Server Deployment Directory to set.
	 */
	@SuppressWarnings("unused")
	private void setAppsServerDeployDir(String appsServerDeployDir) {

		if (isUsed(ADEMPIERE_APPS_DEPLOY))
		{
			if (appsServerDeployDir == null || appsServerDeployDir.isEmpty())
			{
				if (isLoading)
				{
					// Ignore the error and set the default
					appsServerDeployDir = getAppsServerDeployDir();
				}
				else
				{
					throw new IllegalArgumentException(
							"Argument can not be null or an empty string.");
				}
			}

			File deploy = new File(appsServerDeployDir);
			boolean pass = deploy.exists() && deploy.isDirectory();
			String error = "Not found or not a directory: " + deploy;

			setTestError(ConfigurationData.TEST_DEPLOYMENT_DIR, pass, true,
					error);
		}
		updateProperty(ADEMPIERE_APPS_DEPLOY, appsServerDeployDir);
	}

	/**
	 * Set Database Type
	 *
	 * @param databaseType
	 *            The databaseType to set.
	 */
	@SuppressWarnings("unused")
	private int setDatabaseType(String databaseType) {

		int index = getDatabaseTypeIndex(databaseType);
		updateProperty(ADEMPIERE_DB_TYPE, DBTYPE[index]);

		return index;
	} // setDatabaseType

	/**
	 * Get the index of the Database Type
	 * @param databaseType
	 * 			The database type string
	 * @return the index of the databaseType in the DBTYPE array.  0 if not found.
	 */
	private int getDatabaseTypeIndex(String databaseType) {

		int index = -1;
		for (int i = 0; i < DBTYPE.length; i++)
		{
			if (DBTYPE[i].equalsIgnoreCase(databaseType))
			{
				databaseType = DBTYPE[i];
				index = i;
				break;
			}
		}
		if (index == -1)
		{
			index = 0;
			log.warning("Invalid DatabaseType=" + databaseType);
		}

		return index;
	}

	/**
	 * Set Encryption Type
	 * 
	 * @param encryptionType
	 * @return
	 */
	@SuppressWarnings("unused")
	private int setEncryptionType(String encryptionType) {

		int index = -1;
		for (int i = 0; i < ENCRYPTIONTYPE.length; i++)
		{
			if (ENCRYPTIONTYPE[i].equals(encryptionType))
			{
				index = i;
				break;
			}
		}
		if (index == -1)
		{
			index = 0;
			log.warning("Invalid EncryptionType=" + encryptionType);
		}

		updateProperty(ADEMPIERE_MAIL_ET, encryptionType);

		return index;
	} // setDatabaseType

	/**
	 * Set Protocol
	 * 
	 * @param protocol
	 * @return
	 */
	@SuppressWarnings("unused")
	private int setMailProtocol(String protocol) {

		if (protocol == null)
			protocol = "";

		int index = -1;

		if (!protocol.isEmpty())
		{
			for (int i = 0; i < EMAIL_PROTOCOL.length; i++)
			{
				if (EMAIL_PROTOCOL[i].equals(protocol))
				{
					index = i;
					break;
				}
			}
			if (index == -1)
			{
				index = 0;
				log.warning("Invalid Protocol=" + protocol);
			}
		}

		updateProperty(ADEMPIERE_MAIL_PT, protocol);

		return index;
	} // setDatabaseType

	/**
	 * Set Authentication Mechanism
	 * 
	 * @param authMechanism
	 * @return
	 */
	@SuppressWarnings("unused")
	private int setAuthMechanism(String authMechanism) {

		int index = -1;
		for (int i = 0; i < AUTHMECHANISMS.length; i++)
		{
			if (AUTHMECHANISMS[i].equals(authMechanism))
			{
				index = i;
				break;
			}
		}
		if (index == -1)
		{
			String error = "Invalid AuthenticationMechanism=" + authMechanism;
			log.warning(error);
			throw new IllegalArgumentException(error);
		}

		updateProperty(ADEMPIERE_MAIL_AM, authMechanism);

		return index;
	} // setDatabaseType

	/**
	 * The database type may provide a list of databases each with a specific
	 * name. The list is "discovered" by the database configuration and the name
	 * of the selected database is used in the property.
	 * 
	 * @return Returns the database Discovered.
	 */
	public String getDatabaseDiscovered() {

		return m_discoveredDatabase;
	}

	/**
	 * Set the database discovered to a value. The value should be from a list
	 * of discovered databases.
	 * 
	 * @param databaseDiscovered
	 *            The database Discovered to set.
	 */
	private void setDatabaseDiscovered(String databaseDiscovered) {

		log.finest("");
		String oldValue = m_discoveredDatabase;
		m_discoveredDatabase = databaseDiscovered;
		this.pcs.firePropertyChange(DATABASE_DISCOVERED, oldValue,
				databaseDiscovered);
	}

	/**
	 * Get the array of discovered databases.
	 */
	public String[] getDatabaseDiscoveredList() {

		return m_discoveredDatabases;

	}

	/**
	 * Set the array of discovered databases.  Calls the dabase configuration classes
	 * to find the information.
	 */
	public void setDatabaseDiscoveredList() {

		log.finest("");

		String[] oldList = m_discoveredDatabases;

		int index = getDatabaseTypeIndex(getProperty(ADEMPIERE_DB_TYPE));

		if (m_databaseConfig[index] == null)
		{
			log.warning("DatabaseType Config missing: " + DBTYPE[index]
					+ ". Discovered Database List not set.");
			return;
		}
		else
		{
			m_discoveredDatabases = m_databaseConfig[index]
					.discoverDatabases("");
		}

		log.fine("Discovered Database List = " + m_discoveredDatabases
				+ " <- (" + oldList + ")");
		this.pcs.firePropertyChange(DISCOVERED_DATABASE_LIST, oldList,
				m_discoveredDatabases);

	}

	/**
	 * @return Returns the databaseName.
	 */
	@SuppressWarnings("unused")
	private String getDatabaseName() {

		return getValueOrDefault(ADEMPIERE_DB_NAME, DEFAULT_DB_NAME,
				!isUsed(ADEMPIERE_DB_NAME));

	}

	/**
	 * @return Returns the databasePort.
	 */
	@SuppressWarnings("unused")
	private String getDatabasePort() {

		return getValueOrDefault(ADEMPIERE_DB_PORT, DEFAULT_DB_PORT,
				!isUsed(ADEMPIERE_DB_PORT));

	} // getDatabasePort

	/**
	 * Sets the mail server name.
	 */
	@SuppressWarnings("unused")
	private String setMailServer(String mailServer)
			throws IllegalArgumentException, UnknownHostException
	{

		// Allow null or empty strings
		if (mailServer.toLowerCase().indexOf("localhost") != -1
				|| mailServer.indexOf("127.0.0.1") != -1)
		{
			if (isLoading)
			{
				// Ignore the error and use the apps server name
				mailServer = getAppsServer();
			}
			else
			{
				throw new IllegalArgumentException(
						"Mail Server argument can't refer to local host or address 127.0.0.1 "
								+ mailServer);
			}
		}

		if (mailServer == null || mailServer.isEmpty())
		{
			updateProperty(ADEMPIERE_MAIL_SERVER, "");
			return "";
		}

		InetAddress server = null;

		try
		{
			server = InetAddress.getByName(mailServer); // Throws
			// UnknownHostException
		}
		catch (UnknownHostException e)
		{
			log.warning("Not correct: mailServer = " + mailServer
					+ ". Setting to empty string.");
			updateProperty(ADEMPIERE_MAIL_SERVER, "");
			throw e;
		}

		updateProperty(ADEMPIERE_MAIL_SERVER, server.getHostName());
		return server.getHostName();
	}

	/**
	 * Get the Admin email address (ADEMPIERE_ADMIN_EMAIL property).
	 * 
	 * @return The admin email address string.
	 */
	@SuppressWarnings("unused")
	private String getAdminEmail() {

		return getValueOrDefault(ADEMPIERE_ADMIN_EMAIL, DEFAULT_ADMIN_EMAIL
				+ getAppsServer(), ALLOW_EMPTY);

	}

	/**
	 * Set the mail port.  
	 */
	private void setMailPort(String mailPort) throws IllegalArgumentException {

		if (mailPort != null && !mailPort.isEmpty())
		{
			// Test that its a number
			try
			{
				Integer.parseInt(mailPort);
			}
			catch (NumberFormatException e)
			{
				log.severe("mailPort can't be parsed as an integer: "
						+ mailPort + " " + e.getMessage());
				updateProperty(ADEMPIERE_MAIL_PORT, "");
				throw e;
			}
		}

		updateProperty(ADEMPIERE_MAIL_PORT, mailPort);
	}

	/**
	 * Gets the ADEMPIERE_WEB_ALIAS property value.
	 * 
	 * @return The web alias. This will default to the local host canonical
	 *         name, if it exists, or an empty string.
	 */
	@SuppressWarnings("unused")
	private String getWebAlias() {

		String webAlias = (String) p_properties.get(ADEMPIERE_WEB_ALIAS);

		if (webAlias == null) // Empty string allowed
		{
			if (localhost != null)
				webAlias = localhost.getCanonicalHostName();
			else
				webAlias = "";
		}

		return webAlias;
	}

	/**
	 * Respond to Property Change Events.  Typically, this is to manage dependencies 
	 * between properties.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent pce) {

		log.finest("");

		String property = pce.getPropertyName();
		if (property == null
				|| property.isEmpty()
				|| (!KNOWN_PROPERTIES.contains(property)
						&& !property.equals(DATABASE_DISCOVERED)
						&& !property.equals(DISCOVERED_DATABASE_LIST) && !property
							.equals(TEST_ERROR)))
			throw new IllegalArgumentException(
					"Change event received from an unrecognized property "
							+ property);

		log.finest("Property Change Event for property '" + property
				+ "' received from " + pce.getSource());

		// Email protocol defaults
		if (property.equals(ADEMPIERE_MAIL_PT) && !isLoading) // Protocol change
		{
			// Set default ports
			for (int i = 0; i < EMAIL_PROTOCOL.length; i++)
			{
				if (pce.getNewValue().equals(EMAIL_PROTOCOL[i]))
				{
					setMailPort(DEFAULT_MAIL_PORTS[i]);
					break;
				}
			}
		}// Email protocol

		// Java type
		if (property.equals(ADEMPIERE_JAVA_TYPE) && !isLoading) // Protocol
		// change
		{
			initJava();
		}

		// Server type
		if (property.equals(ADEMPIERE_APPS_TYPE)) // Protocol change
		{
			if (isLoading)
			{
				enableAppsServerFields();
			}
			else
			{
				initAppsServer();
			}
		}

		// Database Type
		if (property.equals(ADEMPIERE_DB_TYPE)) // Protocol change
		{
			if (isLoading)
			{
				setDatabaseDiscoveredList();
				enableDatabaseFields();
			}
			else
			{
				initDatabase();
			}
		}

		// Discovered Database List - list has changed
		if (property.equals(DISCOVERED_DATABASE_LIST) && !isLoading)
		{
			// For empty list, set the name to zero
			if (getDatabaseDiscoveredList() == null
					|| getDatabaseDiscoveredList().length == 0)
			{

				setDatabaseDiscovered("");

			}
			else
			{
				// Set the name to the first entry
				setDatabaseDiscovered(getDatabaseDiscoveredList()[0]);
			}
		}

		// Discovered Database has changed. Set the database name accordingly.
		if (property.equals(DATABASE_DISCOVERED) && !isLoading)
		{
			// Assume the new value is the displayed text
			int index = getDatabaseTypeIndex(getProperty(ADEMPIERE_DB_TYPE));
			setProperty(ADEMPIERE_DB_NAME,
					(m_databaseConfig[index]
							.getDatabaseName(getDatabaseDiscovered())));
		}

	}

	/**
	 * Call the application server configurations to enable the relevant fields.
	 */
	private void enableAppsServerFields() {

		log.finest("");
		int index = getAppsServerTypeIndex(getProperty(ADEMPIERE_APPS_TYPE));

		m_appsConfig[index].enable(); // Fires property change events
	}

	/**
	 * Enable the fields when the Database Type changes. Used when loading the
	 * database type. When database type is changed, call initDatabase() which
	 * will also set the associated field values.
	 */
	private void enableDatabaseFields() {

		log.finest("");
		int index = getDatabaseTypeIndex(getProperty(ADEMPIERE_DB_TYPE));

		m_databaseConfig[index].enable(); // Fires property change events
	}

	/**
	 * Sets the isUsed value for the property. Fires a PropertyChange event.
	 * 
	 * @param property
	 * @param enabled
	 */
	public void setIsUsed(String property, Boolean enabled) {

		if (property == null || enabled == null)
			return;

		isUsedList.put(property, enabled);

		String value = p_properties.getProperty(property);

		this.pcs.firePropertyChange(property, null, value);

	}

	/**
	 * Fires a generic error Property Change Event associated with the "TEST_ERROR"
	 * property.
	 */
	private void setTestError(String error) {

		if (error == null)
			error = "";

		setTestError(TEST_ERROR, error.isEmpty(), false, error);

	}

	/**
	 * Sends a property change event with the newValue set to an
	 * {@link ErrorData} structure and the oldValue set to null.
	 * 
	 * @param testProperty
	 *            - on of the Configuration Data static variables that starts
	 *            with TEST_
	 * @param pass
	 *            - the boolean result of the test
	 * @param critical
	 *            - true if the test is critical. False if it is optional.
	 * @param error
	 *            - the error string - not localized.
	 */
	public void setTestError(String testProperty, boolean pass,
			boolean critical, String error)
	{

		ErrorData errorData = new ErrorData();

		errorData.propertyName = testProperty;
		errorData.pass = pass;
		errorData.critical = critical;
		errorData.errorMessage = error;

		this.pcs.firePropertyChange(TEST_ERROR, null, errorData);

	}

	/**
	 * Determine if a property is used in a given configuration. For example,
	 * the Database Type selected may not require a database name or JNP Port.
	 * UIs may use this information to disable the associated fields.
	 * 
	 * @return true if used, false if not required/not used.
	 * @throws IllegalArgumentException
	 *             if the property is null, empty or in not known.
	 */
	public Boolean isUsed(String property) throws IllegalArgumentException {

		if (property == null || property.isEmpty())
			throw new IllegalArgumentException(
					"The property argument cannot be null or empty.");

		if (!KNOWN_PROPERTIES.contains(property)
				&& !property.equals(DATABASE_DISCOVERED)
				&& !property.equals(DISCOVERED_DATABASE_LIST)
				&& !property.equals(TEST_ERROR))
			throw new IllegalArgumentException("Property '" + property
					+ "' is not recognized.");

		return isUsedList.get(property);
	}

	/**
	 * Return the value of a property as an int.  Will throw an error if this is not possible.
	 */
	public int getPropertyAsInt(String property) {

		int value = -1;

		String valueString = getProperty(property);

		// Try to parse the valueString. Throw and error if not possible.
		value = Integer.parseInt(valueString);

		return value;
	}

} // ConfigurationData
