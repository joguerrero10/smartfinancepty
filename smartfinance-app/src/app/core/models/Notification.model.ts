export interface Notification {
  id: number;
  title: string;
  message: string;
  type: string;
  channel: string;
  read: boolean;
  createdAt: string;
}
