/******************************************************************************
 * The contents of this file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use this file except in compliance with the License
 * You may obtain a copy of the License at http://www.compiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2005 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Portions created by Victor Perez are Copyright (C) 1999-2005 e-Evolution,S.C
 * Contributor(s): Victor Perez
 *****************************************************************************/
package org.adempiere.configuration;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.SQLException;

import org.compiere.db.DB_PostgreSQL;

/**
 *	PostgreSQL Configuration
 *	
 *  @author Victor Perez e-Evolution
 *  @version $Id: ConfigPostgreSQL.java,v 1.0 2005/01/31 06:08:15 vpj-cd Exp $
 */
public class ConfigPostgreSQL extends Config
{

	/**
	 * 	ConfigPostgreSQL
	 *	@param data
	 */
	public ConfigPostgreSQL (ConfigurationData data)
	{
		super (data);
	}	//	ConfigPostgreSQL

	/**	PostgreSQL DB Info			*/
	private DB_PostgreSQL			p_db = new DB_PostgreSQL();
	
	/**
	 * 	Init
	 */
	public void init()
	{
		p_data.setDatabaseDiscoveredList();	// Not used
//		p_data.setDatabaseDiscovered(""); // Not used
		p_data.setProperty(ConfigurationData.ADEMPIERE_DB_NAME,"");	// Not used
		p_data.setProperty(ConfigurationData.ADEMPIERE_DB_PORT, String.valueOf(DB_PostgreSQL.DEFAULT_PORT));
		
		enable();
	}	//	init

	/**
	 * 	Discover Databases.
	 * 	To be overwritten by database configs
	 *	@param selected selected database
	 *	@return array of databases
	 */
	public String[] discoverDatabases(String selected)
	{
		log.finest("");
		return new String[]{};

	}	//	discoveredDatabases
	
	
	/**************************************************************************
	 * 	Test
	 *	@return error message or null if OK
	 */
	public String test()
	{
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

		p_data.setTestError(ConfigurationData.TEST_DB_ADMIN_PASS, pass, true, error);

		if (!pass)
			return error;
		log.info("OK: System Connection = " + urlSystem);

		//	Database User Info
		String databaseUser = p_data.getProperty(ConfigurationData.ADEMPIERE_DB_USER);	//	UID
		String databasePassword = p_data.getProperty(ConfigurationData.ADEMPIERE_DB_PASSWORD);	//	PWD
		pass = databasePassword != null && databasePassword.length() > 0;
		error = "Invalid Database User Password";

		p_data.setTestError(ConfigurationData.TEST_DB_PASS, pass, true, error);

		if (!pass)
			return error;
		//
		String url= p_db.getConnectionURL(databaseServer.getHostName(), databasePort, 
			databaseName, databaseUser);

		//	Ignore result as it might not be imported
		pass = testJDBC(url, databaseUser, databasePassword);
		error = "Database imported? Cannot connect to User: " + databaseUser + "/" + databasePassword;

		p_data.setTestError(ConfigurationData.TEST_DB_PASS, pass, true, error);

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
	private boolean testJDBC (String url, String uid, String pwd)
	{
		Connection conn = null;  // Need to close the connection if there is an error
		try
		{
			conn = p_db.getDriverConnection(url, uid, pwd);
		}
		catch (Exception e)
		{
			log.severe(e.toString());
			if (conn != null)
				try {
					conn.close();
				} catch (SQLException e1) {
					// Potential open connection
					// It will get shutdown by the finalizer eventually.
				}
			return false;
		}
		return true;
	}	//	testJDBC

	@Override
	public void enable() {
		
		p_data.setIsUsed(ConfigurationData.DATABASE_DISCOVERED,false);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_DB_NAME,true);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_DB_PASSWORD,true);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_DB_USER,true);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_DB_SYSTEM_PASS,true);
		
	}

	@Override
	public String getDeployDir() {
		// TODO Auto-generated method stub
		return null;
	}
	
}	//	ConfigPostgreSQL
