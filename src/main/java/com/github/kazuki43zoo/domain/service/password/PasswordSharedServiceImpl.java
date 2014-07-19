package com.github.kazuki43zoo.domain.service.password;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.joda.time.DateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.terasoluna.gfw.common.date.DateFactory;
import org.terasoluna.gfw.common.exception.BusinessException;

import com.github.kazuki43zoo.core.message.Message;
import com.github.kazuki43zoo.domain.model.Account;
import com.github.kazuki43zoo.domain.repository.account.AccountRepository;

@Transactional
@Service
public class PasswordSharedServiceImpl implements PasswordSharedService {

    @Inject
    PasswordEncoder passwordEncoder;

    @Inject
    DateFactory dateFactory;

    @Inject
    AccountRepository accountRepository;

    @Override
    public void validatePassword(String rawPassword, Account account) {
        if (rawPassword.toLowerCase().contains(account.getAccountId().toLowerCase())) {
            throw new BusinessException(Message.PASSWORD_CONTAINS_ACCOUNT_ID.buildResultMessages());
        }
        if (account.isPastUsedPassword(rawPassword, passwordEncoder)) {
            throw new BusinessException(Message.PASSWORD_USED_PAST.buildResultMessages());
        }
    }

    @Override
    public void countUpPasswordFailureCount(String failedAccountId) {

        Account failedAccount = accountRepository.findOneByAccountId(failedAccountId);
        if (failedAccount == null) {
            return;
        }

        DateTime currentDateTime = dateFactory.newDateTime();
        failedAccount.countUpPasswordFailureCount(currentDateTime);
        accountRepository.savePasswordFailureCount(failedAccount);
    }

    @Override
    public void resetPasswordLock(Account account) {
        account.resetPasswordFailureCount();
        accountRepository.savePasswordFailureCount(account);
    }

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public String generateNewPassword() {
        return dateFactory.newDateTime().toString("yyyyMMdd");
    }

}
