#./runILP.sh all
./compile.sh
java -Xmx16g -cp ../lib/stanford-classifier-3.2.0.jar:../lib/lpsolve55j.jar:. em/MaxEntLearnMoreTrainData
cd /users/yzcchen/tool/YASMET
cat ~/chen3/zeroEM/EMAlgorithm/src/yasmet.train | ./a.out -deltaPP 0.0 -iter 100 > WT
#cat ~/chen3/zeroEM/EMAlgorithm/src/person.train | ./a.out -deltaPP 0.0 -iter 100 > person.model
#cat ~/chen3/zeroEM/EMAlgorithm/src/gender.train | ./a.out -deltaPP 0.0 -iter 100 > gender.model
#cat ~/chen3/zeroEM/EMAlgorithm/src/number.train | ./a.out -deltaPP 0.0 -iter 100 > number.model
#cat ~/chen3/zeroEM/EMAlgorithm/src/animacy.train | ./a.out -deltaPP 0.0 -iter 100 > animacy.model

cp /users/yzcchen/tool/YASMET/WT /dev/shm/
#cp person.model /dev/shm
#cp gender.model /dev/shm
#cp number.model /dev/shm
#cp animacy.model /dev/shm

cd /users/yzcchen/chen3/zeroEM/EMAlgorithm/src
java -Xmx16g -cp ../lib/lpsolve55j.jar:.:../lib/stanford-classifier-3.2.0.jar -Djava.library.path=../lib/ux64/  em/ApplyMaxEnt $1 classify

#java -Xmx16g -cp ../lib/lpsolve55j.jar:.:../lib/stanford-classifier-3.2.0.jar -Djava.library.path=../lib/ux64/  em/ApplyMaxEnt $1 prepare
cd /users/yzcchen/tool/YASMET

#cat ~/chen3/zeroEM/EMAlgorithm/src/ante.test$1 |  /users/yzcchen/tool/YASMET/./a.out /dev/shm/WT 1>ante.rs$1
#cat ~/chen3/zeroEM/EMAlgorithm/src/person.test$1 | /users/yzcchen/tool/YASMET/./a.out /dev/shm/person.model 1>person.rs$1
#cat ~/chen3/zeroEM/EMAlgorithm/src/gender.test$1 | /users/yzcchen/tool/YASMET/./a.out /dev/shm/gender.model 1>gender.rs$1
#cat ~/chen3/zeroEM/EMAlgorithm/src/number.test$1 | /users/yzcchen/tool/YASMET/./a.out /dev/shm/number.model 1>number.rs$1
#cat ~/chen3/zeroEM/EMAlgorithm/src/animacy.test$1 | /users/yzcchen/tool/YASMET/./a.out /dev/shm/animacy.model 1>animacy.rs$1

cd /users/yzcchen/chen3/zeroEM/EMAlgorithm/src
#java -Xmx16g -cp ../lib/lpsolve55j.jar:.:../lib/stanford-classifier-3.2.0.jar -Djava.library.path=../lib/ux64/  em/ApplyMaxEnt $1 load



#Recall: 47.8108581436077
#Precision: 48.31858407079646
#F-score: 48.063380281690144

