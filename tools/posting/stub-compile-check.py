#!/usr/bin/env python3
"""Builds throwaway Spring API shims to syntax-check new Java files without Maven/network.
Not a replacement for the Spring Boot project build.
"""
import subprocess, tempfile
from pathlib import Path
root=Path.cwd()
shim={
'org/springframework/core/env/Environment.java':'''package org.springframework.core.env; public interface Environment { boolean acceptsProfiles(Profiles profiles); }''',
'org/springframework/core/env/Profiles.java':'''package org.springframework.core.env; public interface Profiles { static Profiles of(String... profiles){return null;} }''',
'org/springframework/beans/factory/annotation/Value.java':'''package org.springframework.beans.factory.annotation; public @interface Value { String value(); }''',
'org/springframework/boot/autoconfigure/condition/ConditionalOnProperty.java':'''package org.springframework.boot.autoconfigure.condition; public @interface ConditionalOnProperty { String prefix() default ""; String[] name(); String havingValue() default ""; }''',
'org/springframework/stereotype/Service.java':'''package org.springframework.stereotype; public @interface Service {}''',
'org/springframework/transaction/annotation/Transactional.java':'''package org.springframework.transaction.annotation; public @interface Transactional { Propagation propagation() default Propagation.REQUIRED; boolean readOnly() default false; }''',
'org/springframework/transaction/annotation/Propagation.java':'''package org.springframework.transaction.annotation; public enum Propagation { REQUIRED, MANDATORY }''',
'org/springframework/transaction/support/TransactionSynchronizationManager.java':'''package org.springframework.transaction.support; public final class TransactionSynchronizationManager { public static boolean isActualTransactionActive(){return true;} public static boolean isCurrentTransactionReadOnly(){return false;} }''',
'org/springframework/jdbc/core/namedparam/MapSqlParameterSource.java':'''package org.springframework.jdbc.core.namedparam; public class MapSqlParameterSource { public MapSqlParameterSource addValue(String k,Object v){return this;} }''',
'org/springframework/jdbc/core/namedparam/NamedParameterJdbcTemplate.java':'''package org.springframework.jdbc.core.namedparam; import java.util.*; import org.springframework.jdbc.core.RowMapper; public class NamedParameterJdbcTemplate { public <T> T queryForObject(String sql,MapSqlParameterSource s, Class<T> cls){return null;} public List<Map<String,Object>> queryForList(String sql,MapSqlParameterSource s){return null;} public List<Map<String,Object>> queryForList(String sql,Map<String,?> s){return null;} public <T> List<T> query(String sql,Map<String,?> s, RowMapper<T> mapper){return null;} public int update(String sql, MapSqlParameterSource p){return 1;} }''',
'org/springframework/jdbc/core/RowMapper.java':'''package org.springframework.jdbc.core; public interface RowMapper<T> { T mapRow(java.sql.ResultSet r,int n) throws java.sql.SQLException; }''',
'org/springframework/beans/factory/ObjectProvider.java':'''package org.springframework.beans.factory; public interface ObjectProvider<T> { T getIfAvailable(); }''',
'org/springframework/web/bind/annotation/RestController.java':'''package org.springframework.web.bind.annotation; public @interface RestController {}''',
'org/springframework/web/bind/annotation/RequestMapping.java':'''package org.springframework.web.bind.annotation; public @interface RequestMapping { String[] value(); }''',
'org/springframework/web/bind/annotation/GetMapping.java':'''package org.springframework.web.bind.annotation; public @interface GetMapping { String[] value(); }''',
'org/springframework/web/bind/annotation/PathVariable.java':'''package org.springframework.web.bind.annotation; public @interface PathVariable {}''',
'org/springframework/web/bind/annotation/RequestHeader.java':'''package org.springframework.web.bind.annotation; public @interface RequestHeader { String name(); boolean required() default true; }''',
'org/springframework/http/ResponseEntity.java':'''package org.springframework.http; public class ResponseEntity<T> { public static BodyBuilder status(int x){return new BodyBuilder();} public static BodyBuilder notFound(){return new BodyBuilder();} public static BodyBuilder badRequest(){return new BodyBuilder();} public static <E> ResponseEntity<E> ok(E body){return new ResponseEntity<E>();} public static class BodyBuilder { public <E> ResponseEntity<E> build(){return new ResponseEntity<E>();} public <E> ResponseEntity<E> body(E value){return new ResponseEntity<E>();} } }'''
}
src=root/'backend/src/main/java/com/behsazan/corebanking/posting/cash'
with tempfile.TemporaryDirectory(prefix='posting-r1d5-java-') as folder:
    root_tmp=Path(folder)
    stubs=[]
    for filename,text in shim.items():
        f=root_tmp/filename;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text);stubs.append(str(f))
    sources=[str(f) for f in sorted(src.glob('*.java'))]
    result=subprocess.run(['javac','-encoding','UTF-8','-d',str(root_tmp/'classes'),*stubs,*sources],capture_output=True,text=True)
    print('JAVA_STUB_SYNTAX_CHECK='+('PASS' if result.returncode==0 else 'FAIL'))
    if result.returncode:
        print(result.stderr)
        raise SystemExit(result.returncode)
print('NOTE: Stub compilation checks the new classes only; Maven/Oracle integration remains untested.')
