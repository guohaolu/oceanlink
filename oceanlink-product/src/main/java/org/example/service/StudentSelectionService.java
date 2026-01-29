package org.example.service;

import org.example.manager.IRuleManager;
import org.example.pojo.dto.RuleNodeDTO;
import org.example.pojo.dto.StudentDTO;
import org.example.support.RuleContext;
import org.springframework.stereotype.Service;

import java.util.Map;

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
    @Resource
    private RuleMappingService ruleMappingService;

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

    public void processStudent2(StudentDTO student) {
        // 1. 构建通用上下文
        RuleContext context = new RuleContext();
        context.put("score", student.getScore());
        context.put("major", student.getMajor());
        context.put("grade", student.getGrade());

        // 2. 调用规则映射服务获取结果
        // 场景：根据成绩和专业映射出 “学生标签” 和 “推荐课程列表”
        Map<String, Object> result = ruleMappingService.match(
            "TENANT_001", 
            "STUDENT_TAGGING", 
            context
        );

        if (!result.isEmpty()) {
            // 3. 解析结果
            String cat1 = (String) result.get("category1");
            List<String> tags = (List<String>) result.get("tags");
            
            System.out.println("匹配成功！一级分类：" + cat1 + "，标签：" + tags);
        } else {
            System.out.println("未命中任何匹配规则");
        }
    }
}
