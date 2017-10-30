#!/bin/bash

encrypt_loc=$(pwd)/$1
inputFile=$(pwd)/$2
keyfile=$(pwd)/$3
outputFile=$(pwd)/$4

cd $encrypt_loc
echo "cd $encrypt_loc" 
echo "./decrypt $inputFile $keyfile $outputFile"

./decrypt $inputFile $keyfile $outputFile


