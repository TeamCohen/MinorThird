package iitb.Model;

public class EdgeSelectorBasedOnChange extends EdgeSelector {
	public EdgeSelectorBasedOnChange(FeatureGenImpl fgen, int width,
			String patternFile, int histSize, int minsize) {
		super(fgen, width, patternFile, histSize, minsize);
	}

	public EdgeSelectorBasedOnChange(FeatureGenImpl fgen, String patternFile) {
		super(fgen, patternFile);
	}

	public EdgeSelectorBasedOnChange(FeatureGenImpl fgen, String patternFile,
			int histSize) {
		super(fgen, patternFile, histSize);
	}

	public EdgeSelectorBasedOnChange(FeatureGenImpl fgen) {
		super(fgen);
	}
	@Override
	public String name() {
		return "EdgeSelChange";
	}
	@Override
	public void next(FeatureImpl f) {
		super.next(f);
		f.val = 1;
	}

	protected boolean advance() {
		while(++index < (patternOccurence.length) && (patternOccurence[index] <= 0 || patternOccurence[index] == segLen));
        return index < patternOccurence.length;
	}
}
