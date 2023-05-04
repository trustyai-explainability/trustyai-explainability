package org.kie.trustyai.connectors.kserve.v2.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Inference Server GRPC endpoints.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.52.1)",
    comments = "Source: grpc_predict_v2.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class GRPCInferenceServiceGrpc {

  private GRPCInferenceServiceGrpc() {}

  public static final String SERVICE_NAME = "inference.GRPCInferenceService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveRequest,
      org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveResponse> getServerLiveMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ServerLive",
      requestType = org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveRequest.class,
      responseType = org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveRequest,
      org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveResponse> getServerLiveMethod() {
    io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveRequest, org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveResponse> getServerLiveMethod;
    if ((getServerLiveMethod = GRPCInferenceServiceGrpc.getServerLiveMethod) == null) {
      synchronized (GRPCInferenceServiceGrpc.class) {
        if ((getServerLiveMethod = GRPCInferenceServiceGrpc.getServerLiveMethod) == null) {
          GRPCInferenceServiceGrpc.getServerLiveMethod = getServerLiveMethod =
              io.grpc.MethodDescriptor.<org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveRequest, org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ServerLive"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GRPCInferenceServiceMethodDescriptorSupplier("ServerLive"))
              .build();
        }
      }
    }
    return getServerLiveMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyRequest,
      org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyResponse> getServerReadyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ServerReady",
      requestType = org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyRequest.class,
      responseType = org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyRequest,
      org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyResponse> getServerReadyMethod() {
    io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyRequest, org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyResponse> getServerReadyMethod;
    if ((getServerReadyMethod = GRPCInferenceServiceGrpc.getServerReadyMethod) == null) {
      synchronized (GRPCInferenceServiceGrpc.class) {
        if ((getServerReadyMethod = GRPCInferenceServiceGrpc.getServerReadyMethod) == null) {
          GRPCInferenceServiceGrpc.getServerReadyMethod = getServerReadyMethod =
              io.grpc.MethodDescriptor.<org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyRequest, org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ServerReady"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GRPCInferenceServiceMethodDescriptorSupplier("ServerReady"))
              .build();
        }
      }
    }
    return getServerReadyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyRequest,
      org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyResponse> getModelReadyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ModelReady",
      requestType = org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyRequest.class,
      responseType = org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyRequest,
      org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyResponse> getModelReadyMethod() {
    io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyRequest, org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyResponse> getModelReadyMethod;
    if ((getModelReadyMethod = GRPCInferenceServiceGrpc.getModelReadyMethod) == null) {
      synchronized (GRPCInferenceServiceGrpc.class) {
        if ((getModelReadyMethod = GRPCInferenceServiceGrpc.getModelReadyMethod) == null) {
          GRPCInferenceServiceGrpc.getModelReadyMethod = getModelReadyMethod =
              io.grpc.MethodDescriptor.<org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyRequest, org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ModelReady"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GRPCInferenceServiceMethodDescriptorSupplier("ModelReady"))
              .build();
        }
      }
    }
    return getModelReadyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataRequest,
      org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataResponse> getServerMetadataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ServerMetadata",
      requestType = org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataRequest.class,
      responseType = org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataRequest,
      org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataResponse> getServerMetadataMethod() {
    io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataRequest, org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataResponse> getServerMetadataMethod;
    if ((getServerMetadataMethod = GRPCInferenceServiceGrpc.getServerMetadataMethod) == null) {
      synchronized (GRPCInferenceServiceGrpc.class) {
        if ((getServerMetadataMethod = GRPCInferenceServiceGrpc.getServerMetadataMethod) == null) {
          GRPCInferenceServiceGrpc.getServerMetadataMethod = getServerMetadataMethod =
              io.grpc.MethodDescriptor.<org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataRequest, org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ServerMetadata"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GRPCInferenceServiceMethodDescriptorSupplier("ServerMetadata"))
              .build();
        }
      }
    }
    return getServerMetadataMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataRequest,
      org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataResponse> getModelMetadataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ModelMetadata",
      requestType = org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataRequest.class,
      responseType = org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataRequest,
      org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataResponse> getModelMetadataMethod() {
    io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataRequest, org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataResponse> getModelMetadataMethod;
    if ((getModelMetadataMethod = GRPCInferenceServiceGrpc.getModelMetadataMethod) == null) {
      synchronized (GRPCInferenceServiceGrpc.class) {
        if ((getModelMetadataMethod = GRPCInferenceServiceGrpc.getModelMetadataMethod) == null) {
          GRPCInferenceServiceGrpc.getModelMetadataMethod = getModelMetadataMethod =
              io.grpc.MethodDescriptor.<org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataRequest, org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ModelMetadata"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GRPCInferenceServiceMethodDescriptorSupplier("ModelMetadata"))
              .build();
        }
      }
    }
    return getModelMetadataMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest,
      org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse> getModelInferMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ModelInfer",
      requestType = org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest.class,
      responseType = org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest,
      org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse> getModelInferMethod() {
    io.grpc.MethodDescriptor<org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest, org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse> getModelInferMethod;
    if ((getModelInferMethod = GRPCInferenceServiceGrpc.getModelInferMethod) == null) {
      synchronized (GRPCInferenceServiceGrpc.class) {
        if ((getModelInferMethod = GRPCInferenceServiceGrpc.getModelInferMethod) == null) {
          GRPCInferenceServiceGrpc.getModelInferMethod = getModelInferMethod =
              io.grpc.MethodDescriptor.<org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest, org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ModelInfer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GRPCInferenceServiceMethodDescriptorSupplier("ModelInfer"))
              .build();
        }
      }
    }
    return getModelInferMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GRPCInferenceServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GRPCInferenceServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GRPCInferenceServiceStub>() {
        @java.lang.Override
        public GRPCInferenceServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GRPCInferenceServiceStub(channel, callOptions);
        }
      };
    return GRPCInferenceServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GRPCInferenceServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GRPCInferenceServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GRPCInferenceServiceBlockingStub>() {
        @java.lang.Override
        public GRPCInferenceServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GRPCInferenceServiceBlockingStub(channel, callOptions);
        }
      };
    return GRPCInferenceServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static GRPCInferenceServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GRPCInferenceServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GRPCInferenceServiceFutureStub>() {
        @java.lang.Override
        public GRPCInferenceServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GRPCInferenceServiceFutureStub(channel, callOptions);
        }
      };
    return GRPCInferenceServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Inference Server GRPC endpoints.
   * </pre>
   */
  public static abstract class GRPCInferenceServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * The ServerLive API indicates if the inference server is able to receive 
     * and respond to metadata and inference requests.
     * </pre>
     */
    public void serverLive(org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveRequest request,
        io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getServerLiveMethod(), responseObserver);
    }

    /**
     * <pre>
     * The ServerReady API indicates if the server is ready for inferencing.
     * </pre>
     */
    public void serverReady(org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyRequest request,
        io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getServerReadyMethod(), responseObserver);
    }

    /**
     * <pre>
     * The ModelReady API indicates if a specific model is ready for inferencing.
     * </pre>
     */
    public void modelReady(org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyRequest request,
        io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getModelReadyMethod(), responseObserver);
    }

    /**
     * <pre>
     * The ServerMetadata API provides information about the server. Errors are 
     * indicated by the google.rpc.Status returned for the request. The OK code 
     * indicates success and other codes indicate failure.
     * </pre>
     */
    public void serverMetadata(org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataRequest request,
        io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getServerMetadataMethod(), responseObserver);
    }

    /**
     * <pre>
     * The per-model metadata API provides information about a model. Errors are 
     * indicated by the google.rpc.Status returned for the request. The OK code 
     * indicates success and other codes indicate failure.
     * </pre>
     */
    public void modelMetadata(org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataRequest request,
        io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getModelMetadataMethod(), responseObserver);
    }

    /**
     * <pre>
     * The ModelInfer API performs inference using the specified model. Errors are
     * indicated by the google.rpc.Status returned for the request. The OK code 
     * indicates success and other codes indicate failure.
     * </pre>
     */
    public void modelInfer(org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest request,
        io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getModelInferMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getServerLiveMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveRequest,
                org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveResponse>(
                  this, METHODID_SERVER_LIVE)))
          .addMethod(
            getServerReadyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyRequest,
                org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyResponse>(
                  this, METHODID_SERVER_READY)))
          .addMethod(
            getModelReadyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyRequest,
                org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyResponse>(
                  this, METHODID_MODEL_READY)))
          .addMethod(
            getServerMetadataMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataRequest,
                org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataResponse>(
                  this, METHODID_SERVER_METADATA)))
          .addMethod(
            getModelMetadataMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataRequest,
                org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataResponse>(
                  this, METHODID_MODEL_METADATA)))
          .addMethod(
            getModelInferMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest,
                org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse>(
                  this, METHODID_MODEL_INFER)))
          .build();
    }
  }

  /**
   * <pre>
   * Inference Server GRPC endpoints.
   * </pre>
   */
  public static final class GRPCInferenceServiceStub extends io.grpc.stub.AbstractAsyncStub<GRPCInferenceServiceStub> {
    private GRPCInferenceServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GRPCInferenceServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GRPCInferenceServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * The ServerLive API indicates if the inference server is able to receive 
     * and respond to metadata and inference requests.
     * </pre>
     */
    public void serverLive(org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveRequest request,
        io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getServerLiveMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * The ServerReady API indicates if the server is ready for inferencing.
     * </pre>
     */
    public void serverReady(org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyRequest request,
        io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getServerReadyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * The ModelReady API indicates if a specific model is ready for inferencing.
     * </pre>
     */
    public void modelReady(org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyRequest request,
        io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getModelReadyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * The ServerMetadata API provides information about the server. Errors are 
     * indicated by the google.rpc.Status returned for the request. The OK code 
     * indicates success and other codes indicate failure.
     * </pre>
     */
    public void serverMetadata(org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataRequest request,
        io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getServerMetadataMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * The per-model metadata API provides information about a model. Errors are 
     * indicated by the google.rpc.Status returned for the request. The OK code 
     * indicates success and other codes indicate failure.
     * </pre>
     */
    public void modelMetadata(org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataRequest request,
        io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getModelMetadataMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * The ModelInfer API performs inference using the specified model. Errors are
     * indicated by the google.rpc.Status returned for the request. The OK code 
     * indicates success and other codes indicate failure.
     * </pre>
     */
    public void modelInfer(org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest request,
        io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getModelInferMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Inference Server GRPC endpoints.
   * </pre>
   */
  public static final class GRPCInferenceServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<GRPCInferenceServiceBlockingStub> {
    private GRPCInferenceServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GRPCInferenceServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GRPCInferenceServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * The ServerLive API indicates if the inference server is able to receive 
     * and respond to metadata and inference requests.
     * </pre>
     */
    public org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveResponse serverLive(org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getServerLiveMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * The ServerReady API indicates if the server is ready for inferencing.
     * </pre>
     */
    public org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyResponse serverReady(org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getServerReadyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * The ModelReady API indicates if a specific model is ready for inferencing.
     * </pre>
     */
    public org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyResponse modelReady(org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getModelReadyMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * The ServerMetadata API provides information about the server. Errors are 
     * indicated by the google.rpc.Status returned for the request. The OK code 
     * indicates success and other codes indicate failure.
     * </pre>
     */
    public org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataResponse serverMetadata(org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getServerMetadataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * The per-model metadata API provides information about a model. Errors are 
     * indicated by the google.rpc.Status returned for the request. The OK code 
     * indicates success and other codes indicate failure.
     * </pre>
     */
    public org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataResponse modelMetadata(org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getModelMetadataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * The ModelInfer API performs inference using the specified model. Errors are
     * indicated by the google.rpc.Status returned for the request. The OK code 
     * indicates success and other codes indicate failure.
     * </pre>
     */
    public org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse modelInfer(org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getModelInferMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Inference Server GRPC endpoints.
   * </pre>
   */
  public static final class GRPCInferenceServiceFutureStub extends io.grpc.stub.AbstractFutureStub<GRPCInferenceServiceFutureStub> {
    private GRPCInferenceServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GRPCInferenceServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GRPCInferenceServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * The ServerLive API indicates if the inference server is able to receive 
     * and respond to metadata and inference requests.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveResponse> serverLive(
        org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getServerLiveMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * The ServerReady API indicates if the server is ready for inferencing.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyResponse> serverReady(
        org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getServerReadyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * The ModelReady API indicates if a specific model is ready for inferencing.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyResponse> modelReady(
        org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getModelReadyMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * The ServerMetadata API provides information about the server. Errors are 
     * indicated by the google.rpc.Status returned for the request. The OK code 
     * indicates success and other codes indicate failure.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataResponse> serverMetadata(
        org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getServerMetadataMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * The per-model metadata API provides information about a model. Errors are 
     * indicated by the google.rpc.Status returned for the request. The OK code 
     * indicates success and other codes indicate failure.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataResponse> modelMetadata(
        org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getModelMetadataMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * The ModelInfer API performs inference using the specified model. Errors are
     * indicated by the google.rpc.Status returned for the request. The OK code 
     * indicates success and other codes indicate failure.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse> modelInfer(
        org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getModelInferMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SERVER_LIVE = 0;
  private static final int METHODID_SERVER_READY = 1;
  private static final int METHODID_MODEL_READY = 2;
  private static final int METHODID_SERVER_METADATA = 3;
  private static final int METHODID_MODEL_METADATA = 4;
  private static final int METHODID_MODEL_INFER = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final GRPCInferenceServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(GRPCInferenceServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SERVER_LIVE:
          serviceImpl.serverLive((org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveRequest) request,
              (io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ServerLiveResponse>) responseObserver);
          break;
        case METHODID_SERVER_READY:
          serviceImpl.serverReady((org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyRequest) request,
              (io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ServerReadyResponse>) responseObserver);
          break;
        case METHODID_MODEL_READY:
          serviceImpl.modelReady((org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyRequest) request,
              (io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ModelReadyResponse>) responseObserver);
          break;
        case METHODID_SERVER_METADATA:
          serviceImpl.serverMetadata((org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataRequest) request,
              (io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ServerMetadataResponse>) responseObserver);
          break;
        case METHODID_MODEL_METADATA:
          serviceImpl.modelMetadata((org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataRequest) request,
              (io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ModelMetadataResponse>) responseObserver);
          break;
        case METHODID_MODEL_INFER:
          serviceImpl.modelInfer((org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest) request,
              (io.grpc.stub.StreamObserver<org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class GRPCInferenceServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    GRPCInferenceServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.kie.trustyai.connectors.kserve.v2.grpc.GrpcPredictV2.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("GRPCInferenceService");
    }
  }

  private static final class GRPCInferenceServiceFileDescriptorSupplier
      extends GRPCInferenceServiceBaseDescriptorSupplier {
    GRPCInferenceServiceFileDescriptorSupplier() {}
  }

  private static final class GRPCInferenceServiceMethodDescriptorSupplier
      extends GRPCInferenceServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    GRPCInferenceServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (GRPCInferenceServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GRPCInferenceServiceFileDescriptorSupplier())
              .addMethod(getServerLiveMethod())
              .addMethod(getServerReadyMethod())
              .addMethod(getModelReadyMethod())
              .addMethod(getServerMetadataMethod())
              .addMethod(getModelMetadataMethod())
              .addMethod(getModelInferMethod())
              .build();
        }
      }
    }
    return result;
  }
}
