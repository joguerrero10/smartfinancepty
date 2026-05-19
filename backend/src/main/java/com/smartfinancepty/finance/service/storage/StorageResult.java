package com.smartfinancepty.finance.service.storage;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageResult {
    private String fileUrl;
    private String storageKey;
    private String storedFilename;
    private String provider;
}
