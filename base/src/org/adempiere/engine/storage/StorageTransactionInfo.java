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
package org.adempiere.engine.storage;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;

import org.adempiere.engine.IDocumentLine;
import org.compiere.util.Env;

public class StorageTransactionInfo {

	private IDocumentLine documentLine;
	private String movementType;
	private Timestamp movementDate; 
	private Optional<BigDecimal> movementQty = Optional.empty();
	private int m_product_id = -1;
	private int transactionWarehouse_id = -1;
	private int transactionLocator_id = -1;
	private int transactionAttributeSetInstance_id = -1;
	private int transactionMPolicyTicket_id = -1;
	private int orderWarehouse_id = -1;
	private int orderMPolicyTicket_id = -1;
	private int orderAttributeSetInstance_id = -1;
	private Optional<Boolean> createMA = Optional.empty();
	private Optional<Boolean> isSOTrx = Optional.empty();
	private Optional<Boolean> deleteExistingMAEntries = Optional.empty();
	private Optional<Boolean> processMA = Optional.empty();
	private Optional<Boolean> useToFields = Optional.empty();
	private Optional<Boolean> updateStorage = Optional.empty();
	private Optional<Boolean> isIncomingTransaction = Optional.empty();
	private Optional<Boolean> isFifoPolicy = Optional.empty();
	
	/**
	 * @return the transactionMPolicyTicket_id
	 */
	public int getTransactionMPolicyTicket_id() {
		return transactionMPolicyTicket_id;
	}

	/**
	 * @param transactionMPolicyTicket_id the transactionMPolicyTicket_id to set
	 */
	public void setTransactionMPolicyTicket_id(int transactionMPolicyTicket_id) {
		this.transactionMPolicyTicket_id = transactionMPolicyTicket_id;
	}

	public StorageTransactionInfo() {
		
	}

	/**
	 * @return the line
	 */
	public IDocumentLine getDocumentLine() {
		return documentLine;
	}

	/**
	 * @param line the line to set
	 */
	public void setDocumentLine(IDocumentLine documentLine) {
		this.documentLine = documentLine;
	}

	/**
	 * @return the movementType
	 */
	public String getMovementType() {
		return Optional.ofNullable(movementType).orElse("");
	}

	/**
	 * @return the transactionWarehouse_id
	 */
	public int getTransactionWarehouse_id() {
		return transactionWarehouse_id;
	}

	/**
	 * @param transactionWarehouse_id the transactionWarehouse_id to set
	 */
	public void setTransactionWarehouse_id(int transactionWarehouse_id) {
		this.transactionWarehouse_id = transactionWarehouse_id;
	}

	/**
	 * @return the orderWarehouse_id
	 */
	public int getOrderWarehouse_id() {
		return orderWarehouse_id;
	}

	/**
	 * @param orderWarehouse_id the orderWarehouse_id to set
	 */
	public void setOrderWarehouse_id(int orderWarehouse_id) {
		this.orderWarehouse_id = orderWarehouse_id;
	}

	/**
	 * @return the orderAttributeSetInstance_id
	 */
	public int getOrderAttributeSetInstance_id() {
		return orderAttributeSetInstance_id;
	}

	/**
	 * @param orderAttributeSetInstance_id the orderAttributeSetInstance_id to set
	 */
	public void setOrderAttributeSetInstance_id(int orderAttributeSetInstance_id) {
		this.orderAttributeSetInstance_id = orderAttributeSetInstance_id;
	}

	/**
	 * @return the createMA
	 */
	public boolean isCreateMaterialAllocations() {
		return createMA.orElse(false);
	}

	/**
	 * @param createMA the createMA to set
	 */
	public void setCreateMA(boolean createMA) {
		this.createMA = Optional.of(createMA);
	}

	/**
	 * @return the isSOTrx
	 */
	public boolean isSOTrx() {
		return isSOTrx.orElse(true);
	}

	/**
	 * @param isSOTrx the isSOTrx to set
	 */
	public void setSOTrx(boolean isSOTrx) {
		this.isSOTrx = Optional.of(isSOTrx);
	}

	/**
	 * @return the processMA
	 */
	public boolean isProcessMA() {
		return processMA.orElse(true);
	}

	/**
	 * @param processMA the processMA to set
	 */
	public void setProcessMA(boolean processMA) {
		this.processMA = Optional.of(processMA);
	}

	/**
	 * @return the updateStorage
	 */
	public boolean isUpdateStorage() {
		return updateStorage.orElse(true);
	}

	/**
	 * @param updateStorage the updateStorage to set
	 */
	public void setUpdateStorage(boolean updateStorage) {
		this.updateStorage = Optional.of(updateStorage);
	}

	/**
	 * @param movementType the movementType to set
	 */
	public void setMovementType(String movementType) {
		this.movementType = movementType;
	}

	/**
	 * @return the movementDate
	 */
	public Timestamp getMovementDate() {
		return movementDate;
	}

	/**
	 * @param movementDate the movementDate to set
	 */
	public void setMovementDate(Timestamp movementDate) {
		this.movementDate = movementDate;
	}

	/**
	 * @return the movementQty
	 */
	public BigDecimal getMovementQty() {
		return movementQty.orElse(Env.ZERO);
	}

	/**
	 * @param movementQty the movementQty to set
	 */
	public void setMovementQty(BigDecimal movementQty) {
		this.movementQty = Optional.ofNullable(movementQty);
	}

	/**
	 * @return the useToFields
	 */
	public boolean isUseToFields() {
		return useToFields.orElse(false);
	}

	/**
	 * @param useToFields the useToFields to set
	 */
	public void setUseToFields(boolean useToFields) {
		this.useToFields = Optional.of(useToFields);
	}

	/**
	 * @return the deleteExistingMALines
	 */
	public boolean isDeleteExistingMAEntries() {
		return deleteExistingMAEntries.orElse(true);
	}

	/**
	 * @param deleteExistingMALines the deleteExistingMALines to set
	 */
	public void setDeleteExistingMAEntries(boolean deleteExistingMAEntries) {
		this.deleteExistingMAEntries = Optional.of(deleteExistingMAEntries);
	}

	/**
	 * @return the orderMPolicyTicket_id
	 */
	public int getOrderMPolicyTicket_id() {
		return orderMPolicyTicket_id;
	}

	/**
	 * @param orderMPolicyTicket_id the orderMPolicyTicket_id to set
	 */
	public void setOrderMPolicyTicket_id(int orderMPolicyTicket_id) {
		this.orderMPolicyTicket_id = orderMPolicyTicket_id;
	}

	/**
	 * @return the transactionLocator_id
	 */
	public int getTransactionLocator_id() {
		return transactionLocator_id;
	}

	/**
	 * @param transactionLocator_id the transactionLocator_id to set
	 */
	public void setTransactionLocator_id(int transactionLocator_id) {
		this.transactionLocator_id = transactionLocator_id;
	}

	/**
	 * @return the transactionAttributeSetInstance_id
	 */
	public int getTransactionAttributeSetInstance_id() {
		return transactionAttributeSetInstance_id;
	}

	/**
	 * @param transactionAttributeSetInstance_id the transactionAttributeSetInstance_id to set
	 */
	public void setTransactionAttributeSetInstance_id(int transactionAttributeSetInstance_id) {
		this.transactionAttributeSetInstance_id = transactionAttributeSetInstance_id;
	}

	/**
	 * @return the m_product_id
	 */
	public int getM_Product_ID() {
		return m_product_id;
	}

	/**
	 * @param m_product_id the m_product_id to set
	 */
	public void setM_Product_ID(int m_product_id) {
		this.m_product_id = m_product_id;
	}

	/**
	 * @return the isIncomingTransaction
	 */
	public boolean isIncomingTransaction() {
		return isIncomingTransaction.orElse(true);
	}

	/**
	 * @param isIncomingTransaction the isIncomingTransaction to set
	 */
	public void setIsIncomingTransaction(boolean isIncomingTransaction) {
		this.isIncomingTransaction = Optional.of(isIncomingTransaction);
	}

	/**
	 * @return the isFifoPolicy
	 */
	public boolean isFifoPolicy() {
		return isFifoPolicy.orElse(true);
	}

	/**
	 * @return the isFifoPolicy
	 */
	public Optional<Boolean> isFifoPolicyOptional() {
		return isFifoPolicy;
	}

	/**
	 * @param isFifoPolicy the isFifoPolicy to set
	 */
	public void setIsFifoPolicy(boolean isFifoPolicy) {
		this.isFifoPolicy = Optional.of(isFifoPolicy);
	}
	
	
}
