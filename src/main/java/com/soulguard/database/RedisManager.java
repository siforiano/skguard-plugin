package com.soulguard.database;

import com.soulguard.SoulGuard;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;

public class RedisManager {

    private final SoulGuard plugin;
    private JedisPool jedisPool;
    private boolean enabled;

    public RedisManager(SoulGuard plugin) {
        this.plugin = plugin;
    }

    public void init() {
        this.enabled = plugin.getConfig().getBoolean("database.redis.enabled", false);
        if (!enabled) return;

        String host = plugin.getConfig().getString("database.redis.host", "localhost");
        int port = plugin.getConfig().getInt("database.redis.port", 6379);
        String password = plugin.getConfig().getString("database.redis.password", "");
        int timeout = plugin.getConfig().getInt("database.redis.timeout", 2000);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(2);

        if (password == null || password.isEmpty()) {
            this.jedisPool = new JedisPool(poolConfig, host, port, timeout);
        } else {
            this.jedisPool = new JedisPool(poolConfig, host, port, timeout, password);
        }

        plugin.getLogManager().logInfo("Connected to Redis for cross-server synchronization.");
    }

    public void publish(String channel, String message) {
        if (!enabled || jedisPool == null) return;
        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(channel, message);
            } catch (Exception e) {
                plugin.getLogManager().logError("Redis Publish Error: " + e.getMessage());
            }
        });
    }

    public void subscribe(JedisPubSub pubSub, String... channels) {
        if (!enabled || jedisPool == null) return;
        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(pubSub, channels);
            } catch (Exception e) {
                plugin.getLogManager().logError("Redis Subscribe Error: " + e.getMessage());
            }
        });
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void shutdown() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}
