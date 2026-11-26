package com.codeforge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring MVC Web configuration.
 *
 * <p>Configures static resource locations so that the frontend files (HTML, CSS, JS, images)
 * are served directly by the Spring Boot server on port 8080.
 * Also configures view controllers to redirect the root path ("/") to "/welcome.html".</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path p1 = Paths.get("frontend").toAbsolutePath().normalize();
        Path p2 = Paths.get("..", "frontend").toAbsolutePath().normalize();
        Path frontendPath = Files.exists(p1) ? p1 : (Files.exists(p2) ? p2 : null);

        if (frontendPath != null) {
            String frontendUri = frontendPath.toUri().toString();
            if (!frontendUri.endsWith("/")) {
                frontendUri += "/";
            }
            registry.addResourceHandler("/**")
                    .addResourceLocations(frontendUri, "classpath:/static/", "classpath:/public/");
        } else {
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/", "classpath:/public/");
        }
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect root "/" to "/welcome.html"
        registry.addViewController("/").setViewName("redirect:/welcome.html");
        // Serve the unified SVG logo for default browser /favicon.ico requests
        registry.addViewController("/favicon.ico").setViewName("forward:/logo.svg");
    }
}
