/******************************************************************************
 *  Copyright 2015 by ATOM Project                                   *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *    http://www.apache.org/licenses/LICENSE-2.0                              *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 ******************************************************************************/

/**
 * @author Min Du, Feifei Li
 * @email min.du.email@gmail.com, lifeifei@cs.utah.edu
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.filechooser.*;

public class AtomApplet extends javax.swing.JApplet implements ActionListener {

    public AtomApplet() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    int MAX_CHANNELS = 8;
    private JButton startButton,pauseButton, stopButton;

    int historyRange=60000;
//    this method is run when the applet is being loaded into memory or constructed. Think of this as the applet constructor. 
    public void init() {
        this.timerLength=5;

        initComponents();
    }

//    this is run once all preparations are done, and the code is actually beginning to run. Place any splash screens or intro stuff here. 
    public void start() {} 

//    this is run when the applet has been terminated, while memory is being freed and the code is stopping. 
    public void stop() {} 

//    this method is called just before a system exit. Notifying that the applet has finished and cannot be run again without reloading it.
    public void destroy() {} 
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        fc = new javax.swing.JFileChooser("./data");
        jPanel1 = null;
        initPanels(null);
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openItem = new javax.swing.JMenuItem();
        openItem.addActionListener(this);
        configItem = new javax.swing.JMenuItem();
        configItem.addActionListener(this);

        fileMenu.setText("File");
        openItem.setText("Input Streams");
        fileMenu.add(openItem);

        jMenuBar1.add(fileMenu);

        setJMenuBar(jMenuBar1);
    }

    private void ExitActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
        System.exit(0);
    }

    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {
        System.exit(0);
    }

    public void initPanels(File infile) {
    	if (jPanel1 != null) jPanel1 = null;
        jPanel1 = new AtomPanel(timerLength, historyRange, infile, AtomUtils.PCAType.pca);
        jPanel1.setPreferredSize(new Dimension(800, 500));
        getContentPane().removeAll();
        getContentPane().add(jPanel1, java.awt.BorderLayout.NORTH);
        jPanel2 = new AtomPanel(timerLength, historyRange, infile, AtomUtils.PCAType.pcaTrack);
        jPanel2.setPreferredSize(new Dimension(800, 500));
        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);

        JPanel sPanel = new JPanel(new BorderLayout());
        jPanel3 = new AtomPanel(timerLength, historyRange, infile, AtomUtils.PCAType.pcaTrackAdjust);
        jPanel3.setPreferredSize(new Dimension(800, 500));
//        getContentPane().add(jPanel3, java.awt.BorderLayout.SOUTH);
        sPanel.add(jPanel3, java.awt.BorderLayout.NORTH);
    	//create control panel
        JPanel panel=new JPanel(new FlowLayout());
        startButton=new JButton("Start");
        startButton.addActionListener(this);
        pauseButton=new JButton("Pause");
        pauseButton.addActionListener(this);
        stopButton=new JButton("Stop");
        stopButton.addActionListener(this);
        panel.add(startButton);
        panel.add(pauseButton);
        panel.add(stopButton);
        sPanel.add(panel, java.awt.BorderLayout.SOUTH);
        getContentPane().add(sPanel, java.awt.BorderLayout.SOUTH);
        getContentPane().revalidate();
        
    }
    
    public void actionPerformed(ActionEvent e) {
        //Handle open button action.
        if (e.getSource() == openItem) {
            int returnVal = fc.showOpenDialog(AtomApplet.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                inputFile = fc.getSelectedFile();
                initPanels(inputFile);
            }
        }
        else if(e.getSource()==startButton){
        	if (inputFile==null) {
        		JOptionPane.showMessageDialog(getContentPane(), "Please select file first.");
        		return;
        	}
        	jPanel1.setFlag(1);
        	jPanel2.setFlag(1);
        	jPanel3.setFlag(1);
        }
        else if(e.getSource()==pauseButton){
        	jPanel1.setFlag(2);
        	jPanel2.setFlag(2);
        	jPanel3.setFlag(2);
        }
        else if(e.getSource()==stopButton){
        	jPanel1.setFlag(3);
        	jPanel2.setFlag(3);
        	jPanel3.setFlag(3);
        }
    }

    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem openItem;
    private javax.swing.JMenuItem configItem;
    private javax.swing.JFileChooser fc;
    private javax.swing.JMenuBar jMenuBar1;
    private AtomPanel jPanel1;
    private AtomPanel jPanel2;
    private AtomPanel jPanel3;
    private int timerLength;
    private File inputFile;

    /**
     * @param args the command line arguments
     */

    private void jbInit() throws Exception {
    }
}

