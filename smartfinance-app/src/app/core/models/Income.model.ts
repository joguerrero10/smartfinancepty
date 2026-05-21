export interface Income {
  id: number;
  name: string;
  amount: number;
  netAmount: number;
  totalDeductions: number;
  incomeType: string;
  frequency: string;
  deductions: Deduction[];
  active: boolean;
  createdAt: string;
}

export interface IncomeRequest {
  name: string;
  amount: number;
  incomeType: string;
  frequency: string;
  deductions?: DeductionRequest[];
}

export interface Deduction {
  id: number;
  name: string;
  deductionType: string;
  isPercentage: boolean;
  value: number;
}

export interface DeductionRequest {
  name: string;
  deductionType: string;
  isPercentage: boolean;
  value: number;
}
