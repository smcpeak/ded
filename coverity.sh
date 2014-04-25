#!/bin/sh
# coverity.sh: Run Coverity Desktop Analysis on specified files.


# ---------------------------------------------------------
# Required parameters in order to run analysis.

# Location of the Coverity analysis tools installation.
COV_ANALYSIS_TOOLS="$HOME/prevent-gilroy/objs/linux64/root"

# Host name of the Coverity Connect server.
COV_HOST="d-linux64-03.sf.coverity.com"

# HTTP port number of the CC server.  Default is 8080.
COV_PORT="4232"

# Directory for Coverity generated artifacts.  This should be on fast,
# local storage.  It typically needs about as much space as your build
# outputs (object files, bytecode files, etc.) take.
COV_DATA="cov/desktop"

# Stream on CC that contains summary information for this code base.
COV_STREAM="ded"

# ID of the snapshot that contains summary information.
COV_SNAPSHOT_ID="10005"


# ---------------------------------------------------------
# Required parameters in order to use --configure.

# Command line arguments to cov-configure.
COV_CONFIGURE_ARGS="--java"


# ---------------------------------------------------------
# Required parameters in order to use --build.

# Command to run a clean, full build of all the source code that you
# might later want to analyze.
COV_CLEAN_BUILD="make clean all"


# ---------------------------------------------------------
# Required parameters in order to use --create-auth-key.

# Username on CC.  This can be set as an envvar ahead of time if desired.
if [ "x$COV_USER" = "x" ]; then
  COV_USER="smcpeak"
fi


# ---------------------------------------------------------
# Variables computed from the above; not usually customized.

# Location of the intermediate directory.
COV_IDIR="$COV_DATA/idir"

# Location of the compiler configuration file.
COV_CONFIG_XML="$COV_DATA/config/coverity_config.xml"

# Location of the authentication key.
COV_AUTH_KEY="$COV_DATA/auth-key"


# ---------------------------------------------------------
# No more parameters below this point.


# Check that the main variable is set.
if [ ! -e "$COV_ANALYSIS_TOOLS/bin/cov-configure" ]; then
  echo "File does not exist: $COV_ANALYSIS_TOOLS/bin/cov-configure"
  echo "This probably means that the COV_ANALYSIS_TOOLS variable in the"
  echo "script \"$0\" is not set properly."
  echo "It has several variables that have to be set before it can be used."
  exit 2
fi

# If no arguments, print usage information.
if [ "x$1" = "x" ]; then
  echo "usage: $0 --configure"
  echo "   or: $0 --build"
  echo "   or: $0 --create-auth-key"
  echo "   or: $0 [cov-run-desktop options] <files_to_analyze>"
  echo ""
  echo "To get started, run the commands in the sequence shown."
  echo ""
  echo "For more information, see:"
  echo ""
  echo "  http://$COV_HOST:$COV_PORT/docs/en/desktop_analysis_user_guide.html"
  echo ""
  exit 2
fi

# Decide what to do based on first argument.
case "$1" in
  --configure)
    echo "$COV_ANALYSIS_TOOLS/bin/cov-configure" -c "$COV_CONFIG_XML" $COV_CONFIGURE_ARGS
    exec "$COV_ANALYSIS_TOOLS/bin/cov-configure" -c "$COV_CONFIG_XML" $COV_CONFIGURE_ARGS
    ;;

  --build)
    if [ ! -e "$COV_CONFIG_XML" ]; then
      echo "File does not exist: $COV_CONFIG_XML"
      echo "Perhaps you need to run \"$0 --configure\"."
      exit 2
    fi
    echo "$COV_ANALYSIS_TOOLS/bin/cov-build" -c "$COV_CONFIG_XML" --dir "$COV_IDIR" --desktop $COV_CLEAN_BUILD
    exec "$COV_ANALYSIS_TOOLS/bin/cov-build" -c "$COV_CONFIG_XML" --dir "$COV_IDIR" --desktop $COV_CLEAN_BUILD
    ;;

  --create-auth-key)
    # This will prompt for the password.
    echo "$COV_ANALYSIS_TOOLS/bin/cov-manage-im" --host "$COV_HOST" --port "$COV_PORT" \
           --user "$COV_USER" --mode auth-key --create --output-file "$COV_AUTH_KEY"
    exec "$COV_ANALYSIS_TOOLS/bin/cov-manage-im" --host "$COV_HOST" --port "$COV_PORT" \
           --user "$COV_USER" --mode auth-key --create --output-file "$COV_AUTH_KEY"
    ;;

  # Anything else: assume it is an argument intended for cov-run-desktop.
  # This allows passing --disconnected, etc.

esac

if [ ! -d "$COV_IDIR" ]; then
  echo "Directory does not exist: $COV_IDIR"
  echo "Perhaps you need to run \"$0 --build\"."
  exit 2
fi

if [ ! -f "$COV_AUTH_KEY" ]; then
  echo "File does not exist: $COV_AUTH_KEY"
  echo "Perhaps you need to run \"$0 --create-auth-key\"."
  exit 2
fi

# Run cov-run-desktop to analyze sources.  The sources need to have
# already been captured using cov-build.
echo "$COV_ANALYSIS_TOOLS/bin/cov-run-desktop" --dir "$COV_IDIR" --auth-key-file "$COV_AUTH_KEY" \
  --host "$COV_HOST" --port "$COV_PORT" --stream "$COV_STREAM" --reference-snapshot-id "$COV_SNAPSHOT_ID" "$@"
exec "$COV_ANALYSIS_TOOLS/bin/cov-run-desktop" --dir "$COV_IDIR" --auth-key-file "$COV_AUTH_KEY" \
  --host "$COV_HOST" --port "$COV_PORT" --stream "$COV_STREAM" --reference-snapshot-id "$COV_SNAPSHOT_ID" "$@"

# EOF
