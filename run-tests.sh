#!/bin/bash

passcount=0
failcount=0
tmpresult="./core/instrument/cct/test_results.tmp"
tmpcct="./core/instrument/cct/test_cct.tmp"

trap "exit" INT

while read line;
do

echo "Executing: $line"

mvn -Dtest=$line -DfailIfNoTests=false surefire:test 2> $tmpcct 1> $tmpresult

result=$( grep "FAILED" $tmpresult )
if [ -z $result ]
then

mv $tmpcct ./core/instrument/cct/pass/$passcount.cct
echo "Passed ($passcount.cct)"
passcount=$((passcount+1))

else

mv $tmpcct ./core/instrument/cct/fail/$failcount.cct
echo "Failed ($failcount.cct)"
failcount=$((failcount+1))

fi 

done < ./core/instrument/testnames.txt

rm $tmpresult
rm $tmpcct

echo "Done. Wrote all calling context trees to ./core/instrument/cct/"
echo "$passcount tests passed, $failcount tests failed"

