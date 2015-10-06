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

// transfer from false alarm deviation bound \miu to eigen-error \epsilon, then from eigen-error to monitor slacks \delta_i
// Following paper: Communication-Efficient Online Detection of Network-Wide Anomalies
public class FalseAlarmDevToTrackBound {
    // initialize beforehand
    private double errPre=0.01; // eigen error precision errPre
    private double c_alpha = 2.88; // correspond to 0.95053 in the standard normal distribution table.
    private double miu = 0.01; // false alarm deviation bound

    // set in PCAHelper
    private int numInstances = 0;
    private int numMetrics = 0;    
    private double SPE; // the current SPE to compute X, filled in by PCAHelper
    private ArrayList<Double> lamda = new ArrayList<Double>(); // each eigen value, only in residual subspace
    private ArrayList<Double> STD = new ArrayList<Double>(); // standard deviation on each dimension

    // intermediate results
    private double lamdaAve; // average of all lamda values
    private double epsilon; // eigen-error epsilon

    // final results
    private ArrayList<Double> delta = new ArrayList<Double>(); // monitor slacks \delta_i
    private ArrayList<Double> DeltaToNC = new ArrayList<Double>(); // each lamda * std + mean: is the actual \Delta needed sent to NC
    
    public FalseAlarmDevToTrackBound(double cAlpha, double miu, int numInsts, int numMetrics, double SPE, 
    		ArrayList<Double> lamda, double [] std) {
    	this.c_alpha = cAlpha;
    	this.miu = miu;
        this.numInstances = numInsts;
        this.numMetrics = numMetrics;
        this.SPE = SPE;

        this.lamda.clear();
        this.lamdaAve = 0;
        for(int i=0; i<lamda.size(); i++) {
            this.lamda.add(lamda.get(i));
            this.lamdaAve += lamda.get(i);
        }
        this.lamdaAve = this.lamdaAve / lamda.size();

        this.STD.clear();
        for(int i=0; i<std.length; i++) {
            this.STD.add(std[i]);
        }
    }

    public FalseAlarmDevToTrackBound(int nAttrs) {
    	for (int i=0; i<nAttrs; i++) {
            DeltaToNC.add(0.0);
    	}
    	this.numMetrics = nAttrs;
    }
            
    double FalseAlarmToEigenError() { // require: false alarm deviation bound miu, eigen error precision errPre
        double epsilon_l = 0.0;
        double epsilon_u = lamdaAve; // average of lamda?  epsilon_l -> epsilon_u: search range for epsilon
        double tempEpsilon = 0.0;
        while((epsilon_u - epsilon_l) > errPre * epsilon_l) {
            tempEpsilon = 0.5 * (epsilon_l + epsilon_u);
            double eta_X = MonteCarloSampling(tempEpsilon);
            double miu_star = Gaussian.Phi(c_alpha) - Gaussian.Phi(c_alpha-eta_X);   // Pr[c_alpha-eta_X < N(0,1) < c_alpha]
            if(miu_star > miu) {
                epsilon_u = tempEpsilon;
            }
            else {
                epsilon_l = tempEpsilon;
            }
        }
        epsilon = tempEpsilon;
        return tempEpsilon;
    }
    
    double MonteCarloSampling(double tempEpsilon) {
        // compute X first, no need to repeat this for 1000 times
        double phi_1=0, phi_2 =0, phi_3 = 0;
        for(int i=0; i< lamda.size(); i++) {
            phi_1 += lamda.get(i);
            phi_2 += lamda.get(i) * lamda.get(i);
            phi_3 += lamda.get(i) * lamda.get(i) * lamda.get(i);
        }
        double h0 = 1 - 2*phi_1*phi_3 / (3*phi_2*phi_2);
        double para1 = Math.pow(SPE / phi_1, h0);
        double para2 = ( para1 - 1 - phi_2 * h0 * (h0-1) ) / (phi_1*phi_1);
        double X = (phi_1 * para2) / (Math.sqrt( 2 * phi_2 * h0 * h0));
        
        double eta_X = -1;
        for(int loop = 0; loop< 1000; loop++) { // loop 1000 times to get the maximum
            double phi_1_hat = 0, phi_2_hat = 0, phi_3_hat = 0;
            for(int i=0; i< lamda.size(); i++) {
                // generate ramdom sample from [lamda-tempEpsilon, lamda+tempEpsilon]
                double randomLamda = lamda.get(i) + (Math.random() - 0.5) * 2 * tempEpsilon; // Math.random() generates a number in [0.0, 1)
                phi_1_hat += randomLamda;
                phi_2_hat += randomLamda * randomLamda;
                phi_3_hat += randomLamda * randomLamda * randomLamda;
            }
            double h0_hat = 1 - 2*phi_1_hat * phi_3_hat / (3 * phi_2_hat * phi_2_hat);
            double para1_hat = Math.pow(SPE / phi_1_hat, h0_hat);
            double para2_hat = ( para1_hat - 1 - phi_2_hat * h0_hat * (h0_hat-1) ) / (phi_1_hat*phi_1_hat);
            double X_hat = (phi_1_hat * para2_hat) / (Math.sqrt( 2 * phi_2_hat * h0_hat * h0_hat));
            double diff = Math.abs(X - X_hat);
            if(eta_X < diff) {
                eta_X = diff;
            }
        }
        return eta_X;
    }

    void getHomoUniDelta() {
        int m = numInstances;
        int n = numMetrics; // in case of confusion later
        assert(m != 0);
        assert(n != 0);
        double para1 = 3 * epsilon * Math.sqrt(m*m + m*n);
        double para2 = 3 * lamdaAve * n + para1;
        double delta_i = ( Math.sqrt(para2) - Math.sqrt(3*lamdaAve*n) ) / (Math.sqrt(m+n));

        delta.clear();
        for(int i=0; i<numMetrics; i++) {
            delta.add(delta_i);
        }
    }
    
    void getDeltaToNC() {
        ArrayList<Double> new_DeltaToNC = new ArrayList<Double>();
        for(int i=0; i<numMetrics; i++) {
            double toNC = delta.get(i) * STD.get(i);
            if((DeltaToNC.size()>i) && Math.abs(toNC - DeltaToNC.get(i)) > 0.000001) {
                new_DeltaToNC.add(toNC);
            }
            else new_DeltaToNC.add(-1.0);
        }
        DeltaToNC = new_DeltaToNC;
    }
    
    ArrayList<Double> getDelta(double cAlpha, double miu, int numInsts, double SPE, 
    		ArrayList<Double> lamda, double[] std) {  // main function to run after pre-setup
    	this.c_alpha = cAlpha;
    	this.miu = miu;
        this.numInstances = numInsts;
        this.SPE = SPE;

        this.lamda.clear();
        this.lamdaAve = 0;
        for(int i=0; i<lamda.size(); i++) {
            this.lamda.add(lamda.get(i));
            this.lamdaAve += lamda.get(i);
        }
        this.lamdaAve = this.lamdaAve / lamda.size();

        this.STD.clear();
        for(int i=0; i<std.length; i++) {
            this.STD.add(std[i]);
        }

        FalseAlarmToEigenError();
        getHomoUniDelta();
        getDeltaToNC();
        return DeltaToNC;
    }

}
