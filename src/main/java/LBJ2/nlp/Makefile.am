
SUBDIRS = . seg

BUILT_SOURCES = Affixes.java Capitalization.java Forms.java \
                WordTypeInformation.java
DISTCLEANFILES = $(BUILT_SOURCES)

JAVA_FILES =  ColumnFormat.java NLDocument.java POS.java \
              POSBracketToVector.java POSBracketToWord.java Sentence.java \
              SentenceSplitter.java StringArraysToWords.java Word.java \
              WordsInDocumentByDirectory.java WordSplitter.java \
              $(BUILT_SOURCES)

if USING_JAVAC
AM_JAVACFLAGS = -source 1.4 @JAVA_DEBUG@
endif

include $(top_srcdir)/JavaMake.am

EXTRA_DIST =  $(JAVA_FILES) CommonFeatures.lbj package.html

$(BUILT_SOURCES) : CommonFeatures.lbj
	@CONVERTED_CLASSPATH=`$(top_builddir)/convertClassPath.pl $(build_os) '$(top_srcdir):$(top_builddir):'`"$$CLASSPATH"; \
	 echo CONVERTED_CLASSPATH=$$CONVERTED_CLASSPATH; \
	 echo '$(JAVA) -cp "$$CONVERTED_CLASSPATH" LBJ2.Main -j '\''$(AM_JAVACFLAGS)'\'' -sourcepath $(top_srcdir) -d $(top_builddir) -gsp $(top_builddir) $(srcdir)/CommonFeatures.lbj'; \
	 $(JAVA) -cp "$$CONVERTED_CLASSPATH" LBJ2.Main -j '$(AM_JAVACFLAGS)' -sourcepath $(top_srcdir) -d $(top_builddir) -gsp $(top_builddir) $(srcdir)/CommonFeatures.lbj

