package ml.pevgen.s5reactivetest1;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.jms.ConnectionFactory;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

@SpringBootApplication
@RestController
public class S5ReactiveTest1Application {

    public static void main(String[] args) {
        SpringApplication.run(S5ReactiveTest1Application.class, args);
    }


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
//        for (int i = 0; i < ; i++) {
//        this.jmsTemplate.convertAndSend("testQueue", "testMessage #" + (0 + 1));
//        }
        Mono<String> mm;
//            mm = Mono.from(jmsReactiveSource())
//                    .timeout(Duration.ofSeconds(2), Mono.error(new RuntimeException("forced " + "failure")))
//                    //.doAfterTerminate(() -> System.out.println("Do anything with your string" + "sdfg"))
//                    .map(Message::getPayload)
            mm = Mono.from(jmsReactiveSource())
                    .timeout(Duration.ofSeconds(2))
                    .map(Message::getPayload)
                    .timeout(Duration.ofSeconds(2), Mono.just("timeout"))
//                    .timeout(Duration.ofSeconds(2), Mono.error(new TimeoutException("Timeout after " + Duration.ofSeconds(2))))
//                    .doAfterTerminate(() -> System.out.println("Do anything with your string !!!!!"))

            ;

        System.out.println(mm);
        return mm;
    }

}
