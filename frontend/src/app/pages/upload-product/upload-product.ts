import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-upload-product',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './upload-product.html'
})
export class UploadProduct {
  cropName = '';
  soilType = '';
  pesticides = '';
  harvestDate = '';             // yyyy-MM-dd
  gpsLocation = '';
  imageFile: File | null = null;
  previewUrl: string | ArrayBuffer | null = null;

  loading = false;
  today = this.getTodayString(); // used as max for date input

  constructor(private http: HttpClient, private router: Router) {}

  // helper to produce today's date in yyyy-MM-dd
  private getTodayString(): string {
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  onFileSelected(event: any) {
    const file = event.target.files && event.target.files[0];
    if (!file) return;

    this.imageFile = file;
    const reader = new FileReader();
    reader.onload = () => this.previewUrl = reader.result;
    reader.readAsDataURL(file);
  }

  detectGPS() {
    if (!navigator.geolocation) {
      alert("GPS not supported");
      return;
    }

    navigator.geolocation.getCurrentPosition(position => {
      const lat = position.coords.latitude;
      const lng = position.coords.longitude;
      this.gpsLocation = `${lat},${lng}`;
      alert('GPS detected');
    }, (err) => {
      console.warn('GPS error', err);
      alert('Unable to detect GPS');
    });
  }

  uploadProduct() {
    if (!this.imageFile) {
      alert("Please select an image");
      return;
    }

    // Prevent future-date uploads
    if (this.harvestDate) {
      if (this.harvestDate > this.today) {
        alert('Harvest date cannot be in the future.');
        return;
      }
    }

    this.loading = true;

    const formData = new FormData();
    formData.append('cropName', this.cropName.trim());
    formData.append('soilType', this.soilType.trim());
    formData.append('pesticides', this.pesticides.trim());
    // ensure backend-friendly date format yyyy-MM-dd (input already gives it)
    formData.append('harvestDate', this.harvestDate);
    formData.append('gpsLocation', this.gpsLocation.trim());
    formData.append('image', this.imageFile);

    this.http.post<any>('/api/products/upload', formData)
      .subscribe({
        next: (res) => {
          this.loading = false;
          alert(`Product uploaded! ID = ${res.id}`);
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading = false;
          console.error('Upload error full:', err);
          console.error('Status:', err?.status);
          console.error('Error body:', err?.error);
          const serverMsg = err?.error?.message || err?.error?.error || err?.statusText || (err?.message ? err.message : 'Upload failed!');
          alert(`Upload failed: ${serverMsg}`);
        }
      });
  }
}
