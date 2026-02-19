.PHONY: build run clean

CP = .:libs/*

build:
	javac -cp "$(CP)" *.java

run:
	java -cp "$(CP)" Tema1 $(ARGS)

clean:
	rm -f *.class