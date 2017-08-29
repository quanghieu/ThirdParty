#include <jni.h>
#include <stdio.h>
#include "MyJniFunc.h"
#include "key_hom.h"
 
// Implementation of native method sayHello() of HelloJNI class
JNIEXPORT void JNICALL Java_MyJniFunc_Decrypt(JNIEnv *env, jclass class, jstring in, jstring key, jstring out) 
{
   printf("Hello World!\n");
   const char *strIn = (*env)->GetStringUTFChars(env, in, 0);
   const char *strKey = (*env)->GetStringUTFChars(env, key, 0);
   const char *strOut = (*env)->GetStringUTFChars(env, out, 0);
   full_decrypt(strIn, strKey, strOut);
   return;
}
