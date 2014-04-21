package LBJ2.util;

import java.io.PrintStream;
import java.util.Arrays;


/**
  * A library of routines for taking tabular data and producing a human
  * readable string representation of it.  The resulting string(s) can be pure
  * ASCII or latex compatible.
  *
  * @author Nick Rizzolo
 **/
public class TableFormat
{
  /**
    * The default number of significant digits to which table entries will be
    * rounded.
   **/
  private static final int defaultSignificantDigits = 3;


  /**
    * Simply converts the type of the given matrix from <code>double</code> to
    * <code>Double</code>.
    *
    * @param m  The matrix.
    * @return The type converted matrix.
   **/
  public static Double[][] wrapDouble(double[][] m) {
    Double[][] result = new Double[m.length][];

    for (int i = 0; i < m.length; ++i)
      if (m[i] != null) {
        result[i] = new Double[m[i].length];
        for (int j = 0; j < m[i].length; ++j)
          result[i][j] = new Double(m[i][j]);
      }

    return result;
  }


  /**
    * Simply prints each element of the given array of strings to the given
    * stream in its own line.
    *
    * @param out    The stream.
    * @param table  The strings.
   **/
  public static void printTable(PrintStream out, String[] table) {
    for (int i = 0; i < table.length; ++i) out.println(table[i]);
  }


  /**
    * Formats the given data into an ASCII table and prints it to the given
    * stream.  All non-<code>null</code> entries will be rounded to three
    * significant digits after the decimal place.
    *
    * @param out  The stream.
    * @param data The numerical data.
   **/
  public static void printTableFormat(PrintStream out, double[][] data) {
    printTable(out, tableFormat(data));
  }


  /**
    * Formats the given data into an ASCII table.  All non-<code>null</code>
    * entries will be rounded to three significant digits after the decimal
    * place.
    *
    * @param data The numerical data.
    * @return The string respresentation of the data.
   **/
  public static String[] tableFormat(double[][] data) {
    if (data == null || data.length == 0) return new String[0];
    return tableFormat(null, null, wrapDouble(data));
  }


  /**
    * Formats the given data into an ASCII table and prints it to the given
    * stream.  All non-<code>null</code> entries will be rounded to three
    * significant digits after the decimal place.
    *
    * @param out  The stream.
    * @param data The numerical data.
   **/
  public static void printTableFormat(PrintStream out, Double[][] data) {
    printTable(out, tableFormat(data));
  }


  /**
    * Formats the given data into an ASCII table.  All non-<code>null</code>
    * entries will be rounded to three significant digits after the decimal
    * place.
    *
    * @param data The numerical data.
    * @return The string respresentation of the data.
   **/
  public static String[] tableFormat(Double[][] data) {
    if (data == null || data.length == 0) return new String[0];
    return tableFormat(null, null, data);
  }


  /**
    * Formats the given data into an ASCII table and prints it to the given
    * stream.  All non-<code>null</code> entries will be rounded to three
    * significant digits after the decimal place.
    *
    * @param out          The stream.
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-<code>null</code> row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
   **/
  public static void printTableFormat(PrintStream out, String[] columnLabels,
                                      String[] rowLabels, double[][] data) {
    printTable(out, tableFormat(columnLabels, rowLabels, data));
  }


  /**
    * Formats the given data into an ASCII table.  All non-<code>null</code>
    * entries will be rounded to three significant digits after the decimal
    * place.
    *
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-<code>null</code> row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
    * @return The string respresentation of the data.
   **/
  public static String[] tableFormat(String[] columnLabels,
                                     String[] rowLabels, double[][] data) {
    if (data == null || data.length == 0) return new String[0];
    return tableFormat(columnLabels, rowLabels, wrapDouble(data));
  }


  /**
    * Formats the given data into an ASCII table and prints it to the given
    * stream.  Any entry in any row of <code>data</code> set to
    * <code>null</code> will become a single dash in the output.  All
    * non-<code>null</code> entries will be rounded to three significant
    * digits after the decimal place.
    *
    * @param out          The stream.
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-<code>null</code> row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
   **/
  public static void printTableFormat(PrintStream out, String[] columnLabels,
                                      String[] rowLabels, Double[][] data) {
    printTable(out, tableFormat(columnLabels, rowLabels, data));
  }


  /**
    * Formats the given data into an ASCII table.  Any entry in any row of
    * <code>data</code> set to <code>null</code> will become a single dash in
    * the output.  All non-<code>null</code> entries will be rounded to three
    * significant digits after the decimal place.
    *
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-<code>null</code> row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
    * @return The string respresentation of the data.
   **/
  public static String[] tableFormat(String[] columnLabels,
                                     String[] rowLabels, Double[][] data) {
    if (data == null || data.length == 0) return new String[0];
    int dataColumns = 0;

    for (int i = 0; i < data.length; ++i) {
      int current = data[i] == null ? 0 : data[i].length;
      dataColumns = Math.max(dataColumns, current);
    }

    if (dataColumns == 0) return new String[0];

    int[] sigDigits = new int[dataColumns];
    for (int i = 0; i < dataColumns; ++i)
      sigDigits[i] = defaultSignificantDigits;
    return tableFormat(columnLabels, rowLabels, data, sigDigits);
  }


  /**
    * Formats the given data into an ASCII table and prints it to the given
    * stream.  Any entry in any row of <code>data</code> set to
    * <code>null</code> will become a single dash in the output.
    *
    * @param out          The stream.
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-<code>null</code> row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
    * @param sigDigits    Significant digits specified on a column by column
    *                     basis, only for <code>data</code> columns.
   **/
  public static void printTableFormat(PrintStream out, String[] columnLabels,
                                      String[] rowLabels, double[][] data,
                                      int[] sigDigits) {
    printTable(out, tableFormat(columnLabels, rowLabels, data, sigDigits));
  }


  /**
    * Formats the given data into an ASCII table.  Any entry in any row of
    * <code>data</code> set to <code>null</code> will become a single dash in
    * the output.
    *
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-<code>null</code> row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
    * @param sigDigits    Significant digits specified on a column by column
    *                     basis, only for <code>data</code> columns.
    * @return The string respresentation of the data.
   **/
  public static String[] tableFormat(String[] columnLabels,
                                     String[] rowLabels, double[][] data,
                                     int[] sigDigits) {
    if (data == null || data.length == 0) return new String[0];
    return tableFormat(columnLabels, rowLabels, wrapDouble(data), sigDigits);
  }


  /**
    * Formats the given data into an ASCII table and prints it to the given
    * stream.  Any entry in any row of <code>data</code> set to
    * <code>null</code> will become a single dash in the output.
    *
    * @param out          The stream.
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-<code>null</code> row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
    * @param sigDigits    Significant digits specified on a column by column
    *                     basis, only for <code>data</code> columns.
   **/
  public static void printTableFormat(PrintStream out, String[] columnLabels,
                                      String[] rowLabels, Double[][] data,
                                      int[] sigDigits) {
    printTable(out, tableFormat(columnLabels, rowLabels, data, sigDigits));
  }


  /**
    * Formats the given data into an ASCII table.  Any entry in any row of
    * <code>data</code> set to <code>null</code> will become a single dash in
    * the output.
    *
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-<code>null</code> row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
    * @param sigDigits    Significant digits specified on a column by column
    *                     basis, only for <code>data</code> columns.
    * @return The string respresentation of the data.
   **/
  public static String[] tableFormat(String[] columnLabels,
                                     String[] rowLabels, Double[][] data,
                                     int[] sigDigits) {
    return tableFormat(columnLabels, rowLabels, data, sigDigits, new int[0]);
  }


  /**
    * Formats the given data into an ASCII table and prints it to the given
    * stream.  Any entry in any row of <code>data</code> set to
    * <code>null</code> will become a single dash in the output.
    *
    * @param out          The stream.
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-<code>null</code> row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
    * @param sigDigits    Significant digits specified on a column by column
    *                     basis, only for <code>data</code> columns.
    * @param dashRows     The indexes of rows in <code>data</code> which
    *                     should be preceded by a row of dashes in the output.
   **/
  public static void printTableFormat(PrintStream out, String[] columnLabels,
                                      String[] rowLabels, double[][] data,
                                      int[] sigDigits, int[] dashRows) {
    printTable(out,
               tableFormat(columnLabels, rowLabels, data, sigDigits,
                           dashRows));
  }


  /**
    * Formats the given data into an ASCII table.  Any entry in any row of
    * <code>data</code> set to <code>null</code> will become a single dash in
    * the output.
    *
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-<code>null</code> row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
    * @param sigDigits    Significant digits specified on a column by column
    *                     basis, only for <code>data</code> columns.
    * @param dashRows     The indexes of rows in <code>data</code> which
    *                     should be preceded by a row of dashes in the output.
    * @return The string respresentation of the data.
   **/
  public static String[] tableFormat(String[] columnLabels,
                                     String[] rowLabels, double[][] data,
                                     int[] sigDigits, int[] dashRows) {
    if (data == null || data.length == 0) return new String[0];
    return
      tableFormat(columnLabels, rowLabels, wrapDouble(data), sigDigits,
                  dashRows);
  }


  /**
    * Formats the given data into an ASCII table and prints it to the given
    * stream.  Any entry in any row of <code>data</code> set to
    * <code>null</code> will become a single dash in the output.
    *
    * @param out          The stream.
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-<code>null</code> row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
    * @param sigDigits    Significant digits specified on a column by column
    *                     basis, only for <code>data</code> columns.
    * @param dashRows     The indexes of rows in <code>data</code> which
    *                     should be preceded by a row of dashes in the output.
   **/
  public static void printTableFormat(PrintStream out, String[] columnLabels,
                                      String[] rowLabels, Double[][] data,
                                      int[] sigDigits, int[] dashRows) {
    printTable(out,
               tableFormat(columnLabels, rowLabels, data, sigDigits,
                           dashRows));
  }


  /**
    * Formats the given data into an ASCII table.  Any entry in any row of
    * <code>data</code> set to <code>null</code> will become a single dash in
    * the output.
    *
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-<code>null</code> row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
    * @param sigDigits    Significant digits specified on a column by column
    *                     basis, only for <code>data</code> columns.
    * @param dashRows     The indexes of rows in <code>data</code> which
    *                     should be preceded by a row of dashes in the output.
    * @return The string respresentation of the data.
   **/
  public static String[] tableFormat(String[] columnLabels,
                                     String[] rowLabels, Double[][] data,
                                     int[] sigDigits, int[] dashRows) {
    if (data == null || data.length == 0) return new String[0];
    if (sigDigits == null) return tableFormat(columnLabels, rowLabels, data);

    int dataColumns = 0;

    for (int i = 0; i < data.length; ++i) {
      int current = data[i] == null ? 0 : data[i].length;
      dataColumns = Math.max(dataColumns, current);
    }

    if (dataColumns == 0) return new String[0];

    int columns = dataColumns;
    if (rowLabels != null) ++columns;

    if (sigDigits.length < dataColumns) {
      int[] temp = new int[dataColumns];
      System.arraycopy(sigDigits, 0, temp, 0, sigDigits.length);
      for (int i = sigDigits.length; i < dataColumns; ++i)
        temp[i] = defaultSignificantDigits;
      sigDigits = temp;
    }

    int dataRows = data.length;
    int rows = dataRows;

    if (columnLabels != null) {
      if (columnLabels.length < columns) {
        String[] temp = new String[columns];
        System.arraycopy(columnLabels, 0, temp,
                         columns - columnLabels.length, columnLabels.length);
        columnLabels = temp;
      }

      for (int i = 0; i < columnLabels.length; ++i)
        if (columnLabels[i] == null) columnLabels[i] = "";
      ++rows;
    }

    if (rowLabels != null) {
      String[] temp = new String[dataRows];
      int length = Math.min(rowLabels.length, dataRows);
      System.arraycopy(rowLabels, 0, temp, 0, length);
      rowLabels = temp;

      for (int i = 0; i < rowLabels.length; ++i)
        if (rowLabels[i] == null) rowLabels[i] = "";
    }

    return asciiTableFormat(columns, dataColumns, rows, dataRows,
                            columnLabels, rowLabels, data, sigDigits,
                            dashRows);
  }


  /**
    * Formats the given data into an ASCII table.  Any row of
    * <code>data</code> set to <code>null</code> will become a row with one
    * dash in each cell of the output table's row.  Any entry in any row of
    * <code>data</code> set to <code>null</code> will also become a single
    * dash in the output.
    *
    * @param columns      The total number of columns in the table.
    * @param dataColumns  The number of columns containing data in the table.
    * @param rows         The total number of rows in the table.
    * @param dataRows     The number of rows containing data in the table.
    * @param columnLabels One label for each column in the output, starting
    *                     with the column of row labels if one exists.  If
    *                     <code>null</code>, no column label row will be
    *                     returned.
    * @param rowLabels    One label for each non-null row in
    *                     <code>data</code>.  If <code>null</code>, no row
    *                     label column will be returned.
    * @param data         The numerical data.
    * @param sigDigits    Significant digits specified on a column by column
    *                     basis, only for <code>data</code> columns.
    * @param dashRows     The indexes of rows in <code>data</code> which
    *                     should be preceded by a row of dashes in the output.
    * @return The string respresentation of the data.
   **/
  protected static String[] asciiTableFormat(
      int columns, int dataColumns, int rows, int dataRows,
      String[] columnLabels, String[] rowLabels, Double[][] data,
      int[] sigDigits, int[] dashRows) {
    String[][] table = new String[rows][columns];
    int dataRowStart = 0;
    int dataColumnStart = 0;

    if (columnLabels != null) {
      dataRowStart = 1;
      table[0] = columnLabels;
    }

    if (rowLabels != null) {
      dataColumnStart = 1;
      for (int i = 0; i < dataRows; ++i)
        table[dataRowStart + i][0] = rowLabels[i];
    }

    for (int i = 0; i < dataRows; ++i)
      for (int j = 0; j < dataColumns; ++j)
        table[dataRowStart + i][dataColumnStart + j] =
          data[i] == null || data[i][j] == null
            ? "-" : format(data[i][j].doubleValue(), sigDigits[j]);

    int[] columnWidths = new int[columns];

    for (int i = 0; i < rows; ++i)
      for (int j = 0; j < columns; ++j) {
        columnWidths[j] = Math.max(columnWidths[j], table[i][j].length());
      }

    if (columnLabels != null)
      for (int j = 0; j < columns; ++j)
        table[0][j] = center(table[0][j], columnWidths[j]);
    if (rowLabels != null)
      for (int i = 0; i < dataRows; ++i)
        table[dataRowStart + i][0] =
          ljust(table[dataRowStart + i][0], columnWidths[0]);

    for (int i = 0; i < dataRows; ++i)
        for (int j = 0; j < dataColumns; ++j)
          table[dataRowStart + i][dataColumnStart + j] =
            table[dataRowStart + i][dataColumnStart + j].equals("-")
              ? center("-", columnWidths[dataColumnStart + j])
              : rjust(table[dataRowStart + i][dataColumnStart + j],
                      columnWidths[dataColumnStart + j]);

    Arrays.sort(dashRows);
    int d = 0;
    while (d < dashRows.length && dashRows[d] < 0) ++d;

    String[] result = new String[rows + dashRows.length - d];
    String dashes = "";
    for (int i = 0; i < columnWidths.length - 1; ++i) dashes += "-";
    for (int j = 0; j < columnWidths.length; ++j)
      for (int i = 0; i < columnWidths[j]; ++i)
        dashes += "-";
    int r = 0;

    for (int i = 0; i < result.length; ++i)
      if (d < dashRows.length && (r == dashRows[d] + 1 || r == rows)) {
        result[i] = dashes;
        ++d;
      }
      else {
        result[i] = table[r][0];
        for (int j = 1; j < columns; ++j)
          result[i] += " " + table[r][j];
        ++r;
      }

    return result;
  }


  /**
    * Transposes the given matrix so that the rows become the columns and the
    * columns become the rows.
    *
    * @param m  The matrix to transpose.
    * @return The transposed matrix.
   **/
  public static double[][] transpose(double[][] m) {
    if (m == null) return null;
    if (m.length == 0) return new double[0][];
    int columns = 0;
    int rows = 0;

    for (int i = 0; i < m.length; ++i)
      if (m[i] != null) {
        rows = Math.max(rows, m[i].length);
        ++columns;
      }

    double[][] result = new double[rows][];

    for (int i = 0; i < result.length; ++i) {
      result[i] = new double[columns];
      int c = 0;

      for (int j = 0; j < m.length; ++j)
        if (m[j] != null) {
          if (m[j].length > i) result[i][c] = m[j][i];
          ++c;
        }
    }

    return result;
  }


  /**
    * Transposes the given matrix so that the rows become the columns and the
    * columns become the rows.
    *
    * @param m  The matrix to transpose.
    * @return The transposed matrix.
   **/
  public static Double[][] transpose(Double[][] m) {
    if (m == null) return null;
    if (m.length == 0) return new Double[0][];
    int columns = 0;
    int rows = 0;

    for (int i = 0; i < m.length; ++i)
      if (m[i] != null) {
        rows = Math.max(rows, m[i].length);
        ++columns;
      }

    Double[][] result = new Double[rows][];

    for (int i = 0; i < result.length; ++i) {
      result[i] = new Double[columns];
      int c = 0;

      for (int j = 0; j < m.length; ++j)
        if (m[j] != null) {
          if (m[j].length > i) result[i][c] = m[j][i];
          ++c;
        }
    }

    return result;
  }


  /**
    * Formats a floating point number so that it is rounded and zero-padded to
    * the given number of significant digits after the decimal point.
    *
    * @param f  The floating point value to format.
    * @param d  The number of significant digits to round to.  If less than 0,
    *           this method assumes 0; if greater than 18, this method assumes
    *           18.
    * @return The formatted result.
   **/
  protected static String format(double f, int d) {
    if (d > 18) d = 18;
    long m = 1;
    for (int j = 0; j < d; ++j) m *= 10;

    String unformatted = "" + f;
    String sign = "";

    if (unformatted.startsWith("-")) {
      sign = "-";
      unformatted = unformatted.substring(1);
      f *= -1;
    }

    int leftDigits = 0;
    int p = unformatted.indexOf('.');
    if (p == -1) leftDigits = unformatted.length();
    else leftDigits = p;

    String base = unformatted;
    String exponent = "";
    int e = unformatted.indexOf('E');

    if (e != -1) {
      base = unformatted.substring(0, e);
      exponent = unformatted.substring(e);
      f = Double.parseDouble(base);
    }

    StringBuffer buffer = new StringBuffer("" + Math.round(f * m));
    while (buffer.length() < d + leftDigits) buffer.insert(0, '0');
    if (d > 0) buffer.insert(buffer.length() - d, '.');
    return sign + buffer + exponent;
  }


  /**
    * Returns a space-padded string of at least the specified width such that
    * the argument string is left-justified within the returned string.
    *
    * @param original The string to justify.
    * @param width    The width within which the original string should be
    *                 padded.
    * @return The padded string.  If the original is longer than the specified
    *         width, the original is simply returned.
   **/
  protected static String ljust(String original, int width) {
    while (original.length() < width) original += " ";
    return original;
  }


  /**
    * Returns a space-padded string of at least the specified width such that
    * the argument string is right-justified within the returned string.
    *
    * @param original The string to justify.
    * @param width    The width within which the original string should be
    *                 padded.
    * @return The padded string.  If the original is longer than the specified
    *         width, the original is simply returned.
   **/
  protected static String rjust(String original, int width) {
    while (original.length() < width) original = " " + original;
    return original;
  }


  /**
    * Returns a space-padded string of at least the specified width such that
    * the argument string is centered within the returned string.
    *
    * @param original The string to justify.
    * @param width    The width within which the original string should be
    *                 padded.
    * @return The padded string.  If the original is longer than the specified
    *         width, the original is simply returned.
   **/
  protected static String center(String original, int width) {
    int toAdd = width - original.length(), i;
    for (i = 0; i < toAdd / 2; ++i) original = " " + original;
    for (; i < toAdd; ++i) original += " ";
    return original;
  }
}

