#!/bin/bash
gcc -o libdecrypt_hom.so -lc -shared -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux myJniFunc.c decrypt.c -lgmp
