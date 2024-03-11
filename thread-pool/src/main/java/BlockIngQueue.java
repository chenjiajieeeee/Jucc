import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
@Slf4j(topic = "c.BlockIngQueue")
public class BlockIngQueue<T> {

    private Deque<T> deque = new ArrayDeque<>();

    private ReentrantLock lock= new ReentrantLock();

    private Condition fullWartSet=lock.newCondition();
    private Condition emptyWaitSet=lock.newCondition();

    private int capatity;

    public BlockIngQueue(int capatity){
        this.capatity=capatity;

    }

    //带超时的阻塞获取
    public T poll(long timeout, TimeUnit timeUnit){
        lock.lock();
        try {
            long nanos = timeUnit.toNanos(timeout);

            while (deque.isEmpty()){
                try {
                    //返回的是剩余的时间
                    if (nanos<=0)return null;
                    nanos= emptyWaitSet.awaitNanos(nanos);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }

            }
            T t = deque.removeFirst();
            fullWartSet.signal();
            return t;
        }finally {
            lock.unlock();
        }

    }

    public T take(){
        lock.lock();
        try {
            while (deque.isEmpty()){
                try {
                    emptyWaitSet.await();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }

            }
            T t = deque.removeFirst();
            fullWartSet.signal();
            return t;
        }finally {
            lock.unlock();
        }

    }

    public void put(T element){
        lock.lock();
        try {
            while (deque.size()==capatity){
                try {
                    log.debug("等待加入任务队列{}",element);
                    fullWartSet.await();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }

            }
            log.debug("加入任务队列{}",element);
            deque.addLast(element);
            emptyWaitSet.signal();
        }finally {
            lock.unlock();
        }
    }
    //带超时时间的添加方法
    public boolean offer(T element,long timeout,TimeUnit timeUnit){
        lock.lock();
        try {
            long nanos=timeUnit.toNanos(timeout);

            while (deque.size()==capatity){
                try {
                    log.debug("等待加入任务队列{}",element);
                    if(nanos<=0)return false;
                    nanos=fullWartSet.awaitNanos(nanos);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }

            }
            log.debug("加入任务队列{}",element);
            deque.addLast(element);
            emptyWaitSet.signal();
            return true;
        }finally {
            lock.unlock();
        }
    }
    public int size(){
        lock.lock();
        try {
            return deque.size();
        }finally {
            lock.unlock();
        }
    }

    public void tryPut(RejectPolicy<T> rejectPolicy, T task) {
        lock.lock();
        try {
            if(deque.size()==capatity){
                rejectPolicy.reject(this,task);
            }else {
                log.debug("加入任务队列{}",task);
                deque.addLast(task);
                emptyWaitSet.signal();
            }
        }finally {
            lock.unlock();
        }
    }
}
@FunctionalInterface
interface RejectPolicy<T>{
    void reject(BlockIngQueue<T> queue,T task);
}

@Slf4j(topic = "c.BlockIngQueue")
class ThreadPool{
    private BlockIngQueue<Runnable> blockIngQueue;
    private HashSet<Worker> workers=new HashSet<>();

    private int coreSize;
    private long timeOut;

    private TimeUnit timeUnit;

    private RejectPolicy<Runnable> rejectPolicy;

    public void execute(Runnable task){
        //根据当前任务队列数量进行处理
        //超过coresize时，加入任务队列暂存
        synchronized (workers) {
            if (workers.size() < coreSize) {

                Worker worker = new Worker(task);
                log.debug("新增一个worker{},{}",worker,task);
                workers.add(worker);
                worker.start();
            } else {
                //死等
                //带超时等待
                //放弃任务执行
                //抛出异常
                //让调用者自己执行任务
                blockIngQueue.tryPut(rejectPolicy,task);
            }
        }

    }

    public ThreadPool(int coreSize,long timeOut,TimeUnit timeUnit,int queueCapatity,RejectPolicy<Runnable> rejectPolicy){
        this.coreSize=coreSize;
        this.timeOut=timeOut;
        this.timeUnit=timeUnit;
        this.blockIngQueue=new BlockIngQueue<>(queueCapatity);
        this.rejectPolicy=rejectPolicy;
    }

    class Worker extends Thread{
        private Runnable task;

        public Worker(Runnable task){
            this.task=task;
        }

        @Override
        public void run(){
            //当task不为空，执行任务
            //当task执行完毕，再从阻塞队列中获取值
            while (task!=null||(task=blockIngQueue.poll(timeOut,timeUnit))!=null ){
                try {
                    log.debug("正在执行。。。{}",task);
                    task.run();
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    task=null;
                }
            }
            synchronized(workers){
                log.debug("woker{}被移除",this);
                workers.remove(this);
            }
        }


    }
}



