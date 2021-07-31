#!/bin/bash

function link_testfile {
  latest="$(ls -1 -t test_run_dir/"$1" | sed /Latest/d | head -n 1)"
  echo ln -sf ./"$latest" test_run_dir/"$1"/Latest
  rm -rf test_run_dir/"$1"/Latest
  rm -f  test_run_dir/"$1"/Latest.vcd
  ln -sf ./"$latest" test_run_dir/"$1"/Latest
  cp -f  test_run_dir/"$1"/"$latest"/dump.vcd test_run_dir/"$1"/Latest.vcd
}

if test -n "$1" -a -d "$(pwd)/test_run_dir/$1"
then
  link_testfile "$1"
else
  for i in test_run_dir/*/
  do
    link_testfile "$(basename "$i")"
  done
fi
