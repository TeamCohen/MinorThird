
JAVA_FILES =  BIOTester.java POSBracketToToken.java PlainToTokenParser.java \
              SegmentTagPlain.java Token.java WordsToTokens.java

if USING_JAVAC
AM_JAVACFLAGS = -source 1.4 @JAVA_DEBUG@
endif

include $(top_srcdir)/JavaMake.am

EXTRA_DIST =  $(JAVA_FILES) package.html

