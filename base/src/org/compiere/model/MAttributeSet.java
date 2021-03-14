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
package org.compiere.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.DBException;
import org.compiere.util.CCache;
import org.compiere.util.DB;

/**
 *  Product Attribute Set
 *
 *	@author Jorg Janke
 *	@version $Id: MAttributeSet.java,v 1.3 2006/07/30 00:51:05 jjanke Exp $
 *
 * @author Teo Sarca, www.arhipac.ro
 *			<li>FR [ 2214883 ] Remove SQL code and Replace for Query
 *
 *  @author mckayERP, www.mckayerp.com
 *  		<li> #254 MAttributeSet.getMAttributes sets isInstanceAttribute incorrectly 
 *  		<li> #255 MAttributeSet.isExcludeSerNo returns incorrect value
 *  		<li> #256 MAttributeSet add convenience function to return all attributes of a set. 
 */
public class MAttributeSet extends X_M_AttributeSet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2703536167929259405L;


	/**
	 * 	Get MAttributeSet from Cache
	 *	@param ctx context
	 *	@param M_AttributeSet_ID id
	 *	@return MAttributeSet
	 */
	public static MAttributeSet get (Properties ctx, int M_AttributeSet_ID)
	{
		return get(ctx, M_AttributeSet_ID, null);
	}
		
   /**
     *  Get MAttributeSet from Cache
     *  @param ctx context
     *  @param attributeSetId id
     *  @param trxName the Transaction name
     *  @return MAttributeSet
     */
    public static MAttributeSet get (Properties ctx, int attributeSetId, String trxName)
    {

		Integer key = Integer.valueOf(attributeSetId);
		MAttributeSet retValue = s_cache.get (key);
		if (retValue != null)
			return retValue;
		retValue = new MAttributeSet (ctx, attributeSetId, trxName);
		if (retValue.get_ID () != 0)
			s_cache.put (key, retValue);
		return retValue;
	}	//	get

	/**	Cache						*/
	private static CCache<Integer,MAttributeSet> s_cache
		= new CCache<Integer,MAttributeSet> ("M_AttributeSet", 20);
	
	public static void clearCache() {
		s_cache.clear();
	}
	
	/**
	 * 	Standard constructor
	 *	@param ctx context
	 *	@param M_AttributeSet_ID id
	 *	@param trxName transaction
	 */
	public MAttributeSet (Properties ctx, int M_AttributeSet_ID, String trxName)
	{
		super (ctx, M_AttributeSet_ID, trxName);
		if (M_AttributeSet_ID == 0)
		{
		//	setName (null);
			setIsGuaranteeDate (false);
			setIsGuaranteeDateMandatory (false);
			setIsLot (false);
			setIsLotMandatory (false);
			setIsSerNo (false);
			setIsSerNoMandatory (false);
			setIsInstanceAttribute(false);
			setMandatoryType (MANDATORYTYPE_NotMandatory);
		}
	}	//	MAttributeSet

	/**
	 * 	Load constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MAttributeSet (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MAttributeSet

	/**	Instance Attributes					*/
	private MAttribute[]		m_instanceAttributes = null;
	/**	Instance Attributes					*/
	private MAttribute[]		m_productAttributes = null;
	
	/** Entry Exclude						*/
	private HashMap<Integer, Boolean> m_excludes = new HashMap<Integer,Boolean>();
	/** Lot create Exclude						*/
	private X_M_LotCtlExclude[] 	m_excludeLots = null;
	/** Serial No create Exclude				*/
	private X_M_SerNoCtlExclude[]	m_excludeSerNos = null;

	/**
	 * 	Get Attribute Array 
	 *	@return attribute array
	 */
	public MAttribute[] getMAttributes ()
	{
		getMAttributes(true);
		getMAttributes(false);
		
		int instanceLength = m_instanceAttributes.length;
		int productLength = m_productAttributes.length;
		
		// Order is important - instance first, then product.
		MAttribute[] allAttributes = new MAttribute[instanceLength + productLength];
		System.arraycopy(m_instanceAttributes, 0, allAttributes, 0, instanceLength);
		System.arraycopy(m_productAttributes, 0, allAttributes, instanceLength, productLength);
		
		return allAttributes;
	}
	
	/**
	 * 	Get Attribute Array in order of attribute use sequence
	 * 	@param instanceAttributes true if for instance
	 *	@return instance or product attribute array
	 */
	public MAttribute[] getMAttributes (boolean instanceAttributes)
	{
		if ((m_instanceAttributes == null && instanceAttributes)
			|| m_productAttributes == null && !instanceAttributes)
		{
			String sql = "SELECT mau.M_Attribute_ID "
				+ "FROM M_AttributeUse mau"
				+ " INNER JOIN M_Attribute ma ON (mau.M_Attribute_ID=ma.M_Attribute_ID) "
				+ "WHERE mau.IsActive='Y' AND ma.IsActive='Y'"
				+ " AND mau.M_AttributeSet_ID=? AND ma.IsInstanceAttribute=? "	//	#1,2
				+ "ORDER BY mau.SeqNo";
			ArrayList<MAttribute> list = new ArrayList<MAttribute>();
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement(sql, get_TrxName());
				pstmt.setInt(1, getM_AttributeSet_ID());
				pstmt.setString(2, instanceAttributes ? "Y" : "N");
				rs = pstmt.executeQuery();
				while (rs.next())
				{
					MAttribute ma = new MAttribute (getCtx(), rs.getInt(1), get_TrxName());
					list.add (ma);
				}
			}
			catch (SQLException ex)
			{
				throw new DBException(ex, sql);
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null; pstmt = null;
			}
			
			//	Differentiate attributes
			if (instanceAttributes)
			{
				m_instanceAttributes = new MAttribute[list.size()];
				list.toArray (m_instanceAttributes);
			}
			else
			{
				m_productAttributes = new MAttribute[list.size()];
				list.toArray (m_productAttributes);
			}
		}		
		//	Return
		if (instanceAttributes)
			return m_instanceAttributes;
		return m_productAttributes;
	}	//	getMAttributes

	/**
	 * 	Something is Mandatory
	 *	@return true if something is mandatory
	 */
	public boolean isMandatory()
	{
		return !MANDATORYTYPE_NotMandatory.equals(getMandatoryType())
			|| isLotMandatory()
			|| isSerNoMandatory()
			|| isGuaranteeDateMandatory();
	}	//	isMandatory
	
	/**
	 * 	Is always mandatory
	 *	@return mandatory 
	 */
	public boolean isMandatoryAlways()
	{
		return MANDATORYTYPE_AlwaysMandatory.equals(getMandatoryType());
	}	//	isMandatoryAlways
	
	/**
	 * 	Is Mandatory when Shipping
	 *	@return true if required for shipping
	 */
	public boolean isMandatoryShipping()
	{
		return MANDATORYTYPE_AlwaysMandatory.equals(getMandatoryType()) 
				|| MANDATORYTYPE_WhenShipping.equals(getMandatoryType());
	}	//	isMandatoryShipping

	/**
	 * 	Exclude entry
	 *	@param tableId tableId
	 *	@param isSOTrx sales order
	 *	@return true if excluded
	 */
	public boolean excludeEntry (int tableId , boolean isSOTrx)
	{
		
		if (tableId == 0)
			return false;
		
		final StringBuilder whereClause = new StringBuilder();
		whereClause.append(X_M_AttributeSetExclude.COLUMNNAME_M_AttributeSet_ID).append("=? AND ");
		whereClause.append(X_M_AttributeSetExclude.COLUMNNAME_AD_Table_ID).append("=? AND ");
		whereClause.append(X_M_AttributeSetExclude.COLUMNNAME_IsSOTrx).append("=?");

		return new Query(getCtx(), X_M_AttributeSetExclude.Table_Name, whereClause.toString(), get_TrxName())
				.setParameters(getM_AttributeSet_ID(), tableId , isSOTrx)
				.setOnlyActiveRecords(true)
				.match();

	}	//	excludeEntry

	
	/**
	 * 	Exclude Lot creation
	 *	@param AD_Column_ID column
	 *	@param isSOTrx SO
	 *	@return true if excluded
	 */
	public boolean isExcludeLot (int AD_Column_ID, boolean isSOTrx)
	{
		
		if (getM_LotCtl_ID() == 0)
			return false;
		
		if (m_excludeLots == null)
		{
			final String whereClause = X_M_LotCtlExclude.COLUMNNAME_M_LotCtl_ID+"=?";
			List<X_M_LotCtlExclude> list = new Query(getCtx(), X_M_LotCtlExclude.Table_Name, whereClause, get_TrxName())
			.setParameters(getM_LotCtl_ID())
			.setOnlyActiveRecords(true)
			.list();
			m_excludeLots = new X_M_LotCtlExclude[list.size ()];
			list.toArray (m_excludeLots);
		}
		//	Find it
		if (m_excludeLots != null && m_excludeLots.length > 0)
		{
			MColumn column = MColumn.get(getCtx(), AD_Column_ID);
			for (int i = 0; i < m_excludeLots.length; i++)
			{
				if (m_excludeLots[i].getAD_Table_ID() == column.getAD_Table_ID()
					&& m_excludeLots[i].isSOTrx() == isSOTrx)
					return true;
			}
		}
		return false;
		
	}	//	isExcludeLot
	
	/**
	 *	Exclude SerNo creation
	 *	@param AD_Column_ID column
	 *	@param isSOTrx SO
	 *	@return true if excluded
	 */
	public boolean isExcludeSerNo (int AD_Column_ID, boolean isSOTrx)
	{
		if (getM_SerNoCtl_ID() == 0)
			return false;  // serial numbers could be manually entered.
		
		if (m_excludeSerNos == null)
		{
			final String whereClause = X_M_SerNoCtlExclude.COLUMNNAME_M_SerNoCtl_ID+"=?";
			List<X_M_SerNoCtlExclude> list = new Query(getCtx(), X_M_SerNoCtlExclude.Table_Name, whereClause, get_TrxName())
			.setParameters(getM_SerNoCtl_ID())
			.setOnlyActiveRecords(true)
			.list();
			m_excludeSerNos = new X_M_SerNoCtlExclude[list.size ()];
			list.toArray (m_excludeSerNos);
		}
		//	Find it
		if (m_excludeSerNos != null && m_excludeSerNos.length > 0)
		{
			MColumn column = MColumn.get(getCtx(), AD_Column_ID);
			for (int i = 0; i < m_excludeSerNos.length; i++)
			{
				if (m_excludeSerNos[i].getAD_Table_ID() == column.getAD_Table_ID()
					&& m_excludeSerNos[i].isSOTrx() == isSOTrx)
					return true;
			}
		}
		return false;
	}	//	isExcludeSerNo

	/**
	 * 	Get Lot Char Start
	 *	@return defined or \u00ab 
	 */
	public String getLotCharStart()
	{
		String s = super.getLotCharSOverwrite ();
		if (s != null && s.length() == 1 && !s.equals(" "))
			return s;
		return "\u00ab";
	}	//	getLotCharStart

	/**
	 * 	Get Lot Char End
	 *	@return defined or \u00bb 
	 */
	public String getLotCharEnd()
	{
		String s = super.getLotCharEOverwrite ();
		if (s != null && s.length() == 1 && !s.equals(" "))
			return s;
		return "\u00bb";
	}	//	getLotCharEnd
	
	/**
	 * 	Get SerNo Char Start
	 *	@return defined or #
	 */
	public String getSerNoCharStart()
	{
		String s = super.getSerNoCharSOverwrite ();
		if (s != null && s.length() == 1 && !s.equals(" "))
			return s;
		return "#";
	}	//	getSerNoCharStart

	/**
	 * 	Get SerNo Char End
	 *	@return defined or none
	 */
	public String getSerNoCharEnd()
	{
		String s = super.getSerNoCharEOverwrite ();
		if (s != null && s.length() == 1 && !s.equals(" "))
			return s;
		return "";
	}	//	getSerNoCharEnd
	
	
	/**
	 * 	Before Save.
	 * 	- set instance attribute flag
	 *	@param newRecord new
	 *	@return true
	 */
	protected boolean beforeSave (boolean newRecord)
	{

		boolean hasInstanceAttributes = false;
		boolean hasMandatoryAttributes = false;

		if (!newRecord)
		{
			String sql = "SELECT * FROM M_AttributeUse mau"
						+ " INNER JOIN M_Attribute ma ON (mau.M_Attribute_ID=ma.M_Attribute_ID) "
						+ "WHERE mau.M_AttributeSet_ID= ?"
						+ " AND mau.IsActive='Y' AND ma.IsActive='Y'"
						+ " AND ma.IsInstanceAttribute='Y'";
			
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			pstmt = DB.prepareStatement(sql, get_TrxName());
			try {
				pstmt.setInt(1, getM_AttributeSet_ID());
				rs = pstmt.executeQuery();
				if (rs.next())
				{
					hasInstanceAttributes = true;
				}
			} catch (SQLException e) {
				log.saveError("Error", e.getMessage());
				return false;
			}
			
			sql = "SELECT * FROM M_AttributeUse mau"
					+ " INNER JOIN M_Attribute ma ON (mau.M_Attribute_ID=ma.M_Attribute_ID) "
					+ "WHERE mau.M_AttributeSet_ID= ?"
					+ " AND mau.IsActive='Y' AND ma.IsActive='Y'"
					+ " AND ma.IsMandatory='Y'";
			
			pstmt = DB.prepareStatement(sql, get_TrxName());
			try {
				pstmt.setInt(1, getM_AttributeSet_ID());
				rs = pstmt.executeQuery();
				if (rs.next())
				{
					hasMandatoryAttributes = true;
				}
			} catch (SQLException e) {
				log.saveError("Error", e.getMessage());
				return false;
			}
			
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		
		setIsInstanceAttribute(isSerNo() || isLot() || isGuaranteeDate() || hasInstanceAttributes);
		
		if (MAttributeSet.MANDATORYTYPE_NotMandatory.equals(getMandatoryType())
			&& (hasMandatoryAttributes || isSerNoMandatory() || isLotMandatory() || isGuaranteeDateMandatory()))
		{
			setMandatoryType(MAttributeSet.MANDATORYTYPE_AlwaysMandatory);
		}
		
		return true;
		
	}	//	beforeSave
	
	
	/**
	 * 	After Save.
	 * 	- Verify Instance Attribute
	 *	@param newRecord new
	 *	@param success success
	 *	@return success
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		return success;
	}	//	afterSave

	
	private static void checkAndSetIsInstanceAttribute(String trxName) {

		String sql;
		
		sql = "UPDATE M_AttributeSet mas "
				+ "SET IsInstanceAttribute='Y' "
				+ "WHERE IsInstanceAttribute='N'"
				+ "	AND (IsSerNo='Y' OR IsLot='Y' OR IsGuaranteeDate='Y'"
				+ " OR EXISTS (SELECT * FROM M_AttributeUse mau"
					+ " INNER JOIN M_Attribute ma ON (mau.M_Attribute_ID=ma.M_Attribute_ID) "
					+ "WHERE mau.M_AttributeSet_ID=mas.M_AttributeSet_ID"
					+ " AND mau.IsActive='Y' AND ma.IsActive='Y'"
					+ " AND ma.IsInstanceAttribute='Y'))";
		DB.executeUpdate(sql, trxName);

		sql = "UPDATE M_AttributeSet mas "
				+ "SET IsInstanceAttribute='N' "
				+ "WHERE IsInstanceAttribute='Y'"
				+ "	AND IsSerNo='N' AND IsLot='N' AND IsGuaranteeDate='N'"
				+ " AND NOT EXISTS (SELECT * FROM M_AttributeUse mau"
					+ " INNER JOIN M_Attribute ma ON (mau.M_Attribute_ID=ma.M_Attribute_ID) "
					+ "WHERE mau.M_AttributeSet_ID=mas.M_AttributeSet_ID"
					+ " AND mau.IsActive='Y' AND ma.IsActive='Y'"
					+ " AND ma.IsInstanceAttribute='Y')";
		DB.executeUpdate(sql, trxName);

	}

	private static void checkAndSetMandatoryType(String trxName) {

		String sql;
		
		sql = "UPDATE M_AttributeSet mas "
				+ "SET MandatoryType ='Y' "
				+ "WHERE MandatoryType ='N'"
				+ "	AND (IsSerNoMandatory='Y' OR IsLotMandatory='Y' OR IsGuaranteeDateMandatory='Y'"
				+ " OR EXISTS (SELECT * FROM M_AttributeUse mau"
					+ " INNER JOIN M_Attribute ma ON (mau.M_Attribute_ID=ma.M_Attribute_ID) "
					+ "WHERE mau.M_AttributeSet_ID=mas.M_AttributeSet_ID"
					+ " AND mau.IsActive='Y' AND ma.IsActive='Y'"
					+ " AND ma.IsMandatory='Y'))";
		DB.executeUpdate(sql, trxName);

		sql = "UPDATE M_AttributeSet mas "
				+ "SET MandatoryType='N' "
				+ "WHERE MandatoryType NOT IN ('N','S')"
				+ "	AND IsSerNoMandatory='N' AND IsLotMandatory='N' AND IsGuaranteeDateMandatory='N'"
				+ " AND NOT EXISTS (SELECT * FROM M_AttributeUse mau"
					+ " INNER JOIN M_Attribute ma ON (mau.M_Attribute_ID=ma.M_Attribute_ID) "
					+ "WHERE mau.M_AttributeSet_ID=mas.M_AttributeSet_ID"
					+ " AND mau.IsActive='Y' AND ma.IsActive='Y'"
					+ " AND ma.IsMandatory='Y')";
		DB.executeUpdate(sql, trxName);

	}

	public static void updateMAttributeSets(String trxName) {
		
		checkAndSetIsInstanceAttribute(trxName);
		checkAndSetMandatoryType(trxName);
		
	}
	
	public void clearMAttributeCache() {
		this.m_instanceAttributes = null;
		this.m_productAttributes = null;
	}
	
}	//	MAttributeSet
