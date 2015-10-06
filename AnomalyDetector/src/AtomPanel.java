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

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.io.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import org.jfree.chart.axis.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.*;
import org.jfree.ui.Spacer;

/**
 * A demo application showing a dynamically updated chart that displays the current JVM memory
 * usage.
 */
public class AtomPanel extends JPanel implements ActionListener, ChangeListener{
    public AtomPanel() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static final int L_MIN = 1;
    static final int L_MAX = 10;
    static final int L_INIT = 1;
    static final int WDL_INIT = 5;
    private double falseAlarmRate=.001;

    private final int MaxNumOfStream=50;
    private int numOfStream;
    /** Time series of incoming streams. */
    public TimeSeriesCollection datasets;
    /** Time series of pca variables */
    public TimeSeriesCollection trend;
    public TimeSeriesCollection projpcs;
    public TimeSeriesCollection spe;
//    private JButton startButton,pauseButton, stopButton;
    private JLabel trackSaveLabel;
    private boolean startFlag;
    private int historyRange;
    public DataGenerator dgen;
    public CombinedDomainXYPlot combineddomainxyplot;
    public XYPlot plot1;
    private AtomMain atom = new AtomMain();
    private AtomUtils.PCAType pcaType;
    private double percent;
    private double devBnd;
    private int wdSz=100;
    /**
     * Creates a new application.
     *
     * @param historyCount  the history count (in milliseconds).
     * @param numOfStream the number of input streams
     */
    public AtomPanel(int timerLength, int historyRange, File inputFile, AtomUtils.PCAType type) {
        super(new BorderLayout());
        this.historyRange=historyRange;
        this.pcaType = type;
        commonInit();
        dgen=new DataGenerator(timerLength, inputFile);
        dgen.start();
    }
    private void commonInit(){
	//initial time series plots
        initJFreeChart();
        startFlag=false;

	//create control panel
//        JPanel panel=new JPanel(new GridLayout(1,2));
	//add control buttons to control panel
        /*
        startButton=new JButton("Start");
        startButton.addActionListener(this);
        pauseButton=new JButton("Pause");
        pauseButton.addActionListener(this);
        stopButton=new JButton("Stop");
        stopButton.addActionListener(this);
        panel.add(startButton);
        panel.add(pauseButton);
        panel.add(stopButton);
        */
    JPanel panel=new JPanel(new FlowLayout(FlowLayout.CENTER));
	//Create the slider
	JSlider faRateSlider = new JSlider(JSlider.HORIZONTAL, L_MIN, L_MAX, L_INIT);
	faRateSlider.setName("faRate");
	faRateSlider.addChangeListener(this);
	faRateSlider.setMajorTickSpacing(30);
	faRateSlider.setPaintTicks(true);

	//Create the label table
	Hashtable labelTable = new Hashtable();
	labelTable.put( new Integer( L_MIN ), new JLabel("0.1%") );
	labelTable.put( new Integer( (L_MIN+L_MAX)/2 ), new JLabel("   False Alarm Rate") );
	labelTable.put( new Integer( L_MAX ), new JLabel("1%") );
	faRateSlider.setLabelTable( labelTable );

	faRateSlider.setPaintLabels(true);
	panel.add(faRateSlider);
	// window size slider
	JSlider wdSzSlider = new JSlider(JSlider.HORIZONTAL, L_MIN, L_MAX, WDL_INIT);
	wdSzSlider.setName("wdSz");
	wdSzSlider.addChangeListener(this);
	wdSzSlider.setMajorTickSpacing(30);
	wdSzSlider.setPaintTicks(true);

	//Create the label table
	Hashtable wdTable = new Hashtable();
	wdTable.put( new Integer( L_MIN ), new JLabel("20") );
	wdTable.put( new Integer( (L_MIN+L_MAX)/2 ), new JLabel("   Window Size") );
	wdTable.put( new Integer( L_MAX ), new JLabel("200") );
	wdSzSlider.setLabelTable( wdTable );

	wdSzSlider.setPaintLabels(true);
	panel.add(wdSzSlider);
	if (AtomUtils.PCAType.pcaTrack==this.pcaType){ // new: tracking delta bar
		JSlider deltaSlider = new JSlider(JSlider.HORIZONTAL, L_MIN, L_MAX, L_INIT);
		deltaSlider.setName("delta");
		deltaSlider.addChangeListener(this);
		deltaSlider.setMajorTickSpacing(30);
		deltaSlider.setPaintTicks(true);
		
		//Create the label table
		labelTable = new Hashtable();
		labelTable.put( new Integer( L_MIN ), new JLabel("0%") );
		labelTable.put( new Integer( (L_MIN+L_MAX)/2 ), new JLabel("   Tracking Threshold") );
		labelTable.put( new Integer( L_MAX ), new JLabel("20%") );
		deltaSlider.setLabelTable( labelTable );
		
		deltaSlider.setPaintLabels(true);
		panel.add(deltaSlider);
		
		// tracking saving label
		trackSaveLabel = new JLabel();
		trackSaveLabel.setText("Saved/Total:0/0");
		panel.add(trackSaveLabel);
	}
	if (AtomUtils.PCAType.pcaTrackAdjust==this.pcaType) {
		JSlider devSlider = new JSlider(JSlider.HORIZONTAL, L_MIN, L_MAX, L_INIT);
		devSlider.setName("devbnd");
		devSlider.addChangeListener(this);
		devSlider.setMajorTickSpacing(30);
		devSlider.setPaintTicks(true);
		
		//Create the label table
		labelTable = new Hashtable();
		labelTable.put( new Integer( L_MIN ), new JLabel("0%") );
		labelTable.put( new Integer( (L_MIN+L_MAX)/2 ), new JLabel("   Deviation Bound") );
		labelTable.put( new Integer( L_MAX ), new JLabel("1%") );
		devSlider.setLabelTable( labelTable );
		
		devSlider.setPaintLabels(true);
		panel.add(devSlider);
		
		// tracking saving label
		trackSaveLabel = new JLabel();
		trackSaveLabel.setText("Saved/Total:0/0");
		panel.add(trackSaveLabel);		
	}
	add(panel, BorderLayout.SOUTH);
	
    }
    /** Listen to the slider. */
    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider)e.getSource();
        if ("faRate".equals(source.getName())) {
	        if (!source.getValueIsAdjusting()) {
	            double L = (double)source.getValue();
		    falseAlarmRate=0.001*L; // L is 1-10, map it to 0.001-0.01
	        }
        }
        else if ("wdSz".equals(source.getName())) { //"window size"
        	if (!source.getValueIsAdjusting()) {
	            double L = (double)source.getValue();
		    wdSz=20*(int)(L); // L is 1-10, map it to 40-400
	        }
        }
        else if ("delta".equals(source.getName())) { //"delta"
        	if (!source.getValueIsAdjusting()) {
	            double L = (double)source.getValue();
		    percent=0.02*(L-1); // L is 1-10, map it to 0-0.2
	        }
        }
        else if ("devbnd".equals(source.getName())) { //"deviation bound"
        	if (!source.getValueIsAdjusting()) {
	            double L = (double)source.getValue();
		    devBnd=0.001*(L-1)/9; // L is 1-10, map it to 0-0.01
	        }
        }
    }
    private void initJFreeChart(){
        this.numOfStream=numOfStream;

        datasets = new TimeSeriesCollection();
        trend = new TimeSeriesCollection();
        projpcs = new TimeSeriesCollection();
        spe= new TimeSeriesCollection();

        for(int i=0; i<MaxNumOfStream;i++) {
            //add streams
            TimeSeries timeseries = new TimeSeries("Stream " + i, Millisecond.class);
            datasets.addSeries(timeseries);
            timeseries.setHistoryCount(historyRange);
            //add trend variables
            TimeSeries trendSeries=new TimeSeries("Trend "+ i, Millisecond.class);
            trend.addSeries(trendSeries);
            trendSeries.setHistoryCount(historyRange);
            //add proj onto PCs variables
            TimeSeries PC=new TimeSeries("Projpcs "+ i, Millisecond.class);
            projpcs.addSeries(PC);
            PC.setHistoryCount(historyRange);
            //add spe streams
            TimeSeries speSeries = new TimeSeries("Spe " + i, Millisecond.class);
            spe.addSeries(speSeries);
            speSeries.setHistoryCount(historyRange);
        }
        combineddomainxyplot = new CombinedDomainXYPlot(new DateAxis("Time"));
        //data stream  plot
        DateAxis domain = new DateAxis("Time");
        NumberAxis range = new NumberAxis("Streams");
        domain.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        range.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        domain.setLabelFont(new Font("SansSerif", Font.PLAIN, 14));
        range.setLabelFont(new Font("SansSerif", Font.PLAIN, 14));

        XYItemRenderer renderer = new DefaultXYItemRenderer();
        renderer.setItemLabelsVisible(false);
        renderer.setStroke(
        new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)
        );
        XYPlot plot = new XYPlot(datasets, domain, range, renderer);
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        domain.setAutoRange(true);
        domain.setLowerMargin(0.0);
        domain.setUpperMargin(0.0);
        domain.setTickLabelsVisible(true);

        range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        combineddomainxyplot.add(plot);

        //Trend captured by pca
        DateAxis domain0 = new DateAxis("Time");
        NumberAxis range0 = new NumberAxis("PCA Trend");
        domain0.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        range0.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        domain0.setLabelFont(new Font("SansSerif", Font.PLAIN, 14));
        range0.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));

        XYItemRenderer renderer0 = new DefaultXYItemRenderer();
        renderer0.setItemLabelsVisible(false);
        renderer0.setStroke(
        new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)
        );
        renderer0.setSeriesPaint(0, Color.blue);
        renderer0.setSeriesPaint(1, Color.red);
        renderer0.setSeriesPaint(2, Color.magenta);
        XYPlot plot0 = new XYPlot(trend, domain0, range0, renderer0);
        plot0.setBackgroundPaint(Color.lightGray);
        plot0.setDomainGridlinePaint(Color.white);
        plot0.setRangeGridlinePaint(Color.white);
        plot0.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        domain0.setAutoRange(true);
        domain0.setLowerMargin(0.0);
        domain0.setUpperMargin(0.0);
        domain0.setTickLabelsVisible(false);

        range0.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        combineddomainxyplot.add(plot0);

        //proj on PCs plot
        DateAxis domain1 = new DateAxis("Time");
        NumberAxis range1 = new NumberAxis("Proj on PCs");
        domain1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        range1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        domain1.setLabelFont(new Font("SansSerif", Font.PLAIN, 14));
        range1.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));

        XYItemRenderer renderer1 = new DefaultXYItemRenderer();
        renderer1.setItemLabelsVisible(false);
        renderer1.setStroke(
        new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)
        );
        renderer1.setSeriesPaint(0, Color.blue);
        renderer1.setSeriesPaint(1, Color.red);
        renderer1.setSeriesPaint(2, Color.magenta);
        plot1 = new XYPlot(projpcs, domain1, range1, renderer1);
        plot1.setBackgroundPaint(Color.lightGray);
        plot1.setDomainGridlinePaint(Color.white);
        plot1.setRangeGridlinePaint(Color.white);
        plot1.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        domain1.setAutoRange(true);
        domain1.setLowerMargin(0.0);
        domain1.setUpperMargin(0.0);
        domain1.setTickLabelsVisible(false);

        range1.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        combineddomainxyplot.add(plot1);

        //spe  plot
        DateAxis domain2 = new DateAxis("Time");
        NumberAxis range2 = new NumberAxis("SPE");
        domain2.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        range2.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        domain2.setLabelFont(new Font("SansSerif", Font.PLAIN, 14));
        range2.setLabelFont(new Font("SansSerif", Font.PLAIN, 14));

        XYItemRenderer renderer2 = new DefaultXYItemRenderer();
        renderer2.setItemLabelsVisible(false);
        renderer2.setStroke(
        new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL)
        );
        XYPlot plot2 = new XYPlot(spe, domain2, range2, renderer);
        plot2.setBackgroundPaint(Color.lightGray);
        plot2.setDomainGridlinePaint(Color.white);
        plot2.setRangeGridlinePaint(Color.white);
        plot2.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        domain2.setAutoRange(true);
        domain2.setLowerMargin(0.0);
        domain2.setUpperMargin(0.0);
        domain2.setTickLabelsVisible(true);

        range2.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        combineddomainxyplot.add(plot2);

        ValueAxis axis = plot.getDomainAxis();
        axis.setAutoRange(true);
        axis.setFixedAutoRange(historyRange);  // 60 seconds
        JFreeChart chart;
        if (this.pcaType==AtomUtils.PCAType.pca)
        	chart = new JFreeChart("CloudWatch-ATOM with Exact Data", 
        				new Font("SansSerif", Font.BOLD, 18), combineddomainxyplot, false);
        else if (this.pcaType==AtomUtils.PCAType.pcaTrack)
        	chart = new JFreeChart("CloudWatch-ATOM with Fixed Tracking Threshold", 
    				new Font("SansSerif", Font.BOLD, 18), combineddomainxyplot, false);
        else
        	chart = new JFreeChart("CloudWatch-ATOM with Dynamic Tracking Threshold", 
    				new Font("SansSerif", Font.BOLD, 18), combineddomainxyplot, false);
        chart.setBackgroundPaint(Color.white);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(4, 4, 4, 4),
        BorderFactory.createLineBorder(Color.black))
        );
        AtomPanel.this.add(chartPanel, BorderLayout.CENTER);
    }
    /**
     * Adds an observation to the 'data stream' time series.
     *
     * @param in  the input array
     * @param ts  the time series collection for inputting
     */
    private void addStreamValues(ArrayList in, TimeSeriesCollection ts) {
        for(int i=0;i<in.size();i++){
            double newValue = ((Double) in.get(i)).doubleValue();
            Millisecond millisecond = new Millisecond();

            ts.getSeries(i).add(new Millisecond(), newValue);
        }
        //for debugging purpose
        if(in.size()<numOfStream){
            for(int i=in.size();i<numOfStream;i++){
                Millisecond millisecond = new Millisecond();
                ts.getSeries(i).add(new Millisecond(), 0);
            }
        }
    }
    /**
     * The data generator.
     */
    class DataGenerator extends Timer implements ActionListener{
        private BufferedReader input;
        private ArrayList inputData;
        /**
         * Constructor from file input
         *
         * @param interval  the interval (in milliseconds)
         * @param fileName the input file name
         */
        DataGenerator(int interval, String fileName) {
            super(interval, null);
            addActionListener(this);
            try{
                this.input = new BufferedReader(new FileReader(fileName));
            }catch(Exception e){
                System.err.println("Open file: "+fileName+":exception:"+e);
                System.exit(1);
            }
            String cline=null;
            try{
                cline=input.readLine();
            }catch(Exception e){
                System.err.println("File IOexception:"+e);
                System.exit(1);
            }
            StringTokenizer parser=new StringTokenizer(cline);
            numOfStream=parser.countTokens();
            for(int i=0; i<numOfStream;i++) {
                //add streams
                TimeSeries timeseries = new TimeSeries("Stream " + i, Millisecond.class);
                datasets.addSeries(timeseries);
                timeseries.setHistoryCount(historyRange);
                //add noise variables
                TimeSeries PC=new TimeSeries("Projpcs "+ i, Millisecond.class);
                projpcs.addSeries(PC);
            }
        }
        /**
         * Constructor from file input
         *
         * @param interval  the interval (in milliseconds)
         * @param file the input file
         */
        DataGenerator(int interval, File file) {
            super(interval, null);
            addActionListener(this);
        	if (file==null) return;
            try{
                input = new BufferedReader(new FileReader(file));
            }catch(Exception e){
                System.err.println("Open file: "+file+":exception:"+e);
                System.exit(1);
            }
            String cline=null;
            try{
                cline=input.readLine();
            }catch(Exception e){
                System.err.println("File IOexception:"+e);
                System.exit(1);
            }
            StringTokenizer parser=new StringTokenizer(cline);
            numOfStream=parser.countTokens();
        }

        /**
         * Adds a new reading to the datasets.
         *
         * @param event  the action event.
         */
        public void actionPerformed(ActionEvent event) {
            ArrayList data;

            if(startFlag) {
                data=getData();
                if (data==null) return;
                ArrayList retData = atom.runPCA(data, wdSz, 1-falseAlarmRate, numOfStream, 
                		pcaType, percent, devBnd); // last param is percent, ignore when simple pca
        		if (pcaType != AtomUtils.PCAType.pca)
	                trackSaveLabel.setText("Saved/Total:"
	        				+String.valueOf(atom.getSaveCnt())+"/"+String.valueOf(atom.getTotalCnt()));
                addStreamValues(retData, datasets); // simple pca return ori data, tracked pca return tracked data
                ArrayList<Double> thisTrend = atom.getTrend(pcaType);
                if (thisTrend==null) return;
                addStreamValues(thisTrend, trend);
                ArrayList<Double> projPCs = atom.getProjPCs(pcaType);
                if (projPCs==null) return;
                addStreamValues(projPCs, projpcs);
                ArrayList<Double> thisSPE = atom.getSPE(pcaType);
                if (thisSPE==null) return;
                if (thisSPE.size()>=2)
//	                System.out.println("Window size: " + wdSz+ 
//	                		", spe: "+thisSPE.get(0)+
//	                		", threshold: "+thisSPE.get(1)+
//	                		", total cnt: "+ atom.getTotalCnt());
                addStreamValues(thisSPE, spe);
		
            }
        }

        /**
         * get data readings from input file
         *
         * @param event  the action event.
         */
        public ArrayList getData() {
            String cline=null;
            try{
                cline=input.readLine();
            }catch(Exception e){
                System.err.println("File IOexception:"+e);
                System.exit(1);
            }
            if (cline==null) return null;
            StringTokenizer parser=new StringTokenizer(cline);
            numOfStream=parser.countTokens();
            ArrayList data=new ArrayList(parser.countTokens());
            while(parser.hasMoreElements()){
                String cStr=(String)parser.nextToken();
                data.add(Double.valueOf(cStr));
            }
            return data;
        }
    }

    /**
     * Entry point for the sample application.
     *
     * @param args  ignored.
     */
    
    public void actionPerformed(ActionEvent e) {
/*    	
        if(e.getSource()==startButton){
            startFlag=true;
        }
        else if(e.getSource()==pauseButton){
            startFlag=false;
        }
        else if(e.getSource()==stopButton){
            startFlag=false;
            dgen.stop();
        }
*/		
    }
    

    public void setFlag(int label) { // action of start-stop button on AtomDemoFrame
        if(label==1){
            startFlag=true;
        }
        else if(label==2){
            startFlag=false;
        }
        else if(label==3){
            startFlag=false;
            dgen.stop();
        }
    }
    
    private void jbInit() throws Exception {
    }
}
