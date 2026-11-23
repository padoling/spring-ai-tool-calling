package com.codeit.chat.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class HttpLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().equals(request.getContextPath() + "/api/chat");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!log.isInfoEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        long started = System.nanoTime();
        boolean completed = false;
        log.info("[HTTP REQUEST] {} {}", request.getMethod(), request.getRequestURI());
        try {
            filterChain.doFilter(request, cachedResponse);
            completed = true;
        }
        finally {
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            if (completed) {
                log.info("[HTTP RESPONSE] status={} duration={}ms body={}", cachedResponse.getStatus(), elapsed,
                        LogContent.preview(new String(cachedResponse.getContentAsByteArray(), StandardCharsets.UTF_8)));
            }
            else {
                log.warn("[HTTP FAILED] {} {} duration={}ms", request.getMethod(), request.getRequestURI(), elapsed);
            }
            cachedResponse.copyBodyToResponse();
        }
    }
}
