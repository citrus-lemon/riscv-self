default: compile

base_dir   = $(abspath .)
src_dir    = $(base_dir)/src/main/scala/
gen_dir    = $(base_dir)/generated-src
test_dir   = $(base_dir)/test_run_dir

SBT       = sbt
SBT_FLAGS =

SH = /bin/sh

PYTHON = python3

sbt:
	$(SBT) $(SBT_FLAGS)

compile: $(wildcard $(src_dir)/*.scala) $(base_dir)/src/main/scala/Constants.scala
	$(SBT) $(SBT_FLAGS) "runMain rself.Compile $(gen_dir)"

test: $(base_dir)/src/test/scala/Constants.scala $(base_dir)/src/main/scala/Constants.scala
	$(SBT) test
	$(SH) $(base_dir)/scripts/link_latest.sh

test-%: $(base_dir)/src/test/scala/Constants.scala $(base_dir)/src/main/scala/Constants.scala
	$(SBT) "testOnly rself.$(patsubst test-%,%,$@)Tests"
	$(SH) $(base_dir)/scripts/link_latest.sh $(patsubst test-%,%,$@)Tester

constants:
	$(PYTHON) $(base_dir)/scripts/testconst.py
	$(PYTHON) $(base_dir)/scripts/constants.py

$(base_dir)/src/test/scala/Constants.scala: $(wildcard $(base_dir)/scripts/{testconst,opcodes,utility}.py)
	$(PYTHON) $(base_dir)/scripts/testconst.py

$(base_dir)/src/main/scala/Constants.scala: $(wildcard $(base_dir)/scripts/{constants,opcodes,utility}.py)
	$(PYTHON) $(base_dir)/scripts/constants.py

clean:
	rm -rf $(gen_dir) $(test_dir) $(base_dir)/src/test/scala/Constants.scala $(base_dir)/src/main/scala/Constants.scala