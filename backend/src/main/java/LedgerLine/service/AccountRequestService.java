
package LedgerLine.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import LedgerLine.entity.Account;
import LedgerLine.entity.AccountRequest;
import LedgerLine.entity.AppUser;
import LedgerLine.entity.RequestStatus;
import LedgerLine.repository.AccountRequestRepository;
import LedgerLine.repository.AppUserRepository;

@Service
public class AccountRequestService {

    private final AccountRequestRepository requestRepository;
    private final AppUserRepository userRepository;
    private final AccountService accountService;

    public AccountRequestService(
            AccountRequestRepository requestRepository,
            AppUserRepository userRepository,
            AccountService accountService) {

        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.accountService = accountService;
    }

    @Transactional
    public AccountRequest createRequest(String username) {

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (requestRepository.existsByUserIdAndStatus(
                user.getId(), RequestStatus.PENDING)) {

            throw new IllegalStateException(
                    "You already have a pending account request");
        }

        AccountRequest request = new AccountRequest();
        request.setUser(user);
        request.setStatus(RequestStatus.PENDING);

        return requestRepository.save(request);
    }

    public List<AccountRequest> getMyRequests(String username) {

        return requestRepository
                .findByUserUsernameOrderByRequestedAtDesc(username);
    }

    public List<AccountRequest> getPendingRequests() {

        return requestRepository
                .findByStatusOrderByRequestedAtAsc(
                        RequestStatus.PENDING);
    }

    @Transactional
    public Account approveRequest(Long requestId, String adminUsername) {

        AccountRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Account request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException(
                    "This request has already been reviewed");
        }

        Account account = accountService.createAccount(
                request.getUser().getId());

        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(adminUsername);

        requestRepository.save(request);

        return account;
    }

    @Transactional
    public AccountRequest rejectRequest(
            Long requestId,
            String adminUsername,
            String reason) {

        AccountRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Account request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException(
                    "This request has already been reviewed");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Rejection reason is required");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(adminUsername);
        request.setRejectionReason(reason.trim());

        return requestRepository.save(request);
    }
}
