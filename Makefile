# Detect operating system
ifeq ($(OS),Windows_NT)
    # Windows commands
    RM = del /Q
    RMDIR = rmdir /S /Q
    MV = move
    MKDIR = mkdir
    SEP = \\
    PATHSEP = ;
else
    # Unix/Linux commands
    RM = rm -f
    RMDIR = rm -rf
    MV = mv
    MKDIR = mkdir -p
    SEP = /
    PATHSEP = :
endif

c: src/JSHOP2/JSHOP2.g src/JSHOP2/*.java
	cd src/JSHOP2 && java antlr.Tool JSHOP2.g && javac *.java
	cd src && jar cvf JSHOP2.jar JSHOP2/*.class
	$(RM) src$(SEP)JSHOP2$(SEP)*.class
	$(MV) src$(SEP)JSHOP2.jar bin.build$(SEP)

clean:
	$(RM) src$(SEP)JSHOP2$(SEP)JSHOP2Lexer.*
	$(RM) src$(SEP)JSHOP2$(SEP)JSHOP2Parser.*
	$(RM) src$(SEP)JSHOP2$(SEP)JSHOP2TokenTypes.java
	$(RM) src$(SEP)JSHOP2$(SEP)JSHOP2TokenTypes.txt

d: src/JSHOP2/*.java
	$(RMDIR) doc
	cd src && javadoc -d ../doc -author -version -private JSHOP2

1: bin.build/JSHOP2.jar
	cd examples/blocks && java JSHOP2.InternalDomain blocks
	cd examples/blocks && java JSHOP2.InternalDomain -r problem
	cd examples/blocks && javac blocks.java problem.java
	cd examples/blocks && javac gui.java
	cd examples/blocks && java -Xss2048K -Xmx512M gui
	cd examples/blocks && $(RM) blocks.java && $(RM) blocks.txt && $(RM) problem.java && $(RM) *.class

2: bin.build/JSHOP2.jar
	cd examples/basic && java JSHOP2.InternalDomain basic
	cd examples/basic && java JSHOP2.InternalDomain -r problem
	cd examples/basic && javac gui.java
	cd examples/basic && java gui
	cd examples/basic && $(RM) basic.java && $(RM) basic.txt && $(RM) problem.java && $(RM) *.class

3: bin.build/JSHOP2.jar
	cd examples/oldblocks && java JSHOP2.InternalDomain oldblocks
	cd examples/oldblocks && java JSHOP2.InternalDomain -r problem
	cd examples/oldblocks && javac gui.java
	cd examples/oldblocks && java gui
	cd examples/oldblocks && $(RM) oldblocks.java && $(RM) oldblocks.txt && $(RM) problem.java && $(RM) *.class

4: bin.build/JSHOP2.jar
	cd examples/test && java JSHOP2.InternalDomain test
	cd examples/test && java JSHOP2.InternalDomain -r12 problem
	cd examples/test && javac gui.java
	cd examples/test && java gui
	cd examples/test && $(RM) test.java && $(RM) test.txt && $(RM) problem.java && $(RM) *.class

5: bin.build/JSHOP2.jar
	cd examples/logistics && java JSHOP2.InternalDomain logistics
	cd examples/logistics && java JSHOP2.InternalDomain -r problem
	cd examples/logistics && javac gui.java
	cd examples/logistics && java gui
	cd examples/logistics && $(RM) logistics.java && $(RM) logistics.txt && $(RM) problem.java && $(RM) *.class

6: bin.build/JSHOP2.jar
	cd examples/freecell && java JSHOP2.InternalDomain freecell
	cd examples/freecell && java JSHOP2.InternalDomain -r problem
	cd examples/freecell && javac gui.java
	cd examples/freecell && java gui
	cd examples/freecell && $(RM) freecell.java && $(RM) freecell.txt && $(RM) problem.java && $(RM) *.class

7: bin.build/JSHOP2.jar
	cd examples/propagation && java JSHOP2.InternalDomain propagation
	cd examples/propagation && java JSHOP2.InternalDomain -r problem
	cd examples/propagation && javac gui.java
	cd examples/propagation && java gui
	cd examples/propagation && $(RM) propagation.java && $(RM) propagation.txt && $(RM) problem.java && $(RM) *.class

8: bin.build/JSHOP2.jar
	cd examples/forall && java JSHOP2.InternalDomain forall
	cd examples/forall && java JSHOP2.InternalDomain -ra problem
	cd examples/forall && javac gui.java
	cd examples/forall && java gui
	cd examples/forall && $(RM) forallexample.java && $(RM) forallexample.txt && $(RM) problem.java && $(RM) *.class

9: bin.build/JSHOP2.jar
	cd examples/rover && java JSHOP2.InternalDomain rover
	cd examples/rover && java JSHOP2.InternalDomain -r problem
	cd examples/rover && javac gui.java
	cd examples/rover && java -Xmx256M gui
	cd examples/rover && $(RM) rover.java && $(RM) rover.txt && $(RM) problem.java && $(RM) *.class

10: bin.build/JSHOP2.jar
	cd examples/blocks && java JSHOP2.InternalDomain blocks
	cd examples/blocks && java JSHOP2.InternalDomain -ra smallproblem
	cd examples/blocks && javac smallgui.java
	cd examples/blocks && java smallgui
	cd examples/blocks && $(RM) blocks.java && $(RM) blocks.txt && $(RM) smallproblem.java && $(RM) *.class

11: bin.build/JSHOP2.jar
	cd examples/madrts && java JSHOP2.InternalDomain madrts
	cd examples/madrts && java JSHOP2.InternalDomain -ra problem
	cd examples/madrts && javac gui.java
	cd examples/madrts && java gui
	cd examples/madrts && $(RM) madrts.java && $(RM) madrts.txt && $(RM) problem.java && $(RM) *.class
