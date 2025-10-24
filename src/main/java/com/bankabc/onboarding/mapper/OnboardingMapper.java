package com.bankabc.onboarding.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.bankabc.onboarding.constants.ApplicationConstants;
import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.openapi.model.OnboardingStartRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between Onboarding entities and DTOs.
 * MapStruct will generate the implementation at compile time.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface OnboardingMapper {


    /**
     * Maps OnboardingStartRequest DTO to Onboarding entity.
     * Sets default values for fields not present in the request.
     * 
     * @param request the onboarding start request DTO
     * @return Onboarding entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = ApplicationConstants.Mapping.INITIATED_STATUS)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "kycVerified", constant = ApplicationConstants.Mapping.FALSE_VALUE)
    @Mapping(target = "kycVerificationNotes", ignore = true)
    @Mapping(target = "addressVerified", constant = ApplicationConstants.Mapping.FALSE_VALUE)
    @Mapping(target = "addressVerificationNotes", ignore = true)
    @Mapping(target = "processInstanceId", ignore = true)
    @Mapping(target = "processDefinitionKey", ignore = true)
    @Mapping(target = "passportPath", ignore = true)
    @Mapping(target = "photoPath", ignore = true)
    @Mapping(target = "documentUploadedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(source = "dob", target = "dateOfBirth")
    @Mapping(source = "gender", target = "gender", qualifiedByName = "mapGender")
    Onboarding toEntity(OnboardingStartRequest request);



    /**
     * Maps LocalDateTime to OffsetDateTime using UTC timezone.
     * 
     * @param localDateTime the LocalDateTime to convert
     * @return OffsetDateTime in UTC
     */
    default OffsetDateTime map(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }

    /**
     * Maps OpenAPI Gender enum to entity Gender enum.
     * 
     * @param gender the OpenAPI gender enum
     * @return entity gender enum
     */
    @Named("mapGender")
    default Onboarding.Gender mapGender(OnboardingStartRequest.GenderEnum gender) {
        if (gender == null) {
            return null;
        }
        return Onboarding.Gender.valueOf(gender.getValue());
    }


}
