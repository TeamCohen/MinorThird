package edu.cmu.minorthird.classify.transform;

/**
 * @author Vitor Carvalho
 * March 2005
 *
 * A 2-by-2 matrix indicating the association between 2 variables.
 * Useful in feature selection or feature association experiments. 
 * 
 * Contains scores for Chi-Squared, Pointwise Mutual-Information, 
 * Compensated Pointwise Mutual Info
 * 
 * Given 2 variables X and Y, the matrix is
 * [a b]    a = X and Y ;     b = X and Not Y
 * [c d]    c = Not X and Y;  d = Not X and Not Y
 * 
 *  */
public class ContingencyTable {
    private long a, b, c, d;
    
    public ContingencyTable(long a, long b, long c, long d){
        this.a=a; this.b=b; this.c=c; this.d=d;
    }
    
    //in log scale to avoid overflow
    public double getChiSquared(){
        double n = Math.log(total());
        double num = 2*Math.log(Math.abs((a*d) - (b*c)));
        double den = Math.log(a+b)+Math.log(a+c)+Math.log(c+d)+Math.log(b+d);
        double tmp = n+num-den;
        return Math.exp(tmp);
    }
    
    //Pointwise Mutual Information
    public double getPMutualInfo(){
        if(a==0) return 0.0;
        double n = Math.log(total());        
        double denon = Math.log(a+b)+Math.log(a+c);
        double tmp2 = Math.log(a)+n-denon;
        double tmp = tmp2/Math.log(2.0);
        return tmp;
    }
    
    //Compensated Pointwise Mutual Information
    // Basically, count(feature1,feature2)*PointwiseMutualInfo
    //it should compensate for low frequency bias in original PMutualInfo
    public double getCompensatedPMutualInfo(int count){
        double tmp2 = getPMutualInfo();
        return tmp2*count;
    }
    
    @Override
		public String toString() {
        return "CTable: [ "+a+" , "+b+" , "+c+" , "+d+" ]";
      }
    
    public long total(){return (a+b+c+d);}

    public static void main(String[] args) {
        System.out.println("Usage: java ContingencyTable a_value b_value c_value d_value");
        ContingencyTable ct = new ContingencyTable(Long.parseLong(args[0]), Long.parseLong(args[1]), Long.parseLong(args[2]), Long.parseLong(args[3]));
        System.out.println("Score chi = "+ct.getChiSquared());
        System.out.println("Score PMI = "+ct.getPMutualInfo());
        System.out.println("Score PMI comp = "+ct.getCompensatedPMutualInfo(3));
        System.out.println(ct.toString());
   }
}
