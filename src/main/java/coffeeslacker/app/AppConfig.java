package coffeeslacker.app;

import coffeeslacker.slack.SlackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@PropertySource("classpath:application.properties")
public class AppConfig extends AsyncConfigurerSupport {

    @Value(value="${slack.webHook}")
    String mSlackWebHook;

    @Value(value="${slack.token}")
    String mSlackToken;

    @Value(value="${slack.channel}")
    String mSlackChannel;

    @Value(value="${slack.debugUser}")
    String mDebugUser;

    @Bean
    public SlackService slackService() {
        String tWebHook = mSlackWebHook;
        String tSlackToken = mSlackToken;
        String tSlackChannelName = mSlackChannel;
        String tDisplayName = "bean-sniffer";
        String tIcon = ":coffee:";
        return new SlackService(tWebHook, tSlackToken, tSlackChannelName, tDisplayName, tIcon, mDebugUser);
    }

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("BrewExecutor-");
        executor.initialize();
        return executor;
    }

    
}
