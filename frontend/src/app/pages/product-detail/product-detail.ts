import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

@Component({
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-detail.html',
  styleUrls: ['./product-detail.scss']
})
export class ProductDetailComponent implements OnInit {
  product: any = null;
  loading = true;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    const productId = this.route.snapshot.paramMap.get('id');
    if (productId) {
      this.loadProductDetails(productId);
    } else {
      this.error = 'No product ID provided';
      this.loading = false;
    }
  }

  loadProductDetails(id: string) {
    this.loading = true;
    this.error = '';

    this.http.get<any>(`${environment.apiUrl}/products/${id}`)
      .pipe(
        catchError(err => {
          console.error('Failed to load product details:', err);
          this.error = err.status === 404 ? 'Product not found' : 'Failed to load product details';
          this.loading = false;
          return throwError(() => err);
        })
      )
      .subscribe({
        next: (product) => {
          console.log('Product details loaded:', product);
          this.product = product;
          this.loading = false;
        },
        error: (err) => {
          this.loading = false;
        }
      });
  }

  goBack() {
    this.router.navigate(['/products/my']);
  }

  getImageUrl(path: string): string {
    return path?.startsWith('http') ? path : `${environment.apiUrl}${path}`;
  }

  getQrUrl(path: string): string {
    return path?.startsWith('http') ? path : `${environment.apiUrl}${path}`;
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
      tomato: '🍅', apple: '🍎', onion: '🧅', mango: '🥭', potato: '🥔', rice: '🌾',
      banana: '🍌', orange: '🍊', grape: '🍇', wheat: '🌿',
      corn: '🌽', carrot: '🥕', cucumber: '🥒', strawberry: '🍓', watermelon: '🍉'
    };
    return Object.keys(map).find(k => n.includes(k))
      ? map[Object.keys(map).find(k => n.includes(k))!]
      : '🌱';
  }

  downloadQr() {
    if (!this.product?.qrCodePath) return;

    const url = this.getQrUrl(this.product.qrCodePath);
    const filename = this.generateFilename();

    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  private generateFilename(): string {
    const cleanName = (this.product?.cropName || 'Product')
      .replace(/[^a-zA-Z0-9]/g, '-')
      .replace(/-+/g, '-')
      .replace(/^-|-$/g, '');
    return `QR_${cleanName}_${this.product?.id}.png`;
  }

  generateQr() {
    if (!this.product?.id) return;

    this.http.post<any>(`${environment.apiUrl}/products/${this.product.id}/qrcode`, {}).subscribe({
      next: (res) => {
        const url = res.qrPath.startsWith('http')
          ? res.qrPath
          : `${environment.apiUrl}${res.qrPath}`;
        const filename = this.generateFilename();

        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        alert(`QR Generated & Downloaded: ${filename}`);
        this.loadProductDetails(this.product.id.toString());
      },
      error: (err) => alert(err.error?.message || 'Failed to generate QR')
    });
  }
}
