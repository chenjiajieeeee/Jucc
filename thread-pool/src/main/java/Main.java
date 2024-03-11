import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j(topic = "c.Main")
public class    Main {
    public static void main(String[] args) {
        ThreadPool threadPool=new ThreadPool(1,1000, TimeUnit.MILLISECONDS,1, ((queue, task) -> {
            queue.offer(task,500,TimeUnit.MILLISECONDS);
        }));

            for(int i=0;i<3;i++){
                final int j=i;
                threadPool.execute(()->{
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    log.debug("{}",j);
                });
            }



    }
}
