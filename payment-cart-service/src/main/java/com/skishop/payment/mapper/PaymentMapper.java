package com.skishop.payment.mapper;

import com.skishop.payment.dto.PaymentResponse;
import com.skishop.payment.dto.PaymentIntentResponse;
import com.skishop.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "transactionId", expression = "java(extractTransactionId(payment))")
    @Mapping(target = "failureReason", expression = "java(extractFailureReason(payment))")
    PaymentResponse toPaymentResponse(Payment payment);

    List<PaymentResponse> toPaymentResponseList(List<Payment> payments);

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "clientSecret", expression = "java(extractClientSecret(payment))")
    PaymentIntentResponse toPaymentIntentResponse(Payment payment);

    default String extractTransactionId(Payment payment) {
        if (payment.getGatewayResponse() != null && payment.getGatewayResponse().containsKey("id")) {
            return payment.getGatewayResponse().get("id").toString();
        }
        return null;
    }

    default String extractFailureReason(Payment payment) {
        if (payment.getGatewayResponse() != null && payment.getGatewayResponse().containsKey("failure_reason")) {
            return payment.getGatewayResponse().get("failure_reason").toString();
        }
        return null;
    }

    default String extractClientSecret(Payment payment) {
        if (payment.getGatewayResponse() != null && payment.getGatewayResponse().containsKey("client_secret")) {
            return payment.getGatewayResponse().get("client_secret").toString();
        }
        return null;
    }
}
