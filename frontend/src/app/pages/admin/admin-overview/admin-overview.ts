import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

interface OverviewData {
  totalUsers: number;
  totalProducts: number;
  totalLogs: number;
  totalFeedbacks: number;
}

@Component({
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-overview.html',
  styleUrl: './admin-overview.scss'
})
export class AdminOverview implements OnInit {
  data?: OverviewData;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<OverviewData>('/api/admin/overview').subscribe(res => {
      this.data = res;
    });
  }
}