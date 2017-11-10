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


/**
 *	Tomcat
 *	
 */
public class ConfigTomcat extends Config
{

	/**
	 * 	ConfigTomcat
	 * 	@param data configuration
	 */
	public ConfigTomcat(ConfigurationData data) {
		super (data);
	}

	/**
	 * 	Initialize Defaults
	 */
	public void init()
	{
		p_data.setProperty(ConfigurationData.ADEMPIERE_APPS_DEPLOY, getDeployDir());
		p_data.setProperty(ConfigurationData.ADEMPIERE_JNP_PORT, "0");
		p_data.setProperty(ConfigurationData.ADEMPIERE_WEB_PORT,"8888");
		p_data.setProperty(ConfigurationData.ADEMPIERE_SSL_PORT,"4444");

		//
		enable();

	}	//	init

	/**
	 * 	Get Deployment Dir
	 *	@return deployment dir
	 */
	public String getDeployDir()
	{
		return p_data.getProperty(ConfigurationData.ADEMPIERE_HOME)
			+ File.separator + "tomcat"
			+ File.separator + "webapps";
	}	//	getDeployDir
	
	/**
	 * 	Test
	 *	@return error message or null if OK
	 */
	public String test()
	{
		//	AppsServer
		if (!testAppsServer())
			return error;

		//	Deployment Dir
		if (!testServerDeployDir(getDeployDir()))
			return error;		
		
		//	JNP Port - no test

		//	Web Port
		if (!testServerWebPort())
			return error;
		
		//	SSL Port
		if (!testServerSSLPort())
			return error;
		//
		return null;
	}	//	test

	@Override
	public void enable() {
		
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_APPS_DEPLOY, true);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_JNP_PORT, false);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_WEB_PORT, true);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_SSL_PORT, true);
			
	}
	
}	//	ConfigJBoss
