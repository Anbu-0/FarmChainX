import { Component, inject, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AdminService } from '../../services/admin.service';

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink],
  selector: 'app-dashboard',
  templateUrl: './dashboard.html'
})
export class Dashboard implements AfterViewInit {
  private authService = inject(AuthService);
  private adminService = inject(AdminService);
  private router = inject(Router);

  name = this.authService.getName() || 'User';
  role = this.authService.getRole()?.replace('ROLE_', '') || 'USER';
  isAdmin = this.authService.isAdmin();

  ngAfterViewInit(): void {
    const ringColor =
      getComputedStyle(document.documentElement)
        .getPropertyValue('--tw-ring-color') || '(not set)';
    console.log('tw-ring-color:', ringColor);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  requestAdminAccess() {
    this.adminService.requestAdminAccess().subscribe({
      next: () => alert('Admin access requested!'),
      error: () => alert('Already requested or you are admin'),
    });
  }
}
