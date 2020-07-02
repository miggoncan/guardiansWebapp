package guardians.webapp.controllers;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpClientErrorException.NotFound;

import lombok.extern.slf4j.Slf4j;

/**
 * This class is responsible for handling errors and exceptions
 * 
 * @author miggoncan
 */
@Slf4j
@ControllerAdvice
public class MyErrorController implements ErrorController {

	/**
	 * This code has been found on:
	 * https://www.baeldung.com/spring-boot-custom-error-page
	 */
	@RequestMapping("/error")
	public String handleError(HttpServletRequest request) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

		if (status != null) {
			Integer statusCode = Integer.valueOf(status.toString());
			if (statusCode == HttpStatus.NOT_FOUND.value()) {
				return "errors/error-404";
			} else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
				return "errors/error-500";
			}
		}

		log.error("Un unexpected error ocurred. Returning the general error template");
		return "errors/error";
	}

	@ExceptionHandler(NotFound.class)
	public String notFoundHandler(NotFound e) {
		log.info("Caught NotFound exception: " + e);
		return "errors/error-404";
	}
	
	@ExceptionHandler(IOException.class)
	public String ioExceptionHandler(IOException e) {
		log.info("Caught IOException: " + e);
		return "errors/error-io";
	}

	@Override
	public String getErrorPath() {
		return "error";
	}
}
