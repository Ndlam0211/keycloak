package com.lamnd.profile.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lamnd.profile.dto.request.RegistrationRequest;
import com.lamnd.profile.dto.response.ProfileResponse;
import com.lamnd.profile.mapper.ProfileMapper;
import com.lamnd.profile.repository.ProfileRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileService {
    ProfileRepository profileRepository;
    ProfileMapper profileMapper;

    public List<ProfileResponse> getAllProfiles() {
        var profiles = profileRepository.findAll();
        return profiles.stream().map(profileMapper::toProfileResponse).toList();
    }

    public ProfileResponse register(RegistrationRequest request) {
        var profile = profileMapper.toProfile(request);
        profile = profileRepository.save(profile);

        return profileMapper.toProfileResponse(profile);
    }
}
