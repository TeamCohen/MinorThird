/*
 * Created on Dec 6, 2004
 *
 */
package iitb.Utils;

/**
 * @author Administrator
 *
 */
public class Utils {
	   public static int log2Ceil(int numArg) {
        int log2 = 0;
        int num = numArg;
        for (; num > 0; log2++) {
            num = (num >> 1);
        }
        if ((1 << (log2-1)) == numArg)
            log2--;
        return log2;
    }
}
