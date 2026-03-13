import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BackgroundService, BackgroundVariant, BackgroundIntensity } from '../../services/background.service';
import { Subscription } from 'rxjs';

@Component({
  standalone: true,
  selector: 'app-animated-background',
  imports: [CommonModule],
  template: `
    <!-- Animated background blobs -->
    <div class="fixed inset-0 -z-20 overflow-hidden pointer-events-none">
      <!-- Primary blob -->
      <div 
        class="absolute w-96 h-96 rounded-full mix-blend-multiply filter blur-3xl animate-blob"
        [class]="primaryBlobClasses"
        [style.top]="primaryBlobPosition.top"
        [style.left]="primaryBlobPosition.left">
      </div>
      
      <!-- Secondary blob -->
      <div 
        class="absolute w-80 h-80 rounded-full mix-blend-multiply filter blur-3xl animate-blob animation-delay-2000"
        [class]="secondaryBlobClasses"
        [style.top]="secondaryBlobPosition.top"
        [style.right]="secondaryBlobPosition.right">
      </div>
      
      <!-- Tertiary blob -->
      <div 
        class="absolute w-72 h-72 rounded-full mix-blend-multiply filter blur-3xl animate-blob animation-delay-4000"
        [class]="tertiaryBlobClasses"
        [style.bottom]="tertiaryBlobPosition.bottom"
        [style.left]="tertiaryBlobPosition.left">
      </div>
      
      <!-- Quaternary blob (optional) -->
      <div 
        *ngIf="showQuaternaryBlob"
        class="absolute w-64 h-64 rounded-full mix-blend-multiply filter blur-3xl animate-float"
        [class]="quaternaryBlobClasses"
        [style.top]="quaternaryBlobPosition.top"
        [style.right]="quaternaryBlobPosition.right">
      </div>
    </div>

    <!-- Gradient background -->
    <div 
      class="fixed inset-0 -z-30"
      [class]="gradientBackgroundClasses">
    </div>
  `
})
export class AnimatedBackgroundComponent implements OnInit, OnDestroy {
  @Input() variant: BackgroundVariant = 'primary';
  @Input() intensity: BackgroundIntensity = 'medium';

  private subscriptions = new Subscription();
  currentVariant: BackgroundVariant = 'primary';
  currentIntensity: BackgroundIntensity = 'medium';

  constructor(private backgroundService: BackgroundService) {}

  ngOnInit() {
    // Subscribe to background service if no explicit inputs are provided
    if (!this.variant || !this.intensity) {
      this.subscriptions.add(
        this.backgroundService.variant$.subscribe(variant => {
          this.currentVariant = variant;
        })
      );

      this.subscriptions.add(
        this.backgroundService.intensity$.subscribe(intensity => {
          this.currentIntensity = intensity;
        })
      );
    } else {
      this.currentVariant = this.variant;
      this.currentIntensity = this.intensity;
    }
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }

  get gradientBackgroundClasses(): string {
    const baseClasses = 'bg-gradient-to-br';
    
    switch (this.currentVariant) {
      case 'primary':
        return `${baseClasses} from-dark-900 via-primary-900 to-dark-900`;
      case 'secondary':
        return `${baseClasses} from-dark-900 via-secondary-900 to-dark-900`;
      case 'accent':
        return `${baseClasses} from-dark-900 via-accent-900 to-dark-900`;
      case 'admin':
        return `${baseClasses} from-dark-900 via-purple-900 to-dark-900`;
      case 'dashboard':
        return `${baseClasses} from-dark-900 via-primary-900 to-secondary-900`;
      default:
        return `${baseClasses} from-dark-900 via-primary-900 to-dark-900`;
    }
  }

  get primaryBlobClasses(): string {
    const opacity = this.currentIntensity === 'light' ? 'opacity-20' : this.currentIntensity === 'medium' ? 'opacity-30' : 'opacity-40';
    const colorClass = this.currentVariant === 'admin' ? 'bg-purple-500' : 'bg-primary-500';
    return `${colorClass} ${opacity}`;
  }

  get secondaryBlobClasses(): string {
    const opacity = this.currentIntensity === 'light' ? 'opacity-15' : this.currentIntensity === 'medium' ? 'opacity-25' : 'opacity-35';
    const colorClass = this.currentVariant === 'admin' ? 'bg-indigo-500' : 'bg-accent-500';
    return `${colorClass} ${opacity}`;
  }

  get tertiaryBlobClasses(): string {
    const opacity = this.currentIntensity === 'light' ? 'opacity-10' : this.currentIntensity === 'medium' ? 'opacity-20' : 'opacity-30';
    const colorClass = this.currentVariant === 'admin' ? 'bg-pink-500' : 'bg-secondary-500';
    return `${colorClass} ${opacity}`;
  }

  get quaternaryBlobClasses(): string {
    const opacity = this.currentIntensity === 'light' ? 'opacity-10' : this.currentIntensity === 'medium' ? 'opacity-15' : 'opacity-25';
    const colorClass = this.currentVariant === 'admin' ? 'bg-purple-400' : 'bg-primary-400';
    return `${colorClass} ${opacity}`;
  }

  get showQuaternaryBlob(): boolean {
    return ['medium', 'strong'].includes(this.currentIntensity);
  }

  get primaryBlobPosition() {
    return { top: '-10rem', left: '-10rem' };
  }

  get secondaryBlobPosition() {
    return { top: '10rem', right: '-8rem' };
  }

  get tertiaryBlobPosition() {
    return { bottom: '-8rem', left: '50%' };
  }

  get quaternaryBlobPosition() {
    return { top: '25%', right: '25%' };
  }
}
