package com.jd.oxygent.core.oxygent.samples.server.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class MimeTypeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;
        String requestURI = req.getRequestURI();
        if (requestURI.endsWith(".svg") || requestURI.endsWith(".svgz")) {
            resp.setContentType("image/svg+xml");
        }
        filterChain.doFilter(req, resp);
    }
}