package ml.pevgen.s5reactivetest1;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.jms.ConnectionFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@RestController
public class WebFluxController {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Bean
    public Publisher<Message<String>> jmsReactiveSource() {


        return IntegrationFlows
                .from(Jms.messageDrivenChannelAdapter(this.connectionFactory)
                        .destination("testQueue"))
//                .from("testQueue")
                .channel(MessageChannels.queue())
                .log(LoggingHandler.Level.DEBUG)
                .log()
                .toReactivePublisher();
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getPatientAlerts() {
        return Flux.from(jmsReactiveSource())
                .map(Message::getPayload);
    }

    @GetMapping(value = "/generate")
    public Flux<String> generateJmsMessage() {
        for (int i = 0; i < 5; i++) {
            this.jmsTemplate.convertAndSend("testQueue", "testMessage #" + (i + 1));
        }
        return Flux.just("OK");
    }


    @GetMapping(value = "/rr")
    public Mono<String> requestResponse() {
        Mono<String> mm;
        return Mono.from(jmsReactiveSource())
                .map(Message::getPayload)
                .timeout(Duration.ofSeconds(10), Mono.just("timeout"))
                ;
    }

    @GetMapping(value = "/rr2")
    public Mono<String> requestResponseWithParams(@RequestParam("id") String id) {

        long timeout = (long) new Random().nextInt(4);

        return Mono.defer(() -> anotherSource(id))
                .subscribeOn(Schedulers.elastic())  // in other thread
                .timeout(Duration.ofSeconds(timeout), Mono.just("timeout"))  // timeout
                ;
    }

    @GetMapping(value = "/rr-exc")
    public Mono<String> requestResponseWithExceptionHandler(@RequestParam("id") String id) {

        Mono<String> mm = Mono.defer(() -> blockedSource())
                .subscribeOn(Schedulers.elastic())  // in other thread
                .timeout(Duration.ofSeconds(2), Mono.error(new TimeoutException("specific timeout")));// timeout
        ;
        System.out.println(mm);
        return mm;
    }

    @GetMapping(value = "/rr-id")
    public Mono<String> requestResponseById(@RequestParam("id") String id) {

        Consumer<? super String> cc = (Consumer<String>) s -> {

        };
        return Mono.defer(() -> anotherSourceBlockedByById(id))
                .subscribeOn(Schedulers.elastic())  // in other thread
                .timeout(Duration.ofSeconds(1), Mono.error(new TimeoutException("timeout !!!")))  // timeout
                .doOnError(Exception.class, s -> {
                    idMap.remove(id);
                })
                ;
    }


    SynchronousQueue<String> synchronousQueue = new SynchronousQueue();

    public Mono<String> anotherSource(String id) {
        String event = null;
        try {
            event = synchronousQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Mono.just("xmlId:" + id + " \n event:" + event);
    }


    ConcurrentHashMap<String, CountDownLatch> idMap = new ConcurrentHashMap<>();


    public Mono<String> anotherSourceBlockedByById(String id) {

        try {
            CountDownLatch cd = new CountDownLatch(1);
            idMap.putIfAbsent(id, cd);
            cd.await();
            System.out.println("Start mono. id = " + id);
        } catch (InterruptedException e) {
            return Mono.error(e);
        }
        return Mono.just("xmlId:" + id + " \n event:" + id);
    }


    public Mono<String> blockedSource() {
        try {
            Thread.sleep(500000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Scheduled(fixedDelay = 15000)
    public void imitateGettingResponseMessageFromResponseQueue() {
        synchronousQueue.offer("event-response:" + LocalDateTime.now());
    }

//    @Scheduled(fixedDelay = 10000)
//    public void send1() {
//        idMap.remove("1").countDown();
//    }

    @Scheduled(fixedDelay = 10000)
    public void send2() {
        idMap.remove("2").countDown();
        System.out.println("Count down/ id = 2");

    }


}
