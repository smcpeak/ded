# ded/Makefile

JAVA_FILES := $(shell find src -name '*.java')

all:
	mkdir -p bin
	javac -sourcepath src -d bin $(JAVA_FILES)
	cp src/ded/ui/*.png bin/ded/ui/
	mkdir -p dist
	cd bin && jar cfm ../dist/ded.jar ../src/MANIFEST.MF *

clean:
	rm -rf bin dist

check:
	java -cp bin -ea ded.model.SerializationTests tests/*.er

# ------------------- Coverity -----------------
PREV_ROOT := $(HOME)/enc/prevent-git/objs/linux64/root
PREV_CONFIG := cov/config/coverity_config.xml

$(PREV_CONFIG):
	mkdir -p cov/config
	$(PREV_ROOT)/bin/cov-configure -c $(PREV_CONFIG) --java

cov-build: $(PREV_CONFIG)
	$(PREV_ROOT)/bin/cov-build -c $(PREV_CONFIG) --dir cov/dir make clean all

cov-analyze:
	$(PREV_ROOT)/bin/cov-analyze-java --dir cov/dir --all -j 8

cov-format:
	$(PREV_ROOT)/bin/cov-format-errors --dir cov/dir
	@echo "See cov/dir/java/output/errors/index.html"

# EOF
