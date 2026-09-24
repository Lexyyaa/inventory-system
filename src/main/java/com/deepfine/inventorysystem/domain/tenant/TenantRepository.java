package com.deepfine.inventorysystem.domain.tenant;

public interface TenantRepository {

    Tenant getByCode(String code);
}
