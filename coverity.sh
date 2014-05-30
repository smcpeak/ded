#!/bin/sh
# coverity.sh: Run Coverity Desktop Analysis on specified files.


# ---------------------------------------------------------
# Required parameters in order to run analysis.

# Location of the Coverity analysis tools installation.
# Can be set externally as an environment variable.
if [ "x$COV_ANALYSIS_TOOLS" = "x" ]; then
  COV_ANALYSIS_TOOLS="$HOME/opt/cov-analysis-linux64-7.5.0-fdt-alpha4"
fi

# Host name of the Coverity Connect server.
COV_HOST="d-linux64-03.sf.coverity.com"

# HTTP or HTTPS port number of the CC server, depending on
# COV_USE_SSL.  Defaults are 8080 and 8443, respectively.
COV_PORT="5122"

# "true" to use SSL, meaning $COV_PORT is the HTTPS port,
# or "false" to use unencrypted HTTP.
COV_USE_SSL="false"

# Directory for Coverity generated artifacts.  This should be on fast,
# local storage.  It typically needs about as much space as your build
# outputs (object files, bytecode files, etc.) take.
#
# This does not start with the word "coverity" because that would add
# an annoying extra step when using command line completion to run
# this script.
COV_DATA="data-coverity"

# Stream on CC that contains summary information for this code base.
COV_STREAM="ded"


# ---------------------------------------------------------
# Required parameters in order to use --configure.

# Command line arguments to cov-configure.
COV_CONFIGURE_ARGS1="--gcc"

# If non-empty, arguments for additional invocations.
COV_CONFIGURE_ARGS2="--java"
COV_CONFIGURE_ARGS3=""


# ---------------------------------------------------------
# Required parameters in order to use --build.

# Command to run a clean, full build of all the source code that you
# might later want to analyze.
COV_CLEAN_BUILD="make clean all"


# ---------------------------------------------------------
# Required parameters in order to use --create-auth-key.

# Username on CC.  This can be set as an envvar ahead of time if desired.
if [ "x$COV_USER" = "x" ]; then
  COV_USER="$USER"            # unix
  if [ "x$COV_USER" = "x" ]; then
    COV_USER="$USERNAME"      # windows
  fi
fi


# ---------------------------------------------------------
# Variables computed from the above; not usually customized.

# Location of the intermediate directory.
COV_IDIR="$COV_DATA/idir"

# Location of the compiler configuration file.
COV_CONFIG_XML="$COV_DATA/config/coverity_config.xml"

# Location of the authentication key.
COV_AUTH_KEY="$COV_DATA/auth-key"

# Location of cov-manage-im.
COV_MANAGE_IM="$COV_ANALYSIS_TOOLS/bin/cov-manage-im"


# ---------------------------------------------------------
# You can put additional variable settings into a file in the
# Coverity data directory.
if [ -f "$COV_DATA/extra-settings.sh" ]; then
  . "$COV_DATA/extra-settings.sh"
fi


# ---------------------------------------------------------
# No more parameters below this point.


# How to specify the port.
if $COV_USE_SSL; then
  COV_PORT_ARG1="--port $COV_PORT --ssl"
  COV_PORT_ARG2="--https-port $COV_PORT"
  PROTO_SCHEME="https"
else
  COV_PORT_ARG1="--port $COV_PORT"
  COV_PORT_ARG2="--port $COV_PORT"
  PROTO_SCHEME="http"
fi


# Check that the main variable is set.
if [ ! -e "$COV_ANALYSIS_TOOLS/bin/cov-configure" ]; then
  echo "File does not exist: $COV_ANALYSIS_TOOLS/bin/cov-configure"
  echo "This probably means that the COV_ANALYSIS_TOOLS variable in the"
  echo "script \"$0\" is not set properly."
  echo "You can set it as an environment variable or modify that script."
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
  echo "  $PROTO_SCHEME://$COV_HOST:$COV_PORT/docs/en/desktop_analysis_user_guide.html"
  echo ""
  exit 2
fi

# Decide what to do based on first argument.
case "$1" in
  # ------------- essential desktop setup --------------
  --configure)
    echo "$COV_ANALYSIS_TOOLS/bin/cov-configure" -c "$COV_CONFIG_XML" $COV_CONFIGURE_ARGS1
    "$COV_ANALYSIS_TOOLS/bin/cov-configure" -c "$COV_CONFIG_XML" $COV_CONFIGURE_ARGS1 || exit
    if [ "x$COV_CONFIGURE_ARGS2" != "x" ]; then
      echo "$COV_ANALYSIS_TOOLS/bin/cov-configure" -c "$COV_CONFIG_XML" $COV_CONFIGURE_ARGS2
      "$COV_ANALYSIS_TOOLS/bin/cov-configure" -c "$COV_CONFIG_XML" $COV_CONFIGURE_ARGS2 || exit
    fi
    if [ "x$COV_CONFIGURE_ARGS3" != "x" ]; then
      echo "$COV_ANALYSIS_TOOLS/bin/cov-configure" -c "$COV_CONFIG_XML" $COV_CONFIGURE_ARGS3
      "$COV_ANALYSIS_TOOLS/bin/cov-configure" -c "$COV_CONFIG_XML" $COV_CONFIGURE_ARGS3 || exit
    fi
    exit 0
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
    echo "At the prompt, enter the Coverity Connect password for \"$COV_USER\"."
    if uname -o 2>/dev/null | grep -i cygwin >/dev/null; then
      echo "NOTE: The password prompt does not always work under Cygwin.  Use --password."   # bug 64255
    fi
    echo "$COV_MANAGE_IM" --host "$COV_HOST" $COV_PORT_ARG1 \
           --user "$COV_USER" --mode auth-key --create --output-file "$COV_AUTH_KEY"
    exec "$COV_MANAGE_IM" --host "$COV_HOST" $COV_PORT_ARG1 \
           --user "$COV_USER" --mode auth-key --create --output-file "$COV_AUTH_KEY"
    ;;

  # -------------- other convenience commands --------------
  --list)
    echo "$COV_ANALYSIS_TOOLS/bin/cov-manage-emit" --dir "$COV_IDIR" list
    exec "$COV_ANALYSIS_TOOLS/bin/cov-manage-emit" --dir "$COV_IDIR" list
    ;;

  --get-central-triage)
    echo "$COV_MANAGE_IM" --host "$COV_HOST" $COV_PORT_ARG1 --stream "$COV_STREAM" \
           --auth-key-file "$COV_AUTH_KEY" --mode defects --show
    exec "$COV_MANAGE_IM" --host "$COV_HOST" $COV_PORT_ARG1 --stream "$COV_STREAM" \
           --auth-key-file "$COV_AUTH_KEY" --mode defects --show
    ;;

  --mark-fp)
    shift
    if [ "x$2" = "x" ]; then
      echo "usage: $0 --mark-fp <CID> <explanation>"
      exit 2
    fi
    echo "$COV_MANAGE_IM" --host "$COV_HOST" $COV_PORT_ARG1 --stream "$COV_STREAM" \
           --auth-key-file "$COV_AUTH_KEY" --mode defects --update \
           --cid "$1" --set "classification:False Positive" --set "comment:$2"
    exec "$COV_MANAGE_IM" --host "$COV_HOST" $COV_PORT_ARG1 --stream "$COV_STREAM" \
           --auth-key-file "$COV_AUTH_KEY" --mode defects --update \
           --cid "$1" --set "classification:False Positive" --set "comment:$2"
    ;;

  --mark-int)
    shift
    if [ "x$2" = "x" ]; then
      echo "usage: $0 --mark-int <CID> <explanation>"
      exit 2
    fi
    echo "$COV_MANAGE_IM" --host "$COV_HOST" $COV_PORT_ARG1 --stream "$COV_STREAM" \
           --auth-key-file "$COV_AUTH_KEY" --mode defects --update \
           --cid "$1" --set "classification:Intentional" --set "comment:$2"
    exec "$COV_MANAGE_IM" --host "$COV_HOST" $COV_PORT_ARG1 --stream "$COV_STREAM" \
           --auth-key-file "$COV_AUTH_KEY" --mode defects --update \
           --cid "$1" --set "classification:Intentional" --set "comment:$2"
    ;;

  # -------------- whole program build+analyze+commit --------------
  --full-build)
    if [ ! -e "$COV_CONFIG_XML" ]; then
      echo "File does not exist: $COV_CONFIG_XML"
      echo "Perhaps you need to run \"$0 --configure\"."
      exit 2
    fi
    echo "$COV_ANALYSIS_TOOLS/bin/cov-build" -c "$COV_CONFIG_XML" --dir "$COV_IDIR" $COV_CLEAN_BUILD
    exec "$COV_ANALYSIS_TOOLS/bin/cov-build" -c "$COV_CONFIG_XML" --dir "$COV_IDIR" $COV_CLEAN_BUILD
    ;;

  --full-analysis)
    COV_STRIP_PATH=`pwd` || exit
    echo "$COV_ANALYSIS_TOOLS/bin/cov-analyze" --strip-path "$COV_STRIP_PATH" --include-java --dir "$COV_IDIR"
    exec "$COV_ANALYSIS_TOOLS/bin/cov-analyze" --strip-path "$COV_STRIP_PATH" --include-java --dir "$COV_IDIR"
    ;;

  --full-commit)
    echo "$COV_ANALYSIS_TOOLS/bin/cov-commit-defects" --dir "$COV_IDIR" --auth-key-file "$COV_AUTH_KEY" \
      --host "$COV_HOST" $COV_PORT_ARG2 --stream "$COV_STREAM"
    exec "$COV_ANALYSIS_TOOLS/bin/cov-commit-defects" --dir "$COV_IDIR" --auth-key-file "$COV_AUTH_KEY" \
      --host "$COV_HOST" $COV_PORT_ARG2 --stream "$COV_STREAM"
    ;;

  --full-b-a-c)
    sh "$0" --full-build || exit
    sh "$0" --full-analysis || exit
    sh "$0" --full-commit || exit
    exit 0
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
  --host "$COV_HOST" $COV_PORT_ARG1 --stream "$COV_STREAM" "$@"
exec "$COV_ANALYSIS_TOOLS/bin/cov-run-desktop" --dir "$COV_IDIR" --auth-key-file "$COV_AUTH_KEY" \
  --host "$COV_HOST" $COV_PORT_ARG1 --stream "$COV_STREAM" "$@"

# EOF
