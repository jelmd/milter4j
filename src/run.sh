#!/bin/ksh

JAVA_HOME=${JAVA_HOME:-/local/apps/jdk}

# additional flags for the java virtual machine
JVM_FLAGS="-Djava.awt.headless=true -server"

if [ "$1" != "shutdown" ]; then
	# Enable JMX to retrieve server information and statistics on demand
	#	JMX clients connect to this TCP port to access the RMI registry
	JVM_FLAGS="$JVM_FLAGS -Dcom.sun.management.jmxremote.port=12345"
	#	TCP port used for JMX RMI client connections. If not set, a random
	#	port is selected as needed, which can be difficult to handle properly
	#	with firewalls.
	#JVM_FLAGS="$JVM_FLAGS -Dcom.sun.management.jmxremote.rmi.port=12346"
	#	Force JMX clients to use this IP or hostname/FQDN
	#JVM_FLAGS="$JVM_FLAGS -Djava.rmi.server.hostname=127.0.0.1"
	#	If a firewall is in place which allows only trusted clients within the
	#	local network to connect, no authentication and no traffic encryption is
	#	probably ok. If in doubts, inverse these settings.
	JVM_FLAGS="$JVM_FLAGS -Dcom.sun.management.jmxremote.authenticate=false"
	JVM_FLAGS="$JVM_FLAGS -Dcom.sun.management.jmxremote.ssl=false"
	#	No dynamic class loading from remote codebases (security best practice).
	JVM_FLAGS="$JVM_FLAGS -Djava.rmi.server.useCodebaseOnly=true"
	#	For additional options, consult the JMX documentation or ask ChatGPT.
	
	# Just in case someone wants to attach a remote debugger:
	#JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,address=45678,server=y,suspend=n"
fi

#=========================================================================
# no further changes required
#=========================================================================
START_CLASS=Server

export LC_CTYPE=de_DE

Usage() {
cat<<EOF
Usage: ${0} [-h] [shutdown]

    -h        print this help and exit
    shutdown  stop the currently running instance
EOF
}

JAVA=`which java 2>/dev/null`
if [ -n "$JAVA_HOME" ]; then
	if [ -x ${JAVA_HOME}/bin/java ]; then
		JAVA=${JAVA_HOME}/bin/java
	fi
fi

if [ ! -x "$JAVA" ]; then
	echo "JVM not found. Set JAVA_HOME or PATH env variable!"
	exit 1
fi

if [ -z "$JVM_FLAGS" ]; then
	JVM_FLAGS=" "
fi

while getopts "h" option ; do
	case "$option" in
		h) Usage; exit 0 ;;
	esac
done	
X=$((OPTIND-1))
shift $X

# resolv links to base directory
PRG=$0
progname=`basename $0`
while [ -h "$PRG" ] ; do
	ls=`ls -ld "$PRG"`
	link=`expr "$ls" : '.*-> \(.*\)$'`
	if expr "$link" : '.*/.*' > /dev/null; then
		PRG="$link"
	else
		PRG="`dirname $PRG`/$link"
	fi
done
BASE_DIR=`dirname "$PRG"`/..

if [ ! -d "${BASE_DIR}/lib" ]; then
	echo "Base directory $BASE_DIR/lib does not exist. Exiting"
	exit 2
fi

# add in the dependency .jar files
DIRLIBS=${BASE_DIR}/lib/*.jar
LOCAL_CLASSPATH="${BASE_DIR}/lib"
for i in $DIRLIBS ; do
    # if the directory is empty, then it will return the input string
    # this is stupid, so case for it
	if [ "$i" != "${DIRLIBS}" ] ; then
		LOCAL_CLASSPATH="$i":$LOCAL_CLASSPATH
	fi
done

if [ -n "$LOCAL_CLASSPATH" ]; then
	LOCAL_CLASSPATH="-cp ${LOCAL_CLASSPATH}"
fi

if [ -d "${BASE_DIR}/lib/endorsed" ]; then
	if [ -z "$JVM_FLAGS" ]; then
		JVM_FLAGS="-Djava.endorsed.dirs=${BASE_DIR}/lib/endorsed"
	else
		JVM_FLAGS="$JVM_FLAGS -Djava.endorsed.dirs=${BASE_DIR}/lib/endorsed"
	fi
fi

exec $JAVA ${JVM_FLAGS} ${JAVA_OPTS} $LOCAL_CLASSPATH de.ovgu.cs.milter4j.$START_CLASS $@
