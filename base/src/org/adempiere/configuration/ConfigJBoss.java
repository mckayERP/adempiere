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
 *	JBoss Server Configuration
 *	
 *  @author Jorg Janke
 *  @version $Id: ConfigJBoss.java,v 1.3 2006/07/30 00:57:42 jjanke Exp $
 */
public class ConfigJBoss extends Config
{


	/**
	 * 	ConfigJBoss
	 * 	@param data configuration
	 */
	public ConfigJBoss(ConfigurationData data) {
		super (data);
	}

	/**
	 * 	Initialize
	 */
	public void init()
	{

		p_data.setProperty(ConfigurationData.ADEMPIERE_APPS_DEPLOY, getDeployDir());
		p_data.setProperty(ConfigurationData.ADEMPIERE_JNP_PORT, "1099");
		p_data.setProperty(ConfigurationData.ADEMPIERE_WEB_PORT,"80");
		p_data.setProperty(ConfigurationData.ADEMPIERE_SSL_PORT,"443");

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
			+ File.separator + "jboss"
			+ File.separator + "server"
			+ File.separator + "adempiere" 
			+ File.separator + "deploy";
	}	//	getDeployDir
	
	/**
	 * 	Test - Panel may or may not exist.
	 *	@return error message or null if OK
	 */
	public String test()
	{
		if (!testAppsServer())
			return error;

		//	Deployment Dir
		if (!testServerDeployDir(getDeployDir()))
			return error;		
		
		//	JNP Port
		if (!testServerJNPPort())
			return error;

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
		
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_APPS_DEPLOY, false);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_JNP_PORT, true);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_WEB_PORT, true);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_SSL_PORT, true);
		
	}
	
}	//	ConfigJBoss
