package org.mineskin.hiatus;

import okhttp3.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MineSkinHiatus {

    private static final Executor REQUEST_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final ScheduledExecutorService PING_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    static final Logger LOGGER = Logger.getLogger("MineSkinHiatus");

    private static final byte[] EMPTY_BODY_CONTENT = "{}".getBytes(StandardCharsets.UTF_8);
    private static final RequestBody EMPTY_BODY = RequestBody.create(EMPTY_BODY_CONTENT);


    public static MineSkinHiatus newInstance(String version, File configFile) {
        return new MineSkinHiatus(version, configFile);
    }

    private final String version;
    private final File configFile;
    private final OkHttpClient httpClient;

    private Account account;
    private ScheduledFuture<?> scheduledPing;


    private MineSkinHiatus(String version, File configFile) {
        this.version = version;
        this.configFile = configFile;
        this.httpClient = new OkHttpClient.Builder().build();
    }

    public void setAccount(Account account) {
        LOGGER.log(Level.INFO, "Setting Account to {0}", new Object[]{account.getUuid()});
        this.account = account;
        HiatusConfig.load(configFile).thenCompose(config -> {
            config.putAccount(account);
            return HiatusConfig.save(this.configFile, config);
        });
    }

    public void deleteAccount(UUID uuid) {
        HiatusConfig.load(configFile).thenCompose(config -> {
            config.removeAccount(uuid);
            cancelPing();
            return HiatusConfig.save(this.configFile, config);
        });
    }

    public void trySetAccountFromUuid(UUID uuid) {
        HiatusConfig.load(configFile).thenAccept(config -> {
            Account account = config.getAccount(uuid);
            if (account != null) {
                this.account = account;
                LOGGER.log(Level.INFO, "Loaded Account info from UUID {0}", new Object[]{uuid});
            }
        });
    }

    public CompletableFuture<Map<UUID, String>> getAccounts() {
        return HiatusConfig.load(configFile).thenApply(HiatusConfig::getAccounts);
    }

    //<editor-fold desc="Request Stuff">

    private Request.Builder newRequest(String endpoint) {
        Request.Builder builder = new Request.Builder()
                .url("https://nugget.api.mineskin.org/" + endpoint)//TODO
                .header("User-Agent", "MineSkin-Hiatus/" + version);
        if (this.account != null) {
            builder.header("Authorization", "Bearer " + this.account.getAuthHeader());
        }
        return builder;
    }

    private CompletableFuture<Optional<Response>> sendRequest(Request request) {
        LOGGER.log(Level.INFO, "Request: {0} {1}", new Object[]{request.method(), request.url()});
        return CompletableFuture.supplyAsync(() -> {
            try {
                Response response = httpClient.newCall(request).execute();
                LOGGER.log(Level.INFO, "  Response: {0} {1}", new Object[]{response.code(), response.message()});
                ResponseBody body = response.body();
                if (body != null) {
                    LOGGER.log(Level.INFO, "  " + body.string());
                }
                return Optional.of(response);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to send request (" + request.url() + ")", e);
            }
            return Optional.empty();
        }, REQUEST_EXECUTOR);
    }

    private void notifyGameLaunch() {
        if (this.account == null) return;
        sendRequest(newRequest("hiatus/launch").post(EMPTY_BODY).build());
    }

    private void notifyGameExit() {
        if (this.account == null) return;
        sendRequest(newRequest("hiatus/exit").post(EMPTY_BODY).build());
    }

    private void sendPing() {
        if (this.account == null) return;
        sendRequest(newRequest("hiatus/ping").post(EMPTY_BODY).build());
    }

    //</editor-fold>


    public void onGameLaunching() {
        if (this.account == null) return;
        notifyGameLaunch();
        cancelPing();
        sendPingAndRepeat();
    }

    public void onGameClosing() {
        if (this.account == null) return;
        notifyGameExit();
    }

    public void sendPingAndRepeat() {
        PING_EXECUTOR.schedule(this::sendPing, ThreadLocalRandom.current().nextInt(1, 30), TimeUnit.SECONDS);
        scheduledPing = PING_EXECUTOR.schedule(this::sendPingAndRepeat, (5 * 60) + ThreadLocalRandom.current().nextInt(2, 60), TimeUnit.SECONDS);
    }

    public void cancelPing() {
        if (scheduledPing != null) {
            scheduledPing.cancel(true);
            scheduledPing = null;
        }
    }


}
