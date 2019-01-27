package org.kai.photostat.client;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import org.kai.photostat.boq.AppModule;
import org.kai.photostat.service.Annotations.PhotoStatServiceHost;
import org.kai.photostat.service.Annotations.PhotoStatServicePort;
import org.kai.photostat.service.GetPhotoStatRequest;
import org.kai.photostat.service.GetPhotoStatResponse;
import org.kai.photostat.service.PhotoStatServiceGrpc;

public final class PhotoStatClient {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ManagedChannel channel;
  private final PhotoStatServiceGrpc.PhotoStatServiceStub asyncStub;

  @Inject
  public PhotoStatClient(@PhotoStatServiceHost String host, @PhotoStatServicePort int port) {
    this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    asyncStub = PhotoStatServiceGrpc.newStub(channel);
  }

  private void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  public CountDownLatch getPhotoStat() {
    final CountDownLatch finishLatch = new CountDownLatch(1);
    GetPhotoStatRequest request =
        GetPhotoStatRequest.newBuilder()
            .addAllPhotoPaths(ImmutableList.of("C:/asdf.jpg", "C:/qwer.jpg"))
            .build();
    logger.atInfo().log("Start commuting with server");

    asyncStub.getPhotoStat(request, new StreamObserver<GetPhotoStatResponse>() {
      @Override
      public void onNext(GetPhotoStatResponse value) {
        logger.atInfo().log("The photo paths are: " + value.getDummyResponse());
      }

      @Override
      public void onError(Throwable t) {
        logger.atSevere().log("RPC failed: %s", t.getMessage());
        finishLatch.countDown();
      }

      @Override
      public void onCompleted() {
        logger.atInfo().log("Complete");
        finishLatch.countDown();
      }
    });
    return finishLatch;
  }

  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(new AppModule());
    final PhotoStatClient client = injector.getInstance(PhotoStatClient.class);
    CountDownLatch finishLatch = client.getPhotoStat();
    if (!finishLatch.await(1, TimeUnit.MINUTES)) {
      logger.atWarning().log("routeChat can not finish within 1 minutes");
    }
    client.shutdown();
  }
}
