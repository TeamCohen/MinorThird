#include <stdlib.h>
#include <string.h>
#include <glpk.h>
#include <jni.h>
#include "config.h"
#include "LBJ2_jni_GLPKHook.h"


#if HAVE_GLP_TERM_OUT
#define setMessagesOff glp_term_out(GLP_OFF)
#else
static int disableMessages(void *info, char *buf) { return 1; }
#define setMessagesOff _glp_lib_print_hook(disableMessages, NULL)
#endif


static LPX* getProblem(JNIEnv* environment, jobject this)
{
  jclass class = (*environment)->GetObjectClass(environment, this);
  jfieldID fieldID =
    (*environment)->GetFieldID(environment, class, "problemPointer", "J");
  return (LPX*) (*environment)->GetLongField(environment, this, fieldID);
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    createProblem
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_LBJ2_jni_GLPKHook_createProblem(JNIEnv* environment, jobject this)
{
  LPX* result = lpx_create_prob();

  jclass class = (*environment)->GetObjectClass(environment, this);
  jfieldID fieldID =
    (*environment)->GetFieldID(environment, class, "generateCuts", "Z");
  jboolean generateCuts =
    (jboolean) (*environment)->GetBooleanField(environment, this, fieldID);

  lpx_set_class(result, LPX_MIP);
  lpx_set_int_parm(result, LPX_K_MSGLEV, 0);
  setMessagesOff;
  if (generateCuts) lpx_set_int_parm(result, LPX_K_USECUTS, LPX_C_GOMORY);

  return (jlong) result;
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    deleteProblem
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_LBJ2_jni_GLPKHook_deleteProblem(JNIEnv* environment, jobject this)
{
  lpx_delete_prob(getProblem(environment, this));
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    setMaximize
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_LBJ2_jni_GLPKHook_setMaximize(JNIEnv* environment, jobject this)
{
  lpx_set_obj_dir(getProblem(environment, this), LPX_MAX);
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    setMinimize
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_LBJ2_jni_GLPKHook_setMinimize(JNIEnv* environment, jobject this)
{
  lpx_set_obj_dir(getProblem(environment, this), LPX_MIN);
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    numberOfVariables
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_LBJ2_jni_GLPKHook_numberOfVariables
(JNIEnv* environment, jobject this)
{
  return (jint) glp_get_num_cols(getProblem(environment, this));
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    numberOfConstraints
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_LBJ2_jni_GLPKHook_numberOfConstraints
(JNIEnv* environment, jobject this)
{
  return (jint) glp_get_num_rows(getProblem(environment, this));
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    addObjectiveCoefficient
 * Signature: (D)V
 */
JNIEXPORT void JNICALL
Java_LBJ2_jni_GLPKHook_addObjectiveCoefficient
(JNIEnv* environment, jobject this, jdouble c)
{
  LPX* problem = getProblem(environment, this);
  int column = lpx_add_cols(problem, 1);
  lpx_set_col_kind(problem, column, LPX_IV);
  lpx_set_col_bnds(problem, column, LPX_DB, 0.0, 1.0);
  lpx_set_obj_coef(problem, column, (double) c);
}


static int newMatrixRow
(JNIEnv* environment, jintArray i, jdoubleArray a, LPX* problem)
{
  jsize indexesLength = (*environment)->GetArrayLength(environment, i);
  jsize coefficientsLength = (*environment)->GetArrayLength(environment, a);
  jsize numberOfCoefficients =
    indexesLength < coefficientsLength ? indexesLength : coefficientsLength;
  jint* iElements = (*environment)->GetIntArrayElements(environment, i, NULL);
  jdouble* aElements =
    (*environment)->GetDoubleArrayElements(environment, a, NULL);

  int indexes[numberOfCoefficients + 1], j;
  double coefficients[numberOfCoefficients + 1];
  int row = lpx_add_rows(problem, 1);

  indexes[0] = coefficients[0] = 0;
  for (j = 0; j < numberOfCoefficients; ++j)
  {
    indexes[j + 1] = (int) iElements[j] + 1;
    coefficients[j + 1] = (double) aElements[j];
  }

  (*environment)->ReleaseIntArrayElements(environment, i, iElements,
                                          JNI_ABORT);
  (*environment)->ReleaseDoubleArrayElements(environment, a, aElements,
                                             JNI_ABORT);

  lpx_set_mat_row(problem, row, numberOfCoefficients, indexes, coefficients);
  return row;
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    addFixedConstraint
 * Signature: ([I[DD)V
 */
JNIEXPORT void JNICALL
Java_LBJ2_jni_GLPKHook_addFixedConstraint
(JNIEnv* environment, jobject this, jintArray i, jdoubleArray a, jdouble b)
{
  LPX* problem = getProblem(environment, this);
  int row = newMatrixRow(environment, i, a, problem);
  lpx_set_row_bnds(problem, row, LPX_FX, (double) b, (double) b);
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    addLowerBoundedConstraint
 * Signature: ([I[DD)V
 */
JNIEXPORT void JNICALL
Java_LBJ2_jni_GLPKHook_addLowerBoundedConstraint
(JNIEnv* environment, jobject this, jintArray i, jdoubleArray a, jdouble b)
{
  LPX* problem = getProblem(environment, this);
  int row = newMatrixRow(environment, i, a, problem);
  lpx_set_row_bnds(problem, row, LPX_LO, (double) b, 0.0);
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    addUpperBoundedConstraint
 * Signature: ([I[DD)V
 */
JNIEXPORT void JNICALL
Java_LBJ2_jni_GLPKHook_addUpperBoundedConstraint
(JNIEnv* environment, jobject this, jintArray i, jdoubleArray a, jdouble b)
{
  LPX* problem = getProblem(environment, this);
  int row = newMatrixRow(environment, i, a, problem);
  lpx_set_row_bnds(problem, row, LPX_UP, 0.0, (double) b);
}


static char* getDebugFileName(JNIEnv* environment, jobject this)
{
  jclass class = (*environment)->GetObjectClass(environment, this);
  jfieldID fieldID =
    (*environment)->GetFieldID(environment, class, "debugFileName",
                               "Ljava/lang/String;");
  jstring debugFileName =
    (jstring) (*environment)->GetObjectField(environment, this, fieldID);
  const char* fileName =
    (char*)
    (*environment)->GetStringUTFChars(environment, debugFileName, NULL);
  char* result;

  if (strlen(fileName) == 0) return NULL;
  result = (char*) calloc(strlen(fileName) + 1, sizeof(char));
  strcpy(result, fileName);
  (*environment)->ReleaseStringUTFChars(environment, debugFileName,
                                        (jbyte*) fileName);
  return result;
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    nativeSolve
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_LBJ2_jni_GLPKHook_nativeSolve(JNIEnv* environment, jobject this)
{
  LPX* problem = getProblem(environment, this);
  int exitCode = lpx_intopt(problem);
  char* fileName;
  FILE* file;

  if (exitCode == LPX_E_OK)
  {
    int status = lpx_mip_status(problem);
    if (status == LPX_I_OPT) return (jboolean) JNI_TRUE;

    fileName = getDebugFileName(environment, this);
    if (fileName != NULL)
    {
      lpx_print_mip(problem, fileName);
      free(fileName);
    }

    return (jboolean) JNI_FALSE;
  }

  fileName = getDebugFileName(environment, this);
  if (fileName == NULL) return (jboolean) JNI_FALSE;

  file = fopen(fileName, "w");
  if (exitCode == LPX_E_FAULT)
  {
    fprintf(file,
        "Unable to start the search because either the problem wasn't set\n");
    fprintf(file,
        "as an MIP (using lpx_set_class(LPX*, int)) or some integer\n");
    fprintf(file, "variable was given a non-integer lower or upper bound.\n");
  }
  else if (exitCode == LPX_E_NOPFS)
  {
    fprintf(file, "The problem has no primal feasible solution.\n");
    fprintf(file,
        "This condition was detected either by the MIP presolver, or by\n");
    fprintf(file,
        "the simplex method while solving a LP relaxation, or on\n");
    fprintf(file, "re-optimization while generating cutting planes.\n");
  }
  else if (exitCode == LPX_E_NODFS)
  {
    fprintf(file,
        "A LP relaxation of the problem has no dual feasible solution.\n");
    fprintf(file,
        "This condition was detected either by the MIP presolver or by\n");
    fprintf(file, "the simplex method while solving the LP relaxation.\n");
  }
  else if (exitCode == LPX_E_ITLIM)
  {
    fprintf(file,
        "The search was prematurely terminated because the simplex\n");
    fprintf(file, "iteration limit has been exceeded.\n");
  }
  else if (exitCode == LPX_E_TMLIM)
  {
    fprintf(file,
        "The search was prematurely terminated because the execution time\n");
    fprintf(file, "limit has been exceeded.\n");
  }
  else if (exitCode == LPX_E_SING)
  {
    fprintf(file,
        "The solver failed because the current basis matrix became\n");
    fprintf(file, "singular or ill-conditioned.\n");
  }

  fclose(file);
  free(fileName);
  return (jboolean) JNI_FALSE;
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    nativeObjectiveValue
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL
Java_LBJ2_jni_GLPKHook_nativeObjectiveValue(JNIEnv* environment, jobject this)
{
  return (jdouble) lpx_mip_obj_val(getProblem(environment, this));
}


/*
 * Class:     LBJ2_jni_GLPKHook
 * Method:    columnPrimalValueOf
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL
Java_LBJ2_jni_GLPKHook_columnPrimalValueOf
(JNIEnv* environment, jobject this, jint i)
{
  return
    (jdouble) lpx_mip_col_val(getProblem(environment, this), (int) (i + 1));
}

