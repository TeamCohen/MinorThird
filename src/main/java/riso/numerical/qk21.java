package riso.numerical;

/** qagse.java, qags.java, qelg.java, qk21.java, and qpsrt.java are
  * derivative works (translations) of Fortran code by Robert Piessens
  * and Elise de Doncker. These five files are released under GPL
  * by permission of Robert Piessens. In response to my question,
  * <pre>
  *  >I would like to have your permission to distribute my
  *  >Java translation of your QUADPACK routines under the
  *  >terms of the GPL. Do I have your permission to do so?
  * </pre>
  * Robert Piessens writes:
  * <pre>
  *  Date:  Mon, 28 Jan 2002 14:41:58 +0100
  *  To: "Robert Dodier" <robert_dodier@yahoo.com>
  *  From: "Robert Piessens" <Robert.Piessens@cs.kuleuven.ac.be>
  *  Subject: Re: Permission to redistribute QUADPACK translation?
  *  
  *  OK, You have my permission.
  *  
  *  Robert Piessens
  * </pre>
  */
public class qk21 implements java.io.Serializable
{
	public static double [ ] D1MACH =
	{
		Double.MIN_VALUE, Double.MAX_VALUE,
		Math.pow ( 2, -52 ) , Math.pow ( 2, -51 ) ,
		Math.log ( 2 ) /Math.log ( 10 )
	};

	public static void do_qk21 ( Callback_1d integrand, double a, double b, double[] result, double[] abserr, double[] resabs, double[] resasc ) throws Exception
	{
// System.err.println( "do_qk21: a: "+a+"  b: "+b );
		double absc,centr,dhlgth,dmax1,dmin1;
		double epmach,fc,fsum,fval1,fval2,hlgth,resg,resk,reskh,uflow;
		int j,jtw,jtwm1;
		double [ ] fv1 = new double [ 10 ];
		double [ ] fv2 = new double [ 10 ];
		double [ ] wg =
		{
			0.066671344308688137593568809893332, 0.149451349150580593145776339657697,
			0.219086362515982043995534934228163, 0.269266719309996355091226921569469,
			0.295524224714752870173892994651338
		};
		double [ ] xgk =
		{
			0.995657163025808080735527280689003, 0.973906528517171720077964012084452,
			0.930157491355708226001207180059508, 0.865063366688984510732096688423493,
			0.780817726586416897063717578345042, 0.679409568299024406234327365114874,
			0.562757134668604683339000099272694, 0.433395394129247190799265943165784,
			0.294392862701460198131126603103866, 0.148874338981631210884826001129720,
			0.000000000000000000000000000000000
		};
		double [ ] wgk =
		{
			0.011694638867371874278064396062192, 0.032558162307964727478818972459390,
			0.054755896574351996031381300244580, 0.075039674810919952767043140916190,
			0.093125454583697605535065465083366, 0.109387158802297641899210590325805,
			0.123491976262065851077958109831074, 0.134709217311473325928054001771707,
			0.142775938577060080797094273138717, 0.147739104901338491374841515972068,
			0.149445554002916905664936468389821
		};
		epmach = D1MACH [ 4-1 ];
		uflow = D1MACH [ 1-1 ];
		centr = 0.5* ( a+b );
		hlgth = 0.5* ( b-a );
		dhlgth = Math.abs ( hlgth );
		resg = 0;
		fc = integrand.f ( centr );
// double max_fx = fc;
		resk = wgk [ 11-1 ] *fc;
		resabs[0] = Math.abs ( resk );
		for ( j = 1 ; j <= 5 ; j++ )
		{
			jtw = 2*j;
			absc = hlgth*xgk [ jtw-1 ];
			fval1 = integrand.f ( centr-absc );
// if ( fval1 > max_fx ) max_fx = fval1;
			fval2 = integrand.f ( centr+absc );
// if ( fval2 > max_fx ) max_fx = fval2;
			fv1 [ jtw-1 ] = fval1;
			fv2 [ jtw-1 ] = fval2;
			fsum = fval1+fval2;
			resg = resg+wg [ j-1 ] *fsum;
			resk = resk+wgk [ jtw-1 ] *fsum;
			resabs[0] = resabs[0]+wgk [ jtw-1 ] * ( Math.abs ( fval1 ) +Math.abs ( fval2 ) );
		}
		for ( j = 1 ; j <= 5 ; j++ )
		{
			jtwm1 = 2*j-1;
			absc = hlgth*xgk [ jtwm1-1 ];
			fval1 = integrand.f ( centr-absc );
// if ( fval1 > max_fx ) max_fx = fval1;
			fval2 = integrand.f ( centr+absc );
// if ( fval2 > max_fx ) max_fx = fval2;
			fv1 [ jtwm1-1 ] = fval1;
			fv2 [ jtwm1-1 ] = fval2;
			fsum = fval1+fval2;
			resk = resk+wgk [ jtwm1-1 ] *fsum;
			resabs[0] = resabs[0]+wgk [ jtwm1-1 ] * ( Math.abs ( fval1 ) +Math.abs ( fval2 ) );
		}
		reskh = resk*0.5;
		resasc[0] = wgk [ 11-1 ] *Math.abs ( fc-reskh );
		for ( j = 1 ; j <= 10 ; j++ ) resasc[0] = resasc[0]+wgk [ j-1 ] * ( Math.abs ( fv1 [ j-1 ] -reskh ) +Math.abs ( fv2 [ j-1 ] -reskh ) );
		result[0] = resk*hlgth;
		resabs[0] = resabs[0]*dhlgth;
		resasc[0] = resasc[0]*dhlgth;
		abserr[0] = Math.abs ( ( resk-resg ) *hlgth );
		if ( resasc[0] != 0 && abserr[0] != 0 ) abserr[0] = resasc[0]*Math.min ( 1,Math.pow ( ( 200*abserr[0]/resasc[0] ) , 1.5 ) );
		if ( resabs[0] > uflow/ ( 50*epmach ) ) abserr[0] = Math.max ( ( epmach*50 ) *resabs[0],abserr[0] );
// System.err.println( "do_qk21: max_fx: "+max_fx+", result: "+result[0]+"  abserr: "+abserr[0] );
	}

	public static void main( String[] args )
	{
		double a, b;
		double[] result = new double[1];
		double[] abserr = new double[1];
		double[] resabs = new double[1];
		double[] resasc = new double[1];

		qk21 q = new qk21();
		a = Double.parseDouble( args[0] );
		b = Double.parseDouble( args[1] );
		System.err.println( "a: "+a+"  b: "+b );
		Callback_1d integrand = new GaussBump();
		try { q.do_qk21( integrand, a, b, result, abserr, resabs, resasc ); }
		catch (Exception e) { e.printStackTrace(); return; }

		System.err.println( "result: "+result[0] );
		System.err.println( "abserr: "+abserr[0] );
		System.err.println( "resabs: "+resabs[0] );
		System.err.println( "resasc: "+resasc[0] );
	}
}

class GaussBump implements Callback_1d
{
	public double f( double x ) 
	{
		double fx = 1/Math.sqrt( 2*Math.PI ) * Math.exp( -x*x/2 );
// System.err.println( "x: "+x+"  fx: "+fx );
		return fx;
	}
}
