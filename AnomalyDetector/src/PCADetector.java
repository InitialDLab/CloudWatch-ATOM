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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import java.math.*;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.PrincipalComponents;
import weka.attributeSelection.Ranker;

public class PCADetector {
    // store original data matrix
    private ArrayList<ArrayList<Double>> m_oriDataMatrix = new ArrayList<ArrayList<Double>>();
    // private ArrayList<String> TimeStamps = new ArrayList<String>();
    private double c_alpha;
    private int m_nDims;
    private Instances m_scaledInstances;
    private ArrayList<Integer> m_normalPts = new ArrayList<Integer>();
    private ArrayList<Integer> m_abnormalPts = new ArrayList<Integer>();
    private HashMap<Integer, ArrayList<Integer>> m_susDims = new HashMap<Integer, ArrayList<Integer>>(); // suspicious dimensions for each abnormal point
    private ArrayList<Double> m_SPE = new ArrayList<Double>(); // squared projection error onto residual space plus threshold
//    private double m_SPE; 
    private double m_threshold;
    private ArrayList<Double> m_projPCs = new ArrayList<Double>(); // squared projection onto principal space
    private ArrayList<Double> m_pcRec = new ArrayList<Double>(); // sum of reconstruction of PCs
    private ArrayList<Double> m_eigens = new ArrayList<Double>(); // eigen values of each dim
    
    public PCADetector() {
    }

    public PCADetector(int nAttrs) {
    	this.m_nDims = nAttrs;
    	for (int i=0; i<this.m_nDims; i++) {
    		m_oriDataMatrix.add(new ArrayList<Double>()); // one list for each attribute
    	}
    }

    public PCADetector(ArrayList<ArrayList<Double>> oriDataMatrix, double cAlpha) {
        this.m_oriDataMatrix = oriDataMatrix;
        this.m_nDims = oriDataMatrix.size();
        assert (m_nDims > 0);
        this.c_alpha = cAlpha;
    }

    public int[] Sort(double[] eigenValues, double[][] eigenVectors) {
        int numDims = eigenValues.length;
        int numRows = eigenVectors.length;
        int[] order = Utils.sort(eigenValues);
        double[] sortedEigenValues = new double[numDims];
        double[][] sortedEigenVectors = new double[eigenVectors.length][numDims];
        for (int i = 0; i < numDims; i++) {
            sortedEigenValues[i] = eigenValues[order[numDims - 1 - i]];
            for (int j = 0; j < numRows; j++) {
                sortedEigenVectors[j][i] = eigenVectors[j][order[numDims - 1 - i]];
            }
        }
        for (int i = 0; i < numDims; i++) {
            eigenValues[i] = sortedEigenValues[i];
            for (int j = 0; j < numRows; j++) {
                eigenVectors[j][i] = sortedEigenVectors[j][i];
            }
        }
        return order;
    }

    public double computeThreshold(double[] sortedEigenValues, int startDim) {
        double phi_1 = 0.0;
        double phi_2 = 0.0;
        double phi_3 = 0.0;
        ArrayList<Double> varList = new ArrayList<Double>();

        for (int i = startDim; i < sortedEigenValues.length; i++) {
            double lamda = sortedEigenValues[i];
            phi_1 += lamda;
            phi_2 += lamda * lamda;
            phi_3 += lamda * lamda * lamda;
            varList.add(lamda);
        }

        double h_0 = 1 - 2 * phi_1 * phi_3 / (3 * phi_2 * phi_2);
        double h_0_reci = 1 / h_0;
        double first_fraction = c_alpha * Math.sqrt(2 * phi_2 * h_0 * h_0) / phi_1;
        double second_fraction = phi_2 * h_0 * (h_0 - 1) / (phi_1 * phi_1);
        double base = first_fraction + 1 + second_fraction;
        double delta_alpha_square = phi_1 * Math.pow(base, h_0_reci);

        return delta_alpha_square;
    }

    public Instances getInstances() {
        int numAtts = m_oriDataMatrix.size();
        if (numAtts < 0)
            return null;
        ArrayList<Attribute> atts = new ArrayList<Attribute>(numAtts);
        for (int att = 0; att < numAtts; att++) {
            atts.add(new Attribute(Integer.toString(att), att));
        }
        int numInstances = m_oriDataMatrix.get(0).size();
        if (numInstances <= 0)
            return null;
        Instances dataset = new Instances("MetricInstances", atts, numInstances);
        for (int inst = 0; inst < numInstances; inst++) {
            Instance newInst = new DenseInstance(numAtts);
            for (int att = 0; att < numAtts; att++) {
                newInst.setValue(att, m_oriDataMatrix.get(att).get(inst));
            }
            dataset.add(newInst);
        }
        return dataset;
    }

    public double[] getScaled(ArrayList<Double> newData) {
    	double[] scaled = new double[this.m_nDims];
    	double[] means = AtomUtils.getMean(m_oriDataMatrix);
    	double[] stds = AtomUtils.getStandardDeviation(m_oriDataMatrix);
    	for (int i=0; i<this.m_nDims; i++) {
    		scaled[i] = (newData.get(i)-means[i])/stds[i];
    	}
    	return scaled;
    }
    
    // compute the squared projection onto PCs, only for comparison purpose in demo
    public double computeProjPCs(double[][] sortedEigenVectors, int residualStartDimension, ArrayList<Double> newData) { 
        double[] thisSample = getScaled(newData);
        double[] transP_newSample = new double[residualStartDimension];
    	double[] Y = new double[m_nDims];

        // trans(sortedEigenVectors) * m_newestSample
        for (int i = 0; i < residualStartDimension; i++) {
            double temp = 0;
            for (int j = 0; j < m_nDims; j++) {
                temp += sortedEigenVectors[j][i] * thisSample[j];
            }
            transP_newSample[i] = temp;
        }
        // sortedEigenVectors * transP_newSample
        for (int i = 0; i < m_nDims; i++) {
            double temp = 0;
            for (int j = 0; j < residualStartDimension; j++) {
                temp += sortedEigenVectors[i][j] * transP_newSample[j];
            }
            Y[i] = temp;
        }
        double projPCs = 0;
        double trend = 0;
        for (int i = 0; i < Y.length; i++) {
            trend += Y[i];
           	projPCs += Y[i] * Y[i];
        }
        m_projPCs.clear(); m_projPCs.add(projPCs);
        m_pcRec.clear(); m_pcRec.add(trend);
        return projPCs;
    }
    
    public boolean checkSPE(double[][] sortedEigenVectors, int residualStartDimension, ArrayList<Double> newData) {
        boolean ret = false;

        double[] thisSample = getScaled(newData);
        // starting from residualStartDimension, use sortedEigenVectros *
        // trans(sortedEigenVectors) * m_newestSample
        int residualDims = sortedEigenVectors.length - residualStartDimension;
        double[] transP_newSample = new double[residualDims];
        double[] Y = new double[m_nDims];
        int eigenVectorNum = sortedEigenVectors[0].length;

        // trans(sortedEigenVectors) * m_newestSample
        for (int i = residualStartDimension; i < eigenVectorNum; i++) {
            double temp = 0;
            for (int j = 0; j < m_nDims; j++) {
                temp += sortedEigenVectors[j][i] * thisSample[j];
            }
            transP_newSample[i - residualStartDimension] = temp;
        }
        // sortedEigenVectors * transP_newSample
        for (int i = 0; i < m_nDims; i++) {
            double temp = 0;
            for (int j = residualStartDimension; j < eigenVectorNum; j++) {
                temp += sortedEigenVectors[i][j] * transP_newSample[j - residualStartDimension];
            }
            Y[i] = temp;
        }

        double SPE = 0;
        for (int i = 0; i < Y.length; i++) {
            SPE += Y[i] * Y[i];
        }
        m_SPE.clear();
        m_SPE.add(SPE);
        m_SPE.add(m_threshold);
        boolean bAddNew = true;
        if (SPE - m_threshold > 0.000001) { // abnormal
            ret = true;
            if (SPE > 2*m_threshold) { // do not add a "too abnormal" point into sliding window
            	bAddNew = false;
            }
        }
        if (bAddNew) { // only add normal points
        	AtomUtils.addNewData(newData, m_oriDataMatrix);
        }
        return ret;
    }

    public double[] getNormalMean() {
        int numInsts = m_normalPts.size();
        assert (numInsts > 0);
        double[] Means = new double[m_nDims];
        for (int i = 0; i < m_nDims; i++) {
            double att1 = 0;
            for (int j = 0; j < numInsts; j++) {
                att1 += m_oriDataMatrix.get(i).get(m_normalPts.get(j));
            }
            Means[i] = att1 / numInsts;
        }
        return Means;
    }

    public double[] getNormalSTD() {
        int numInsts = m_normalPts.size();
        assert (numInsts > 0);
        double[] att1 = new double[numInsts];
        double[] std = new double[m_nDims];
        for (int i = 0; i < m_nDims; i++) {
            for (int j = 0; j < numInsts; j++) {
                att1[j] = m_oriDataMatrix.get(i).get(m_normalPts.get(j));
            }
            std[i] = Math.sqrt(Utils.variance(att1));
        }
        return std;
    }
    
    public ArrayList<Double> getSPE() {
    	return this.m_SPE;
    }

    public double getThreshold() {
    	return this.m_threshold;
    }
    
    public ArrayList<Double> getProjPCs() {
    	return this.m_projPCs;
    }
    public ArrayList<Double> getPCRec() {
    	return this.m_pcRec;
    }
    
    public ArrayList<ArrayList<Double>> getDataset() {
    	return this.m_oriDataMatrix;
    }
    
    public void diagnosis(double[][] sortedEigenVectors, int residualStartDimension, ArrayList<Double> newData) {
        assert (m_abnormalPts.size() != 0);
        double[] changePct = new double[m_nDims];

        // naive way, to check the original metric change, how far the abnormal
        // points away from the previous mean
        double[] normalAvg = AtomUtils.getMean(m_oriDataMatrix);
        double[] normalStd = AtomUtils.getStandardDeviation(m_oriDataMatrix);

            ArrayList<Integer> newSusDims = new ArrayList<Integer>();
            for (int i = 0; i < m_nDims; i++) {
                if (Math.abs((m_oriDataMatrix.get(i).get(m_oriDataMatrix.get(0).size()-1)
                		- normalAvg[i])) < 0.000001)
                    changePct[i] = 0;
                else if (normalStd[i] < 0.0000001)
                    changePct[i] = Double.MAX_VALUE;
                else
                    changePct[i] = Math.abs((m_oriDataMatrix.get(i).get(m_oriDataMatrix.get(0).size()-1)
                    		- normalAvg[i]) / normalStd[i]);
                if (changePct[i] > 3) {
                	newSusDims.add(i);
                }
            }
        // Now what to do with the newSusDims ???
            //            m_susDims.put(m_abnormalPts.get(id), newSusDims);
//            System.out.println("Suspicious point id: "+ m_abnormalPts.get(id) + "; supicious dimension for this point: "+newSusDims);
//        }
        return;
    }

    // modify m_oriDataMatrix based on current sliding window size.
    // If slidewdSz becomes bigger, the first several rounds have smaller points.
    public boolean prepareData(ArrayList<Double> newData, int slidewdSz) {
    	assert(this.m_nDims>0);
    	if (m_oriDataMatrix.get(0).size() < slidewdSz) {
        	for (int i=0; i<this.m_nDims; i++) {
        		m_oriDataMatrix.get(i).add(newData.get(i));
        	}
        	return false;
    	}
    	
    	if (m_oriDataMatrix.get(0).size() < slidewdSz) return false; // not enough data for PCA
    	int toRm = m_oriDataMatrix.get(0).size()-slidewdSz;
    	for (int i=0; i<m_nDims; i++) {
    		for (int j=0; j<toRm; j++)
    			m_oriDataMatrix.get(i).remove(8); // remove toRm values for each list, first 7 ones are for stable threshold and spe
    	}
    	return true;
    }
    
    public void verifyData(ArrayList<Double> newData) {
    	for (int i=0; i<this.m_nDims; i++) {
    		if (newData.get(i)<0)  // set as the last received value
    			newData.set(i, m_oriDataMatrix.get(i).get(m_oriDataMatrix.get(0).size()-1)); 
    	}
    }
    
    public ArrayList<Double> setEigens(double[] eigens) {
    	for (int i=0; i<eigens.length; i++)
    		m_eigens.add(eigens[i]);
    	return m_eigens;
    }
    
    public ArrayList<Double> getEigens() {
    	return m_eigens;
    }
    
    public boolean runPCA(ArrayList<Double> newData, int slidewdSz, double cAlpha, int nAttrs) {
        try {
        	if (m_nDims==0) {
        		m_nDims = nAttrs;
            	for (int i=0; i<this.m_nDims; i++) {
            		m_oriDataMatrix.add(new ArrayList<Double>()); // one list for each attribute
            	}
        	}
        	verifyData(newData);
        	this.c_alpha = cAlpha;
        	if (false == prepareData(newData, slidewdSz)) return false;
            Instances oriDataInsts = getInstances();
            if (oriDataInsts != null) {
                // standardization + PCA covariance matrix
                m_scaledInstances = new Instances(oriDataInsts);
                Standardize filter = new Standardize();

                filter.setInputFormat(m_scaledInstances);
                m_scaledInstances = Standardize.useFilter(m_scaledInstances, filter); // standardization

                PrincipalComponents PCA = new PrincipalComponents();
                PCA.setVarianceCovered(1.0); // means 100%
                PCA.setMaximumAttributeNames(-1);
                PCA.setCenterData(true);
                Ranker ranker = new Ranker();
                AttributeSelection selector = new AttributeSelection();
                selector.setSearch(ranker);
                selector.setEvaluator(PCA);
                selector.SelectAttributes(m_scaledInstances);
//                Instances transformedData = selector.reduceDimensionality(m_scaledInstances);

                // get sorted eigens
                double[] eigenValues = PCA.getEigenValues();
                // eigenVectors[i][j]  i: rows; j: cols
                double[][] eigenVectors = PCA.getUnsortedEigenVectors();
                Sort(eigenValues, eigenVectors);
                setEigens(eigenValues);
                
                // get residual start dimension
                int residualStartDimension = -1;
				double sum=0; double major=0;
				for(int ss=0; ss<eigenValues.length; ss++) {
					sum += eigenValues[ss];
				}				
				for(int ss=0; ss<eigenValues.length; ss++) {
					major += eigenValues[ss];
					if((residualStartDimension<0) && (major/sum>0.95)) {
						residualStartDimension = ss+1; break;
					}
				}
//				System.out.println("residualStartDim: "+residualStartDimension);
                m_threshold = computeThreshold(eigenValues, residualStartDimension);
                
                // check new data abnormal or not
                boolean bAbnormal = checkSPE(eigenVectors, residualStartDimension, newData);
                computeProjPCs(eigenVectors, residualStartDimension, newData); // only for demo
                
				
                if (bAbnormal) { // anomaly, now to diagnosis
                    // check original space using all the lists
                    diagnosis(eigenVectors, residualStartDimension, newData);
                }

            }
            
        } catch (Exception exc) {
        }
        return true;
    }

    public double[] getStandardDeviation(Instances Matrix) {
        int numAtts = Matrix.numAttributes();
        int numInsts = Matrix.numInstances();
        double[] att1 = new double[numInsts];
        double[] std = new double[numAtts];
        for (int i = 0; i < numAtts; i++) {
            for (int j = 0; j < numInsts; j++) {
                att1[j] = Matrix.instance(j).value(i);
            }
            std[i] = Math.sqrt(Utils.variance(att1));
        }
        return std;
    }

}