package rxf113;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义LRU缓存
 *
 * @param <K> key
 * @param <V> val
 * @author rxf113
 */
public class LRUCache<K, V> {

    static class Node<K, V> {
        private K key;
        private V value;
        private Node<K, V> pre;
        private Node<K, V> next;

        public V getValue() {
            return value;
        }

        public Node() {
        }

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final Map<K, Node<K, V>> cacheMap = new HashMap<>();
    private int size;
    private final int capacity;
    private final Node<K, V> head;
    private final Node<K, V> tail;

    public LRUCache(int capacity) {
        this.size = 0;
        this.capacity = capacity;
        head = new Node<>();
        tail = new Node<>();
        head.next = tail;
        tail.pre = head;
    }

    public V get(K key) {
        Node<K, V> node = cacheMap.get(key);
        if (node == null) {
            return null;
        }
        // 如果 key 存在，先通过哈希表定位，再移到头部
        moveToHead(node);
        return node.value;
    }

    public Node<K, V> getNode(K key) {
        return cacheMap.get(key);
    }

    public void put(K key, V value) {
        Node<K, V> node = cacheMap.get(key);
        if (node == null) {
            //不存在
            //构建新的node
            Node<K, V> newNode = new Node<>(key, value);
            cacheMap.put(key, newNode);
            //添加到链表头
            addToHead(newNode);
            if (++size > capacity) {
                // 如果超出容量，删除双向链表的尾部节点
                Node<K, V> tailPre = removeTail();
                // 删除哈希表中对应的项
                cacheMap.remove(tailPre.key);
                --size;
            }
        } else {
            //存在
            // 如果 key 存在，先通过哈希表定位，再修改 value，并移到头部
            //更新value
            node.value = value;
            moveToHead(node);
        }
    }

    public void moveToHead(Node<K, V> node) {
        //当前
        if (node != head.next) {
            removeNode(node);
            addToHead(node);
        }
    }

    public void addToHead(Node<K, V> newNode) {
        Node<K, V> originNode = head.next;

        head.next = newNode;
        newNode.pre = head;
        newNode.next = originNode;
        originNode.pre = newNode;

    }

    public Node<K, V> removeTail() {
        if (tail.pre == head) {
            throw new NullPointerException("容器容量为空");
        }
        Node<K, V> tailPre = tail.pre;
        removeNode(tailPre);
        return tailPre;
    }

    public void removeNode(Node<K, V> node) {
        node.pre.next = node.next;
        node.next.pre = node.pre;
    }
}