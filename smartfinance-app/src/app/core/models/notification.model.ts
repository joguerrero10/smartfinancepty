export interface Notification {
  id: number;
  title: string;
  message: string;
  type: string;
  channel: string;
  read: boolean;
  referenceId?: number;
  referenceType?: string;
  createdAt: string;
}
