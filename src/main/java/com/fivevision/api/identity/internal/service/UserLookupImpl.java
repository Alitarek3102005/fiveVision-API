package com.fivevision.api.identity.internal.service;

import com.fivevision.api.identity.UserLookup;
import com.fivevision.api.identity.UserPublicSummary;
import com.fivevision.api.identity.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserLookupImpl implements UserLookup {
    private final UserRepository userRepository;

    @Override
    public Optional<UserPublicSummary> findPublicSummary(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new UserPublicSummary(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName()));
    }
}