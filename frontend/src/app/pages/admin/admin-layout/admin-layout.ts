import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AdminSidebar } from '../components/admin-sidebar/admin-sidebar';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, AdminSidebar],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss'
})
export class AdminLayout {

  // ADD THIS CONSTRUCTOR + METHOD
  constructor(private authService: AuthService) {}

  currentAdminName(): string {
    const name = this.authService.getName();
    return name ? name.split(' ')[0] : 'Admin';   
}
}