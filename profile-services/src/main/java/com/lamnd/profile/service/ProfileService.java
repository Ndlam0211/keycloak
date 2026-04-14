package com.lamnd.profile.service;

import java.util.List;

import com.lamnd.profile.exception.AppException;
import com.lamnd.profile.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.lamnd.profile.dto.identity.Credential;
import com.lamnd.profile.dto.identity.TokenExchangeParam;
import com.lamnd.profile.dto.identity.TokenExchangeResponse;
import com.lamnd.profile.dto.identity.UserCreationParam;
import com.lamnd.profile.dto.request.RegistrationRequest;
import com.lamnd.profile.dto.response.ProfileResponse;
import com.lamnd.profile.exception.ErrorNormalizer;
import com.lamnd.profile.feign.IdentityClient;
import com.lamnd.profile.mapper.ProfileMapper;
import com.lamnd.profile.repository.ProfileRepository;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileService {
    ProfileRepository profileRepository;
    ProfileMapper profileMapper;
    IdentityClient identityClient;
    ErrorNormalizer errorNormalizer;

    @Value("${idp.client-id}")
    @NonFinal
    String idpClientId;

    @Value("${idp.client-secret}")
    @NonFinal
    String idpClientSecret;

    @PreAuthorize("hasRole('ADMIN')")
    public List<ProfileResponse> getAllProfiles() {
        var profiles = profileRepository.findAll();
        return profiles.stream().map(profileMapper::toProfileResponse).toList();
    }

    public ProfileResponse getMyProfile() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName(); // get subject from jwt token, subject is userid in keycloak

        var profile = profileRepository.findByUserId(userId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED)
        );

        return profileMapper.toProfileResponse(profile);
    }

    public ProfileResponse register(RegistrationRequest request) {
        try {
            // exchange client token of keycloak
            TokenExchangeResponse tokenExchangeResponse = identityClient.exchangeToken(TokenExchangeParam.builder()
                    .client_id(idpClientId)
                    .client_secret(idpClientSecret)
                    .grant_type("client_credentials")
                    .scope("openid")
                    .build());

            log.info("Token exchange response: {}", tokenExchangeResponse);

            // create user in keycloak with the token and given info
            var response = identityClient.createUser(
                    "Bearer " + tokenExchangeResponse.getAccessToken(),
                    UserCreationParam.builder()
                            .username(request.getUsername())
                            .email(request.getEmail())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .enabled(true)
                            .emailVerified(false)
                            .credentials(List.of(Credential.builder()
                                    .type("password")
                                    .value(request.getPassword())
                                    .temporary(false)
                                    .build()))
                            .build());

            // get userid of the account in keycloak just created
            String userId = extractUserIdFromResponse(response);

            var profile = profileMapper.toProfile(request);
            profile.setUserId(userId); // map an account in keycloak to a profile in database
            profile = profileRepository.save(profile);

            return profileMapper.toProfileResponse(profile);
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
    }

    private String extractUserIdFromResponse(ResponseEntity<?> response) {
        String location = response.getHeaders().getLocation().getPath();

        String[] segments = location.split("/");
        return segments[segments.length - 1];
    }
}
