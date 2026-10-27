#!/usr/bin/env python3
"""Signature compile of R4C1 Spring/JDBC/JUnit with isolated stubs; NOT a Maven build."""
import subprocess,tempfile,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
SRC=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3'
STUB={
'org/springframework/stereotype/Service.java':'package org.springframework.stereotype;public @interface Service {}',
'org/springframework/stereotype/Repository.java':'package org.springframework.stereotype;public @interface Repository {}',
'org/springframework/beans/factory/annotation/Value.java':'package org.springframework.beans.factory.annotation;public @interface Value {String value();}',
'org/springframework/transaction/annotation/Transactional.java':'package org.springframework.transaction.annotation;public @interface Transactional {boolean readOnly() default false;}',
'org/springframework/web/bind/annotation/RestController.java':'package org.springframework.web.bind.annotation;public @interface RestController {}',
'org/springframework/web/bind/annotation/RequestMapping.java':'package org.springframework.web.bind.annotation;public @interface RequestMapping {String value() default "";}',
'org/springframework/web/bind/annotation/PostMapping.java':'package org.springframework.web.bind.annotation;public @interface PostMapping {String value() default "";}',
'org/springframework/web/bind/annotation/RequestBody.java':'package org.springframework.web.bind.annotation;public @interface RequestBody {}',
'org/springframework/web/bind/annotation/ExceptionHandler.java':'package org.springframework.web.bind.annotation;public @interface ExceptionHandler {Class<?>[] value() default {};}',
'org/springframework/web/bind/annotation/RestControllerAdvice.java':'package org.springframework.web.bind.annotation;public @interface RestControllerAdvice {Class<?>[] assignableTypes() default {};}',
'org/springframework/http/HttpStatus.java':'package org.springframework.http;public enum HttpStatus { UNPROCESSABLE_ENTITY }',
'org/springframework/http/ProblemDetail.java':'''package org.springframework.http;import java.net.URI;
public class ProblemDetail {public static ProblemDetail forStatusAndDetail(HttpStatus s,String d){return new ProblemDetail();}
public void setType(URI u){}public void setProperty(String key,Object v){} }''',
'org/springframework/dao/DataAccessException.java':'package org.springframework.dao;public class DataAccessException extends RuntimeException {}',
'org/springframework/jdbc/core/simple/JdbcClient.java':'''package org.springframework.jdbc.core.simple;
import java.sql.ResultSet;import java.sql.SQLException;import java.util.List;
public class JdbcClient {public Spec sql(String sql){return new Spec();}
public static interface Mapper<T> {T mapRow(ResultSet rs,int index) throws SQLException;}
public static class Spec{public Spec param(String n,Object v){return this;}
public <T> Query<T> query(Mapper<T> mapper){return new Query<>();}}
public static class Query<T>{public List<T> list(){return List.of();}}
}''',
'com/behsazan/corebanking/fee3/application/Fee3Problem.java':'''package com.behsazan.corebanking.fee3.application;
public class Fee3Problem extends RuntimeException{
public Fee3Problem(org.springframework.http.HttpStatus s,String c,String m){} 
public String errorCode(){return "INVALID";}}''',
'com/behsazan/corebanking/fee3/oracle/Fee3Repository.java':'''package com.behsazan.corebanking.fee3.oracle;
import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
public class Fee3Repository {public java.time.LocalDate bankDate(){return java.time.LocalDate.now();}
public java.util.Optional<Operation> activeOperation(String module,String code){return java.util.Optional.empty();}
public java.util.List<Formula> formulas(){return java.util.List.of();}
public String sourceStatus(long source){return null;}}
''',
'com/behsazan/corebanking/fee3/r4/Fee3R4ReferenceRepository.java':'''package com.behsazan.corebanking.fee3.r4;
import com.behsazan.corebanking.fee3.domain.Fee3Models.DraftRequest;
public class Fee3R4ReferenceRepository {
public Fee3R4CalculationContract.VersionReference resolve(DraftRequest d){return null;}}
''',
'org/junit/jupiter/api/Test.java':'package org.junit.jupiter.api;public @interface Test {}',
'org/assertj/core/api/Assertions.java':'''package org.assertj.core.api;
public class Assertions {public static Check assertThat(Object o){return new Check();}
public static class Check {public Check isEqualByComparingTo(String a){return this;}
public Check isEqualTo(Object o){return this;}}}'''
}
def main():
    with tempfile.TemporaryDirectory(prefix='fee3-r4c1-signature-') as temporary:
        temp=Path(temporary);files=[]
        for name,source in STUB.items():
            path=temp/'stubs'/name;path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(source,encoding='utf8');files.append(path)
        files += [SRC/'domain/Fee3Models.java',SRC/'r3/Fee3PreviewEngine.java',
                  SRC/'r3/Fee3SubmissionPolicy.java',SRC/'r4/Fee3R4CalculationContract.java',
                  SRC/'application/Fee3DraftPolicy.java']
        files += sorted((SRC/'r4c1').glob('*.java'))
        files += [ROOT/'backend/src/test/java/com/behsazan/corebanking/fee3/r4c1/Fee3R4C1LoanProfitContractTest.java']
        output=subprocess.run(['javac','-encoding','UTF-8','-d',str(temp/'classes'),*map(str,files)],
            capture_output=True,text=True,encoding='utf8',errors='replace',timeout=85)
        if output.returncode:
            print('FEE3_R4C1_SIGNATURE_COMPILE_FAIL\n'+output.stderr[-4500:]);return 2
        print('FEE3_R4C1_SIGNATURE_COMPILE_PASS=5 Java sources + JUnit class (framework stubs)');return 0
if __name__=='__main__':sys.exit(main())
