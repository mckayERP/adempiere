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

import static org.adempiere.test.TestUtilities.randomString;
import static org.adempiere.util.attributes.AttributeUtilities.isUniqueAttributeSetInstance;
import static org.compiere.model.X_M_AttributeSet.MANDATORYTYPE_NotMandatory;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MProduct;
import org.compiere.model.MTransaction;
import org.compiere.util.Env;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Utilities")
@Tag("AttributeUtilities")
class IT_AttributeUtilities_isUniqueAttributeSetInstance extends AttributeTestSetup {

	@BeforeEach
	void setup()
	{

		product = createProductWithNoAttributeSet();
		movementType = MTransaction.MOVEMENTTYPE_InventoryIn;

	}

	@Test
	void throwsNPEIfPassedNullProduct()
	{

		assertThrows(NullPointerException.class, () -> {

			isUniqueAttributeSetInstance(null, 0, null, null, trxName);

		});

	}

	@Test
	void throwsNPEIfPassedNullMovementQty()
	{

		assertThrows(NullPointerException.class, () -> {

			isUniqueAttributeSetInstance(product, 0, null, null, trxName);

		});

	}

	@Test
	void throwsNPEIfPassedNullMovementType()
	{

		assertThrows(NullPointerException.class, () -> {

			isUniqueAttributeSetInstance(product, 0, Env.ZERO, null, trxName);

		});

	}

	@Test
	void ifASIidIsZeroReturnsTrue()
	{

		assertTrue(isUniqueAttributeSetInstance(product, 0, Env.ZERO, movementType, trxName));

	}

	@Test
	void ifMovementQtyIsZeroReturnsTrue()
	{

		assertTrue(isUniqueAttributeSetInstance(product, 1, Env.ZERO, movementType, trxName));

	}

	@Test
	void ifProductIDIsZeroReturnsTrue()
	{

		MProduct product = new MProduct(ctx, 0, trxName);
		assertTrue(isUniqueAttributeSetInstance(product, 1, Env.ONE, movementType, trxName));

	}

	@Test
	void ifProductIsNotStockedReturnsTrue()
	{

		product.setIsStocked(false);
		assertTrue(isUniqueAttributeSetInstance(product, 1, Env.ONE, movementType, trxName));

	}

	@Test
	void ifProductHasNoAttributeSetReturnsTrue()
	{

		assertTrue(isUniqueAttributeSetInstance(product, 1, Env.ONE, movementType, trxName));

	}

	@Test
	void ifProductAttributeSetHasNoSerNumReturnsTrue()
	{

		boolean isSerNo = false;
		boolean isSerNoMandatory = false;
		setupProductWithAttributeSetAndAttribute(product,
				MANDATORYTYPE_NotMandatory, null, isSerNo, isSerNoMandatory);
		assertTrue(isUniqueAttributeSetInstance(product, 1, Env.ONE, movementType, trxName));

	}

	@Test
	void ifProductAttributeSetHasNoMandatorySerNumReturnsTrue()
	{

		boolean isSerNo = true;
		boolean isSerNoMandatory = false;
		setupProductWithAttributeSetAndAttribute(product,
				MANDATORYTYPE_NotMandatory, null, isSerNo, isSerNoMandatory);
		assertTrue(isUniqueAttributeSetInstance(product, 1, Env.ONE, movementType, trxName));

	}

	@Test
	void mandatorySerNoButMovementQtyGreaterThan1ReturnsFalse()
	{

		boolean isSerNo = true;
		boolean isSerNoMandatory = true;

		MAttributeSet attributeSetUnderTest = setupProductWithAttributeSetAndAttribute(product,
				MANDATORYTYPE_NotMandatory, null, isSerNo, isSerNoMandatory);

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0,
				attributeSetUnderTest.getM_AttributeSet_ID(), trxName);
		asi.saveEx();

		BigDecimal qty = BigDecimal.valueOf(2);

		assertFalse(isUniqueAttributeSetInstance(product, asi.getM_AttributeSetInstance_ID(),
				qty, movementType, trxName));

	}

	@Test
	void ifOutboundReturnsFalse()
	{

		boolean isSerNo = true;
		boolean isSerNoMandatory = true;

		MAttributeSet attributeSetUnderTest = setupProductWithAttributeSetAndAttribute(product,
				MANDATORYTYPE_NotMandatory, null, isSerNo, isSerNoMandatory);

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0,
				attributeSetUnderTest.getM_AttributeSet_ID(), trxName);
		asi.saveEx();

		movementType = MTransaction.MOVEMENTTYPE_CustomerShipment;
		assertTrue(isUniqueAttributeSetInstance(product, asi.getM_AttributeSetInstance_ID(),
				Env.ONE, movementType, trxName));

	}

	@Test
	void ifSerNoNotInStockReturnsTrue()
	{

		boolean isSerNo = true;
		boolean isSerNoMandatory = true;

		MAttributeSet attributeSetUnderTest = setupProductWithAttributeSetAndAttribute(product,
				MANDATORYTYPE_NotMandatory, null, isSerNo, isSerNoMandatory);

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0,
				attributeSetUnderTest.getM_AttributeSet_ID(), trxName);
		asi.saveEx();

		String serialNumber = randomString(6);
		asi.setSerNo(serialNumber);
		asi.saveEx();
		assertTrue(isUniqueAttributeSetInstance(product, asi.getM_AttributeSetInstance_ID(),
				Env.ONE, movementType, trxName));

	}

	@Test
	void ifSerNoAlreadyExistsOnReceiptReturnsFalse()
	{

		MAttributeSetInstance asi = createSerialNoProductAndAddToInventory();
		assertFalse(isUniqueAttributeSetInstance(product, asi.getM_AttributeSetInstance_ID(),
				Env.ONE, movementType, trxName));

	}

	@Test
	void ifSerNoAlreadyExistsOnNegReceiptReturnsTrue()
	{

		MAttributeSetInstance asi = createSerialNoProductAndAddToInventory();
		assertTrue(isUniqueAttributeSetInstance(product, asi.getM_AttributeSetInstance_ID(),
				Env.ONE.negate(), movementType, trxName));

	}

	@Test
	void ifSerNoAlreadyExistsOnShipmentReturnsTrue()
	{

		MAttributeSetInstance asi = createSerialNoProductAndAddToInventory();
		movementType = MTransaction.MOVEMENTTYPE_CustomerShipment;
		assertTrue(isUniqueAttributeSetInstance(product, asi.getM_AttributeSetInstance_ID(),
				Env.ONE, movementType, trxName));

	}

	@Test
	void ifSerNoAlreadyExistsOnNegativeShipmentReturnsFalse()
	{

		MAttributeSetInstance asi = createSerialNoProductAndAddToInventory();
		movementType = MTransaction.MOVEMENTTYPE_CustomerShipment;
		assertFalse(isUniqueAttributeSetInstance(product, asi.getM_AttributeSetInstance_ID(),
				Env.ONE.negate(), movementType, trxName));

	}
}
