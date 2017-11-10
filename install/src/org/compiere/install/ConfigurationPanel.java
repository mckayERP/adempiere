/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
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
package org.compiere.install;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.VetoableChangeListener;
import java.util.ResourceBundle;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.adempiere.configuration.ConfigurationData;
import org.apache.tools.ant.Main;
import org.compiere.Adempiere;
import org.compiere.grid.SimpleLookup;
import org.compiere.grid.ed.VCheckBox;
import org.compiere.grid.ed.VFile;
import org.compiere.grid.ed.VLookup;
import org.compiere.grid.ed.VPassword;
import org.compiere.grid.ed.VString;
import org.compiere.swing.CButton;
import org.compiere.swing.CComboBox;
import org.compiere.swing.CLabel;
import org.compiere.swing.CPanel;
import org.compiere.util.CLogger;


/**
 *	Configuration Panel
 *
 * 	@author 	Jorg Janke
 * 	@version 	$Id: ConfigurationPanel.java,v 1.3 2006/07/30 00:57:42 jjanke Exp $
 *  @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<li> FR [ 402 ] Mail setup is hardcoded
 *		@see https://github.com/adempiere/adempiere/issues/402
 *		<li> FR [ 391 ] Add connection support to MariaDB
 *		@see https://github.com/adempiere/adempiere/issues/464
 *  @author mckayERP, michael.mckay@mckayerp.com
 *  	<li> Modified to follow a MVC model.
 */
public class ConfigurationPanel extends CPanel implements ActionListener, PropertyChangeListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5113669370606054608L;

	/**	Static Logger	*/
	static CLogger	log	= CLogger.getCLogger (ConfigurationPanel.class);


	/**
	 * 	Constructor
	 *  @param statusBar for info
	 */
	public ConfigurationPanel (JLabel statusBar)
	{
		m_statusBar = statusBar;
		try
		{
			jbInit();
			m_cc.loadMap(); // Have to do this AFTER the fields are created.
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}	//	ConfigurationPanel

	/** Error Info				*/
	private String				m_errorString;
	/** Test Success			*/
	private volatile boolean	m_success = false;
	/** Sync					*/
	private volatile boolean	m_testing = false;

	/** Translation				*/
	static ResourceBundle 		res = ResourceBundle.getBundle("org.compiere.install.SetupRes");
	/** Status Bar				*/
	private JLabel 				m_statusBar;
	
	/** Configuration Controller	*/
	private ConfigurationController m_cc = new ConfigurationController(this);

	private static ImageIcon iOpen = new ImageIcon(ConfigurationPanel.class.getResource("openFile.gif"));
	private static ImageIcon iSave = new ImageIcon(Adempiere.class.getResource("images/Save16.gif"));
	private static ImageIcon iHelp = new ImageIcon(Adempiere.class.getResource("images/Help16.gif"));


	//	-------------	Static UI
	private GridBagLayout gridBagLayout = new GridBagLayout();
	private static final int	FIELDLENGTH = 60;
	private static final int	DISPLAYLENGTH = 30;
	//	Java
	private CLabel 		lJavaHome = new CLabel();
	VFile 	fJavaHome = null;
	VCheckBox 	okJavaHome = new VCheckBox();
	
	private CLabel 		lJavaType = new CLabel();
	VLookup fJavaType = null;

	private CLabel 		lJavaOptions = new CLabel();
	VString fJavaOpts = null;
	
	//	Adempiere - KeyStore
	private CLabel 		lAdempiereHome = new CLabel();
	VFile 	fAdempiereHome = null;
	VCheckBox 	okAdempiereHome = new VCheckBox();
//	private CButton 	bAdempiereHome = new CButton(iOpen);
	
	private CLabel 		lKeyStore = new CLabel();
	VPassword 	fKeyStore = new VPassword();
	VCheckBox 	okKeyStore = new VCheckBox();

	//	Apps Server  - Type 
	private CLabel lAppsServer = new CLabel();
	VString fAppsServer = null;
	VCheckBox okAppsServer = new VCheckBox();

	private CLabel lAppsType = new CLabel();
	VLookup 	fAppsType = null;
	
	//	Deployment Directory - JNP
	private CLabel 		lDeployDir = new CLabel();
	VFile 	fDeployDir = null;
	VCheckBox 	okDeployDir = new VCheckBox();
//	CButton 	bDeployDir = new CButton(iOpen);
	
	private CLabel lJNPPort = new CLabel();
	VString fJNPPort = null;
	VCheckBox okJNPPort = new VCheckBox();
	
	//	Web Ports
	private CLabel lWebPort = new CLabel();
	VString fWebPort = null;
	VCheckBox okWebPort = new VCheckBox();
	private CLabel lSSLPort = new CLabel();
	VString fSSLPort = null;
	VCheckBox okSSLPort = new VCheckBox();
	//	Database
	private CLabel lDatabaseType = new CLabel();
	VLookup fDatabaseType = null;
	//
	CLabel lDatabaseServer = new CLabel();
	VString fDatabaseServer = null;
	private CLabel lDatabaseName = new CLabel();
	VString fDatabaseName = null;
	private CLabel lDatabaseDiscovered = new CLabel();
	VLookup fDatabaseDiscovered = null;
	private CLabel lDatabasePort = new CLabel();
	VString fDatabasePort = null;
	private CLabel lSystemPassword = new CLabel();
	VPassword fSystemPassword = new VPassword();
	private CLabel lDatabaseUser = new CLabel();
	VString fDatabaseUser = null;
	private CLabel lDatabasePassword = new CLabel();
	VPassword fDatabasePassword = new VPassword();
	VCheckBox okDatabaseServer = new VCheckBox();
	VCheckBox okDatabaseUser = new VCheckBox();
	VCheckBox okDatabaseSystem = new VCheckBox();
	VCheckBox okDatabaseSQL = new VCheckBox();
	//	Server for Send Mail
	CLabel 			lMailServer = new CLabel();
	VString 		fMailServer = null;
	private CLabel 	lAdminEMail = new CLabel();
	VString 		fAdminEMail = null;
	private CLabel 	lMailUser = new CLabel();
	VString 		fMailUser = null;
	private CLabel 	lMailPassword = new CLabel();
	VPassword 		fMailPassword = new VPassword();
	VCheckBox 		okMailServer = new VCheckBox();
	VCheckBox 		okMailUser = new VCheckBox();
	//	FR [ 402 ]
	private CLabel 	lMailPort = new CLabel();
	VString 		fMailPort = null;
	private CLabel 	lEncryptionType = new CLabel();
	VLookup 		fEncryptionType = null;
	private CLabel 	lAuthMechanism = new CLabel();
	VLookup 		fAuthMechanism = null;
	private CLabel 	lMailProtocol = new CLabel();
	VLookup 		fMailProtocol = null;
	//
	private CButton bHelp = new CButton(iHelp);
	private CButton bTest = new CButton();
	private CButton bSave = new CButton(iSave);
	
	/**
	 * Layout the look and feel of the panel
	 */
	private void layoutGrid()
	{
		int row = 0;
		
		this.setLayout(gridBagLayout);
		TitledBorder titledBorder = new TitledBorder("dummy");

		JLabel sectionLabel = new JLabel("Java");
		sectionLabel.setForeground(titledBorder.getTitleColor());
		JSeparator separator = new JSeparator();
		this.add(sectionLabel, new GridBagConstraints(0, row++, 7, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
						15, 5, 0, 10), 0, 0));
		this.add(separator, new GridBagConstraints(0, row++, 7, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(0, 5, 0, 10), 0, 0));

		this.add(lJavaHome, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,
						5, 2, 5), 0, 0));
		this.add(fJavaHome, new GridBagConstraints(1, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
						5, 2, 0), 0, 0));
		this.add(okJavaHome, new GridBagConstraints(2, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5,
						0, 2, 5), 0, 0));
		this.add(lJavaType, new GridBagConstraints(4, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,
						5, 2, 5), 0, 0));
		this.add(fJavaType, new GridBagConstraints(5, row++, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 5, 2, 0), 0, 0));

		this.add(lJavaOptions, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fJavaOpts, new GridBagConstraints(1, row++, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2,
						5, 2, 0), 0, 0));

		
		sectionLabel = new JLabel("Adempiere");
		sectionLabel.setForeground(titledBorder.getTitleColor());
		separator = new JSeparator();
		this.add(sectionLabel, new GridBagConstraints(0, row++, 7, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
						15, 5, 0, 0), 0, 0));
		this.add(separator, new GridBagConstraints(0, row++, 7, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(0, 5, 0, 10), 0, 0));
		this.add(lAdempiereHome, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,
						5, 2, 5), 0, 0));
		this.add(fAdempiereHome, new GridBagConstraints(1, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
						5, 2, 0), 0, 0));
		this.add(okAdempiereHome, new GridBagConstraints(2, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5,
						0, 2, 5), 0, 0));
		this.add(lKeyStore, new GridBagConstraints(4, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
						0, 0, 0, 0), 0, 0));
		this.add(fKeyStore, new GridBagConstraints(5, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 5, 2, 0), 0, 0));
		this.add(okKeyStore, new GridBagConstraints(6, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5,
						5, 2, 5), 0, 0));
		
		row++;

		sectionLabel = new JLabel(res.getString("AppsServer"));
		sectionLabel.setForeground(titledBorder.getTitleColor());
		separator = new JSeparator();
		this.add(sectionLabel, new GridBagConstraints(0, row++, 6, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
						15, 5, 0, 0), 0, 0));
		this.add(separator, new GridBagConstraints(0, row++, 7, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(0, 5, 0, 10), 0, 0));
		this.add(lAppsServer, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,
						5, 2, 5), 0, 0));
		this.add(fAppsServer, new GridBagConstraints(1, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
						5, 2, 0), 0, 0));
		this.add(okAppsServer, new GridBagConstraints(2, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5,
						0, 2, 5), 0, 0));
		this.add(lAppsType, new GridBagConstraints(4, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,
						5, 2, 5), 0, 0));
		this.add(fAppsType, new GridBagConstraints(5, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 5, 2, 0), 0, 0));

		row++;
		
		this.add(lDeployDir, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fDeployDir, new GridBagConstraints(1, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2,
						5, 2, 0), 0, 0));
		this.add(okDeployDir, new GridBagConstraints(2, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2,
						0, 2, 5), 0, 0));
		this.add(lJNPPort, new GridBagConstraints(4, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fJNPPort, new GridBagConstraints(5, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2,
						5, 2, 0), 0, 0));
		this.add(okJNPPort, new GridBagConstraints(6, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2,
						0, 2, 5), 0, 0));

		row++;
		
		this.add(lWebPort, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fWebPort, new GridBagConstraints(1, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2,
						5, 2, 0), 0, 0));
		this.add(okWebPort, new GridBagConstraints(2, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2,
						0, 2, 5), 0, 0));
		this.add(lSSLPort, new GridBagConstraints(4, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fSSLPort, new GridBagConstraints(5, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2,
						5, 2, 0), 0, 0));
		this.add(okSSLPort, new GridBagConstraints(6, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2,
						0, 2, 5), 0, 0));

		row++;
		
		sectionLabel = new JLabel(res.getString("DatabaseServer"));
		sectionLabel.setForeground(titledBorder.getTitleColor());
		separator = new JSeparator();
		this.add(sectionLabel, new GridBagConstraints(0, row++, 6, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
						15, 5, 0, 0), 0, 0));
		this.add(separator, new GridBagConstraints(0, row++, 7, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(0, 5, 0, 10), 0, 0));
		this.add(lDatabaseServer, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,
						5, 2, 5), 0, 0));
		this.add(fDatabaseServer, new GridBagConstraints(1, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,
						5, 2, 0), 0, 0));
		this.add(okDatabaseServer, new GridBagConstraints(2, row, 1, 1, 0.0,
				0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 2, 5), 0, 0));
		this.add(lDatabaseType, new GridBagConstraints(4, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,
						5, 2, 5), 0, 0));
		this.add(fDatabaseType, new GridBagConstraints(5, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 5, 2, 0), 0, 0));

		row++;
		
		this.add(lDatabaseName, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fDatabaseName, new GridBagConstraints(1, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2,
						5, 2, 0), 0, 0));
		this.add(okDatabaseSQL, new GridBagConstraints(2, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2,
						0, 2, 5), 0, 0));
		this.add(lDatabaseDiscovered, new GridBagConstraints(4, row, 1, 1, 0.0,
				0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 0, 2, 5), 0, 0));
		this.add(fDatabaseDiscovered, new GridBagConstraints(5, row, 1, 1, 0.5,
				0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(2, 5, 2, 0), 0, 0));

		row++;
		
		this.add(lDatabasePort, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fDatabasePort, new GridBagConstraints(1, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2,
						5, 2, 0), 0, 0));
		this.add(lSystemPassword, new GridBagConstraints(4, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fSystemPassword, new GridBagConstraints(5, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 0), 0, 0));
		this.add(okDatabaseSystem, new GridBagConstraints(6, row, 1, 1, 0.0,
				0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 0, 2, 5), 0, 0));

		row++;
		
		this.add(lDatabaseUser, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fDatabaseUser, new GridBagConstraints(1, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2,
						5, 2, 0), 0, 0));
		this.add(lDatabasePassword, new GridBagConstraints(4, row, 1, 1, 0.0,
				0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 5), 0, 0));
		this.add(fDatabasePassword, new GridBagConstraints(5, row, 1, 1, 0.5,
				0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 0), 0, 0));
		this.add(okDatabaseUser, new GridBagConstraints(6, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2,
						0, 2, 5), 0, 0));
		
		row++;

		sectionLabel = new JLabel(res.getString("MailServer"));
		sectionLabel.setForeground(titledBorder.getTitleColor());
		separator = new JSeparator();
		this.add(sectionLabel, new GridBagConstraints(0, row++, 6, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
						15, 5, 0, 0), 0, 0));
		this.add(separator, new GridBagConstraints(0, row++, 7, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(0, 5, 0, 10), 0, 0));

		this.add(lMailServer, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,
						5, 2, 5), 0, 0));
		this.add(fMailServer, new GridBagConstraints(1, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 5, 2, 0), 0, 0));
		this.add(lMailPort, new GridBagConstraints(4, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,
						5, 2, 5), 0, 0));
		this.add(fMailPort, new GridBagConstraints(5, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 5, 2, 0), 0, 0));
		this.add(okMailServer, new GridBagConstraints(6, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5,
						0, 2, 5), 0, 0));
		
		row++;
		
		this.add(lMailProtocol, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fMailProtocol, new GridBagConstraints(1, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 5), 0, 0));
		this.add(lAdminEMail, new GridBagConstraints(4, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fAdminEMail, new GridBagConstraints(5, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 0), 0, 0));
		
		row++;
		
		// FR [ 402 ]
		this.add(lEncryptionType, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fEncryptionType, new GridBagConstraints(1, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 5), 0, 0));
		this.add(lAuthMechanism, new GridBagConstraints(4, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fAuthMechanism, new GridBagConstraints(5, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 5), 0, 0));
		//
		row++;
		
		this.add(lMailUser, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fMailUser, new GridBagConstraints(1, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 0), 0, 0));
		this.add(lMailPassword, new GridBagConstraints(4, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,
						5, 2, 5), 0, 0));
		this.add(fMailPassword, new GridBagConstraints(5, row, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 0), 0, 0));
		this.add(okMailUser, new GridBagConstraints(6, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2,
						0, 2, 5), 0, 0));

		// grap extra space when window is maximized
		CPanel filler = new CPanel();
		filler.setOpaque(false);
		filler.setBorder(null);
		this.add(filler, new GridBagConstraints(0, row++, 1, 1, 0.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
				new Insets(0, 0, 0, 0), 0, 0));

		this.add(bTest, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
						15, 5, 10, 5), 0, 0));
		this.add(bHelp, new GridBagConstraints(3, row, 2, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
						15, 5, 10, 5), 0, 0));
		this.add(bSave, new GridBagConstraints(5, row, 2, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(
						15, 5, 10, 5), 0, 0));

	}
	/**
	 * 	Static Layout Init
	 *  @throws Exception
	 */
	private void jbInit() throws Exception
	{
		// Field configurations
		// Files
		fJavaHome 		= new VFile(res.getString("JavaHome"), 		true, false, true, FIELDLENGTH, true, false);
		fAdempiereHome 	= new VFile(res.getString("AdempiereHome"),	true, false, true, FIELDLENGTH, true, false);
		fDeployDir 		= new VFile(res.getString("DeployDir"), 	true, false, true, FIELDLENGTH, true, false);
		
		// Lookup/comboboxes
		fJavaType 		= new VLookup(res.getString("JavaType"), true, false, true, m_cc.lookupJavaType);
		fAppsType 		= new VLookup(res.getString("AppsType"), true, false, true, m_cc.lookupAppsType);
		fDatabaseType 	= new VLookup(res.getString("DatabaseType"), true, false, true, m_cc.lookupDBType); 								
		fDatabaseDiscovered = new VLookup(res.getString("TNSName"), true, false, true, m_cc.lookupDBDiscovered);
		((CComboBox) fDatabaseDiscovered.getCombo()).setAutoReducible(false);

		fEncryptionType	= new VLookup(res.getString("MailEncryptionType"), false, false, true, 
								new SimpleLookup(res.getString("MailEncryptionType"),ConfigurationData.ENCRYPTIONTYPE));
		fAuthMechanism 	= new VLookup(res.getString("MailAuthMechanism"), false, false, true, 
								new SimpleLookup(res.getString("MailAuthMechanism"),ConfigurationData.AUTHMECHANISMS));
		fMailProtocol 	= new VLookup(res.getString("MailProtocol"), false, false, true, 
								new SimpleLookup(res.getString("MailProtocol"),ConfigurationData.EMAIL_PROTOCOL));

		// Strings
		fJavaOpts		= new VString (res.getString("JavaOptions"), 	true, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);
		fAppsServer 	= new VString (res.getString("AppsServer"),		true, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);
		fJNPPort 		= new VString (res.getString("JNPPort"), 		false, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);
		fWebPort		= new VString (res.getString("WebPort"), 		false, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);
		fSSLPort 		= new VString (res.getString("SSLPort"), 		false, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);
		fDatabaseServer = new VString (res.getString("DatabaseServer"),	true, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);
		fDatabaseName 	= new VString (res.getString("DatabaseName"), 	false, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);
		fDatabasePort 	= new VString (res.getString("DatabasePort"), 	false, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);
		fDatabaseUser 	= new VString (res.getString("DatabaseUser"), 	false, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);
		fMailServer 	= new VString (res.getString("MailServer"), 	false, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);
		fAdminEMail 	= new VString (res.getString("AdminEMail"), 	false, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);
		fMailUser 		= new VString (res.getString("MailUser"), 		false, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);
		fMailPort 		= new VString (res.getString("MailPort"), 		false, false, true, DISPLAYLENGTH, FIELDLENGTH, null, null);

		
		// Lables and layout
		//	Java
		
		lJavaHome.setToolTipText(res.getString("JavaHomeInfo"));
		lJavaHome.setText(res.getString("JavaHome"));
		okJavaHome.setEnabled(false);
		
		lJavaType.setToolTipText(res.getString("JavaTypeInfo"));
		lJavaType.setText(res.getString("JavaType"));		
		fJavaType.setPreferredSize(fJavaHome.getPreferredSize());

		lJavaOptions.setToolTipText(res.getString("JavaOptionsInfo"));
		lJavaOptions.setText(res.getString("JavaOptions"));		
		fJavaOpts.setPreferredSize(fJavaHome.getPreferredSize());

		//	AdempiereHome - KeyStore

		lAdempiereHome.setToolTipText(res.getString("AdempiereHomeInfo"));
		lAdempiereHome.setText(res.getString("AdempiereHome"));
		okAdempiereHome.setEnabled(false);

		lKeyStore.setText(res.getString("KeyStorePassword"));
		lKeyStore.setToolTipText(res.getString("KeyStorePasswordInfo"));
		fKeyStore.setText("");
		okKeyStore.setEnabled(false);
		
		//	Apps Server - Type
		lAppsServer.setToolTipText(res.getString("AppsServerInfo"));
		lAppsServer.setText(res.getString("AppsServer"));
		lAppsServer.setFont(lAppsServer.getFont().deriveFont(Font.BOLD));
		fAppsServer.setText(".");
		okAppsServer.setEnabled(false);

		lAppsType.setToolTipText(res.getString("AppsTypeInfo"));
		lAppsType.setText(res.getString("AppsType"));
		fAppsType.setPreferredSize(fAppsServer.getPreferredSize());

		// 	Deployment - JNP
		lDeployDir.setToolTipText(res.getString("DeployDirInfo"));
		lDeployDir.setText(res.getString("DeployDir"));
		okDeployDir.setEnabled(false);

		lJNPPort.setToolTipText(res.getString("JNPPortInfo"));
		lJNPPort.setText(res.getString("JNPPort"));
		fJNPPort.setText(".");
		okJNPPort.setEnabled(false);

		//	Web Ports
		lWebPort.setToolTipText(res.getString("WebPortInfo"));
		lWebPort.setText(res.getString("WebPort"));
		fWebPort.setText(".");
		okWebPort.setEnabled(false);

		lSSLPort.setText(res.getString("SSLPortInfo"));
		lSSLPort.setText(res.getString("SSLPort"));
		fSSLPort.setText(".");
		okSSLPort.setEnabled(false);

		//	Database Server - Type
		lDatabaseServer.setToolTipText(res.getString("DatabaseServerInfo"));
		lDatabaseServer.setText(res.getString("DatabaseServer"));
		lDatabaseServer.setFont(lDatabaseServer.getFont().deriveFont(Font.BOLD));
		okDatabaseServer.setEnabled(false);
		
		lDatabaseType.setToolTipText(res.getString("DatabaseTypeInfo"));
		lDatabaseType.setText(res.getString("DatabaseType"));
		fDatabaseType.setPreferredSize(fDatabaseServer.getPreferredSize());
		
		//Database/Service Name
		lDatabaseName.setToolTipText(res.getString("DatabaseNameInfo"));
		lDatabaseName.setText(res.getString("DatabaseName"));
		fDatabaseName.setText(".");
		
		//TNS/Native connection
		lDatabaseDiscovered.setToolTipText(res.getString("TNSNameInfo"));
		lDatabaseDiscovered.setText(res.getString("TNSName"));
		fDatabaseDiscovered.setPreferredSize(fDatabaseName.getPreferredSize());
		okDatabaseSQL.setEnabled(false);
		
		//	Port - System
		lDatabasePort.setToolTipText(res.getString("DatabasePortInfo"));
		lDatabasePort.setText(res.getString("DatabasePort"));
		fDatabasePort.setText(".");
		
		lSystemPassword.setToolTipText(res.getString("SystemPasswordInfo"));
		lSystemPassword.setText(res.getString("SystemPassword"));
		fSystemPassword.setText(".");
		okDatabaseSystem.setEnabled(false);

		//	User - Password
		lDatabaseUser.setToolTipText(res.getString("DatabaseUserInfo"));
		lDatabaseUser.setText(res.getString("DatabaseUser"));
		fDatabaseUser.setText(".");
		
		lDatabasePassword.setToolTipText(res.getString("DatabasePasswordInfo"));
		lDatabasePassword.setText(res.getString("DatabasePassword"));
		fDatabasePassword.setText(".");
		okDatabaseUser.setEnabled(false);
		
		//	Mail Server - Email
		lMailServer.setToolTipText(res.getString("MailServerInfo"));
		lMailServer.setText(res.getString("MailServer"));
		lMailServer.setFont(lMailServer.getFont().deriveFont(Font.BOLD));
		fMailServer.setText(".");
		
		lAdminEMail.setToolTipText(res.getString("AdminEMailInfo"));
		lAdminEMail.setText(res.getString("AdminEMail"));
		fAdminEMail.setText(".");
		okMailServer.setEnabled(false);
		
		//	Mail User = Password
		lMailUser.setToolTipText(res.getString("MailUserInfo"));
		lMailUser.setText(res.getString("MailUser"));
		fMailUser.setText(".");
		
		lMailPassword.setToolTipText(res.getString("MailPasswordInfo"));
		lMailPassword.setText(res.getString("MailPassword"));
		fMailPassword.setText(".");
		
		//	FR [ 402 ]
		lMailProtocol.setToolTipText(res.getString("MailProtocolInfo"));
		lMailProtocol.setText(res.getString("MailProtocol"));
		fMailProtocol.setPreferredSize(fMailProtocol.getPreferredSize());
		
		lEncryptionType.setToolTipText(res.getString("MailEncryptionTypeInfo"));
		lEncryptionType.setText(res.getString("MailEncryptionType"));
		fEncryptionType.setPreferredSize(fEncryptionType.getPreferredSize());
		
		lAuthMechanism.setToolTipText(res.getString("MailAuthMechanismInfo"));
		lAuthMechanism.setText(res.getString("MailAuthMechanism"));
		fAuthMechanism.setPreferredSize(fAuthMechanism.getPreferredSize());
		
		lMailPort.setToolTipText(res.getString("MailPortInfo"));
		lMailPort.setText(res.getString("MailPort"));
		fMailPort.setText("25");
		okMailUser.setEnabled(false);

		//	End
		bTest.setToolTipText(res.getString("TestInfo"));
		bTest.setText(res.getString("Test"));

		bSave.setToolTipText(res.getString("SaveInfo"));
		bSave.setText(res.getString("Save"));

		bHelp.setToolTipText(res.getString("HelpInfo"));

		//  Register "this" as an action listener with the buttons that trigger actions
		bHelp.addActionListener(this);
		bTest.addActionListener(this);
		bSave.addActionListener(this);

		// Register the Configuration Controller as a vetoable Change Listener
		// to all the fields.
		fAdempiereHome.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fAdminEMail.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fAppsServer.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fAppsType.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fAuthMechanism.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fDatabaseDiscovered.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fDatabaseName.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fDatabasePassword.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fDatabasePort.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fDatabaseServer.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fDatabaseType.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fDatabaseUser.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fDeployDir.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fEncryptionType.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fJavaHome.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fJavaType.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fJNPPort.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fKeyStore.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fMailPassword.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fMailPort.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fMailProtocol.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fMailServer.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fMailUser.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fSSLPort.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fSystemPassword.addVetoableChangeListener((VetoableChangeListener) m_cc);
		fWebPort.addVetoableChangeListener((VetoableChangeListener) m_cc);
		
		// Setup look and feel
		layoutGrid();
	}	//	jbInit

	/**
	 * 	Dynamic Initial. Load the data.  The load process will fire property change events
	 *  which will be caught by the Configuration Controller which will update the field values.
	 * 	Called by Setup
	 *	@return true if success
	 */
	public boolean dynInit()
	{
		log.info("");
		return m_cc.load();  // Will fire property change events.
	}	//	dynInit

	/**
	 * 	Set Status Bar Text
	 *	@param text text
	 */
	protected void setStatusBar(String text)
	{
		m_statusBar.setText(text);
	}	//	setStatusBar
	
	
	/**************************************************************************
	 * 	ActionListener
	 *  @param e event
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (m_testing)
			return;
		
		if (e.getSource() == bHelp)
			new Setup_Help((Frame)SwingUtilities.getWindowAncestor(this));
		else if (e.getSource() == bTest)
			startTest(false);
		else if (e.getSource() == bSave)
			startTest(true);
	}	//	actionPerformed

	
	/**************************************************************************
	 * 	Start Test Async.
	 * 	@param saveIt save
	 *  @return SwingWorker
	 */
	private org.compiere.apps.SwingWorker startTest(final boolean saveIt)
	{
		org.compiere.apps.SwingWorker worker = new org.compiere.apps.SwingWorker()
		{
			//	Start it
			public Object construct()
			{
				m_testing = true;
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				bTest.setEnabled(false);
				m_success = false;
				m_errorString = null;
				try
				{
					test();
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					m_errorString += "\n" + ex.toString();
				}
				//
				setCursor(Cursor.getDefaultCursor());
				if (m_errorString == null)
					m_success = true;
				bTest.setEnabled(true);
				m_testing = false;
				return new Boolean(m_success);
			}
			//	Finish it
			public void finished()
			{
				if (m_errorString != null)
				{
					CLogger.get().severe(m_errorString);
					JOptionPane.showConfirmDialog (m_statusBar.getParent(), 
						m_errorString, 
						res.getString("ServerError"),
						JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
				}
				else if (saveIt)
					save();
				
			}
		};
		worker.start();
		return worker;
	}	//	startIt

	/**
	 *	Test it
	 * 	@throws Exception
	 */
	private void test() throws Exception
	{
		bSave.setEnabled(false);
		if (!m_cc.test())
			return;
		//
		m_statusBar.setText(res.getString("Ok"));
		bSave.setEnabled(true);
		m_errorString = null;
	}	//	test

	/**
	 * 	UI Signal OK
	 *	@param cb check box
	 *	@param resString resource string key
	 *	@param pass true if test passed
	 *	@param critical true if critical
	 *	@param errorMsg error Message
	 */
	void signalOK (VCheckBox cb, String resString, 
		boolean pass, boolean critical, String errorMsg)
	{
		m_errorString = res.getString(resString);
		if (cb != null)
		{
			cb.setSelected(pass);
			if (pass)
				cb.setToolTipText(null);
			else
			{
				cb.setToolTipText(errorMsg);
			}
		}
		if (!pass)
		{
			m_errorString += " \n(" + errorMsg + ")";
			
			CLogger.get().severe(m_errorString);
			JOptionPane.showConfirmDialog (m_statusBar.getParent(), 
				m_errorString, 
				res.getString("ServerError"),
				JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
		}

		if (!pass && critical)
			cb.setBackground(Color.RED);
		else
			cb.setBackground(Color.GREEN);
		

	}	//	setOK


	/**************************************************************************
	 * 	Save Settings.
	 * 	Called from startTest.finished()
	 */
	private void save()
	{
		if (!m_success)
			return;

		bSave.setEnabled(false);
		bTest.setEnabled(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		;  // update the m_data values from the fields.
		
		if (!saveData())// update the m_data values from the fields and save the properties file.
			return;
		
		//	Final Info
		JOptionPane.showConfirmDialog(this, res.getString("EnvironmentSaved"),
			res.getString("AdempiereServerSetup"),
			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);

		/**	Run Ant	**/
		try
		{
			CLogger.get().info("Starting Ant ... ");
			System.setProperty("ant.home", ".");
			String[] 	args = new String[] {"setup"};
		//	Launcher.main (args);	//	calls System.exit
			Main antMain = new Main();
			antMain.startAnt(args, null, null);
		}
		catch (Exception e)
		{
			CLogger.get().log(Level.SEVERE, "ant", e);
		}
			
		//	To be sure
		((Frame)SwingUtilities.getWindowAncestor(this)).dispose();
		System.exit(0);		//	remains active when License Dialog called
		/** **/
	}	//	save

	/**
	 * Save the data
	 * @return
	 */
	private boolean saveData() {
		
		boolean result = false;
		
		try {
			result = m_cc.save();
		}
		catch (Exception e)
		{
			JOptionPane.showConfirmDialog(this, 
				ConfigurationPanel.res.getString("ErrorSave"), 
				ConfigurationPanel.res.getString("AdempiereServerSetup"),
				JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
		}
		
		return result;
	}

//	/**
//	 * 	enable Apps Server related fields
//	 */
//	public void enableAppsServer()
//	{
//		int index = fAppsType.getSelectedIndex();
//		enableAppsServer(index);
//	}	//	initAppsServer

//	private void enableAppsServer(int index)
//	{
//		if (index < 0 || index >= m_data.APPSTYPE.length)
//		{
//			log.warning("AppsServerType Index invalid: " + index);
//		}
//		else if (m_appsConfig[index] == null)
//		{
//			log.warning("AppsServerType Config missing: " + m_data.APPSTYPE[index]);
//			fAppsType.setSelectedIndex(0);
//		}
//		else
//			m_appsConfig[index].enable();
//	}

//	/**
//	 * 	Test Apps Server
//	 *	@return error message or null of OK
//	 */
//	public String testAppsServer()
//	{
//		int index = p_panel != null 
//			? p_panel.fAppsType.getSelectedIndex()
//			: setAppsServerType((String)p_properties.get(ADEMPIERE_APPS_TYPE));
//		if (index < 0 || index >= APPSTYPE.length)
//			return "AppsServerType Index invalid: " + index;
//		else if (m_appsConfig[index] == null)
//			return "AppsServerType Config class missing: " + index;
//		return m_appsConfig[index].test();
//	}	//	testAppsServer

	public ConfigurationData getConfigurationData() {
		
		return null;
	}

	/**
	 * @param enable if true enable entry
	 */
	public void enableAppsServerDeployDir (boolean enable)
	{
		fDeployDir.setEnabled(enable);
//		bDeployDir.setEnabled(enable);
	}

	/**
	 * @param enable if enable JNP entry
	 */
	public void enableAppsServerJNPPort (boolean enable)
	{
		fJNPPort.setEnabled(enable);
	}

	/**
	 * @param enable if tre enable Web entry
	 */
	public void enableAppsServerWebPort (boolean enable)
	{
		fWebPort.setEnabled(enable);
	}

	/**
	 * @param enable if tre enable SSL entry
	 */
	public void enableAppsServerSSLPort (boolean enable)
	{
		fSSLPort.setEnabled(enable);
	
		
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		// TODO Auto-generated method stub
		
	}


}	//	ConfigurationPanel
