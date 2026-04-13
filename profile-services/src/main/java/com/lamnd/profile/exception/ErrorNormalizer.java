package com.lamnd.profile.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lamnd.profile.dto.response.KeyCloakErrorResponse;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ErrorNormalizer {
    private final ObjectMapper objectMapper;
    private final Map<String, ErrorCode> errorMapping;

    public ErrorNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.errorMapping = new HashMap<>();

        errorMapping.put("User exists with same username", ErrorCode.USERNAME_EXISTED);
        errorMapping.put("User exists with same email", ErrorCode.EMAIL_EXISTED);
        errorMapping.put("User name is missing", ErrorCode.USERNAME_MISSING);
    }

    public AppException handleKeyCloakException(FeignException feignException) {
        try {
            log.warn("Received KeyCloak error: {}", feignException.contentUTF8());
            var response = objectMapper.readValue(feignException.contentUTF8(), KeyCloakErrorResponse.class);

            if (Objects.nonNull(response.getErrorMessage())
                    && Objects.nonNull(errorMapping.get(response.getErrorMessage()))) {
                var errorCode = errorMapping.get(response.getErrorMessage());
                return new AppException(errorCode);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse KeyCloak error response: {}", e);
        }
        return new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
}
