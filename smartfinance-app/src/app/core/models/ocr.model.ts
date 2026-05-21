import { SeverityLevel } from './analytics.model';

export interface OcrResult {
  rawText: string;
  totalAmount?: number;
  invoiceDate?: string;
  merchantName?: string;
  merchantRuc?: string;
  taxAmount?: number;
  subtotal?: number;
  currency: string;
  confidence: number;
  processed: boolean;
  errorMessage?: string;
  suggestion?: ExpenseSuggestion;
}

export interface ExpenseSuggestion {
  suggestedAmount: number;
  suggestedCategoryId: number;
  suggestedCategoryName?: string;
  suggestedDescription: string;
  suggestedDate: string;
  confidence: SeverityLevel;
}
