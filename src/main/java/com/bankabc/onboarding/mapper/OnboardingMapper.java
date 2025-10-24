package com.bankabc.onboarding.mapper;

import com.bankabc.onboarding.openapi.model.OnboardingStartRequest;
import com.bankabc.onboarding.openapi.model.OnboardingStartResponse;
import com.bankabc.onboarding.openapi.model.OnboardingStatusResponse;
import com.bankabc.onboarding.entity.Onboarding;
import com.bankabc.onboarding.entity.Onboarding.OnboardingStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.bankabc.onboarding.constants.ApplicationConstants;

/**
 * MapStruct mapper for converting between Onboarding entities and DTOs.
 * 
 * This mapper handles the conversion between:
 * - OnboardingRequest DTO to Onboarding entity
 * - Onboarding entity to OnboardingResponse DTO
 * - Partial updates of Onboarding entities
 * 
 * MapStruct will generate the implementation at compile time.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface OnboardingMapper {

    OnboardingMapper INSTANCE = Mappers.getMapper(OnboardingMapper.class);

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
     * Updates an existing Onboarding entity with values from OnboardingStartRequest.
     * Ignores null values in the source.
     * 
     * @param request the onboarding start request DTO
     * @param entity the existing onboarding entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "kycVerified", ignore = true)
    @Mapping(target = "kycVerificationNotes", ignore = true)
    @Mapping(target = "addressVerified", ignore = true)
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
    void updateEntityFromRequest(OnboardingStartRequest request, @MappingTarget Onboarding entity);

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
