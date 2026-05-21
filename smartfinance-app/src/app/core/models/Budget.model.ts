export interface Budget {
  id: number;
  categoryId?: number;
  categoryName: string;
  isGlobal: boolean;
  limitAmount: number;
  spentAmount: number;
  remainingAmount: number;
  usagePercentage: number;
  isOverBudget: boolean;
  isNearLimit: boolean;
  alertMessage?: string;
  year: number;
  month: number;
}
