#!/bin/bash

passcount=0
failcount=0

while read line ;
do

echo "Executing: $line"

temppath="./core/instrument/cct/$line.tmp"

mvn -Dtest=$line -DfailIfNoTests=false surefire:test > $temppath

cct=$( awk '/Tests run: 1/{f=0} f; /Running/{f=1}' < $temppath )

result=$( grep "FAILED" $temppath )
if [ -z $result ]
then

echo -e "$cct" > ./core/instrument/cct/pass/$passcount.cct
echo "Passed ($passcount.cct)"
passcount=$((passcount+1))

else

echo -e "$cct" > ./core/instrument/cct/fail/$failcount.cct
echo "Failed ($failcount.cct)"
failcount=$((failcount+1))

fi 

rm $temppath

done < ./core/instrument/testnames.txt

echo "Done. Wrote all calling context trees to ./core/instrument/cct/"
echo "$passcount tests passed, $failcount tests failed"

