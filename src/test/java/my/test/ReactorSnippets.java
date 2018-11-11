package my.test;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public void simpleBuffer() {
        //Flux.range(1, 100).buffer(20).subscribe(System.out::println);
        Flux.interval(Duration.ofMillis(100)).buffer(Duration.ofMillis(1001)).take(3).toStream()
                .forEach(System.out::println);



    }
}