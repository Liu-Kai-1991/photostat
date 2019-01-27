package org.kai.photostat.service;

import com.google.common.flogger.FluentLogger;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import org.kai.photostat.boq.AppModule;
import org.kai.photostat.service.Annotations.PhotoStatServicePort;

final public class PhotoStatServiceRunner {
  private final int port;

  @Inject
  public PhotoStatServiceRunner(@PhotoStatServicePort int port){
    this.port = port;
  }

  private Server server;
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  private void start() throws IOException {

    server = ServerBuilder.forPort(port)
        .addService(new ProtoStatServiceImpl())
        .build()
        .start();
    logger.atInfo().log("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        logger.atInfo().log("*** shutting down gRPC server since JVM is shutting down");
        stopService();
        logger.atInfo().log("*** server shut down");
      }
    });
  }

  private void stopService() {
    if (server != null) {
      server.shutdown();
    }
  }

  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    Injector injector = Guice.createInjector(new AppModule());
    final PhotoStatServiceRunner server = injector.getInstance(PhotoStatServiceRunner.class);
    server.start();
    server.blockUntilShutdown();
  }
}
