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

public class Tracker {
    int m_nDims;

    private class trackerType {
        double delta;
        double boundHigh;
        double boundLow;
        double lastTracked;
    }

    // store original data matrix
    private ArrayList<ArrayList<Double>> m_observedMatrix = new ArrayList<ArrayList<Double>>();
    // store tracked data matrix
    private ArrayList<ArrayList<Double>> m_trackedMatrix = new ArrayList<ArrayList<Double>>();
    private trackerType[] m_dimsTracker = new trackerType[0];

    public Tracker(ArrayList<ArrayList<Double>> observedMatrix) {
        this.m_nDims = observedMatrix.size();
        this.m_observedMatrix = observedMatrix;
        for (int i = 0; i < this.m_nDims; i++) {
            this.m_trackedMatrix.add(new ArrayList<Double>());
        }

        this.m_dimsTracker = new trackerType[this.m_nDims];
        for (int i = 0; i < this.m_nDims; i++) {
            this.m_dimsTracker[i] = new trackerType();
            this.m_dimsTracker[i].delta = 0;
            this.m_dimsTracker[i].boundHigh = Double.MAX_VALUE;
            this.m_dimsTracker[i].boundLow = -Double.MAX_VALUE;
            this.m_dimsTracker[i].lastTracked = Double.MAX_VALUE;
        }
    }

    public Tracker(int nAttrs) {
    	this.m_nDims = nAttrs;
    	for (int i=0; i<nAttrs; i++) 
    		this.m_observedMatrix.add(new ArrayList<Double>());
        this.m_dimsTracker = new trackerType[this.m_nDims];
        for (int i = 0; i < this.m_nDims; i++) {
            this.m_dimsTracker[i] = new trackerType();
            this.m_dimsTracker[i].delta = 0;
            this.m_dimsTracker[i].boundHigh = Double.MAX_VALUE;
            this.m_dimsTracker[i].boundLow = -Double.MAX_VALUE;
            this.m_dimsTracker[i].lastTracked = Double.MAX_VALUE;
        }
    }

    // deltas are previously saved ones by FalseAlarmDevToTrackBound.java
    public ArrayList<Double> runTracking(ArrayList<Double> newData, int slideWdSz) {
    	if (this.m_observedMatrix.get(0).size()>slideWdSz) { // if less than wd sz, no pca yet, use 10% of sliding window as delta; else use the delta adjusted by pca
    		int toRm = this.m_observedMatrix.get(0).size()-slideWdSz;
    		for (int i=0; i<m_nDims; i++) {
    			for (int j=0; j<toRm; j++) {
    				this.m_observedMatrix.get(i).remove(0);
    			}
    		}
    	}
    	else if (this.m_observedMatrix.get(0).size()==0) { // first data, simply add
    		AtomUtils.addNewData(newData, m_observedMatrix);
    		return newData;
    	}
    	// track now
    	ArrayList<Double> trackedData = tracking(newData);
    	AtomUtils.addNewData(newData, m_observedMatrix); // must add AFTER tracking!
    	return trackedData;
    }

    
    // use the percent * mean_of_sliding_windows as deltas, if not enough values in sliding windows, use them all
    public ArrayList<Double> runTracking(ArrayList<Double> newData, int slideWdSz, double percent) {
    	if (this.m_observedMatrix.get(0).size()==0) { // first data, simply add
    		AtomUtils.addNewData(newData, m_observedMatrix);
    		return newData;
    	}
    	setDeltas(getDeltas(percent));
    	// track now
    	ArrayList<Double> trackedData = tracking(newData);
    	AtomUtils.addNewData(newData, m_observedMatrix); // must add AFTER tracking!
    	if (this.m_observedMatrix.get(0).size()>slideWdSz) { // keep wd sz data and calc delta for next newData
    		int toRm = this.m_observedMatrix.get(0).size()-slideWdSz;
    		for (int i=0; i<m_nDims; i++) {
    			for (int j=0; j<toRm; j++) {
    				this.m_observedMatrix.get(i).remove(0);
    			}
    		}
    	}
    	return trackedData;
    }
    
    public double[] getDeltas(double percent) {
    	double[] deltas = new double[this.m_observedMatrix.size()];
    	double[] means = AtomUtils.getMean(this.m_observedMatrix);
    	for (int i=0; i<this.m_nDims; i++) {
    		deltas[i] = means[i]*percent;
    	}
    	return deltas;
    }
    
    public void setDeltas(double[] deltas) {
        for (int i = 0; i < this.m_nDims; i++) {
            this.m_dimsTracker[i].delta = deltas[i];
        }
    }
    public void setDeltas(ArrayList<Double> deltas) {
        for (int i = 0; i < this.m_nDims; i++) {
            this.m_dimsTracker[i].delta = deltas.get(i);
        }
    }
    
    public ArrayList<ArrayList<Double>> getTrackedMatrix() {
        return this.m_trackedMatrix;
    }

    public ArrayList<Double> tracking(ArrayList<Double> newData) {
        assert (m_nDims > 0);
        ArrayList<Double> trackedData = new ArrayList<Double>();
        for (int dim = 0; dim < m_nDims; dim++) {
            if (Math.abs(newData.get(dim) - m_dimsTracker[dim].lastTracked) - m_dimsTracker[dim].delta > 1e-10) {
                double thisVal = newData.get(dim);
                double thisHigh = thisVal + m_dimsTracker[dim].delta;
                double thisLow = thisVal - m_dimsTracker[dim].delta;
                double newHigh = Math.min(m_dimsTracker[dim].boundHigh, thisHigh);
                double newLow = Math.max(m_dimsTracker[dim].boundLow, thisLow);
                if (newLow - newHigh > 1e-10) { // new round
                    trackedData.add(thisVal);
                    m_dimsTracker[dim].lastTracked = thisVal;
                    m_dimsTracker[dim].boundHigh = thisHigh;
                    m_dimsTracker[dim].boundLow = thisLow;
                }
                else {
                    double newVal = (newHigh + newLow) / 2;
                    trackedData.add(newVal);
                    m_dimsTracker[dim].lastTracked = newVal;
                    m_dimsTracker[dim].boundHigh = newHigh;
                    m_dimsTracker[dim].boundLow = newLow;
                }
            }
            else { // not exceed, do not send out this data (in reality). Here just mimic, so set this value to -1 and give PCA.
            	trackedData.add(-1.0);
            }
        }
        return trackedData;
    }
}