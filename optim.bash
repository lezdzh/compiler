cd "$(dirname "$0")"
cat > ./test/test.txt
java -classpath antlr-4.7.2-complete.jar: Main
cat ./test/test.asm
