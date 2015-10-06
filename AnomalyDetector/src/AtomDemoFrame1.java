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

public class AtomDemoFrame1 extends javax.swing.JFrame implements ActionListener {
    public AtomDemoFrame1() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    int MAX_CHANNELS = 8;
    private JButton startButton,pauseButton, stopButton;

    int historyRange=60000;
    public AtomDemoFrame1(int timerLength) {
        this.timerLength=timerLength;
        this.setIconImage( (new ImageIcon("./image/scslogo.gif").getImage()));

        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        fc = new javax.swing.JFileChooser("./data");
        jPanel1 = null;
        tmpPanel=new JPanel();

        getContentPane().add(tmpPanel,java.awt.BorderLayout.CENTER);

        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openItem = new javax.swing.JMenuItem();
        openItem.addActionListener(this);
        configItem = new javax.swing.JMenuItem();
        configItem.addActionListener(this);
        exitItem = new javax.swing.JMenuItem();
        exitItem.addActionListener(this);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("CloudWatch-ATOM Anomaly Detector");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });


        fileMenu.setText("File");
        openItem.setText("Input Streams");
        fileMenu.add(openItem);

        exitItem.setText("Exit");
        exitItem.addActionListener(this);

        fileMenu.add(exitItem);

        jMenuBar1.add(fileMenu);

        setJMenuBar(jMenuBar1);

        pack();
    }

    private void ExitActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
        System.exit(0);
    }

    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {
        System.exit(0);
    }

    public void actionPerformed(ActionEvent e) {
        //Handle open button action.
        if (e.getSource() == openItem) {
            int returnVal = fc.showOpenDialog(AtomDemoFrame1.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                inputFile = fc.getSelectedFile();
                if(jPanel1!=null)//jPanel is not null
                {
                    jPanel1=null;
                }
                jPanel1 = new AtomPanel(timerLength, historyRange, inputFile, AtomUtils.PCAType.pca); //varF.falseAlarmRate);
                jPanel1.setPreferredSize(new Dimension(800, 450));
                getContentPane().removeAll();
                getContentPane().add(jPanel1, java.awt.BorderLayout.NORTH);
                
//                JPanel trackPanel = new JPanel(new FlowLayout());
                jPanel2 = new AtomPanel(timerLength, historyRange, inputFile, AtomUtils.PCAType.pcaTrack); //varF.falseAlarmRate);
                jPanel2.setPreferredSize(new Dimension(800, 450));
                jPanel3 = new AtomPanel(timerLength, historyRange, inputFile, AtomUtils.PCAType.pcaTrackAdjust); //varF.falseAlarmRate);
                jPanel3.setPreferredSize(new Dimension(800, 450));
                JTabbedPane tabbedPane = new JTabbedPane();
                tabbedPane.addTab("Fixed Tracking Threshold", jPanel2);
                tabbedPane.addTab("Dynamic Tracking Threshold", jPanel3);
                getContentPane().add(tabbedPane, java.awt.BorderLayout.CENTER);
         
                getContentPane().add(tabbedPane, BorderLayout.CENTER);
                
                //create control panel
                JPanel panel=new JPanel(new FlowLayout());
        	//add control buttons to control panel
                startButton=new JButton("Start");
                startButton.addActionListener(this);
                pauseButton=new JButton("Pause");
                pauseButton.addActionListener(this);
                stopButton=new JButton("Stop");
                stopButton.addActionListener(this);
                panel.add(startButton);
                panel.add(pauseButton);
                panel.add(stopButton);
                getContentPane().add(panel, java.awt.BorderLayout.SOUTH);
                pack();
            }
        }
        else if (e.getSource() == exitItem) {
            System.exit(0);
        }
        else if(e.getSource()==startButton){
        	jPanel1.setFlag(1);
        	jPanel2.setFlag(1);
        	jPanel3.setFlag(1);
        }
        else if(e.getSource()==pauseButton){
        	jPanel1.setFlag(2);
        	jPanel3.setFlag(2);
        	jPanel2.setFlag(2);
        }
        else if(e.getSource()==stopButton){
        	jPanel1.setFlag(3);
        	jPanel2.setFlag(3);
        	jPanel3.setFlag(3);
        }
    }

    // Variables declaration - do not modify
    private javax.swing.JMenuItem exitItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem openItem;
    private javax.swing.JMenuItem configItem;
    private javax.swing.JFileChooser fc;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel tmpPanel;
    private AtomPanel jPanel1;
    private AtomPanel jPanel2;
    private AtomPanel jPanel3;
    private int timerLength;
    private File inputFile;
    private boolean trackType=true; // use fixed dynamic threshold
    // End of variables declaration

    /**
     * @param args the command line arguments
     */
    
    public static void main(String args[]) {
        AtomDemoFrame1 frame = new AtomDemoFrame1(5);
        frame.setBounds(200, 0, 500, 400);
//        JScrollPane pane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        frame.setContentPane(pane);
        frame.setVisible(true);
    }

    private void jbInit() throws Exception {
    }
}