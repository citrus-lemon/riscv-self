#!/bin/bash
pwd=$(pwd)
for i in $(ls test_run_dir)
do
  (cd test_run_dir/$i/Latest; echo $i; $(pwd)/V$i)
done
