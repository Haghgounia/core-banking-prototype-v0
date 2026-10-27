#!/usr/bin/env python3
"""Compile R4-A Java signatures against narrow STUBS. This is NOT a Maven build."""
from pathlib import Path
import subprocess,sys,tempfile
ROOT=Path(__file__).resolve().parents[3]
B=ROOT/'backend/src/main/java/com/behsazan/corebanking/fee3'
STUBS={
'org/springframework/stereotype/Service.java':'''package org.springframework.stereotype;public @interface Service {}''',
'org/springframework/stereotype/Repository.java':'''package org.springframework.stereotype;public @interface Repository {}''',
'org/springframework/transaction/annotation/Transactional.java':'''package org.springframework.transaction.annotation;public @interface Transactional { boolean readOnly() default false; }''',
'org/springframework/web/bind/annotation/RestController.java':'''package org.springframework.web.bind.annotation;public @interface RestController {}''',
'org/springframework/web/bind/annotation/RequestMapping.java':'''package org.springframework.web.bind.annotation;public @interface RequestMapping { String value() default ""; }''',
'org/springframework/web/bind/annotation/PostMapping.java':'''package org.springframework.web.bind.annotation;public @interface PostMapping { String value() default ""; }''',
'org/springframework/web/bind/annotation/RequestBody.java':'''package org.springframework.web.bind.annotation;public @interface RequestBody {}''',
'org/springframework/web/bind/annotation/ExceptionHandler.java':'''package org.springframework.web.bind.annotation;public @interface ExceptionHandler { Class<?>[] value() default {}; }''',
'org/springframework/web/bind/annotation/RestControllerAdvice.java':'''package org.springframework.web.bind.annotation;public @interface RestControllerAdvice { Class<?>[] assignableTypes() default {}; }''',
'org/springframework/http/HttpStatus.java':'''package org.springframework.http;public enum HttpStatus { UNPROCESSABLE_ENTITY }''',
'org/springframework/http/ProblemDetail.java':'''package org.springframework.http;import java.net.URI;public class ProblemDetail { public static ProblemDetail forStatusAndDetail(HttpStatus s,String d){return new ProblemDetail();}public void setType(URI u){}public void setProperty(String k,Object v){} }''',
'org/springframework/jdbc/core/simple/JdbcClient.java':'''package org.springframework.jdbc.core.simple;
import java.sql.*;import java.util.*;
public class JdbcClient {
 public Statement sql(String q){return new Statement();}
 public static class Statement {
  public Statement param(String k,Object v){return this;}
  public <T> Mapping<T> query(Mapper<T> m){return new Mapping<T>();}
 }
 public interface Mapper<T>{T map(ResultSet rs,int row)throws SQLException;}
 public static class Mapping<T>{public List<T> list(){return List.of();}}
}''',
'tools/jackson/databind/JsonNode.java':'''package tools.jackson.databind;public class JsonNode {public JsonNode get(String s){return null;}public JsonNode path(String s){return null;}public boolean isObject(){return false;}public int asInt(){return 0;}public String asText(){return "";}}''',
'tools/jackson/databind/node/ObjectNode.java':'''package tools.jackson.databind.node;public class ObjectNode extends tools.jackson.databind.JsonNode {public JsonNode remove(String s){return null;}}'''.replace('public JsonNode remove','public tools.jackson.databind.JsonNode remove'),
'tools/jackson/databind/json/JsonMapper.java':'''package tools.jackson.databind.json;import tools.jackson.databind.JsonNode;public class JsonMapper { public JsonNode readTree(String s){return null;}public String writeValueAsString(Object x){return "";} }''',
'com/behsazan/corebanking/fee3/application/Fee3Problem.java':'''package com.behsazan.corebanking.fee3.application;public class Fee3Problem extends RuntimeException{public org.springframework.http.HttpStatus status(){return org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;}public String errorCode(){return "";}}''',
'com/behsazan/corebanking/fee3/oracle/Fee3Repository.java':'''package com.behsazan.corebanking.fee3.oracle;public class Fee3Repository{public String schema(){return "FEE3";}public java.time.LocalDate bankDate(){return java.time.LocalDate.now();}}''',
'com/behsazan/corebanking/fee3/r3/Fee3R3Service.java':'''package com.behsazan.corebanking.fee3.r3;public class Fee3R3Service{public Fee3PreviewEngine.PreviewResult preview(Fee3PreviewEngine.PreviewRequest r){return Fee3PreviewEngine.calculate(r);}}'''
}

def main():
 with tempfile.TemporaryDirectory(prefix='fee3-r4a-signatures-') as d:
  td=Path(d);files=[]
  for name,content in STUBS.items():
   p=td/'src'/name;p.parent.mkdir(parents=True,exist_ok=True)
   p.write_text(content,encoding='utf-8');files.append(p)
  files.extend([B/'domain/Fee3Models.java',B/'r3/Fee3PreviewEngine.java'])
  files.extend(sorted((B/'r4').glob('*.java')))
  p=subprocess.run(['javac','-encoding','UTF-8','-d',str(td/'classes')]+list(map(str,files)),
                   text=True,capture_output=True,encoding='utf-8',errors='replace',timeout=50)
  if p.returncode:
   print('FEE3_R4A_JAVA_SIGNATURES_FAIL',p.stdout,p.stderr[-4000:]);return 2
  print('FEE3_R4A_JAVA_SIGNATURES_PASS=5 (stubbed Spring/JDBC/Jackson; NOT Maven)')
  return 0

if __name__=='__main__':sys.exit(main())
