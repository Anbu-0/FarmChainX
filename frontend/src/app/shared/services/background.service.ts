import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type BackgroundVariant = 'primary' | 'secondary' | 'accent' | 'admin' | 'dashboard';
export type BackgroundIntensity = 'light' | 'medium' | 'strong';

@Injectable({
  providedIn: 'root'
})
export class BackgroundService {
  private variantSubject = new BehaviorSubject<BackgroundVariant>('primary');
  private intensitySubject = new BehaviorSubject<BackgroundIntensity>('medium');

  variant$ = this.variantSubject.asObservable();
  intensity$ = this.intensitySubject.asObservable();

  setVariant(variant: BackgroundVariant) {
    this.variantSubject.next(variant);
  }

  setIntensity(intensity: BackgroundIntensity) {
    this.intensitySubject.next(intensity);
  }

  setBackground(variant: BackgroundVariant, intensity: BackgroundIntensity = 'medium') {
    this.setVariant(variant);
    this.setIntensity(intensity);
  }

  // Convenience methods for common page types
  setPrimaryBackground(intensity: BackgroundIntensity = 'medium') {
    this.setBackground('primary', intensity);
  }

  setAdminBackground(intensity: BackgroundIntensity = 'strong') {
    this.setBackground('admin', intensity);
  }

  setDashboardBackground(intensity: BackgroundIntensity = 'medium') {
    this.setBackground('dashboard', intensity);
  }
}
