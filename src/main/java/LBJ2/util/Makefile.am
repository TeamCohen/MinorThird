
JAVA_FILES =  ByteString.java ClassUtils.java DVector2D.java DVector.java \
              ExceptionlessInputStream.java ExceptionlessOutputStream.java \
              FVector.java IVector2D.java IVector.java OVector.java \
              Sort.java StudentT.java TableFormat.java

if USING_JAVAC
AM_JAVACFLAGS = -source 1.4 @JAVA_DEBUG@
endif

include $(top_srcdir)/JavaMake.am

EXTRA_DIST =  $(JAVA_FILES) package.html

