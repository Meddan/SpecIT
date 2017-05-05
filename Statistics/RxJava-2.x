========= STATS GATHERED =========
Methods processed: 18651
Total behaviors: 24203
Successful behaviors: 2777
Failing behaviors: 21426
	Symbol solver Failures: 15732
		Method Failures: 15658
		Other Failures: 74
	Uncovered Statement Failures: 2262
	Unresolved Parameter Failures: 3432
Total postconditions: 16715
Total preconditions: 1696
	 Null checks: 684
Total amount of Null checks: 58444
Preconditions per method: 
Mean: 0,045 Min: 0,000 Median: 0,000 Max: 96,000 Standard Deviation: 1,035 
Postconditions per method: 
Mean: 0,896 Min: 0,000 Median: 0,000 Max: 448,000 Standard Deviation: 4,996 
Null checks per method 
Mean: 0,037 Min: 0,000 Median: 0,000 Max: 96,000 Standard Deviation: 1,161 
Behaviors per method: 
Mean: 0,149 Min: 0,000 Median: 0,000 Max: 18,000 Standard Deviation: 0,456 
Preconditions per behavior: 
Mean: 0,305 Min: 0,000 Median: 0,000 Max: 33,000 Standard Deviation: 1,783 
Postconditions per behavior: 
Mean: 6,019 Min: 0,000 Median: 5,000 Max: 30,000 Standard Deviation: 5,588 
Null checks per behavior: 
Mean: 0,246 Min: 0,000 Median: 0,000 Max: 33,000 Standard Deviation: 2,387 
==================================


===========INTERESTING METHODS===========
======== 12 found ========

/RxJava-2.x/src/main/java/io/reactivex/internal/operators/flowable/BlockingFlowableNext.java --> public T next()
/RxJava-2.x/src/main/java/io/reactivex/internal/operators/flowable/FlowableFlatMap.java --> SimpleQueue<U> getMainQueue()
/RxJava-2.x/src/main/java/io/reactivex/internal/operators/flowable/FlowableObserveOn.java --> final boolean checkTerminated(boolean d, boolean empty, Subscriber<?> a)
/RxJava-2.x/src/main/java/io/reactivex/internal/operators/flowable/FlowableSkipLastTimed.java --> boolean checkTerminated(boolean d, boolean empty, Subscriber<? super T> a, boolean delayError)
/RxJava-2.x/src/main/java/io/reactivex/internal/operators/flowable/FlowableWindow.java --> public void onNext(T t)
/RxJava-2.x/src/main/java/io/reactivex/internal/operators/observable/BlockingObservableNext.java --> public T next()
/RxJava-2.x/src/main/java/io/reactivex/internal/operators/observable/ObservableCombineLatest.java --> void combine(T value, int index)
/RxJava-2.x/src/main/java/io/reactivex/plugins/RxJavaPlugins.java --> static boolean isBug(Throwable error)
/RxJava-2.x/src/main/java/io/reactivex/processors/BehaviorProcessor.java --> void emitNext(Object value, long stateIndex)
/RxJava-2.x/src/main/java/io/reactivex/subjects/BehaviorSubject.java --> void emitNext(Object value, long stateIndex)
/RxJava-2.x/src/test/java/io/reactivex/ParamValidationCheckerTest.java --> void addCheckPrimitive(Class<?> clazz, ParamOverride override, List<Object> values)
/RxJava-2.x/src/test/java/io/reactivex/ParamValidationCheckerTest.java --> Object defaultPrimitive(Class<?> clazz, ParamOverride override)



 ####### NEW RUN ####### 

========= STATS GATHERED =========
Methods processed: 18651
Total behaviors: 22997
Successful behaviors: 2817
Failing behaviors: 20180
	Symbol solver Failures: 14774
		Method Failures: 14753
		Other Failures: 21
	Uncovered Statement Failures: 2162
	Unresolved Parameter Failures: 3244
Total postconditions: 16578
Total preconditions: 1575
	 Null checks: 463
Total amount of Null checks: 53905
Preconditions per method: 
Mean: 0,042 Min: 0,000 Median: 0,000 Max: 196,000 Standard Deviation: 1,339 
Postconditions per method: 
Mean: 0,889 Min: 0,000 Median: 0,000 Max: 233,000 Standard Deviation: 4,150 
Null checks per method 
Mean: 0,025 Min: 0,000 Median: 0,000 Max: 112,000 Standard Deviation: 0,953 
Behaviors per method: 
Mean: 0,151 Min: 0,000 Median: 0,000 Max: 52,000 Standard Deviation: 0,584 
Preconditions per behavior: 
Mean: 0,280 Min: 0,000 Median: 0,000 Max: 28,000 Standard Deviation: 1,240 
Postconditions per behavior: 
Mean: 5,885 Min: 0,000 Median: 4,000 Max: 30,000 Standard Deviation: 5,395 
Null checks per behavior: 
Mean: 0,164 Min: 0,000 Median: 0,000 Max: 28,000 Standard Deviation: 1,500 
==================================


===========INTERESTING METHODS===========
======== 9 found ========

/RxJava-2.x/src/main/java/io/reactivex/internal/operators/flowable/BlockingFlowableNext.java --> public T next()
/RxJava-2.x/src/main/java/io/reactivex/internal/operators/flowable/FlowableFlatMap.java --> SimpleQueue<U> getMainQueue()
/RxJava-2.x/src/main/java/io/reactivex/internal/operators/flowable/FlowableObserveOn.java --> final boolean checkTerminated(boolean d, boolean empty, Subscriber<?> a)
/RxJava-2.x/src/main/java/io/reactivex/internal/operators/flowable/FlowableSkipLastTimed.java --> boolean checkTerminated(boolean d, boolean empty, Subscriber<? super T> a, boolean delayError)
/RxJava-2.x/src/main/java/io/reactivex/internal/operators/flowable/FlowableWindow.java --> public void onNext(T t)
/RxJava-2.x/src/main/java/io/reactivex/internal/operators/observable/BlockingObservableNext.java --> public boolean hasNext()
/RxJava-2.x/src/main/java/io/reactivex/internal/operators/observable/BlockingObservableNext.java --> public T next()
/RxJava-2.x/src/main/java/io/reactivex/plugins/RxJavaPlugins.java --> static boolean isBug(Throwable error)
/RxJava-2.x/src/test/java/io/reactivex/ParamValidationCheckerTest.java --> void addCheckPrimitive(Class<?> clazz, ParamOverride override, List<Object> values)

