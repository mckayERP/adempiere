/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
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
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.grid.ed;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.adempiere.controller.AttributeDialogController;
import org.adempiere.controller.AttributeDialogControllerImpl;
import org.adempiere.controller.form.AttributeDialog;
import org.adempiere.controller.form.AttributeDialogInfo;
import org.adempiere.controller.form.AttributeInstanceSearchDialog;
import org.compiere.apps.ADialog;
import org.compiere.apps.AEnv;
import org.compiere.apps.ALayout;
import org.compiere.apps.ALayoutConstraint;
import org.compiere.apps.AWindow;
import org.compiere.apps.ConfirmPanel;
import org.compiere.apps.search.PAttributeInstance;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MQuery;
import org.compiere.model.MWindow;
import org.compiere.swing.CButton;
import org.compiere.swing.ICheckBox;
import org.compiere.swing.CCheckBox;
import org.compiere.swing.IComboBox;
import org.compiere.swing.CComboBox;
import org.compiere.swing.IDate;
import org.compiere.swing.CDialog;
import org.compiere.swing.CEditor;
import org.compiere.swing.ILabel;
import org.compiere.swing.CLabel;
import org.compiere.swing.CMenuItem;
import org.compiere.swing.CPanel;
import org.compiere.swing.CScrollPane;
import org.compiere.swing.ITextField;
import org.compiere.swing.CTextField;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;

/**
 * Product Attribute Set Product/Instance Dialog Editor. Called from
 * VPAttribute.actionPerformed
 *
 * @author Jorg Janke
 * @version $Id: VPAttributeDialog.java,v 1.4 2006/07/30 00:51:27 jjanke Exp $
 * 
 * @author Michael McKay (mjmckay)
 *         <li>BF3468823 - Attribute Set Instance editor does not display
 *         <li>ADEMPIERE-72 VLookup and Info Window improvements
 *         https://adempiere.atlassian.net/browse/ADEMPIERE-72
 *         <li>#281 Improve tests of validity of ASI values
 *         <li>#258 Reduce duplication of ASI values
 */
public class VPAttributeDialog extends CDialog
        implements ActionListener, AttributeDialog {
    
    private static final long serialVersionUID = -1062346984681892620L;

    private CLogger log = CLogger.getCLogger(getClass());

    /**************************************************************************
     * Mouse Listener for Popup Menu
     */
    final class VPAttributeDialogMouseAdapter
            extends java.awt.event.MouseAdapter {

        VPAttributeDialogMouseAdapter(VPAttributeDialog dialogAdaptee) {

            adaptee = dialogAdaptee;

        } // VPAttributeDialog_mouseAdapter

        private VPAttributeDialog adaptee;

        @Override
        public void mouseClicked(MouseEvent e) {

            if (isOpenPopupMenu(e))
                openPopupMenu(e);

        }

        public void openPopupMenu(MouseEvent e) {

            adaptee.popupMenu.show((Component) e.getSource(), e.getX(),
                    e.getY());

        }

        public boolean isOpenPopupMenu(MouseEvent e) {

            return SwingUtilities.isRightMouseButton(e);

        }

    }

    /**
     * Product Attribute Instance Dialog
     * 
     * @param frame         parent frame
     * @param asiId         Product Attribute Set Instance id
     * @param productId     Product id
     * @param bpartnerId    b partner
     * @param productWindow this is the product window (define Product Instance)
     * @param columnId      column
     * @param myWindowNo      window
     */
    public VPAttributeDialog(Frame frame, AttributeDialogInfo info) {

        super(frame, info.getTitle(), true);
        log.config(info.toString());
        myWindowNo = Env.createWindowNo(this);
        info.setDialogWindowNo(myWindowNo);
        controller = new AttributeDialogControllerImpl(this, info);
        
        try {
            jbInit();
        } catch (Exception ex) {
            log.log(Level.SEVERE, "VPAttributeDialog" + ex);
        }
        
        if (!controller.init()) {
            dispose();
            return;
        }
        AEnv.showCenterWindow(frame, this);

    }

    private AttributeDialogController controller;
    private int myWindowNo;
    private int rowCounter = 0;
    private VString fieldLotString;
    private CComboBox fieldLot = null;
    private CButton bLot;
    JPopupMenu popupMenu = new JPopupMenu();
    private CMenuItem mZoom;
    private VString fieldSerNo;
    private CButton bSerNo;
    private VDate fieldGuaranteeDate;
    private CTextField fieldDescription;
    
    private BorderLayout mainLayout = new BorderLayout();
    private CPanel centerPanel = new CPanel();
    private transient ALayout centerLayout = new ALayout(5, 5, true);
    private ConfirmPanel confirmPanel = new ConfirmPanel(true);
    private CScrollPane centerScroll = new CScrollPane();
    private PAttributeInstance asiSearchAndSelectDialog = null;


    private void jbInit() {

        getContentPane().setLayout(mainLayout);
        centerScroll.getViewport().add(centerPanel);
        add(centerScroll, BorderLayout.CENTER);
        getContentPane().add(confirmPanel, BorderLayout.SOUTH);
        centerPanel.setLayout(centerLayout);
        confirmPanel.addActionListener(this);

    }

    @Override
    public void dispose() {

        removeAll();
        super.dispose();

    }


    public void actionPerformed(ActionEvent e) {

        if (e.getActionCommand().equals(ConfirmPanel.A_OK))
            controller.actionOK();
        else if (e.getActionCommand().equals(ConfirmPanel.A_CANCEL)) {
            controller.actionCancel();
        }
        else if (e.getSource() == mZoom)
            cmd_zoom();
        else
            controller.action(e.getSource());


    }


    public void openAttributeSetInstanceSearchAndSelectDialog(String title,
            int productId, int warehouseId, int locatorId, int bpartnerId) {

        asiSearchAndSelectDialog = new PAttributeInstance(this, title,
                warehouseId, locatorId, productId, bpartnerId);

    }


    /**
     * Zoom M_Lot
     */
    @SuppressWarnings("deprecation")
    private void cmd_zoom() {

        int lotId = 0;
        KeyNamePair pp = (KeyNamePair) fieldLot.getSelectedItem();
        if (pp != null)
            lotId = pp.getKey();
        MQuery zoomQuery = new MQuery("M_Lot");
        zoomQuery.addRestriction("M_Lot_ID", MQuery.EQUAL, lotId);
        log.info(zoomQuery.toString());
        
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        int windowId = MWindow.getWindow_ID("Lot"); // Lot
        AWindow frame = new AWindow();
        if (frame.initWindow(windowId, zoomQuery)) {
            this.setVisible(false);
            this.setModal(false); // otherwise blocked
            this.setVisible(true);
            AEnv.addToWindowManager(frame);
            AEnv.showScreen(frame, SwingConstants.EAST);
        }
        setCursor(Cursor.getDefaultCursor());

    }


    public boolean isChanged() {

        return controller.isChanged();

    }

    public Object getController() {

        return controller;

    }

    @Override
    public AttributeInstanceSearchDialog getAttributeInstanceSearchDialog() {

        return asiSearchAndSelectDialog;

    }

    @Override
    public void clearPanel() {

        centerPanel.removeAll();
        rowCounter = 0;

    }

    @Override
    public ICheckBox createNewEditCheckBox() {

        CCheckBox cbNewEdit = new CCheckBox();
        cbNewEdit.addActionListener(this);
        centerPanel.add(cbNewEdit, new ALayoutConstraint(rowCounter++, 0));
        return cbNewEdit;

    }

    @Override
    public CButton createButtonSelect(String imagePath) {

        ImageIcon imageIcon = Env.getImageIcon(imagePath);
        CButton bSelect = new CButton(imageIcon);
        bSelect.addActionListener(this);
        centerPanel.add(bSelect, null);

        return bSelect;

    }

    @Override
    public CButton createButtonSerialNumber(String name) {

        bSerNo = new CButton(name);
        return bSerNo;


    }

    @Override
    public CButton createButtonLot(String name) {

        bLot = new CButton(name);
        return bLot;

    }

    @Override
    public CLabel createAttributeGroupLable(String label) {

        CLabel group = new CLabel(label);
        group.setFontBold(true);
        group.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(group, new ALayoutConstraint(rowCounter++, 0));
        return group;

    }

    @Override
    public void enableCancel(boolean enable) {

        confirmPanel.getCancelButton().setEnabled(enable);

    }

   @Override
    public ITextField getFieldLotString() {

        return fieldLotString;

    }

    @Override
    public IComboBox getFieldLot() {

        return fieldLot;

    }

    @Override
    public CEditor getFieldGuaranteeDate() {

        return fieldGuaranteeDate;

    }

    @Override
    public ILabel createLabel(String name) {

        CLabel label = new CLabel(name);
        centerPanel.add(label, new ALayoutConstraint(rowCounter++, 0));
        return label;

    }

    @Override
    public ITextField getFieldDescription() {

        return fieldDescription;

    }

    @Override
    public ITextField createFieldDescription() {

        fieldDescription = new CTextField(20);
        centerPanel.add(fieldDescription, null);
        return fieldDescription;

    }

    @Override
    public void resize() {

        // Window usually to wide (??)
        Dimension dd = centerPanel.getPreferredSize();
        dd.width = Math.min(500, dd.width);
        centerPanel.setPreferredSize(dd);
        centerPanel.revalidate();

    }

    @Override
    public IComboBox createCComboBox(MAttributeValue[] values) {

        CComboBox cb = new CComboBox(values);
        centerPanel.add(cb, null);
        return cb;

    }

    @Override
    public CEditor createNumberEditor(String name, boolean mandatory,
            boolean readOnly, boolean updateable, int number, String title) {

        VNumber ne = new VNumber(name, mandatory, readOnly, updateable, number, title);
       ne. addActionListener(this);
        centerPanel.add(ne, null);
        return ne;

    }

    @Override
    public ITextField createStringEditor(String name, boolean mandatory,
            boolean readOnly, boolean updateable, int displayLength,
            int fieldLength, String vFormat, String obscureType) {

        VString se = new VString(name, mandatory, readOnly, updateable, 
                displayLength, fieldLength, vFormat, obscureType);
        se.addActionListener(this);
        centerPanel.add(se, null);
        return se;

    }

    @Override
    public ILabel createGroupLabel(String name) {

        CLabel groupLabel = new CLabel(name);
        groupLabel.setFontBold(true);
        groupLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(groupLabel, new ALayoutConstraint(rowCounter++, 0));
        return groupLabel;

    }

    @Override
    public ITextField createFieldLotString() {

        fieldLotString  =
                new VString("Lot", false, false, true, 20, 20, null, null);
        centerPanel.add(fieldLotString, null);
        fieldLotString.removeActionListener(this);
        fieldLotString.addActionListener(this);
        return fieldLotString;

    }

    @Override
    public IComboBox createFieldLot(KeyNamePair[] lotKeyNamePairs) {

        fieldLot = new CComboBox(lotKeyNamePairs);
        fieldLot.addActionListener(this);
        centerPanel.add(fieldLot, null);

        fieldLot.addMouseListener(
                new VPAttributeDialogMouseAdapter(this)); // popup
        mZoom = new CMenuItem(Msg.getMsg(Env.getCtx(), "Zoom"),
                Env.getImageIcon("Zoom16.gif"));
        mZoom.addActionListener(this);
        popupMenu.add(mZoom);

        return fieldLot;

    }

    @Override
    public void createNewLotButton() {

        centerPanel.add(bLot, null);
        bLot.removeActionListener(this);
        bLot.addActionListener(this);

    }

    @Override
    public ITextField createFieldSerNo(String serNo) {

        fieldSerNo =  new VString("SerNo", false, false, true, 20, 20, null, null);
        centerPanel.add(fieldSerNo, null);
        fieldSerNo.addActionListener(this);
        return fieldSerNo;

    }

    @Override
    public void createNewSerNoButton() {

        centerPanel.add(bSerNo, null);
        bSerNo.removeActionListener(this);
        bSerNo.addActionListener(this);
        
    }

    @Override
    public IDate createFieldGuaranteeDate(String name) {

        fieldGuaranteeDate = new VDate("GuaranteeDate", false, false, true, DisplayType.Date,
                        name);
        centerPanel.add(fieldGuaranteeDate, null);
        fieldGuaranteeDate.removeActionListener(this);
        fieldGuaranteeDate.addActionListener(this);
        return fieldGuaranteeDate;

    }

    @Override
    public ITextField getFieldSerNo() {

        return fieldSerNo;

    }

    @Override
    public void showError(String string, String mandatoryMsg) {

        ADialog.error(myWindowNo, this, "FillMandatory", mandatoryMsg);


    }

    @Override
    public int getM_AttributeSetInstance_ID() {
        
        return controller.getAttributeSetInstanceId();
        
    }

    @Override
    public String getM_AttributeSetInstanceName() {
        
        return controller.getM_AttributeSetInstanceName();
        
    }

    @Override
    public void reset() {

        centerPanel.validate();
        
    }

}
