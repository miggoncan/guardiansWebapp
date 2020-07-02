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
			.requiresChannel()
				.anyRequest().requiresSecure()
				.and()
			.formLogin()
				.permitAll()
				.and()
			.logout()
				.invalidateHttpSession(true)
				.permitAll()
				.and()
			.authorizeRequests()
				.anyRequest().authenticated();
	}
}
