#!/bin/bash

encrypt_loc=$1
inputFile=$2
keyfile=$3
outputFile=$4

cd $encrypt_loc
echo "cd $encrypt_loc" 
echo "./decrypt $inputFile $keyfile $outputFile"

./decrypt $inputFile $keyfile $outputFile


