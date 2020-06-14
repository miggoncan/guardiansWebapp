package guardians.webapp.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

/**
 * This controller will serve the main page of this application
 * 
 * @author miggoncan
 */
@Controller
@Slf4j
public class RootController {
	/**
	 * @return The path to the thymeleaf template containing the home page (The path
	 *         is relative to resources/templates and does not contain the file
	 *         extension)
	 */
	@GetMapping("")
	public String getRoot() {
		log.info("Request to get the index file");
		return "index";
	}
}
