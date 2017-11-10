/******************************************************************************
 * Copyright (C) 2008 Low Heng Sin                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.adempiere.as;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.adempiere.configuration.ConfigurationData;

/**
 * 
 * @author Low Heng Sin
 *
 */
public class ASFactory {
	public final static String JBOSS = ConfigurationData.APPSTYPE_JBOSS;
	
	public final static String GLASS_FISH = ConfigurationData.APPSTYPE_GLASSFISH;
	
	public final static String TOMCAT = ConfigurationData.APPSTYPE_TOMCAT;
	
	public final static String[] AS_Names = ConfigurationData.APPSTYPE;
	
	// Use a hashmap to connect server classes with the server types
	private final static Map<String, String> AS_Classes;
	static
	{
		AS_Classes = new HashMap<String, String>();
		AS_Classes.put(JBOSS, "org.adempiere.as.jboss.JBoss");
		AS_Classes.put(GLASS_FISH, "org.adempiere.as.glassfish.GlassFish");
		AS_Classes.put(TOMCAT, "org.adempiere.as.tomcat.Tomcat");
	}
	
	private static IApplicationServer applicationServer;
	
	static {
		
		String serverType = getServerType();
		
		if (serverType == null || serverType.isEmpty()) // Not identified in the data
		{
			//detect the installed application server class - use the first match
			for(String s : AS_Classes.values()) {
				try {
					Class<?> c = Class.forName(s);
					IApplicationServer server = (IApplicationServer) c.newInstance();
					applicationServer = server;
					break;
				} catch (Throwable t) {
				}
			}
		}
		else // Use the specific server defined in the environment properties
		{
			String serverClass = AS_Classes.get(serverType);

			if (serverClass !=null && !serverClass.isEmpty())
			{
				try {
					Class<?> c = Class.forName(serverClass);
					IApplicationServer server = (IApplicationServer) c.newInstance();
					applicationServer = server;
				} catch (Throwable t) {
				}
			}
		}
		
		// Fallback - provide an null server
		if (applicationServer == null) {
			applicationServer = new IApplicationServer() {

				public int getDefaultNamingServicePort() {
					return 0;
				}

				public Hashtable<String, String> getInitialContextEnvironment(
						String AppsHost, int AppsPort, String principal,
						String credential) {
					return new Hashtable<String, String>();
				}

				public boolean canGetInitialContext() {
					return false;
				}			
			};
		}
	}

	/**
	 * @return IApplicationServer
	 */
	public static IApplicationServer getApplicationServer() {
		return applicationServer;
	}

	private static String getServerType() {

		String serverType = JBOSS;
		
		ConfigurationData data = new ConfigurationData();
		if (data.load()) {
		    serverType = data.getProperty(ConfigurationData.ADEMPIERE_APPS_TYPE);
		}

		return serverType;
	}

}
