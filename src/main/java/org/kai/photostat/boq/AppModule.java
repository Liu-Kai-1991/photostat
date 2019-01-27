package org.kai.photostat.boq;

import com.google.inject.AbstractModule;
import org.kai.photostat.service.Annotations.PhotoStatServiceHost;
import org.kai.photostat.service.Annotations.PhotoStatServicePort;

public class AppModule extends AbstractModule{

  @Override
  protected void configure() {
    bind(Integer.class)
        .annotatedWith(PhotoStatServicePort.class)
        .toInstance(50001);
    bind(String.class)
        .annotatedWith(PhotoStatServiceHost.class)
        .toInstance("localhost");
  }
}
