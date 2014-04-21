package edu.cmu.minorthird.util;

/**
 * Encode/decode special characters in strings.
 *
 */
public class StringEncoder
{
	private char escapeChar;
	private String illegalChars;

	public StringEncoder(char escapeChar,String illegalChars)
	{
		this.escapeChar = escapeChar;
		this.illegalChars = illegalChars;
	}

	public String encode(String s)
	{
		StringBuffer buf = new StringBuffer("");
		for (int i=0; i<s.length(); i++) {
			char ch = s.charAt(i);
			if (ch==escapeChar) {
				buf.append(escapeChar);
				buf.append(escapeChar);
			} else if (isIllegal(ch)) {
				buf.append(escapeChar);
				int code = ch;
				int hex1 = code/16;
				int hex2 = code%16;
				buf.append(Character.forDigit(hex1,16));
				buf.append(Character.forDigit(hex2,16));
			} else {
				buf.append(ch);
			}
		}
		return buf.toString();
	}
	public String decode(String s)
	{
		StringBuffer buf = new StringBuffer("");
		int k=0;
		while (k<s.length()) {
			if (s.charAt(k)!=escapeChar) {
				buf.append(s.charAt(k));
				k+=1;
			} else if (s.charAt(k+1)==escapeChar) {
				buf.append(escapeChar);
				k+=2;
			} else {
				int hex1 = Character.digit(s.charAt(k+1),16);
				int hex2 = Character.digit(s.charAt(k+2),16);
//				int code = hex1*16+hex2;
				buf.append((char)(hex1*16+hex2));
				k+=3;
			}
		}
		return buf.toString();
	}

	private boolean isIllegal(char c) 
	{
		for (int i=0; i<illegalChars.length(); i++) {
			if (illegalChars.charAt(i)==c) return true;
		}
		return false;
	}

	static public void main(String[] args)
	{
		StringEncoder e = new StringEncoder('%',".= \t");
		for (int i=0; i<args.length; i++) {
			System.out.println("'"+args[i]+"'\t'"+e.encode(args[i])+"'\t'"+e.decode(e.encode(args[i]))+"'");
		}
	}
}
