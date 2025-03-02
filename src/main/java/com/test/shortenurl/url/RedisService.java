package com.test.shortenurl.url;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.shortenurl.domain.url.Url;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Service
public class RedisService {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveSingleData(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
        redisTemplate.expire(key, 10, TimeUnit.MINUTES);
    }

    public void saveListData(String key, List<Url> value) throws JsonProcessingException {
        try {
            String urls = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, urls, 10, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            throw e;
        }
    }

    public String getSingleData(String key) {
        String value = (String) redisTemplate.opsForValue().get(key);

        return value;
    }

    public List<Url> getListData(String key) {
        try {
            String json = (String) redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<List<Url>>() {});
            }
            else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching list data from Redis", e);
        }
    }

    public boolean deleteSingleData(String key) {
        boolean result = redisTemplate.delete(key);
        return result;
    }
}
