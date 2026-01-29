package org.example.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.manager.IRuleManager;
import org.example.mapper.ExpressionRuleMapper;
import org.example.mapper.ExpressionRuleNodeMapper;
import org.example.pojo.dto.RuleNodeDTO;
import org.example.pojo.entity.ExpressionRuleEntity;
import org.example.pojo.entity.ExpressionRuleNodeEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RuleManagerImpl implements IRuleManager {

    @Resource
    private ExpressionRuleMapper ruleMapper;
    @Resource
    private ExpressionRuleNodeMapper nodeMapper;

    @Override
    public RuleNodeDTO getRuleTree(Long ruleId) {
        List<ExpressionRuleNodeEntity> nodes = nodeMapper.selectList(
            new LambdaQueryWrapper<ExpressionRuleNodeEntity>()
                .eq(ExpressionRuleNodeEntity::getRuleId, ruleId)
                .orderByAsc(ExpressionRuleNodeEntity::getSortOrder)
        );
        
        if (nodes.isEmpty()) return null;

        // 构建 ID 到 DTO 的映射
        Map<Long, RuleNodeDTO> dtoMap = nodes.stream().collect(Collectors.toMap(
            ExpressionRuleNodeEntity::getId,
            node -> {
                RuleNodeDTO dto = new RuleNodeDTO();
                BeanUtils.copyProperties(node, dto);
                return dto;
            }
        ));

        RuleNodeDTO root = null;
        for (ExpressionRuleNodeEntity node : nodes) {
            RuleNodeDTO current = dtoMap.get(node.getId());
            if (node.getParentId() == null) {
                root = current;
            } else {
                RuleNodeDTO parent = dtoMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(current);
                }
            }
        }
        return root;
    }

    @Override
    @Transactional
    public void saveRuleTree(ExpressionRuleEntity rule, RuleNodeDTO treeRoot) {
        // 1. 保存/更新规则头
        if (rule.getId() == null) {
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
            // 2. 删除旧节点（简单起见，全量覆盖）
            nodeMapper.delete(new LambdaQueryWrapper<ExpressionRuleNodeEntity>()
                .eq(ExpressionRuleNodeEntity::getRuleId, rule.getId()));
        }

        // 3. 递归保存节点
        saveNodesRecursive(rule.getId(), null, treeRoot, 0);
    }

    private void saveNodesRecursive(Long ruleId, Long parentId, RuleNodeDTO nodeDto, int sort) {
        ExpressionRuleNodeEntity entity = new ExpressionRuleNodeEntity();
        BeanUtils.copyProperties(nodeDto, entity);
        entity.setRuleId(ruleId);
        entity.setParentId(parentId);
        entity.setSortOrder(sort);
        nodeMapper.insert(entity);

        if (nodeDto.getChildren() != null) {
            for (int i = 0; i < nodeDto.getChildren().size(); i++) {
                saveNodesRecursive(ruleId, entity.getId(), nodeDto.getChildren().get(i), i);
            }
        }
    }
}
