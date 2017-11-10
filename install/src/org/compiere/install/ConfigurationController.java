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
package org.compiere.install;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.IOException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.adempiere.configuration.ConfigurationData;
import org.adempiere.configuration.KeyStoreMgt;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.compiere.grid.SimpleLookup;
import org.compiere.grid.SimpleLookupCallback;
import org.compiere.grid.ed.VCheckBox;
import org.compiere.grid.ed.VFile;
import org.compiere.grid.ed.VLookup;
import org.compiere.grid.ed.VString;
import org.compiere.swing.CCheckBox;
import org.compiere.swing.CEditor;
import org.compiere.util.CLogger;

/**
 * Provides the interface controller function for the configuration panel. 
 * @author mckayERP
 *
 */
public class ConfigurationController implements PropertyChangeListener, VetoableChangeListener, SimpleLookupCallback {
	
	private CLogger log = CLogger.getCLogger(this.getClass().getName());

	private ConfigurationPanel p_panel;
	private ConfigurationData p_data;
	
	private BidiMap mapPropertyField = new DualHashBidiMap();
	private BidiMap mapPropertyLookup = new DualHashBidiMap();
	private BidiMap mapLookupField = new DualHashBidiMap();
	private HashMap<String, CEditor> mapPropertyCheckbox = new HashMap<String, CEditor>();
	private HashMap<String, String> mapPropertyError = new HashMap<String,String>();
	
	// Data models for comboboxes
	/**
	 *  The combo box with the Java Type based on the 
	 *  {@link org.adempiere.configuration.ConfigurationData#JAVATYPE}
	 */
	public SimpleLookup lookupJavaType = new SimpleLookup("JavaType", ConfigurationData.JAVATYPE);
	/**
	 *  The combo box with the Application Server Type based on the 
	 *  {@link org.adempiere.configuration.ConfigurationData#APPSTYPE}
	 */
	public SimpleLookup lookupAppsType = new SimpleLookup("AppsType", ConfigurationData.APPSTYPE);
	/**
	 *  The combo box with the Database Type based on the 
	 *  {@link org.adempiere.configuration.ConfigurationData#DBTYPE}
	 */
	public SimpleLookup lookupDBType = new SimpleLookup("DatabaseType",ConfigurationData.DBTYPE);
	/**
	 *  The combo box with the list of Discovered Databases.  The data is gathered through a call 
	 *  back {@link #getLookupData(String)}
	 */
	public SimpleLookup lookupDBDiscovered = new SimpleLookup("DatabaseDiscovered", this);

	/** A flag indicating the data is loading */
	private boolean m_loading;

	/**
	 * Constructor called from the UI.
	 * @param configurationPanel
	 * 				The {@link org.compiere.install.ConfigurationPanel} "view" being controlled
	 */
	public ConfigurationController(ConfigurationPanel configurationPanel) {
		
		p_panel = configurationPanel;
		
		init();
	}

	/**
	 * Initialize the controller.  Creates the {@link org.adempiiere.configuration.ConfigurationData Configuration properties}
	 * and registers as a Property Change Listener on the properties.
	 */
	private void init() {
		
		log.finest("");
		p_data = new ConfigurationData();		
		p_data.addPropertyChangeListener(this);
		
	}
	
	/**
	 * Load the properties which will trigger the events that will be used 
	 * to fill in the UI
	 * @return true if successful.
	 */
	public boolean load() {
		
		log.finest("");
		m_loading = true;
		boolean result = p_data.load();  // Will send property change events
		m_loading = false;
		
		return result;
		
	}

	/**
	 * Property change handler for events triggered by the ConfigurationData 
	 * elements. These events signal changes in the fields.  This listener
	 * maps the property to the appropriate field and updates its value.  There are 
	 * several types of properties dealt with:  basic properties and test results.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		
		log.finest("");
		
		String propertyName = pce.getPropertyName();
		
		log.fine("Property Change received for '" + propertyName + " from '" + pce.getOldValue()
				+ "' to '" + pce.getNewValue() + "'.");
				
		SimpleLookup lookup = (SimpleLookup) mapPropertyLookup.get(propertyName);
		
		if (lookup != null)
		{

			log.fine("  Property has a lookup.");			
			VLookup lookupField = (VLookup) mapLookupField.get(lookup);
			lookup.fillComboBox(lookupField.isMandatory(), true, true, false);
						
		}
		
		CEditor field = (CEditor) mapPropertyField.get(propertyName);
		
		if (field != null)
		{
			log.fine("  Property has a field.");
			
			if (field.getValue() != pce.getNewValue())
			{
				field.setValue((String) pce.getNewValue());

				log.fine("Updated field for property " 
					+ propertyName + " from '" + (String) pce.getOldValue()
					+ "' to '" + pce.getNewValue() + "'.  Field value: " + field.getValue());
			}
			else
			{
				log.fine("field for property " 
						+ propertyName + " already matches new value.  No change made.");
				
			}
			// Check if the field is used/enabled
			Object isUsed = p_data.isUsed(propertyName);
			if (isUsed != null && isUsed instanceof Boolean)
			{
				field.setReadWrite((Boolean) isUsed);
			}
		}
		
		CCheckBox checkBox = (CCheckBox) mapPropertyCheckbox.get(propertyName);
		
		if (checkBox != null)
		{
			log.fine("  Property has checkbox.");
			
			if (checkBox.isSelected() ^ (boolean) pce.getNewValue())
			{
				checkBox.setSelected((boolean) pce.getNewValue()); 
			}
		}
		
		if (ConfigurationData.TEST_ERROR.equals(propertyName))
		{
			if (pce.getNewValue() instanceof ConfigurationData.ErrorData)
			{
				// The config data is sending a test result
				ConfigurationData.ErrorData errorData = (ConfigurationData.ErrorData) pce.getNewValue();
				
				String propName = errorData.propertyName;
				String errorMsg = errorData.errorMessage;
				boolean pass = errorData.pass;
				boolean critical = errorData.critical;
				
				VCheckBox cb = (VCheckBox) mapPropertyCheckbox.get(propName);
				String resString = (String) mapPropertyError.get(propName);
				
				p_panel.signalOK(cb, resString, pass, critical, errorMsg);
			}
		}
	}


	/**
	 * Execute tests on the properties.
	 * @return
	 */
	public boolean test() {
		
		log.finest("");
		
		// Check the keyStore with the UI panel
		// If it doesn't exist, it will open an interface 
		// to create it.  Without the UI, the KeyStore
		// has to be created manually.
		updateKeyStore();
		
		
		return p_data.test();
		
	}

	/**
	 * Update the KeyStore data.  This is performed in the controller to provide
	 * access to the KeystoreDialog GUI panel. If the KeyStore doesn't already exist, a GUI 
	 * will be presented to the user where the values can be changed. The values are
	 * saved back in the ConfigurationData.
	 */
	private void updateKeyStore() {
		
		if (p_panel == null)
			return;  // No point in proceeding.
		
		// Load the keystore data
		String fileName = KeyStoreMgt.getKeystoreFileName(p_data.getProperty(ConfigurationData.ADEMPIERE_HOME));
		KeyStoreMgt ks = new KeyStoreMgt (fileName, 
				p_data.getProperty(ConfigurationData.ADEMPIERE_KEYSTOREPASS).toCharArray());
		
		try
		{
			KeyStore keyStore = ks.getKeyStore();
			// If the ketStore exists, no need to open the dialog
			if (keyStore != null)
				return;
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "get KeyStore", e);
			return;
		}
		
		// No keystore - use the UI to create one.
		// Set default values from the configuration
		KeyStoreDialog ksd = new KeyStoreDialog(
				(JFrame)SwingUtilities.getWindowAncestor(p_panel),
				(String)p_data.getProperty(ConfigurationData.ADEMPIERE_CERT_CN), 
				(String)p_data.getProperty(ConfigurationData.ADEMPIERE_CERT_ORG_UNIT),
				(String)p_data.getProperty(ConfigurationData.ADEMPIERE_CERT_ORG),
				(String)p_data.getProperty(ConfigurationData.ADEMPIERE_CERT_LOCATION),
				(String)p_data.getProperty(ConfigurationData.ADEMPIERE_CERT_STATE), 
				(String)p_data.getProperty(ConfigurationData.ADEMPIERE_CERT_COUNTRY));	
		
		if (ksd.isOK())
		{
			// Update the data
			p_data.setProperty(ConfigurationData.ADEMPIERE_CERT_CN, ksd.getCN()); 
			p_data.setProperty(ConfigurationData.ADEMPIERE_CERT_ORG_UNIT, ksd.getOU());
			p_data.setProperty(ConfigurationData.ADEMPIERE_CERT_ORG, ksd.getO());
			p_data.setProperty(ConfigurationData.ADEMPIERE_CERT_LOCATION, ksd.getL());
			p_data.setProperty(ConfigurationData.ADEMPIERE_CERT_STATE,ksd.getS()); 
			p_data.setProperty(ConfigurationData.ADEMPIERE_CERT_COUNTRY,ksd.getC());	

		}
		
	}

	/**
	 * Execute the save operation on the properties.
	 * @return true if successful.
	 * @throws IOException
	 */
	public boolean save() throws IOException {
		
		log.finest("");
		return p_data.save();
		
	}


	/**
	 * Load a map of property names to the panel fields.  Do this after
	 * the panel fields are initialized.
	 */
	public void loadMap() {
		
		// This is a bidirectional map.
		// Properties <-> Fields  one-to-one, no duplication allowed in either direction.
		mapPropertyField.put(ConfigurationData.ADEMPIERE_ADMIN_EMAIL, p_panel.fAdminEMail);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_APPS_DEPLOY, p_panel.fDeployDir);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_APPS_TYPE, p_panel.fAppsType);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_APPS_SERVER, p_panel.fAppsServer);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_DB_NAME, p_panel.fDatabaseName);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_DB_PASSWORD, p_panel.fDatabasePassword);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_DB_PORT, p_panel.fDatabasePort);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_DB_SERVER, p_panel.fDatabaseServer);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_DB_SYSTEM_PASS, p_panel.fSystemPassword);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_DB_TYPE, p_panel.fDatabaseType);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_DB_USER, p_panel.fDatabaseUser);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_HOME, p_panel.fAdempiereHome);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_JAVA_TYPE, p_panel.fJavaType);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_JAVA_OPTIONS, p_panel.fJavaOpts);		
		mapPropertyField.put(ConfigurationData.ADEMPIERE_JNP_PORT, p_panel.fJNPPort);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_KEYSTOREPASS, p_panel.fKeyStore);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_MAIL_AM, p_panel.fAuthMechanism);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_MAIL_ET, p_panel.fEncryptionType);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_MAIL_PASSWORD, p_panel.fMailPassword);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_MAIL_PORT, p_panel.fMailPort);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_MAIL_PT, p_panel.fMailProtocol);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_MAIL_SERVER, p_panel.fMailServer);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_MAIL_USER, p_panel.fMailUser);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_SSL_PORT, p_panel.fSSLPort);
		mapPropertyField.put(ConfigurationData.ADEMPIERE_WEB_PORT, p_panel.fWebPort);
		mapPropertyField.put(ConfigurationData.DATABASE_DISCOVERED, p_panel.fDatabaseDiscovered);
		mapPropertyField.put(ConfigurationData.JAVA_HOME, p_panel.fJavaHome);
		
		// Legacy name mapping
		//map.put(ConfigurationData.ADEMPIERE_DB_SYSTEM, p_panel.fSystemPassword);

		
		// Dynamic Lookups and field data
		mapPropertyLookup.put(ConfigurationData.DATABASE_DISCOVERED, lookupDBDiscovered);
		
		mapLookupField.put(lookupDBDiscovered, p_panel.fDatabaseDiscovered);

		// Map of test properties to checkboxes
		mapPropertyCheckbox.put(ConfigurationData.TEST_ADEMPIERE_HOME, p_panel.okAdempiereHome);
		mapPropertyCheckbox.put(ConfigurationData.TEST_JAVA_HOME, p_panel.okJavaHome);
		mapPropertyCheckbox.put(ConfigurationData.TEST_KEYSTORE_PASS, p_panel.okKeyStore);
		mapPropertyCheckbox.put(ConfigurationData.TEST_APPS_SERVER, p_panel.okAppsServer);
		mapPropertyCheckbox.put(ConfigurationData.TEST_DEPLOYMENT_DIR, p_panel.okDeployDir);
		mapPropertyCheckbox.put(ConfigurationData.TEST_JNP_PORT, p_panel.okJNPPort);
		mapPropertyCheckbox.put(ConfigurationData.TEST_WEB_PORT, p_panel.okWebPort);
		mapPropertyCheckbox.put(ConfigurationData.TEST_SSL_PORT, p_panel.okSSLPort);
		mapPropertyCheckbox.put(ConfigurationData.TEST_DB_SERVER, p_panel.okDatabaseServer);
		mapPropertyCheckbox.put(ConfigurationData.TEST_DB_PORT, p_panel.okDatabaseServer);
		mapPropertyCheckbox.put(ConfigurationData.TEST_DB_ADMIN_PASS, p_panel.okDatabaseSystem);
		mapPropertyCheckbox.put(ConfigurationData.TEST_DB_PASS, p_panel.okDatabaseUser);
		mapPropertyCheckbox.put(ConfigurationData.TEST_DB_SQL, p_panel.okDatabaseSQL);
		mapPropertyCheckbox.put(ConfigurationData.TEST_EMAIL_PORT, p_panel.okMailServer);
		mapPropertyCheckbox.put(ConfigurationData.TEST_EMAIL_PASS, p_panel.okMailUser);

		// Map of the test properties to the Resource strings for localization.
		mapPropertyError.put(ConfigurationData.TEST_ERROR, "ServerError");
		mapPropertyError.put(ConfigurationData.TEST_JAVA_HOME, "ErrorJavaHome");
		mapPropertyError.put(ConfigurationData.TEST_ADEMPIERE_HOME, "ErrorAdempiereHome");
		mapPropertyError.put(ConfigurationData.TEST_APPS_SERVER, "ErrorAppsServer");
		mapPropertyError.put(ConfigurationData.TEST_DEPLOYMENT_DIR, "ErrorAppsServer");
		mapPropertyError.put(ConfigurationData.TEST_KEYSTORE_PASS, "KeyStorePassword");
		mapPropertyError.put(ConfigurationData.TEST_WEB_PORT, "ErrorWebPort");
		mapPropertyError.put(ConfigurationData.TEST_JNP_PORT, "ErrorJNPPort");
		mapPropertyError.put(ConfigurationData.TEST_SSL_PORT, "ErrorSSLPort");
		mapPropertyError.put(ConfigurationData.TEST_DB_SERVER, "ErrorDatabaseServer");
		mapPropertyError.put(ConfigurationData.TEST_DB_PORT, "ErrorDatabasePort");
		mapPropertyError.put(ConfigurationData.TEST_DB_ADMIN_PASS, "ErrorJDBC");
		mapPropertyError.put(ConfigurationData.TEST_DB_PASS, "ErrorJDBC");
		mapPropertyError.put(ConfigurationData.TEST_DB_SQL, "ErrorTNS");
		mapPropertyError.put(ConfigurationData.TEST_DB_USER, "ErrorJDBC");
		mapPropertyError.put(ConfigurationData.TEST_EMAIL_PORT, "ErrorMailServer");
		mapPropertyError.put(ConfigurationData.TEST_EMAIL_PASS, "ErrorMail");
//		mapPropertyError.put("ErrorSave");
		
	}

	/**
	 * The vetoable change listener for change events fired by fields in the UI. These are processed
	 * here and, if there is a problem, a PropertyVetoException is fired which will return
	 * the source field in the UI to its original value. 
	 */
	@Override
	public void vetoableChange(PropertyChangeEvent vce)
			throws PropertyVetoException {
		
		log.finest("");
		// ignore incoming changes from the fields during load. These changes are caused by the 
		// property change events, not by the user.
		if (m_loading)
			return;
		
		log.fine(vce.getPropertyName() + "=" + vce.getNewValue() + " (" + vce.getOldValue() + ") "
				+ (vce.getOldValue() == null ? "" : vce.getOldValue().getClass().getName()));
		
		if (vce.getSource() == null || !(vce.getSource() instanceof CEditor))
			return;

		CEditor field = null;
		
		if (vce.getSource() instanceof VFile)
		{
			field = (VFile) vce.getSource();
		}
		else
		if (vce.getSource() instanceof VLookup)
		{
			field = (VLookup) vce.getSource();
		}
		else
		if (vce.getSource() instanceof VString)
		{
			field = (VString) vce.getSource();
		}
		else
		{
			field = (CEditor) vce.getSource();
		}
		
		String property = (String) mapPropertyField.getKey(field);
		if (property == null)
		{
			throw new IllegalArgumentException("mapPropertyField does not contain a property key for field " + field);
		}
		
		try {
			p_data.setProperty(property, (String) field.getValue());
		} catch (IllegalArgumentException e) {
			String msg = e.getMessage();
			if (msg == null || msg.isEmpty())
			{
				if (e.getCause() != null)
					msg = e.getCause().getMessage();
			}
			log.warning(msg);
			//e.printStackTrace();
			throw new PropertyVetoException("Veto property change for  '" + property + "': " + msg,vce);
		}  

	}

	/**
	 * A lookup callback for the SimpleLookup for the list of discovered databases.
	 */
	@Override
	public String[] getLookupData(String columnName) {
		
		log.finest(columnName);
		// Only have one field that has dynamic content
		return p_data.getDatabaseDiscoveredList();
		
	}
}
