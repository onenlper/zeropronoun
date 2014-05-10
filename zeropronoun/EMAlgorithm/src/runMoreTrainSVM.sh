#./runILP.sh all
./compile.sh
path=../lib/jnisvmlight.jar:../lib/stanford-classifier-3.2.0.jar:../lib/lpsolve55j.jar:.
java -Xmx32g -cp $path em/MaxEntLearnMoreTrainData
cd /users/yzcchen/tool/YASMET

cd /users/yzcchen/tool/svmrank
./svm_rank_learn -c 30 /users/yzcchen/chen3/zeroEM/EMAlgorithm/src/svmRankAZPCR.train azpCR.model
#./svm_rank_learn -c 300 /users/yzcchen/chen3/zeroEM/EMAlgorithm/src/svmRankAZP.train azp.model

cd /users/yzcchen/chen3/zeroEM/EMAlgorithm/src
java -Xmx32g -cp $path -Djava.library.path=../lib/ux64/  em/ApplyMaxEntMoreTrainingData all classify both

#java -Xmx16g -cp ../lib/lpsolve55j.jar:.:../lib/stanford-classifier-3.2.0.jar -Djava.library.path=../lib/ux64/  em/ApplyMaxEntMoreTrainingData $1 prepare
cd /users/yzcchen/tool/YASMET

#cat ~/chen3/zeroEM/EMAlgorithm/src/ante.test$1 |  /users/yzcchen/tool/YASMET/./a.out /dev/shm/WT 1>ante.rs$1
#cat ~/chen3/zeroEM/EMAlgorithm/src/ante.test$1 |  /users/yzcchen/tool/YASMET/./a.out /dev/shm/WTAZP 1>anteAZP.rs$1

cd /users/yzcchen/chen3/zeroEM/EMAlgorithm/src
#java -Xmx16g -cp ../lib/lpsolve55j.jar:.:../lib/stanford-classifier-3.2.0.jar -Djava.library.path=../lib/ux64/  em/ApplyMaxEntMoreTrainingData $1 load



#Recall: 47.8108581436077
#Precision: 48.31858407079646
#F-score: 48.063380281690144

