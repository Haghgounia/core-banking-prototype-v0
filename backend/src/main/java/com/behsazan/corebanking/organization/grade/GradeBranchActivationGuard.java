package com.behsazan.corebanking.organization.grade;

import org.springframework.stereotype.Component;
import java.util.Map;

/** No activation restrictions in the prototype; existing ORG forms must keep working. */
@Component
public class GradeBranchActivationGuard {
    public void beforeCreate(Map<String,Object> values) { }
    public void beforeUpdate(long id, Map<String,Object> previous, Map<String,Object> values) { }
}
