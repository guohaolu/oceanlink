package org.example.manager;

import org.example.pojo.dto.RuleNodeDTO;
import org.example.pojo.entity.ExpressionRuleEntity;
import java.util.List;

public interface IRuleManager {
    RuleNodeDTO getRuleTree(Long ruleId);
    void saveRuleTree(ExpressionRuleEntity rule, RuleNodeDTO treeRoot);
}
