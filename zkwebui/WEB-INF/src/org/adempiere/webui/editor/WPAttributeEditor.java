/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2021 ADempiere Foundation, All Rights Reserved.         *
 * Copyright (C) 2008 Low Heng Sin  All Rights Reserved.                      *
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

package org.adempiere.webui.editor;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import org.adempiere.controller.ed.AttributeEditorControllerImpl;
import org.adempiere.controller.ed.AttributeEditorController;
import org.adempiere.controller.ed.NullAttributeEditorControllerImpl;
import org.adempiere.controller.form.AttributeDialogInfo;
import org.adempiere.controller.ed.AttributeEditor;
import org.adempiere.exceptions.ValueChangeEvent;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.PAttributebox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.event.ContextMenuEvent;
import org.adempiere.webui.event.ContextMenuListener;
import org.adempiere.webui.panel.InfoPAttributePanel;
import org.adempiere.webui.panel.InfoProductPanel;
import org.adempiere.webui.window.WPAttributeDialog;
import org.adempiere.webui.window.WRecordInfo;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MPAttributeLookup;
import org.compiere.model.MQuery;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;

/**
 *
 * @author Low Heng Sin
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *         <li>FR [ 146 ] Remove unnecessary class, add support for info to
 *         specific column
 * @see https://github.com/adempiere/adempiere/issues/146
 *      <a href="https://github.com/adempiere/adempiere/issues/966">
 * @see FR [ 966 ] ZK Dialog for Attribute don't change gridfield</a>
 */
public class WPAttributeEditor extends WEditor
        implements ContextMenuListener, AttributeEditor {

    private static final String[] LISTENER_EVENTS =
            { Events.ON_CLICK, Events.ON_CHANGE, Events.ON_OK };

    private static final CLogger log =
            CLogger.getCLogger(WPAttributeEditor.class);

    private int windowNo;
    private WEditorPopupMenu popupMenu;

    /** Calling Window Info */
    private int columnId = 0;

    private AttributeEditorController controller;

    /** No Instance Key */

    private WPAttributeDialog attributeDialog;

    public WPAttributeEditor(GridTab tab, GridField field) {

        super(new PAttributebox(), field);
        gridTab = tab;
        windowNo = field.getWindowNo();
        columnId = field.getAD_Column_ID();
        gridField = field;

        controller = createController(tab, field);
        controller.setSearchOnly(false);
        controller.setGridField(field);

        initComponents();
        controller.init();

    }

    AttributeEditorControllerImpl createController(GridTab tab, GridField field) {

        return new AttributeEditorControllerImpl(this, tab,
                field.isMandatory(false),
                !field.isReadOnly(), field.isUpdateable(), windowNo,
                field.getLookup());

    }

    /**
     * Create Product Attribute Set Instance Editor.
     * 
     * @param tab
     * @param mandatory       mandatory
     * @param isReadOnly      read only
     * @param isUpdateable    updateable
     * @param windowNo        WindowNo
     * @param attributeLookup Model Product Attribute
     * @param isSearchOnly    True if only used to search instances
     */
    public WPAttributeEditor(GridTab tab,
            boolean mandatory,
            boolean isReadOnly,
            boolean isUpdateable,
            int windowNo,
            MPAttributeLookup attributeLookup,
            boolean isSearchOnly) {

        super(new PAttributebox(), DEFAULT_COLUMN_NAME, null, null, mandatory,
                isReadOnly, isUpdateable);

        requireNonNull(attributeLookup);
        gridTab = tab;
        this.windowNo = windowNo;
        if (gridField == null && gridTab != null) {
            gridField = this.gridTab.getField(DEFAULT_COLUMN_NAME);
        }
        if (this.gridField != null) {
            this.columnId = this.gridField.getAD_Column_ID();
        }

        controller = new AttributeEditorControllerImpl(this, tab, mandatory,
                isReadOnly, isUpdateable, windowNo, attributeLookup);
        controller.setSearchOnly(isSearchOnly);

        initComponents();

        controller.setValue(0);
        controller.init();

    }

    private void initComponents() {

        getComponent().setButtonImage("images/PAttribute10.png");
        getComponent().addEventListener(Events.ON_CLICK, this);
        getComponent().addEventListener(Events.ON_CHANGE, this);

        popupMenu = new WEditorPopupMenu(true, false, false);
        getComponent().getTextbox().setContext(popupMenu.getId());

        if (super.gridField != null && super.gridField.getGridTab() != null) {
            WRecordInfo.addMenu(popupMenu);
        }

    }

    @Override
    public WEditorPopupMenu getPopupMenu() {

        return popupMenu;

    }

    @Override
    public PAttributebox getComponent() {

        return (PAttributebox) component;

    }

    @Override
    public void setValue(Object value) {

        getController().setValue(value);

    } // setValue

    public String getAttributeWhere() {

        return getController().getAttributeWhere();

    }

    @Override
    public Object getValue() {

        return getController().getValue();

    }

    @Override
    public String getDisplay() {

        return getComponent().getText();

    }

    public void onEvent(Event event) {

        if (Events.ON_CHANGE.equals(event.getName())
                || Events.ON_OK.equals(event.getName())) {
            getController().actionText();
            event.stopPropagation();
        } else if (Events.ON_CLICK.equals(event.getName())) {
            controller.actionButton();
        }

    }

    /**
     * Start dialog
     */
    private void cmd_dialog() {

//		Integer oldValueDialog = 0;
//		try
//		{
//			oldValueDialog = (Integer)getValue ();
//		}
//		catch(Exception npe)
//		{
//			// Possible Invalid Cast exception if getValue() return new instance of Object.
//		}
//		int oldValueInt = oldValueDialog == null ? 0 : oldValueDialog.intValue ();
//		int attributeSetInstanceId = oldValueInt;
//		int productId = 0;
//		int productBOMId = 0;
//		if (this.gridTab != null) {
//			productId = Env.getContextAsInt (Env.getCtx (), this.windowNo, this.gridTab.getTabNo(), "M_Product_ID");
//			productBOMId = Env.getContextAsInt (Env.getCtx (), this.windowNo, this.gridTab.getTabNo(), "M_ProductBOM_ID");
//		} else {
//			productId = Env.getContextAsInt (Env.getCtx (), this.windowNo, "M_Product_ID");
//			productBOMId = Env.getContextAsInt (Env.getCtx (), this.windowNo, "M_ProductBOM_ID");
//		}
//		int locatorId = -1;
//
//		log.config("M_Product_ID=" + productId + "/" + productBOMId
//			+ ",M_AttributeSetInstance_ID=" + attributeSetInstanceId);
//
//		//	M_Product.M_AttributeSetInstance_ID = 8418
//		boolean productWindow = this.columnId == MColumn.getColumn_ID(I_M_Product.Table_Name, I_M_Product.COLUMNNAME_M_AttributeSetInstance_ID);
//
//		//	Exclude ability to enter ASI
//		boolean exclude = true;
//
//		if (productId != 0)
//		{
//			MProduct product = MProduct.get(Env.getCtx(), productId);
//			int attributeSetId = product.getM_AttributeSet_ID();
//			if (attributeSetId != 0)
//			{
//				int tableId = MColumn.getTable_ID(Env.getCtx(), this.columnId, null);
//				MAttributeSet mas = MAttributeSet.get(Env.getCtx(), attributeSetId);
//				exclude = mas.excludeEntry(tableId, Env.isSOTrx(Env.getCtx(), this.windowNo));
//			}
//		}
//
//		boolean changed = false;
//		if (productBOMId != 0)	//	Use BOM Component
//			productId = productBOMId;
//		//
//		// If the VPAttribute component is in a dialog, use the search
//		if (this.isSearchOnly)
//		{	
//			// Determine if the component is associated with the InfoProduct window
//			Component me = ((Component) this.component.getParent());
//			while (me != null)
//			{
//				if (me instanceof InfoProductPanel)
//					break;
//				me = me.getParent();
//			}
//			//
//			InfoPAttributePanel attributePanel = new InfoPAttributePanel((Window) me);
//			this.attributeWhere = attributePanel.getWhereClause();
//			String oldText = getComponent().getText();
//			getComponent().setText(attributePanel.getDisplay());
//			String curText = getComponent().getText();
//			//
//    		ValueChangeEvent changeEvent = new ValueChangeEvent(this, this.getColumnName(), oldText, curText);
//    		this.fireValueChange(changeEvent);
//
//		}
//		else {	
//			if (!productWindow && (productId == 0 || exclude))
//			{
//				changed = true;
//				getComponent().setText(null);
//				attributeSetInstanceId = 0;
//			}
//			else
//			{
//				WPAttributeDialog vad = new WPAttributeDialog (
//					attributeSetInstanceId, productId, this.partnerId,
//					productWindow, gridField.getAD_Column_ID(), this.windowNo);
//				if (vad.isChanged())
//				{
//					getComponent().setText(vad.getM_AttributeSetInstanceName());
//					attributeSetInstanceId = vad.getM_AttributeSetInstance_ID();
//					if (this.gridTab != null && !productWindow && vad.getM_Locator_ID() > 0)
//						this.gridTab.setValue("M_Locator_ID", vad.getM_Locator_ID());
//					changed = true;
//				}
//			}
//		}
//
//		//	Set Value
//		if (changed)
//		{
//			log.finest("Changed M_AttributeSetInstance_ID=" + attributeSetInstanceId);
//			this.value = new Object();				//	force re-query display
//			if (attributeSetInstanceId == 0)
//				setValue(null);
//			else
//				setValue(new Integer(attributeSetInstanceId));
//			// Change Locator
//			if (this.gridTab != null && locatorId > 0)
//			{
//				log.finest("Change M_Locator_ID="+locatorId);
//				this.gridTab.setValue("M_Locator_ID", locatorId);
//			}
//			//
//			String columnName = "M_AttributeSetInstance_ID";
//	 	 	if (this.gridField != null)
//	 	 	{
//	 	 		columnName = this.gridField.getColumnName();
//	 	 	}
//			ValueChangeEvent vce = new ValueChangeEvent(this, columnName, new Object(), getValue());
//			fireValueChange(vce);
//			//
//			if (attributeSetInstanceId == oldValueInt && this.gridTab != null && this.gridField != null)
//			{
//				//  force Change - user does not realize that embedded object is already saved.
//				this.gridTab.processFieldChange(super.gridField);
//			}
//		}	//	change
    } // cmd_file

    @Override
    public String[] getEvents() {

        return LISTENER_EVENTS;

    }

    public void onMenu(ContextMenuEvent evt) {

        if (WEditorPopupMenu.ZOOM_EVENT.equals(evt.getContextEvent())) {
            getController().actionZoom();
        } else if (WEditorPopupMenu.CHANGE_LOG_EVENT
                .equals(evt.getContextEvent())) {
            WRecordInfo.start(super.gridField);
        }

    }

    public void actionZoom() {

        AEnv.actionZoom(getController().getLookup(),
                getController().getValue());

    }

    @Override
    public boolean isReadWrite() {

        return !getController().isReadOnly();

    }

    @Override
    public void setReadWrite(boolean readWrite) {

        getController().setReadOnly(!readWrite);

    }

    /**
     * Set the old value of the field. For use in future comparisons. The old
     * value must be explicitly set though this call.
     */
    @Override
    public void set_oldValue() {

        super.set_oldValue();
        getController().set_oldValue();

    }

    /**
     * Get the old value of the field explicitly set in the past
     * 
     * @return
     */
    @Override
    public Object get_oldValue() {

        return getController().get_oldValue();

    }

    /**
     * Has the field changed over time?
     * 
     * @return true if the old value is different than the current.
     */
    @Override
    public boolean hasChanged() {

        return getController().hasChanged();

    }

    public void setController(
            AttributeEditorController attributeEditorController) {

        controller = attributeEditorController;

    }

    public AttributeEditorController getController() {

        return Optional.ofNullable(controller)
                .orElseGet(() -> {
                    controller = new NullAttributeEditorControllerImpl();
                    return controller;
                });

    }

    @Override
    public void setText(String textToSet) {

        getComponent().getTextbox().setText(textToSet);
        getComponent().getTextbox().setTooltip(textToSet);

    }

    @Override
    public void setAndBindValue(Object value) {

        String columnName = getController().getColumnName();
        ValueChangeEvent vce = new ValueChangeEvent(this, columnName,
                getValue(), value);

        fireValueChange(vce);

    }

    @Override
    public void warn(String message) {

        // TODO Auto-generated method stub

    }

    @Override
    public void requestFocus() {

        // TODO Auto-generated method stub

    }

    @Override
    public void enableButton(boolean isEnabled) {

        getComponent().getButton().setEnabled(isEnabled);

    }

    @Override
    public void enableTextField(boolean isEnabled) {

        getComponent().getTextbox().setEnabled(isEnabled);

    }

    @Override
    public void actionSearch() {

        // Determine if the component is associated with the InfoProduct window
        Component me = this.component.getParent();
        while (me != null) {
            if (me instanceof InfoProductPanel)
                break;
            me = me.getParent();
        }
        //
        InfoPAttributePanel attributePanel =
                new InfoPAttributePanel((Window) me);
        getController().setAttributeWhere(attributePanel.getWhereClause());
        getController().setText(attributePanel.getDisplay());
        getComponent().setText(attributePanel.getDisplay());
        getComponent().setTooltip(attributePanel.getDisplay());

        getController().actionText();

    }

    @Override
    public void openAttributeDialog(AttributeDialogInfo info) {

        attributeDialog = new WPAttributeDialog(info);
        
//            if (attributeDialog.isChanged())
//            {
//                getComponent().setText(attributeDialog.getM_AttributeSetInstanceName());
//                attributeSetInstanceId = attributeDialog.getM_AttributeSetInstance_ID();
//                if (this.gridTab != null && !productWindow && attributeDialog.getM_Locator_ID() > 0)
//                    this.gridTab.setValue("M_Locator_ID", attributeDialog.getM_Locator_ID());
//                changed = true;
//            }

    }

    String getDialogTitle() {

        return Msg.translate(Env.getCtx(), "M_AttributeSetInstance_ID");

    }

    @Override
    public boolean isAttributeDialogChanged() {

        return Optional.ofNullable(attributeDialog)
                .map(WPAttributeDialog::isChanged)
                .orElse(false);

    }

    @Override
    public int getAttributeSetInstanceFromDialog() {

        return Optional.ofNullable(attributeDialog)
                .map(WPAttributeDialog::getM_AttributeSetInstance_ID)
                .orElse(0);

    }

    @Override
    public String getAttributeSetInstanceNameFromDialog() {

        return Optional.ofNullable(attributeDialog)
                .map(WPAttributeDialog::getM_AttributeSetInstanceName)
                .orElse(null);

    }

    @Override
    public void setWaitCursor() {

        // TODO Auto-generated method stub

    }

    @Override
    public void setDefaultCursor() {

        // TODO Auto-generated method stub

    }

    @Override
    public void zoomToWindow(MQuery zoomQuery, int windowId, int windowNo) {

        AEnv.actionZoom(getController().getLookup(),
                getController().getValue());

    }

    @Override
    public void setVisibleState(boolean visible) {

        setVisible(visible);

    }


}
