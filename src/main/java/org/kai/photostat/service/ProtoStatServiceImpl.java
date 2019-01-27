package org.kai.photostat.service;

import com.drew.imaging.ImageMetadataReader;
import com.google.common.flogger.FluentLogger;
import io.grpc.stub.StreamObserver;

class ProtoStatServiceImpl extends PhotoStatServiceGrpc.PhotoStatServiceImplBase {
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public void getPhotoStat(
      GetPhotoStatRequest request, StreamObserver<GetPhotoStatResponse> responseObserver) {
    logger.atInfo().log("Receive request:", request);
    for (String path : request.getPhotoPathsList()) {
      responseObserver.onNext(GetPhotoStatResponse.newBuilder().setDummyResponse(path).build());
    }
    responseObserver.onCompleted();
  }
}
