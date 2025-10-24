package com.bankabc.onboarding.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.bankabc.onboarding.constants.ApplicationConstants;
import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import com.bankabc.onboarding.openapi.model.OnboardingStartRequest;
import com.bankabc.onboarding.openapi.model.OnboardingStatusResponse;
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
     * Maps Onboarding entity to OnboardingStatusResponse DTO.
     * 
     * @param entity the onboarding entity
     * @return OnboardingStatusResponse DTO
     */
    @Mapping(target = "nextStep", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "currentStep", ignore = true)
    @Mapping(source = "processInstanceId", target = "processInstanceId")
    @Mapping(source = "status", target = "status", qualifiedByName = "mapStatusToEnum")
    @Mapping(target = "message", expression = "java(entity.getStatus().getDescription())")
    @Mapping(source = "accountNumber", target = "accountNumber")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "completedAt", target = "completedAt")
    @Mapping(source = "kycVerified", target = "kycVerified")
    @Mapping(source = "addressVerified", target = "addressVerified")
    OnboardingStatusResponse toStatusDto(Onboarding entity);


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

    /**
     * Maps entity OnboardingStatus to OpenAPI StatusEnum.
     * 
     * @param status the entity status
     * @return OpenAPI status enum
     */
    @Named("mapStatusToEnum")
    default OnboardingStatusResponse.StatusEnum mapStatusToEnum(OnboardingStatus status) {
        if (status == null) {
            return null;
        }
        return OnboardingStatusResponse.StatusEnum.fromValue(status.name());
    }

}
