package rxf113;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * @author rxf113
 */
public final class LoopTableProcessor<K, V> {

    private final CacheNode<K, V>[] table;

    LRUCache<K, Integer> lruCache;

    ExecutorService callbackExecutorService;

    ExecutorService loopExecutorsService = getLoopExecutor();

    Thread loopThread;

    List<Integer> deleteIdxList;

    public LoopTableProcessor(CacheNode<K, V>[] table,
                              LRUCache<K, Integer> lruCache,
                              ExecutorService callbackExecutorService,
                              List<Integer> deleteIdxList) {
        this.table = table;
        this.lruCache = lruCache;
        this.callbackExecutorService = callbackExecutorService;
        this.deleteIdxList = deleteIdxList;
    }

    @SuppressWarnings("unchecked")
    private void eventLoopCheck() {
        loopExecutorsService.execute(() -> {
            while (true) {
                //记录最小相差的时间, 用来在每次循环完后整个table后阻塞一段时间，防止cpu空转。取最小的时间 防止监听触发不及时
                long minDiff = Long.MAX_VALUE;
                for (CacheNode<K, V> node : table) {
                    try {
                        if (node == null) {
                            continue;
                        }
                        //loop线程占有当前node, 判断是否过期
                        if (node.casSetState(0, 1)) {
                            long diff;
                            if ((diff = System.nanoTime() - node.nextNanoExpireTime) >= 0) {  //如果到期了
                                callbackExecutorService.execute(() -> {
                                    //执行回调的线程占有当前node
                                    if (node.casSetState(1, 2)) {
                                        //执行过期回调
                                        Object nodeCallback = node.getCallback();
                                        V newVal = null;
                                        if (nodeCallback instanceof BiFunction) {
                                            BiFunction<K, V, V> callback = ((BiFunction<K, V, V>) nodeCallback);
                                            newVal = callback.apply(node.key, node.val);
                                        } else {
                                            BiConsumer<K, V> callback = ((BiConsumer<K, V>) nodeCallback);
                                            callback.accept(node.key, node.val);
                                        }
                                        //刷新过期时间, 不删除缓存
                                        if (node.ifRefreshExpireTime) {
                                            node.nextNanoExpireTime = System.nanoTime() + node.intervalExpireTime;
                                            //更新缓存
                                            if (newVal != null) {
                                                node.val = newVal;
                                            }
                                        } else {
                                            //删除缓存
                                            node.casSetState(2, 3);
                                            Integer deleteIdx = lruCache.get(node.key);
                                            //记录下删除的位置, 后续put寻找空位置使用
                                            deleteIdxList.add(deleteIdx);
                                        }
                                        node.casSetState(2, 0);
                                    }
                                });
                                minDiff = Math.min(minDiff, node.intervalExpireTime);
                            } else {
                                node.casSetState(1, 0);
                                minDiff = Math.min(minDiff, -diff);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                LockSupport.parkNanos(minDiff);
            }
        });
    }

    boolean initFlag = false;

    public void unParkLoop() {
        if (!initFlag) {
            eventLoopCheck();
            initFlag = true;
        }
        LockSupport.unpark(loopThread);
    }

    private ThreadPoolExecutor getLoopExecutor() {
        return new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), r -> {
            loopThread = new Thread(r);
            loopThread.setName("loop-thread");
            return loopThread;
        });
    }
}
