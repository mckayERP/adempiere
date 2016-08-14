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
 * Copyright (C) 2003-2010 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): victor.perez@e-evolution.com http://www.e-evolution.com    *
 *****************************************************************************/

package org.eevolution.process;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.adempiere.engine.CostEngineFactory;
import org.adempiere.engine.CostingMethodFactory;
import org.adempiere.engine.StandardCostingMethod;
import org.compiere.model.I_M_Cost;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MCostDetail;
import org.compiere.model.MCostElement;
import org.compiere.model.MCostType;
import org.compiere.model.MInOutLine;
import org.compiere.model.MLandedCostAllocation;
import org.compiere.model.MMatchInv;
import org.compiere.model.MMatchPO;
import org.compiere.model.MTransaction;
import org.compiere.model.Query;
import org.compiere.model.X_M_CostType;
import org.compiere.util.DB;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Trx;
import org.eevolution.model.MPPCostCollector;

/**
 * Regenerate Cost Detail The Generate Cost Transaction process allows the
 * detailed cost calculation and cost generation beginning from a date. If you
 * have not yet set COGs Adjustment, you should execute this process before a
 * period's end in order to fix the cost layers.
 *
 * @author victor.perez@e-evolution.com, www.e-evolution.com
 * 
 * @author mckayERP www.mckayERP.com
 * 			<li> #167 Fix Order of treating documents when generating cost details
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *			<li> FR [ 405 ] Wrong Syntax of Delete query in GenerateCostDetail.java
 *			@see https://github.com/adempiere/adempiere/issues/405
 */
public class GenerateCostDetail extends GenerateCostDetailAbstract {
    /**
     * Variables *
     */
    private ArrayList<Object> deleteParameters;
    private ArrayList<Object> resetCostParameters;
    private List<MAcctSchema> acctSchemas = new ArrayList<MAcctSchema>();
    private List<MCostType> costTypes = new ArrayList<MCostType>();
    private List<MCostType> costTypesDelete = new ArrayList<MCostType>();
    private List<MCostElement> costElements = new ArrayList<MCostElement>();
    private List<MCostElement> costElementsDelete = new ArrayList<MCostElement>();
    private StringBuffer deleteCostDetailWhereClause;
    private StringBuffer resetCostWhereClause;
    private List<Integer> deferredTransactionIds = new ArrayList<Integer>();
    private List<Integer> deferredProductIds = new ArrayList<Integer>();
    private KeyNamePair[] transactions;

    /**
     * Prepare - e.g., get Parameters.
     */
    protected void prepare() {
        super.prepare();
        if (getAccountDate() != null)
            setup();

    } // prepare

    /**
     * execute the Generate Cost Detail
     */
    protected String doIt() throws Exception {
        generateCostDetail();
        return "@Ok@";

    }

    /**
     * Delete Cost Detail
     *
     * @throws SQLException
     */
    private void deleteCostDetail(String trxName) throws SQLException {
        StringBuffer sqlDelete;
        //	BR [ 405 ]
        sqlDelete = new StringBuffer("DELETE FROM M_CostDetail WHERE ");
        sqlDelete.append(deleteCostDetailWhereClause);
        DB.executeUpdateEx(sqlDelete.toString(),
                deleteParameters.toArray(), trxName);
    }

    /**
     * Reset Cost Dimension
     * @param costingMethod
     * @param trxName
     * @throws SQLException
     */
    private void resetCostDimension(String costingMethod, String trxName) throws SQLException {
        StringBuffer sqlReset;
        sqlReset = new StringBuffer("UPDATE M_Cost SET ");

        // Delete M_Cost not for others than average
        if (MCostType.COSTINGMETHOD_AverageInvoice.equals(costingMethod)) {
            sqlReset.append(I_M_Cost.COLUMNNAME_CurrentCostPrice).append("=0.0,");
            sqlReset.append(I_M_Cost.COLUMNNAME_CurrentCostPriceLL).append("= 0.0,");
        }

        sqlReset.append(I_M_Cost.COLUMNNAME_CurrentQty).append("= 0.0,");
        sqlReset.append(I_M_Cost.COLUMNNAME_CumulatedAmt).append("= 0.0,");
        sqlReset.append(I_M_Cost.COLUMNNAME_CumulatedAmtLL).append("= 0.0,");
        sqlReset.append(I_M_Cost.COLUMNNAME_CumulatedQty).append("= 0.0 ");
        sqlReset.append(" WHERE ").append(resetCostWhereClause);
        DB.executeUpdateEx(sqlReset.toString(),
                resetCostParameters.toArray(), trxName);
    }


    /**
     * Setup the collections
     *
     * @throws SQLException
     */
    private void setup() {

        if (getAccountingSchemaId() > 0)
            acctSchemas.add(MAcctSchema.get(getCtx(), getAccountingSchemaId() , get_TrxName()));
        else
            acctSchemas = new ArrayList<MAcctSchema>(Arrays.asList(MAcctSchema
                    .getClientAcctSchema(getCtx(), getAD_Client_ID(),
                            get_TrxName())));

        if (getCostTypeId() > 0) {
            costTypes.add(new MCostType(getCtx(), getCostTypeId(),
                    get_TrxName()));
            costTypesDelete = costTypes;
        }
        else {
        	// All active cost types
            costTypes = MCostType.get(getCtx(), get_TrxName());
            // All types including inactive entries
            costTypesDelete = new Query(getCtx(), MCostType.Table_Name, null, get_TrxName())
            		.setClient_ID()
            		.setOrderBy(MCostType.COLUMNNAME_M_CostType_ID)
            		.list();
        }
        if (getCostElementId() > 0) {
            costElements.add(MCostElement.get(getCtx(), getCostElementId()));
            costElementsDelete = costElements;
        }
        else {
            costElements = MCostElement.getCostElement(getCtx(), get_TrxName());
	        costElementsDelete = new Query(getCtx(), MCostElement.Table_Name, null, get_TrxName())
			.setClient_ID()
			.setOrderBy(MCostElement.COLUMNNAME_M_CostElement_ID)
			.list();
        }
    }

    /**
     * Apply Criteria for where clause
     *
     * @param accountSchemaId
     * @param costTypeId
     * @param costElementId
     * @param productId
     * @param dateAccount
     */
    private void applyCriteria(int accountSchemaId, int costTypeId,
                               int costElementId, int transactionId, 
                               int productId, Timestamp dateAccount, 
                               Timestamp dateAccountTo) 
    {
        deleteParameters = new ArrayList<Object>();
        resetCostParameters = new ArrayList<Object>();
        deleteCostDetailWhereClause = new StringBuffer("1=1");
        resetCostWhereClause = new StringBuffer("1=1");

        if (accountSchemaId > 0) {
            deleteCostDetailWhereClause.append(" AND ")
                    .append(MCostDetail.COLUMNNAME_C_AcctSchema_ID)
                    .append("=? ");
            deleteParameters.add(accountSchemaId);
            resetCostWhereClause.append(" AND ")
                    .append(MCostDetail.COLUMNNAME_C_AcctSchema_ID)
                    .append("=? ");
            resetCostParameters.add(accountSchemaId);
        }
        if (costTypeId > 0) {
            deleteCostDetailWhereClause.append(" AND ")
                    .append(MCostDetail.COLUMNNAME_M_CostType_ID).append("=? ");
            deleteParameters.add(costTypeId);
            resetCostWhereClause.append(" AND ")
                    .append(MCostDetail.COLUMNNAME_M_CostType_ID).append("=? ");
            resetCostParameters.add(costTypeId);//SHW
        }
        if (costElementId > 0) {
            deleteCostDetailWhereClause.append(" AND ")
                    .append(MCostDetail.COLUMNNAME_M_CostElement_ID)
                    .append("=? ");
            deleteParameters.add(costElementId);
            resetCostWhereClause.append(" AND ")
                    .append(MCostDetail.COLUMNNAME_M_CostElement_ID)
                    .append("=? ");
            resetCostParameters.add(costElementId);//SHW
        }
        if (productId > 0) {
            deleteCostDetailWhereClause.append(" AND ")
                    .append(MCostDetail.COLUMNNAME_M_Product_ID).append("=? ");
            deleteParameters.add(productId);
            resetCostWhereClause.append(" AND ")
                    .append(MCostDetail.COLUMNNAME_M_Product_ID).append("=? ");
            resetCostParameters.add(productId);
        }
        if (transactionId > 0) {
            deleteCostDetailWhereClause.append(" AND ")
                    .append(MCostDetail.COLUMNNAME_M_Transaction_ID).append("=? ");
            deleteParameters.add(transactionId);
        }
        if (dateAccount != null) {
            deleteCostDetailWhereClause.append(" AND ")
                    .append(MCostDetail.COLUMNNAME_DateAcct).append(">=? ");
            deleteParameters.add(dateAccount);
        }
        if (dateAccountTo != null) {
            deleteCostDetailWhereClause.append(" AND ")
                    .append(MCostDetail.COLUMNNAME_DateAcct).append("<=? ");
            deleteParameters.add(dateAccountTo); 
        }
        return;
    }

    public void generateCostDetail() {

        transactions = getTransactionIdsByDateAcct();
        // System.out.println("Transaction to process : " + transactions.length);
        
        // Delete all the cost details affected.  This will prevent any of the old ones 
        // being found as the lastCostDetail 
        deleteCostDetails();

        Integer productId = 0;
        boolean processNewProduct = true;
        Trx dbTransaction = null;

        try {

        	// There are 5 dimensions Product, Transaction, Schema, Cost method, Cost element
            // Process transaction - For each product/material transaction, sorted by Product
            for (KeyNamePair keyNamePair : transactions) {

                int transactionId = keyNamePair.getKey();
                int transactionProductId = new Integer(keyNamePair.getName());

                //Detected a new product
                if (productId != transactionProductId) {

                    //commit last transaction by product
                    if (dbTransaction != null) {
                        dbTransaction.commit(true);
                        dbTransaction.close();
                    }

                    productId = transactionProductId;
                    processNewProduct = true;

                    //Create new transaction for this product
                    dbTransaction = Trx.get(productId.toString(), true);

                    generateCostCollectorNotTransaction(productId, dbTransaction.getTrxName());
                }
                
                MTransaction transaction = new MTransaction(getCtx(), transactionId, dbTransaction.getTrxName());
                
                // for each Account Schema
                for (MAcctSchema accountSchema : acctSchemas) {
                    // for each Cost Type
                    for (MCostType costType : costTypes) {
                    	// for each Cost Element
                        for (MCostElement costElement : costElements) {

                        	applyCriteria(accountSchema.getC_AcctSchema_ID(),
                                    costType.getM_CostType_ID(), costElement.getM_CostElement_ID(),
                                    transactionId, productId, getAccountDate(), getAccountDateTo());
                            
                            // The following only have to be done once per product, Schema, cost type and cost element
                            if (processNewProduct) {
                                resetCostDimension(costType.getCostingMethod(), dbTransaction.getTrxName());
                            }

                        	// For products used in production and for certain cost types, defer the processing of the cost details
                            if (MCostType.COSTINGMETHOD_AverageInvoice.equals(costType.getCostingMethod())
                                    || MCostType.COSTINGMETHOD_AveragePO.equals(costType.getCostingMethod())) {
                                if (IsUsedInProduction(productId, dbTransaction.getTrxName())
                                	&& !deferredProductIds.contains(transaction.getM_Product_ID())) 
                                {
                                    deferredProductIds.add(productId);
                                    deferredTransactionIds.add(transactionId);
                                    continue;
                                }
                            }

                            if (deferredProductIds.contains(transaction.getM_Product_ID())
                            		&& !deferredTransactionIds.contains(transactionId)) {
                                deferredTransactionIds.add(transactionId);
                                continue;
                            }

                            if (deferredTransactionIds.contains(transactionId)) {
                                continue;
                            }

                            generateCostDetail(accountSchema, costType, costElement, transaction);
                        } // Cost elements
                    } // Cost types
                } // Schema

                processNewProduct = false;

                //process++;
                //System.out.println("Transaction : " + transactionId + " Transaction Type :"+ transaction.getMovementType() + " record ..." + process);
            } // Transaction * Product

            if (dbTransaction != null) {
                dbTransaction.commit(true);
                dbTransaction.close();
                dbTransaction = null;
            }

            //process deferred transaction for Average Invoice or Average PO
            final Comparator<Integer> orderTransaction =
                    new Comparator<Integer>() {
                        public int compare(Integer t1, Integer t2) {
                            return t2.compareTo(t1);
                        }
                    };

            Collections.sort(deferredTransactionIds, orderTransaction);
                //process deferred transaction for Average Invoice or Average PO
            for (Integer transactionId : deferredTransactionIds) {
                for (MAcctSchema accountSchema : acctSchemas) {
                    // for each Cost Type
                    for (MCostType costType : costTypes) {
                        if (MCostType.COSTINGMETHOD_AverageInvoice.equals(costType.getCostingMethod())
                        ||  MCostType.COSTINGMETHOD_AveragePO.equals(costType.getCostingMethod()))
                            ;
                        else
                            continue;
                        // for each Cost Element
                        for (MCostElement costElement : costElements) {
                                int transactionProductId = DB.getSQLValue(get_TrxName(), "SELECT M_Product_ID FROM M_Transaction WHERE M_Transaction_ID=?", transactionId);

                                //Detected a new product
                                if (productId != transactionProductId) {

                                    //commit last transaction by product
                                    if (dbTransaction != null) {
                                        dbTransaction.commit(true);
                                        dbTransaction.close();
                                    }

                                    productId = transactionProductId;

                                    //Create new transaction for this product
                                    dbTransaction = Trx.get(productId.toString(), true);

                                    //MProduct product = MProduct.get(Env.getCtx(), productId);
                                    //System.out.println("Deferred Product : " + product.getValue() + " Name :" + product.getName());

                                }

                                MTransaction transaction = new MTransaction(getCtx(), transactionId, dbTransaction.getTrxName());
                                generateCostDetail(accountSchema, costType, costElement, transaction);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (dbTransaction != null) {
                dbTransaction.rollback();
                dbTransaction.close();
                dbTransaction = null;
                e.printStackTrace();
                addLog(e.getMessage());
            }
        } finally {
            if (dbTransaction != null) {
                dbTransaction.commit();
                dbTransaction.close();
                dbTransaction = null;
            }
        }
    }

    /**
     * Delete the cost details.  The field transactions should be initialized prior to the call. 
     */
    private void deleteCostDetails() {

        //Create new transaction for the delete
        Trx dbTransaction = Trx.get("DeleteCostDetails", true);

        try {

        	// There are 5 dimensions Product, Transaction, Schema, Cost method, Cost element
            // Process transaction - For each product/material transaction, sorted by Product
            for (KeyNamePair keyNamePair : transactions) {

                int transactionId = keyNamePair.getKey();
                int productId = new Integer(keyNamePair.getName());
                
                // for each Account Schema
                for (MAcctSchema accountSchema : acctSchemas) {
                    // for each Cost Type
                    for (MCostType costType : costTypesDelete) {
                    	// for each Cost Element
                        for (MCostElement costElement : costElementsDelete) {

                        	log.fine("Deleting and resetting cost info for\n" + 
                        			"    acctSchema: " + accountSchema.toString() + "\n" +
                        			"    costType: " + costType.toString() + "\n" + 
                        			"    costElement: " + costElement.toString() + "\n" + 
                        			"    Product ID=" + productId + "\n" +
                        			"    Material Transaction ID =" + transactionId + "\n" +
                        			"    DateAcct from " + getAccountDate() + " to " + getAccountDateTo());
                        	applyCriteria(accountSchema.getC_AcctSchema_ID(),
                                    costType.getM_CostType_ID(), costElement.getM_CostElement_ID(),
                                    transactionId, productId, getAccountDate(), getAccountDateTo());
                            deleteCostDetail(dbTransaction.getTrxName());
                        } // Cost elements
                    } // Cost types
                } // Schema

            } // Transaction * Product
	    } catch (Exception e) {
	        if (dbTransaction != null) {
	            dbTransaction.rollback();
	            dbTransaction.close();
	            dbTransaction = null;
	            e.printStackTrace();
	        }
	    } finally {
	        if (dbTransaction != null) {
	            dbTransaction.commit();
	            dbTransaction.close();
	            dbTransaction = null;
	        }
	    }
    }

    private boolean IsUsedInProduction(int productId, String trxName) {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append(MTransaction.COLUMNNAME_M_Product_ID).append("=? AND ");
        whereClause.append(MTransaction.COLUMNNAME_MovementType).append("=?");

        return new Query(getCtx(), MTransaction.Table_Name, whereClause.toString(), trxName)
                .setClient_ID()
                .setParameters(productId, MTransaction.MOVEMENTTYPE_ProductionPlus)
                .match();
    }

    public void generateCostDetail(MAcctSchema accountSchema, MCostType costType, MCostElement costElement, MTransaction transaction) {

        //Create Cost Detail for this Transaction
        CostEngineFactory.getCostEngine(accountSchema.getAD_Client_ID())
                .createCostDetail(accountSchema, costType, costElement, transaction,
                        transaction.getDocumentLine(), true);
        CostEngineFactory.getCostEngine(accountSchema.getAD_Client_ID())
                .clearAccounting(accountSchema, transaction);

        // Calculate adjustment cost by variances in
        // invoices
        if (MTransaction.MOVEMENTTYPE_VendorReceipts.equals(transaction.getMovementType())) {

            MInOutLine line = (MInOutLine) transaction.getDocumentLine();

            if (MCostElement.COSTELEMENTTYPE_Material.equals(costElement.getCostElementType())) {
                //get purchase matches
                List<MMatchPO> orderMatches = MMatchPO
                        .getInOutLine(line);
                orderMatches.stream().forEach(match -> {
                    if (match.getM_Product_ID() == transaction.getM_Product_ID()
                    && match.getDateAcct().after(getAccountDate())
                    && match.getDateAcct().before(getAccountDateTo())) {
                        CostEngineFactory.getCostEngine(
                                accountSchema.getAD_Client_ID())
                                .createCostDetail(accountSchema, costType, costElement, transaction,
                                        match, true);
                    }
                });
                //get invoice matches
                List<MMatchInv> invoiceMatches = MMatchInv
                        .getInOutLine(line);
               invoiceMatches.forEach(match -> {
                    if (match.getM_Product_ID() == transaction.getM_Product_ID()
                    && match.getDateAcct().after(getAccountDate())
                    && match.getDateAcct().before(getAccountDateTo())) {
                        CostEngineFactory.getCostEngine(
                                accountSchema.getAD_Client_ID())
                                .createCostDetail(accountSchema, costType, costElement, transaction,
                                        match, true);
                    }
                });
            }

            //get landed allocation cost
            MLandedCostAllocation.getOfInOutline(line, costElement.getM_CostElement_ID()).stream().forEach(allocation -> {
                if (allocation.getDateAcct().after(getAccountDate())
                 && allocation.getDateAcct().before(getAccountDateTo()))
                CostEngineFactory
                        .getCostEngine(accountSchema.getAD_Client_ID())
                        .createCostDetail(accountSchema, costType, costElement, transaction, allocation, true);
            });
        }
    }

    private void generateCostCollectorNotTransaction(int productId, String trxName)
            throws SQLException {
        List<MPPCostCollector> costCollectors = MPPCostCollector
                .getCostCollectorNotTransaction(getCtx(), productId, getAccountDate(), getAccountDateTo(), trxName);
        // Process Collector Cost Manufacturing
        for (MPPCostCollector costCollector : costCollectors) {
            for (MCostDetail costDetail : MCostDetail.getByCollectorCost(costCollector)) {
                costDetail.deleteEx(true);
            }

            final StandardCostingMethod standardCostingMethod = (StandardCostingMethod) CostingMethodFactory.get()
                    .getCostingMethod(X_M_CostType.COSTINGMETHOD_StandardCosting);

            if (MPPCostCollector.COSTCOLLECTORTYPE_UsegeVariance.equals(costCollector.getCostCollectorType()))
                standardCostingMethod.createUsageVariances(costCollector);
            else if (MPPCostCollector.COSTCOLLECTORTYPE_MethodChangeVariance.equals(costCollector.getCostCollectorType()))
                standardCostingMethod.createMethodVariances(costCollector);
            else if (MPPCostCollector.COSTCOLLECTORTYPE_RateVariance.equals(costCollector.getCostCollectorType()))
                standardCostingMethod.createRateVariances(costCollector);
            else if (MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl.equals(costCollector.getCostCollectorType()))
                standardCostingMethod.createActivityControl(costCollector);
            //else
                //System.out.println("Cost Collector Type: " + costCollector.getCostCollectorType());
        }
    }

    private KeyNamePair[] getTransactionIdsByDateAcct() {
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<Object>();
        StringBuilder whereClause = new StringBuilder("WHERE ");
        whereClause.append(MCostDetail.COLUMNNAME_AD_Client_ID).append("=")
                .append(getAD_Client_ID()).append(" AND ");
        if (getProductId() > 0) {
            whereClause.append(MCostDetail.COLUMNNAME_M_Product_ID)
                    .append("=?").append(" AND ");
            parameters.add(getProductId());
        }
        whereClause.append("TRUNC(").append(MCostDetail.COLUMNNAME_DateAcct).append(")>=?");
        parameters.add(getAccountDate());

        if (getAccountDateTo() != null) {
            whereClause.append(" AND TRUNC(").append(MCostDetail.COLUMNNAME_DateAcct).append(")<=?");
            parameters.add(getAccountDateTo());
        }

        sql.append("SELECT M_Transaction_ID , M_Product_ID FROM RV_Transaction ")
                .append(whereClause)
                .append(" ORDER BY M_Product_ID ,  TRUNC( DateAcct ) , reversalline_id , SUBSTR(MovementType,2,1) Desc ");
        // .append(" ORDER BY M_Product_ID ,  TRUNC( DateAcct ) , M_Transaction_ID , SUBSTR(MovementType,2,1) ");
        //.append(" ORDER BY M_Product_ID , DateAcct , M_Transaction_ID");
        //System.out.append("SQL :" + sql);
        return DB.getKeyNamePairs(get_TrxName(), sql.toString(), false, parameters.toArray());
    }
}
