# Major Makefile variables.
# - PACKAGES lists the names of the principal packages. These are the packages
#	that would be distributed to users. Test cases and build tools are not
#	included.
# - ARCHIVE is the name of the zip archive created by the archive target for
#   source code submission and distribution.
# - JAVAFILES is all of the Java files in the project, including test cases and
#   build tools.

PACKAGES = rmi
ARCHIVE = project1.zip
JAVAFILES = */*.java */*/*.java

# Javadoc-related variables.
# - DOCDIR gives the relative path to the directory into which the documentation
#   generated by the docs target will be placed.
# - ALLDOCDIR is the same for the docs-all target.
# - DOCLINK is the URL to Javadoc for the standard Java class library.

DOCDIR = javadoc
ALLDOCDIR = javadoc-all
DOCLINK = http://download.oracle.com/javase/6/docs/api

# Define the variable CPSEPARATOR, the classpath separator character. This is
# : on Unix-like systems and ; on Windows. The separator is returned by a
# Java program implemented in build/PathSeparator.java. The Makefile fragment
# included here is made to depend on build/PathSeparator.class to ensure that
# the program is compiled before make procedes past this line.

include build/Makefile.separator

# Source and class directory tree bases. These are given as the classpath
# argument when running unit test and as the sourcepath argument when generating
# Javadoc for all files (including unit tests). The value is quoted for Cygwin:
# the Windows Java implementation requires the path separator to be ; but
# Cygwin's bash interprets this as a separator between commands.

UNITCLASSPATH = ".$(CPSEPARATOR)unit"

# Compile all Java files.
.PHONY : all-classes
all-classes :
	javac $(JAVAFILES)

# Run unit and conformance tests.
.PHONY : test
test : all-classes
	java -cp $(UNITCLASSPATH) unit.UnitTests
	@echo
	java conformance.ConformanceTests

.PHONY : pingpong-client
pingpong-client : all-classes
	java test.PingPong.PingPongClient

.PHONY : pingpong-server
pingpong-server : all-classes
	java test.PingPong.PingPongServer

# Delete all intermediate and final output and leave only the source.
.PHONY : clean
clean :
	rm -rf $(JAVAFILES:.java=.class) *.zip $(DOCDIR) $(ALLDOCDIR)

# Generate documentation for the public interfaces of the principal packages.
.PHONY : docs
docs :
	javadoc -link $(DOCLINK) -d $(DOCDIR) $(PACKAGES)

# Generate documentation for all classes and all members in all packages.
.PHONY : docs-all
docs-all :
	javadoc -link $(DOCLINK) -private -sourcepath $(UNITCLASSPATH) \
		-d $(ALLDOCDIR) $(PACKAGES) test conformance conformance.rmi \
		conformance.common conformance.storage conformance.naming unit build

# Create a source code archive.
.PHONY : archive
archive : clean
	zip -9r $(ARCHIVE) *

# Dependencies for the Makefile fragment reporting the classpath separator.
build/Makefile.separator : build/PathSeparator.class

build/PathSeparator.class : build/PathSeparator.java
	javac build/PathSeparator.java
