//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.inventory;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.awt.image.*;
import java.awt.*;
import java.awt.event.*;

import com.openbravo.pos.forms.*;
import com.openbravo.data.loader.*;
import java.sql.*;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.format.Formats;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.loader.SentenceFind;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.sales.TaxesLogic;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author adrianromero
 */
public class ProductsEditor extends JPanel implements EditorRecord {
       
    private SentenceList m_sentcat;
    private ComboBoxValModel m_CategoryModel;

    private SentenceList taxcatsent;
    private ComboBoxValModel taxcatmodel;  

    private SentenceList attsent;
    private ComboBoxValModel attmodel;
    
    private SentenceList taxsent;
    private TaxesLogic taxeslogic;

    private SentenceFind loadimage;
    
    private ComboBoxValModel m_CodetypeModel;
    
    private Object m_id;
    private Object pricesell;
    private boolean priceselllock = false;
    
    private boolean reportlock = false;
    
    public Session session = null;

    /** Creates new form JEditProduct */
    public ProductsEditor(DataLogicSales dlSales, DirtyManager dirty) {
        initComponents();

        try {
            Method method = dlSales.getClass().getDeclaredMethod("getSession");
            method.setAccessible(true);
            session = Session.class.cast(method.invoke(dlSales));
        } catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            // field will always exist, and we override accessibility
            // if getting session fails, we handle the null later
        }

        loadimage = dlSales.getProductImage();
        
        // The taxes sentence
        taxsent = dlSales.getTaxList();
             
        // The categories model
        m_sentcat = dlSales.getCategoriesList();
        m_CategoryModel = new ComboBoxValModel();
        
        // The taxes model
        taxcatsent = dlSales.getTaxCategoriesList();
        taxcatmodel = new ComboBoxValModel();

        // The attributes model
        attsent = dlSales.getAttributeSetList();
        attmodel = new ComboBoxValModel();
        
        m_CodetypeModel = new ComboBoxValModel();
        m_CodetypeModel.add(null);
        m_CodetypeModel.add(CodeType.EAN13);
        m_CodetypeModel.add(CodeType.CODE128);
        m_jCodetype.setModel(m_CodetypeModel);
        m_jCodetype.setVisible(false);
               
        m_jRef.getDocument().addDocumentListener(dirty);
        m_jName.getDocument().addDocumentListener(dirty);
        m_jComment.addActionListener(dirty);
        m_jScale.addActionListener(dirty);
        m_jCategory.addActionListener(dirty);
        m_jTax.addActionListener(dirty);
        m_jAtt.addActionListener(dirty);
        m_jPriceBuy.getDocument().addDocumentListener(dirty);
        m_jPriceSell.getDocument().addDocumentListener(dirty);
        m_jImage.addPropertyChangeListener("image", dirty);
        m_jstockcost.getDocument().addDocumentListener(dirty);
        m_jstockvolume.getDocument().addDocumentListener(dirty);
        m_jInCatalog.addActionListener(dirty);
        m_jCatalogOrder.getDocument().addDocumentListener(dirty);
        txtAttributes.getDocument().addDocumentListener(dirty);

        FieldsManager fm = new FieldsManager();
        m_jPriceBuy.getDocument().addDocumentListener(fm);
        m_jPriceSell.getDocument().addDocumentListener(new PriceSellManager());
        m_jTax.addActionListener(fm);
        
        m_jPriceSellTax.getDocument().addDocumentListener(new PriceTaxManager());
        m_jmargin.getDocument().addDocumentListener(new MarginManager());
        
        writeValueEOF();
    }
    
    public void activate() throws BasicException {
        
        // Load the taxes logic
        taxeslogic = new TaxesLogic(taxsent.list());        
        
        m_CategoryModel = new ComboBoxValModel(m_sentcat.list());
        m_jCategory.setModel(m_CategoryModel);

        taxcatmodel = new ComboBoxValModel(taxcatsent.list());
        m_jTax.setModel(taxcatmodel);

        attmodel = new ComboBoxValModel(attsent.list());
        attmodel.add(0, null);
        m_jAtt.setModel(attmodel);
    }
    
    public void refresh() {
    }    
    
    public void writeValueEOF() {
        
        reportlock = true;
        // Los valores
        m_jTitle.setText(AppLocal.getIntString("label.recordeof"));
        m_id = null;
        m_jRef.setText(null);
        m_jCode.setText(null);
        m_jName.setText(null);
        m_jComment.setSelected(false);
        m_jScale.setSelected(false);
        m_CategoryModel.setSelectedKey(null);
        taxcatmodel.setSelectedKey(null);
        attmodel.setSelectedKey(null);
        m_jPriceBuy.setText(null);
        setPriceSell(null);         
        m_jImage.setImage(null);
        m_jstockcost.setText(null);
        m_jstockvolume.setText(null);
        m_jInCatalog.setSelected(false);
        m_jCatalogOrder.setText(null);
        txtAttributes.setText(null);
        reportlock = false;
        
        // Los habilitados
        m_jRef.setEnabled(false);
        m_jCode.setEnabled(false);
        m_jName.setEnabled(false);
        m_jComment.setEnabled(false);
        m_jScale.setEnabled(false);
        m_jCategory.setEnabled(false);
        m_jTax.setEnabled(false);
        m_jAtt.setEnabled(false);
        m_jPriceBuy.setEnabled(false);
        m_jPriceSell.setEnabled(false);
        m_jPriceSellTax.setEnabled(false);
        m_jmargin.setEnabled(false);
        m_jImage.setEnabled(false);
        m_jstockcost.setEnabled(false);
        m_jstockvolume.setEnabled(false);
        m_jInCatalog.setEnabled(false);
        m_jCatalogOrder.setEnabled(false);
        txtAttributes.setEnabled(false);
        
        calculateMargin();
        calculatePriceSellTax();
    }
    public void writeValueInsert() {
       
        reportlock = true;
        // Los valores
        m_jTitle.setText(AppLocal.getIntString("label.recordnew"));
        m_id = UUID.randomUUID().toString();
        m_jRef.setText(null);
        m_jCode.setText(null);
        m_jName.setText(null);
        m_jComment.setSelected(false);
        m_jScale.setSelected(false);
        m_CategoryModel.setSelectedKey(null);
        taxcatmodel.setSelectedKey(null);
        attmodel.setSelectedKey(null);
        m_jPriceBuy.setText(null);
        setPriceSell(null);                     
        m_jImage.setImage(null);
        m_jstockcost.setText(null);
        m_jstockvolume.setText(null);
        m_jInCatalog.setSelected(true);
        m_jCatalogOrder.setText(null);
        txtAttributes.setText(null);
        reportlock = false;
        
        // Los habilitados
        m_jRef.setEnabled(true);
        m_jCode.setEnabled(true);
        m_jName.setEnabled(true);
        m_jComment.setEnabled(true);
        m_jScale.setEnabled(true);
        m_jCategory.setEnabled(true);
        m_jTax.setEnabled(true);
        m_jAtt.setEnabled(true);
        m_jPriceBuy.setEnabled(true);
        m_jPriceSell.setEnabled(true); 
        m_jPriceSellTax.setEnabled(true);
        m_jmargin.setEnabled(true);
        m_jImage.setEnabled(true);
        m_jstockcost.setEnabled(true);
        m_jstockvolume.setEnabled(true);
        m_jInCatalog.setEnabled(true); 
        m_jCatalogOrder.setEnabled(false);
        txtAttributes.setEnabled(true);
        
        calculateMargin();
        calculatePriceSellTax();
   }
    public void writeValueDelete(Object value) {
        
        reportlock = true;       
        Object[] myprod = (Object[]) value;
        m_jTitle.setText(Formats.STRING.formatValue(myprod[1]) + " - " + Formats.STRING.formatValue(myprod[3]) + " " + AppLocal.getIntString("label.recorddeleted"));
        m_id = myprod[0];
        m_jRef.setText(Formats.STRING.formatValue(myprod[1]));
        m_jCode.setText(Formats.STRING.formatValue(myprod[2]));
        m_jName.setText(Formats.STRING.formatValue(myprod[3]));
        m_jComment.setSelected(((Boolean)myprod[4]).booleanValue());
        m_jScale.setSelected(((Boolean)myprod[5]).booleanValue());
        m_jPriceBuy.setText(Formats.CURRENCY.formatValue(myprod[6]));
        setPriceSell(myprod[7]);                    
        m_CategoryModel.setSelectedKey(myprod[8]);
        taxcatmodel.setSelectedKey(myprod[9]);
        attmodel.setSelectedKey(myprod[10]);
        m_jImage.setImage(findImage(m_id));
        m_jstockcost.setText(Formats.CURRENCY.formatValue(myprod[12]));
        m_jstockvolume.setText(Formats.DOUBLE.formatValue(myprod[13]));
        m_jInCatalog.setSelected(((Boolean)myprod[14]).booleanValue());
        m_jCatalogOrder.setText(Formats.INT.formatValue(myprod[15]));
        txtAttributes.setText(Formats.BYTEA.formatValue(myprod[16]));
        txtAttributes.setCaretPosition(0);
        reportlock = false;
        
        // Los habilitados
        m_jRef.setEnabled(false);
        m_jCode.setEnabled(false);
        m_jName.setEnabled(false);
        m_jComment.setEnabled(false);
        m_jScale.setEnabled(false);
        m_jCategory.setEnabled(false);
        m_jTax.setEnabled(false);
        m_jAtt.setEnabled(false);
        m_jPriceBuy.setEnabled(false);
        m_jPriceSell.setEnabled(false);
        m_jPriceSellTax.setEnabled(false);
        m_jmargin.setEnabled(false);
        m_jImage.setEnabled(false);
        m_jstockcost.setEnabled(false);
        m_jstockvolume.setEnabled(false);
        m_jInCatalog.setEnabled(false);
        m_jCatalogOrder.setEnabled(false);
        txtAttributes.setEnabled(false);
        
        calculateMargin();
        calculatePriceSellTax();
    }    
    
    public void writeValueEdit(Object value) {
        
        reportlock = true;
        Object[] myprod = (Object[]) value;
        m_jTitle.setText(Formats.STRING.formatValue(myprod[1]) + " - " + Formats.STRING.formatValue(myprod[3]));
        m_id = myprod[0];
        m_jRef.setText(Formats.STRING.formatValue(myprod[1]));
        m_jCode.setText(Formats.STRING.formatValue(myprod[2]));
        m_jName.setText(Formats.STRING.formatValue(myprod[3]));
        m_jComment.setSelected(((Boolean)myprod[4]).booleanValue());
        m_jScale.setSelected(((Boolean)myprod[5]).booleanValue());
        m_jPriceBuy.setText(Formats.CURRENCY.formatValue(myprod[6]));
        setPriceSell(myprod[7]);                               
        m_CategoryModel.setSelectedKey(myprod[8]);
        taxcatmodel.setSelectedKey(myprod[9]);
        attmodel.setSelectedKey(myprod[10]);
        m_jImage.setImage(findImage(m_id));
        m_jstockcost.setText(Formats.CURRENCY.formatValue(myprod[12]));
        m_jstockvolume.setText(Formats.DOUBLE.formatValue(myprod[13]));
        m_jInCatalog.setSelected(((Boolean)myprod[14]).booleanValue());
        m_jCatalogOrder.setText(Formats.INT.formatValue(myprod[15]));
        txtAttributes.setText(Formats.BYTEA.formatValue(myprod[16]));
        txtAttributes.setCaretPosition(0);
        reportlock = false;
        
        // Los habilitados
        m_jRef.setEnabled(true);
        m_jCode.setEnabled(true);
        m_jName.setEnabled(true);
        m_jComment.setEnabled(true);
        m_jScale.setEnabled(true);
        m_jCategory.setEnabled(true);
        m_jTax.setEnabled(true);
        m_jAtt.setEnabled(true);
        m_jPriceBuy.setEnabled(true);
        m_jPriceSell.setEnabled(true); 
        m_jPriceSellTax.setEnabled(true);
        m_jmargin.setEnabled(true);
        m_jImage.setEnabled(true);
        m_jstockcost.setEnabled(true);
        m_jstockvolume.setEnabled(true);
        m_jInCatalog.setEnabled(true);
        m_jCatalogOrder.setEnabled(m_jInCatalog.isSelected());  
        txtAttributes.setEnabled(true);
        
        calculateMargin();
        calculatePriceSellTax();
    }

    public Object createValue() throws BasicException {
        
        Object[] myprod = new Object[17];
        myprod[0] = m_id;
        myprod[1] = m_jRef.getText();
        myprod[2] = m_jCode.getText();
        myprod[3] = m_jName.getText();
        myprod[4] = Boolean.valueOf(m_jComment.isSelected());
        myprod[5] = Boolean.valueOf(m_jScale.isSelected());
        myprod[6] = Formats.CURRENCY.parseValue(m_jPriceBuy.getText());
        myprod[7] = pricesell;
        myprod[8] = m_CategoryModel.getSelectedKey();
        myprod[9] = taxcatmodel.getSelectedKey();
        myprod[10] = attmodel.getSelectedKey();
        myprod[11] = m_jImage.getImage();
        myprod[12] = Formats.CURRENCY.parseValue(m_jstockcost.getText());
        myprod[13] = Formats.DOUBLE.parseValue(m_jstockvolume.getText());
        myprod[14] = Boolean.valueOf(m_jInCatalog.isSelected());
        myprod[15] = Formats.INT.parseValue(m_jCatalogOrder.getText());
        myprod[16] = Formats.BYTEA.parseValue(txtAttributes.getText());
        
        return myprod;
    }    
    
    public Component getComponent() {
        return this;
    }

    private BufferedImage findImage(Object id) {
        try {
            return (BufferedImage) loadimage.find(id);
        } catch (BasicException e) {
            return null;
        }
    }

    private void calculateMargin() {
        
        if (!reportlock) {
            reportlock = true;
            
            Double dPriceBuy = readCurrency(m_jPriceBuy.getText());
            Double dPriceSell = (Double) pricesell;

            if (dPriceBuy == null || dPriceSell == null) {
                m_jmargin.setText(null);
            } else {
                m_jmargin.setText(Formats.PERCENT.formatValue(new Double(dPriceSell.doubleValue() / dPriceBuy.doubleValue() - 1.0)));
            }    
            reportlock = false;
        }
    }
    
    private void calculatePriceSellTax() {
        
        if (!reportlock) {
            reportlock = true;
            
            Double dPriceSell = (Double) pricesell;
            
            if (dPriceSell == null) {
                m_jPriceSellTax.setText(null);
            } else {               
                double dTaxRate = taxeslogic.getTaxRate((TaxCategoryInfo) taxcatmodel.getSelectedItem(), new Date());
                m_jPriceSellTax.setText(Formats.CURRENCY.formatValue(new Double(dPriceSell.doubleValue() * (1.0 + dTaxRate))));
            }            
            reportlock = false;
        }
    }
    
    private void calculatePriceSellfromMargin() {
        
        if (!reportlock) {
            reportlock = true;
            
            Double dPriceBuy = readCurrency(m_jPriceBuy.getText());
            Double dMargin = readPercent(m_jmargin.getText());  
            
            if (dMargin == null || dPriceBuy == null) {
                setPriceSell(null);
            } else {
                setPriceSell(new Double(dPriceBuy.doubleValue() * (1.0 + dMargin.doubleValue())));
            }                        
            
            reportlock = false;
        }
      
    }
    
    private void calculatePriceSellfromPST() {
        
        if (!reportlock) {
            reportlock = true;
                    
            Double dPriceSellTax = readCurrency(m_jPriceSellTax.getText());  

            if (dPriceSellTax == null) {
                setPriceSell(null);
            } else {
                double dTaxRate = taxeslogic.getTaxRate((TaxCategoryInfo) taxcatmodel.getSelectedItem(), new Date());
                setPriceSell(new Double(dPriceSellTax.doubleValue() / (1.0 + dTaxRate)));
            }   
                        
            reportlock = false;
        }    
    }
    
    private void setPriceSell(Object value) {
        
        if (!priceselllock) {
            priceselllock = true;
            pricesell = value;
            m_jPriceSell.setText(Formats.CURRENCY.formatValue(pricesell));  
            priceselllock = false;
        }
    }
    
    private class PriceSellManager implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            if (!priceselllock) {
                priceselllock = true;
                pricesell = readCurrency(m_jPriceSell.getText());
                priceselllock = false;
            }
            calculateMargin();
            calculatePriceSellTax();
        }
        public void insertUpdate(DocumentEvent e) {
            if (!priceselllock) {
                priceselllock = true;
                pricesell = readCurrency(m_jPriceSell.getText());
                priceselllock = false;
            }
            calculateMargin();
            calculatePriceSellTax();
        }    
        public void removeUpdate(DocumentEvent e) {
            if (!priceselllock) {
                priceselllock = true;
                pricesell = readCurrency(m_jPriceSell.getText());
                priceselllock = false;
            }
            calculateMargin();
            calculatePriceSellTax();
        }  
    }
    
    private class FieldsManager implements DocumentListener, ActionListener {
        public void changedUpdate(DocumentEvent e) {
            calculateMargin();
            calculatePriceSellTax();
        }
        public void insertUpdate(DocumentEvent e) {
            calculateMargin();
            calculatePriceSellTax();
        }    
        public void removeUpdate(DocumentEvent e) {
            calculateMargin();
            calculatePriceSellTax();
        }         
        public void actionPerformed(ActionEvent e) {
            calculateMargin();
            calculatePriceSellTax();
        }
    }

    private class PriceTaxManager implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            calculatePriceSellfromPST();
            calculateMargin();
        }
        public void insertUpdate(DocumentEvent e) {
            calculatePriceSellfromPST();
            calculateMargin();
        }    
        public void removeUpdate(DocumentEvent e) {
            calculatePriceSellfromPST();
            calculateMargin();
        }         
    }
    
    private class MarginManager implements DocumentListener  {
        public void changedUpdate(DocumentEvent e) {
            calculatePriceSellfromMargin();
            calculatePriceSellTax();
        }
        public void insertUpdate(DocumentEvent e) {
            calculatePriceSellfromMargin();
            calculatePriceSellTax();
        }    
        public void removeUpdate(DocumentEvent e) {
            calculatePriceSellfromMargin();
            calculatePriceSellTax();
        }         
    }
    
    private final static Double readCurrency(String sValue) {
        try {
            return (Double) Formats.CURRENCY.parseValue(sValue);
        } catch (BasicException e) {
            return null;
        }
    }
        
    private final static Double readPercent(String sValue) {
        try {
            return (Double) Formats.PERCENT.parseValue(sValue);
        } catch (BasicException e) {
            return null;
        }
    }

    public void setBarcodeAndRef(String code){
        m_jCode.setText(code);
        m_jRef.setText(code);
    }
    
    public String getCode(){
        return m_jCode.getText();
    }

    private final void showBarcodeGen(){
        new BFrame(this);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        m_jRef = new javax.swing.JTextField();
        m_jName = new javax.swing.JTextField();
        m_jTitle = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JButton("More");
        genCode = new javax.swing.JButton("Print");
        m_jCode = new javax.swing.JTextField();
        m_jImage = new com.openbravo.data.gui.JImageEditor();
        jLabel3 = new javax.swing.JLabel();
        m_jPriceBuy = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        m_jPriceSell = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        m_jCategory = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        m_jTax = new javax.swing.JComboBox();
        m_jmargin = new javax.swing.JTextField();
        m_jPriceSellTax = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        m_jCodetype = new javax.swing.JComboBox();
        jLabel13 = new javax.swing.JLabel();
        m_jAtt = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        m_jstockcost = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        m_jstockvolume = new javax.swing.JTextField();
        m_jScale = new javax.swing.JCheckBox();
        m_jComment = new javax.swing.JCheckBox();
        jLabel18 = new javax.swing.JLabel();
        m_jCatalogOrder = new javax.swing.JTextField();
        m_jInCatalog = new javax.swing.JCheckBox();
        jLabel8 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtAttributes = new javax.swing.JTextArea();

        jLabel6.addActionListener(new ActionListener(){
            int num = 15;
            JTextField[]  fields;
            public void actionPerformed(ActionEvent ae){
                new JFrame(){
                    {
                        fields = new JTextField[num];
                        JLabel[] labels = new JLabel[num];
                        JButton submit = new JButton("Submit");
                        AppConfig config = new AppConfig(new String[0]);
                        config.load();
                        try {
                            Session s = AppViewConnection.createSession(config);

                            setLayout(new GridLayout(num+1,2));
                            setPreferredSize(new Dimension(230,25*(num+1)));
                            for(int n = 0; n < num; n++){
                                fields[n] = new JTextField();
                                labels[n] = new JLabel(String.valueOf(n+1), JLabel.CENTER);
                                fields[n].setPreferredSize(new Dimension(200,25));
                                labels[n].setPreferredSize(new Dimension(30,25));
                                add(labels[n]);
                                add(fields[n]);
                            }
                            add(new JLabel());
                            add(submit);
                            //check product exists in database
                            String statement = "SELECT * FROM PRODUCTS WHERE PRODUCTS.CODE ='" + m_jCode.getText() + "'";
                            PreparedStatement ps = s.getConnection().prepareStatement(statement);
                            ps.execute();
                            ResultSet rs = ps.getResultSet();
                            rs.last();
                            if(rs.getRow() < 1){
                                javax.swing.JOptionPane.showMessageDialog(null,"Please save product before adding more barcodes");
                            }
                            else{
                                submit.addActionListener(new ActionListener(){
                                    public void actionPerformed(ActionEvent ae){
                                        try{
                                            AppConfig config = new AppConfig(new String[0]);
                                            config.load();

                                            Session s = AppViewConnection.createSession(config);
                                            String statement;
                                            PreparedStatement ps ;
                                            ResultSet rs ;

                                            //check for duplication attempt
                                            statement = "SELECT BARCODE_TABLE.Code,PRODUCTS.NAME FROM BARCODE_TABLE, PRODUCTS WHERE BARCODE_TABLE.PID = PRODUCTS.ID AND PRODUCTS.ID != '" + m_id + "'";
                                            ps = s.getConnection().prepareStatement(statement);
                                            ps.execute();
                                            rs = ps.getResultSet();

                                            String code;
                                            while(rs.next()){
                                                code = rs.getString(1);
                                                for(int n = 0; n < num; n++){
                                                    Matcher m = Pattern.compile("0{0,2}"+fields[n].getText()+"{0,1}").matcher(code);
                                                    if(m.matches()){
                                                        javax.swing.JOptionPane.showMessageDialog(null,code + " is a duplicate. It is already assigned to '" + rs.getString(2) + "'");
                                                        return;
                                                    }
                                                }
                                            }
                                            //check local codes against products.code
                                            statement = "SELECT Products.Code,PRODUCTS.NAME FROM PRODUCTS";
                                            ps = s.getConnection().prepareStatement(statement);
                                            ps.execute();
                                            rs = ps.getResultSet();

                                            while(rs.next()){
                                                code = rs.getString(1);
                                                for(int n = 0; n < num; n++){
                                                    Matcher m = Pattern.compile("0{0,2}"+fields[n].getText()+"{0,1}").matcher(code);
                                                    if(m.matches()){
                                                        javax.swing.JOptionPane.showMessageDialog(null,code + " is a duplicate. It is already assigned to '" + rs.getString(2) + "'");
                                                        return;
                                                    }
                                                }
                                            }
                                            //check for 2 same entries on form
                                            for(int n = 0; n < num; n++){
                                                for(int m = 0; m < num; m++){
                                                    if(n != m && !fields[m].getText().equals("") && fields[m].getText().equals(fields[n].getText())){
                                                        javax.swing.JOptionPane.showMessageDialog(null,fields[n].getText() + " is a duplicate. It has been entered more than once on this form");
                                                        return;
                                                    }
                                                }
                                            }

                                            //delete old
                                            statement = "DELETE FROM BARCODE_TABLE WHERE pid = '" + m_id +"'";
                                            ps = s.getConnection().prepareStatement(statement);
                                            ps = s.getConnection().prepareStatement(statement);
                                            ps.execute();

                                            //insert new
                                            for(int p = 0; p < num; p++){
                                                if(!"".equals(fields[p].getText())){
                                                    statement = "INSERT INTO BARCODE_TABLE (pid, code) VALUES ('" + m_id + "','" + fields[p].getText() + "')";
                                                    ps = s.getConnection().prepareStatement(statement);
                                                    ps.execute();
                                                }
                                            }
                                        } catch(Exception e) {
                                            e.printStackTrace();
                                        }
                                        hid();
                                    }
                                });

                                statement = "SELECT barcode_table.code FROM barcode_table,products WHERE barcode_table.pid = products.ID AND products.ID = '" + m_id + "'";

                                ps = s.getConnection().prepareStatement(statement);
                                ps.execute();

                                rs = ps.getResultSet();

                                int count = 0;
                                while(rs.next()){
                                    if(count > num-1 ){
                                        break;
                                    }
                                    fields[count++].setText(rs.getString(1));
                                }

                                pack();
                                setLocationRelativeTo(null);
                                setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                setVisible(true);
                            }
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                    public void hid(){
                        setVisible(false);
                    }
                };
            }
        });

		genCode.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				showBarcodeGen();
			}
		});

		m_jCode.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent ke){
			}
			public void keyReleased(KeyEvent ke){
			}
			public void keyTyped(KeyEvent ke){
				if(m_jCode.getText().length() == 13){
					m_jRef.setText(m_jCode.getText());
				}
			}
		});

        setLayout(null);

        jLabel1.setText(AppLocal.getIntString("label.prodref")); // NOI18N
        add(jLabel1);
        jLabel1.setBounds(10, 50, 80, 15);

        jLabel2.setText(AppLocal.getIntString("label.prodname")); // NOI18N
        add(jLabel2);
        jLabel2.setBounds(180, 50, 70, 15);
        add(m_jRef);
        m_jRef.setBounds(90, 50, 70, 19);
        add(m_jName);
        m_jName.setBounds(250, 50, 220, 19);

        m_jTitle.setFont(new java.awt.Font("SansSerif", 3, 18));
        add(m_jTitle);
        m_jTitle.setBounds(10, 10, 320, 30);

        jPanel1.setLayout(null);

        jPanel1.add(jLabel6);
        jPanel1.add(genCode);
        genCode.setBounds(85, 20, 75, 25);
        jLabel6.setBounds(10, 20, 75, 25);
        jPanel1.add(m_jCode);
        m_jCode.setBounds(160, 20, 170, 19);
        jPanel1.add(m_jImage);
        m_jImage.setBounds(340, 20, 200, 180);

        jLabel3.setText(AppLocal.getIntString("label.prodpricebuy")); // NOI18N
        jPanel1.add(jLabel3);
        jLabel3.setBounds(10, 50, 150, 15);

        m_jPriceBuy.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel1.add(m_jPriceBuy);
        m_jPriceBuy.setBounds(160, 50, 80, 19);

        jLabel4.setText(AppLocal.getIntString("label.prodpricesell")); // NOI18N
        jPanel1.add(jLabel4);
        jLabel4.setBounds(10, 80, 150, 15);

        m_jPriceSell.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel1.add(m_jPriceSell);
        m_jPriceSell.setBounds(160, 80, 80, 19);

        jLabel5.setText(AppLocal.getIntString("label.prodcategory")); // NOI18N
        jPanel1.add(jLabel5);
        jLabel5.setBounds(10, 170, 150, 15);
        jPanel1.add(m_jCategory);
        m_jCategory.setBounds(160, 170, 170, 20);

        jLabel7.setText(AppLocal.getIntString("label.taxcategory")); // NOI18N
        jPanel1.add(jLabel7);
        jLabel7.setBounds(10, 140, 150, 15);
        jPanel1.add(m_jTax);
        m_jTax.setBounds(160, 140, 170, 20);

        m_jmargin.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel1.add(m_jmargin);
        m_jmargin.setBounds(250, 80, 80, 19);

        m_jPriceSellTax.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel1.add(m_jPriceSellTax);
        m_jPriceSellTax.setBounds(160, 110, 80, 19);

        jLabel16.setText(AppLocal.getIntString("label.prodpriceselltax")); // NOI18N
        jPanel1.add(jLabel16);
        jLabel16.setBounds(10, 110, 150, 15);
        jPanel1.add(m_jCodetype);
        m_jCodetype.setBounds(250, 40, 80, 20);

        jLabel13.setText(AppLocal.getIntString("label.attributes")); // NOI18N
        jPanel1.add(jLabel13);
        jLabel13.setBounds(10, 200, 150, 15);
        jPanel1.add(m_jAtt);
        m_jAtt.setBounds(160, 200, 170, 20);

        jTabbedPane1.addTab(AppLocal.getIntString("label.prodgeneral"), jPanel1); // NOI18N

        jPanel2.setLayout(null);

        jLabel9.setText(AppLocal.getIntString("label.prodstockcost")); // NOI18N
        jPanel2.add(jLabel9);
        jLabel9.setBounds(10, 20, 150, 15);

        m_jstockcost.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel2.add(m_jstockcost);
        m_jstockcost.setBounds(160, 20, 80, 19);

        jLabel10.setText(AppLocal.getIntString("label.prodstockvol")); // NOI18N
        jPanel2.add(jLabel10);
        jLabel10.setBounds(10, 50, 150, 15);

        m_jstockvolume.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel2.add(m_jstockvolume);
        m_jstockvolume.setBounds(160, 50, 80, 19);
        jPanel2.add(m_jScale);
        m_jScale.setBounds(160, 140, 80, 21);
        jPanel2.add(m_jComment);
        m_jComment.setBounds(160, 110, 80, 21);

        jLabel18.setText(AppLocal.getIntString("label.prodorder")); // NOI18N
        jPanel2.add(jLabel18);
        jLabel18.setBounds(250, 80, 60, 15);

        m_jCatalogOrder.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jPanel2.add(m_jCatalogOrder);
        m_jCatalogOrder.setBounds(310, 80, 80, 19);

        m_jInCatalog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jInCatalogActionPerformed(evt);
            }
        });
        jPanel2.add(m_jInCatalog);
        m_jInCatalog.setBounds(160, 80, 50, 21);

        jLabel8.setText(AppLocal.getIntString("label.prodincatalog")); // NOI18N
        jPanel2.add(jLabel8);
        jLabel8.setBounds(10, 80, 150, 15);

        jLabel11.setText(AppLocal.getIntString("label.prodaux")); // NOI18N
        jPanel2.add(jLabel11);
        jLabel11.setBounds(10, 110, 150, 15);

        jLabel12.setText(AppLocal.getIntString("label.prodscale")); // NOI18N
        jPanel2.add(jLabel12);
        jLabel12.setBounds(10, 140, 150, 15);

        jTabbedPane1.addTab(AppLocal.getIntString("label.prodstock"), jPanel2); // NOI18N

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel3.setLayout(new java.awt.BorderLayout());

        txtAttributes.setFont(new java.awt.Font("DialogInput", 0, 12));
        jScrollPane1.setViewportView(txtAttributes);

        jPanel3.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab(AppLocal.getIntString("label.properties"), jPanel3); // NOI18N

        add(jTabbedPane1);
        jTabbedPane1.setBounds(10, 90, 560, 280);
    }// </editor-fold>//GEN-END:initComponents

    private void m_jInCatalogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jInCatalogActionPerformed
 
        if (m_jInCatalog.isSelected()) {
            m_jCatalogOrder.setEnabled(true);   
        } else {
            m_jCatalogOrder.setEnabled(false);   
            m_jCatalogOrder.setText(null);   
        }

    }//GEN-LAST:event_m_jInCatalogActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JButton jLabel6;
    private javax.swing.JButton genCode;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JComboBox m_jAtt;
    private javax.swing.JTextField m_jCatalogOrder;
    private javax.swing.JComboBox m_jCategory;
    private javax.swing.JTextField m_jCode;
    private javax.swing.JFrame m_jCodeFrame;
    private javax.swing.JComboBox m_jCodetype;
    private javax.swing.JCheckBox m_jComment;
    private com.openbravo.data.gui.JImageEditor m_jImage;
    private javax.swing.JCheckBox m_jInCatalog;
    private javax.swing.JTextField m_jName;
    private javax.swing.JTextField m_jPriceBuy;
    private javax.swing.JTextField m_jPriceSell;
    private javax.swing.JTextField m_jPriceSellTax;
    private javax.swing.JTextField m_jRef;
    private javax.swing.JCheckBox m_jScale;
    private javax.swing.JComboBox m_jTax;
    private javax.swing.JLabel m_jTitle;
    private javax.swing.JTextField m_jmargin;
    private javax.swing.JTextField m_jstockcost;
    private javax.swing.JTextField m_jstockvolume;
    private javax.swing.JTextArea txtAttributes;
    // End of variables declaration//GEN-END:variables
    
}
