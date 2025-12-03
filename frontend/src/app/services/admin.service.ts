import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AdminOverview {
  totalUsers: number;
  totalProducts: number;
  totalLogs: number;
  totalFeedbacks: number;
}

export interface UserDto {
  id: number;
  name: string;
  email: string;
  roles: string[];
}

export interface AdminPromotionRequestDto {
  id: number;
  user: {
    id: number;
    name: string;
    email: string;
  };
  requestedAt: string;
  approved?: boolean;
  rejected?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  private baseUrl = '/api/admin';  

  constructor(private http: HttpClient) {}

  getOverview(): Observable<AdminOverview> {
    return this.http.get<AdminOverview>(`${this.baseUrl}/overview`);
  }

  getAllUsers(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(`${this.baseUrl}/users`);
  }

  promoteUser(userId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/promote/${userId}`, {});
  }

  requestAdminAccess(): Observable<any> {
    return this.http.post(`${this.baseUrl}/request-admin`, {});
  }

  getPendingRequests(): Observable<AdminPromotionRequestDto[]> {
    return this.http.get<AdminPromotionRequestDto[]>(`${this.baseUrl}/promotion-requests`);
  }

  approveRequest(requestId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/promotion-requests/${requestId}/approve`, {});
  }

  rejectRequest(requestId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/promotion-requests/${requestId}/reject`, {});
  }
}