package com.behsazan.corebanking.deposit.account.operations.application;

import com.behsazan.corebanking.deposit.account.balance.application.DepositBalanceService;
import com.behsazan.corebanking.deposit.account.error.DepositAccountNotFoundException;
import com.behsazan.corebanking.deposit.account.operations.domain.DepositAccountOperationsModels.*;
import com.behsazan.corebanking.deposit.account.operations.oracle.DepositAccountOperationsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class DepositAccountOperationsService {
    private static final Set<String> STATUSES=Set.of("PENDING_ACTIVATION","ACTIVE","SUSPENDED","DORMANT","CLOSED");
    private static final Set<String> FAMILIES=Set.of("QARD_SAVINGS","CURRENT_ACCOUNT","SHORT_TERM_DEPOSIT","LONG_TERM_DEPOSIT");
    private final DepositAccountOperationsRepository repository;
    private final DepositBalanceService balanceService;
    public DepositAccountOperationsService(DepositAccountOperationsRepository repository,DepositBalanceService balanceService){this.repository=repository;this.balanceService=balanceService;}
    @Transactional(readOnly=true) public AccountSearchResponse search(String accountNo,String status,Long openingRequestId,Long partyId,String productFamilyCode,int offset,int limit){String s=normalize(status),f=normalize(productFamilyCode);if(s!=null&&!STATUSES.contains(s))throw new IllegalArgumentException("ACCOUNT_STATUS_CODE نامعتبر است: "+s);if(f!=null&&!FAMILIES.contains(f))throw new IllegalArgumentException("PRODUCT_FAMILY_CODE خارج از چهار خانواده سپرده است: "+f);int o=Math.max(0,offset),l=Math.min(Math.max(limit,1),200);var items=repository.search(accountNo,s,openingRequestId,partyId,f,o,l);return new AccountSearchResponse(items,repository.count(accountNo,s,openingRequestId,partyId,f),o,l);}
    @Transactional(readOnly=true) public AccountDetails get(long accountId){if(accountId<=0)throw new IllegalArgumentException("accountId باید مثبت باشد.");AccountSummary a=repository.find(accountId).orElseThrow(()->new DepositAccountNotFoundException("حساب سپرده با شناسه "+accountId+" یافت نشد."));var financial=balanceService.get(accountId);return new AccountDetails(a,repository.parties(accountId),repository.contacts(accountId),repository.servicingHistory(accountId),repository.events(accountId),repository.statusHistory(accountId),repository.holds(accountId),repository.holdHistory(accountId),financial.balance(),financial.subledgerEntries(),financial.reservations());}
    private static String normalize(String v){return v==null||v.isBlank()?null:v.trim().toUpperCase(Locale.ROOT);}
}
