package com.smartfinancepty.finance.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Sube un archivo y retorna la URL pública de acceso.
     *
     * @param file el archivo a subir
     * @param folder carpeta/prefijo (ej: "receipts/userId")
     * @return resultado con URL y storage key
     */
    StorageResult upload(MultipartFile file, String folder);

    /**
     * Elimina un archivo por su storage key.
     */
    void delete(String storageKey);

    /**
     * Nombre del proveedor: LOCAL, S3, CLOUDINARY
     */
    String getProviderName();
}
