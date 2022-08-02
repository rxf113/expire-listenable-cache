package rxf113;

import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExpireListenableCacheTest {

    @Test
    public void test() throws Exception {
        ExpireListenableCache<String, Object> cache = ExpireListenableCache
                .<String, Object>newBuilder()
                //容量
                .size(4)
                //5秒过期
                .expireTime(Duration.ofSeconds(5))
                //执行callback的线程池
                .callbackExecutorService(Executors.newFixedThreadPool(5))
                .build();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < 8; i++) {
            int finalI = i;
            System.out.println(i);
            cache.put("k" + i, "v" + i, true, (k, v) -> {
                System.out.println("过期回调k" + finalI + ": " + formatter.format(LocalDateTime.now()) + " v: " + v);
                return 234 + " " + v;
            });
        }

//        cache.put("k1", "v1", true, (k, v) -> {
//            System.out.println("过期回调k1: " + formatter.format(LocalDateTime.now()));
//        });
//        System.out.println("put k1: " + formatter.format(LocalDateTime.now()));
//
//        Thread.sleep(1000);
//        cache.put("k2", "v2", true, (k, v) -> {
//            System.out.println("过期回调k2: " + formatter.format(LocalDateTime.now()));
//        });
//        System.out.println("put k2: " + formatter.format(LocalDateTime.now()));
//
//        Thread.sleep(1000);
//        cache.put("k3", "v3", true, (k, v) -> {
//            System.out.println("过期回调k3: " + formatter.format(LocalDateTime.now()));
//        });
//        System.out.println("put k3: " + formatter.format(LocalDateTime.now()));
//
//        Thread.sleep(1000);
//        cache.put("k4", "v4", true, (k, v) -> {
//            System.out.println("过期回调k4: " + formatter.format(LocalDateTime.now()));
//        });
//        System.out.println("put k4: " + formatter.format(LocalDateTime.now()));

        System.in.read();
    }

}