
JAVA_FILES =  ArrayParser.java ArrayFileParser.java ChildrenFromVectors.java \
              FeatureVectorParser.java FoldParser.java FoldSeparator.java \
              LineByLine.java LinkedChild.java LinkedVector.java Parser.java

if USING_JAVAC
AM_JAVACFLAGS = -source 1.4 @JAVA_DEBUG@
endif

include $(top_srcdir)/JavaMake.am

EXTRA_DIST =  $(JAVA_FILES) package.html

