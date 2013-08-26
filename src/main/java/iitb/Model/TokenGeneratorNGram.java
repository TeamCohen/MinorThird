/*
 * Created on May 4, 2008
 * @author sunita
 */
package iitb.Model;

public class TokenGeneratorNGram extends TokenGenerator {
    int N;
    public TokenGeneratorNGram(int N, boolean onlyLastNGram) {
        this.N=N;
        assert(onlyLastNGram);
    }
    @Override
    public Object getKey(Object xArg) {
        String xstr = xArg.toString();
        return xstr.substring(Math.max(0,xstr.length()-N)).toLowerCase();
    }
}
