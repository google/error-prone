#!/bin/bash

FILES=$( find ../src/test/java -name "*.java" )

for f in $FILES;
do

fname=${f##*/}
class=${fname%.*}

package=$( grep "^package" $f | sed 's/package //' | sed 's/;//' )

methods=$( grep @Test -A 1 $f | grep "public void" )

while IFS= read -r line
do

method=$( sed 's/().*//' <<< $line )
method=$( sed 's/.*void //' <<< $method )

if ! [ -z "$method" ]
then

echo -e "$package.$class#$method" >> testnames.txt

fi

done <<< "$methods"

done

exit 0
