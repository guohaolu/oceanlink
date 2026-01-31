package org.example.service;

import org.example.pojo.dto.RuleNodeDTO;
import org.example.support.RuleContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * @author guohao.lu
 */
@Component
public class RuleExecutor {

    public boolean checkHit(RuleNodeDTO node, RuleContext context) {
        if ("LOGICAL".equals(node.getNodeType())) {
            return executeLogical(node, context);
        } else {
            return executePredicate(node, context);
        }
    }

    private boolean executeLogical(RuleNodeDTO node, RuleContext context) {
        List<RuleNodeDTO> children = node.getChildren();
        return switch (node.getOperator()) {
            case "AND" -> children.stream().allMatch(child -> checkHit(child, context));
            case "OR" -> children.stream().anyMatch(child -> checkHit(child, context));
            case "NOT" -> !checkHit(children.get(0), context);
            default -> false;
        };
    }

    private boolean executePredicate(RuleNodeDTO node, RuleContext context) {
        Object actualValue = context.getValue(node.getFieldName());
        Object targetValue = context.resolveValue(node.getValueType(), node.getValueContent());

        return switch (node.getOperator()) {
            case "EQ" -> Objects.equals(actualValue, targetValue);
            case "LIKE" -> String.valueOf(actualValue).contains(String.valueOf(targetValue));
            case "STARTS_WITH" -> String.valueOf(actualValue).startsWith(String.valueOf(targetValue));
            // ... 其他操作符
            default -> false;
        };
    }
}
