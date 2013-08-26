
if USING_JAVAC
AM_JAVACFLAGS = -source 1.4 @JAVA_DEBUG@
endif

lib_LTLIBRARIES =
if WANT_LIBGLPK
lib_LTLIBRARIES += libLBJGLPKHook.la
BUILT_SOURCES = LBJ2_jni_GLPKHook.h
DISTCLEANFILES = $(BUILT_SOURCES)
endif

GLPKHook.class : GLPKHook.java
	@CONVERTED_CLASSPATH=`$(top_builddir)/convertClassPath.pl $(build_os) '$(top_srcdir):$(top_builddir):'`"$$CLASSPATH"; \
	 echo CONVERTED_CLASSPATH=$$CONVERTED_CLASSPATH; \
	 echo '$(JAVAC) -classpath "$$CONVERTED_CLASSPATH" @JAVA_DEBUG@ -d $(top_builddir) $(AM_JAVACFLAGS) $(JAVACFLAGS) $(srcdir)/GLPKHook.java'; \
	 $(JAVAC) -classpath "$$CONVERTED_CLASSPATH" @JAVA_DEBUG@ -d $(top_builddir) $(AM_JAVACFLAGS) $(JAVACFLAGS) $(srcdir)/GLPKHook.java

LBJ2_jni_GLPKHook.h : GLPKHook.class
	@CONVERTED_CLASSPATH=`$(top_builddir)/convertClassPath.pl $(build_os) '$(top_srcdir):$(top_builddir):'`"$$CLASSPATH"; \
	 echo CONVERTED_CLASSPATH=$$CONVERTED_CLASSPATH; \
	 echo 'test "x$(JAVAH)" != "x" && $(JAVAH) -classpath "$$CONVERTED_CLASSPATH" -jni LBJ2.jni.GLPKHook'; \
	 test "x$(JAVAH)" != "x" \
	   && $(JAVAH) -classpath "$$CONVERTED_CLASSPATH" -jni LBJ2.jni.GLPKHook \
	   && touch LBJ2_jni_GLPKHook.h

libLBJGLPKHook_la_SOURCES = LBJ2_jni_GLPKHook.c LBJ2_jni_GLPKHook.h

EXTRA_DIST =  GLPKHook.java package.html

clean-local:
	-rm -f *.class

