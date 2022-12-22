package com.revealprecision.revealserver;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Order(1)
public class GzipDecompressionFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig){
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    boolean isGzipped = request.getHeader(HttpHeaders.CONTENT_ENCODING) != null && request
        .getHeader(HttpHeaders.CONTENT_ENCODING).contains("gzip");
    boolean requestTypeSupported = "POST".equals(request.getMethod());
    if (isGzipped && !requestTypeSupported) {
      throw new IllegalStateException(
          request.getMethod() + " does not supports gzipped body of parameters."
              + " Only POST requests are currently supported.");
    }
    String requestURI = ((HttpServletRequest) servletRequest).getRequestURI();
    if (isGzipped) {
      log.debug("Decompressing POST request to {}", requestURI);
      request = new GzippedInputStreamWrapper((HttpServletRequest) servletRequest);
    } else if (requestTypeSupported) {
      log.debug("POST body to {} does not require decompression. Skipping filter", requestURI);
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {

  }

  static final class GzippedInputStreamWrapper extends HttpServletRequestWrapper {

    static final String DEFAULT_ENCODING = "ISO-8859-1";
    public static final String FORM_DATA = "application/x-www-form-urlencoded";
    private byte[] bytes;

    GzippedInputStreamWrapper(final HttpServletRequest request) throws IOException {
      super(request);
      try {
        final InputStream in = new GZIPInputStream(request.getInputStream());
        bytes = ByteStreams.toByteArray(in);
      } catch (EOFException e) {
        bytes = new byte[0];
      }
    }

    @Override
    public ServletInputStream getInputStream() {
      final ByteArrayInputStream sourceStream = new ByteArrayInputStream(bytes);
      return new ServletInputStream() {
        private ReadListener readListener;

        @Override
        public boolean isFinished() {
          return sourceStream.available() <= 0;
        }

        @Override
        public boolean isReady() {
          return sourceStream.available() > 0;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
          this.readListener = readListener;
        }

        public int read() {
          return sourceStream.read();
        }

        @Override
        public void close() throws IOException {
          super.close();
          sourceStream.close();
        }
      };
    }

    @Override
    public Map<String, String[]> getParameterMap() {
      String contentEncodingHeader = getHeader(HttpHeaders.CONTENT_TYPE);
      if (!Strings.isNullOrEmpty(contentEncodingHeader) && contentEncodingHeader
          .contains(FORM_DATA)) {
        Map<String, String[]> params = new HashMap<>(super.getParameterMap());
        try {
          params.putAll(parseParams(new String(bytes)));
        } catch (UnsupportedEncodingException e) {
          log.error("Could not decompress incoming message!", e);
        }
        return params;
      } else {
        return super.getParameterMap();
      }
    }

    private Map<String, String[]> parseParams(final String body)
        throws UnsupportedEncodingException {
      String characterEncoding = getCharacterEncoding();
      if (null == characterEncoding) {
        characterEncoding = DEFAULT_ENCODING;
      }
      final Multimap<String, String> parameters = ArrayListMultimap.create();
      for (String pair : body.split("&")) {
        if (Strings.isNullOrEmpty(pair)) {
          continue;
        }
        int idx = pair.indexOf('=');

        String key;
        if (idx > 0) {
          key = URLDecoder.decode(pair.substring(0, idx), characterEncoding);
        } else {
          key = pair;
        }
        String value;
        if (idx > 0 && pair.length() > idx + 1) {
          value = URLDecoder.decode(pair.substring(idx + 1), characterEncoding);
        } else {
          value = null;
        }
        parameters.put(key, value);
      }

      return parameters.asMap().entrySet().stream().collect(Collectors
          .toMap(Map.Entry::getKey, kv -> Iterables.toArray(kv.getValue(), String.class)));
    }
  }
}
