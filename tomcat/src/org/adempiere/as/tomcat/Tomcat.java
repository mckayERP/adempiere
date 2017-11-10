/******************************************************************************
 * Copyright (C) 2017 ADempiere Foundation, All Rights Reserved.              *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2  or (at your option) any later version           *
 * of the GNU General Public License as published by the Free Software        *
 * Foundation. This program is distributed in the hope that it will be useful,*
 * but WITHOUT ANY WARRANTY, without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General   *
 * Public License for more details.                                           *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
package org.adempiere.as.tomcat;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.adempiere.as.IApplicationServer;
import org.adempiere.configuration.ConfigurationData;
import org.compiere.util.CLogger;

/**
 * @author Michael McKay, MckayERP.com
 *
 */
public class Tomcat implements IApplicationServer {
	
	CLogger log = CLogger.getCLogger(this.getClass());
	
	LocalContext localContext = null;
	
	/**
	 * @see IApplicationServer#getInitialContextEnvironment(String, int, String, String)
	 */

	@SuppressWarnings("unchecked")
	public Hashtable<String, String> getInitialContextEnvironment(
			String AppsHost, int AppsPort, String principal, String credential) {
		
		if (localContext == null)
			try
			{
				localContext = LocalContextFactory.createLocalContext();
			}
			catch (NamingException e)
			{
				log.severe(e.getMessage());
			}
		
		Hashtable<String,String> env = new Hashtable<String,String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "org.adempiere.as.tomcat.LocalContextFactory");
				
		try {
			Context initialContext = new InitialContext(env);
			env = (Hashtable<String, String>) initialContext.getEnvironment();
			
		} catch (NamingException e) {
			e.printStackTrace();
		}

		return env;
	}

	public int getDefaultNamingServicePort() {
		return 8080;  // same as web port?
	}

	@Override
	public boolean canGetInitialContext() {
		// tomcat allows initialContext();
		return false;
	}
}
