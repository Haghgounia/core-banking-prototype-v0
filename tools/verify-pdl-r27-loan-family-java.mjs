/** Behavioural Java test of production loan group classification, with a DTO stub. */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const source=path.join(root,'backend/src/main/java/com/behsazan/corebanking/productbuilder/application');
const temp=fs.mkdtempSync(path.join(os.tmpdir(),'pdl-r27-family-java-'));
function run(bin,args){const result=spawnSync(bin,args,{encoding:'utf8'});
 if(result.error)throw result.error;
 if(result.status!==0)throw Error(`${bin} exited ${result.status}\n${result.stdout}\n${result.stderr}`);
 if(result.stdout)process.stdout.write(result.stdout);
}
try{
 const dto=path.join(temp,'com/behsazan/corebanking/productbuilder/domain/ProductBuilderModels.java');
 fs.mkdirSync(path.dirname(dto),{recursive:true});
 fs.writeFileSync(dto,'package com.behsazan.corebanking.productbuilder.domain; public class ProductBuilderModels {public record SelectOption(Object value,String code,String label) {}}\n');
 const test=path.join(temp,'CheckLoanFamilyR27.java');
 fs.writeFileSync(test,`import java.util.*;
import com.behsazan.corebanking.productbuilder.application.*;
class CheckLoanFamilyR27 {
 static int n=0;
 static void ck(boolean test, String message){if(!test)throw new AssertionError(message);System.out.println("PASS | "+message);n++;}
 static Map<String,Object> row(int group, int obligation){Map<String,Object> x=new HashMap<>();x.put("LOAN_GROUP", group);x.put("OBLIGATION_TYPE", obligation);return x;}
 public static void main(String[]args){
 ck(LoanTypeClassification.groupForProductFamily("QARD_HASAN_LOAN")==1,"Qard Hasan family -> group 1");
 ck(LoanTypeClassification.groupForProductFamily("EXCHANGE_LOAN")==2,"Exchange family -> group 2");
 ck(LoanTypeClassification.groupForProductFamily("PARTICIPATORY_LOAN")==3,"Participatory family -> group 3");
 ck(LoanTypeClassification.groupForProductFamily("COMMITMENT_LOAN")==4,"Commitment family -> group 4");
 ck(LoanTypeClassification.groupForProductFamily("RETAIL_LOAN")==0,"Legacy retail is not a selectable loan group");
 ck(LoanTypeClassification.isLegacyRetailFamily("RETAIL_LOAN"),"Legacy retail remains recognizable");
 Map<String,Object> qard=row(1,0); LoanTypeClassification.validateForSave(qard,null);
 ck(Integer.valueOf(0).equals(qard.get("INTEREST_METHOD")),"Qard contract has system-only pending interest method");
 Map<String,Object> civil=row(3,0); LoanTypeClassification.validateForSave(civil,null);
 ck(Integer.valueOf(0).equals(civil.get("INTEREST_METHOD")),"Civil participation belongs to group 3, not independent family");
 boolean invalid=false;try{LoanTypeClassification.validateForSave(row(5,0),null);}catch(ProductBuilderValidationException x){invalid=true;}
 ck(invalid,"Unknown loan group is rejected");
 System.out.println("PDL_R27_LOAN_FAMILY_JAVA_PASS="+n+" FAIL=0");
 }
}\n`);
 run('javac',['-encoding','UTF-8','-d',temp,dto,path.join(source,'ProductBuilderValidationException.java'),path.join(source,'LoanTypeClassification.java'),test]);
 run('java',['-cp',temp,'CheckLoanFamilyR27']);
}finally{fs.rmSync(temp,{recursive:true,force:true});}
