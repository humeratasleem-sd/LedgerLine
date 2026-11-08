package com.ledgerline.controller;

import java.util.List;

import com.ledgerline.dto.DepositRequest;
import com.ledgerline.dto.TransactionResponse;
import com.ledgerline.dto.TransferRequest;
import com.ledgerline.dto.WithdrawalRequest;
import com.ledgerline.service.AccountStatementService;
import com.ledgerline.service.MoneyMovementService;
import com.ledgerline.service.TransactionResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping
public class TransactionController {

    private final MoneyMovementService moneyMovementService;
    private final AccountStatementService accountStatementService;

    public TransactionController(
            MoneyMovementService moneyMovementService,
            AccountStatementService accountStatementService) {
        this.moneyMovementService = moneyMovementService;
        this.accountStatementService = accountStatementService;
    }

    @PostMapping("/api/transactions/transfers")
    public ResponseEntity<TransactionResponse> transfer(
            Authentication authentication,
            @RequestHeader("Idempotency-Key")
                    @NotBlank @Size(min = 8, max = 255) @Pattern(regexp = "[\\x21-\\x7E]{8,255}")
                    String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        return transactionResponse(moneyMovementService.transfer(authentication, request, idempotencyKey));
    }

    @PostMapping("/api/admin/transactions/deposits")
    public ResponseEntity<TransactionResponse> deposit(
            Authentication authentication,
            @RequestHeader("Idempotency-Key")
                    @NotBlank @Size(min = 8, max = 255) @Pattern(regexp = "[\\x21-\\x7E]{8,255}")
                    String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {
        return transactionResponse(moneyMovementService.deposit(authentication, request, idempotencyKey));
    }

    @PostMapping("/api/admin/transactions/withdrawals")
    public ResponseEntity<TransactionResponse> withdraw(
            Authentication authentication,
            @RequestHeader("Idempotency-Key")
                    @NotBlank @Size(min = 8, max = 255) @Pattern(regexp = "[\\x21-\\x7E]{8,255}")
                    String idempotencyKey,
            @Valid @RequestBody WithdrawalRequest request) {
        return transactionResponse(moneyMovementService.withdraw(authentication, request, idempotencyKey));
    }

    @GetMapping("/api/accounts/{accountId}/transactions")
    public List<TransactionResponse> statement(
            Authentication authentication,
            @PathVariable long accountId,
            @RequestParam(defaultValue = "50") int limit) {
        return accountStatementService.latestForAccount(authentication, accountId, limit);
    }

    private static ResponseEntity<TransactionResponse> transactionResponse(TransactionResult result) {
        return ResponseEntity.status(result.accepted() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY)
                .body(result.transaction());
    }
}