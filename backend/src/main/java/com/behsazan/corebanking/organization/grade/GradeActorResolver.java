package com.behsazan.corebanking.organization.grade;

import org.springframework.stereotype.Component;

/** Transparent attribution label for the prototype, not an authenticated user. */
@Component
public class GradeActorResolver {
    public String actor() { return "PROTOTYPE"; }
}
