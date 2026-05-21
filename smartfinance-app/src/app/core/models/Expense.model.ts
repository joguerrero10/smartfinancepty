export interface Expense {
  id: number;
  description: string;
  amount: number;
  expenseType: 'FIXED' | 'VARIABLE';
  categoryId?: number;
  categoryName: string;
  subCategoryName?: string;
  expenseDate: string;
  frequency?: string;
  dueDay?: number;
  notes?: string;
  active: boolean;
  createdAt: string;
}

export interface ExpenseRequest {
  description: string;
  amount: number;
  expenseType: 'FIXED' | 'VARIABLE';
  categoryId: number;
  subCategoryId?: number;
  expenseDate: string;
  frequency?: string;
  dueDay?: number;
  notes?: string;
}

export interface ExpenseCategory {
  id: number;
  name: string;
  color: string;
  icon: string;
}
