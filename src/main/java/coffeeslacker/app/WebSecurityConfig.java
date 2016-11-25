package coffeeslacker.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
@PropertySource("classpath:application.properties")
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger cLogger = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Value(value="${debug.user}")
    String mDebugUser;

    @Value(value="${debug.password}")
    String mDebugPassword;


    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        cLogger.info("Enabling debug user api access: " + mDebugUser);

        auth
                .inMemoryAuthentication()
                .withUser(mDebugUser).password(mDebugPassword).roles("USER");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.POST, "/api/debug/*").hasRole("USER")
                .antMatchers(HttpMethod.PUT, "/api/debug/*").hasRole("USER")
                .antMatchers(HttpMethod.GET, "/api/debug/*").hasRole("USER")
                .anyRequest().permitAll()
                .and().httpBasic();

    }



}