package guardians.webapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableWebSecurity
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	@Value("${auth.user.username}")
	private String username;
	@Value("${auth.user.password}")
	private String password;
	
//	@Value("#{url.login}")
	// TODO this values should be injected
	private String loginURL = "/guardians/login";
//	@Value("#{url.logout = /guardians/logout}")
	private String logoutURL = "/guardians/logout";
//	@Value("#{url.home}")
	private String homeURL = "/guardians/";
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		auth
			.inMemoryAuthentication()
			.withUser(username)
			.password(encoder.encode(password))
			.roles("USER");
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// TODO allow CSRF
		http.csrf().disable();
		http
			.formLogin()
				.loginProcessingUrl(loginURL)
				.defaultSuccessUrl(homeURL)
				.and()
			.logout()
				.logoutUrl(logoutURL)
				.logoutSuccessUrl(loginURL)
				.invalidateHttpSession(true)
				.and()
			.authorizeRequests()
				.anyRequest().authenticated()
				.and()
			.formLogin()
				.and()
			.httpBasic();
	}
}
