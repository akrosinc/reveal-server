package com.revealprecision.revealserver.config;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.MessageBodyWriterContext;
import org.jboss.resteasy.spi.interception.MessageBodyWriterInterceptor;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.jboss.resteasy.spi.metadata.ResourceMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@ServerInterceptor
class RestEasyLogger implements PreProcessInterceptor, MessageBodyWriterInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(RestEasyLogger.class);

  @Context
  private HttpServletRequest servletRequest;

  public ServerResponse preProcess(HttpRequest request,
      ResourceMethod resourceMethod) throws Failure,
      WebApplicationException {

    logger.info("Receiving request: {} ", servletRequest.getRequestURL());

    BufferedInputStream bis = new BufferedInputStream(request.getInputStream());
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    String content = "";
    int result;

    try {
      result = bis.read();
      while (result != -1) {
        byte b = (byte) result;
        buf.write(b);
        result = bis.read();
      }
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }

    try {
      content = buf.toString("UTF-8");
      ByteArrayInputStream bi = new ByteArrayInputStream(buf.toByteArray());
      request.setInputStream(bi);
    } catch (UnsupportedEncodingException ex) {
      logger.error(ex.getLocalizedMessage(), ex);
    }

    logger.info("\t\t" + content);

    return null;
  }

  @Override
  public void write(MessageBodyWriterContext mbwc) throws IOException, WebApplicationException {
    OutputStream oStream = mbwc.getOutputStream();
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    mbwc.setOutputStream(buf);
    mbwc.proceed();
    String content = buf.toString("UTF-8");
    oStream.write(buf.toByteArray());
    mbwc.setOutputStream(oStream);
    logger.info("\t\t" + content);
  }

  @Override
  public ServerResponse preProcess(HttpRequest request, ResourceMethodInvoker method)
      throws Failure, WebApplicationException {
    return null;
  }
}