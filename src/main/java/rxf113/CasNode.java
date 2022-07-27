package rxf113;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * cas控制state
 *
 * @author rxf113
 */
public class CasNode {

    protected static final Unsafe UNSAFE = getUnsafe();

    /**
     * 状态 1: 被loop线程占有, 2: 被业务线程占有, 0: 空闲
     */
    private volatile int state = 0;

    private static final long STATE_OFFSET;

    static {
        try {
            STATE_OFFSET = UNSAFE.objectFieldOffset(CasNode.class.getDeclaredField("state"));
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }


    public boolean casSetState(int oldState, int newState) {
        return UNSAFE.compareAndSwapInt(this, STATE_OFFSET, oldState, newState);
    }

    /**
     * 自定义获取Unsafe的theUnsafe实例
     *
     * @return
     */
    @SuppressWarnings("all")
    private static Unsafe getUnsafe() {
        Class<Unsafe> cla = Unsafe.class;
        try {
            Field theUnsafe = cla.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new SecurityException("Unsafe...");
    }
}
