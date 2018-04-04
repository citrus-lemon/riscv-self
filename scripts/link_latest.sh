#!/bin/bash
if test -n "$1" -a -d "$(pwd)/test_run_dir/$1"
then
  echo ln -sf $(pwd)/test_run_dir/$1/$(ls -1 -t test_run_dir/$1 | sed /Latest/d | head -n 1) $(pwd)/test_run_dir/$1/Latest
  rm -rf $(pwd)/test_run_dir/$1/Latest
  rm -f  $(pwd)/test_run_dir/$1/Latest.vcd
  ln -sf $(pwd)/test_run_dir/$1/$(ls -1 -t test_run_dir/$1 | sed /Latest/d | head -n 1) $(pwd)/test_run_dir/$1/Latest
  cp -f  $(pwd)/test_run_dir/$1/$(ls -1 -t test_run_dir/$1 | sed /Latest/d | head -n 1)/dump.vcd $(pwd)/test_run_dir/$1/Latest.vcd
else
  for i in $(ls test_run_dir)
  do
    echo ln -sf $(pwd)/test_run_dir/$i/$(ls -1 -t test_run_dir/$i | sed /Latest/d | head -n 1) $(pwd)/test_run_dir/$i/Latest
    rm -rf $(pwd)/test_run_dir/$i/Latest
    rm -f  $(pwd)/test_run_dir/$i/Latest.vcd
    ln -sf $(pwd)/test_run_dir/$i/$(ls -1 -t test_run_dir/$i | sed /Latest/d | head -n 1) $(pwd)/test_run_dir/$i/Latest
    cp -f  $(pwd)/test_run_dir/$i/$(ls -1 -t test_run_dir/$i | sed /Latest/d | head -n 1)/dump.vcd $(pwd)/test_run_dir/$i/Latest.vcd
  done
fi
