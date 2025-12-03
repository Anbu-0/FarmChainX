import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html'
})
export class Navbar {
  private router = inject(Router);
  private auth = inject(AuthService);

  get isLoggedIn() {
    return this.auth.isLoggedIn();
  }

  get userRole() {
    return this.auth.getRole();
  }

  get userName() {
    return this.auth.getName();
  }

  get isFarmer() {
    return this.auth.hasRole('ROLE_FARMER');
  }

  // THIS MAKES THE ADMIN BUTTON APPEAR
  get isAdmin() {
    return this.auth.isAdmin();
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}