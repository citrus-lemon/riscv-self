default: compile

base_dir   = $(abspath .)
src_dir    = $(base_dir)/src/main/scala/
gen_dir    = $(base_dir)/generated-src
test_dir   = $(base_dir)/test_run_dir

SBT       = sbt
SBT_FLAGS =

PYTHON = python3

sbt:
	$(SBT) $(SBT_FLAGS)

compile: $(wildcard $(src_dir)/*.scala) $(base_dir)/src/main/scala/Constants.scala
	$(SBT) $(SBT_FLAGS) "runMain rself.Compile $(gen_dir)"

$(base_dir)/src/main/scala/Constants.scala:
	$(PYTHON) $(base_dir)/scripts/constants.py

test: $(base_dir)/src/test/scala/Constants.scala
	$(SBT) test

test-%: $(base_dir)/src/test/scala/Constants.scala
	$(SBT) "testOnly rself.$(patsubst test-%,%,$@)Tests"

$(base_dir)/src/test/scala/Constants.scala:
	$(PYTHON) $(base_dir)/scripts/testconst.py

clean:
	rm -rf $(gen_dir) $(test_dir) $(base_dir)/src/test/scala/Constants.scala $(base_dir)/src/main/scala/Constants.scala