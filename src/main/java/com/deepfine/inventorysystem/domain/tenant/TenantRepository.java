package com.deepfine.inventorysystem.domain.tenant;

public interface TenantRepository {

    /** 업체 코드로 등록된 업체를 찾는다. 없으면 INVALID_TENANT. */
    Tenant getByCode(String code);
}
