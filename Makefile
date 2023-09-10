# ded/Makefile

all: dist/ded.jar

JAVA_FILES := $(shell find src -name '*.java')
RESOURCE_FILES := $(shell find resources -type f)

PYTHON3 := python3
RUN_COMPARE_EXPECT := $(PYTHON3) ./run-compare-expect.py

# Ensure the directory meant to hold the output file of a recipe exists.
CREATE_OUTPUT_DIRECTORY = @mkdir -p $(dir $@)


# Eliminate all implicit rules.
.SUFFIXES:

# Delete a target when its recipe fails.
.DELETE_ON_ERROR:

# Do not remove "intermediate" targets.
.SECONDARY:


dist/ded.jar: $(JAVA_FILES) $(RESOURCE_FILES)
	rm -rf bin
	mkdir -p bin
	javac -sourcepath src -d bin $(JAVA_FILES)
	cp src/ded/ui/*.png bin/ded/ui/
	mkdir -p bin/resources
	cp -r resources/* bin/resources
	git log -n 1 --format=format:'%h %ai%n' > bin/resources/version.txt
	mkdir -p dist
	cd bin && jar cfm ../dist/ded.jar ../src/MANIFEST.MF *

.PHONY: clean all check
clean:
	rm -rf bin dist out

# Unit tests that do not require a GUI.
check: dist/ded.jar
	java -cp bin -ea util.awt.BDFParser
	java -cp bin -ea ded.model.DiagramTests
	java -cp bin -ea ded.model.SerializationTests
	java -cp bin -ea ded.model.SerializationTests tests/*.ded
	java -cp bin -ea ded.model.SerializationTests tests/*.er
	java -cp bin -ea ded.ui.GraphNodeDialogTests
	java -cp bin -ea ded.ui.ObjectGraphSizesDialogTests
	java -cp bin -ea util.IdentityHashSetTests
	java -cp bin -ea util.UtilTests
	java -cp bin -ea util.WrapTextTests
	java -cp bin -ea util.StringUtilTests
	make -C tests/image-map check


# ---- Tests using --check-graph ----
.PHONY: check-graphs
check-graphs: out/check-graph.ded.cg
check-graphs: out/check-graph-fixed.ded.cg
check-graphs: out/objgraph.ded.cg
check: check-graphs

out/%.ded.cg: dist/ded.jar tests/%.ded tests/%.ded.cg.exp
	$(CREATE_OUTPUT_DIRECTORY)
	$(RUN_COMPARE_EXPECT) \
	  --expect tests/$*.ded.cg.exp \
	  --actual $@ \
	  java -cp bin -ea ded.Ded --check-graph tests/$*.ded

# Create an empty expected output if needed.
tests/%.exp:
	touch $@


# ---- Tests using --check-graph-source ----
.PHONY: check-graph-source
check-graph-source: out/objgraph.ded.cgs
check: check-graph-source

out/%.ded.cgs: dist/ded.jar tests/%.ded tests/%.ded.cgs.exp
	$(CREATE_OUTPUT_DIRECTORY)
	$(RUN_COMPARE_EXPECT) \
	  --expect tests/$*.ded.cgs.exp \
	  --actual $@ \
	  java -cp bin -ea ded.Ded --check-graph-source tests/$*.ded


# ---- Tests using both --check-source and --check-graph-source ----
.PHONY: check-graph-both
check-graph-both: out/objgraph-fixed.ded.cgb
check: check-graph-both

out/%.ded.cgb: dist/ded.jar tests/%.ded tests/%.ded.cgb.exp
	$(CREATE_OUTPUT_DIRECTORY)
	$(RUN_COMPARE_EXPECT) \
	  --expect tests/$*.ded.cgb.exp \
	  --actual $@ \
	  java -cp bin -ea ded.Ded --check-graph \
	    --check-graph-source tests/$*.ded


# ------------------- Coverity -----------------
PREV_ROOT := $(HOME)/enc/prevent-current/objs/linux64/root
PREV_CONFIG := cov/config/coverity_config.xml

$(PREV_CONFIG):
	mkdir -p cov/config
	$(PREV_ROOT)/bin/cov-configure -c $(PREV_CONFIG) --java

cov-build: $(PREV_CONFIG)
	$(PREV_ROOT)/bin/cov-build -c $(PREV_CONFIG) --dir cov/dir make clean all

cov-analyze:
	$(PREV_ROOT)/bin/cov-analyze-java --dir cov/dir --all -j 8 --export-summaries true

cov-format:
	$(PREV_ROOT)/bin/cov-format-errors --dir cov/dir
	@echo "See cov/dir/java/output/errors/index.html"

# EOF
