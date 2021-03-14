/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
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
 * Copyright (C) 2003-2007 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): victor.perez@e-evolution.com http://www.e-evolution.com    *
 *                 Teo Sarca, www.arhipac.ro                                  *
 *****************************************************************************/

package org.adempiere.engine.storage;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.engine.storage.rules.MInOutLineStorageRule;
import org.adempiere.engine.storage.rules.MMovementLineStorageRule;
import org.adempiere.engine.storage.rules.MOrderLineStorageRule;
import org.adempiere.engine.storage.rules.MPPOrderBOMLineStorageRule;
import org.adempiere.engine.storage.rules.MPPOrderReceiptStorageRule;
import org.adempiere.engine.storage.rules.MPPOrderStorageRule;
import org.adempiere.engine.storage.rules.StorageEngineRule;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
import org.compiere.model.MStorage;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * Storage Engine
 * 
 * @author victor.perez@e-evolution.com http://www.e-evolution.com
 * @author Teo Sarca
 */
public class StorageEngine {

	/** Logger */
	private static final CLogger log = CLogger.getCLogger(StorageEngine.class);

	protected CLogger getCLogger() {

		return log;

	}

	private static Stream<StorageEngineRule<IDocumentLine, String>> streamStorageEngineRules() {

		List<StorageEngineRule<IDocumentLine, String>> storageEngineRules = new ArrayList<>();

		storageEngineRules.add(new MOrderLineStorageRule());
		storageEngineRules.add(new MPPOrderStorageRule());
		storageEngineRules.add(new MPPOrderReceiptStorageRule());
		storageEngineRules.add(new MPPOrderBOMLineStorageRule());
		storageEngineRules.add(new MInOutLineStorageRule());
		storageEngineRules.add(new MMovementLineStorageRule());

		return storageEngineRules.stream();

	}

	public static StorageEngine getStorageEngine() {

		return new StorageEngine();

	}

	public static String applyStorageRules(IDocumentLine docLine) {

		String msg = streamStorageEngineRules()
				.filter(rule -> rule.matches(docLine))
				.map(rule -> rule.process(docLine))
				.findFirst()
				.orElseThrow(() -> new AdempiereException(
						Msg.getMsg(Env.getCtx(), IStorageEngine.MSG_NO_MATCHING_RULE) + " "
								+ docLine));

		return msg.isEmpty() ? null : msg;

	}

	/**
	 * get (default) Locator based on qty.
	 * 
	 * @param qty quantity Assumes Warehouse is set
	 */
	public int getM_Locator_ID(
			Properties ctx,
			int m_warehouse_id,
			int m_product_id,
			int m_attributeSetInstance_id,
			BigDecimal qty,
			String trxName) {

		// Get existing Location
		int m_locator_id = MStorage.getM_Locator_ID(m_warehouse_id,
				m_product_id, m_attributeSetInstance_id, 0,
				qty, trxName);

		// Get default Location
		if (m_locator_id == 0) {
			MWarehouse wh = MWarehouse.get(ctx, m_warehouse_id);
			m_locator_id = wh.getDefaultLocator().getM_Locator_ID();
		}
		return m_locator_id;

	} // setM_Locator_ID

	/**
	 * @deprecated - use {@link #applyStorageRules(PO parent, IDocumentLine docLine)}
	 * @param docLine
	 * @param movementType
	 * @param movementDate
	 * @param movementQty
	 * @param isReversal
	 * @param warehouseId
	 * @param reservationAttributeSetInstanceId
	 * @param orderWarehouseId
	 * @param isSOTrx
	 */
	@Deprecated
	public static void createTransaction(
			IDocumentLine docLine,
			String movementType,
			Timestamp movementDate,
			BigDecimal movementQty,
			boolean isReversal,
			int warehouseId,
			int reservationAttributeSetInstanceId,
			int orderWarehouseId,
			boolean isSOTrx) {

		createTransaction(docLine,
				movementType,
				movementDate,
				movementQty,
				isReversal,
				warehouseId,
				orderWarehouseId,
				reservationAttributeSetInstanceId,
				isSOTrx,
				true,
				true,
				false,
				true);

	}

	@Deprecated
	public static boolean createTransaction(
			IDocumentLine docLine,
			String movementType,
			Timestamp movementDate,
			BigDecimal movementQty,
			boolean isReversal,
			int warehouseId,
			int orderWarehouseId,
			int orderAttributeSetInstanceId,
			boolean isSOTrx,
			boolean deleteExistingMAEntries,
			boolean processMA,
			boolean useToFields,
			boolean updateStorage) {

		String msg = applyStorageRules(docLine);
		if (msg != null) {
			log.severe(msg);
			return false;
		}
		return true;

	}

	@Deprecated
	public void reserveOrOrderStock(Properties ctx, int M_Warehouse_ID, int M_Locator_ID,
			int M_Product_ID, int M_AttributeSetInstance_ID, int M_MPolicyTicket_ID,
			BigDecimal qtyOrdered, BigDecimal qtyReserved, String trxName) {

		if (M_Product_ID <= 0)
			throw new AdempiereException("@Error@ @M_Product_ID@ @NotZero@");

		if (M_Warehouse_ID <= 0)
			throw new AdempiereException("@Error@ @M_Warehouse_ID@ @NotZero@");

		if (qtyOrdered.compareTo(Env.ZERO) == 0 && qtyReserved.compareTo(Env.ZERO) == 0)
			return; // Nothing to do

		MProduct product = MProduct.get(ctx, M_Product_ID);
		if (product == null)
			throw new AdempiereException("@Error@ @M_Product-ID@=" + M_Product_ID + " @NotFound@");
		if (!product.isStocked())
			return; // Nothing to do

		if (M_Locator_ID == 0) {
			// Get Locator to order/reserve
			// For orders, is there a sufficient qty of this product/ASI (ASI could be zero)
			// in inventory?
			// Get the locator with sufficient qty and with the highest locator priority.
			M_Locator_ID = MStorage.getM_Locator_ID(M_Warehouse_ID,
					M_Product_ID, M_AttributeSetInstance_ID, M_MPolicyTicket_ID,
					qtyOrdered, trxName);
			// Get default Location
			if (M_Locator_ID == 0) {
				// try to take default locator for product first
				// if it is from the selected warehouse

				MWarehouse wh = MWarehouse.get(ctx, M_Warehouse_ID);
				M_Locator_ID = product.getM_Locator_ID();
				if (M_Locator_ID != 0) {
					MLocator locator = new MLocator(ctx, product.getM_Locator_ID(), trxName);
					// product has default locator defined but is not from the order warehouse
					if (locator.getM_Warehouse_ID() != wh.get_ID()) {
						M_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();
					}
				} else {
					M_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();
				}
			}
		}
		// Update Storage
		if (!MStorage.add(ctx, M_Warehouse_ID, M_Locator_ID,
				M_Product_ID,
				M_AttributeSetInstance_ID, M_AttributeSetInstance_ID, M_MPolicyTicket_ID,
				Env.ZERO, qtyReserved, qtyOrdered, trxName)) {
			throw new AdempiereException(); // Cannot reserve or order stock
		}

	}

}
