package w4cash;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiPrefixForwardFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String servletPath = request.getServletPath();
		if (!servletPath.equals("/api") && !servletPath.startsWith("/api/")) {
			filterChain.doFilter(request, response);
			return;
		}

		String targetPath = servletPath.equals("/api") ? "/" : servletPath.substring(4);
		RequestDispatcher dispatcher = request.getRequestDispatcher(targetPath);
		dispatcher.forward(request, response);
	}
}