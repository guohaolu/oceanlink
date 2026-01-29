package org.example.pojo.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class RuleNodeDTO {
    private Long id;
    private String nodeType;
    private String operator;
    private String fieldName;
    private String valueType;
    private String valueContent;
    private List<RuleNodeDTO> children = new ArrayList<>();
}
