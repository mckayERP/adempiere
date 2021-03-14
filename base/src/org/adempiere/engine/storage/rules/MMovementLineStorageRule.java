package org.adempiere.engine.storage.rules;

import static java.util.Objects.requireNonNull;
import static org.compiere.model.X_M_Transaction.MOVEMENTTYPE_MovementFrom;
import static org.compiere.model.X_M_Transaction.MOVEMENTTYPE_MovementTo;

import java.math.BigDecimal;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.engine.storage.StorageTransactionInfo;
import org.adempiere.engine.storage.StorageTransactionInfoBuilder;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrderLine;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

public class MMovementLineStorageRule extends StorageEngineRuleAbstract
        implements StorageEngineRule<IDocumentLine, String> {

    protected static CLogger log =
            CLogger.getCLogger(MMovementLineStorageRule.class);

    protected StorageTransactionInfoBuilder infoBuilder = null;
    protected MMovementLine moveLine = null;
    protected MOrderLine orderLine = null;

    public MMovementLineStorageRule() {

        super();

    }

    @Override
    public boolean matches(IDocumentLine line) {

        return (line instanceof MMovementLine);

    }

    @Override
    public StorageTransactionInfo createTransactionInfo(IDocumentLine line) {

        // for the Move From side of the Movement
        moveLine = (MMovementLine) requireNonNull(line);
        MMovement move = (MMovement) requireNonNull(line.getParent());

        MLocator locator = (MLocator) moveLine.getM_Locator();

        int orderWarehouseID = 0;
        int orderMPolicyTicketID = 0;
        int orderAttributeSetInstanceID = 0;

        return getASetOfStorageTransactionInfo()
                .withDocumentLine(line)
                .withMovementType(MOVEMENTTYPE_MovementFrom)
                .withMovementDate(moveLine.getMovementDate())
                .withMovementQty(moveLine.getMovementQty())
                .withM_Product_ID(moveLine.getM_Product_ID())
                .withTransactionAttributeSetInstance_id(
                        moveLine.getM_AttributeSetInstance_ID())
                .withTransactionWarehouse_id(locator.getM_Warehouse_ID())
                .withTransactionLocator_id(locator.get_ID())
                .withOrderWarehouse_id(orderWarehouseID)
                .withOrderAttributeSetInstance_id(orderAttributeSetInstanceID)
                .withOrderMPolicyTicket_id(orderMPolicyTicketID)
                .withSOTrx(false)
                .withCreateMaterialAllocation(!move.isReversal())
                .withProcessMA(false)
                .build();

    }

    protected StorageTransactionInfo
            createTransactionInfoTo(IDocumentLine line) {

        // for the Move To side of the Movement
        moveLine = (MMovementLine) requireNonNull(line);
        MMovement move = (MMovement) requireNonNull(line.getParent());

        MLocator locator = (MLocator) moveLine.getM_LocatorTo();

        int orderWarehouseID = 0;
        int orderMPolicyTicketID = 0;
        int orderAttributeSetInstanceID = 0;

        return getASetOfStorageTransactionInfo()
                .withDocumentLine(line)
                .withMovementType(MOVEMENTTYPE_MovementTo)
                .withMovementDate(moveLine.getMovementDate())
                .withMovementQty(moveLine.getMovementQty())
                .withM_Product_ID(moveLine.getM_Product_ID())
                .withTransactionAttributeSetInstance_id(
                        moveLine.getM_AttributeSetInstanceTo_ID())
                .withTransactionWarehouse_id(locator.getM_Warehouse_ID())
                .withTransactionLocator_id(locator.get_ID())
                .withOrderWarehouse_id(orderWarehouseID)
                .withOrderAttributeSetInstance_id(orderAttributeSetInstanceID)
                .withOrderMPolicyTicket_id(orderMPolicyTicketID)
                .withSOTrx(false)
                .withCreateMaterialAllocation(!move.isReversal())
                .withProcessMA(true)
                .build();

    }

    public String process(IDocumentLine line) {

        try {

            setStorageTransactionInfo(createTransactionInfo(line));

            if (productIsStocked) {
                deleteExistingMaterialAllocations();
                allocateMaterial();
                setStorageTransactionInfo(createTransactionInfoTo(line));
                allocateMaterial();
                updateStorageRecords();
                save(line);
            }
            return "";

        } catch (AdempiereException e) {
            return e.getMessage();
        }

    }

    @Override
    protected void clearStorageRelatedFieldsInModel() {

        // Nothing to do

    }

    @Override
    protected void setStorageRelatedFieldsInModel() {

        // Nothing to do

    }

    @Override
    protected BigDecimal getChangeInQtyOnHand() {

        // Not used
        return Env.ZERO;

    }

    @Override
    protected BigDecimal getChangeInQtyOrdered() {

        // Not used
        return Env.ZERO;

    }

    @Override
    protected BigDecimal getChangeInQtyReserved() {

        // Not used
        return Env.ZERO;

    }

}
