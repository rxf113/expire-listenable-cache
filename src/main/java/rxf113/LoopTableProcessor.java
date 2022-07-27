package rxf113;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

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
                                        node.expireCallbackConsumer.accept(node.key, node.val); //执行过期回调
                                        //刷新过期时间, 不删除缓存
                                        if (node.ifRefreshExpireTime) {
                                            node.nextNanoExpireTime = System.nanoTime() + node.intervalExpireTime;
                                        } else {
                                            //删除缓存
                                            Integer deleteIdx = lruCache.get(node.key);
                                            table[deleteIdx] = null;
                                            lruCache.removeNode(lruCache.getNode(node.key));
                                            //记录下删除的位置, 后续put寻找空位置使用
                                            deleteIdxList.add(deleteIdx);
                                        }
                                        node.casSetState(2, 0);
                                    }
                                });
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
