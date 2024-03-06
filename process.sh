#!/bin/bash

BASEDIR=$(dirname "$0")
if [ -d $1 ]; then
  SOURCE="$1/*.sql"
else
  SOURCE=$1
fi

TMP_OUT=$BASEDIR/target/seqeline.out.xml
TMP_ERR=$BASEDIR/target/seqeline.err.txt

MODEL_DIR=$BASEDIR/data/model

for path in $SOURCE; do
  filename=$(basename $path)
  echo -n "Processing $filename ... "
  deps/pmd/bin/pmd ast-dump -l plsql --file $path > $TMP_OUT 2> $TMP_ERR
  if [ $? == 0 ]; then
    echo "done."
    cp $TMP_OUT $MODEL_DIR/$filename.xml
  else
    echo "ERROR!"
    cp $TMP_ERR $MODEL_DIR/$filename.err.txt
  fi
done

#for i in data/model/*.xml; do
#   mvn exec:java -Dexec.mainClass=ch.post.tools.seqeline.Main -Dexec.args="post.ch padasa $i"
#   output=$(basename $i)
#   cp target/out.trig data/target/${output/.sql.xml/.trig}
#done