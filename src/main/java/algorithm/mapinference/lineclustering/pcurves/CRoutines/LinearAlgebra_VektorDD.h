/* DO NOT EDIT THIS FILE - it is machine generated */
#include "jni.h"
/* Header for class LinearAlgebra_VektorDD */

#ifndef _Included_LinearAlgebra_VektorDD
#define _Included_LinearAlgebra_VektorDD
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     LinearAlgebra_VektorDD
 * Method:    InitializeCoordsC
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL Java_LinearAlgebra_VektorDD_InitializeCoordsC
  (JNIEnv *, jobject, jdoubleArray);

/*
 * Class:     LinearAlgebra_VektorDD
 * Method:    UpdateC
 * Signature: ([D[D)V
 */
JNIEXPORT void JNICALL Java_LinearAlgebra_VektorDD_UpdateC
  (JNIEnv *, jobject, jdoubleArray, jdoubleArray);

/*
 * Class:     LinearAlgebra_VektorDD
 * Method:    MulVektorialC
 * Signature: ([D[D)D
 */
JNIEXPORT jdouble JNICALL Java_LinearAlgebra_VektorDD_MulVektorialC
  (JNIEnv *, jobject, jdoubleArray, jdoubleArray);

/*
 * Class:     LinearAlgebra_VektorDD
 * Method:    AddEqualC
 * Signature: ([D[D)V
 */
JNIEXPORT void JNICALL Java_LinearAlgebra_VektorDD_AddEqualC
  (JNIEnv *, jobject, jdoubleArray, jdoubleArray);

/*
 * Class:     LinearAlgebra_VektorDD
 * Method:    SubEqualC
 * Signature: ([D[D)V
 */
JNIEXPORT void JNICALL Java_LinearAlgebra_VektorDD_SubEqualC
  (JNIEnv *, jobject, jdoubleArray, jdoubleArray);

/*
 * Class:     LinearAlgebra_VektorDD
 * Method:    MulEqualC
 * Signature: ([DD)V
 */
JNIEXPORT void JNICALL Java_LinearAlgebra_VektorDD_MulEqualC
  (JNIEnv *, jobject, jdoubleArray, jdouble);

/*
 * Class:     LinearAlgebra_VektorDD
 * Method:    DivEqualC
 * Signature: ([DD)V
 */
JNIEXPORT void JNICALL Java_LinearAlgebra_VektorDD_DivEqualC
  (JNIEnv *, jobject, jdoubleArray, jdouble);

/*
 * Class:     LinearAlgebra_VektorDD
 * Method:    Norm2C
 * Signature: ([D)D
 */
JNIEXPORT jdouble JNICALL Java_LinearAlgebra_VektorDD_Norm2C
  (JNIEnv *, jobject, jdoubleArray);

/*
 * Class:     LinearAlgebra_VektorDD
 * Method:    Norm2SquaredC
 * Signature: ([D)D
 */
JNIEXPORT jdouble JNICALL Java_LinearAlgebra_VektorDD_Norm2SquaredC
  (JNIEnv *, jobject, jdoubleArray);

/*
 * Class:     LinearAlgebra_VektorDD
 * Method:    Dist2C
 * Signature: ([D[D)D
 */
JNIEXPORT jdouble JNICALL Java_LinearAlgebra_VektorDD_Dist2C
  (JNIEnv *, jobject, jdoubleArray, jdoubleArray);

/*
 * Class:     LinearAlgebra_VektorDD
 * Method:    Dist2SquaredC
 * Signature: ([D[D)D
 */
JNIEXPORT jdouble JNICALL Java_LinearAlgebra_VektorDD_Dist2SquaredC
  (JNIEnv *, jobject, jdoubleArray, jdoubleArray);

#ifdef __cplusplus
}
#endif
#endif
