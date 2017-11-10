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
package org.compiere.grid;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

import org.compiere.model.Lookup;
import org.compiere.util.DisplayType;
import org.compiere.util.KeyNamePair;
import org.compiere.util.NamePair;
import org.compiere.util.ValueNamePair;


/**
 *  A simple lookup holding a single array of strings
 */
public class SimpleLookup extends Lookup
{

	
	private String[] m_data;
	private String m_columnName;
	
	private SimpleLookupCallback m_callback;
	
	/**
	 *	Manual Lookup with a static string array.
	 * 	@param The String array to load.
	 */
	public SimpleLookup(String columnName, String[] data)
	{
		super(DisplayType.TableDir, 0);
		m_data = data;
		Method dataCallback = null;
		m_columnName = columnName;
	}	
	
	public SimpleLookup(String columnName, SimpleLookupCallback callback)
	{
		super(DisplayType.TableDir, 0);
		m_callback = callback;
				
		m_columnName = columnName;
	}	
	


	/**
	 *	Get Display String of key value
	 * 	@param key key
	 * 	@return display
	 */
	public String getDisplay (Object key)
	{
		//  linear search in m_data
		for (int i = 0; i < p_data.size(); i++)
		{
			Object oo = p_data.get(i);
			if (oo != null && oo instanceof String && key instanceof String)
			{
				if (((String) key).equals((String) oo))
					return (String) key;
			}
		}
		return "<" + key + ">";
	}	//	getDisplay

	/**
	 *  The Lookup contains the key
	 * 	@param key key
	 * 	@return true if contains key
	 */
	public boolean containsKey (Object key)
	{
		//  linear search in p_data
		for (int i = 0; i < p_data.size(); i++)
		{
			Object oo = p_data.get(i);
			if (oo != null && oo instanceof String && key instanceof String)
			{
				if (((String) key).equals((String) oo))
					return true;
			}
		}
		return false;
	}   //  containsKey



	/**
	 *	Return data as sorted Array
	 * 	@param mandatory mandatory
	 * 	@param onlyValidated only validated
	 * 	@param onlyActive only active
	 * 	@param temporary force load for temporary display
	 * 	@return list of data
	 */
	public ArrayList<Object> getData (boolean mandatory, 
		boolean onlyValidated, boolean onlyActive, boolean temporary)
	{
		ArrayList<Object> list = new ArrayList<Object>();
		
		// m_data is initialized when the instance constructor is called OR
		// a callback is provided to get the data.  m_data could be null.
		
		if (m_callback != null)
		{
			m_data = m_callback.getLookupData(m_columnName);
		}
		
		if (m_data != null) 
		{
			for (String entry : m_data)
			{
				ValueNamePair np = new ValueNamePair(entry,entry);
				list.add(np);
			}
		}
		
		return list;
	}	//	getArray

	/**
	 *	Refresh Values (nop)
	 * 	@return number of cache
	 */
	public int refresh()
	{
		if (m_callback != null)
		{
			m_data = m_callback.getLookupData(m_columnName);
		}
		
		return m_data.length;
		
	}	//	refresh

	/**
	 *	Get underlying fully qualified Table.Column Name
	 * 	@return column name
	 */
	public String getColumnName()
	{
		return m_columnName;
	}   //  getColumnName


	@Override
	public NamePair get(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

}	//	XLookup
