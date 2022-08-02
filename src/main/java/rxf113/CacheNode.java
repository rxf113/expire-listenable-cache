package rxf113;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * @author rxf113
 */
public class CacheNode<K, V> extends CasNode {

    private BiConsumer<K, V> expireCallbackConsumer;
    private BiFunction<K, V, ?> expireCallbackFunction;
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

    public CacheNode(K key, V val, boolean ifRefreshExpireTime, long intervalExpireTime, BiFunction<K, V, ?> expireCallbackFunction) {
        this.key = key;
        this.val = val;
        this.ifRefreshExpireTime = ifRefreshExpireTime;
        this.nextNanoExpireTime = System.nanoTime() + intervalExpireTime;
        this.intervalExpireTime = intervalExpireTime;
        this.expireCallbackFunction = expireCallbackFunction;
    }

    public Object getCallback() {
        return expireCallbackFunction == null ? expireCallbackConsumer : expireCallbackFunction;
    }


}
