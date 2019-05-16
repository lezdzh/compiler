cd "$(dirname "$0")"
java -classpath antlr-4.7.2-complete.jar org.antlr.v4.Tool mxstar.g4
javac -classpath antlr-4.7.2-complete.jar *.java
