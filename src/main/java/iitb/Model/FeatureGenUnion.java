/*
 * Created on Mar 7, 2007
 * @author sunita
 */
package iitb.Model;

import java.util.Vector;
import iitb.CRF.DataSequence;
import iitb.CRF.Feature;
import iitb.CRF.FeatureGenerator;
import iitb.CRF.FeatureGeneratorNested;

public class FeatureGenUnion extends  Vector<FeatureGeneratorNested>  implements FeatureGeneratorNested {
    int currentId=-1;
    FeatureGenerator currentFGen;
    int currentFeatureOffset;
    FeatureImpl savedFeature = new FeatureImpl();
    public String featureName(int featureIndex) {
        int numF=0;
        for (FeatureGenerator fgen : this) {
            if (featureIndex < numF+fgen.numFeatures()) {
                return fgen.featureName(featureIndex-numF);
            }
            numF += fgen.numFeatures();
        }
        return null;
    }
    public boolean hasNext() {
        return currentFGen.hasNext();
    }
    
    private void advance() {
        if (currentFGen.hasNext()) return;
        for (currentId++;currentId < size();currentId++) {
            currentFGen = get(currentId);
            currentFeatureOffset += get(currentId-1).numFeatures();
            if (currentFGen.hasNext()) return;
        }
    }
    public Feature next() {
        Feature f = currentFGen.next();
        savedFeature.copy(f);
        savedFeature.id += currentFeatureOffset;
        advance();
        return savedFeature;
    }

    public int numFeatures() {
        int numF=0;
        for (FeatureGenerator fgen : this) {
            numF += fgen.numFeatures();
        }
        return numF;
    }

    public void startScanFeaturesAt(DataSequence data, int pos) {
        startScanFeaturesAt(data, pos-1,pos);
    }
    public void startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        for (int i = 0; i < size(); i++) {
            get(i).startScanFeaturesAt(data,prevPos, pos);
        }
        currentId = 0;
        currentFeatureOffset=0;
        currentFGen = get(currentId);
        advance();
    }
    public int maxMemory() {
        return get(0).maxMemory();
    }
}
