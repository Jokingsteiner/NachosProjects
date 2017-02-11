package nachos.threads;

    import nachos.machine.*;

    import java.util.*;

/**
 * Created by cjk98 on 2/10/2017.
 */

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param transferPriority <tt>true</tt> if this queue should transfer
     * priority from waiting threads to the owning thread.
     * @return a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum
                && priority <= priorityMaximum);

        // Only setPriority (and call updateEffectivePriority() inside if necessary
        if (priority != getThreadState(thread).getPriority())
            getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();
        boolean ret = true;

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            ret = false;
        else
            setPriority(thread, priority + 1);

        Machine.interrupt().restore(intStatus);
        return ret;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();
        boolean ret = true;

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            ret = false;
        else
            setPriority(thread, priority - 1);

        Machine.interrupt().restore(intStatus);
        return ret;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;

    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;

    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param thread the thread whose scheduling state to return.
     * @return the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    // This is the comparator use to sort waitQueue(javaPQ) in the PriorityQueue
    private static class BY_THREADSTATE implements Comparator<ThreadState> {
        private nachos.threads.PriorityScheduler.PriorityQueue threadWaitQueue;

        public BY_THREADSTATE(nachos.threads.PriorityScheduler.PriorityQueue pq) {
            this.threadWaitQueue = pq;
        }

        @Override
        public int compare(ThreadState o1, ThreadState o2) {
            int ePriority1 = o1.getEffectivePriority(), ePriority2 = o2.getEffectivePriority();
            if (ePriority1 > ePriority2) {
                return -1;
            }
            else if (ePriority1 < ePriority2) {
                return 1;
            }
            else { // equal effective priority, order by the time these thread wait
                long waitTime1 = o1.waitingMap.get(threadWaitQueue), waitTime2 = o2.waitingMap.get(threadWaitQueue);

                if (waitTime1 < waitTime2) {
                    return -1;
                }
                else if(waitTime1 > waitTime2) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        }
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        public boolean transferPriority;

        // explicitly declare java.util.PriorityQueue in case of confusion to compiler
        // the threads who are waiting for this resource (PQ)
        private java.util.PriorityQueue<ThreadState> waitQueue = new java.util.PriorityQueue<ThreadState>(8,new BY_THREADSTATE(this));

        // the thread who acquire the resource represented by this PQ
        private KThread threadHolding = null;

        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            if (waitQueue.isEmpty()) {
                // no thread is waiting
                return null;
            }
            else {
                // let next thread (top of the waitQueue) to acquire resource
                acquire(waitQueue.poll().thread);
                return threadHolding;
            }
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return the next thread that <tt>nextThread()</tt> would return.
         */
        protected ThreadState pickNextThread() {
            // peek() will not remove the thread in waitQueue
            return waitQueue.peek();
        }

        public void print() {
        }
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue it's
     * waiting for, if any.
     *
     * @see nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
        /** The thread with which this object is associated. */
        protected KThread thread;
        /** The priority of the associated thread. */
        protected int priority;
        // Cached effective priority
        protected int effectivePriority;
        // a set to store which resources (nachosPQ) this thread has acquired
        private HashSet<nachos.threads.PriorityScheduler.PriorityQueue> acquiredSet = new HashSet<nachos.threads.PriorityScheduler.PriorityQueue>();

        // a Map to store resources (nachosPQ) that this thread is waiting for
        // also the start wating time is stored in the map for breaking tie
        protected HashMap<PriorityQueue,Long> waitingMap = new HashMap<nachos.threads.PriorityScheduler.PriorityQueue,Long>();
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param thread the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;
            this.priority= priorityDefault;
            this.effectivePriority = priorityDefault;
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return the effective priority of the associated thread.
         */
        public int getEffectivePriority() {
            // implement me
            return priority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param priority the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;

            this.priority = priority;

            // implement me
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the resource
         * guarded by <tt>waitQueue</tt>. This method is only called if the
         * associated thread cannot immediately obtain access.
         *
         * @param waitQueue the queue that the associated thread is now waiting
         * on.
         *
         * @see nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(PriorityQueue waitQueue) {
            // implement me
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see nachos.threads.ThreadQueue#acquire
         * @see nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(PriorityQueue waitQueue) {
            // implement me
        }

        // in any case the calling thread do not need this resource,
        // or the calling thread
        private void release(PriorityQueue priorityQueue) {
            // remove priorityQueue from my acquired set
            if (acquiredSet.remove(priorityQueue)) {
                priorityQueue.threadHolding = null;
                updateEffectivePriority();
            }
        }
    }
}
