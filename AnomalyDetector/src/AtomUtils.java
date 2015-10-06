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

import weka.core.Utils;

public class AtomUtils {

	public AtomUtils() {
		// TODO Auto-generated constructor stub
	}

	public enum PCAType {
		pca, pcaTrack, pcaTrackAdjust
	}

	// get mean for each ArrayList<Double>
	static public double[] getMean(ArrayList<ArrayList<Double>> lists) {
		int numAtts = lists.size();
		assert(numAtts>0);
		int numInsts = lists.get(0).size();
		assert(numInsts>0);
		double[] Means = new double[numAtts]; // default values are 0s, so return 0 if no elements inside
		for(int i=0; i<numAtts; i++) {
			double att1 = 0;
			for(int j=0; j<numInsts; j++) {
				att1 += lists.get(i).get(j);
			}
			Means[i] = att1 / numInsts;
		}
		return Means;		
	}
	
	// get std for each ArrayList<Double>
	static public double[] getStandardDeviation(ArrayList<ArrayList<Double>> lists) {
		int numAtts = lists.size();
		assert(numAtts>0);
		int numInsts = lists.get(0).size();
		assert(numInsts>0);
		double[] att1 = new double[numInsts];
		double[] std = new double[numAtts];
		for(int i=0; i<numAtts; i++) {
			for(int j=0; j<numInsts; j++) {
				att1[j] = lists.get(i).get(j);
			}
			std[i] = Math.sqrt(Utils.variance(att1));
		}
		return std;
	}
	
	// add newData into data matrix
	static public void addNewData(ArrayList<Double> newData, 
			ArrayList<ArrayList<Double>> lists, int wdSz) {
		for (int i=0; i<lists.size(); i++) {
			lists.get(i).add(newData.get(i));
		}
    	int toRm = lists.get(0).size()-wdSz;
    	for (int i=0; i<lists.size(); i++) {
    		for (int j=0; j<toRm; j++)
    			lists.get(i).remove(8); // remove toRm values for each list, first 7 ones are for stable threshold and spe
    	}
	}
	// add newData into data matrix
	static public void addNewData(ArrayList<Double> newData, 
			ArrayList<ArrayList<Double>> lists) {
		for (int i=0; i<lists.size(); i++) {
			lists.get(i).add(newData.get(i));
		}
	}
}
