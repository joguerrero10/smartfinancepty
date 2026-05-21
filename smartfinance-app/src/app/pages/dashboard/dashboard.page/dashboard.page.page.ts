import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import {
  IonButtons,
  IonCardSubtitle,
  IonContent,
  IonHeader,
  IonItem,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import { DashboardData } from '../../../core/models';
import { GraphqlService } from '../../../core/services/graphql.service';
// import { NotificationsPage } from '../notifications/notifications.page';

@Component({
  selector: 'app-dashboard.page',
  templateUrl: './dashboard.page.page.html',
  styleUrls: ['./dashboard.page.page.scss'],
  standalone: true,
  imports: [
    IonButtons,
    IonItem,
    IonCardSubtitle,
    IonContent,
    IonHeader,
    IonTitle,
    IonToolbar,
    CommonModule,
    FormsModule,
  ],
})
export class DashboardPagePage implements OnInit {
  dashboard: DashboardData | null = null;
  loading = true;
  unreadCount = 0;

  constructor(
    private readonly graphql: GraphqlService,
    private readonly modalCtrl: ModalController,
  ) {}

  ngOnInit() {
    this.loadDashboard();
  }

  loadDashboard() {
    this.loading = true;
    this.graphql.getDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  get savingsBadgeColor(): string {
    const pct = this.dashboard?.savingsPercentage ?? 0;
    if (pct >= 20) return 'success';
    if (pct >= 10) return 'warning';
    return 'danger';
  }

  async refresh(event: any) {
    this.loadDashboard();
    event.target.complete();
  }

  // async openNotifications() {
  //     const modal = await this.modalCtrl.create({
  //       component: NotificationsPage,
  //       breakpoints: [0, 0.5, 0.9],
  //       initialBreakpoint: 0.9,
  //       handle: tr,ue,
  //     });
  //     await modal.present();
  //   }
}
