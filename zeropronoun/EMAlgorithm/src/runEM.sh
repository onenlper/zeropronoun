./compile.sh
#java -Xmx10g ngram/Ngram
#java -Xmx10g ngram/PatternAnalysis
#java -Xmx10g ngram/PatternGenerate
java -Xmx16g -cp ../lib/stanford-classifier-3.2.0.jar:. em/EMLearn
