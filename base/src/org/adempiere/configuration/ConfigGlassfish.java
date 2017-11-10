/**********************************************************************
* This file is part of Adempiere ERP Bazaar                           *
* http://www.adempiere.org                                            *
*                                                                     *
* Copyright (C) Praneet Tiwari.                                       *
* Copyright (C) Contributors                                          *
*                                                                     *
* This program is free software; you can redistribute it and/or       *
* modify it under the terms of the GNU General Public License         *
* as published by the Free Software Foundation; either version 2      *
* of the License, or (at your option) any later version.              *
*                                                                     *
* This program is distributed in the hope that it will be useful,     *
* but WITHOUT ANY WARRANTY; without even the implied warranty of      *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
* GNU General Public License for more details.                        *
*                                                                     *
* You should have received a copy of the GNU General Public License   *
* along with this program; if not, write to the Free Software         *
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
* MA 02110-1301, USA.                                                 *
*                                                                     *
* Contributors:                                                       *
* - Trifon Trifonov (trifonnt@users.sourceforge.net)                  *
*                                                                     *
* Sponsors:                                                           *
* - D3 Soft (http://www.d3-soft.com)                                  *
***********************************************************************/

package org.adempiere.configuration;

import java.io.File;

/**
 *	GlassFish v2UR1 Apps Server Configuration
 *	
 *  @author Praneet Tiwari
 *  @author Trifon Trifonov
 *  @version $Id:  $
 */

public class ConfigGlassfish extends Config {

	
	public ConfigGlassfish(ConfigurationData data) {
		super(data);
	}

	/**
	 * 	Initialize
	 */
	public void init()
	{
		
		p_data.setProperty(ConfigurationData.ADEMPIERE_APPS_DEPLOY,getDeployDir());
		p_data.setProperty(ConfigurationData.ADEMPIERE_JNP_PORT,"3700");
		p_data.setProperty(ConfigurationData.ADEMPIERE_WEB_PORT,"8080");
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
		// TODO - check deployment directory
		return p_data.getProperty(ConfigurationData.ADEMPIERE_HOME) + File.separator + "glassfish";
                /*Commented for now
			+ File.separator + "glassfish"
			+ File.separator + "domains"
			+ File.separator + "domain1" ;
                 * */
	}	//	getDeployDir
	
	/**
	 * 	Test - Panel may or may not exist.
	 *	@return error message or null if OK
	 */
	public String test()
	{
		
		if (!testAppsServer())
			return error;
		
		//  Deployment Directory - no test
		
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

		p_data.setIsUsed(ConfigurationData.ADEMPIERE_APPS_DEPLOY, false);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_JNP_PORT, true);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_WEB_PORT, true);
		p_data.setIsUsed(ConfigurationData.ADEMPIERE_SSL_PORT, true);

	}
	
}	//	ConfigGlassfish

