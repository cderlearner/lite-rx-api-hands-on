package my.test;

import org.assertj.core.util.Lists;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Author: linjx
 * Date: 2018/11/11
 */
public class ReactorSnippets {

    private static List<String> words = Arrays.asList(
            "the",
            "quick",
            "brown",
            "fox",
            "jumped",
            "over",
            "the",
            "lazy",
            "dog"
    );

    @Test
    public void simpleCreation() {
        Flux<String> fewWorlds = Flux.just("Hello", "World");
        Flux<String> manyWorlds = Flux.fromIterable(words);

        fewWorlds.subscribe(System.out::println);
        System.out.println();
        manyWorlds.subscribe(System.out::println);
    }

    @Test
    public void simpleCreation2() {
        //使用 generate() 方法生成 Flux 序列：
        //generate() 方法通过同步和逐一的方式来产生 Flux 序列
        //通过调用 SynchronousSink 对象的 next()，complete() 和 error(Throwable) 方法来完成。
        Flux.generate(sink -> {
            sink.next("Hello");   //通过 next()方法产生一个简单的值，至多调用一次
            sink.complete();      //然后通过 complete()方法来结束该序列
        }).subscribe(System.out::println);


        //使用 create() 方法生成 Flux 序列：
        //与 generate() 方法的不同之处在于所使用的是 FluxSink 对象。
        // FluxSink 支持同步和异步的消息产生，并且可以在一次调用中产生多个元素。
        Flux.create(sink -> {
            for (int i = 0; i < 10; i++) {
                sink.next(i);
            }
            sink.complete();
        }).subscribe(System.out::println);
    }

    @Test
    public void simpleGenerate() {
        final Random random = new Random();
        Flux.generate(ArrayList::new, (list, sink) -> {
            int value = random.nextInt(100);
            list.add(value);
            sink.next(value);
            if (list.size() == 10) {
                sink.complete();
            }
            return list;
        }).subscribe(System.out::println);
    }

    @Test
    public void simpleZipwith() {
        Flux.just("a", "b").zipWith(Flux.just("c", "d")).subscribe(System.out::println);
        // zipWith 操作符把当前流中的元素与另外一个流中的元素按照一对一的方式进行合并。
        // 类似python的zip操作
    }

    @Test
    public void simpleFilter() {
        Flux.range(1, 10).filter(i -> i % 2 == 0).subscribe(System.out::println);
        // 只输出满足filter条件的元素

    }

    @Test
    public void simpleReduce() {
        Flux.range(1, 100).reduce((x, y) -> x + y).subscribe(System.out::println);
        Flux.range(1, 100).reduceWith(() -> 100, (x, y) -> x + y).subscribe(System.out::println);
        // reduce 和 reduceWith 操作符对流中包含的所有元素进行累积操作，得到一个包含计算结果的 Mono 序列。
    }

    @Test
    public void findingMissingLetter() {
        Mono<String> missing = Mono.just("s");
        Flux<String> manyLetters = Flux
                .fromIterable(words)
                .flatMap(word -> Flux.fromArray(word.split(""))) //扁平化成一个个字母
                .concatWith(missing)
                .distinct()
                .sort()
                .zipWith(Flux.range(1, Integer.MAX_VALUE), (string, count) -> String.format("%2d. %s", count, string));
        manyLetters.subscribe(System.out::println);

    }

    @Test
    public void shortCircuit() {
        Flux<String> helloPauseWorld = Mono.just("Hello")
                .concatWith(Mono.just("world")
                .delaySubscription(Duration.ofMillis(500)));

        helloPauseWorld.subscribe(System.out::println);

        //此代码段打印“Hello”，但无法打印延迟的“world”，因为测试终止太早。
        //在片段和测试中，你只需要编写一个这样的主类，你通常会想要恢复到阻塞行为。
        //为此，您可以创建CountDownLatch,并在您的订阅者中(onError 和 onCompelete 中)执行 countDown 。
        //但是那不是 reactive（而且错误时，你忘记了 countDown 呢？）

        //你可以解决这个问题的第二种方法是使用一个可以恢复到非 reactive 世界的运算符。
        //具体来说，toIterable 并且 toStream 都将产生阻塞实例。所以让我们使用 toStream ,来说明我们的例子：
    }

    @Test
    public void blocks() {
        Flux<String> helloPauseWorld = Mono.just("Hello")
                .concatWith(Mono.just("World"))
                .delaySubscription(Duration.ofMillis(500));

        helloPauseWorld.toStream().forEach(System.out::println);
    }

    @Test
    public void firstEmitting() {
        Mono<String> a = Mono.just("oops I'm late")
                .delaySubscription(Duration.ofMillis(400));

        // fromCallable()、fromCompletionStage()、fromFuture()、fromRunnable()和 fromSupplier()：
        // 分别从 Callable、CompletionStage、CompletableFuture、Runnable 和 Supplier 中创建 Mono。

        Flux<String> b = Flux.just("let's get", "the party", "started")
                .delaySubscription(Duration.ofMillis(400));

        Flux.first(a, b).toIterable().forEach(System.out::println);
    }

    @Test
    public void simpleBuffer() {
        //Flux.range(1, 100).buffer(20).subscribe(System.out::println);
        //Flux.interval(Duration.ofMillis(100)).buffer(Duration.ofMillis(1001)).take(3).toStream()
        //        .forEach(System.out::println);

        Flux.range(1, 10).bufferUntil(i -> i % 2 == 0).subscribe(System.out::println);
        Flux.range(1, 10).bufferWhile(i -> i % 2 == 0).subscribe(System.out::println);
    }

    @Test
    public void simpleWindow() {
        //Flux.range(1, 100).window(20).subscribe(System.out::println);
        Flux.interval(Duration.ofMillis(100)).window(Duration.ofMillis(1001)).take(2).toStream().forEach(System.out::println);
    }

    @Test
    public void simpleTake() {
//        take
//        take 系列操作符用来从当前流中提取元素。提取的方式可以有很多种。
//
//        take(long n)，take(Duration timespan)和 takeMillis(long timespan)：按照指定的数量或时间间隔来提取。
//        takeLast(long n)：提取流中的最后 N 个元素。
//        takeUntil(Predicate<? super T> predicate)：提取元素直到 Predicate 返回 true。
//        takeWhile(Predicate<? super T> continuePredicate)： 当 Predicate 返回 true 时才进行提取。
//        takeUntilOther(Publisher<?> other)：提取元素直到另外一个流开始产生元素。

//        Flux.range(1, 1000).take(10).subscribe(System.out::println);
//        Flux.range(1, 1000).takeLast(10).subscribe(System.out::println);
        Flux.range(1, 1000).takeWhile(i -> i < 10).subscribe(System.out::println);
        Flux.range(1, 1000).takeUntil(i -> i == 10).subscribe(System.out::println);
    }

    /**
     * 进行合并的流都是每隔 100 毫秒产生一个元素，
     * 不过第二个流中的每个元素的产生都比第一个流要延迟 50 毫秒。
     * 在使用 merge 的结果流中，来自两个流的元素是按照时间顺序交织在一起；
     * 而使用 mergeSequential 的结果流则是首先产生第一个流中的全部元素，再产生第二个流中的全部元素。
     */
    @Test
    public void simpleMerge() {
//        Flux.merge(
//                Flux.interval(Duration.ofMillis(0), Duration.ofMillis(100)).take(5),
//                Flux.interval(Duration.ofMillis(50), Duration.ofMillis(100)).take(5))
//                .toStream()
//                .forEach(System.out::println);

        Flux.mergeSequential(
                Flux.interval(Duration.ofMillis(0), Duration.ofMillis(100)).take(5),
                Flux.interval(Duration.ofMillis(50), Duration.ofMillis(100)).take(5))
                .toStream()
                .forEach(System.out::println);
    }

    /**
     * flatMap 和 flatMapSequential 操作符把流中的每个元素转换成一个流，再把所有流中的元素进行合并。
     * flatMapSequential 和 flatMap 之间的区别与 mergeSequential 和 merge 之间的区别是一样的。
     *
     * 流中的元素被转换成每隔 100 毫秒产生的数量不同的流，再进行合并。
     * 由于第一个流中包含的元素数量较少，所以在结果流中一开始是两个流的元素交织在一起，然后就只有第二个流中的元素。
     */
    @Test
    public void simpleFlatMap() {
        Flux.just(5, 10)
                .flatMap(x -> Flux.interval(Duration.ofMillis(x * 10), Duration.ofMillis(10)).take(x))
                .toStream()
                .forEach(System.out::println);
    }

    /**
     *concatMap 操作符的作用也是把流中的每个元素转换成一个流，再把所有流进行合并。
     * 与 flatMap 不同的是，concatMap 会根据原始流中的元素顺序依次把转换之后的流进行合并；
     * 与 flatMapSequential 不同的是，concatMap 对转换之后的流的订阅是动态进行的，而 flatMapSequential 在合并之前就已经订阅了所有的流。
     */
    @Test
    public void simpleConcatMap() {
        Flux.just(5, 10)
                .concatMap(x -> Flux.interval(Duration.ofMillis(x * 10), Duration.ofMillis(10)).take(x))
                .toStream()
                .forEach(System.out::println);
    }

    @Test
    public void simpleCombineLatest() {
        Flux.combineLatest(
                Arrays::toString,
                Flux.interval(Duration.ofMillis(100)).take(5),
                Flux.interval(Duration.ofMillis(50), Duration.ofMillis(100)).take(5)
        ).toStream().forEach(System.out::println);
    }



    //========================================================================================


    @Test
    public void simpleErr() {
        //当需要处理 Flux 或 Mono 中的消息时，可以通过 subscribe 方法来添加相应的订阅逻辑。例如：
        //通过 subscribe()方法处理正常和错误消息：
        Flux.just(1, 2)
                .concatWith(Mono.error(new IllegalStateException()))
                .subscribe(System.out::println, System.err::println);

        //出现错误时返回默认值0：
        Flux.just(1, 2)
                .concatWith(Mono.error(new IllegalStateException()))
                .onErrorReturn(0)
                .subscribe(System.out::println);

        //出现错误时使用另外的流：
//        Flux.just(1, 2)
//                .concatWith(Mono.error(new IllegalStateException()))
//
//                .subscribe(System.out::println);


        //使用 retry 操作符进行重试：
        Flux.just(1, 2)
                .concatWith(Mono.error(new IllegalStateException()))
                .retry(1)
                .subscribe(System.out::println);
    }

    @Test
    public void simpleErr2() {
        Flux.just(1, 2)
                .concatWith(Mono.error(new IllegalArgumentException()))
                .onErrorResume(e -> {
                    if (e instanceof IllegalStateException) {
                        return Mono.just(0);
                    } else if (e instanceof IllegalArgumentException) {
                        return Mono.just(-1);
                    }
                    return Mono.empty();
                })
                .subscribe(System.out::println);
    }

    /***
     * 当前线程，通过 Schedulers.immediate()方法来创建。
     * 单一的可复用的线程，通过 Schedulers.single()方法来创建。
     * 使用弹性的线程池，通过 Schedulers.elastic()方法来创建。线程池中的线程是可以复用的。当所需要时，新的线程会被创建。如果一个线程闲置太长时间，则会被销毁。该调度器适用于 I/O 操作相关的流的处理。
     * 使用对并行操作优化的线程池，通过 Schedulers.parallel()方法来创建。其中的线程数量取决于 CPU 的核的数量。该调度器适用于计算密集型的流的处理。
     * 使用支持任务调度的调度器，通过 Schedulers.timer()方法来创建。
     * 从已有的 ExecutorService 对象中创建调度器，通过 Schedulers.fromExecutorService()方法来创建。
     * 某些操作符默认就已经使用了特定类型的调度器。比如 intervalMillis()方法创建的流就使用了由 Schedulers.timer()创建的调度器。通过 publishOn()和 subscribeOn()方法可以切换执行操作的调度器。其中 publishOn()方法切换的是操作符的执行方式，而 subscribeOn()方法切换的是产生流中元素时的执行方式。
     *
     * 使用 create()方法创建一个新的 Flux 对象，其中包含唯一的元素是当前线程的名称。
     * 接着是两对 publishOn()和 map()方法，其作用是先切换执行时的调度器，再把当前的线程名称作为前缀添加。
     * 最后通过 subscribeOn()方法来改变流产生时的执行方式。运行之后的结果是[elastic-2] [single-1] parallel-1。
     * 最内层的线程名字 parallel-1 来自产生流中元素时使用的 Schedulers.parallel()调度器，
     * 中间的线程名称 single-1 来自第一个 map 操作之前的 Schedulers.single()调度器，
     * 最外层的线程名字 elastic-2 来自第二个 map 操作之前的 Schedulers.elastic()调度器。
     */
    @Test
    public void simpleScheduler() {
        Flux.create(sink -> {
            sink.next(Thread.currentThread().getName());
            sink.complete();
        })
        .publishOn(Schedulers.single())
        .map(x -> String.format("[%s] %s", Thread.currentThread().getName(), x))
        .publishOn(Schedulers.elastic())
        .map(x -> String.format("[%s] %s", Thread.currentThread().getName(), x))
        .subscribeOn(Schedulers.parallel())
        .toStream()
        .forEach(System.out::println);
    }


    /**
     * 启用调试模式，
     * 在调试模式启用之后，所有的操作符在执行时都会保存额外的与执行链相关的信息。
     * 当出现错误时，这些信息会被作为异常堆栈信息的一部分输出。通过这些信息可以分析出具体是在哪个操作符的执行中出现了问题。
     *
     * Hooks.onOperator(providedHook -> providedHook.operatorStacktrace());
     * 不过当调试模式启用之后，记录这些额外的信息是有代价的。一般只有在出现了错误之后，再考虑启用调试模式。
     * 但是当为了找到问题而启用了调试模式之后，之前的错误不一定能很容易重现出来。为了减少可能的开销，
     * 可以限制只对特定类型的操作符启用调试模式。
     */

    /**
     * 使用检查点
     * 另外一种做法是通过 checkpoint 操作符来对特定的流处理链来启用调试模式。代码清单 25 中，在 map 操作符之后添加了一个名为 test 的检查点。当出现错误时，检查点名称会出现在异常堆栈信息中。对于程序中重要或者复杂的流处理链，可以在关键的位置上启用检查点来帮助定位可能存在的问题。
     *
     * 使用 checkpoint 操作符
     *
     * Flux.just(1, 0).map(x -> 1 / x).checkpoint("test").subscribe(System.out::println);
     */

    /**
     * 在开发和调试中的另外一项实用功能是把流相关的事件记录在日志中。这可以通过添加 log 操作符来实现。在代码清单 26 中，添加了 log 操作符并指定了日志分类的名称。
     *
     * 使用 log 操作符记录事件
     *
     * Flux.range(1, 2).log("Range").subscribe(System.out::println);
     */


    /**
     * 之前的代码清单中所创建的都是冷序列。
     * 冷序列的含义是不论订阅者在何时订阅该序列，总是能收到序列中产生的全部消息。
     * 而与之对应的热序列，则是在持续不断地产生消息，订阅者只能获取到在其订阅之后产生的消息。
     *
     * 原始的序列中包含 10 个间隔为 1 秒的元素。
     * 通过 publish()方法把一个 Flux 对象转换成 ConnectableFlux 对象。
     * 方法 autoConnect()的作用是当 ConnectableFlux 对象有一个订阅者时就开始产生消息。
     * 代码 source.subscribe()的作用是订阅该 ConnectableFlux 对象，让其开始产生数据。
     * 接着当前线程睡眠 5 秒钟，第二个订阅者此时只能获得到该序列中的后 5 个元素，因此所输出的是数字 5 到 9。
     * @throws Exception
     */
    @Test
    public void simpleHot() throws Exception{
        final Flux<Long> source = Flux.interval(Duration.ofMillis(1000))
                .take(10)
                .publish()
                .autoConnect();

        source.subscribe();
        Thread.sleep(5000);
        source.toStream().forEach(System.out::println);
    }

    @Test
    public void testFlatMap(){
        List<String> secondList = Lists.newArrayList("1","2","3","4","5");
        Flux<String> flatMapFlux = Flux.fromIterable(secondList)
                .flatMap((str) ->{
                    return Mono.just(str).repeat(2).map(String::toUpperCase).delayElements(Duration.ofMillis(1));
                });
        flatMapFlux.subscribe(e -> {
            System.out.println("subscribe:" + e);
        });
        flatMapFlux.blockLast();

        Flux<String> mapFlux = Flux.fromIterable(secondList)
                //.repeat(2)
                .map(String::toUpperCase);
        mapFlux.subscribe(e -> {
            System.out.println("map subscribe:" + e);
        });
        mapFlux.blockLast();
    }
}