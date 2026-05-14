import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './footer.html'
})
export class Footer {
  private auth = inject(AuthService);

  get isLoggedIn() {
    return this.auth.isLoggedIn();
  }

  get isFarmer() {
    return this.auth.hasRole('ROLE_FARMER');
  }

  get year() {
    return new Date().getFullYear();
  }
}
