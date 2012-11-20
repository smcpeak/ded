# ded/Makefile

all:
	@echo "No compile rule defined yet."

check:
	java -cp bin -ea ded.model.SerializationTests tests/*.er

# EOF
