package com.kazuki43zoo.domain.service.password;

import com.kazuki43zoo.core.message.Message;
import com.kazuki43zoo.domain.model.account.Account;
import com.kazuki43zoo.domain.model.account.AccountPasswordHistory;
import com.kazuki43zoo.domain.repository.account.AccountRepository;
import org.joda.time.DateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.terasoluna.gfw.common.date.jodatime.JodaTimeDateFactory;
import org.terasoluna.gfw.common.exception.ResourceNotFoundException;

@Transactional
@Service
@lombok.RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final PasswordEncoder passwordEncoder;

    private final JodaTimeDateFactory dateFactory;

    private final AccountRepository accountRepository;

    private final PasswordSharedService passwordSharedService;

    @Override
    public Account change(final String accountId, final String rawCurrentPassword, final String rawNewPassword) {
        final Account currentAccount = this.accountRepository.findOneByAccountId(accountId);

        authenticate(currentAccount, rawCurrentPassword);

        this.passwordSharedService.validatePassword(rawNewPassword, currentAccount);

        final DateTime currentDateTime = this.dateFactory.newDateTime();

        final String encodedNewPassword = this.passwordEncoder.encode(rawNewPassword);
        currentAccount.setPassword(encodedNewPassword);
        currentAccount.setPasswordModifiedAt(currentDateTime);
        this.accountRepository.update(currentAccount);
        this.passwordSharedService.resetPasswordLock(currentAccount);

        this.accountRepository.createPasswordHistory(new AccountPasswordHistory(currentAccount.getAccountUuid(), encodedNewPassword, currentDateTime));

        return currentAccount;

    }

    private void authenticate(final Account currentAccount, final String rawPassword) {
        if (currentAccount == null) {
            throw new ResourceNotFoundException(Message.SECURITY_ACCOUNT_NOT_FOUND.resultMessages());
        }
        if (!this.passwordEncoder.matches(rawPassword, currentAccount.getPassword())) {
            throw new ResourceNotFoundException(Message.SECURITY_ACCOUNT_NOT_FOUND.resultMessages());
        }
    }

}
