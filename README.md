# expire-listenable-cache

### 缓存中的key value过期后，主动执行过期回调方法

#### 尝试用一个单独的loop线程，轮询缓存中所有节点，判断过期时间并主动触发回调

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

        cache.put("k1","v1",true,(k,v)->{
        System.out.println("过期回调k1: "+formatter.format(LocalDateTime.now()));
        });

```