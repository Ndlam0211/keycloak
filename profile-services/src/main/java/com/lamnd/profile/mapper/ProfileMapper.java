package com.lamnd.profile.mapper;

import org.mapstruct.Mapper;

import com.lamnd.profile.dto.request.RegistrationRequest;
import com.lamnd.profile.dto.response.ProfileResponse;
import com.lamnd.profile.entity.Profile;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    Profile toProfile(RegistrationRequest request);

    ProfileResponse toProfileResponse(Profile profile);
}
