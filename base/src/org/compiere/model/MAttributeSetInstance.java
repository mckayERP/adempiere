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

import static java.util.Objects.requireNonNull;

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

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;

/**
 *  Product Attribute Set Instance
 *
 *	@author Jorg Janke
 *	@version $Id: MAttributeSetInstance.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 *
 * @author Teo Sarca, www.arhipac.ro
 *			<li>BF [ 2675699 ] MAttributeSetInstance.create should create Lot/Serial/Guaran
 *
 *  @author mckayERP, www.mckayerp.com
 *  		<li>MAttributeSetInstance Improvements:
 *  		<li>  #258 Reduce duplication.
 *  		<li>  #260 Determine if all mandatory requirements are met
 *  		<li>  #261 Add beforeSave function
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
		return get(ctx, M_AttributeSetInstance_ID, M_Product_ID, null);
	}
	
	/**
	 * 	Get Attribute Set Instance from ID or Product
	 *	@param ctx context
	 * 	@param M_AttributeSetInstance_ID id or 0
	 * 	@param M_Product_ID required if id is 0
	 *  @param trxName - the transaction name to use
	 * 	@return Attribute Set Instance or null if error
	 */
	public static MAttributeSetInstance get (Properties ctx, 
		int M_AttributeSetInstance_ID, int M_Product_ID, String trxName)
	{
		MAttributeSetInstance retValue = null;
		//	Load Instance if not 0
		if (M_AttributeSetInstance_ID != 0)
		{
			s_log.fine("From M_AttributeSetInstance_ID=" + M_AttributeSetInstance_ID);
			return new MAttributeSetInstance (ctx, M_AttributeSetInstance_ID, trxName);
		}
		
		//	Get new from Product
		s_log.fine("From M_Product_ID=" + M_Product_ID);
		if (M_Product_ID == 0)
			return null;
		
		MProduct product = MProduct.get(ctx, M_Product_ID, trxName);
		
		if (product == null || product.getM_AttributeSet_ID() == 0)
			return null;
		
		return new MAttributeSetInstance (ctx, 0, product.getM_AttributeSet_ID(), trxName);
		
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
	 * 	@param M_AttributeSet_ID attribute to set
	 *	@param trxName transaction
	 */
	public MAttributeSetInstance (Properties ctx, int M_AttributeSetInstance_ID, 
		int M_AttributeSet_ID, String trxName)
	{
		this (ctx, M_AttributeSetInstance_ID, trxName);
		if (getM_AttributeSet_ID() == 0)
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
		
		if (mas == null || mas.get_ID() == 0)
		{
			setM_AttributeSet_ID(0);
			m_mas = null;
		}
		else
		{
			setM_AttributeSet_ID(mas.getM_AttributeSet_ID());
			m_mas = mas;
		}
		
	}	//	setAttributeSet

	/**
	 * 	Get Attribute Set
	 *	@return Attrbute Set or null
	 */
	public MAttributeSet getMAttributeSet()
	{
		if (getM_AttributeSet_ID() > 0)
		{
			if (m_mas == null || (m_mas != null && m_mas.getM_AttributeSet_ID() != getM_AttributeSet_ID()))
			{
				m_mas = new MAttributeSet (getCtx(), getM_AttributeSet_ID(), get_TrxName());
			}
		}
		else if (getM_AttributeSet_ID() <= 0)
		{
			m_mas = null;
		}
		return m_mas;
		
	}	//	getMAttributeSet
	
	public String getDescription() 
	{
		
		String description = super.getDescription();
		if (description == null || description.length() == 0)
		{
			description = setDescription();
		}
		
		return description;
		
	}

	/**
	 * 	Set Description.
	 * 	- Product Values
	 * 	- Instance Values
	 * 	- SerNo	= #123
	 *  - Lot 	= \u00ab123\u00bb
	 *  - GuaranteeDate	= 10/25/2003
	 */
	public String setDescription()
	{
		String description = null;
		
		//	Make sure we have a Attribute Set
		MAttributeSet attributeSet = getMAttributeSet();
		if (attributeSet != null)
		{
		
			StringBuffer sb = new StringBuffer();
			
			//	Instance Attribute Values
			MAttribute[] attributes = attributeSet.getMAttributes(true);
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
			if (attributeSet.isSerNo() && getSerNo() != null)
			{
				if (sb.length() > 0)
					sb.append("_");
				sb.append(attributeSet.getSerNoCharStart()).append(getSerNo()).append(attributeSet.getSerNoCharEnd());
			}
			//	Lot
			if (attributeSet.isLot() && getLot() != null)
			{
				if (sb.length() > 0)
					sb.append("_");
				sb.append(attributeSet.getLotCharStart()).append(getLot()).append(attributeSet.getLotCharEnd());
			}
			//	GuaranteeDate
			if (attributeSet.isGuaranteeDate() && getGuaranteeDate() != null)
			{
				if (sb.length() > 0)
					sb.append("_");
				sb.append (m_dateFormat.format(getGuaranteeDate()));
			}
	
			//	Product Attribute Values
			attributes = attributeSet.getMAttributes(false);
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
			description = sb.toString();
		}
		
		if (description == null || description.length() == 0)
		{
			description = Integer.valueOf(getM_AttributeSetInstance_ID()).toString();
		}
		
		setDescription (description);
		return description;
		
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
		
		getMAttributeSet();
		if (m_mas == null)
			return retValue;
		
		int M_LotCtl_ID = m_mas.getM_LotCtl_ID();
		if (M_LotCtl_ID != 0)
		{
			checkProductAttributeSetMatch(M_Product_ID);
			
			MLotCtl ctl = new MLotCtl (getCtx(), M_LotCtl_ID, get_TrxName());
			MLot lot = ctl.createLot(M_Product_ID);
			setM_Lot_ID (lot.getM_Lot_ID());
			setLot (lot.getName());
			retValue = new KeyNamePair (lot.getM_Lot_ID(), lot.getName());	
		}
		return retValue;
	}	//	createLot
	
	private void checkProductAttributeSetMatch(int m_product_id) {

		if (m_product_id <= 0)
			throw new AdempiereException("@M_Product_ID@ <= 0");
		
		MProduct product = MProduct.get(getCtx(), m_product_id, get_TrxName());
		if (product.getM_Product_ID() <= 0)
			throw new AdempiereException("@M_Product_ID@ <= 0");

		if (product.getM_AttributeSet_ID() != getM_AttributeSet_ID())
			throw new AdempiereException("@M_Product_ID@ @M_AttributeSet_ID@ != @M_AttributeSetInstance_ID@ @M_AttributeSet_ID@");
		
	}

	/**
	 * 	To to find lot and set Lot/ID
	 *	@param Lot lot
	 *	@param M_Product_ID product
	 */
	public void setLot (String Lot, int M_Product_ID)
	{
		//	Try to find it
		MLot mLot = MLot.getProductLot(getCtx(), M_Product_ID, Lot, get_TrxName());
		if (mLot != null && mLot.getM_Lot_ID() > 0)
		{
			setM_Lot_ID(mLot.getM_Lot_ID());
		}
		else
		{
			setM_Lot_ID(0);
		}
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
			getMAttributeSet();
			if (m_mas == null)
				return null;
						
			int M_SerNoCtl_ID = m_mas.getM_SerNoCtl_ID();
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

//	@Override
//	protected boolean afterSave(boolean newRecord, boolean success) 
//	{
//		if (newRecord && success)
//		{
//			//use id as description when description is empty
//			String desc = this.getDescription();
//			if (desc == null || desc.trim().length() == 0)
//			{
//				this.set_ValueNoCheck("Description", Integer.toString(getM_AttributeSetInstance_ID()));
//				String sql = "UPDATE M_AttributeSetInstance SET Description = ? WHERE M_AttributeSetInstance_ID = ?";
//				int no = DB.executeUpdateEx(sql, 
//						new Object[]{Integer.toString(getM_AttributeSetInstance_ID()), getM_AttributeSetInstance_ID()}, 
//						get_TrxName());
//				if (no <= 0)
//				{
//					log.log(Level.SEVERE, "Failed to update description.");
//					return false;
//				}
//			}
//		}
//		return success;
//	}
	
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
		
		if (requireNonNull(product).getM_AttributeSet_ID() <= 0)
			throw new IllegalArgumentException (Msg.parseTranslation(ctx, "@M_Product_ID@ @M_AttributeSet_ID@ <= 0"));
		
		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		asi.setM_AttributeSet_ID(product.getM_AttributeSet_ID());
		asi.getLot(true, product.get_ID());
		asi.getSerNo(true);
		asi.getGuaranteeDate(true);

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

		if (getM_AttributeSet_ID() == 0)
		{
			log.saveError("FillMandatory", Msg.getElement(getCtx(), "M_AttributeSet_ID"));
			return false;
		}
		return true;
		
	}	//	beforeSave
			
	public String toString() {
		return "ASI=" + this.getM_AttributeSetInstance_ID() + " AS=" + this.getM_AttributeSet_ID();
	}
	
}	//	MAttributeSetInstance
