import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j(topic = "c.Main")
public class    Main {
    public static void main(String[] args) {
        ThreadPool threadPool=new ThreadPool(2,1000, TimeUnit.MILLISECONDS,10);


            for(int i=0;i<5;i++){
                final int j=i;
                threadPool.execute(()->{
                    try {
                        Thread.sleep(1000000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    log.debug("{}",j);
                });
            }



    }
}
