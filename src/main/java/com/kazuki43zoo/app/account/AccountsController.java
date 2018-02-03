package com.kazuki43zoo.app.account;

import com.kazuki43zoo.core.message.Message;
import com.kazuki43zoo.domain.model.account.Account;
import com.kazuki43zoo.domain.model.account.AccountAuthority;
import com.kazuki43zoo.domain.repository.account.AccountsSearchCriteria;
import com.kazuki43zoo.domain.service.account.AccountService;
import org.dozer.Mapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.terasoluna.gfw.common.exception.BusinessException;
import org.terasoluna.gfw.web.token.transaction.TransactionTokenCheck;
import org.terasoluna.gfw.web.token.transaction.TransactionTokenType;

@TransactionTokenCheck("accounts")
@RequestMapping("accounts")
@Controller
@lombok.RequiredArgsConstructor
public class AccountsController {

    private final Mapper beanMapper;

    private final AccountService accountService;

    @GetMapping
    public String list(final @Validated AccountsSearchQuery query, final BindingResult bindingResult, final @PageableDefault(size = 15) Pageable pageable, final Model model) {
        if (bindingResult.hasErrors()) {
            return "account/list";
        }
        final AccountsSearchCriteria criteria = this.beanMapper.map(query, AccountsSearchCriteria.class);
        final Page<Account> page = this.accountService.searchAccounts(criteria, pageable);
        model.addAttribute("page", page);
        return "account/list";
    }

    @TransactionTokenCheck(type = TransactionTokenType.BEGIN)
    @GetMapping(path = "{accountUuid}")
    public String show(final @PathVariable("accountUuid") String accountUuid, final Model model) {
        final Account account = this.accountService.getAccount(accountUuid);
        model.addAttribute(account);
        return "account/detail";
    }

    @TransactionTokenCheck(value = "create", type = TransactionTokenType.BEGIN)
    @GetMapping(params = "form=create")
    public String createForm(final AccountForm form) {
        return "account/createForm";
    }

    @TransactionTokenCheck(value = "create")
    @PostMapping
    public String create(final @Validated AccountForm form, final BindingResult bindingResult, final Model model, final RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return createForm(form);
        }

        final Account inputAccount = this.beanMapper.map(form, Account.class);
        for (final String authority : form.getAuthorities()) {
            inputAccount.addAuthority(new AccountAuthority(null, authority));
        }

        final Account createdAccount;
        try {
            createdAccount = this.accountService.create(inputAccount);
        } catch (final DuplicateKeyException e) {
            model.addAttribute(Message.ACCOUNT_ID_USED.resultMessages());
            return createForm(form);
        } catch (final BusinessException e) {
            model.addAttribute(e.getResultMessages());
            return createForm(form);
        }

        return redirectDetailView(createdAccount.getAccountUuid(), Message.ACCOUNT_CREATED, redirectAttributes);
    }

    @TransactionTokenCheck(value = "edit", type = TransactionTokenType.BEGIN)
    @GetMapping(path = "{accountUuid}", params = "form=edit")
    public String editForm(final @PathVariable("accountUuid") String accountUuid, final AccountForm form, final Model model) {

        final Account account = this.accountService.getAccount(accountUuid);
        model.addAttribute(account);
        this.beanMapper.map(account, form);
        for (final AccountAuthority accountAuthority : account.getAuthorities()) {
            form.addAuthority(accountAuthority.getAuthority());
        }
        form.setPassword(null);
        return "account/editForm";
    }

    @TransactionTokenCheck(value = "edit")
    @PostMapping(path = "{accountUuid}", params = "_method=put")
    public String edit(final @PathVariable("accountUuid") String accountUuid, final @Validated AccountForm form, final BindingResult bindingResult, final Model model, final RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return editRedo(accountUuid, model);
        }

        final Account inputAccount = this.beanMapper.map(form, Account.class);
        inputAccount.setAccountUuid(accountUuid);
        for (final String authority : form.getAuthorities()) {
            inputAccount.addAuthority(new AccountAuthority(accountUuid, authority));
        }

        try {
            this.accountService.change(inputAccount);
        } catch (final DuplicateKeyException e) {
            model.addAttribute(Message.ACCOUNT_ID_USED.resultMessages());
            return editRedo(accountUuid, model);
        } catch (final BusinessException e) {
            model.addAttribute(e.getResultMessages());
            return editRedo(accountUuid, model);
        }

        return redirectDetailView(accountUuid, Message.ACCOUNT_EDITED, redirectAttributes);
    }

    @TransactionTokenCheck
    @PostMapping(path = "{accountUuid}", params = "_method=delete")
    public String delete(final @PathVariable("accountUuid") String accountUuid, final RedirectAttributes redirectAttributes) {

        this.accountService.delete(accountUuid);

        redirectAttributes.addFlashAttribute(Message.ACCOUNT_DELETED.resultMessages());
        return "redirect:/app/accounts";
    }

    @TransactionTokenCheck
    @PostMapping(path = "{accountUuid}/unlock", params = "_method=patch")
    public String unlock(final @PathVariable("accountUuid") String accountUuid, final RedirectAttributes redirectAttributes) {

        this.accountService.unlock(accountUuid);

        return redirectDetailView(accountUuid, Message.ACCOUNT_UNLOCKED, redirectAttributes);
    }

    private String editRedo(final String accountUuid, final Model model) {
        final Account account = this.accountService.getAccount(accountUuid);
        model.addAttribute(account);
        return "account/editForm";
    }

    private String redirectDetailView(final String accountUuid, final Message message, final RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(message.resultMessages());
        redirectAttributes.addAttribute("accountUuid", accountUuid);
        return "redirect:/app/accounts/{accountUuid}";
    }

}
