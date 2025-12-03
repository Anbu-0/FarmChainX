import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { DatePipe } from '@angular/common';

@Component({
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './admin-promotion-requests.html',
  styleUrl: './admin-promotion-requests.scss'
})
export class AdminPromotionRequests implements OnInit {
  requests: any[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.http.get<any[]>('/api/admin/promotion-requests').subscribe(data => {
      this.requests = data;
    });
  }

  approve(id: number): void {
    this.http.post(`/api/admin/promotion-requests/${id}/approve`, {}).subscribe({
      next: () => {
        alert('Admin access granted!');
        this.loadRequests();
      }
    });
  }

  reject(id: number): void {
    if (!confirm('Reject this admin request?')) return;
    this.http.post(`/api/admin/promotion-requests/${id}/reject`, {}).subscribe({
      next: () => {
        alert('Request rejected');
        this.loadRequests();
      }
    });
  }
}