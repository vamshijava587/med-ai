package com.medai.shared.api;

import java.util.List;

public record UserProfile(
    String userId,
    String fullName,
    List<String> allergies,
    List<String> medications,
    String insuranceProvider,
    String insurancePlan,
    String notes
) {
}
