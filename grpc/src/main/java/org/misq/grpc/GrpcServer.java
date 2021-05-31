package org.misq.grpc;


import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.misq.api.CoreApi;
import org.misq.common.di.annotation.Singleton;
import org.misq.common.threadmodel.UserThread;
import org.misq.wallet.installer.WalletInstallerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

import static java.util.concurrent.TimeUnit.SECONDS;

@Singleton
public class GrpcServer {
    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

    // Use grpcurl to test endpoints.
    // https://github.com/grpc/grpc-java/blob/master/documentation/server-reflection-tutorial.md
    // https://offensivedefence.co.uk/posts/grpc-attack-surface
    // Examples:
    // $ grpcurl --plaintext   localhost:9999 list
    //      grpc.reflection.v1alpha.ServerReflection
    //      io.misq.protobuffer.GetVersion
    //      io.misq.protobuffer.Help
    //      io.misq.protobuffer.Wallets
    //
    // $ grpcurl --plaintext   localhost:9999 grpc.proto.Wallets/GetBalance
    // $ grpcurl --plaintext   localhost:9999 grpc.proto.Help/GetMethodHelp
    // $ grpcurl --plaintext   localhost:9999 grpc.proto.P2P/GetPeers
    // $ grpcurl --plaintext   localhost:9999 grpc.proto.GetVersion/GetVersion
    // $ grpcurl --plaintext   localhost:9999 grpc.proto.WalletInstaller/InstallWallet
    //
    // Describe services
    // $ grpcurl --plaintext   localhost:9999 describe grpc.proto.P2P
    // $ grpcurl --plaintext   localhost:9999 describe grpc.proto.Wallets
    // $ grpcurl --plaintext   localhost:9999 describe grpc.proto.WalletInstaller
    //
    // Enabling gRPC reflection in Java:
    // https://github.com/grpc/grpc-java/blob/master/documentation/server-reflection-tutorial.md#enable-server-reflection

    private final Server server;

    public GrpcServer(CoreApi coreApi, WalletInstallerApi walletInstallerApi) {
        this.server = ServerBuilder.forPort(9999)
                .executor(UserThread.getExecutor())
                .addService(new GrpcHelpService(coreApi))
                .addService(new GrpcP2PService(coreApi))
                .addService(new GrpcVersionService(coreApi))
                .addService(new GrpcWalletsService(coreApi))
                .addService(new GrpcWalletInstallerService(walletInstallerApi))
                // .intercept(new PasswordAuthInterceptor())
                .addService(ProtoReflectionService.newInstance())
                .build();
    }

    public void start() {
        try {
            server.start();
            log.info("Listening on port {}", server.getPort());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public void shutdown() {
        log.info("Server shutdown started");
        server.shutdown();
        try {
            if (server.awaitTermination(1, SECONDS)) {
                log.info("Server shutdown complete");
                return;
            }
            server.shutdownNow();
            if (server.awaitTermination(1, SECONDS)) {
                log.info("Forced server shutdown complete");
                return;
            }
        } catch (InterruptedException ex) {
            log.error("", ex);
        }
        throw new RuntimeException("Unable to shutdown server");
    }
}
