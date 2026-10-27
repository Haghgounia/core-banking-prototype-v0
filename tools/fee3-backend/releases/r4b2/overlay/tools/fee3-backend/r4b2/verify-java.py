#!/usr/bin/env python3
"""Compile real R4B2 sources and JUnit test signatures against narrowly scoped fake framework APIs.
This is NOT an actual Spring/Maven build. The pure Java probe runs real implementation code.
"""
from __future__ import annotations
import subprocess,sys,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
SRC=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3'
STUBS={
'org/springframework/stereotype/Service.java': 'package org.springframework.stereotype; public @interface Service {}',
'org/springframework/transaction/annotation/Transactional.java': 'package org.springframework.transaction.annotation;public @interface Transactional { boolean readOnly() default false; }',
'org/springframework/web/bind/annotation/RestController.java': 'package org.springframework.web.bind.annotation;public @interface RestController {}',
'org/springframework/web/bind/annotation/RequestMapping.java': 'package org.springframework.web.bind.annotation;public @interface RequestMapping { String value() default ""; }',
'org/springframework/web/bind/annotation/PostMapping.java': 'package org.springframework.web.bind.annotation;public @interface PostMapping { String value() default ""; }',
'org/springframework/web/bind/annotation/RequestBody.java': 'package org.springframework.web.bind.annotation;public @interface RequestBody {}',
'org/springframework/web/bind/annotation/ExceptionHandler.java': 'package org.springframework.web.bind.annotation;public @interface ExceptionHandler { Class<?>[] value() default {}; }',
'org/springframework/web/bind/annotation/RestControllerAdvice.java': 'package org.springframework.web.bind.annotation;public @interface RestControllerAdvice { Class<?>[] assignableTypes() default {}; }',
'org/springframework/http/HttpStatus.java': 'package org.springframework.http;public enum HttpStatus { UNPROCESSABLE_ENTITY }',
'org/springframework/http/ProblemDetail.java': '''package org.springframework.http;import java.net.URI;
public class ProblemDetail {public static ProblemDetail forStatusAndDetail(HttpStatus s,String d){return new ProblemDetail();}
public void setType(URI u){}public void setProperty(String k,Object v){} }''',
'org/junit/jupiter/api/Test.java': 'package org.junit.jupiter.api;public @interface Test {}',
'org/assertj/core/api/Assertions.java': '''package org.assertj.core.api;
public class Assertions {public static Check assertThat(Object any){return new Check();}
public static class Check {public Check isEqualByComparingTo(String s){return this;}
public Check isNull(){return this;} public Check isFalse(){return this;}
public Check isEqualTo(Object o){return this;} public Check hasSize(int n){return this;}} }''',
'com/behsazan/corebanking/fee3/application/Fee3Problem.java': '''package com.behsazan.corebanking.fee3.application;
public class Fee3Problem extends RuntimeException{
private final String code; public Fee3Problem(org.springframework.http.HttpStatus s,String c,String m){super(m);code=c;}
public org.springframework.http.HttpStatus status(){return org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;}
public String errorCode(){return code;}}''',
'com/behsazan/corebanking/fee3/oracle/Fee3Repository.java': '''package com.behsazan.corebanking.fee3.oracle;
public class Fee3Repository {
public java.time.LocalDate bankDate(){return java.time.LocalDate.now();}
public java.util.Optional<com.behsazan.corebanking.fee3.domain.Fee3Models.Operation> activeOperation(String m,String o){return java.util.Optional.empty();}
public String sourceStatus(long id){return null;}}''',
'com/behsazan/corebanking/fee3/r4/Fee3R4ReferenceRepository.java': '''package com.behsazan.corebanking.fee3.r4;
import com.behsazan.corebanking.fee3.domain.Fee3Models.DraftRequest;
public class Fee3R4ReferenceRepository {
public Fee3R4CalculationContract.VersionReference resolve(DraftRequest draft){return null;}}'''
}

def run():
    with tempfile.TemporaryDirectory(prefix='fee3-r4b2-java-') as dirname:
        p=Path(dirname);files=[]
        for name,source in STUBS.items():
            f=p/'stubs'/name;f.parent.mkdir(parents=True,exist_ok=True)
            f.write_text(source,encoding='utf8');files.append(f)
        files.extend([SRC/'domain/Fee3Models.java',SRC/'r3/Fee3PreviewEngine.java',
                      SRC/'r3/Fee3SubmissionPolicy.java',SRC/'r4/Fee3R4CalculationContract.java',
                      SRC/'application/Fee3DraftPolicy.java'])
        files.extend(sorted((SRC/'r4b2').glob('*.java')))
        test=ROOT/'backend/src/test/java/com/behsazan/corebanking/fee3/r4b2/Fee3R4B2GuaranteeContractTest.java'
        files.append(test)
        cmd=['javac','-encoding','UTF-8','-d',str(p/'classes'),*map(str,files)]
        result=subprocess.run(cmd,capture_output=True,text=True,encoding='utf8',errors='replace',timeout=70)
        if result.returncode:
            print('FEE3_R4B2_SIGNATURE_COMPILE_FAIL',result.stderr[-4500:],flush=True);return 2
        print('FEE3_R4B2_SIGNATURE_COMPILE_PASS=4 production classes + JUnit test (framework stubs; NOT Maven)',flush=True)
        return 0

if __name__=='__main__':sys.exit(run())
