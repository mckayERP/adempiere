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
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.DB;
import org.compiere.util.Env;


/**
 *	Distribution List Line
 *	
 *  @author Jorg Janke
 *  @version $Id: MDistributionListLine.java,v 1.3 2006/07/30 00:51:02 jjanke Exp $
 */
public class MDistributionListLine extends X_M_DistributionListLine
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8227610572847013425L;


	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_DistributionListLine_ID id
	 *	@param trxName transaction
	 */
	public MDistributionListLine (Properties ctx, int M_DistributionListLine_ID, String trxName)
	{
		super (ctx, M_DistributionListLine_ID, trxName);
	}	//	MDistributionListLine

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MDistributionListLine (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MDistributionListLine
	
	
	/**
	 * 	Get Min Qty
	 *	@return min Qty or 0
	 */
	public BigDecimal getMinQty ()
	{
		BigDecimal minQty = super.getMinQty ();
		if (minQty == null)
			return Env.ZERO;
		return minQty;
	}	//	getMinQty
	
	
	/**
	 * 	Get Ratio
	 *	@return ratio or 0
	 */
	public BigDecimal getRatio ()
	{
		BigDecimal ratio = super.getRatio();
		if (ratio == null)
			return Env.ZERO;
		return ratio;
	}	//	getRatio

	/**
	 *	Update Header Ratio Total
	 *	@return true if header updated
	 */
	private boolean updateHeaderRatioTotal()
	{
		int no = 0;
		String sql = "UPDATE M_DistributionList dl SET RatioTotal = "
						+ "(SELECT SUM(COALESCE(Ratio,0)) FROM M_DistributionListLine dll"
						+ " WHERE dll.M_DistributionList_ID=dl.M_DistributionList_ID)"
					+ " WHERE M_DistributionList_ID=? ";
		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getM_DistributionList_ID());
			no = pstmt.executeUpdate();
			if (no != 1)
				log.warning("(1) #" + no);
			pstmt.close ();
			pstmt = null;
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "getLines", e);
		}
		return no == 1;
	}	//	updateHeaderRatioTotal

	/**
	 * 	After Save
	 *	@param newRecord new
	 *	@param success success
	 *	@return saved
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;

		if (newRecord || is_ValueChanged("Ratio"))
		{
			return updateHeaderRatioTotal();
		}
		return success;
	}	//	afterSave

}	//	MDistributionListLine
