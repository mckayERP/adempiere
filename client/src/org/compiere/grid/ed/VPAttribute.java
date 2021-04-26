/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2021 ADempiere Foundation, All Rights Reserved.         *
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
package org.compiere.grid.ed;

import static org.compiere.model.GridField.PROPERTY;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.Optional;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.adempiere.controller.ed.AttributeEditorControllerImpl;
import org.adempiere.controller.ed.AttributeEditorController;
import org.adempiere.controller.ed.NullAttributeEditorControllerImpl;
import org.adempiere.controller.form.AttributeDialogInfo;
import org.adempiere.controller.ed.AttributeEditor;
import org.adempiere.exceptions.ValueChangeListener;
import org.adempiere.plaf.AdempierePLAF;
import org.compiere.apps.ADialog;
import org.compiere.apps.AEnv;
import org.compiere.apps.AWindow;
import org.compiere.apps.RecordInfo;
import org.compiere.apps.search.InfoPAttribute;
import org.compiere.apps.search.InfoProduct;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.Lookup;
import org.compiere.model.MLookup;
import org.compiere.model.MPAttributeLookup;
import org.compiere.model.MQuery;
import org.compiere.swing.CButton;
import org.compiere.swing.CDialog;
import org.compiere.swing.ILabel;
import org.compiere.swing.CMenuItem;
import org.compiere.swing.CTextField;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Msg;
import org.compiere.util.ValueNamePair;

/**
 * Product Attribute Set Instance Editor
 *
 * @author Jorg Janke
 * @version $Id: VPAttribute.java,v 1.2 2006/07/30 00:51:27 jjanke Exp $
 * 
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 *         <li>BF [ 1895041 ] NPE when move product with attribute set
 *         <li>BF [ 1770177 ] Inventory Move Locator Error - integrated
 *         MGrigioni bug fix
 *         <li>BF [ 2011222 ] ASI Dialog is reseting locator
 * 
 * @author Michael McKay, mckayERP www.mckayERP.com
 *         <li>ADEMPIERE-72 VLookup and Info Window improvements
 *         https://adempiere.atlassian.net/browse/ADEMPIERE-72
 *         <li>#278 Add Lookup to the popup menu
 *         <li>#280 ASI field should accept text input
 * 
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *         <li>FR [ 146 ] Remove unnecessary class, add support for info to
 *         specific column
 * @see https://github.com/adempiere/adempiere/issues/146
 *
 */
public class VPAttribute extends JComponent
        implements VEditor, AttributeEditor, ActionListener, FocusListener {

    private static final long serialVersionUID = -1823370077523962901L;

    /** Logger */
    private static CLogger log = CLogger.getCLogger(VPAttribute.class);

    final class VPAttributeMouseAdapter extends MouseAdapter {

        VPAttributeMouseAdapter(VPAttribute attribute) {

            this.attribute = attribute;

        }

        private VPAttribute attribute;

        @Override
        public void mouseClicked(MouseEvent e) {

            if (isDoubleClick(e))
                sendMouseActionToRecipient(e);
            if (isOpenPopupMenu(e))
                openThePopup(e);

        }

        public void sendMouseActionToRecipient(MouseEvent e) {

            this.attribute.actionPerformed(
                    new ActionEvent(e.getSource(), e.getID(), "Mouse"));

        }

        public void openThePopup(MouseEvent e) {

            this.attribute.popupMenu.show((Component) e.getSource(), e.getX(),
                    e.getY());

        }

        public boolean isOpenPopupMenu(MouseEvent e) {

            return SwingUtilities.isRightMouseButton(e);

        }

        public boolean isDoubleClick(MouseEvent e) {

            return e.getClickCount() > 1;

        }

    }

    /**
     * IDE Constructor
     */
    public VPAttribute() {

        this(null, false, false, true, 0, (MLookup) null, false);

    }

    /**
     * Create Product Attribute Set Instance Editor.
     * 
     * @param mandatory  mandatory
     * @param isReadOnly read only
     * @param isUpdated  isUpdated
     * @param windowNo   WindowNo
     * @param pAttribute Model Product Attribute
     */
    @Deprecated
    public VPAttribute(boolean mandatory, boolean isReadOnly, boolean isUpdated,
            int windowNo, MPAttributeLookup pAttribute, boolean searchOnly) {

        this(null, mandatory, isReadOnly, isUpdated, windowNo, (MLookup) null,
                searchOnly);

    }

    /**
     * Create Product Attribute Set Instance Editor.
     * 
     * @param mandatory    mandatory
     * @param isReadOnly   read only
     * @param isUpdateable updateable
     * @param windowNo     WindowNo
     * @param lookup       the colum lookup model (MLookup)
     */
    public VPAttribute(boolean mandatory, boolean isReadOnly,
            boolean isUpdateable,
            int windowNo, MLookup lookup, boolean searchOnly) {

        this(null, mandatory, isReadOnly, isUpdateable, windowNo, lookup,
                searchOnly);

    }

    /**
     * Create Product Attribute Set Instance Editor.
     * 
     * @param gridTab
     * @param mandatory    mandatory
     * @param isReadOnly   read only
     * @param isUpdateable updateable
     * @param WindowNo     WindowNo
     * @param pAttribute   Model Product Attribute
     * @param searchOnly   True if only used to search instances
     */
    @Deprecated
    public VPAttribute(GridTab gridTab,
            boolean mandatory,
            boolean isReadOnly,
            boolean isUpdateable,
            int windowNo,
            MPAttributeLookup lookup,
            boolean searchOnly) {

        this(gridTab, mandatory, isReadOnly, isUpdateable, windowNo,
                (MLookup) null, searchOnly);

    }

    /**
     * Create Product Attribute Set Instance Editor.
     * 
     * @param gridTab
     * @param mandatory    Set true if the field is mandatory
     * @param isReadOnly   Set true if the field is read only
     * @param isUpdateable Set true if the field can be updated
     * @param WindowNo     The parent window number
     * @param lookup       The MLookup to use
     * @param searchOnly   Set true if the field is to be used to search only
     *                     and should not hold a value.
     */
    public VPAttribute(GridTab gridTab, boolean mandatory, boolean isReadOnly,
            boolean isUpdateable, int windowNo, MLookup lookup,
            boolean searchOnly) {

        super();
        super.setName(DEFAULT_COLUMN_NAME);
        textField.setName("VPAttribute Text - " + DEFAULT_COLUMN_NAME);
        button.setName("VPAttribute Button - " + DEFAULT_COLUMN_NAME);
        this.windowNo = windowNo;

        controller = new AttributeEditorControllerImpl(this, gridTab, mandatory,
                isReadOnly, isUpdateable, windowNo, (Lookup) lookup);
        controller.setSearchOnly(searchOnly);

        //  The creating function should set the field and name. See
        //  VEditorFactory.  To initialize the field in cases of forms, 
        //  set the field to null.
        setField(null);

        LookAndFeel.installBorder(this, "TextField.border");
        this.setLayout(new BorderLayout());
        // Size
        this.setPreferredSize(this.textField.getPreferredSize());
        int height = this.textField.getPreferredSize().height;

        // *** Text ***
        this.textField.setEditable(true);
        this.textField.setFocusable(true);
        this.textField.setBorder(null);
        this.textField.setHorizontalAlignment(SwingConstants.LEADING);
        this.textField.addActionListener(this);
        this.textField.addFocusListener(this);
        this.add(this.textField, BorderLayout.CENTER);

        // *** Button ***
        this.getButton().setIcon(Env.getImageIcon("PAttribute10.gif"));
        this.getButton().setMargin(new Insets(0, 0, 0, 0));
        this.getButton().setPreferredSize(new Dimension(height, height));
        this.getButton().addActionListener(this);
        this.getButton().setFocusable(true);
        this.add(this.getButton(), BorderLayout.EAST);

        // Prefereed Size
        this.setPreferredSize(this.getPreferredSize()); // causes r/o to be the
                                                        // same length
        // Popup
        this.textField.addMouseListener(new VPAttributeMouseAdapter(this));
        menuInfo = new CMenuItem(Msg.getMsg(Env.getCtx(), "Info"),
                Env.getImageIcon("Zoom16.gif"));
        menuZoom = new CMenuItem(Msg.getMsg(Env.getCtx(), "Zoom"),
                Env.getImageIcon("Zoom16.gif"));
        menuInfo.addActionListener(this);
        menuZoom.addActionListener(this);
        popupMenu.add(menuZoom);
        popupMenu.add(menuInfo);
        
        controller.init();

    } 

    CTextField textField = new CTextField();
    CButton button = new CButton();

    JPopupMenu popupMenu = new JPopupMenu();
    private CMenuItem menuZoom;
    private CMenuItem menuInfo;

    private VPAttributeDialog attributeDialog;

    private AttributeEditorController controller;

    private int columnId = 0;
    private boolean haveFocus;
    private int windowNo;
    
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

    /**
     * Action Listener Interface
     * 
     * @param listener listener
     */
    public void addActionListener(ActionListener listener) {
    
        this.textField.addActionListener(listener);
    
    }

    /**
     * Action Listener - start dialog
     * 
     * @param e Event
     */
    public void actionPerformed(ActionEvent e) {
    
        if (e.getActionCommand().equals(RecordInfo.CHANGE_LOG_COMMAND)) {
            RecordInfo.start(getController().getGridField());
            return;
        }
    
        if (e.getSource() instanceof CTextField)
            getController().actionText();
        else if (e.getSource() instanceof CButton || e.getSource() == menuInfo)
            getController().actionButton();
    
        else if (e.getSource() == menuZoom)
            getController().actionZoom();
    
        requestFocus();
    
    }

    @Override
    public void addValueChangeListener(ValueChangeListener listener) {
    
        // Not used in Swing
    
    }

    /**
     * Property Change Listener
     * 
     * @param evt event
     */
    public void propertyChange(PropertyChangeEvent evt) {
    
        if (evt.getPropertyName().equals(PROPERTY))
            getController().setValue(evt.getNewValue());
    
    }

    /**
     * Take action on focus gained
     * @param e FocusEvent
     */
    public void focusGained(FocusEvent e) {
    
        if (alreadyHaveTheFocus(e))
            return;
    
        log.fine("Have Focus!");
        haveFocus = true; // prevents calling focus gained twice
        textField.selectAll();
    
    }

    private boolean alreadyHaveTheFocus(FocusEvent e) {

        return (e.getSource() != textField)
                || e.isTemporary() || haveFocus;

    } 

    /**
     * Take action on focus lost event
     * @param e FocusEvent
     */
    public void focusLost(FocusEvent e) {
    
        if (e.isTemporary() || !getButton().isEnabled()) {
            haveFocus = false; 
            return;
        }
    
        if (e.getSource() == textField) {
            getController().actionText();
        }
        
        log.fine("Losing Focus!");
        haveFocus = false; 
    
    } 
    
    /**
     * Request Focus
     */
    public void requestFocus() {
    
        textField.requestFocus();
    
    }

    @Override
    public GridField getField() {
    
        return getController().getGridField();
    
    }

    @Override
    public void setField(GridField field) {
    
        getController().setGridField(field);
        if (field != null)
            RecordInfo.addMenu(this, popupMenu);
    
    }

    /**
     * Get Attribute Where clause
     * 
     * @return String
     */
    public String getAttributeWhere() {
    
        return getController().getAttributeWhere();
    
    }

    /**
     * Get Value
     * 
     * @return value
     */
    public Object getValue() {
    
        return getController().getValue();
    
    } // getValue

    /**
     * Set/lookup Value
     * 
     * @param value value
     */
    public void setValue(Object value) {
    
        log.fine(this.DEFAULT_COLUMN_NAME + "=" + value);
        getController().setValue(value);
    
    }

    @Override
    public void setAndBindValue(Object value) {
    
        Integer currentValue = 0;
        Integer newValue = (Integer) value;
    
        try {
            currentValue = (Integer) getValue();
        } catch (ClassCastException cce) {
            // Possible Invalid Cast exception if getValue() returns new
            // instance of Object.
            currentValue = 0;
        }
    
        try {
            fireVetoableChange(DEFAULT_COLUMN_NAME, currentValue, newValue);
        } catch (PropertyVetoException pve) {
            log.log(Level.SEVERE, "", pve);
        }
    
    }

    /**
     * Get the old value of the field explicitly set in the past
     * 
     * @return
     */
    public Object get_oldValue() {
    
        return getController().get_oldValue();
    
    }

    /**
     * Set the old value of the field. For use in future comparisons. The old
     * value must be explicitly set though this call.
     */
    public void set_oldValue() {
    
        getController().set_oldValue();
    
    }

    /**
     * Has the field changed over time?
     * 
     * @return true if the old value is different than the current.
     */
    public boolean hasChanged() {
        
        return getController().hasChanged();
    
    }

    public CButton getButton() {
    
        return button;
    
    }

    @Override
    public void enableButton(boolean isEnabled) {
    
        this.button.setEnabled(isEnabled);
    
    }

    @Override
    public void enableTextField(boolean isEnabled) {
    
        this.textField.setEnabled(isEnabled);
    
    }

    public CTextField getTextField() {
    
        return textField;
    
    }

    @Override
    public void setText(String textToSet) {
    
        textField.setText(textToSet);
        textField.setToolTipText(textToSet);
    
    }

    /**
     * Get Display Value
     * 
     * @return info
     */
    public String getDisplay() {
    
        return this.textField.getText();
    
    }

    public void actionSearch() {
    
        // We are in the infoProduct panel
        // The component is an element in a CPanel, which is part of a JPanel
        // which is in a JLayeredPane which is in ... the InfoProduct window
        Container me = getParent();
        while (me != null) {
            if (me instanceof InfoProduct)
                break;
            me = me.getParent();
        }
        // The infoPAttribute doesn't select an attribute set instance, it
        // builds the where clause so setting the value is not required here.
        InfoPAttribute ia = new InfoPAttribute((CDialog) me);
        getController().setAttributeWhere(ia.getWhereClause());
        getController().setText(ia.getDisplay());
        this.textField.setText(ia.getDisplay());
        this.textField.setToolTipText(this.textField.getText());
        getController().setLastDisplay(this.textField.getText());
    
        ActionEvent ae = new ActionEvent(this.textField, 1001, "updated");
        // not the generally correct way to fire an event
        Optional.ofNullable((InfoProduct) me)
                .ifPresent(ip -> ip.actionPerformed(ae));
    
    }

    public void openAttributeDialog(AttributeDialogInfo info) {
    
        attributeDialog = new VPAttributeDialog(Env.getFrame(this), info);
    
    }

    String getDialogTitle() {

        return Msg.translate(Env.getCtx(), "M_AttributeSetInstance_ID");

    }

    @Override
    public int getAttributeSetInstanceFromDialog() {
    
        return Optional.ofNullable(attributeDialog)
                .map(VPAttributeDialog::getM_AttributeSetInstance_ID)
                .orElse(0);
    
    }

    @Override
    public String getAttributeSetInstanceNameFromDialog() {
    
        return Optional.ofNullable(attributeDialog)
                .map(VPAttributeDialog::getM_AttributeSetInstanceName)
                .orElse(null);
    
    }

    @Override
    public boolean isAttributeDialogChanged() {
    
        return Optional.ofNullable(attributeDialog)
                .map(VPAttributeDialog::isChanged)
                .orElse(false);
    
    }

    public void zoomToWindow(MQuery zoomQuery, int windowId, int windowNo) {
    
        setWaitCursor();
    
        AWindow frame = new AWindow(getGraphicsConfiguration());
        if (!frame.initWindow(windowId, zoomQuery)) {
            ValueNamePair pp = CLogger.retrieveError();
            String msg = pp == null ? "AccessTableNoView" : pp.getValue();
            ADialog.error(windowNo, this, msg, pp == null ? "" : pp.getName());
        } else {
            AEnv.addToWindowManager(frame);
            if (Ini.isPropertyBool(Ini.P_OPEN_WINDOW_MAXIMIZED)) {
                AEnv.showMaximized(frame);
            } else {
                AEnv.showCenterScreen(frame);
            }
        }
    
        setDefaultCursor();
    
    }

    @Override
    public void warn(String message) {
    
        ADialog.warn(windowNo, this, message);
    
    }

    @Override
    public void setDefaultCursor() {
    
        setCursor(Cursor.getDefaultCursor());
    
    }

    @Override
    public void setWaitCursor() {
    
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    
    }

    public boolean isMandatory() {
    
        return getController().isMandatory();
    
    }

    public void setMandatory(boolean mandatory) {

        getController().setMandatory(mandatory);

    }

    public boolean isReadWrite() {
    
        return !getController().isReadOnly();
    
    }

    public void setReadWrite(boolean rw) {

        getController().setReadOnly(!rw);

    }


    public void setForeground(Color color) {

        this.textField.setForeground(color);

    }

    /**
     * Set Background based on error
     * 
     * @param error Error
     */
    public void setBackground(boolean error) {

        if (error)
            setBackground(AdempierePLAF.getFieldBackground_Error());
        else if (!isReadWrite())
            setBackground(AdempierePLAF.getFieldBackground_Inactive());
        else if (isMandatory())
            setBackground(AdempierePLAF.getFieldBackground_Mandatory());
        else
            setBackground(AdempierePLAF.getInfoBackground());

    }

    @Override
    public void setBackground(Color color) {

        this.textField.setBackground(color);

    }

    public void dispose() {
    
        this.textField = null;
        this.button = null;
        getController().dispose();
    
    }

    @Override
    public void setVisibleState(boolean visible) {

        setVisible(visible);

    }

    @Override
    public void setLable(ILabel label) {

        // TODO Auto-generated method stub
        

    }

}
