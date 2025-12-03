import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

type PrimaryRole = 'ADMIN' | 'FARMER' | 'DISTRIBUTOR' | 'RETAILER' | 'CONSUMER' | 'USER';

interface User {
  id: number;
  name: string;
  email: string;
  roles: string[];
  isAdmin: boolean;
  primaryRole: PrimaryRole;
}

@Component({
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss'
})
export class AdminUsers implements OnInit {
  users: User[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  private normalizeRole(r: any): string {
    const val = (r?.name ?? r ?? '').toString().toUpperCase();
    return val.startsWith('ROLE_') ? val : `ROLE_${val}`;
  }

  private derivePrimaryRole(roles: string[]): PrimaryRole {
    const set = new Set(roles.map(this.normalizeRole));
    if (set.has('ROLE_ADMIN')) return 'ADMIN';
    if (set.has('ROLE_FARMER')) return 'FARMER';
    if (set.has('ROLE_DISTRIBUTOR')) return 'DISTRIBUTOR';
    if (set.has('ROLE_RETAILER')) return 'RETAILER';
    if (set.has('ROLE_CONSUMER')) return 'CONSUMER';
    return 'USER';
    }

  loadUsers(): void {
    this.http.get<any[]>('/api/admin/users').subscribe(data => {
      this.users = data.map(u => {
        const roles = (u.roles ?? []).map((r: any) => this.normalizeRole(r));
        const isAdmin = roles.includes('ROLE_ADMIN');
        return {
          id: u.id,
          name: u.name,
          email: u.email,
          roles,
          isAdmin,
          primaryRole: this.derivePrimaryRole(roles)
        };
      });
    });
  }

  roleClass(role: PrimaryRole): string {
    switch (role) {
      case 'ADMIN': return 'bg-purple-500/30 text-purple-200';
      case 'FARMER': return 'bg-emerald-500/30 text-emerald-200';
      case 'DISTRIBUTOR': return 'bg-cyan-500/30 text-cyan-200';
      case 'RETAILER': return 'bg-amber-500/30 text-amber-200';
      case 'CONSUMER': return 'bg-slate-500/30 text-slate-200';
      default: return 'bg-gray-500/30 text-gray-200';
    }
  }

  promote(userId: number): void {
    if (!confirm('Promote this user to Admin?')) return;
    this.http.post(`/api/admin/promote/${userId}`, {}).subscribe({
      next: () => {
        alert('User promoted to Admin successfully!');
        this.loadUsers();
      },
      error: (err) => alert(err.error?.message || 'Promotion failed')
    });
  }
}
