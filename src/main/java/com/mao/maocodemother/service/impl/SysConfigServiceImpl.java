package com.mao.maocodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mao.maocodemother.exception.ErrorCode;
import com.mao.maocodemother.exception.ThrowUtils;
import com.mao.maocodemother.mapper.SysConfigMapper;
import com.mao.maocodemother.model.entity.SysConfig;
import com.mao.maocodemother.service.SysConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置 服务层实现。
 *
 * <p>配置读多写少（扣费单价 / 签到奖励 / 赠送额度等热路径每次请求都读），
 * 这里加了一层 60s TTL 的进程内缓存，热路径不再每次打 DB；
 * 后台改配置会立即失效缓存，直改数据库最多滞后 60s。
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    /**
     * 本地缓存 TTL（毫秒）
     */
    private static final long CACHE_TTL_MS = 60_000L;

    /**
     * configKey -> 缓存值。用 Optional 包一层：value 为空表示「已确认该配置不存在或为空」，
     * 同样缓存，避免不存在的 key 反复穿透到 DB。
     */
    private final Map<String, Optional<String>> localCache = new ConcurrentHashMap<>();

    /**
     * configKey -> 缓存写入时间戳（毫秒）
     */
    private final Map<String, Long> cacheTime = new ConcurrentHashMap<>();

    @Override
    public List<SysConfig> listAllConfig() {
        return this.list(QueryWrapper.create().orderBy("id", true));
    }

    @Override
    public void updateConfig(String configKey, String configValue) {
        ThrowUtils.throwIf(StrUtil.isBlank(configKey), ErrorCode.PARAMS_ERROR, "配置键不能为空");
        SysConfig existConfig = this.getOne(QueryWrapper.create().eq("configKey", configKey));
        ThrowUtils.throwIf(existConfig == null, ErrorCode.PARAMS_ERROR, "配置项不存在");

        SysConfig updateConfig = new SysConfig();
        updateConfig.setId(existConfig.getId());
        updateConfig.setConfigValue(configValue);
        updateConfig.setUpdateTime(LocalDateTime.now());
        boolean result = this.updateById(updateConfig);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新配置失败");
        // 更新成功立即失效缓存，保证马上能读到新值
        localCache.remove(configKey);
        cacheTime.remove(configKey);
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        if (StrUtil.isBlank(key)) {
            return defaultValue;
        }
        long now = System.currentTimeMillis();
        Long loadAt = cacheTime.get(key);
        if (loadAt == null || now - loadAt > CACHE_TTL_MS) {
            // 过期或未缓存：回源 DB 并回填（并发下可能重复回源，幂等且代价小，无需加锁）
            SysConfig sysConfig = this.getOne(QueryWrapper.create().eq("configKey", key));
            String value = (sysConfig == null || StrUtil.isBlank(sysConfig.getConfigValue()))
                    ? null : sysConfig.getConfigValue();
            localCache.put(key, Optional.ofNullable(value));
            cacheTime.put(key, now);
        }
        return localCache.get(key).orElse(defaultValue);
    }
}
