package org.adempiere.util.attributes;

import static java.util.Objects.requireNonNull;
import static org.compiere.model.I_M_AttributeSetInstance.COLUMNNAME_AD_Org_ID;
import static org.compiere.model.I_M_AttributeSetInstance.COLUMNNAME_GuaranteeDate;
import static org.compiere.model.I_M_AttributeSetInstance.COLUMNNAME_Lot;
import static org.compiere.model.I_M_AttributeSetInstance.COLUMNNAME_M_AttributeSetInstance_ID;
import static org.compiere.model.I_M_AttributeSetInstance.COLUMNNAME_M_AttributeSet_ID;
import static org.compiere.model.I_M_AttributeSetInstance.COLUMNNAME_SerNo;
import static org.compiere.model.I_M_AttributeSetInstance.Table_Name;
import static org.compiere.model.X_M_Attribute.ATTRIBUTEVALUETYPE_List;
import static org.compiere.model.X_M_Attribute.ATTRIBUTEVALUETYPE_Number;
import static org.compiere.model.X_M_Attribute.ATTRIBUTEVALUETYPE_StringMax40;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_M_InOutLine;
import org.compiere.model.I_M_Product;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MColumn;
import org.compiere.model.MProduct;
import org.compiere.model.MStorage;
import org.compiere.model.MTransaction;
import org.compiere.model.Query;
import org.compiere.model.X_C_AcctSchema;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

public class AttributeUtilities {
	
	protected static CLogger log = CLogger.getCLogger(AttributeUtilities.class);
	
	protected static final Integer I_ZERO = 0;
	
	public static final boolean NOT_MANDATORY = false;
	public static final boolean MANDATORY = true;
	public static final boolean NOT_INSTANCE_ATTRIBUTE = false;
	public static final boolean INSTANCE_ATTRIBUTE = true;
	
	public static final String ATTRIBUTE_UTILITIES_MISSING_ATTRIBUTE_INSTANCE_VALUES 
			= "@LinesWithoutMandatoryAttributeInstanceValues@ ";

	private static final String AND = " AND ";
	private static final String IS_NULL = " is null";

	private AttributeUtilities() {}
	
	private static boolean attributeDoesntMatchValue(MAttributeSetInstance asi, MAttribute attribute, Object value) {
		
		if (attribute == null  && value == null)
			return false;
		
		if (attribute == null)
			return true;
			
		MAttributeInstance instance = attribute.getMAttributeInstance (asi.getM_AttributeSetInstance_ID());
	
		if (value == null && instance == null)
			return false;
		
		if (value != null && instance == null 
			|| value == null)
			return true;
				
		return attributeInstanceDoesntMatchValue(attribute, value, instance);
	
	}

	private static boolean attributeInstanceDoesntMatchValue(MAttribute attribute, 
			Object value, MAttributeInstance instance) {
		if (ATTRIBUTEVALUETYPE_List.equals(attribute.getAttributeValueType()))
		{
			if (((MAttributeValue) value).getM_AttributeValue_ID() 
					!= instance.getM_AttributeValue_ID()) {
				return true;
			}
		}
		else if (ATTRIBUTEVALUETYPE_Number.equals(attribute.getAttributeValueType()))
		{
			if (((BigDecimal) value).compareTo(instance.getValueNumber()) != 0) {
				return true;
			}
		}
		else if(ATTRIBUTEVALUETYPE_StringMax40.equals(attribute.getAttributeValueType()))	
		{
			if (!((String) value).equals(instance.getValue())) {
				return true;
			}
		}
		else
		{
			throw new AdempiereException("Unknown attribute value type: " 
						+ attribute.getAttributeValueType());
		}
	
		return false;
	}

	private static boolean attributeSetInstanceIsNotCompatibleWithProduct(MProduct product, MAttributeSetInstance masi) {
		return masi != null && masi.getMAttributeSet().get_ID() != product.getM_AttributeSet_ID();
	}

	private static boolean attributeSetIsExcludedAndASIIsZero(MAttributeSet as, 
			int columnId, boolean isSOTrx, int asi_id) {
		return as.excludeEntry(columnId, isSOTrx) && asi_id == 0;
	}

	private static boolean attributeSetIsExcludedAndASIIsNotZero(MAttributeSet as, 
			int columnId, boolean isSOTrx, int asi_id) {
		return as.excludeEntry(columnId, isSOTrx) && asi_id != 0;
	}

	private static boolean costingLevelBatchLotOverridesExcludesAndMandatorySettings(MProduct product) {
		return Arrays.stream(MAcctSchema.getClientAcctSchema(product.getCtx(), product.getAD_Client_ID(), product.get_TrxName()))
			.anyMatch(as -> X_C_AcctSchema.COSTINGLEVEL_BatchLot.equals(product.getCostingLevel(as)));
	}

	private static boolean lotSerialNoOrGuaranteeDateDontExistButAreRequired(Properties ctx, MProduct product, boolean isSOTrx,
			int columnId, String trxName, MAttributeSet as, MAttributeSetInstance masi) {
		
		if (as.isLot() && as.isLotMandatory() && !as.isExcludeLot(columnId, isSOTrx) 
				&& (masi == null || masi.getLot() == null || masi.getLot().isEmpty()))
				return true;
		
		if (as.isSerNo() && as.isSerNoMandatory() && !as.isExcludeSerNo(columnId, isSOTrx) 
				&& (masi == null || masi.getSerNo() == null || masi.getSerNo().isEmpty()))
				return true;
		
		if (as.isGuaranteeDate() && as.isGuaranteeDateMandatory() 
				&& (masi == null || masi.getGuaranteeDate() == null))
				return true;
		
		int table_id = MColumn.getTable_ID(ctx, columnId, trxName);
		
		return isAttributeSetInstanceMandatory(ctx, product, table_id, isSOTrx, trxName) 
				&& (masi == null || !hasMandatoryValues(masi));
	}

	private static boolean isProductWindow(int columnId) {
		return 	columnId == MColumn.getColumn_ID(I_M_Product.Table_Name, 
				I_M_Product.COLUMNNAME_M_AttributeSetInstance_ID);
	}

	private static boolean isValueNull(MAttribute attribute, 
			MAttributeSetInstance attributeSetInstance) {
		
		requireNonNull(attribute);
	
		MAttributeInstance instance = attribute
				.getMAttributeInstance (attributeSetInstance.getM_AttributeSetInstance_ID());
	
		if (instance == null)
		{
			return true;
		}
		else if (ATTRIBUTEVALUETYPE_List.equals(attribute.getAttributeValueType()))
		{
			if (instance.getM_AttributeValue_ID() <= 0)
				return true;
		}
		else if (ATTRIBUTEVALUETYPE_StringMax40.equals(attribute.getAttributeValueType()))
		{
			if (instance.getValue() == null || instance.getValue().isEmpty())
				return true;
		}
		else if (ATTRIBUTEVALUETYPE_Number.equals(attribute.getAttributeValueType())
				&& instance.getValueNumber() == null)
		{
				return true;
		}
		
		return false;
		
	}

	private static boolean productASImatches(MProduct product, int asi_id) {
		return product.getM_AttributeSetInstance_ID() > 0 && asi_id == product.getM_AttributeSetInstance_ID();
	}

	private static boolean productHasMandatorySerialNumber(MProduct product) {
		MAttributeSet as = (MAttributeSet) product.getM_AttributeSet();
		return as.isSerNo() && as.isSerNoMandatory();
	}

	private static boolean receivingMaterial(BigDecimal movementQty, String movementType) {
		if (Boolean.TRUE.equals(MTransaction.isIncomingTransaction(movementType))) 
			return movementQty.signum() > 0; 
		else  
			return movementQty.signum() < 0;
	}

	private static boolean serialNumberAlreadyExistsInStock(MProduct product, int attributeSetInstanceId,
			String trxName) {
		BigDecimal available = MStorage.getQtyOnHand(product.getCtx(), 
				product.getM_Product_ID(), attributeSetInstanceId, 0, trxName);
		
		return available.signum() > 0;
	}

	private static void setWhereAndParameters(MAttributeSetInstance asi, StringBuilder where, List<Object> parameters) {
		
		where.append(COLUMNNAME_AD_Org_ID + " in (?,0)");
		parameters.add(asi.getAD_Org_ID());
		
		where.append(AND).append(COLUMNNAME_M_AttributeSet_ID).append("=?");
		parameters.add(asi.getM_AttributeSet_ID());
		//
		
		if (asi.getGuaranteeDate() != null) {
			where.append(AND).append(COLUMNNAME_GuaranteeDate).append("=?");
			parameters.add(asi.getGuaranteeDate());
		}
		else
			where.append(AND).append(COLUMNNAME_GuaranteeDate).append(IS_NULL);
		//
		if (asi.getLot() != null) {
			where.append(AND).append(COLUMNNAME_Lot).append("=?");
			parameters.add(asi.getLot());
		}
		else
			where.append(AND).append(COLUMNNAME_Lot).append(IS_NULL);
		//
		if (asi.getSerNo() != null) {
			where.append(AND).append(COLUMNNAME_SerNo).append("=?");
			parameters.add(asi.getSerNo());
		}
		else
			where.append(AND).append(COLUMNNAME_SerNo).append(IS_NULL);
		//
		where.append(AND).append(COLUMNNAME_M_AttributeSetInstance_ID).append("!=?");			
		parameters.add(asi.getM_AttributeSetInstance_ID());
	
		
	}

	/**
	 * Finds an existing Attribute Set Instance that is identical to the provided set
	 * of values.  The match looks at the M_AttributeSetInstance and the 
	 * M_AttributeInstance tables for duplicates across the main fields. 
	 * 
	 * @return The M_AttributeSetInstance_ID of the first matching instance is returned or 
	 * zero if no match is found.
	 */
	public static int findMatchingAttributeSetInstance(Properties ctx, 
			MAttributeSetInstance asi, Object[] values, String trxName) {
		
		requireNonNull(asi);
		
		List<Object> parameters = new ArrayList<>();
		StringBuilder where = new StringBuilder();

		setWhereAndParameters(asi, where, parameters);
		
		return new Query(ctx, Table_Name, where.toString(), trxName)
					.setClient_ID()
					.setParameters(parameters)
					.setOrderBy(COLUMNNAME_M_AttributeSetInstance_ID + " ASC")
					.list()
					.stream()
					.filter(match -> match.getAD_Org_ID() == asi.getAD_Org_ID())
					.filter(match -> hasValues((MAttributeSetInstance) match, values))
					.map(match -> ((MAttributeSetInstance) match)
							.getM_AttributeSetInstance_ID())
					.findFirst()
					.orElse(0)
					.intValue();
		
	}
	
	/**
	 * Determines if the values of an attribute set instance match the provided set of values.
	 * @param values in the order provided by MAttributeSet.getMAttributes()
	 * @return true if the values provided match those of this attribute set. Will return 
	 * false if values is null or the values do not match.
	 */
	public static boolean hasValues(MAttributeSetInstance asi, Object[] values) {
		
		if (values == null)
			return false;
		
		MAttributeSet as = asi.getMAttributeSet();
		if (as == null)
			return false;
		
		MAttribute[] attributes = as.getMAttributes();
		
		if (attributes.length != values.length)
			return false;

		for (int i = 0; i < attributes.length; i++) {
			MAttribute attribute = attributes[i];
			Object value = values[i];

			if (attributeDoesntMatchValue(asi, attribute, value))
				return false;			
		}
		return true;
	} // hasValues
	
	
	/**
	 * Determines if the mandatory attributes of an attribute set are included in this instance.
	 * @return true if the mandatory attribute values exist.
	 */
	public static boolean hasMandatoryValues(MAttributeSetInstance asi) {
		
		requireNonNull(asi);		
		MAttributeSet as = requireNonNull(asi.getMAttributeSet());
		
		return Stream.of(as.getMAttributes())
				.filter(attribute -> attribute.isMandatory() && isValueNull(attribute, asi))
				.map(attribute -> false)
				.findFirst()
				.orElse(true);
		
	}
	
	/**
	 * Returns true if the serial number will not violate uniqueness rules.  Assumes
	 * the attribute set instance is valid for the column and product. (See #281)
	 * @param product MProduct associated with the attribute set instance
	 * @param attributeSetInstanceId
	 * @param movementQty
	 * @param movementType
	 * @return
	 */
	public static boolean isUniqueAttributeSetInstance(MProduct product, int attributeSetInstanceId, 
			BigDecimal movementQty, String movementType,
			String trxName) {
		
		requireNonNull(product);
		requireNonNull(movementQty);
		requireNonNull(movementType);

		if (attributeSetInstanceId == 0  
			|| movementQty.signum() == 0 
			|| product.getM_Product_ID() == 0 
			|| !product.isStocked()
			|| !productHasMandatorySerialNumber(product))
			return true;

		return movementQty.abs().compareTo(Env.ONE) == 0
				&& !(receivingMaterial(movementQty, movementType)
					&& serialNumberAlreadyExistsInStock(product, attributeSetInstanceId, trxName));
		
	}

	/**
	 * Test the provided Attribute Set Instance for validity and completeness.  This test determines 
	 * if the attribute set instance has all mandatory value instances where the values and attribute
	 * set instance itself are not excluded. (See #281)
	 * @param isSOTrx
	 * @param columnId
	 * @param m_AttributeSetInstance_ID 
	 * @return Return true if the Attribute Set Instance is valid, not excluded and complete, otherwise false.
	 */
	public static boolean isValidAttributeSetInstance(Properties ctx, MProduct product, boolean isSOTrx, 
			int columnId, Integer attributeSetInstanceId, String trxName) {
		
		final boolean valid = true;
		final boolean anythingGoes = true;
		final boolean invalid = false;
		
		int asi_id = Optional.ofNullable(attributeSetInstanceId).orElse(0);
		
		if (product.getM_AttributeSet_ID() == 0  && asi_id == 0)  
			return valid;

		if (asi_id == 0)
			return invalid;

		MAttributeSet as = (MAttributeSet) product.getM_AttributeSet();
		MAttributeSetInstance asi = MAttributeSetInstance.get(ctx, asi_id, 0, trxName);
		
		if (as == null
			|| attributeSetInstanceIsNotCompatibleWithProduct(product, asi)
			|| attributeSetIsExcludedAndASIIsNotZero(as, columnId, isSOTrx, asi_id)
			)
			return invalid;
		
		if (attributeSetIsExcludedAndASIIsZero(as, columnId, isSOTrx, asi_id))
			return valid;

		if (isProductWindow(columnId))
			return anythingGoes;  // In the product window, anything goes as long as there is a match in the attribute set.
		
		if (as.isInstanceAttribute()) {
			if (lotSerialNoOrGuaranteeDateDontExistButAreRequired(ctx, product, isSOTrx, columnId, trxName, as, asi))
				return invalid;
		}
		else {
			if (productASImatches(product, asi_id))
				return valid;
		}
		
		return hasMandatoryValues(asi);
	}

	/**
	 * Is AttributeSet Instance Mandatory
	 * @param product
	 * @param tableId
	 * @param isSOTrx
	 * @param attributeSetIntanceId
	 * @return 
	 */
	public static boolean isAttributeSetInstanceMandatory(Properties ctx, MProduct product, int tableId, boolean isSOTrx, String trxName)
	{
		
		requireNonNull(product);
		
		MAttributeSet as = MAttributeSet.get(ctx, product.getM_AttributeSet_ID(), trxName);

		if (!as.isInstanceAttribute())
			return false;

		if (costingLevelBatchLotOverridesExcludesAndMandatorySettings(product))
		{
			return true;
		}

		boolean notExcluded = !as.excludeEntry(tableId, isSOTrx);
		
		return notExcluded && as.isMandatory() 
				&& (as.isMandatoryAlways() 
				|| (as.isMandatoryShipping() && I_M_InOutLine.Table_ID == tableId && isSOTrx));
		
	}

	public static String validateAttributeSetInstanceMandatory(IDocumentLine line) {
		
		return validateAttributeSetInstanceMandatory(line.getCtx(), 
			MProduct.get(line.getCtx(), line.getM_Product_ID(), line.get_TrxName()), 
			line.get_Table_ID(), line.isSOTrx(), 
			line.getM_AttributeSetInstance_ID(), line.get_TrxName());
		
	}

	/**
	 * Validate if an AttributeSetInstance value is mandatory for the product
	 * @param product
	 * @param tableId
	 * @param isSOTrx
	 * @param attributeSetIntanceId
	 */
	@Deprecated
	public static String validateAttributeSetInstanceMandatory(Properties ctx, 
			MProduct product, int tableId, boolean isSOTrx , 
			int attributeSetIntanceId, String trxName)
	{
		if (product == null) 
			return null;
		
		if (isAttributeSetInstanceMandatory(ctx, product, tableId, isSOTrx, trxName)
			&& (attributeSetIntanceId == 0 
			|| !hasMandatoryValues(MAttributeSetInstance.get(ctx, 
					attributeSetIntanceId, 0, trxName))))
		{
			return ATTRIBUTE_UTILITIES_MISSING_ATTRIBUTE_INSTANCE_VALUES + product.getName();			
		}

		return null;
	}
	
	/**
	 * Verify that the document lines reference Attribute Set Instances where
	 * these are mandatory
	 * @param lines
	 * @return A string if there is an error or null if all lines are valid
	 */
	public static String verifyMandatoryAttributeSetInstancesExist(IDocumentLine[] lines) {
		
		requireNonNull(lines);
		
		return Stream.of(lines)
				.map(AttributeUtilities::validateAttributeSetInstanceMandatory)
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);				
		
	}
}
