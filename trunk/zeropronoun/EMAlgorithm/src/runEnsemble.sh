#./runILP.sh all
./compile.sh
path=../lib/jnisvmlight.jar:../lib/stanford-classifier-3.2.0.jar:../lib/lpsolve55j.jar:.
java -Xmx32g -cp $path em/MaxEntLearnMoreTrainData
cd /users/yzcchen/tool/YASMET
cat ~/chen3/zeroEM/EMAlgorithm/src/yasmet.train | ./a.out -deltaPP 0.0 -iter 100 > WT
cat ~/chen3/zeroEM/EMAlgorithm/src/yasmetAZP.train | ./a.out -deltaPP 0.0 -iter 100 > WTAZP

cat ~/chen3/zeroEM/EMAlgorithm/src/yasmetCR.train | ./a.out -deltaPP 0.0 -iter 100 > WTCR
cat ~/chen3/zeroEM/EMAlgorithm/src/yasmetAZPCR.train | ./a.out -deltaPP 0.0 -iter 100 > WTAZPCR

cp /users/yzcchen/tool/YASMET/WT /dev/shm/
cp /users/yzcchen/tool/YASMET/WTAZP /dev/shm/

cp /users/yzcchen/tool/YASMET/WTCR /dev/shm/
cp /users/yzcchen/tool/YASMET/WTAZPCR /dev/shm/

cd /users/yzcchen/chen3/zeroEM/EMAlgorithm/src
java -Xmx32g -cp $path -Djava.library.path=../lib/ux64/  em/ApplyMaxEntMoreTrainingData $1 classify both

#java -Xmx16g -cp ../lib/lpsolve55j.jar:.:../lib/stanford-classifier-3.2.0.jar -Djava.library.path=../lib/ux64/  em/ApplyMaxEntMoreTrainingData $1 prepare
cd /users/yzcchen/tool/YASMET

#cat ~/chen3/zeroEM/EMAlgorithm/src/ante.test$1 |  /users/yzcchen/tool/YASMET/./a.out /dev/shm/WT 1>ante.rs$1
#cat ~/chen3/zeroEM/EMAlgorithm/src/ante.test$1 |  /users/yzcchen/tool/YASMET/./a.out /dev/shm/WTAZP 1>anteAZP.rs$1

cd /users/yzcchen/chen3/zeroEM/EMAlgorithm/src
#java -Xmx16g -cp ../lib/lpsolve55j.jar:.:../lib/stanford-classifier-3.2.0.jar -Djava.library.path=../lib/ux64/  em/ApplyMaxEntMoreTrainingData $1 load



#Recall: 47.8108581436077
#Precision: 48.31858407079646
#F-score: 48.063380281690144

