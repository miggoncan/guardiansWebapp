package guardians.webapp.services;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * This is the base class for all services in the application. It contains some
 * general methods and properties needed by the rest of the services
 * 
 * @author miggoncan
 */
@Slf4j
public class MyService {
	@Autowired
	protected Traverson traverson;
	@Autowired
	protected RestTemplateBuilder restTemplateBuilder;
	
	@Value("${api.uri}")
	protected String restUri;
	
	@Value("${api.links.doctors}")
	protected String doctorsLink;
	@Value("${api.links.doctor}")
	protected String doctorLink;
	@Value("${api.links.doctorUpdate}")
	protected String updateDoctorLink;
	@Value("${api.links.newDoctor}")
	protected String newDoctorLink;
	@Value("${api.links.shiftconfs}")
	protected String shiftConfsLink;
	@Value("${api.links.shiftconf}")
	protected String shiftConfLink;
	@Value("${api.links.calendars}")
	protected String calendarsLink;
	@Value("${api.links.calendar}")
	protected String calendarLink;
	@Value("${api.links.schedule}")
	protected String scheduleLink;
	@Value("${api.links.schedules}")
	protected String schedulesLink;
	
	/**
	 * This method will return the needed headers to make a request to the REST
	 * service
	 * 
	 * @return The headers that should be used in the request
	 */
	protected HttpHeaders getSessionHeaders() {
		log.info("Request to get session headers");
		HttpHeaders headers = new HttpHeaders();
		ResponseEntity<Object> resp = restTemplateBuilder.build()
				.getForEntity(restUri, Object.class);
		log.debug("The response is: " + resp);
		List<String> cookiesStr = resp.getHeaders().get(HttpHeaders.SET_COOKIE);
		log.debug("The list of cookies as Strings is: " + cookiesStr);
		if (cookiesStr != null && !cookiesStr.isEmpty()) {
			List<HttpCookie> cookies = new ArrayList<>();
			// Parse the list of cookiesStr and add them to the cookies list
			cookiesStr.stream().map((c) -> HttpCookie.parse(c)).forEachOrdered((cook) -> {
                cook.forEach((a) -> {
                    HttpCookie cookieExists = cookies.stream().filter(x -> a.getName().equals(x.getName())).findAny().orElse(null);
                    if (cookieExists != null) {
                        cookies.remove(cookieExists);
                    }
                    cookies.add(a);
                });
            });
			log.debug("The extracted cookies are: " + cookies);
			StringBuilder sb = new StringBuilder();
            for (HttpCookie cookie : cookies) {
                sb.append(cookie.toString()).append(";");
            }
            String mappedCookies = sb.toString();
            log.debug("The cookies to be added are: " + mappedCookies);
            headers.add(HttpHeaders.COOKIE, mappedCookies);
		} else {
			log.debug("No cookies to set found on the response");
		}
		log.info("The created headers are: " + headers);
		return headers;
	}
	
	/**
	 * Get a required {@link Link} from the root resource.
	 * 
	 * Note a link is considered required if it will always be present in the
	 * response.
	 * 
	 * @param rel The relation of the required link
	 * @return The link found
	 */
	protected Link getRootRequiredLink(String rel) {
		log.info("Request to get root required link with rel: " + rel);
		EntityModel<?> rootResource = restTemplateBuilder.build()
				.getForObject(restUri, EntityModel.class);
		log.debug("The root resource is: " + rootResource);
		Link link = rootResource.getRequiredLink(rel);
		log.info("The found link is: " + link);
		return link;
	}
}
