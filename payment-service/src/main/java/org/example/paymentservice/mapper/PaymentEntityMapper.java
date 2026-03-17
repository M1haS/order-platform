package org.example.paymentservice.mapper;

import org.example.paymentservice.dto.CreatePaymentRequestDto;
import org.example.paymentservice.dto.CreatePaymentResponseDto;
import org.example.paymentservice.model.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)
public interface PaymentEntityMapper {
    PaymentEntity toEntity(CreatePaymentRequestDto requestDto);

    @Mapping(source = "id", target = "paymentId")
    CreatePaymentResponseDto toResponseDto(PaymentEntity entity);
}
