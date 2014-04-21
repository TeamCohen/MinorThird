
BUILT_SOURCES = Yylex.java parser.java sym.java SymbolNames.java
MAINTAINERCLEANFILES = $(BUILT_SOURCES) scanner.jlx scanner.jlx.java

JAVA_FILES =  ScannerError.java TokenValue.java $(BUILT_SOURCES)

if USING_JAVAC
AM_JAVACFLAGS = -source 1.4 @JAVA_DEBUG@
endif

include $(top_srcdir)/JavaMake.am

EXTRA_DIST =  $(JAVA_FILES) scanner.jlex parser.cup package.html

Yylex.java : $(srcdir)/scanner.jlex
	cp $(srcdir)/scanner.jlex $(builddir)/scanner.jlx
	$(JAVA) JLex.Main scanner.jlx
	mv scanner.jlx.java Yylex.java

parser.java sym.java : parser.cup
	$(JAVA) java_cup.Main $(srcdir)/parser.cup

SymbolNames.java : sym.java
	$(PERL) genNameTable.pl sym.java > SymbolNames.java

