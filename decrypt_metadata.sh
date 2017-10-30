#!/bin/bash

file=$(pwd)/$1

metadata=$(pwd)/$2
#guid=$2

cd /home/hieu/Downloads/libfenc/tools/

#echo "guid is: $guid"
#read -r line<$file

#line=$line$guid
 
#printf '%s\n' "$line"

#START_TIME=`date +%s.%N`
#./abe-keygen -m CP -a 'ONE,TWO,THREE,FOUR,FIVE,SIX.SEVEN,EIGHT' -o /home/hieu/Downloads/piwigo_client/iusr_privCP_Java.key
#END_TIME=`date +%s.%N`
#TIMEDIFF=`echo "$END_TIME - $START_TIME" | bc | awk -F"." '{print $1"."substr($2,1,3)}'`
#echo "TIME DIFF keygen is: $TIMEDIFF"

#START_TIME=`date +%s.%N`
#./abe-enc -m CP -d "$line" -p "((ONE and TWO) or THREE)" -o /home/hieu/Downloads/piwigo_client/metadata
#END_TIME=`date +%s.%N`
#TIMEDIFF=`echo "$END_TIME - $START_TIME" | bc | awk -F"." '{print $1"."substr($2,1,3)}'`
#echo "Time diff enc is: $TIMEDIFF"


START_TIME=`date +%s.%N`
./abe-dec -m CP -k $file -f $metadata
END_TIME=`date +%s.%N`
TIMEDIFF=`echo "$END_TIME - $START_TIME" | bc | awk -F"." '{print $1"."substr($2,1,3)}'`
echo "Time diff dec is: $TIMEDIFF"


