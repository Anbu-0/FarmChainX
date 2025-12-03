import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterModule } from '@angular/router';

@Component({
  standalone: true,
  selector: 'app-register',
  templateUrl: './register.html',
  imports: [CommonModule, FormsModule, RouterModule]
})
export class Register {
  name = '';
  email = '';
  password = '';
  role = '';

  constructor(private http: HttpClient, private router: Router) {}

  submit() {
    if (!this.role) {
      alert('Please select a role');
      return;
    }

    this.http.post('/api/auth/register', {
      name: this.name,
      email: this.email,
      password: this.password,
      role: this.role
    }).subscribe({
      next: () => {
        alert('Registration successful 🎉');
        this.router.navigate(['/login']);
      },
    error: (err) => {
  console.log("REGISTER ERROR →", err);
  alert(JSON.stringify(err.error));
    }
    });
  }
}
