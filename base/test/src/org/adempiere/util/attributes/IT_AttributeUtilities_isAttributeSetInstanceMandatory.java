/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2020 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
package org.adempiere.util.attributes;

import static org.adempiere.util.attributes.AttributeUtilities.INSTANCE_ATTRIBUTE;
import static org.adempiere.util.attributes.AttributeUtilities.MANDATORY;
import static org.adempiere.util.attributes.AttributeUtilities.NOT_INSTANCE_ATTRIBUTE;
import static org.adempiere.util.attributes.AttributeUtilities.NOT_MANDATORY;
import static org.adempiere.util.attributes.AttributeUtilities.isAttributeSetInstanceMandatory;
import static org.compiere.model.X_M_AttributeSet.MANDATORYTYPE_AlwaysMandatory;
import static org.compiere.model.X_M_AttributeSet.MANDATORYTYPE_NotMandatory;
import static org.compiere.model.X_M_AttributeSet.MANDATORYTYPE_WhenShipping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.compiere.model.MAcctSchema;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategoryAcct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("Utilities")
@Tag("AttributeUtilities")
class IT_AttributeUtilities_isAttributeSetInstanceMandatory extends AttributeTestSetup {

	private static MProduct product = null;

	private void setCostingLevelToBatchLot()
	{

		MAcctSchema acctSchema = (MAcctSchema.getClientAcctSchema(ctx, AD_CLIENT_ID, trxName))[0];
		MProductCategoryAcct.cleareCache();
		MProductCategoryAcct pca = MProductCategoryAcct.get(ctx, product.getM_Product_Category_ID(),
				acctSchema.get_ID(), trxName);
		pca.setCostingLevel(MAcctSchema.COSTINGLEVEL_BatchLot);
		pca.saveEx();

	}

	@BeforeEach
	void setup()
	{

		product = createProductWithNoAttributeSet();

	}

	static Stream<Arguments> tableAndTrxTypeProvider()
	{

		String mt_nm = MANDATORYTYPE_NotMandatory;
		String mt_am = MANDATORYTYPE_AlwaysMandatory;
		String mt_ws = MANDATORYTYPE_WhenShipping;
		boolean nm = NOT_MANDATORY;
		boolean m = MANDATORY;
		boolean nia = NOT_INSTANCE_ATTRIBUTE;
		boolean ia = INSTANCE_ATTRIBUTE;
		boolean sblc = true; // SetBatchLotCosting
		boolean nc = false; // NoCosting
		boolean ns = false; // No serial number
		boolean s = true; // serial number
		boolean snm = true; // Serial number mandatory - ignored if no serial number

		String msg_1 = "Should return false when attribute set is not an instance set";
		String msg_2 = "Should return false when attribute set is an instance set but not mandatory";
		String msg_3 = "Attribute set is always mandatory but excluded in MOrderLine";
		String msg_4 = "Attribute set is always mandatory";
		String msg_5 = "Attribute set is excluded in MOrderLine";
		String msg_6 = "Attribute set should not be mandatory if not shipping";
		String msg_7 = "Attribute set should be mandatory when shipping";
		String msg_8 = "Attribute set should not be mandatory when receiving";
		String msg_9 = "Attribute set is always mandatory if costing level is batch/lot and "
				+ "there is instance info";
		String msg_10 = "Should return false when attribute set is not an instance set "
				+ "despite costing level";
		String msg_11 = "Attribute set is always mandatory because of serial number but "
				+ "excluded in MOrderLine";
		String msg_12 = "Attribute set is always mandatory because of serial number";
		String msg_13 = "Attribute set should be mandatory when shipping because of serial number";

		return Stream.of(
				arguments(MOrderLine.Table_ID, SALES_TRX, mt_nm, nm, nia, false, nc, ns, snm,
						msg_1),
				arguments(MOrderLine.Table_ID, PURCHASE_TRX, mt_nm, nm, nia, false, nc, ns, snm,
						msg_1),
				arguments(MInOutLine.Table_ID, SALES_TRX, mt_nm, nm, nia, false, nc, ns, snm,
						msg_1),
				arguments(MInOutLine.Table_ID, PURCHASE_TRX, mt_nm, nm, nia, false, nc, ns, snm,
						msg_1),

				arguments(MOrderLine.Table_ID, SALES_TRX, mt_am, nm, nia, false, nc, ns, snm,
						msg_1),
				arguments(MOrderLine.Table_ID, PURCHASE_TRX, mt_am, nm, nia, false, nc, ns, snm,
						msg_1),
				arguments(MInOutLine.Table_ID, SALES_TRX, mt_am, nm, nia, false, nc, ns, snm,
						msg_1),
				arguments(MInOutLine.Table_ID, PURCHASE_TRX, mt_am, nm, nia, false, nc, ns, snm,
						msg_1),

				arguments(MOrderLine.Table_ID, SALES_TRX, mt_am, m, nia, false, nc, ns, snm, msg_1),
				arguments(MOrderLine.Table_ID, PURCHASE_TRX, mt_am, m, nia, false, nc, ns, snm,
						msg_1),
				arguments(MInOutLine.Table_ID, SALES_TRX, mt_am, m, nia, false, nc, ns, snm, msg_1),
				arguments(MInOutLine.Table_ID, PURCHASE_TRX, mt_am, m, nia, false, nc, ns, snm,
						msg_1),

				arguments(MOrderLine.Table_ID, SALES_TRX, mt_nm, nm, ia, false, nc, ns, snm, msg_2),
				arguments(MOrderLine.Table_ID, PURCHASE_TRX, mt_nm, nm, ia, false, nc, ns, snm,
						msg_2),
				arguments(MInOutLine.Table_ID, SALES_TRX, mt_nm, nm, ia, false, nc, ns, snm, msg_2),
				arguments(MInOutLine.Table_ID, PURCHASE_TRX, mt_nm, nm, ia, false, nc, ns, snm,
						msg_2),

				arguments(MOrderLine.Table_ID, SALES_TRX, mt_am, m, ia, false, nc, ns, snm, msg_3),
				arguments(MOrderLine.Table_ID, PURCHASE_TRX, mt_am, m, ia, true, nc, ns, snm,
						msg_4),
				arguments(MInOutLine.Table_ID, SALES_TRX, mt_am, m, ia, true, nc, ns, snm, msg_4),
				arguments(MInOutLine.Table_ID, PURCHASE_TRX, mt_am, m, ia, true, nc, ns, snm,
						msg_4),

				arguments(MOrderLine.Table_ID, SALES_TRX, mt_ws, nm, ia, false, nc, ns, snm, msg_5),
				arguments(MOrderLine.Table_ID, PURCHASE_TRX, mt_ws, nm, ia, false, nc, ns, snm,
						msg_6),
				arguments(MInOutLine.Table_ID, SALES_TRX, mt_ws, nm, ia, true, nc, ns, snm, msg_7),
				arguments(MInOutLine.Table_ID, PURCHASE_TRX, mt_ws, nm, ia, false, nc, ns, snm,
						msg_8),

				arguments(MOrderLine.Table_ID, SALES_TRX, mt_nm, nm, ia, true, sblc, ns, snm,
						msg_9),
				arguments(MOrderLine.Table_ID, PURCHASE_TRX, mt_nm, nm, ia, true, sblc, ns, snm,
						msg_9),
				arguments(MInOutLine.Table_ID, SALES_TRX, mt_nm, nm, ia, true, sblc, ns, snm,
						msg_9),
				arguments(MInOutLine.Table_ID, PURCHASE_TRX, mt_nm, nm, ia, true, sblc, ns, snm,
						msg_9),

				arguments(MOrderLine.Table_ID, SALES_TRX, mt_nm, nm, nia, false, sblc, ns, snm,
						msg_10),
				arguments(MOrderLine.Table_ID, PURCHASE_TRX, mt_nm, nm, nia, false, sblc, ns, snm,
						msg_10),
				arguments(MInOutLine.Table_ID, SALES_TRX, mt_nm, nm, nia, false, sblc, ns, snm,
						msg_10),
				arguments(MInOutLine.Table_ID, PURCHASE_TRX, mt_nm, nm, nia, false, sblc, ns, snm,
						msg_10),

				arguments(MOrderLine.Table_ID, SALES_TRX, mt_nm, nm, nia, false, nc, s, snm,
						msg_11),
				arguments(MOrderLine.Table_ID, PURCHASE_TRX, mt_nm, nm, nia, true, nc, s, snm,
						msg_12),
				arguments(MInOutLine.Table_ID, SALES_TRX, mt_nm, nm, nia, true, nc, s, snm, msg_12),
				arguments(MInOutLine.Table_ID, PURCHASE_TRX, mt_nm, nm, nia, true, nc, s, snm,
						msg_12),

				arguments(MOrderLine.Table_ID, SALES_TRX, mt_ws, nm, nia, false, nc, s, snm,
						msg_11),
				arguments(MOrderLine.Table_ID, PURCHASE_TRX, mt_ws, nm, nia, false, nc, s, snm,
						msg_6),
				arguments(MInOutLine.Table_ID, SALES_TRX, mt_ws, nm, nia, true, nc, s, snm, msg_13),
				arguments(MInOutLine.Table_ID, PURCHASE_TRX, mt_ws, nm, nia, false, nc, s, snm,
						msg_8));

	}

	@Test
	void whenPassedNullThrowsNPE()
	{

		assertThrows(NullPointerException.class, () -> {

			isAttributeSetInstanceMandatory(ctx, null, 0, true, trxName);

		}, "Should throw NPE when passed a null product");

	}

	@Test
	void whenPassedNoAttributeSetReturnFalse()
	{

		assertFalse(isAttributeSetInstanceMandatory(ctx, product, 0, true, trxName),
				"Should return false when product has no attribute set");

	}

	@Test
	void whenPassedNotAnInstanceAttributeSetReturnsFalse()
	{

		createAttributeSetAndAddToProduct(product);
		assertFalse(isAttributeSetInstanceMandatory(ctx, product, 0, true, trxName),
				"Should return false when attribute set is not an instance set");

	}

	@ParameterizedTest
	@MethodSource("tableAndTrxTypeProvider")
	void test_isAttributeSetInstanceMandatory(int tableID, boolean isSOTrx,
			String mandatoryType, boolean isMandatoryAttribute, boolean isInstanceAttribute,
			boolean expectedResult, boolean setCosting, boolean setSerNo, boolean isSerNoMandatory,
			String message)
	{

		if (setCosting)
			setCostingLevelToBatchLot();

		setupProductWithAttributeSetAndAttribute(product, mandatoryType,
				isMandatoryAttribute, isInstanceAttribute, setSerNo, isSerNoMandatory);

		assertEquals(expectedResult,
				isAttributeSetInstanceMandatory(ctx, product, tableID, isSOTrx, trxName),
				message);

	}

}
