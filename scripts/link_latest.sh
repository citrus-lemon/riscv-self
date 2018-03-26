#!/bin/bash
if test -z $1
then
  for i in $(ls test_run_dir)
  do
    echo ln -sf $(pwd)/test_run_dir/$i/$(ls -1 -t test_run_dir/$i | sed /Latest/d | head -n 1) $(pwd)/test_run_dir/$i/Latest
    rm -rf $(pwd)/test_run_dir/$i/Latest
    ln -sf $(pwd)/test_run_dir/$i/$(ls -1 -t test_run_dir/$i | sed /Latest/d | head -n 1) $(pwd)/test_run_dir/$i/Latest
  done
else
  echo ln -sf $(pwd)/test_run_dir/$1/$(ls -1 -t test_run_dir/$1 | sed /Latest/d | head -n 1) $(pwd)/test_run_dir/$1/Latest
  rm -rf $(pwd)/test_run_dir/$1/Latest
  ln -sf $(pwd)/test_run_dir/$1/$(ls -1 -t test_run_dir/$1 | sed /Latest/d | head -n 1) $(pwd)/test_run_dir/$1/Latest
fi