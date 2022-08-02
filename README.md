# expire-listenable-cache

### 过期主动执行回调的本地key value缓存方案，目的是用更简单的方式对定时类任务进行处理


> 实现方法是: 尝试用一个单独的loop线程，轮询缓存中所有节点，判断过期时间并主动触发回调

#### 使用示例

```java

ExpireListenableCache<String, Object> cache=ExpireListenableCache
        .<String, Object>newBuilder()
        //容量
        .size(3)
        //5秒过期
        .expireTime(Duration.ofSeconds(5))
        //执行callback的线程池
        .callbackExecutorService(Executors.newFixedThreadPool(10))
        .build();

        DateTimeFormatter formatter=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        //consumer
        cache.put("k1","v1",true,(k,v)->{
        System.out.println("过期回调k1: "+formatter.format(LocalDateTime.now()));
        });

//function
//        cache.put("k" + i, "v" + i, true, (k, v) -> {
//        System.out.println("过期回调k" + finalI + ": " + formatter.format(LocalDateTime.now()) + " v: " + v);
//        return "newVal";
//        });

```