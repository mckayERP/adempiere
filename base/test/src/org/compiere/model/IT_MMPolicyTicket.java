package org.compiere.model;

import static org.adempiere.test.TestUtilities.randomString;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;

import org.adempiere.engine.storage.StorageTransactionInfo;
import org.adempiere.engine.storage.StorageTransactionInfoBuilder;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.test.CommonGWSetup;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("MMPolicyTicket")
class IT_MMPolicyTicket extends CommonGWSetup {

	private static int m_product_id = 128;
	private static int m_locator_id;
	private static int m_warehouse_id = 103; // GW HQ Warehouse
	private static Timestamp now;

	@BeforeAll
	static void localSetUpBeforeClass() throws Exception
	{

		MProduct product = new MProduct(ctx, 0, trxName);
		product.setName("ProductUnderTest_" + randomString(4));
		product.setC_UOM_ID(MUOM.getDefault_UOM_ID(ctx));
		product.setM_Product_Category_ID(105); // GW Standard
		product.setC_TaxCategory_ID(107); // GW Standard
		product.setIsPurchased(true);
		product.setIsSold(true);
		product.setIsStocked(true);
		product.saveEx();
		product.load(trxName);
		m_product_id = product.getM_Product_ID();

		m_locator_id = MWarehouse.get(ctx, m_warehouse_id).getDefaultLocator().getM_Locator_ID();

		Calendar cal = Calendar.getInstance(); // locale-specific
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		now = new Timestamp(cal.getTimeInMillis());

	}

	@Test
	void testMMPolicyTicket_PropertiesIntString()
	{

		MMPolicyTicket ticket = new MMPolicyTicket(ctx, 0, trxName);
		assertNotNull(ticket, "Constructor returns null");
		assertEquals(0, ticket.get_ID(), "Ticket id is non-zero.");

	}

	@Test
	void testMMPolicyTicket_PropertiesResultSetString()
	{

		boolean tested = false;
		String sql = "SELECT * FROM M_MPolicyTicket t "
				+ "WHERE AD_Client_ID=?";
		PreparedStatement pstmt = null;
		try
		{

			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, AD_CLIENT_ID);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{

				MMPolicyTicket ticket = new MMPolicyTicket(ctx, rs, trxName);
				assertNotNull(ticket, "Constructor returns null");
				assertEquals(rs.getInt(MMPolicyTicket.COLUMNNAME_M_MPolicyTicket_ID),
						ticket.get_ID(), "Ticket id doesn't match record set.");
				tested = true;
				break;

			}
			rs.close();
			pstmt.close();
			pstmt = null;

		} catch (Exception e)
		{

			fail(e.getMessage());

		}
		try
		{

			if (pstmt != null)
				pstmt.close();
			pstmt = null;

		} catch (Exception e)
		{

			fail(e.getMessage());
			pstmt = null;

		}

		assertTrue(tested, "No valid result set found. Result set not tested.");

	}

	@Test
	void testMMPolicyTicket_Create()
	{

		MInOutLine inOutLine = new MInOutLine(ctx, 0, trxName);
		inOutLine.set_ValueNoCheck(MInOutLine.COLUMNNAME_M_InOutLine_ID, 1234);
		StorageTransactionInfo info = new StorageTransactionInfoBuilder()
				.withDocumentLine(inOutLine)
				.withM_Product_ID(12345)
				.withTransactionWarehouse_id(m_warehouse_id)
				.withTransactionLocator_id(m_locator_id)
				.withMovementType(MInOut.MOVEMENTTYPE_InventoryIn)
				.withMovementQty(Env.ONE)
				.withMovementDate(now)
				.build();

		MMPolicyTicket ticket = MMPolicyTicket.create(info);
		assertNotNull(ticket, "Constructor returns null");
		assertNotEquals(0, ticket.get_ID(), "Ticket id is zero.");
		assertEquals(inOutLine.get_ID(), ticket.getM_InOutLine_ID(),
				"Ticket MInOutLine ID should match line");
		assertEquals(now, ticket.getMovementDate(), "Ticket date should match docLine date");

	}

	@Test
	void testMMPolicyTicket_CreateFail()
	{

		MInOutLine lineMock = mock(MInOutLine.class);
		when(lineMock.get_TableName()).thenReturn("testTable");
		when(lineMock.get_ID()).thenReturn(123);
		when(lineMock.getCtx()).thenReturn(ctx);
		when(lineMock.get_TrxName()).thenReturn(trxName);

		StorageTransactionInfo info = new StorageTransactionInfoBuilder()
				.withDocumentLine(lineMock)
				.withM_Product_ID(12345)
				.withTransactionWarehouse_id(m_warehouse_id)
				.withTransactionLocator_id(m_locator_id)
				.withMovementType(MInOut.MOVEMENTTYPE_InventoryIn)
				.withMovementQty(Env.ONE)
				.withMovementDate(now)
				.build();

		Exception e = assertThrows(AdempiereException.class, () -> {
			MMPolicyTicket.create(info);
		});

		assertTrue(
				e.getMessage().startsWith(
						MMPolicyTicket.Table_Name + " does not contain reference column for "),
				"Incorrect error message.");

	}

	@Test
	void testMMPolicyTicket_GetOrCreateFromTransactionReturnsNull()
	{

		assertNull(MMPolicyTicket.getOrCreateFromTransaction(ctx, null, trxName),
				"Should return null");

	}

	@Test
	void testMMPolicyTicket_GetOrCreateFromTransactionTicketExists()
	{

		MTransaction transactionMock = mock(MTransaction.class);

		MInOutLine inOutLine = new MInOutLine(ctx, 0, trxName);
		inOutLine.set_ValueNoCheck(MInOutLine.COLUMNNAME_M_InOutLine_ID, 1234);
		StorageTransactionInfo info = new StorageTransactionInfoBuilder()
				.withDocumentLine(inOutLine)
				.withM_Product_ID(12345)
				.withTransactionWarehouse_id(m_warehouse_id)
				.withTransactionLocator_id(m_locator_id)
				.withMovementType(MInOut.MOVEMENTTYPE_InventoryIn)
				.withMovementQty(Env.ONE)
				.withMovementDate(now)
				.build();

		MMPolicyTicket ticket = MMPolicyTicket.create(info);

		when(transactionMock.getM_MPolicyTicket_ID()).thenReturn(ticket.get_ID());

		MMPolicyTicket ticket2 = MMPolicyTicket.getOrCreateFromTransaction(ctx, transactionMock,
				trxName);
		assertNotNull(ticket2, "getOrCreateFromTransaction returned null");
		assertEquals(ticket.get_ID(), ticket2.get_ID(),
				"Transaction does not have the correct ticket");

	}

	@Test
	void testMMPolicyTicket_GetOrCreateFromTransactionTicketDoesNotExists()
	{

		MTransaction transaction = new MTransaction(ctx, 0, trxName);
		transaction.setAD_Client_ID(AD_CLIENT_ID);
		transaction.setAD_Org_ID(AD_ORG_ID);
		transaction.setM_MPolicyTicket_ID(0);
		transaction.setIsActive(true);
		transaction.setC_ProjectIssue_ID(1);
		transaction.setM_AttributeSetInstance_ID(0);
		transaction.setM_InOutLine_ID(0);
		transaction.setM_Locator_ID(m_locator_id);
		transaction.setM_InventoryLine_ID(0);
		transaction.setM_MovementLine_ID(0);
		transaction.setM_MPolicyTicket_ID(0);
		transaction.setM_Product_ID(m_product_id);
		transaction.setM_ProductionLine_ID(0);
		transaction.setM_Transaction_ID(0);
		transaction.setMovementDate(now);
		transaction.setMovementQty(Env.ONE);
		transaction.setMovementType(MTransaction.MOVEMENTTYPE_VendorReceipts);
		transaction.setPP_Cost_Collector_ID(0);

		MMPolicyTicket ticket = MMPolicyTicket.getOrCreateFromTransaction(ctx, transaction,
				trxName);
		assertNotNull(ticket, "getOrCreateFromTransaction returned null");

		// Copy fields with the same name
		int count = transaction.get_ColumnCount();
		for (int i = 0; i < count; i++)
		{

			String columnName = transaction.get_ColumnName(i);
			// Ignore the ticket ID and UUID but copy all other fields. This includes the
			// client/org
			// and other standard fields
			if (!MMPolicyTicket.COLUMNNAME_M_MPolicyTicket_ID.equalsIgnoreCase(columnName)
					&& !"UUID".equalsIgnoreCase(columnName)
					&& !"Created".equalsIgnoreCase(columnName)
					&& !"Updated".equalsIgnoreCase(columnName)
					&& !MTransaction.COLUMNNAME_M_Product_ID.equalsIgnoreCase(columnName)
					&& !MTransaction.COLUMNNAME_M_AttributeSetInstance_ID
							.equalsIgnoreCase(columnName)
					&& !MTransaction.COLUMNNAME_M_Locator_ID.equalsIgnoreCase(columnName)
					&& !MTransaction.COLUMNNAME_M_Transaction_ID.equalsIgnoreCase(columnName))
			{

				int index = ticket.get_ColumnIndex(columnName);
				if (index >= 0)
				{

					assertEquals(ticket.get_Value(index), transaction.get_Value(i),
							"Values don't match for " + columnName);

				} else
				{

					if (columnName.endsWith("_ID"))
					{

						fail(ticket.get_TableName() + " does not contain reference column for "
								+ columnName);

					}

				}

			}

		}

	}

}
