package com.behsazan.corebanking.deposit.account.profit.application;

import com.behsazan.corebanking.deposit.account.error.DepositAccountLifecycleException;
import com.behsazan.corebanking.deposit.account.profit.oracle.DepositProfitRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DepositProfitContractProvisioningService {
    private static final Set<String> PROFIT_REQUIRED_FAMILIES=Set.of("SHORT_TERM_DEPOSIT","LONG_TERM_DEPOSIT");
    private final DepositProfitRepository repository;
    public DepositProfitContractProvisioningService(DepositProfitRepository repository){this.repository=repository;}

    public void ensureForActivation(long accountId,long openingRequestId,long productVersionId,String actor){
        if(repository.contract(accountId).isPresent())return;
        String family=repository.productFamily(productVersionId).map(DepositProfitContractProvisioningService::upper).orElse(null);
        boolean hasInstruction=repository.openingProfitInstructionExists(openingRequestId);
        if(!hasInstruction){
            if(PROFIT_REQUIRED_FAMILIES.contains(family))throw new DepositAccountLifecycleException(
                    "Activation حساب مدت‌دار بدون Profit Instruction معتبر مجاز نیست.",
                    Map.of("DEPOSIT_OPENING_PROFIT_INSTRUCTION.OPENING_REQUEST_ID",String.valueOf(openingRequestId),"PRODUCT_FAMILY_CODE",String.valueOf(family)));
            return;
        }
        int created=repository.provisionContractFromOpening(accountId,openingRequestId,productVersionId,actor);
        if(created!=1)throw new DepositAccountLifecycleException(
                "Profit Contract عملیاتی از Snapshot افتتاح ایجاد نشد.",
                Map.of("DEPOSIT_PROFIT_CONTRACT.ACCOUNT_ID",String.valueOf(accountId),"DEPOSIT_OPENING_PROFIT_INSTRUCTION.OPENING_REQUEST_ID",String.valueOf(openingRequestId)));
        repository.syncTermRate(accountId,actor);
    }
    private static String upper(String v){return v==null?null:v.trim().toUpperCase(Locale.ROOT);}
}
