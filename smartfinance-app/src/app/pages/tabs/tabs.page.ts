import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  IonIcon,
  IonLabel,
  IonTabBar,
  IonTabButton,
  IonTabs,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  analyticsOutline,
  homeOutline,
  personOutline,
  receiptOutline,
  walletOutline,
} from 'ionicons/icons';

@Component({
  selector: 'app-tabs',
  templateUrl: './tabs.page.html',
  styleUrls: ['./tabs.page.scss'],
  standalone: true,
  imports: [
    IonTabs,
    IonTabButton,
    IonIcon,
    IonTabBar,
    IonLabel,
    CommonModule,
    FormsModule,
  ],
})
export class TabsPage {
  constructor() {
    addIcons({
      homeOutline,
      receiptOutline,
      analyticsOutline,
      walletOutline,
      personOutline,
    });
  }
}
