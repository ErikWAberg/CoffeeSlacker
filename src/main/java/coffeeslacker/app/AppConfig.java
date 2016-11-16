package coffeeslacker.app;

import coffeeslacker.slack.SlackService;
import in.ashwanthkumar.slack.webhook.Slack;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@PropertySource("classpath:application.properties")
public class AppConfig {

    @Value(value="${slack.webHook}")
    String mSlackWebHook;

    @Value(value="${slack.token}")
    String mSlackToken;

    @Value(value="${slack.channel}")
    String mSlackChannel;

    @Value(value="${slack.debug.webHook}")
    String mSlackDebugWebHook;

    @Value(value="${slack.debug.channel}")
    String mSlackDebugChannel;

    @Value(value="${slack.debug.token}")
    String mSlackDebugToken;

    @Value(value="${slack.debug.user}")
    String mSlackDebugUser;

    private final String cDisplayName = "bean-sniffer";
    private final String cIcon = ":coffee:";

    private SlackService debugSlackService() {
        if(mSlackDebugChannel != null && mSlackDebugToken != null && mSlackDebugWebHook != null && mSlackDebugUser != null) {
            return new SlackService(mSlackDebugWebHook, mSlackDebugToken, mSlackDebugChannel, cDisplayName, cIcon, null);
        }
        return null;
    }

    @Bean
    public SlackService slackService() {
        final SlackService tSlackService = new SlackService(mSlackWebHook, mSlackToken, mSlackChannel, cDisplayName, cIcon, debugSlackService());
        return tSlackService;
    }
    
}
