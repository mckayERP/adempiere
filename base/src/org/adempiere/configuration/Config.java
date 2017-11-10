/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.adempiere.configuration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.compiere.util.CLogger;
import org.compiere.util.DB;


/**
 *	Configuration Setup and Test
 *	
 *  @author Jorg Janke
 */
public abstract class Config
{
	/**	Configuration Data			*/
	protected ConfigurationData 	p_data = null;
	
	protected String error;
	protected InetAddress appsServer;

	public class DatabaseData {
		String dbType = "";
		String[] dbSearch = {};
		String dbName = "";
		int dbSearchIndex = 0;
		String dbPort = "";
		
	}
	
	protected DatabaseData p_databaseData = new DatabaseData();

	/**	Logger	*/
	static CLogger	log	= CLogger.getCLogger (Config.class);

	
	/**
	 * 	Configuration
	 * 	@param data configuration
	 */
	public Config (ConfigurationData data)
	{
		super ();
		p_data = data;
	}	//	Config
	/**
	 * 	Initialize
	 */
	public abstract void init();

	/**
	 * 	Enable properties - indicate that properties are used and 
	 *  relevant to the configuration
	 */
	public abstract void enable();

	/**
	 * 	Test
	 *	@return error message or null of OK
	 */
	public abstract String test();

	/**
	 * 	Discover Databases.
	 * 	To be overwritten by database configs
	 *	@param selected selected database
	 *	@return array of databases
	 */
	public String[] discoverDatabases(String selected)
	{
		return new String[]{};
	}	//	discoverDatabases
	
	/**
	 * Get real database name from native connection profile name
	 * return from discoverDatabases
	 * @param nativeConnectioName
	 * @return Database name
	 */
	public String getDatabaseName(String nativeConnectioName)
	{
		return nativeConnectioName;
	}
	
	/**
	 * Get real database name from a list. List is
	 * set from discoverDatabases
	 * @param nativeConnectioName
	 * @return Database name
	 */	
	public String getDatabaseName(int index) 
	{
		return "";
	}
		
	/**
	 * 	UI Signal OK - Deprecated.  Use {@link org.adempiere.configuration.ConfigurationData#setTestError(String, boolean, boolean, String) setTestError} instead.
	 *	@param cb ckeck box
	 *	@param resString resource string key
	 *	@param pass true if test passed
	 *	@param critical true if critical
	 *	@param errorMsg error Message
	 */
	@Deprecated
	void signalOK (Object cb, String resString, boolean pass, boolean critical, String errorMsg)
	{
		//p_panel.signalOK(cb, resString, pass, critical, errorMsg);
	}	//	signalOK

	/**
	 * 	Get Web Store Context Names separated by ,
	 *	@param con connection
	 *	@return String of Web Store Names - e.g. /wstore
	 */
	protected String getWebStores(Connection con) {
		String sql = "SELECT WebContext FROM W_Store WHERE IsActive='Y'";
		Statement stmt = null;
		ResultSet rs = null;
		StringBuffer result = new StringBuffer();
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				if (result.length() > 0)
					result.append(",");
				result.append(rs.getString(1));
			}
		} catch (Exception e) {
			log.severe(e.toString());
		} finally {
			DB.close(rs, stmt);
			rs = null;
			stmt = null;
		}
		return result.toString();
	} // getWebStores

	/**
	 * Test the Application Server properties
	 * @return
	 */
	protected boolean testAppsServer() {
		
		//	AppsServer - update using the InetAddress host name, if found.
		//  Tests are performed by the p_data.setAppsServer() function which will
		//  throw errors

		String server = p_data.getProperty(ConfigurationData.ADEMPIERE_APPS_SERVER);

		boolean pass = true;
		try {

			// Set the apps server property. This will perform tests and may change the value
			p_data.setProperty(ConfigurationData.ADEMPIERE_APPS_SERVER, server);
			appsServer = InetAddress.getByName(server);
			
		} catch (Exception e) { // 
			pass = false;
			error = "Not correct: AppsServer = " + server + " " + e.getMessage();
			log.warning(e.getMessage());
		}  // Performs tests			

		if (pass)
		{
			
			log.info("OK: AppsServer = " + appsServer);

		}

		p_data.setTestError(ConfigurationData.TEST_APPS_SERVER, pass, true, error);

		return pass;
	}
	
	/**
	 * Test the Application Server Deployment Directory
	 * @param deployDir
	 * @return
	 */
	protected boolean testServerDeployDir(String deployDir) {
		File deploy = new File (deployDir);
		boolean pass = deploy.exists();
		error = "Not found: " + deploy;
		
		p_data.setTestError(ConfigurationData.TEST_DEPLOYMENT_DIR, pass, true, error);
				
		if (pass)
		{
			log.info("OK: Deploy Directory = " + deployDir);			
		}
		
		return pass;
	}
	
	/**
	 * 	Test Server Port
	 *  @param port port
	 *  @return true if able to create
	 */
	private boolean testServerPort (int port)
	{
		log.fine("" + port);
		try
		{
			ServerSocket ss = new ServerSocket (port);
			log.fine(ss.getInetAddress() + ":" + ss.getLocalPort() + " - created");
			ss.close();
		}
		catch (Exception ex)
		{
			log.warning("Port " + port + ": " + ex.getMessage());
			return false;
		}
		return true;
	}	//	testPort

	/**************************************************************************
	 * 	Test Apps Server Port (client perspective)
	 *  @param protocol protocol (http, ..)
	 *  @param server server name
	 *  @param port port
	 *  @param file file name
	 *  @return true if able to connect
	 */
	private boolean testPort (String protocol, String server, int port, String file)
	{
		System.out.println("testPort[" + protocol + "," + server + ", " + port + ", " + file +"]");
		URL url = null;
		try
		{
			url = new URL (protocol, server, port, file);
		}
		catch (MalformedURLException ex)
		{
			log.severe("No URL for Protocol=" + protocol 
				+ ", Server=" + server
				+ ": " + ex.getMessage());
			return false;
		}
		try
		{
			URLConnection c = url.openConnection();
			Object o = c.getContent();
			if (o == null)
				log.warning("In use=" + url);	//	error
			else
				log.warning("In Use=" + url);	//	error
		}
		catch (Exception ex)
		{
			log.fine("Not used=" + url);	//	ok
			return false;
		}
		return true;
	}	//	testPort

	/**
	 * 	Test host:port with a simple ping
	 *  @param host host
	 *  @param port port
	 *  @param shouldBeUsed true if it should be used
	 *  @return true if some server answered on port
	 */
	public static boolean testPort (InetAddress host, int port, boolean shouldBeUsed)
	{
		log.fine("Testing Port[" + host.getHostAddress() + ", " + port + "]");
		
		String error = "";
		
		Socket pingSocket = null;
		try
		{
			pingSocket = new Socket(host, port);
		}
		catch (Exception e)
		{
			if (shouldBeUsed)
			{
				error = "Open Socket " + host + ":" + port + " - " + e.getMessage();
				log.warning(error);
			}
			else
			{
				error = host + ":" + port + " - " + e.getMessage();
				log.fine(error);
			}
		}
		
		if (error.isEmpty())
		{
			if (!shouldBeUsed)
				log.warning("Open Socket " + host + ":" + port + " - " + pingSocket);
			
			log.fine(host + ":" + port + " - " + pingSocket);
	
			//	success
			try
			{
				pingSocket.close();
			}
			catch (IOException e)
			{
				log.warning("Trying to close socket=" + e.toString());
			}
		}
		
		if (!error.isEmpty())
			return false;
		
		return true;
	}	//	testPort

	/**
	 * Ping the JNP Port
	 * @return
	 */
	protected boolean testServerJNPPort() {
		
		boolean pass = false;
		
		int JNPPort = -1;
		
		if (appsServer == null)
		{
			error = "Error JNP Port: No Application Server found.";
			pass = false;
		}
		else
		{
			
			String JNPPortString = p_data.getProperty(ConfigurationData.ADEMPIERE_JNP_PORT);
			
			JNPPort = Integer.parseInt(JNPPortString);
		
			pass = !testPort (appsServer, JNPPort, false) 
						&& testServerPort(JNPPort);
			error = "Not correct: JNP Port = " + JNPPort;
		}
		
		p_data.setTestError(ConfigurationData.TEST_JNP_PORT, pass, true, error);
		
		if (pass)
			log.info("OK: JNPPort = " + JNPPort);

		return pass;

	}
	
	/**
	 * Ping the Web Port
	 * @return
	 */
	protected boolean testServerWebPort() {
		
		boolean pass = false;
		int webPort = -1;
		
		if (appsServer == null)
		{
			error = "Error Web Port: No Application Server found.";
			pass = false;
		}
		else
		{	
			String webPortString = p_data.getProperty(ConfigurationData.ADEMPIERE_WEB_PORT);
			
			webPort = Integer.parseInt(webPortString);
			
			pass = !testPort ("http", appsServer.getHostName(), webPort, "/") 
				&& testServerPort(webPort);
			error = "Not correct: Web Port = " + webPort;
		}
		
		p_data.setTestError(ConfigurationData.TEST_WEB_PORT, pass, true, error);
		
		if (pass)
			log.info("OK: Web Port = " + webPort);

		return pass;
	}
	
	/**
	 * Pint the SSL Port
	 * @return
	 */
	protected boolean testServerSSLPort() {

		boolean pass = false;
		int sslPort = -1;
		
		if (appsServer == null)
		{
			error = "Error SSL Port: No Application Server found.";
			pass = false;
		}
		else
		{
			String sslPortString = p_data.getProperty(ConfigurationData.ADEMPIERE_SSL_PORT);
			
			sslPort = Integer.parseInt(sslPortString);
			
			pass = !testPort ("https", appsServer.getHostName(), sslPort, "/") 
				&& testServerPort(sslPort);
			error = "Not correct: SSL Port = " + sslPort;
		}
		
		p_data.setTestError(ConfigurationData.TEST_SSL_PORT, pass, true, error);
		
		if (pass)
			log.info("OK: SSL Port = " + sslPort);

		return pass;
	}
	
	/**
	 * Resolve the Database Name from the Connection
	 * @param connectionName
	 * @return
	 */
	public String resolveDatabaseName(String connectionName) {
		
		String dbType = p_data.getProperty(ConfigurationData.ADEMPIERE_DB_TYPE);
		String[] types = ConfigurationData.DBTYPE;
		int index = -1;
		for (int i = 0; i < ConfigurationData.DBTYPE.length; i++)
		{
			if (dbType.equals(types[i]))
			{
				index = i;
				break;
			}
		}
		
		if (index == -1)
			log.warning("DatabaseType Index invalid: " + index);
		else if (p_data.m_databaseConfig[index] == null)
			log.warning("DatabaseType Config missing: " + ConfigurationData.DBTYPE[index]);
		else
			return p_data.m_databaseConfig[index].getDatabaseName(connectionName);
		
		return connectionName;
	}
	
	/**
	 * Get the configuration specific deployment directory
	 * @return
	 */
	public abstract String getDeployDir();

}	//	Config
