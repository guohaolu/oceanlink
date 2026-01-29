package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.manager.IRuleManager;
import org.example.mapper.ExpressionRuleMapper;
import org.example.pojo.dto.RuleNodeDTO;
import org.example.pojo.entity.ExpressionRuleEntity;
import org.example.support.RuleContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class RuleMappingService {
    @Resource
    private ExpressionRuleMapper ruleMapper;
    @Resource
    private IRuleManager ruleManager;
    @Resource
    private RuleExecutor ruleExecutor;

    /**
     * 匹配规则并获取结果
     * 
     * @param tenantId   租户ID
     * @param reportType 业务类型
     * @param context    上下文数据
     * @return 命中的规则结果配置，未命中返回空 Map
     */
    public Map<String, Object> match(String tenantId, String reportType, RuleContext context) {
        // 1. 按优先级加载该类型下所有启用的规则
        List<ExpressionRuleEntity> rules = ruleMapper.selectList(
            new LambdaQueryWrapper<ExpressionRuleEntity>()
                .eq(ExpressionRuleEntity::getTenantId, tenantId)
                .eq(ExpressionRuleEntity::getReportType, reportType)
                .eq(ExpressionRuleEntity::getStatus, 1)
                .orderByDesc(ExpressionRuleEntity::getPriority)
        );

        for (ExpressionRuleEntity rule : rules) {
            // 2. 加载规则树（建议此处增加缓存）
            RuleNodeDTO ruleTree = ruleManager.getRuleTree(rule.getId());
            
            // 3. 执行判定
            if (ruleExecutor.checkHit(ruleTree, context)) {
                return rule.getResultConfig();
            }
        }
        return Collections.emptyMap();
    }
}
