package rxf113;

import java.util.function.BiConsumer;

/**
 * @author rxf113
 */
public class CacheNode<K, V> extends CasNode {

    BiConsumer<K, V> expireCallbackConsumer;
    K key;
    V val;
    volatile long nextNanoExpireTime;
    final long intervalExpireTime;
    boolean ifRefreshExpireTime;

    public CacheNode(K key, V val, boolean ifRefreshExpireTime, long intervalExpireTime, BiConsumer<K, V> expireCallbackConsumer) {
        this.key = key;
        this.val = val;
        this.ifRefreshExpireTime = ifRefreshExpireTime;
        this.nextNanoExpireTime = System.nanoTime() + intervalExpireTime;
        this.intervalExpireTime = intervalExpireTime;
        this.expireCallbackConsumer = expireCallbackConsumer;
    }


}
