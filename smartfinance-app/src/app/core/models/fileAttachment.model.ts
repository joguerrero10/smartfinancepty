export interface FileAttachment {
  id: number;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  fileSizeFormatted: string;
  fileUrl: string;
  storageProvider: string;
  description?: string;
  expenseId?: number;
  expenseDescription?: string;
  createdAt: string;
}
