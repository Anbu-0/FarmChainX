import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, delay, retryWhen, scan, throwError } from 'rxjs';

@Component({
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-products.html'
})
export class MyProducts {
  products: any[] = [];
  loading = true;
  retryMessage = '';
  page = 0;
  size = 9;
  totalPages = 0;

  constructor(private http: HttpClient, private router: Router) {
    this.load();
  }

  load(page: number = 0) {
    this.loading = true;
    this.retryMessage = '';

    this.http
      .get<any>(`/api/products/my?page=${page}&size=${this.size}&sort=id,desc`)
      .pipe(
        retryWhen(errors =>
          errors.pipe(
            scan((retryCount) => {
              retryCount++;
              if (retryCount > 3) throw errors;
              this.retryMessage = `🔁 Reconnecting... (Attempt ${retryCount} of 3)`;
              return retryCount;
            }, 0),
            delay(1000) // exponential backoff could be delay(500 * Math.pow(2, retryCount))
          )
        ),
        catchError(err => {
          this.retryMessage = '';
          this.loading = false;
          alert('❌ Failed to load products after multiple attempts.');
          return throwError(() => err);
        })
      )
      .subscribe({
        next: (res) => {
          this.products = res?.content || [];
          this.page = res?.number || 0;
          this.totalPages = res?.totalPages || 1;
          this.loading = false;
          this.retryMessage = '';
        }
      });
  }

  nextPage() {
    if (this.page < this.totalPages - 1) this.load(this.page + 1);
  }

  prevPage() {
    if (this.page > 0) this.load(this.page - 1);
  }

  generateQr(id: number) {
    this.http.post<any>(`/api/products/${id}/qrcode`, {}).subscribe({
      next: (res) => {
        const product = this.products.find(p => p.id === id)!;
        const url = res.qrPath.startsWith('http')
          ? res.qrPath
          : `http://localhost:8080${res.qrPath}`;
        const filename = this.generateFilename(product);

        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        alert(`QR Generated & Downloaded: ${filename}`);
        this.load(this.page);
      },
      error: (err) => alert(err.error?.message || 'Failed to generate QR')
    });
  }

  downloadQr(id: number) {
    const product = this.products.find(p => p.id === id);
    if (!product?.qrCodePath) return;

    const url = this.getQrUrl(product.qrCodePath);
    const filename = this.generateFilename(product);

    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  private generateFilename(product: any): string {
    const cleanName = (product.cropName || 'Product')
      .replace(/[^a-zA-Z0-9]/g, '-')
      .replace(/-+/g, '-')
      .replace(/^-|-$/g, '');
    return `QR_${cleanName}_${product.id}.png`;
  }

  getImageUrl(path: string): string {
    return path?.startsWith('http') ? path : `http://localhost:8080${path}`;
  }

  getQrUrl(path: string): string {
    return path?.startsWith('http') ? path : `http://localhost:8080${path}`;
  }

  formatDate(date: string | null): string {
    if (!date) return 'Unknown Date';
    return new Date(date).toLocaleDateString('en-US', {
      month: 'long',
      day: 'numeric',
      year: 'numeric'
    });
  }

  getCropEmoji(name: string): string {
    const n = (name || '').toLowerCase();
    const map: Record<string, string> = {
      onion: '🧅', tomato: '🍅', mango: '🥭', potato: '🥔', rice: '🌾',
      banana: '🍌', apple: '🍎', orange: '🍊', grape: '🍇', wheat: '🌿',
      corn: '🌽', carrot: '🥕', cucumber: '🥒', strawberry: '🍓', watermelon: '🍉'
    };
    return Object.keys(map).find(k => n.includes(k))
      ? map[Object.keys(map).find(k => n.includes(k))!]
      : '🌱';
  }
}
