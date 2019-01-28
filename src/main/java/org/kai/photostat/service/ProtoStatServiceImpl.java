package org.kai.photostat.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.annotations.NotNull;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.protobuf.Option;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.kai.photostat.service.MetadataAggregation.Builder;

class ProtoStatServiceImpl extends PhotoStatServiceGrpc.PhotoStatServiceImplBase {
  private final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public void getPhotoStat(
      GetPhotoStatRequest request, StreamObserver<GetPhotoStatResponse> responseObserver) {
    HashMap<PhotoMetadata, List<Timestamp>> photoMetaDataMap = new HashMap<>();
    logger.atInfo().log("Receive request:", request);
    long numProcessed = 0;
    for (String path : request.getPhotoPathsList()) {
      extractMetadataIntoMap(photoMetaDataMap, path);
      responseObserver.onNext(reportProgress(++numProcessed));
    }
    responseObserver.onNext(
        GetPhotoStatResponse.newBuilder()
            .setPhotoStatistics(getPhotoStatisticsFromMap(photoMetaDataMap))
            .build());
    responseObserver.onCompleted();
  }

  private PhotoStatistics getPhotoStatisticsFromMap(
      HashMap<PhotoMetadata, List<Timestamp>> photoMetaDataMap) {
    return PhotoStatistics.newBuilder()
        .addAllMetaDataAggregation(
            photoMetaDataMap.entrySet().stream()
                .map(
                    entry ->
                        MetadataAggregation.newBuilder()
                            .setCameraMake(entry.getKey().cameraMake())
                            .setExposureTime(entry.getKey().exposureTime())
                            .setFocalLength(entry.getKey().focalLength())
                            .setLensModel(entry.getKey().lensModel())
                            .setFNumber(entry.getKey().fNumber())
                            .addAllTimestamp(entry.getValue())
                            .build())
                .collect(ImmutableList.toImmutableList()))
        .build();
  }

  private GetPhotoStatResponse reportProgress(long numProcessed) {
    return GetPhotoStatResponse.newBuilder().setNumFinishedPhotos(numProcessed).build();
  }

  private void extractMetadataIntoMap(
      HashMap<PhotoMetadata, List<Timestamp>> photoMetaDataMap, String path) {
    File imageFile = new File(path);
    try {
      Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
      String exposureTime = null,
          focalLength = null,
          fNumber = null,
          cameraMake = null,
          lensModel = null;
      Date date = null;

      // Get ExifSubIFD Metadata
      ExifSubIFDDirectory exifSubIFDDirectory =
          metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
      if (exifSubIFDDirectory != null) {
        exposureTime = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
        focalLength = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
        fNumber = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_FNUMBER);
        lensModel = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_LENS_MODEL);
        date = exifSubIFDDirectory.getDate(ExifIFD0Directory.TAG_DATETIME_ORIGINAL);
      }

      // Get ExifIFD0 Metadata
      ExifIFD0Directory exifIFD0Directory =
          metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
      if (exifIFD0Directory != null) {
        cameraMake = exifIFD0Directory.getString(ExifIFD0Directory.TAG_MAKE);
      }

      PhotoMetadata photoMetadata =
          PhotoMetadata.create(
              Strings.nullToEmpty(exposureTime),
              Strings.nullToEmpty(focalLength),
              Strings.nullToEmpty(fNumber),
              Strings.nullToEmpty(cameraMake),
              Strings.nullToEmpty(lensModel));

      photoMetaDataMap
          .computeIfAbsent(photoMetadata, (d) -> new ArrayList<>())
          .add(
              date == null
                  ? Timestamp.newBuilder().setSeconds(0).build()
                  : Timestamp.newBuilder().setSeconds(date.getTime() / 1000).build());
    } catch (ImageProcessingException e) {
      logger.atWarning().log("Image processing exception for %s", path);
    } catch (IOException e) {
      logger.atWarning().log("IOException for %s", path);
    }
  }

  @AutoValue
  abstract static class PhotoMetadata {
    static PhotoMetadata create(
        String exposureTime,
        String focalLength,
        String fNumber,
        String cameraMake,
        String lensModel) {
      return new AutoValue_ProtoStatServiceImpl_PhotoMetadata(
          exposureTime, focalLength, fNumber, cameraMake, lensModel);
    }

    abstract String exposureTime();

    abstract String focalLength();

    abstract String fNumber();

    abstract String cameraMake();

    abstract String lensModel();
  }
}
