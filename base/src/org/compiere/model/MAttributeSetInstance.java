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

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.TimeUtil;

/**
 *  Product Attribute Set Instance
 *
 *	@author Jorg Janke
 *	@version $Id: MAttributeSetInstance.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 *
 * @author Teo Sarca, www.arhipac.ro
 *			<li>BF [ 2675699 ] MAttributeSetInstance.create should create Lot/Serial/Guaran
 */
public class MAttributeSetInstance extends X_M_AttributeSetInstance
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7870720973216607658L;


	/**
	 * 	Get Attribute Set Instance from ID or Product
	 *	@param ctx context
	 * 	@param M_AttributeSetInstance_ID id or 0
	 * 	@param M_Product_ID required if id is 0
	 * 	@return Attribute Set Instance or null if error
	 */
	public static MAttributeSetInstance get (Properties ctx, 
		int M_AttributeSetInstance_ID, int M_Product_ID)
	{
		MAttributeSetInstance retValue = null;
		//	Load Instance if not 0
		if (M_AttributeSetInstance_ID != 0)
		{
			s_log.fine("From M_AttributeSetInstance_ID=" + M_AttributeSetInstance_ID);
			return new MAttributeSetInstance (ctx, M_AttributeSetInstance_ID, null);
		}
		//	Get new from Product
		s_log.fine("From M_Product_ID=" + M_Product_ID);
		if (M_Product_ID == 0)
			return null;
		String sql = "SELECT M_AttributeSet_ID, M_AttributeSetInstance_ID "
			+ "FROM M_Product "
			+ "WHERE M_Product_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, M_Product_ID);
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				int M_AttributeSet_ID = rs.getInt(1);
			//	M_AttributeSetInstance_ID = rs.getInt(2);	//	needed ?
				//
				retValue = new MAttributeSetInstance (ctx, 0, M_AttributeSet_ID, null);
			}
		}
		catch (SQLException ex)
		{
			s_log.log(Level.SEVERE, sql, ex);
			retValue = null;
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		//
		return retValue;
	}	//	get

	private static CLogger		s_log = CLogger.getCLogger (MAttributeSetInstance.class);

	
	/**************************************************************************
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_AttributeSetInstance_ID id
	 *	@param trxName transaction
	 */
	public MAttributeSetInstance (Properties ctx, int M_AttributeSetInstance_ID, String trxName)
	{
		super (ctx, M_AttributeSetInstance_ID, trxName);
		if (M_AttributeSetInstance_ID == 0)
		{
		}
	}	//	MAttributeSetInstance

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MAttributeSetInstance (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MAttributeSetInstance

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_AttributeSetInstance_ID id
	 * 	@param M_AttributeSet_ID attribute set
	 *	@param trxName transaction
	 */
	public MAttributeSetInstance (Properties ctx, int M_AttributeSetInstance_ID, 
		int M_AttributeSet_ID, String trxName)
	{
		this (ctx, M_AttributeSetInstance_ID, trxName);
		setM_AttributeSet_ID(M_AttributeSet_ID);
	}	//	MAttributeSetInstance

	/**	Attribute Set				*/
	private MAttributeSet 		m_mas = null;
	/**	Date Format					*/
	private DateFormat			m_dateFormat = DisplayType.getDateFormat(DisplayType.Date);

	/**
	 * 	Set Attribute Set
	 * 	@param mas attribute set
	 */
	public void setMAttributeSet (MAttributeSet mas)
	{
		m_mas = mas;
		setM_AttributeSet_ID(mas.getM_AttributeSet_ID());
	}	//	setAttributeSet

	/**
	 * 	Get Attribute Set
	 *	@return Attrbute Set or null
	 */
	public MAttributeSet getMAttributeSet()
	{
		if (m_mas == null && getM_AttributeSet_ID() != 0)
			m_mas = new MAttributeSet (getCtx(), getM_AttributeSet_ID(), get_TrxName());
		return m_mas;
	}	//	getMAttributeSet

	/**
	 * 	Set Description.
	 * 	- Product Values
	 * 	- Instance Values
	 * 	- SerNo	= #123
	 *  - Lot 	= \u00ab123\u00bb
	 *  - GuaranteeDate	= 10/25/2003
	 */
	public void setDescription()
	{
		//	Make sure we have a Attribute Set
		getMAttributeSet();
		if (m_mas == null)
		{
			setDescription ("");
			return;
		}
		
		StringBuffer sb = new StringBuffer();
		
		//	Instance Attribute Values
		MAttribute[] attributes = m_mas.getMAttributes(true);
		for (int i = 0; i < attributes.length; i++)
		{
			MAttributeInstance mai = attributes[i].getMAttributeInstance(getM_AttributeSetInstance_ID());
			if (mai != null && mai.getValue() != null)
			{
				if (sb.length() > 0)
					sb.append("_");
				sb.append(mai.getValue());
			}
		}
		//	SerNo
		if (m_mas.isSerNo() && getSerNo() != null)
		{
			if (sb.length() > 0)
				sb.append("_");
			sb.append(m_mas.getSerNoCharStart()).append(getSerNo()).append(m_mas.getSerNoCharEnd());
		}
		//	Lot
		if (m_mas.isLot() && getLot() != null)
		{
			if (sb.length() > 0)
				sb.append("_");
			sb.append(m_mas.getLotCharStart()).append(getLot()).append(m_mas.getLotCharEnd());
		}
		//	GuaranteeDate
		if (m_mas.isGuaranteeDate() && getGuaranteeDate() != null)
		{
			if (sb.length() > 0)
				sb.append("_");
			sb.append (m_dateFormat.format(getGuaranteeDate()));
		}

		//	Product Attribute Values
		attributes = m_mas.getMAttributes(false);
		for (int i = 0; i < attributes.length; i++)
		{
			MAttributeInstance mai = attributes[i].getMAttributeInstance(getM_AttributeSetInstance_ID());
			if (mai != null && mai.getValue() != null)
			{
				if (sb.length() > 0)
					sb.append("_");
				sb.append(mai.getValue());
			}
		}
		setDescription (sb.toString());
	}	//	setDescription


	/**
	 * 	Get Guarantee Date
	 * 	@param getNew if true calculates/sets guarantee date
	 *	@return guarantee date or null if days = 0
	 */
	public Timestamp getGuaranteeDate(boolean getNew)
	{
		if (getNew)
		{
			int days = getMAttributeSet().getGuaranteeDays();
			if (days > 0)
			{
				Timestamp ts = TimeUtil.addDays(new Timestamp(System.currentTimeMillis()), days);
				setGuaranteeDate(ts);
			}
		}
		return getGuaranteeDate();
	}	//	getGuaranteeDate

	/**
	 * 	Get Lot No
	 * 	@param getNew if true create/set new lot
	 * 	@param M_Product_ID product used if new
	 *	@return lot
	 */
	public String getLot (boolean getNew, int M_Product_ID)
	{
		if (getNew)
			createLot(M_Product_ID);
		return getLot();
	}	//	getLot

	/**
	 * 	Create Lot
	 * 	@param M_Product_ID product used if new
	 *	@return lot info
	 */
	public KeyNamePair createLot (int M_Product_ID)
	{
		KeyNamePair retValue = null;
		int M_LotCtl_ID = getMAttributeSet().getM_LotCtl_ID();
		if (M_LotCtl_ID != 0)
		{
			MLotCtl ctl = new MLotCtl (getCtx(), M_LotCtl_ID, null);
			MLot lot = ctl.createLot(M_Product_ID);
			setM_Lot_ID (lot.getM_Lot_ID());
			setLot (lot.getName());
			retValue = new KeyNamePair (lot.getM_Lot_ID(), lot.getName());	
		}
		return retValue;
	}	//	createLot
	
	/**
	 * 	To to find lot and set Lot/ID
	 *	@param Lot lot
	 *	@param M_Product_ID product
	 */
	public void setLot (String Lot, int M_Product_ID)
	{
		//	Try to find it
		MLot mLot = MLot.getProductLot(getCtx(), M_Product_ID, Lot, get_TrxName());
		if (mLot != null)
			setM_Lot_ID(mLot.getM_Lot_ID());
		setLot (Lot);
	}	//	setLot

	/**
	 * 	Exclude Lot creation
	 *	@param AD_Column_ID column
	 *	@param isSOTrx SO
	 *	@return true if excluded
	 */
	public boolean isExcludeLot (int AD_Column_ID, boolean isSOTrx)
	{
		getMAttributeSet();
		if (m_mas != null)
			return m_mas.isExcludeLot (AD_Column_ID, isSOTrx);
		return false;
	}	//	isExcludeLot

	/**
	 *	Get Serial No
	 * 	@param getNew if true create/set new Ser No
	 *	@return Serial Number
	 */
	public String getSerNo (boolean getNew)
	{
		if (getNew)
		{
			int M_SerNoCtl_ID = getMAttributeSet().getM_SerNoCtl_ID();
			if (M_SerNoCtl_ID != 0)
			{
				MSerNoCtl ctl = new MSerNoCtl (getCtx(), M_SerNoCtl_ID, get_TrxName());
				setSerNo(ctl.createSerNo());
			}
		}
		return getSerNo();
	}	//	getSerNo

	/**
	 *	Exclude SerNo creation
	 *	@param AD_Column_ID column
	 *	@param isSOTrx SO
	 *	@return true if excluded
	 */
	public boolean isExcludeSerNo (int AD_Column_ID, boolean isSOTrx)
	{
		getMAttributeSet();
		if (m_mas != null)
			return m_mas.isExcludeSerNo (AD_Column_ID, isSOTrx);
		return false;
	}	//	isExcludeSerNo

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) 
	{
		if (super.afterSave(newRecord, success)) 
		{
			if (newRecord && success)
			{
				//use id as description when description is empty
				String desc = this.getDescription();
				if (desc == null || desc.trim().length() == 0)
				{
					this.set_ValueNoCheck("Description", Integer.toString(getM_AttributeSetInstance_ID()));
					String sql = "UPDATE M_AttributeSetInstance SET Description = ? WHERE M_AttributeSetInstance_ID = ?";
					int no = DB.executeUpdateEx(sql, 
							new Object[]{Integer.toString(getM_AttributeSetInstance_ID()), getM_AttributeSetInstance_ID()}, 
							get_TrxName());
					if (no <= 0)
					{
						log.log(Level.SEVERE, "Failed to update description.");
						return false;
					}
				}
			}
			return true;
		}
		
		return false;
	}
	
	/**
	 * Create & save a new ASI for given product.
	 * Automatically creates Lot#, Serial# and Guarantee Date.
	 * @param ctx
	 * @param product
	 * @param trxName
	 * @return newly created ASI
	 */
	public static MAttributeSetInstance create(Properties ctx, MProduct product, String trxName)
	{
		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		asi.setClientOrg(product.getAD_Client_ID(), 0);
		asi.setM_AttributeSet_ID(product.getM_AttributeSet_ID());
		// Create new Lot, Serial# and Guarantee Date
		if (asi.getM_AttributeSet_ID() > 0)
		{
			asi.getLot(true, product.get_ID());
			asi.getSerNo(true);
			asi.getGuaranteeDate(true);
		}
		//
		asi.saveEx();
		return asi;
	}
	
	/**
	 * 	Called before Save for Pre-Save Operation.   
	 * 	@param newRecord new record
	 *  @return true if record can be saved
	 */
	protected boolean beforeSave(boolean newRecord)
	{
		// Fix description
		if (newRecord)
			return true;
		
		if ( getDescription() == get_ValueOld("Description") )
		{
			setDescription();
		}

		return true;
	}	//	beforeSave

	/**
	 * Determines if the values of an attribute set instance match the provided set of values.
	 * @param values in the order provided by MAttributeSet.getMAttributes()
	 * @return true if the values provided match those of this attribute set.
	 */
	public boolean hasValues(Object[] values) {
		
		if (values == null)
			return false;
		
		MAttributeSet as = this.getMAttributeSet();
		if (as == null)
			return false;
		
		MAttribute[] attributes = as.getMAttributes();
		
		if (attributes.length != values.length)
			return false;
		
		for (int i = 0; i < attributes.length; i++) {
			MAttribute attribute = attributes[i];
			Object value = values[i];
			
			if (attribute == null  && value == null)
				continue;
			else if (attribute == null && value != null)
				return false;
				
			log.finest(attribute.getName());
			MAttributeInstance instance = attribute.getMAttributeInstance (getM_AttributeSetInstance_ID());

			if (value != null && instance == null 
				|| value == null && instance != null)
				return false;
			
			if (value == null && instance == null)
				continue;
			
			// Check if the values match
			if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attribute.getAttributeValueType()))
			{
				if (((MAttributeValue) value).getM_AttributeValue_ID() != instance.getM_AttributeValue_ID()) {
					return false;
				}
			}
			else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attribute.getAttributeValueType()))
			{
				if (((BigDecimal) value).compareTo(instance.getValueNumber()) != 0) {
					return false;
				}
			}
			else	//	Text Field
			{
				if (!((String) value).equals(instance.getValue())) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Finds an existing Attribute Set Instance that is identical to the provided set
	 * of values.  The match looks at the M_AttributeSetInstance and the 
	 * M_AttributeInstance tables for duplicates across the main fields. 
	 * 
	 * @return The M_AttributeSetInstance_ID of the first matching instance is returned.
	 */
	public int findMatchingASI(Object[] values) {
		// Test main ASI fields
		
		List<Object> parameters = new ArrayList<Object>();
		
		String where =  MAttributeSetInstance.COLUMNNAME_AD_Org_ID + "=?";
		parameters.add(this.getAD_Org_ID());
		//
		where += " AND " + MAttributeSetInstance.COLUMNNAME_M_AttributeSet_ID + "=?";
		parameters.add(this.getM_AttributeSet_ID());
		//
		if (this.getGuaranteeDate() != null) {
			where += " AND " + MAttributeSetInstance.COLUMNNAME_GuaranteeDate + "=?" ;
			parameters.add(this.getGuaranteeDate());
		}
		else
			where += " AND " + MAttributeSetInstance.COLUMNNAME_GuaranteeDate + " is null";
		//
		if (this.getLot() != null) {
			where += " AND " + MAttributeSetInstance.COLUMNNAME_Lot + "=?" ;
			parameters.add(this.getLot());
		}
		else
			where += " AND " + MAttributeSetInstance.COLUMNNAME_Lot + " is null";
		//
		if (this.getSerNo() != null) {
			where += " AND " + MAttributeSetInstance.COLUMNNAME_SerNo + "=?" ;
			parameters.add(this.getSerNo());
		}
		else
			where += " AND " + MAttributeSetInstance.COLUMNNAME_SerNo + " is null";
		//
		where += " AND " + MAttributeSetInstance.COLUMNNAME_M_AttributeSetInstance_ID + "!=?";			
		parameters.add(this.getM_AttributeSetInstance_ID());
		
		List<MAttributeSetInstance> matchingASIs = new Query(Env.getCtx(), MAttributeSetInstance.Table_Name, where, null)
						.setClient_ID()
						.setParameters(parameters)
						.setOrderBy(COLUMNNAME_M_AttributeSetInstance_ID + " ASC")
						.list();
		
		if (matchingASIs.size() == 0) {
			// No match of the Attribute Set Instance fields.  No need to test the values.
			return 0;
		}
		
		//  There is one or more matching ASIs. Test each for same attribute values
		//  and return the ID of the first match.
		for (MAttributeSetInstance match : matchingASIs) {
			if (match.hasValues(values))
				return match.getM_AttributeSetInstance_ID();
		}
		return 0;
	}

	/**
	 * Determines if the mandatory attributes of an attribute set are included in this instance.
	 * @return true if the mandatory attributes values exist.
	 */
	public boolean hasMandatoryValues() {
		
		MAttributeSet as = this.getMAttributeSet();
		if (as == null)
			return false;
		
		MAttribute[] attributes = as.getMAttributes();
		
		for (int i = 0; i < attributes.length; i++) {
			MAttribute attribute = attributes[i];
			
			if (attribute == null || !attribute.isMandatory())
				continue;
				
			log.finest(attribute.getName());
			MAttributeInstance instance = attribute.getMAttributeInstance (getM_AttributeSetInstance_ID());

			if (instance == null)
				return false;
		}

		// Else - assume the values are set
		return true;
	}	
}	//	MAttributeSetInstance
