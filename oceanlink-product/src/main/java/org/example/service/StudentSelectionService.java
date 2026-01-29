package org.example.service;

import org.example.manager.IRuleManager;
import org.example.pojo.dto.RuleNodeDTO;
import org.example.pojo.dto.StudentDTO;
import org.example.support.RuleContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 学生选课服务
 *
 * @author guohao.lu
 */
@Service
public class StudentSelectionService {
    @Resource
    private IRuleManager ruleManager;
    @Resource
    private RuleExecutor ruleExecutor;

    public void processStudent(Long ruleId, StudentDTO student) {
        // 1. 加载规则树
        RuleNodeDTO ruleTree = ruleManager.getRuleTree(ruleId);

        // 2. 构建上下文 (Context)
        RuleContext context = new RuleContext();
        context.put("site", "US");
        context.put("title", "okkkkk");
        // 假设规则中有引用量 ${age}
        context.put("age", student.getAge());

        // 3. 执行检测
        boolean isMatch = ruleExecutor.checkHit(ruleTree, context);

        if (isMatch) {
            System.out.println("该数据命中规则！");
        }
    }
}
