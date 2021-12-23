package org.mineskin.hiatus;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class HiatusConfig {

    private static final Gson GSON = new Gson();
    private static final TypeToken<Map<UUID, Account>> TYPE_TOKEN = new TypeToken<>() {};

    private final Map<UUID, Account> accountsByUuid = new HashMap<>();

    private HiatusConfig(Map<UUID, Account> accountsByUuid) {
        if (accountsByUuid != null)
            this.accountsByUuid.putAll(accountsByUuid);
    }

    public Map<UUID, String> getAccounts() {
        Map<UUID, String> out = new HashMap<>();
        for (Map.Entry<UUID, Account> entry : accountsByUuid.entrySet()) {
            out.put(entry.getKey(), entry.getValue().getEmail());
        }
        return out;
    }

    public Account getAccount(UUID uuid) {
        return this.accountsByUuid.get(uuid);
    }

    public void putAccount(Account account) {
        this.accountsByUuid.put(account.getUuid(), account);
    }

    public void removeAccount(UUID uuid) {
        this.accountsByUuid.remove(uuid);
    }

    @Override
    public String toString() {
        return "HiatusConfig{" +
                "accountsByUuid=" + accountsByUuid +
                '}';
    }

    public static CompletableFuture<HiatusConfig> load(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                MineSkinHiatus.LOGGER.log(Level.WARNING, "Failed to create config file", e);
            }
        }
        return CompletableFuture.supplyAsync(() -> {
            MineSkinHiatus.LOGGER.info("Loading config");
            Map<UUID, Account> map = null;
            try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                map = GSON.fromJson(reader, TYPE_TOKEN.getType());
            } catch (IOException e) {
                MineSkinHiatus.LOGGER.log(Level.WARNING, "Failed to parse config", e);
                e.printStackTrace();
            }
            return new HiatusConfig(map);
        });
    }

    public static CompletableFuture<Void> save(File file, HiatusConfig config) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                MineSkinHiatus.LOGGER.log(Level.WARNING, "Failed to create config file", e);
            }
        }
        return CompletableFuture.runAsync(() -> {
            MineSkinHiatus.LOGGER.info("Saving config");
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                GSON.toJson(config.accountsByUuid, writer);
            } catch (IOException e) {
                MineSkinHiatus.LOGGER.log(Level.WARNING, "Failed to save config", e);
                e.printStackTrace();
            }
        });
    }

}
