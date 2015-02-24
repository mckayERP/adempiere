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
 * Copyright (C) 2003-2007 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): victor.perez@e-evolution.com			                          *
 *****************************************************************************/
package org.eevolution.form;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.VetoableChangeListener;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.plaf.AdempierePLAF;
import org.adempiere.webui.apps.form.WGenForm;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.event.WTableModelEvent;
import org.adempiere.webui.event.WTableModelListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.compiere.apps.ADialog;
import org.compiere.apps.ADialogDialog;
import org.compiere.apps.StatusBar;
import org.compiere.apps.form.FormFrame;
import org.compiere.apps.form.FormPanel;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.MiniTable;
import org.compiere.model.MColumn;
import org.compiere.model.MDocType;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MQuery;
import org.compiere.model.MSysConfig;
import org.compiere.model.PrintInfo;
import org.compiere.plaf.CompiereColor;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportEngine;
import org.compiere.print.Viewer;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.eevolution.model.MDDOrder;
import org.eevolution.model.MDDOrderLine;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zkex.zul.Borderlayout;
import org.zkoss.zkex.zul.Center;
import org.zkoss.zkex.zul.North;
import org.zkoss.zkex.zul.South;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Space;

/**
 *	Create Movement for Material Receipt from Distribution Order
 *
 *  @author victor.perez@www.e-evolution.com 
 *  @version $Id: VOrderDistributionReceipt,v 1.0 
 */
public class WOrderDistributionReceipt extends OrderDistributionReceipt
	implements IFormController, EventListener, ValueChangeListener,
	Serializable,WTableModelListener  
{
	
	/**
	 *	Initialize Panel
	 */
	public void WOrderDistributionReceipt ()
	{
		
		log.info("");
		
		form = new WGenForm(this);
		Env.setContext(Env.getCtx(), form.getWindowNo(), "IsSOTrx", "Y");
		
		try
		{
			super.dynInit();
			dynInit();
			zkInit();
			
			form.postQueryEvent();
		}
		catch(Exception ex)
		{
			log.log(Level.SEVERE, "init", ex);
		}
	}	//	init
	

	private WGenForm form;

	/**	Window No			*/
	private int         	m_WindowNo = 0;
	/**	FormFrame			*/
	private FormFrame 		m_frame;

	private boolean			m_selectionActive = true;
	private Object 			m_DD_Order_ID = null;
	private Object 			m_MovementDate = null;

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(VOrderDistributionReceipt.class);
	//
	private Panel mainPanel = new Panel();
	private Borderlayout mainLayout = new Borderlayout();
	private Panel parameterPanel = new Panel();
	private Grid parameterLayout = GridFactory.newGridLayout();

	private Borderlayout selPanelLayout = new Borderlayout();
	//
	private Label lOrder = new Label();
	private Label lMovementDate = new Label(Msg.translate(Env.getCtx(),"MovementDate"));
	
	private WSearchEditor fOrder;
	private WDateEditor fMovementDate = new WDateEditor("MovementDate", true, false, true, "MovementDate");
	private FlowLayout northPanelLayout = new FlowLayout();
	private StatusBar statusBar = new StatusBar();
	private Panel selPanel = new Panel();
	private Panel genPanel = new Panel();
	private Panel southPanel = new Panel();
	private BorderLayout genLayout = new BorderLayout();
	private Textbox info = new Textbox();
	private Label dataStatus = new Label();
	private WListbox miniTable;
	private ConfirmPanel commandPanel = new ConfirmPanel(true, false, false, false, false, false, false);
	private Button bCancel = commandPanel.getButton(ConfirmPanel.A_CANCEL);
	private Button bGenerate = commandPanel.createButton(ConfirmPanel.A_PROCESS);
	private Button bRefresh = commandPanel.createButton(ConfirmPanel.A_REFRESH);

	/** User selection */
	private ArrayList<Integer> selection = null;
	
	/**
	 *	Static Initialization of te ZK interface.
	 *  <pre>
	 *  selPanel (tabbed)
	 *      fOrg, fBPartner
	 *      scrollPane & miniTable
	 *  genPanel
	 *      info
	 *  </pre>
	 *  @throws Exception
	 */
	void zkInit() throws Exception
	{
		//
		lOrder.setText(Msg.translate(Env.getCtx(), MDDOrder.COLUMNNAME_DD_Order_ID));
		fOrder = new WSearchEditor (MDDOrder.COLUMNNAME_DD_Order_ID, true, false, true, 
				MLookupFactory.get(Env.getCtx(), m_WindowNo, 
						MColumn.getColumn_ID(MDDOrder.Table_Name, MDDOrder.COLUMNNAME_DD_Order_ID), 
						DisplayType.Search, Language.getLoginLanguage(), 
						MDDOrder.COLUMNNAME_DD_Order_ID, 0, false, 
						"DocStatus='CO' AND IsInTransit='Y'"));
		fOrder.addValueChangeListener(this);
		fOrder.getComponent().setAttribute("zk_component_ID", "Lookup_Criteria_DD_Order_ID");
		fOrder.getComponent().setAttribute("zk_component_prefix", "Lookup_");
		fOrder.getComponent().setAttribute("IsDynamic", "False");
		fOrder.getComponent().setAttribute("fieldName", "fOrder");
		fOrder.getComponent().setWidth("200px");		
		//
		lMovementDate.setText(Msg.translate(Env.getCtx(), "MovementDate"));
		Timestamp today = new Timestamp (System.currentTimeMillis());
		m_MovementDate = today;
		fMovementDate.setValue(m_MovementDate);
		fMovementDate.addValueChangeListener(this);
		fMovementDate.getComponent().setAttribute("zk_component_ID", "Lookup_Criteria_MovementDate");
		fMovementDate.getComponent().setAttribute("zk_component_prefix", "Lookup_");
		fMovementDate.getComponent().setAttribute("IsDynamic", "False");
		fMovementDate.getComponent().setAttribute("fieldName", "fMovementDate");
		fMovementDate.getComponent().setWidth("200px");
		//
		form.getStatusBar().setStatusLine(Msg.getMsg(Env.getCtx(), "OrderDistGenerateReceipt"));
		//
		Row row = form.getParameterPanel().newRows().newRow();
		row.appendChild(lOrder.rightAlign());
		row.appendChild(fOrder.getComponent());
		row.appendChild(lMovementDate.rightAlign());
		row.appendChild(fMovementDate.getComponent());
		//
		//
		row = new Row();
		form.getParameterPanel().getRows().appendChild(row);
		row.appendChild(bRefresh);
		row.appendChild(new Space());
		row.appendChild(new Space());
		row.appendChild(new Space());

	}	//	zkInit

	/**
	 *	Dynamic Init.
	 *	- Create GridController & Panel
	 *	- AD_Column_ID from C_Order
	 */
	public void dynInit()
	{
		miniTable = form.getMiniTable();
		configureMiniTable(miniTable);		
		miniTable.getModel().addTableModelListener(this);
		//	Tabbed Pane Listener
		//tabbedPane.addChangeListener(this);
	}	//	dynInit

	/**
	 * Get SQL for Orders that needs to be shipped
	 * @return sql
	 */
	private String getOrderSQL()
	{
	//  Create SQL
        StringBuffer sql = new StringBuffer(
            "SELECT ol.DD_OrderLine_ID, ol.QtyInTransit , uom.Name , p.Value ,p.Name  , w.Name "
            + "FROM DD_OrderLine ol INNER JOIN DD_Order o ON (o.DD_Order_ID=ol.DD_Order_ID) INNER JOIN M_Product p ON (p.M_Product_ID=ol.M_Product_ID) "
            + " INNER JOIN C_UOM uom  ON (uom.C_UOM_ID=ol.C_UOM_ID)"
            + " INNER JOIN M_Locator  l ON (l.M_Locator_ID = ol.M_Locator_ID)"
            + " INNER JOIN M_Warehouse  w ON (w.M_Warehouse_ID = l.M_Warehouse_ID)"  
            + " WHERE o.DocStatus= 'CO' AND  ol.QtyInTransit > 0  AND  o.DD_Order_ID = ? ");
        
        return sql.toString();
	}

	/**
	 *  Query Info
	 */
	public void executeQuery()
	{
		executeQuery(miniTable);
	}   //  executeQuery

	/**
	 * 	Dispose
	 */
	public void dispose()
	{
		if (m_frame != null)
			m_frame.dispose();
		m_frame = null;
	}	//	dispose

	/**
	 *	Action Listener
	 *  @param e event
	 */
	public void actionPerformed (ActionEvent e)
	{
		log.info("Cmd=" + e.getActionCommand());
		//
		if (e.getActionCommand().equals(ConfirmPanel.A_CANCEL))
		{
			dispose();
			return;
		}

		//
		saveSelection();
		if (selection != null
			&& selection.size() > 0
			&& m_selectionActive	//	on selection tab
			&& m_DD_Order_ID != null && m_MovementDate != null)
			generateMovements ();
		else
			dispose();
	}	//	actionPerformed

	/**
	 *	Vetoable Change Listener - requery
	 *  @param e event
	 */
	public void vetoableChange(PropertyChangeEvent e)
	{
		log.info(e.getPropertyName() + "=" + e.getNewValue());
		if (e.getPropertyName().equals("DD_Order_ID"))
			m_DD_Order_ID = e.getNewValue();
		
		if(e.getPropertyName().equals("MovementDate"))
			m_MovementDate = e.getNewValue();
		
		executeQuery();
	}	//	vetoableChange


	/**
	 *	Save Selection & return selecion Query or ""
	 *  @return where clause like C_Order_ID IN (...)
	 */
	private void saveSelection()
	{
		log.info("");
		//  ID selection may be pending
		//miniTable.editingStopped(new ChangeEvent(this));
		//  Array of Integers
		ArrayList<Integer> results = new ArrayList<Integer>();
		selection = null;

		//	Get selected entries
		int rows = miniTable.getRowCount();
		for (int i = 0; i < rows; i++)
		{
			IDColumn id = (IDColumn)miniTable.getValueAt(i, 0);     //  ID in column 0
		//	log.fine( "Row=" + i + " - " + id);
			if (id != null && id.isSelected())
				results.add(id.getRecord_ID());
		}

		if (results.size() == 0)
			return;
		log.config("Selected #" + results.size());
		selection = results;
		
	}	//	saveSelection

	
	/**************************************************************************
	 *	Generate Shipments
	 */
	private void generateMovements ()
	{

		log.info("DD_Order_ID=" + m_DD_Order_ID);
		log.info("MovementDate" + m_MovementDate);
		String trxName = Trx.createTrxName("IOG");	
		Trx trx = Trx.get(trxName, true);	//trx needs to be committed too

		m_selectionActive = false;  //  prevents from being called twice
		statusBar.setStatusLine(Msg.translate(Env.getCtx(), "M_Movement_ID"));
		statusBar.setStatusDB(String.valueOf(selection.size()));
		
		if (selection.size() <= 0)
			return;
		Properties m_ctx = Env.getCtx();
		
		Timestamp MovementDate = (Timestamp) m_MovementDate;
		MDDOrder order = new MDDOrder(m_ctx , Integer.parseInt(m_DD_Order_ID.toString()), trxName);
		MMovement movement = new MMovement(m_ctx , 0 , trxName);
		movement.setDD_Order_ID(order.getDD_Order_ID());
		movement.setAD_User_ID(order.getAD_User_ID());
		movement.setPOReference(order.getPOReference());
		movement.setReversal_ID(0);
		movement.setM_Shipper_ID(order.getM_Shipper_ID());
		movement.setDescription(order.getDescription());
		movement.setC_BPartner_ID(order.getC_BPartner_ID());
		movement.setC_BPartner_Location_ID(order.getC_BPartner_Location_ID());
		movement.setAD_Org_ID(order.getAD_Org_ID());
		movement.setAD_OrgTrx_ID(order.getAD_OrgTrx_ID());
		movement.setAD_User_ID(order.getAD_User_ID());
		movement.setC_Activity_ID(order.getC_Activity_ID());
		movement.setC_Campaign_ID(order.getC_Campaign_ID());
		movement.setC_Project_ID(order.getC_Project_ID());
		movement.setMovementDate(MovementDate);
		movement.setDeliveryRule(order.getDeliveryRule());
		movement.setDeliveryViaRule(order.getDeliveryViaRule());
		movement.setDocAction(MMovement.ACTION_Prepare);
		movement.setDocStatus(MMovement.DOCSTATUS_Drafted);
		
		//Look the document type for the organization
		int docTypeDO_ID = getDocType(MDocType.DOCBASETYPE_MaterialMovement, order.getAD_Org_ID());		
		if(docTypeDO_ID>0)
			movement.setC_DocType_ID(docTypeDO_ID);
		movement.saveEx();
	
		for (int i = 0 ; i < selection.size() ; i++ )
		{
			int DD_OrderLine_ID = selection.get(i);
			MDDOrderLine oline = new MDDOrderLine(m_ctx, DD_OrderLine_ID, trxName);
			MMovementLine line = new MMovementLine(movement);
			line.setM_Product_ID(oline.getM_Product_ID());
			BigDecimal QtyDeliver = (BigDecimal) miniTable.getValueAt(i, 1);
			if(QtyDeliver == null | QtyDeliver.compareTo(oline.getQtyInTransit()) > 0)
				 throw new AdempiereException("Error in Qty");
			
			line.setOrderLine(oline, QtyDeliver, true);
			line.saveEx();
		}
		
		movement.setDocAction(MMovement.DOCACTION_Close);
		movement.setDocStatus(movement.completeIt());
		movement.saveEx();
		trx.commit();
		generateMovements_complete(movement);
		

		//
	}	//	generateMovements

	/**
	 * Get document type based on organization
	 * @param docBaseType Document Type Base
	 * @param AD_Org_ID  Oeganization ID
	 * @return C_DocType_ID
	 */
	private int getDocType(String docBaseType, int AD_Org_ID)
	{
		MDocType[] docs = MDocType.getOfDocBaseType(Env.getCtx(), docBaseType);
 
		if (docs == null || docs.length == 0) 
		{
			String textMsg = "Not found default document type for docbasetype "+ docBaseType;
			throw new AdempiereException(textMsg);
		} 
		else
		{
			for(MDocType doc:docs)
			{
				if(doc.getAD_Org_ID()==AD_Org_ID)
				{
					return doc.getC_DocType_ID();
				}
			}
			log.info("Doc Type for "+docBaseType+": "+ docs[0].getC_DocType_ID());
			return docs[0].getC_DocType_ID();
		}
	}

	/**
	 *  Complete generating movements.
	 * @param movement
	 */
	private void generateMovements_complete (MMovement movement)
	{
		//  Switch Tabs
		//tabbedPane.setSelectedIndex(1);
		StringBuffer iText = new StringBuffer();
		iText.append("<b>").append("")
			.append("</b><br>")
			.append(Msg.translate(Env.getCtx(), "DocumentNo") +" : " +movement.getDocumentNo())
			//  Shipments are generated depending on the Delivery Rule selection in the Order
			.append("<br>")
			.append("");
		info.setText(iText.toString());


		//confirmPanelGen.getOKButton().setEnabled(false);
		//	OK to print shipments
		//if (ADialog.ask(m_WindowNo, this, "PrintShipments"))
//		{
//			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//			int retValue = ADialogDialog.A_CANCEL;	//	see also ProcessDialog.printShipments/Invoices
//			do
//			{
//
//					 MPrintFormat format = MPrintFormat.get(Env.getCtx(), MPrintFormat.getPrintFormat_ID("Inventory Move Hdr (Example)", MMovement.Table_ID,  0), false);
//					 MQuery query = new MQuery(MMovement.Table_Name);
//					 query.addRestriction(MMovement.COLUMNNAME_M_Movement_ID, MQuery.EQUAL, movement.getM_Movement_ID());
//		                                
//					//	Engine
//		             PrintInfo info = new PrintInfo(MMovement.Table_Name,MMovement.Table_ID, movement.getM_Movement_ID());               
//		             ReportEngine re = new ReportEngine(Env.getCtx(), format, query, info);
//		             re.print();
//                     new Viewer(re);
//
//				
//				ADialogDialog d = new ADialogDialog (m_frame,
//					Env.getHeader(Env.getCtx(), m_WindowNo),
//					Msg.getMsg(Env.getCtx(), "PrintoutOK?"),
//					JOptionPane.QUESTION_MESSAGE);
//				retValue = d.getReturnCode();
//			}
//			while (retValue == ADialogDialog.A_CANCEL);
//			setCursor(Cursor.getDefaultCursor());
//		}	//	OK to print shipments

		//
//		confirmPanelGen.getOKButton().setEnabled(true);
	}   //  generateMovement_complete

	@Override
	public void tableChanged(WTableModelEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void valueChange(ValueChangeEvent evt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEvent(Event event) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ADForm getForm() {
		// TODO Auto-generated method stub
		return null;
	}

}	//	VOrderDistributionReceipt
