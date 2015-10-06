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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class AtomMain {
    // store original data matrix
    private ArrayList<ArrayList<Double>> m_oriDataMatrix = new ArrayList<ArrayList<Double>>();
    private PCADetector m_pcaAgent = null;
    private PCADetector m_pcaTrackAgent = null;
    private Tracker m_tracker = null;
    private FalseAlarmDevToTrackBound m_adjuster = null;
    private PCADetector m_pcaTrackAdjustAgent = new PCADetector();
    private int m_trackSaveCnt;
    private int m_totalCnt;
    
    public ArrayList<Double> runPCA(ArrayList<Double> newData, int slideWdSz, 
    		double confLevel, int nAttrs, AtomUtils.PCAType pcaType, double percent, double devBnd) {
    	ArrayList<Double> ret = null;
    	if (m_oriDataMatrix.size()==0) {
    		for (int i=0; i<nAttrs; i++) {
    			m_oriDataMatrix.add(new ArrayList<Double>());
    		}
    	}
    	if (pcaType==AtomUtils.PCAType.pca) {
	    	if (this.m_pcaAgent==null) {
	    		m_totalCnt = 0;
	    		m_pcaAgent = new PCADetector();
	    	}
	    	double cAlpha = Gaussian.PhiInverse(confLevel);
	    	m_pcaAgent.runPCA(newData, slideWdSz, cAlpha, nAttrs);
	    	m_totalCnt += newData.size();
	    	ret = newData;
    	}
    	else if (pcaType==AtomUtils.PCAType.pcaTrack) {
    		ret = runPCAtrack(newData, slideWdSz, confLevel, nAttrs, percent);
    	}
    	else if (pcaType==AtomUtils.PCAType.pcaTrackAdjust)
    		ret = runPCAadjust(newData, slideWdSz, confLevel, devBnd, nAttrs);
    	AtomUtils.addNewData(newData, m_oriDataMatrix, slideWdSz);
    	return ret;
    }
    
    public ArrayList<Double> runPCAadjust(ArrayList<Double> newData, int slideWdSz, double confLevel, double devBnd, int nAttrs) {
    	// track data
    	if (this.m_pcaTrackAdjustAgent==null) {
    		m_pcaTrackAdjustAgent = new PCADetector();
    		m_trackSaveCnt = 0;
    		m_totalCnt = 0;
    	}
    	if (this.m_tracker==null) m_tracker = new Tracker(nAttrs);
    	if (this.m_adjuster==null) m_adjuster = new FalseAlarmDevToTrackBound(nAttrs);
    	m_totalCnt += newData.size();
    	ArrayList<Double> trackedData = newData;
    	if (m_totalCnt>=(slideWdSz+2)*nAttrs) // if not enough data for pca yet, use exact data instead
    		trackedData = m_tracker.runTracking(newData, slideWdSz);
    	m_trackSaveCnt += getTrackSaveCnt(trackedData);
    	
    	// run pca
    	double cAlpha = Gaussian.PhiInverse(confLevel);
    	m_pcaTrackAdjustAgent.runPCA(trackedData, slideWdSz, cAlpha, nAttrs); // this step will change '-1' in trackedData to last data received, which is exactly what we want to return to show
    	
    	// adjust delta
    	if (m_pcaTrackAdjustAgent.getSPE().size()==0 || m_pcaTrackAdjustAgent.getEigens().size()==0 ||
    			m_pcaTrackAdjustAgent.getDataset().size()==0) {
    		return newData;
    	}
    	ArrayList<Double> deltas = m_adjuster.getDelta(cAlpha, devBnd, slideWdSz, 
    			m_pcaTrackAdjustAgent.getSPE().get(0), 
    			m_pcaTrackAdjustAgent.getEigens(), 
    			AtomUtils.getStandardDeviation(m_oriDataMatrix));
    	m_tracker.setDeltas(deltas);
    	
    	return trackedData;
    }
    
   // run pca with tracked data
    public ArrayList<Double> runPCAtrack(ArrayList<Double> newData, int slideWdSz, double confLevel, int nAttrs, double percent) {
    	// track data
    	if (this.m_pcaTrackAgent==null) {
    		m_pcaTrackAgent = new PCADetector();
    		m_trackSaveCnt = 0;
    		m_totalCnt = 0;
    	}
    	if (this.m_tracker==null) m_tracker = new Tracker(nAttrs);
    	m_totalCnt += newData.size();
    	ArrayList<Double> trackedData = m_tracker.runTracking(newData, slideWdSz, percent);
    	m_trackSaveCnt += getTrackSaveCnt(trackedData);
    	// run pca
    	double cAlpha = Gaussian.PhiInverse(confLevel);
    	m_pcaTrackAgent.runPCA(trackedData, slideWdSz, cAlpha, nAttrs); // this step will change '-1' in trackedData to last data received, which is exactly what we want to return to show
    	// return tracked data
    	return trackedData;
    }

    public int getTrackSaveCnt(ArrayList<Double> data) {
    	int ret=0;
    	for(int i=0; i<data.size(); i++) {
    		if (data.get(i)<0) ret += 1;
    	}
    	return ret;
    }

    public int getTotalCnt() {
    	return m_totalCnt;
    }
    public int getSaveCnt() {
    	return m_trackSaveCnt;
    }
    
    public ArrayList<Double> getSPE(AtomUtils.PCAType ptype) {
    	if (ptype==AtomUtils.PCAType.pca)
    		return this.m_pcaAgent.getSPE();
    	else if (ptype==AtomUtils.PCAType.pcaTrack)
    		return this.m_pcaTrackAgent.getSPE();
    	else if (ptype==AtomUtils.PCAType.pcaTrackAdjust)
    		return this.m_pcaTrackAdjustAgent.getSPE();
    	else assert(false);
    	return null;
    }

    public ArrayList<Double> getProjPCs(AtomUtils.PCAType ptype) {
    	if (ptype==AtomUtils.PCAType.pca)
    		return this.m_pcaAgent.getProjPCs();
    	else if (ptype==AtomUtils.PCAType.pcaTrack)
    		return this.m_pcaTrackAgent.getProjPCs();
    	else if (ptype==AtomUtils.PCAType.pcaTrackAdjust)
    		return this.m_pcaTrackAdjustAgent.getProjPCs();
    	else assert(false);
    	return null;
    }

    public ArrayList<Double> getTrend(AtomUtils.PCAType ptype) {
    	if (ptype==AtomUtils.PCAType.pca)
    		return this.m_pcaAgent.getPCRec();
    	else if (ptype==AtomUtils.PCAType.pcaTrack)
    		return this.m_pcaTrackAgent.getPCRec();
    	else if (ptype==AtomUtils.PCAType.pcaTrackAdjust)
    		return this.m_pcaTrackAdjustAgent.getPCRec();
    	else assert(false);
    	return null;
    }
}
