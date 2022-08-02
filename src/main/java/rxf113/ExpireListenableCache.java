package rxf113;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * 可监听过期事件的缓存
 *
 * @author rxf113
 */
public class ExpireListenableCache<K, V> {
    /**
     * CacheNode数组存储实际数据，保证大小
     */
    private final CacheNode<K, V>[] table;

    /**
     * 数组的索引
     */
    private int tableIdx = 0;

    /**
     * 容量满时用来判断删除哪组key val
     * key = key
     * val = key-val 在 table中的索引
     */
    private final LRUCache<K, Integer> lruCache;

    /**
     * 记录删除的位置
     */
    private static final List<Integer> DELETE_IDX_LIST = new CopyOnWriteArrayList<>();

    /**
     * 过期间隔时间
     */
    private final long intervalExpireTime;

    private final Lock putLock = new ReentrantLock();

    private final LoopTableProcessor<K, V> loopTableProcessor;

    @SuppressWarnings("unchecked")
    public ExpireListenableCache(int size, Duration intervalExpireTime, ExecutorService callbackExecutorService) {
        lruCache = new LRUCache<>(size);
        table = new CacheNode[size];
        this.intervalExpireTime = intervalExpireTime.toNanos();
        //初始化loop
        loopTableProcessor = new LoopTableProcessor<>(table, lruCache, callbackExecutorService, DELETE_IDX_LIST);
    }

    public V get(K k) {
        Integer idx = lruCache.get(k);
        return idx == null ? null : table[idx].val;
    }


    /**
     * 自定义节点过期时间
     */
    public void put(K key, V val, boolean ifRefreshExpireTime, BiConsumer<K, V> expireConsumer, Duration expireTime) {
        CacheNode<K, V> cacheNode = new CacheNode<>(key, val, ifRefreshExpireTime, expireTime.toNanos(), expireConsumer);
        putVal(cacheNode);
    }

    /**
     * 统一使用 builder 中的过期时间
     */
    public void put(K key, V val, boolean ifRefreshExpireTime, BiConsumer<K, V> expireConsumer) {
        CacheNode<K, V> cacheNode = new CacheNode<>(key, val, ifRefreshExpireTime, intervalExpireTime, expireConsumer);
        putVal(cacheNode);
    }

    public void put(K key, V val, boolean ifRefreshExpireTime, BiFunction<K, V, ?> expireCallbackFunction) {
        CacheNode<K, V> cacheNode = new CacheNode<>(key, val, ifRefreshExpireTime, intervalExpireTime, expireCallbackFunction);
        putVal(cacheNode);
    }

    private void putVal(CacheNode<K, V> cacheNode) {
        putLock.lock();
        try {
            K key = cacheNode.key;
            //检查key是否已经存在
            LRUCache.Node<K, Integer> existsNode = lruCache.getNode(key);
            if (existsNode != null) {
                //存在, 替换值
                table[existsNode.getValue()] = cacheNode;
                lruCache.moveToHead(existsNode);
            } else {
                if (tableIdx < table.length) {
                    //如果table未满, 直接添加
                    setTableNodeAndSyncLruCache(tableIdx++, cacheNode);
                } else {
                    //table已经满了, 判断存不存在已删除的位置
                    boolean foundDeleteIdx = false;
                    while (!DELETE_IDX_LIST.isEmpty()) {
                        //存在已删除的位置, 新node放到删除位置
                        int deletedIdx = DELETE_IDX_LIST.remove(0);
                        if (table[deletedIdx].getState() == 3) {
                            setTableNodeAndSyncLruCache(deletedIdx, cacheNode);
                            foundDeleteIdx = true;
                            break;
                        }
                        //否则表示, loop线程删除此位置时, 刚好此位置作为eldest主动删除, 然后发生了替换, 所以此位置保留
                    }
                    if (!foundDeleteIdx) {
                        //不存在已删除的位置, 主动删除 eldest node
                        LRUCache.Node<K, Integer> tail = lruCache.manualRemoveTail();
                        setTableNodeAndSyncLruCache(tail.getValue(), cacheNode);
                    }
                }
            }
        } finally {
            putLock.unlock();
            //唤醒loop线程(如果table为空 会park LongMax)
            loopTableProcessor.unParkLoop();
        }
    }

    private void setTableNodeAndSyncLruCache(int idx, CacheNode<K, V> node) {
        table[idx] = node;
        lruCache.put(node.key, idx);
    }

    public static <K, V> Builder<K, V> newBuilder() {
        return new Builder<>();
    }

    public static class Builder<K, V> {

        private int size;

        private Duration expireTime;

        private ExecutorService callbackExecutorService;

        public Builder<K, V> size(int size) {
            this.size = size;
            return this;
        }

        public Builder<K, V> expireTime(Duration expireTime) {
            this.expireTime = expireTime;
            return this;
        }

        public Builder<K, V> callbackExecutorService(ExecutorService callbackExecutorService) {
            this.callbackExecutorService = callbackExecutorService;
            return this;
        }

        public ExpireListenableCache<K, V> build() {
            return new ExpireListenableCache<>(this.size, this.expireTime, this.callbackExecutorService);
        }
    }
}
