# ded/Makefile

JAVA_FILES := $(shell find src -name '*.java')

all: dist/ded.jar

dist/ded.jar: $(JAVA_FILES)
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
check: dist/ded.jar check-graphs
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
check-graphs: out/check-graph.ded.cg.ok
check-graphs: out/objgraph.ded.cg.ok

out/%.ded.cg.ok: dist/ded.jar tests/%.ded
	@mkdir -p $(dir $@)
	@#
	@# Check the graph in $*.ded.
	java -cp bin -ea ded.Ded --check-graph tests/$*.ded > out/$*.ded.cg
	@#
	@# Compare to what we expect.
	diff -u tests/$*.ded.cg.exp out/$*.ded.cg
	@#
	@# Record the test as successful.
	touch $@


# GUI tests.  These require Abbot:
#
#   http://abbot.sourceforge.net/doc/overview.shtml
#
# Download and unpack it.  Then create a script in your $PATH
# called "run-abbot-test" with the line:
#
#   exec java -cp $ABBOT/lib/costello.jar junit.extensions.abbot.ScriptTestSuite "$@"
#
# where $ABBOT is where you unpacked Abbot.
#
# TODO: This test hasn't been maintained in a while and probably does
# not work.  Remove it.
uicheck:
	run-abbot-test tests/abbot/*.xml

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
