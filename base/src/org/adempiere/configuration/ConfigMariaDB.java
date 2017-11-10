/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2016 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
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

import java.net.InetAddress;
import java.sql.Connection;

import org.compiere.db.DB_MariaDB;


/**
 * Configuration class for MariaDB Database
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<li> FR [ 391 ] Add connection support to MariaDB
 *		@see https://github.com/adempiere/adempiere/issues/464
 */
public class ConfigMariaDB extends Config {
	/**
	 * 	ConfigPostgreSQL
	 *	@param data
	 */
	public ConfigMariaDB (ConfigurationData data) {
		super (data);
	}	//	ConfigPostgreSQL

	/** Discovered TNS			*/
	private String[] 			p_discovered = null;
	/**	MariaDB DB Info			*/
	private DB_MariaDB			p_db = new DB_MariaDB();
	
	/**
	 * 	Init
	 */
	public void init() {
		
		p_data.setDatabaseDiscoveredList();	// Not used
//		p_data.setDatabaseDiscovered(""); // Not used
		p_data.setProperty(ConfigurationData.ADEMPIERE_DB_NAME,"");	// Not used
		p_data.setProperty(ConfigurationData.ADEMPIERE_DB_PORT, String.valueOf(DB_MariaDB.DEFAULT_PORT));
		
		enable();
	}	//	init

	/**
	 * 	Discover Databases.
	 * 	To be overwritten by database configs
	 *	@param selected selected database
	 *	@return array of databases
	 */
	public String[] discoverDatabases(String selected) {
		if (p_discovered != null)
			return p_discovered;
		p_discovered = new String[]{};
		return p_discovered;
	}	//	discoveredDatabases
	
	
	/**************************************************************************
	 * 	Test
	 *	@return error message or null if OK
	 */
	public String test() {
		//	Database Server
		String server = p_data.getProperty(ConfigurationData.ADEMPIERE_DB_SERVER);
		boolean pass = server != null && server.length() > 0;
		// vpj-cd e-evolution && server.toLowerCase().indexOf("localhost") == -1                        
		// vpj-cd e-evolution && !server.equals("127.0.0.1");
                        
		String error = "Not correct: DB Server = " + server;
		InetAddress databaseServer = null;
		try
		{
			if (pass)
				databaseServer = InetAddress.getByName(server);
		}
		catch (Exception e)
		{
			error += " - " + e.getMessage();
			pass = false;
		}

		p_data.setTestError(ConfigurationData.TEST_DB_SERVER, pass, true, error);
		
		log.info("OK: Database Server = " + databaseServer);

		//	Database Port
		int databasePort = p_data.getPropertyAsInt(ConfigurationData.ADEMPIERE_DB_PORT);
		pass = testPort (databaseServer, databasePort, true);
		error = "DB Server Port = " + databasePort;
		
		p_data.setTestError(ConfigurationData.TEST_DB_PORT, pass, true, error);

		if (!pass)
			return error;
		
		log.info("OK: Database Port = " + databasePort);

		//	JDBC Database Info
		String databaseName = p_data.getProperty(ConfigurationData.ADEMPIERE_DB_NAME);	//	Service Name
		String systemPassword = p_data.getProperty(ConfigurationData.ADEMPIERE_DB_SYSTEM_PASS);

		//	URL (derived)
		String urlSystem = p_db.getConnectionURL(databaseServer.getHostName(), databasePort, 
			p_db.getSystemDatabase(databaseName), p_db.getSystemUser());
		pass = testJDBC(urlSystem, p_db.getSystemUser(), systemPassword);
		error = "Error connecting: " + urlSystem 
			+ " - " + p_db.getSystemUser() + "/" + systemPassword;
		
		p_data.setTestError(ConfigurationData.TEST_DB_ADMIN_PASS, pass, false,  error);
		
		if (!pass)
			return error;
		
		log.info("OK: System Connection = " + urlSystem);

		//	Database User Info
		String databaseUser = p_data.getProperty(ConfigurationData.ADEMPIERE_DB_USER);;	//	UID
		String databasePassword = p_data.getProperty(ConfigurationData.ADEMPIERE_DB_PASSWORD);;	//	PWD
		pass = databasePassword != null && databasePassword.length() > 0;
		error = "Invalid Database User Password";

		p_data.setTestError(ConfigurationData.TEST_DB_PASS, pass, false, error);

		if (!pass)
			return error;
		//
		String url= p_db.getConnectionURL(databaseServer.getHostName(), databasePort, 
			databaseName, databaseUser);
		
		//	Ignore result as it might not be imported
		pass = testJDBC(url, databaseUser, databasePassword);
		error = "Database imported? Cannot connect to User: " + databaseUser + "/" + databasePassword;

		p_data.setTestError(ConfigurationData.TEST_DB_USER, pass, false, error);

		if (pass)
			log.info("OK: Database User = " + databaseUser);
		else
			log.warning(error);

		return null;
	}	//	test
	
	/**
	 * 	Test JDBC Connection to Server
	 * 	@param url connection string
	 *  @param uid user id
	 *  @param pwd password
	 * 	@return true if OK
	 */
	private boolean testJDBC (String url, String uid, String pwd) {
		try {
			@SuppressWarnings("unused")
			Connection conn = p_db.getDriverConnection(url, uid, pwd);
		} catch (Exception e) {
			log.severe(e.toString());
			return false;
		}
		return true;
	}

	@Override
	public void enable() {

		p_data.setIsUsed(ConfigurationData.DATABASE_DISCOVERED,false);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_DB_NAME,false);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_DB_PASSWORD,true);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_DB_USER,true);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_DB_SYSTEM_PASS,true);
		
	}

	@Override
	public String getDeployDir() {
		// TODO Auto-generated method stub
		return null;
	}
}
